package com.gae.scaffolder.plugin;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CheckLocationService extends Service {
    private static final String TAG = "FCMPlugin-CLS";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;
    private int TIMESTAMP_MAX_DIFF_MS = 10 * 60 * 1000;
    private double DISTANCE_MAX_KM = 5.0;
    private Intent receivedIntent = null;

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {

            String sentLocation = null;
            Long sentTimestamp = null;

            Map<String, Object> data = new HashMap<String, Object>();

            for (String key : receivedIntent.getExtras().keySet()) {
                Object value = receivedIntent.getExtras().get(key);
                Log.d(TAG, String.format("%s %s (%s)", key,
                        value.toString(), value.getClass().getName()));
                data.put(key, value);

                if (key.equals("coords")) {
                    sentLocation = (String) value;
                } else if (key.equals("timestamp_sent")) {
                    sentTimestamp = Long.parseLong((String) value);
                }
            }

            // Log.d(TAG,.getExtras().get("data").toString());
            Log.e(TAG, "onLocationChanged: " + location);

            if (sentLocation != null && sentTimestamp != null) {
                try {
                    JSONObject jsonObject = new JSONObject(sentLocation);
                    double distance = getDistanceFromLatLonInKm(location.getLatitude(), location.getLongitude(),
                            Double.parseDouble(jsonObject.getString("latitude")), Double.parseDouble(jsonObject.getString("longitude")));

                    if (System.currentTimeMillis() - sentTimestamp < TIMESTAMP_MAX_DIFF_MS) {
                        if (distance <= DISTANCE_MAX_KM) {




                            sendNotification("Notfall in deiner Nähe", "öffne die App um Genaueres zu erfahren", data);
                        } else {
                            Log.d(TAG, "outside of range (> 5km)");
                        }
                    } else {
                        Log.d(TAG, "older than: " + (TIMESTAMP_MAX_DIFF_MS / 1000) + " seconds");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                Log.d(TAG, "sentLocation or sentTimestamp was null - sentLocation: " + sentLocation + "  sentTimestamp: " + sentTimestamp);
            }
            mLastLocation.set(location);

      /*
      sendNotification(
        "It's working",
        "Latitude: " + location.getLatitude() + " Longitude: " + location.getLongitude(),
        new HashMap()
      );
      */
            stopSelf();
        }

        private double getDistanceFromLatLonInKm(double lat1, double lon1, double lat2, double lon2) {
            double R = 6371; // Radius of the earth in km
            double dLat = this.deg2rad(lat2 - lat1);  // deg2rad below
            double dLon = this.deg2rad(lon2 - lon1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(this.deg2rad(lat1)) * Math.cos(this.deg2rad(lat2)) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c; // Distance in km
        }

        private double deg2rad(double deg) {
            return deg * (Math.PI / 180);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        Log.e(TAG, intent == null ? "null" : "not null");
        receivedIntent = intent;
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        initializeLocationManager();

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void sendNotification(String title, String messageBody, Map<String, Object> data) {

        Intent launchIntent = getActivity.getPackageManager().getLaunchIntentForPackage("com.cardiaccustodian.prototype");
        startActivity(launchIntent);

        Intent intent = new Intent(this, FCMPluginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        for (String key : data.keySet()) {
            intent.putExtra(key, data.get(key).toString());
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(getApplicationInfo().icon)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
