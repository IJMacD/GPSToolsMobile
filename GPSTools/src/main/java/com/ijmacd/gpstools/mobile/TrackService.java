package com.ijmacd.gpstools.mobile;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.ijmacd.gpstools.mobile.R;
import com.movisens.smartgattlib.Characteristic;

import java.util.List;

public class TrackService extends Service {
    private static final String LOG_TAG = "TrackService";
    private LocationManager mLocationManager;
    private NotificationManager mNotificationManager;

    private int mUpdateInterval;

    private Track mTrack;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.local_service_started;

    private static final int MIN_ACCURACY = 30;
    private static final int MIN_TIME = 1000;
    private static final float MIN_DISTANCE = 0;

    private boolean mRecording;

    final private TrackService self = this;

    private double mLastSensorSpeed;
    private double mLastCadence;


    @Override
    public void onCreate() {
        mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        final String updateKey = getResources().getString(R.string.pref_display_freq_key);
        mUpdateInterval = Integer.parseInt(preferences.getString(updateKey, MIN_TIME+""));

        Log.d(LOG_TAG, "onCreate()");


        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "Received start id " + startId +
                ((flags & START_FLAG_REDELIVERY) > 0 ? " (REDELIVERY)" : "") +
                ((flags & START_FLAG_RETRY) > 0 ? " (RETRY)" : "") +
                ": " + intent);

        // I think we're recovering
        // Check the database for unfinished tracks
        if(intent == null){
            Track track = Track.getLatestTrack(this);
            if(track != null && !track.isComplete()){
                Log.i(LOG_TAG, "Recovered incomplete track");
                startLogging(track);
            }
        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
        // Cancel the persistent notification.
        mNotificationManager.cancel(NOTIFICATION);

        unregisterReceiver(mGattUpdateReceiver);

        stopLogging();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public Track getTrack(){
        return mTrack;
    }

    public boolean isRecording() {
        return mRecording;
    }


    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        TrackService getService() {
            return self;
        }
        Track getTrack(){
            return mTrack;
        }
        boolean isRecording(){
            return mRecording;
        }
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {

        Intent intent = new Intent(this, DashboardActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(DashboardActivity.class);
        stackBuilder.addNextIntent(intent);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(R.drawable.ic_gps);
        builder.setContentText(getText(R.string.local_service_started));
        builder.setContentTitle(getText(R.string.local_service_label));
        builder.setOngoing(true);
        builder.setContentIntent(stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));

        final Notification notification = builder.build();

        // Send the notification.
        mNotificationManager.notify(NOTIFICATION, notification);
    }

    private void hideNotification(){
        mNotificationManager.cancel(NOTIFICATION);
    }

    public Track startLogging(){
        return startLogging(new Track(this));
    }

    private Track startLogging(Track track) {

        Log.d(LOG_TAG, "startLogging()");

        if(mTrack != null){
            mTrack.save();
        }

        mTrack = track;

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                MIN_TIME, MIN_DISTANCE,
                mLocationListener);

        mRecording = true;

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();

        return mTrack;
    }

    public void stopLogging(){

        Log.d(LOG_TAG, "stopLogging()");

        hideNotification();

        mRecording = false;

        mLocationManager.removeUpdates(mLocationListener);

        if(mTrack != null)
            mTrack.close();

    }

    private LocationListener mLocationListener = new LocationListener() {

        private int lastStatus;
        private boolean showingDebugToast;

        public void onLocationChanged(Location loc) {
            if (loc != null) {
                Point p = new Point(loc);
                p.setSensorSpeed(mLastSensorSpeed);
                p.setCadence(mLastCadence);
                mTrack.addPoint(p);
            }
        }

        public void onProviderDisabled(String provider) {

        }

        public void onProviderEnabled(String provider) {

        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            String showStatus = null;
            if (status == LocationProvider.AVAILABLE)
                showStatus = "Available";
            if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
                showStatus = "Temporarily Unavailable";
            if (status == LocationProvider.OUT_OF_SERVICE)
                showStatus = "Out of Service";
            if (status != lastStatus && showingDebugToast) {
                Toast.makeText(getBaseContext(),
                        "new status: " + showStatus,
                        Toast.LENGTH_SHORT).show();
            }
            lastStatus = status;
        }

    };
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
//                mConnected = true;
//                updateConnectionState(R.string.connected);
//                invalidateOptionsMenu();
                Log.d(LOG_TAG, "BLE Connected");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
//                mConnected = false;
//                updateConnectionState(R.string.disconnected);
//                invalidateOptionsMenu();
//                clearUI();
                Log.d(LOG_TAG, "BLE Disconnected");
                mLastSensorSpeed = 0;
                mLastCadence = 0;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                Log.d(LOG_TAG, "BLE Discovered services");

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
//                Log.d(LOG_TAG, "BLE Data: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                mLastSensorSpeed = intent.getDoubleExtra(BluetoothLeService.EXTRA_SPEED, 0);
                mLastCadence = intent.getDoubleExtra(BluetoothLeService.EXTRA_CADENCE, 0);
                Log.d(LOG_TAG, "Speed: " + mLastSensorSpeed + " Cadence: " + mLastCadence);
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
