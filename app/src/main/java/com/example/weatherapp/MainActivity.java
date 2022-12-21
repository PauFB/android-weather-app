package com.example.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private TextView currentLocationTextView;
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView temperatureTextView;
    private TextView skyTextView;

    private EditText searchedLocationEditText;
    private EditText searchedLocationTemperatureEditText;
    private EditText searchedLocationSkyEditText;

    private Button refreshButton;
    private Button searchButton;

    private LocationManager locationManager;

    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        currentLocationTextView = findViewById(R.id.currentLocation_EditText);
        latitudeTextView = findViewById(R.id.latitude_TextView);
        longitudeTextView = findViewById(R.id.longitude_TextView);
        temperatureTextView = findViewById(R.id.currentTemperature);
        skyTextView = findViewById(R.id.sky_TextView);

        searchedLocationEditText = findViewById(R.id.searchedLocation_EditText);
        searchedLocationTemperatureEditText = findViewById(R.id.editTextSearchedLocationTemperature);
        searchedLocationSkyEditText = findViewById(R.id.searchedLocationSky_EditText);

        refreshButton = findViewById(R.id.refresh_button);
        searchButton = findViewById(R.id.search_button);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }

        refreshButton.setOnClickListener(view -> {
            Toast.makeText(getApplicationContext(), R.string.toast_refreshing, Toast.LENGTH_SHORT).show();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        });

        searchButton.setOnClickListener(view -> {
            StringRequest request = new StringRequest(Request.Method.GET,
                    "https://geocoding-api.open-meteo.com/v1/search?name=" + searchedLocationEditText.getText(),
                    response -> {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            ArrayList<String> resultStrings = new ArrayList<>();
                            JSONArray resultsJSONArray = jsonObject.getJSONArray("results");
                            for (int i = 0; i < resultsJSONArray.length(); i++) {
                                String str;
                                try {
                                    str = resultsJSONArray.getJSONObject(i).get("name").toString() + ", " + resultsJSONArray.getJSONObject(i).get("admin1") + ", " + resultsJSONArray.getJSONObject(i).get("country").toString();
                                } catch (JSONException e1) {
                                    try {
                                        str = resultsJSONArray.getJSONObject(i).get("name").toString() + ", " + resultsJSONArray.getJSONObject(i).get("admin2") + ", " + resultsJSONArray.getJSONObject(i).get("country").toString();
                                    } catch (JSONException e2) {
                                        try {
                                            str = resultsJSONArray.getJSONObject(i).get("name").toString() + ", " + resultsJSONArray.getJSONObject(i).get("admin3") + ", " + resultsJSONArray.getJSONObject(i).get("country").toString();
                                        } catch (JSONException e3) {
                                            try {
                                                str = resultsJSONArray.getJSONObject(i).get("name").toString() + ", " + resultsJSONArray.getJSONObject(i).get("admin4") + ", " + resultsJSONArray.getJSONObject(i).get("country").toString();
                                            } catch (JSONException e4) {
                                                str = "";
                                            }
                                        }
                                    }
                                }
                                resultStrings.add(str);
                            }

                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                            alertDialogBuilder.setTitle(R.string.selectAResult);

                            AtomicInteger selectedIndex = new AtomicInteger(-1);
                            alertDialogBuilder.setSingleChoiceItems(resultStrings.toArray(new String[resultStrings.size()]), -1, (dialog, which) -> {
                                selectedIndex.set(which);
                            });
                            alertDialogBuilder.setPositiveButton(R.string.selectButton, (dialog, which) -> {
                                String selectedLocationLatitude = null, selectedLocationLongitude = null;
                                try {
                                    selectedLocationLatitude = resultsJSONArray.getJSONObject(selectedIndex.intValue()).get("latitude").toString();
                                    selectedLocationLongitude = resultsJSONArray.getJSONObject(selectedIndex.intValue()).get("longitude").toString();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                updateSearchedLocationWeatherInfo(selectedLocationLatitude, selectedLocationLongitude);
                            });
                            alertDialogBuilder.setNegativeButton(R.string.cancelButton, null);

                            alertDialogBuilder.create().show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {}
            );
            requestQueue.add(request);
        });
    }

    public void updateSearchedLocationWeatherInfo(String lat, String lon) {
        StringRequest request = new StringRequest(Request.Method.GET,
                "https://api.open-meteo.com/v1/forecast" + "?latitude=" + lat +  "&longitude=" + lon + "&current_weather=true",
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        searchedLocationTemperatureEditText.setText(String.valueOf(jsonObject.getJSONObject("current_weather").get("temperature")));
                        int id = getResources().getIdentifier("weathercode" + String.valueOf(jsonObject.getJSONObject("current_weather").get("weathercode")), "string", getPackageName());
                        String result = getString(id);
                        searchedLocationSkyEditText.setText(result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {}
        );
        requestQueue.add(request);
    }

    public void updateCurrentLocationWeatherInfo(String lat, String lon) {
        StringRequest request = new StringRequest(Request.Method.GET,
                "https://api.open-meteo.com/v1/forecast" + "?latitude=" + lat +  "&longitude=" + lon + "&current_weather=true",
                    response -> {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            temperatureTextView.setText(String.valueOf(jsonObject.getJSONObject("current_weather").get("temperature")));
                            int id = getResources().getIdentifier("weathercode" + String.valueOf(jsonObject.getJSONObject("current_weather").get("weathercode")), "string", getPackageName());
                            String result = getString(id);
                            skyTextView.setText(result);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {}
        );
        requestQueue.add(request);
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            double lat = location.getLatitude(), lon = location.getLongitude();

            latitudeTextView.setText(String.valueOf(lat));
            longitudeTextView.setText(String.valueOf(lon));
            updateCurrentLocationWeatherInfo(String.valueOf(lat), String.valueOf(lon));

            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            try {
                currentLocationTextView.setText(geocoder.getFromLocation(lat, lon, 1).get(0).getLocality());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
                }
            } else {
                refreshButton.setEnabled(false);
                Toast.makeText(getApplicationContext(), "No pots usar la app", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else {
                refreshButton.setEnabled(false);
                Toast.makeText(getApplicationContext(), "No pots usar la app", Toast.LENGTH_SHORT).show();
            }
        }
    }

}