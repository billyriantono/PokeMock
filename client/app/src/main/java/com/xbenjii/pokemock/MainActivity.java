package com.xbenjii.pokemock;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.xbenjii.pokemock.services.LocalBinder;
import com.xbenjii.pokemock.services.PokeMockService;

import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";
    private LocationManager mLocationManager;
    private TextView textView;
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;
    private static final int MY_PERMISSION_ACCESS_INSTALL_PROVIDER = 13;
    private LocationListener locationListener;

    private boolean mBounded;
    private PokeMockService mService;


    private final ServiceConnection mConnection = new ServiceConnection() {

        @SuppressWarnings("unchecked")
        @Override
        public void onServiceConnected(final ComponentName name,
                                       final IBinder service) {
            mService = ((LocalBinder<PokeMockService>) service).getService();
            mBounded = true;
            Log.d(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mService = null;
            mBounded = false;
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        final Button button = (Button) findViewById(R.id.button);
        final EditText editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);

        textView.setFocusable(false);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String ipAddress = editText.getText().toString();
                if(!isIP(ipAddress)) {
                    textView.append("Not a valid IP address: " + ipAddress + "\n");
                    return;
                }
                connectToWebSocketService("ws://" + ipAddress);
            }
        });


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INSTALL_LOCATION_PROVIDER) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.INSTALL_LOCATION_PROVIDER
            }, MY_PERMISSION_ACCESS_INSTALL_PROVIDER);
        } else {

        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_SECURE_SETTINGS
            }, MY_PERMISSION_ACCESS_INSTALL_PROVIDER);
        } else {

        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_SETTINGS
            }, MY_PERMISSION_ACCESS_INSTALL_PROVIDER);
        } else {

        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, MY_PERMISSION_ACCESS_COARSE_LOCATION);
        } else {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, MY_PERMISSION_ACCESS_FINE_LOCATION);
        } else {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                } else {
                    // permission denied
                }
                break;
            }
            case MY_PERMISSION_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                } else {
                    // permission denied
                }
                break;
            }
            case MY_PERMISSION_ACCESS_INSTALL_PROVIDER: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    // permission denied
                }
                break;
            }
        }
    }

    private static final Pattern PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5]):(\\d+?)$"
    );

    private static boolean isIP(String ipAddress) {
        return PATTERN.matcher(ipAddress).matches();
    }

    private void connectToWebSocket(final String address) {
        textView.append("Connecting to " + address);
        Intent intent = new Intent("address");
        intent.putExtra("address", address);
        sendBroadcast(intent);
    }

    private void connectToWebSocketService(final String address) {
        textView.append("Connecting to " + address);
        doBindService(address);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //doUnbindService();
    }

    void doBindService(String address) {
        Log.d(TAG, "bind");
        Intent intent = new Intent(this, PokeMockService.class);
        intent.putExtra(PokeMockService.WEBSOCKET_SERVER, address);
        bindService(intent, mConnection,
                Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        if (mConnection != null) {
            try {
                unbindService(mConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
