package com.example.weatherapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    RequestQueue requestQueue;
    private TextView currentLocationTextView;
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView temperatureTextView;
    private LocationManager locationManager;
    private Button refreshButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        temperatureTextView = findViewById(R.id.currentTemperature);
        currentLocationTextView = findViewById(R.id.editTextCurrentLocation);
        latitudeTextView = findViewById(R.id.latitude_textView);
        longitudeTextView = findViewById(R.id.longitude_textView);
        refreshButton = findViewById(R.id.refresh_button);

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
    }

    public void getTemperatureByCoordinates(String lat, String lon) {
        StringRequest request = new StringRequest(Request.Method.GET,
                "https://api.open-meteo.com/v1/forecast" + "?latitude=" + lat +  "&longitude=" + lon + "&current_weather=true",
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        temperatureTextView.setText(String.valueOf(jsonObject.getJSONObject("current_weather").get("temperature")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {

                }
        );
        requestQueue.add(request);
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            latitudeTextView.setText(String.valueOf(location.getLatitude()));
            longitudeTextView.setText(String.valueOf(location.getLongitude()));
            getTemperatureByCoordinates(latitudeTextView.getText().toString(), longitudeTextView.getText().toString());
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
        } else if (requestCode == 1){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else {
                refreshButton.setEnabled(false);
                Toast.makeText(getApplicationContext(), "No pots usar la app", Toast.LENGTH_SHORT).show();
            }
        }
    }

}