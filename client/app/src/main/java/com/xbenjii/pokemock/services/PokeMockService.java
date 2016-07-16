package com.xbenjii.pokemock.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.xbenjii.pokemock.MainActivity;
import com.xbenjii.pokemock.R;
import com.xbenjii.pokemock.utils.LocationUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by billy on 7/15/16.
 */
public class PokeMockService extends Service {

    private String mockLocationProvider;
    private LocationManager mLocationManager;

    private static final String TAG = "PokeMockService";

    public static String WEBSOCKET_SERVER = "address";

    private String address;

    private PowerManager.WakeLock wl;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        address = intent.getStringExtra(WEBSOCKET_SERVER);
        startWebSocketProcess(address);
        return new LocalBinder<PokeMockService>(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            mockLocationProvider = LocationManager.GPS_PROVIDER;

            mLocationManager.addTestProvider(mockLocationProvider, true, true, true, false, true,
                    true, true, 0, 5);
            mLocationManager.setTestProviderEnabled(mockLocationProvider, true);
            mLocationManager.setTestProviderStatus(mockLocationProvider, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }


        startForeground(1337, generateNotification("PokeMock Started ....."));

        //wakelock screen
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        wl.acquire();

    }

    private Notification generateNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Poke Mock")
                .setContentIntent(pendingIntent)
                .setContentText(text).build();

        return notification;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            this.mLocationManager.removeTestProvider(mockLocationProvider);
            stopForeground(true);
            wl.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_NOT_STICKY;
    }


    private void updateNotification(String lat, String lon) {

        String text = "Current Location : " + lat + "," + lon;

        Notification notification = generateNotification(text);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1337, notification);
    }


    private void startWebSocketProcess(final String address) {
        try {
            new WebSocketFactory()
                    .setConnectionTimeout(5000)
                    .createSocket(address)
                    .addListener(new WebSocketAdapter() {
                        @Override
                        public void onTextMessage(WebSocket websocket, String message) {
                            Log.v(TAG, "Message received:" + message);
                            String[] parts = message.split(":");
                            final String latitude = parts[0];
                            final String longitude = parts[1];
                            final String altitude = parts[2];
                            Thread t = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Location mockLocation = new Location(mockLocationProvider);
                                    mockLocation.setLatitude(Double.parseDouble(latitude));
                                    mockLocation.setLongitude(Double.parseDouble(longitude));
                                    mockLocation.setAltitude(Double.parseDouble(altitude));
                                    mockLocation.setTime(System.currentTimeMillis());
                                    mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                                    mockLocation.setAccuracy(50);
                                        /* every time you mock location, you should use these code */
                                    int value = LocationUtils.setMockLocationSettings(PokeMockService.this);//toggle ALLOW_MOCK_LOCATION on
                                    try {
                                        mLocationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation);
                                        updateNotification(String.valueOf(latitude), String.valueOf(longitude));
                                    } catch (SecurityException e) {
                                        e.printStackTrace();
                                    } finally {
                                        LocationUtils.restoreMockLocationSettings(PokeMockService.this, value);//toggle ALLOW_MOCK_LOCATION off
                                    }

                                }
                            });
                            t.start();
                        }

                        @Override
                        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
                            Log.v(TAG, "Connected to server: " + address);
                            if (ActivityCompat.checkSelfPermission(PokeMockService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(PokeMockService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
                            }
                            Location currentLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            websocket.sendText(String.valueOf(currentLocation.getLatitude()) + ":" + String.valueOf(currentLocation.getLongitude()) + ":" + String.valueOf(currentLocation.getAltitude()));

                        }

                        @Override
                        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
                            Log.e(TAG, "Error Connected to Server : " + exception.getMessage());
                        }
                    })
                    .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                    .connect();
        } catch (IOException ex) {
            Log.e(TAG, "IO Exception " + ex.getMessage());
        } catch (WebSocketException ex) {
            Log.e(TAG, "WS Exception " + ex.getMessage());
        }
    }
}
