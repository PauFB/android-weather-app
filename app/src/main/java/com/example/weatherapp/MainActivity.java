package com.example.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelUuid;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
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

    public TextView luxBluetoothTextView;
    public TextView temperatureBluetoothTextView;

    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    private IntentFilter filter;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getAddress().equalsIgnoreCase(HMSOFT_MAC_ADDRESS)) {
                    bluetoothAdapter.cancelDiscovery();
                    ConnectThread c = new ConnectThread(device);
                    c.start();
                }
            }
        }
    };

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

        luxBluetoothTextView = findViewById(R.id.lux_TextView);
        temperatureBluetoothTextView = findViewById(R.id.temperature_TextView);

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
                                                str = resultsJSONArray.getJSONObject(i).get("name").toString() + ", " + resultsJSONArray.getJSONObject(i).get("country").toString();
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
                    error -> {
                    }
            );
            requestQueue.add(request);
        });

        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        this.setUpBluetooth();
    }

    public void updateSearchedLocationWeatherInfo(String lat, String lon) {
        StringRequest request = new StringRequest(Request.Method.GET,
                "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true",
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
                error -> {
                }
        );
        requestQueue.add(request);
    }

    public void updateCurrentLocationWeatherInfo(String lat, String lon) {
        StringRequest request = new StringRequest(Request.Method.GET,
                "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true",
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
                error -> {
                }
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
                Toast.makeText(getApplicationContext(), R.string.internetUnavailable, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else {
                refreshButton.setEnabled(false);
                Toast.makeText(getApplicationContext(), R.string.gpsUnavailable, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String bothValues = new String((byte[]) msg.obj, 0, ((byte[]) msg.obj).length);
            luxBluetoothTextView.setText(bothValues.split(";")[0]);
            temperatureBluetoothTextView.setText(bothValues.split(";")[1]);
        }
    }

    private Handler myHandler = new MyHandler();

    private final static int REQUEST_ENABLE_BT = 2;
    private final String HMSOFT_MAC_ADDRESS = "00:0E:EA:CF:61:5A";

    public void setUpBluetooth() {
        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), R.string.bluetoothUnavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (deviceHardwareAddress.equalsIgnoreCase(HMSOFT_MAC_ADDRESS)) {
                    ConnectThread c = new ConnectThread(device);
                    c.start();
                    break;
                }
            }
        } else {
            //bluetoothAdapter.startDiscovery();
            bluetoothAdapter.getBluetoothLeScanner().startScan(new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    //List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();
                    //hmSoftUuid = uuids.get(0);
                    hmSoftUuid = UUID.nameUUIDFromBytes(result.getScanRecord().getBytes());
                }
            });
        }

    }

    UUID hmSoftUuid;

    public class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            @SuppressLint("MissingPermission") ParcelUuid[] uuids = device.getUuids();
            try {
                tmp = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                //tmp = device.createRfcommSocketToServiceRecord(hmSoftUuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        @SuppressLint("MissingPermission")
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

    public class TransferThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;

        public TransferThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[20];
            int numBytes = 0; // bytes returned from read()

            while (true) {
                try {
                    while (mmInStream.available() != 20) {}
                    numBytes = mmInStream.read(mmBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Message readMsg = myHandler.obtainMessage(
                        0, numBytes, -1,
                        mmBuffer);
                readMsg.sendToTarget();
            }
        }
    }

}
