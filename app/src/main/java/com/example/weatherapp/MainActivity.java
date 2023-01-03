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
import android.widget.ImageView;
import android.widget.TextView;

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
    private TextView currentTemperatureTextView;
    private TextView currentSkyTextView;
    private ImageView currentSkyImageView;

    private EditText searchedLocationEditText;
    private TextView searchedTemperatureTextView;
    private TextView searchedSkyTextView;
    private ImageView searchedSkyImageView;

    private Button searchButton;

    private LocationManager locationManager;

    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        currentLocationTextView = findViewById(R.id.currentLocation_textView);
        currentTemperatureTextView = findViewById(R.id.currentTemperature_textView);
        currentSkyTextView = findViewById(R.id.currentSky_textView);
        currentSkyImageView = findViewById(R.id.currentSky_imageView);
        currentSkyImageView.setImageResource(android.R.color.transparent);

        searchedLocationEditText = findViewById(R.id.searchedLocation_editText);
        searchedTemperatureTextView = findViewById(R.id.searchedTemperature_textView);
        searchedSkyTextView = findViewById(R.id.searchedSky_textView);
        searchedSkyImageView = findViewById(R.id.searchedSky_ImageView);
        searchedSkyImageView.setImageResource(android.R.color.transparent);

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
                                    str = resultsJSONArray.getJSONObject(i).get("name") + ", " + resultsJSONArray.getJSONObject(i).get("admin1") + ", " + resultsJSONArray.getJSONObject(i).get("country");
                                } catch (JSONException e1) {
                                    try {
                                        str = resultsJSONArray.getJSONObject(i).get("name") + ", " + resultsJSONArray.getJSONObject(i).get("admin2") + ", " + resultsJSONArray.getJSONObject(i).get("country");
                                    } catch (JSONException e2) {
                                        try {
                                            str = resultsJSONArray.getJSONObject(i).get("name") + ", " + resultsJSONArray.getJSONObject(i).get("admin3") + ", " + resultsJSONArray.getJSONObject(i).get("country");
                                        } catch (JSONException e3) {
                                            try {
                                                str = resultsJSONArray.getJSONObject(i).get("name") + ", " + resultsJSONArray.getJSONObject(i).get("admin4") + ", " + resultsJSONArray.getJSONObject(i).get("country");
                                            } catch (JSONException e4) {
                                                str = resultsJSONArray.getJSONObject(i).get("name") + ", " + resultsJSONArray.getJSONObject(i).get("country");
                                            }
                                        }
                                    }
                                }
                                resultStrings.add(str);
                            }

                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                            alertDialogBuilder.setTitle(R.string.selectAResult);

                            AtomicInteger selectedIndex = new AtomicInteger(-1);
                            alertDialogBuilder.setSingleChoiceItems(resultStrings.toArray(new String[0]), -1, (dialog, which) -> selectedIndex.set(which));
                            alertDialogBuilder.setPositiveButton(R.string.selectButton, (dialog, which) -> {
                                String selectedLocationLatitude = null, selectedLocationLongitude = null;
                                try {
                                    selectedLocationLatitude = resultsJSONArray.getJSONObject(selectedIndex.intValue()).get("latitude").toString();
                                    selectedLocationLongitude = resultsJSONArray.getJSONObject(selectedIndex.intValue()).get("longitude").toString();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                updateWeatherInfo(selectedLocationLatitude, selectedLocationLongitude, searchedTemperatureTextView, searchedSkyTextView, searchedSkyImageView);
                            });
                            alertDialogBuilder.setNegativeButton(R.string.cancelButton, null);

                            alertDialogBuilder.create().show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    },
                    Throwable::printStackTrace
            );
            requestQueue.add(request);
        });
    }

    public void updateWeatherInfo(String lat, String lon, TextView temperatureTextView, TextView skyTextView, ImageView skyImageView) {
        StringRequest request = new StringRequest(Request.Method.GET,
                "https://api.open-meteo.com/v1/forecast?latitude=" + lat +  "&longitude=" + lon + "&current_weather=true",
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        String temperature = jsonObject.getJSONObject("current_weather").get("temperature") +"º";
                        temperatureTextView.setText(temperature);

                        int weatherCode = (int) jsonObject.getJSONObject("current_weather").get("weathercode");
                        int id = getResources().getIdentifier("weathercode" + weatherCode, "string", getPackageName());
                        String result = getString(id);
                        skyTextView.setText(result);
                        if (0 <= weatherCode && weatherCode <= 2)
                            skyImageView.setImageResource(R.drawable.sun_moon);
                        if (3 <= weatherCode && weatherCode <= 48)
                            skyImageView.setImageResource(R.drawable.cloud);
                        if ((51 <= weatherCode && weatherCode <= 67) || (80 <= weatherCode && weatherCode <= 82) || (95 <= weatherCode && weatherCode <= 99))
                            skyImageView.setImageResource(R.drawable.rain);
                        if ((71 <= weatherCode && weatherCode <= 77) || (85 <= weatherCode && weatherCode <= 86))
                            skyImageView.setImageResource(R.drawable.snow);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                Throwable::printStackTrace
        );
        requestQueue.add(request);
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            double lat = location.getLatitude(), lon = location.getLongitude();

            updateWeatherInfo(String.valueOf(lat), String.valueOf(lon), currentTemperatureTextView, currentSkyTextView, currentSkyImageView);

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
                currentLocationTextView.setText(R.string.no_location);
            }
        } else if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else {
                searchedLocationEditText.setEnabled(false);
                searchButton.setEnabled(false);
            }
        }
    }

}