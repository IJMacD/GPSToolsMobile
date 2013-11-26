package com.ijmacd.gpstools.mobile;

import android.content.*;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
import android.location.*;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DashboardWidget extends FrameLayout
 implements SharedPreferences.OnSharedPreferenceChangeListener {

	// Stateless - GPS		0 - 31
	// Static
	public static final int WIDGET_GPSDATE = 1;
	public static final int WIDGET_GPSTIME = 2;
	public static final int WIDGET_GPSDATETIME = 3;
	public static final int WIDGET_LATITUDE = 4;
	public static final int WIDGET_LONGITUDE = 5;
	public static final int WIDGET_LATLON = 6;
	public static final int WIDGET_ALTITUDE = 7;
	public static final int WIDGET_ACCURACY = 8;
	public static final int WIDGET_SATELLITES = 9;
	public static final int WIDGET_MAP = 10;
	// Moving
	public static final int WIDGET_SPEED = 16;
	public static final int WIDGET_VAM = 17;
	public static final int WIDGET_HEADING = 18;
	
	// Stateless - Non-GPS	32 - 63
    public static final int WIDGET_DATE = 32;
    public static final int WIDGET_TIME = 33;
    public static final int WIDGET_DATETIME = 34;
	public static final int WIDGET_BATTERY_LEVEL = 35;
	public static final int WIDGET_BATTERY_TEMP = 36;
	public static final int WIDGET_BATTERY_VOLT = 37;
	
	// Stateful - Track		64 - 95
	public static final int WIDGET_DISTANCE = 64;
	public static final int WIDGET_DURATION = 65;
	public static final int WIDGET_SPEED_AVERAGE = 66;
	public static final int WIDGET_SPEED_MAX = 67;
	public static final int WIDGET_HEIGHT_GAIN = 68;
	public static final int WIDGET_ALTITUDE_MIN = 69;
	public static final int WIDGET_ALTITUDE_MAX = 70;
	
	// Route	96 - 127
	public static final int WIDGET_ROUTE_OFFSET = 96;	// Distance from closest point on route
	public static final int WIDGET_ROUTE_DISTANCE = 97;	// Remaining
	public static final int WIDGET_ROUTE_HEIGHT_GAIN = 98;	// Remaining
	public static final int WIDGET_ROUTE_DURATION = 99;	// Remaining (Estimation)
	public static final int WIDGET_ROUTE_ETA = 100;	// (Estimation)
    public static final int WIDGET_ROUTE_DISPLACEMENT = 101; // Absolute distance to destination
	public static final int WIDGET_ROUTE_TRUE_SPEED = 102;	// Speed towards destination
	
	// Point	128 - 159
	public static final int WIDGET_BEARING = 128;
	public static final int WIDGET_POINT_DISTANCE = 129;
	public static final int WIDGET_POINT_TRUE_SPEED = 130;

    public static final int UNITS_DEFAULT = 0;
    public static final int UNITS_METRIC = 1;
    public static final int UNITS_IMPERIAL = 2;
    public static final int UNITS_SI = 3;
    public static final int UNITS_NAUTICAL = 4;

    private static final int DATE_UPDATE_INTERVAL = 200;
    private static final int GPS_UPDATE_INTERVAL = 1000;

    private int mWidgetType;
    private int mUnitsType;
    private boolean mDefaultUnits = true;

	private TextView mValueText;
	private TextView mUnitsText;

	private double mValue;
    private String mValueFormat;

    private int mUpdateInterval;

    private Context mContext;

    private BroadcastReceiver mReceiver;
    private IntentFilter mBatteryFilter;

    private LocationManager mLocationManager;

    private LocationListener mLocationListener;

    private GpsStatus.Listener mGpsStatusListener;

    private SharedPreferences mPreferences;

    private Handler mHandler;
    private Runnable mRunnable;

    private Track mTrack;
    private Route mRoute;
    private Point mPoint;

    private static final boolean DEBUG = true;

    public DashboardWidget(Context context, AttributeSet attrs) {
		super(context, attrs);

        mContext = context;
		
		TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DashboardWidget, 0, 0);
		try {
            int widgetType = attributes.getInteger(R.styleable.DashboardWidget_widgetType, 0);
            int unitsType = attributes.getInteger(R.styleable.DashboardWidget_units, UNITS_DEFAULT);
            init(widgetType, unitsType);
		}
		finally {
			attributes.recycle();
		}

	}

    public DashboardWidget(Context context, int widgetType) {
        super(context);
        mContext = context;
        init(widgetType, UNITS_DEFAULT);
    }

    public DashboardWidget(Context context, int widgetType, int unitsType) {
        super(context);
        mContext = context;
        init(widgetType, unitsType);
    }

    private void init(int widgetType, int unitsType){
        mWidgetType = widgetType;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_dashboard_item, this);

        setBackgroundResource(R.drawable.widget_background);

        mValueText = (TextView)findViewById(R.id.value_text);

        mUnitsText = (TextView)findViewById(R.id.unit_text);

        ImageView categoryImage = (ImageView)findViewById(R.id.category_image);
        if(mWidgetType < 32){
            // Category GPS
            categoryImage.setImageResource(R.drawable.category_gps);
        }
        else if (mWidgetType < 64){
            // Category System
            if(mWidgetType < 35){
                // System Date
            }
            else if(mWidgetType < 38){
                categoryImage.setImageResource(R.drawable.category_battery);
            }
        }
        else if(mWidgetType < 96){
            // Category Track
            categoryImage.setImageResource(R.drawable.category_track);
        }
        else if(mWidgetType < 128){
            // Category Route
            categoryImage.setImageResource(R.drawable.category_route);
        }
        else if(mWidgetType < 160){
            // Category Point
            categoryImage.setImageResource(R.drawable.category_point);
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        mPreferences.registerOnSharedPreferenceChangeListener(this);

        resetUpdateInterval();

        switch (mWidgetType){
            case WIDGET_ACCURACY:
            case WIDGET_ALTITUDE:
            case WIDGET_HEADING:
            case WIDGET_LATITUDE:
            case WIDGET_LONGITUDE:
            case WIDGET_SPEED:
            case WIDGET_GPSDATE:
            case WIDGET_GPSTIME:
                mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                mLocationListener = new LocationListener() {
//                    private long mLastFix;
                    @Override
                    public void onLocationChanged(Location location) {
//                        should not be here, should be in gpsstatus.listener or in a timer
//                        final long currentTime = SystemClock.elapsedRealtime();
//                        setStale(mLastFix > 0 && (currentTime - mLastFix) > GPS_UPDATE_INTERVAL * 2);
//                        mLastFix = currentTime;
                        Date fixDate;
                        SimpleDateFormat dateFormatter;
                        switch (mWidgetType){
                            case WIDGET_SPEED:
                                setValue(location.getSpeed());
                                break;
                            case WIDGET_ACCURACY:
                                setValue(location.getAccuracy());
                                break;
                            case WIDGET_ALTITUDE:
                                setValue(location.getAltitude());
                                break;
                            case WIDGET_HEADING:
                                setValue(location.getBearing());
                                break;
                            case WIDGET_LATITUDE:
                                setValue(location.getLatitude());
                                break;
                            case WIDGET_LONGITUDE:
                                setValue(location.getLongitude());
                                break;
                            case WIDGET_GPSTIME:
                                fixDate = new Date(location.getTime());
                                dateFormatter = new SimpleDateFormat("HH:mm:ss");
                                mValueText.setText(dateFormatter.format(fixDate));
                                break;
                            case WIDGET_GPSDATE:
                                fixDate = new Date(location.getTime());
                                dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
                                mValueText.setText(dateFormatter.format(fixDate));
                                break;
                        }
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
                break;
            case WIDGET_BATTERY_LEVEL:
            case WIDGET_BATTERY_TEMP:
            case WIDGET_BATTERY_VOLT:
                mBatteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                mReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if(intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)){
                            float value = 0;
                            if(mWidgetType == WIDGET_BATTERY_LEVEL)
                                value = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                            else if(mWidgetType == WIDGET_BATTERY_TEMP)
                                value = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
                            else if(mWidgetType == WIDGET_BATTERY_VOLT)
                                value = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f;
                            setValue(value);
                        }
                    }
                };
                break;
            case WIDGET_DATE:
            case WIDGET_TIME:
                mHandler = new Handler();
                mRunnable = new Runnable() {
                    @Override
                    public void run() {
                        final Date now = new Date();
                        SimpleDateFormat dateFormatter = null;
                        switch (mWidgetType){
                            case WIDGET_DATE:
                                dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
                                break;
                            case WIDGET_TIME:
                                dateFormatter = new SimpleDateFormat("HH:mm:ss");
                        }
                        mValueText.setText(dateFormatter.format(now));
                        mHandler.postDelayed(this, DATE_UPDATE_INTERVAL);
                    }
                };
                break;
            case WIDGET_SATELLITES:
                mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                mGpsStatusListener = new GpsStatus.Listener() {
                    @Override
                    public void onGpsStatusChanged(int event) {
                        if(event == GpsStatus.GPS_EVENT_SATELLITE_STATUS){
                            GpsStatus status = mLocationManager.getGpsStatus(null);
                            int count = 0;
                            for(GpsSatellite sat : status.getSatellites()){
                                count += sat.usedInFix() ? 1 : 0;
                            }
                            setValue(count);
                        }
                    }
                };
                break;
        }

        setValueFormat(widgetType);
        setUnits(unitsType);

        onResume();
    }

    public void destroy(){
        onPause();
        onDestroy();
    }

    public void onDestroy() {
        mContext = null;
        mReceiver = null;
    }

    public void onPause(){
        switch (mWidgetType) {
            case WIDGET_BATTERY_LEVEL:
            case WIDGET_BATTERY_TEMP:
            case WIDGET_BATTERY_VOLT:
                mContext.unregisterReceiver(mReceiver);
                break;
            case WIDGET_DATE:
            case WIDGET_TIME:
                mHandler.removeCallbacks(mRunnable);
                break;
            case WIDGET_ACCURACY:
            case WIDGET_ALTITUDE:
            case WIDGET_HEADING:
            case WIDGET_LATITUDE:
            case WIDGET_LONGITUDE:
            case WIDGET_SPEED:
            case WIDGET_GPSDATE:
            case WIDGET_GPSTIME:
                mLocationManager.removeUpdates(mLocationListener);
                break;
            case WIDGET_SATELLITES:
                mLocationManager.removeGpsStatusListener(mGpsStatusListener);
                break;
        }


        mPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onResume() {

        switch(mWidgetType)  {
            case WIDGET_BATTERY_LEVEL:
            case WIDGET_BATTERY_TEMP:
            case WIDGET_BATTERY_VOLT:
                mContext.registerReceiver(mReceiver, mBatteryFilter);
                break;
            case WIDGET_DATE:
            case WIDGET_TIME:
                mHandler.postDelayed(mRunnable, 0);
                break;
            case WIDGET_SATELLITES:
                mLocationManager.addGpsStatusListener(mGpsStatusListener);
                break;
            case WIDGET_ACCURACY:
            case WIDGET_ALTITUDE:
            case WIDGET_HEADING:
            case WIDGET_LATITUDE:
            case WIDGET_LONGITUDE:
            case WIDGET_SPEED:
            case WIDGET_GPSDATE:
            case WIDGET_GPSTIME:
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        GPS_UPDATE_INTERVAL, 0, mLocationListener);
        }

        // Preferences might have changed while we were paused
        if(mDefaultUnits){
            setUnits(UNITS_DEFAULT);
        }
        resetUpdateInterval();
        mPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public int getWidgetType(){
		return mWidgetType;
	}

//  Cannot change widget type
//
//	private void setWidgetType(int type){
//		mWidgetType = type;
//        setValueFormat(mWidgetType);
//        setUnitsText(mUnitsType);
//		invalidate();
//		requestLayout();
//	}

	public double getValue() {
		return mValue;
	}

	public void setValue(double value) {
		this.mValue = value;
        if(Double.isNaN(value)){
            value = 0;
        }
        else {
            value = convertValue(value);
        }
        mValueText.setText(String.format(mValueFormat, value));
		invalidate();
		requestLayout();
	}

	public int getUnits() {
        if(mDefaultUnits){
            return UNITS_DEFAULT;
        }
		return mUnitsType;
	}

	public void setUnits(int units) {
        if(units == UNITS_DEFAULT){
            mDefaultUnits = true;
            final Resources res = getResources();
            mUnitsType = Integer.parseInt(mPreferences.getString(
                    res.getString(R.string.pref_units_key),
                    res.getInteger(R.integer.default_unit) + ""));
        }
        else {
            mDefaultUnits = false;
            mUnitsType = units;
        }

        setUnitsText(mUnitsType);
        if(mValue != 0){
            setValue(mValue);
        }
//        invalidate();
//        requestLayout();
	}

    public void resetUpdateInterval(){
        final String updateKey = getResources().getString(R.string.pref_display_freq_key);
        final int interval = Integer.parseInt(mPreferences.getString(updateKey, GPS_UPDATE_INTERVAL + ""));
        setUpdateInterval(interval);
    }

    public void setUpdateInterval(int interval){
        if(interval != mUpdateInterval){
            // TODO: hocus pocus
        }
        mUpdateInterval = interval;
    }

    public void setStale(boolean isStale) {
        mValueText.getPaint().setAlpha(isStale ? 128 : 255);
        invalidate();
    }

    public void setTrack(Track track){
        if(mTrack != null){
            mTrack.unregisterForTrackChanges(mTrackChangeListener);
        }
        mTrack = track;
        if(mTrack != null){
            mTrack.registerForTrackChanges(mTrackChangeListener);
            // initialise values
            mTrackChangeListener.onTrackChanged(mTrack);
        }
    }

    public void setRoute(Route route){
        mRoute = route;
    }

    public void setPoint(Point point){
        mPoint = point;
    }

    private void setUnitsText(int units){
        final Resources res = getResources();
        String[] values = null;
        String text = null;
        switch (mWidgetType){
            case WIDGET_SPEED:
            case WIDGET_SPEED_AVERAGE:
                values = res.getStringArray(R.array.units_speed);
                break;
            case WIDGET_DISTANCE:
                values = res.getStringArray(R.array.units_distance);
                break;
            case WIDGET_ACCURACY:
            case WIDGET_ALTITUDE:
            case WIDGET_ROUTE_OFFSET:
                values = res.getStringArray(R.array.units_shortdistance);
                break;
            case WIDGET_LATITUDE:
                text = res.getString(R.string.units_lat);
                break;
            case WIDGET_LONGITUDE:
                text = res.getString(R.string.units_lon);
                break;
            case WIDGET_HEADING:
            case WIDGET_BEARING:
                text = res.getString(R.string.units_heading);
                break;
            case WIDGET_BATTERY_LEVEL:
                text = res.getString(R.string.units_battery_level);
                break;
            case WIDGET_BATTERY_TEMP:
                text = res.getString(R.string.units_battery_temp);
                break;
            case WIDGET_BATTERY_VOLT:
                text = res.getString(R.string.units_battery_volt);
                break;
        }
        if(text == null && values != null && values.length >= units) {
            text = values[units-1];
        }
        mUnitsText.setText(text);
    }

    private void setValueFormat(int widgetType){
        switch(widgetType){
            case WIDGET_SPEED:
            case WIDGET_SPEED_AVERAGE:
                mValueFormat = "%.1f";
                break;
            case WIDGET_BATTERY_VOLT:
                mValueFormat = "%.2f";
                break;
            case WIDGET_LATITUDE:
                mValueFormat = "%05.3f";
                break;
            case WIDGET_LONGITUDE:
                mValueFormat = "%06.3f";
                break;
            case WIDGET_DISTANCE:
            case WIDGET_BATTERY_LEVEL:
            case WIDGET_HEADING:
            case WIDGET_BEARING:
            default:
                mValueFormat = "%.0f";
        }
    }

    private double convertValue(double value){
        Resources res = getResources();
        TypedArray values = null;
        double factor = 1;
        switch (mWidgetType){
            case WIDGET_SPEED:
            case WIDGET_SPEED_AVERAGE:
                values = res.obtainTypedArray(R.array.unit_conversion_speed);
                break;
            case WIDGET_DISTANCE:
                values = res.obtainTypedArray(R.array.unit_conversion_distance);
                break;
            case WIDGET_ACCURACY:
            case WIDGET_ALTITUDE:
            case WIDGET_ROUTE_OFFSET:
                values = res.obtainTypedArray(R.array.unit_conversion_shortdistance);
                break;
        }
        if(values != null && values.length() >= mUnitsType){
            factor = values.getFloat(mUnitsType-1,1);
        }
        return value * factor;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int w = r - l;
        int h = b - t;
        //mValueText.setTextSize((float)h * 0.4f);

        Shader textShader = new LinearGradient(0, 0, 0, h,
                new int[]{Color.rgb(255,255,255),Color.rgb(128,128,128),Color.rgb(255,255,255)},
                new float[]{0, 0.75f, 1}, Shader.TileMode.CLAMP);
        mValueText.getPaint().setShader(textShader);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final Resources res = getResources();
        if(key.equals(res.getString(R.string.pref_units_key))){
            if(mDefaultUnits){
                setUnits(UNITS_DEFAULT);
            }
        }else if(key.equals(res.getString(R.string.pref_display_freq_key))){
            resetUpdateInterval();
        }
    }

    Track.OnTrackChangedListener mTrackChangeListener = new Track.OnTrackChangedListener() {
        @Override
        public void onTrackChanged(Track target) {
            switch (mWidgetType){
                case WIDGET_DISTANCE:
                    setValue(target.getDistance());
                    break;
                case WIDGET_DURATION:
                    int s = (int)target.getDuration();
                    mValueText.setText(String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60));
                    break;
                case WIDGET_SPEED_AVERAGE:
                    double d = target.getDistance() / target.getDuration();
                    setValue(d);
                    break;
            }
        }
    };
}
