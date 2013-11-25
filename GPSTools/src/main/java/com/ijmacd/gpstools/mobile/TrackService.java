package com.ijmacd.gpstools.mobile;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
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
import com.ijmacd.gpstools.mobile.R;

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


    @Override
    public void onCreate() {
        mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        final String updateKey = getResources().getString(R.string.pref_display_freq_key);
        mUpdateInterval = Integer.parseInt(preferences.getString(updateKey, MIN_TIME+""));

        Log.d(LOG_TAG, "onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "Received start id " + startId + ": " + intent);

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
        // Cancel the persistent notification.
        mNotificationManager.cancel(NOTIFICATION);

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
            return TrackService.this;
        }
        Track getTrack(){
            return mTrack;
        }
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_gps, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, DashboardActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.local_service_label),
                text, contentIntent);

        // Send the notification.
        mNotificationManager.notify(NOTIFICATION, notification);
    }

    private void hideNotification(){
        mNotificationManager.cancel(NOTIFICATION);
    }

    public Track startLogging(){
        Log.d(LOG_TAG, "startLogging()");

        if(mTrack != null){
            mTrack.save();
        }

        mTrack = new Track(this);

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
            mTrack.save();

    }

    private LocationListener mLocationListener = new LocationListener() {

        private int lastStatus;
        private boolean showingDebugToast;

        public void onLocationChanged(Location loc) {
            if (loc != null) {
                mTrack.addPoint(new Point(loc));
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
}