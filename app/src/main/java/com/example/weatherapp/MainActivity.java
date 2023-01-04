package com.example.weatherapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
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

    public TextView environmentLuminosityTextView;
    public TextView environmentTemperatureTextView;

    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;

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

        environmentLuminosityTextView = findViewById(R.id.environmentLuminosity_textView);
        environmentTemperatureTextView = findViewById(R.id.environmentTemperature_textView);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
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
                    error -> Toast.makeText(getApplicationContext(), R.string.connection_error, Toast.LENGTH_SHORT).show()
            );
            requestQueue.add(request);
        });

        this.setUpBluetooth();
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
                error -> Toast.makeText(getApplicationContext(), R.string.connection_error, Toast.LENGTH_SHORT).show()
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
        @Override
        public void onProviderDisabled(@NonNull String provider) {
            currentLocationTextView.setText(R.string.no_location);
            currentTemperatureTextView.setText("");
            currentSkyTextView.setText("");
            currentSkyImageView.setImageResource(android.R.color.transparent);
        }
        @Override
        public void onProviderEnabled(@NonNull String provider) {
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
                }
            } else {
                currentLocationTextView.setText(R.string.no_location);
            }
        } else if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
            } else {
                searchedLocationEditText.setEnabled(false);
                searchButton.setEnabled(false);
            }
        }
    }


    public void setUpBluetooth() {
        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            environmentLuminosityTextView.setText(R.string.no_bluetooth);
            return;
        }

        ActivityResultLauncher<Intent> bluetoothResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        connectDevice();
                    } else {
                        environmentLuminosityTextView.setText(R.string.no_bluetooth);
                    }
                });

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothResultLauncher.launch(enableBtIntent);
        } else {
            connectDevice();
        }
    }

    @SuppressLint("MissingPermission")
    public void connectDevice() {
        String HMSOFT_MAC_ADDRESS = "00:0E:EA:CF:61:5A";

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (deviceHardwareAddress.equalsIgnoreCase(HMSOFT_MAC_ADDRESS)) {
                    ConnectThread c = new ConnectThread(device);
                    c.start();
                    return;
                }
            }
        }
        environmentLuminosityTextView.setText(R.string.no_bluetooth_device);
    }

    @SuppressLint("MissingPermission")
    public class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            ParcelUuid[] uuids = device.getUuids();
            try {
                tmp = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run() {
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            TransferThread t = new TransferThread(mmSocket);
            t.start();
        }
    }

    public class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String bothValues = new String((byte[]) msg.obj, 0, ((byte[]) msg.obj).length);
            String luminosity = bothValues.split(";")[0] + " lux";
            environmentLuminosityTextView.setText(luminosity);
            String temperature = bothValues.split(";")[1] + "º";
            environmentTemperatureTextView.setText(temperature);
        }
    }

    public class TransferThread extends Thread {
        private final InputStream mmInStream;

        public TransferThread(BluetoothSocket socket) {
            InputStream tmpIn = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
        }

        public void run() {
            Handler myHandler = new MyHandler();
            byte[] mmBuffer = new byte[20];
            int numBytes = 0;

            while (true) {
                try {
                    while (mmInStream.available() != 20) {}
                    numBytes = mmInStream.read(mmBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Message readMsg = myHandler.obtainMessage(0, numBytes, -1, mmBuffer);
                readMsg.sendToTarget();
            }
        }
    }
}