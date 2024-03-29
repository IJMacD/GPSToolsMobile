package com.ijmacd.gpstools.mobile;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.*;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DashboardWidget extends FrameLayout
 implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int WIDGET_UNKNOWN = 0;

	// Stateless - GPS		1 - 31
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
	public static final int WIDGET_GPSAGE = 10;
	public static final int WIDGET_MAP = 11;
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

    // Sensor 160 - 191
    public static final int WIDGET_PROXIMITY = 160;
    public static final int WIDGET_LIGHT = 161;
    public static final int WIDGET_AMBIENT_TEMPERATURE = 162;
    public static final int WIDGET_PRESSURE = 163;
    public static final int WIDGET_RELATIVE_HUMIDITY = 164;
    public static final int WIDGET_ACCELERATION = 165;
    public static final int WIDGET_GRAVITY = 166;
    public static final int WIDGET_LINEAR_ACCELERATION = 167;
    public static final int WIDGET_CSC_SPEED = 168;
    public static final int WIDGET_CSC_CADENCE = 169;

    public static final int UNITS_DEFAULT = 0;
    public static final int UNITS_METRIC = 1;
    public static final int UNITS_IMPERIAL = 2;
    public static final int UNITS_SI = 3;
    public static final int UNITS_NAUTICAL = 4;

    private static final int DATE_UPDATE_INTERVAL = 200;
    private static final int GPS_UPDATE_INTERVAL = 1000;

    private int mWidgetType;
    private int mUnitsType = UNITS_DEFAULT;
    private boolean mDefaultUnits = true;

	private TextView mValueText;
	private TextView mUnitsText;

	private double mValue;
    private String mValueFormat;

    private int mUpdateInterval;

    private Context mContext;

    private SharedPreferences mPreferences;

    private Track mTrack;
    private Route mRoute;
    private Point mPoint;

    private static final boolean DEBUG = true;
    private ImageView mCategoryImage;
    private Shader mTextShader;
    private Matrix mTextShaderMatrix;

    public DashboardWidget(Context context){
        super(context);
    }

    public DashboardWidget(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        final Resources.Theme theme = context.getTheme();
        if (theme != null) {
            final TypedArray attributes = theme.obtainStyledAttributes(attrs, R.styleable.DashboardWidget, 0, 0);
            if (attributes != null) {
                try {
                    mWidgetType = attributes.getInteger(R.styleable.DashboardWidget_widgetType, 0);
                    mUnitsType = attributes.getInteger(R.styleable.DashboardWidget_units, UNITS_DEFAULT);
                    init();
                }
                finally {
                    attributes.recycle();
                }
            }
        }

    }

    public DashboardWidget(Context context, AttributeSet attributeSet, int defStyle){
        super(context, attributeSet, defStyle);
    }

    public DashboardWidget(Context context, int widgetType) {
        super(context);
        mContext = context;
        mWidgetType = widgetType;
        init();
    }

    public DashboardWidget(Context context, int widgetType, int unitsType) {
        super(context);
        mContext = context;
        mWidgetType = widgetType;
        mUnitsType = unitsType;
        init();
    }

    private void init(){

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_dashboard_item, this);

        setBackgroundResource(R.drawable.widget_background);

        mValueText = (TextView)findViewById(R.id.value_text);

        mUnitsText = (TextView)findViewById(R.id.unit_text);

        mCategoryImage = (ImageView)findViewById(R.id.category_image);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        if(mPreferences != null){
            mPreferences.registerOnSharedPreferenceChangeListener(this);
        }

        mTextShader = new LinearGradient(0, 0, 0, 1,
                new int[]{
                        Color.rgb(255,255,255),
                        Color.rgb(128,128,128),
                        Color.rgb(255,255,255)},
                new float[]{0, 0.75f, 1}, Shader.TileMode.CLAMP);
        mTextShaderMatrix = new Matrix();

        mValueText.getPaint().setAlpha(255);
        mValueText.getPaint().setShader(mTextShader);

        if (mWidgetType < 64){}
        else if(mWidgetType < 96){
            // Category Track
            mCategoryImage.setImageResource(R.drawable.category_track);
        }
        else if(mWidgetType < 128){
            // Category Route
            mCategoryImage.setImageResource(R.drawable.category_route);
        }
        else if(mWidgetType < 160){
            // Category Point
            mCategoryImage.setImageResource(R.drawable.category_point);
        }

        resetUpdateInterval();

        setValueFormat(null);

        setUnits(mUnitsType);
    }

    public void onResume() {

        // Preferences might have changed while we were paused
        if(mDefaultUnits){
            setUnits(UNITS_DEFAULT);
        }
        resetUpdateInterval();
        if(mPreferences != null){
            mPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    public void onPause(){
        if(mPreferences != null){
            mPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    public void onDestroy() {
        mContext = null;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int w = r - l;
        int h = b - t;

        // Scale text gradient
        mTextShaderMatrix.setScale(1, h);
        mTextShader.setLocalMatrix(mTextShaderMatrix);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final Resources res = getResources();
        if (res != null) {
            if(key.equals(res.getString(R.string.pref_units_key))){
                if(mDefaultUnits){
                    setUnits(UNITS_DEFAULT);
                }
            }else if(key.equals(res.getString(R.string.pref_display_freq_key))){
                resetUpdateInterval();
            }
        }
    }

    public void destroy(){
        onPause();
        onDestroy();
    }

    public int getWidgetType(){
		return mWidgetType;
	}

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
        setFormattedValue(String.format(mValueFormat, value));
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
            if (res != null && mPreferences != null) {
                mUnitsType = Integer.parseInt(mPreferences.getString(
                        res.getString(R.string.pref_units_key),
                        res.getInteger(R.integer.default_unit) + ""));
            }
        }
        else {
            mDefaultUnits = false;
            mUnitsType = units;
        }

        setUnitsText(null);
        if(mValue != 0){
            setValue(mValue);
        }
	}

    protected void setUnitsText(CharSequence unitsText){
        CharSequence text = null;
        if(unitsText != null && unitsText.length() > 0){
            text = unitsText;
        }
        else {
            final Resources res = getResources();
            if(res == null){
                return;
            }
            String[] values = null;
            switch (mWidgetType){
                case WIDGET_SPEED:
                case WIDGET_SPEED_AVERAGE:
                case WIDGET_CSC_SPEED:
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
            }
            if(text == null && values != null && values.length >= mUnitsType) {
                text = values[mUnitsType-1];
            }
        }
        if(text != null)
            mUnitsText.setText(text);
    }

    protected void setValueFormat(String format){
        if(format != null && format.length() > 0){
            mValueFormat = format;
        }
        else {
            switch(mWidgetType){
                case WIDGET_SPEED:
                case WIDGET_SPEED_AVERAGE:
                case WIDGET_CSC_SPEED:
                    mValueFormat = "%.1f";
                    break;
                case WIDGET_LATITUDE:
                    mValueFormat = "%06.3f";
                    break;
                case WIDGET_LONGITUDE:
                    mValueFormat = "%07 .3f";
                    break;
                case WIDGET_DISTANCE:
                case WIDGET_HEADING:
                case WIDGET_BEARING:
                default:
                    mValueFormat = "%.0f";
            }
        }
    }

    protected void setFormattedValue(String value){
        mValueText.setText(value);
    }

    protected void setCategoryImage(int resourceId) {
        mCategoryImage.setImageResource(resourceId);
    }
    public void setUpdateInterval(int interval){
        if(interval != mUpdateInterval){
            // TODO: hocus pocus
        }
        mUpdateInterval = interval;
    }

    public void resetUpdateInterval(){
        final Resources res = getResources();
        if (res != null && mPreferences != null) {
            final String updateKey = res.getString(R.string.pref_display_freq_key);
            final int interval = Integer.parseInt(mPreferences.getString(updateKey, GPS_UPDATE_INTERVAL + ""));
            setUpdateInterval(interval);
        }
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

    private double convertValue(double value){
        double factor = 1;
        Resources res = getResources();
        if (res != null) {
            TypedArray values = null;
            switch (mWidgetType){
                case WIDGET_SPEED:
                case WIDGET_SPEED_AVERAGE:
                case WIDGET_CSC_SPEED:
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
        }
        return value * factor;
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

    public static DashboardWidget WidgetFactory(Context context, int widgetType) {
        return WidgetFactory(context, widgetType, UNITS_DEFAULT);
    }

    public static DashboardWidget WidgetFactory(Context context, int widgetType, int unitsType) {
        switch (widgetType){
            case WIDGET_BATTERY_LEVEL:
                return new BatteryLevelWidget(context);
            case WIDGET_BATTERY_TEMP:
                return new BatteryTemperatureWidget(context);
            case WIDGET_BATTERY_VOLT:
                return new BatteryVoltageWidget(context);
            case WIDGET_DATE:
            case WIDGET_TIME:
            case WIDGET_DATETIME:
                return new DateTimeWidget(context, widgetType);
            case WIDGET_ACCURACY:
            case WIDGET_ALTITUDE:
            case WIDGET_HEADING:
            case WIDGET_LATITUDE:
            case WIDGET_LONGITUDE:
            case WIDGET_SPEED:
            case WIDGET_GPSDATE:
            case WIDGET_GPSTIME:
            case WIDGET_GPSDATETIME:
                return new GPSLocationWidget(context, widgetType, unitsType);
            case WIDGET_SATELLITES:
                return new GPSStatusWidget(context);
            case WIDGET_PROXIMITY:
            case WIDGET_LIGHT:
            case WIDGET_AMBIENT_TEMPERATURE:
            case WIDGET_PRESSURE:
            case WIDGET_RELATIVE_HUMIDITY:
            case WIDGET_ACCELERATION:
            case WIDGET_GRAVITY:
            case WIDGET_LINEAR_ACCELERATION:
                return new SensorWidget(context, widgetType);
            case WIDGET_CSC_SPEED:
            case WIDGET_CSC_CADENCE:
                return new CscWidget(context, widgetType);
            default:
                return new DashboardWidget(context, widgetType, unitsType);
        }
    }

    static class GPSLocationWidget extends DashboardWidget{
        private final Context mContext;
        private final LocationManager mLocationManager;
        private final LocationListener mLocationListener;

        public GPSLocationWidget(Context context, final int widgetType, int unitsType){
            super(context, widgetType, unitsType);
            mContext = context;

            final DateFormat dateFormat;
            switch (widgetType){
                case WIDGET_GPSDATE:
//                    dateFormat = DateFormat.getDateInstance();
                    dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    break;
                case WIDGET_GPSTIME:
//                    dateFormat = DateFormat.getTimeInstance();
                    dateFormat = new SimpleDateFormat("HH:mm:ss");
                    break;
                case WIDGET_GPSDATETIME:
//                    dateFormat = DateFormat.getDateTimeInstance();
                    dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
                    break;
                default:
                    dateFormat = null;
            }

            mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            mLocationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Date fixDate;
                    switch (widgetType){
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
                        case WIDGET_GPSDATE:
                        case WIDGET_GPSTIME:
                        case WIDGET_GPSDATETIME:
                            fixDate = new Date(location.getTime());
                            setFormattedValue(dateFormat.format(fixDate));
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

            setCategoryImage(R.drawable.category_gps);
        }

        @Override
        public void onResume(){
            super.onResume();

            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    GPS_UPDATE_INTERVAL, 0, mLocationListener);

        }

        @Override
        public void onPause(){
            super.onPause();
            mLocationManager.removeUpdates(mLocationListener);
        }
    }

    static class GPSStatusWidget extends DashboardWidget{
        private final Context mContext;
        private final LocationManager mLocationManager;
        private final GpsStatus.Listener mGpsStatusListener;

        public GPSStatusWidget(Context context){
            super(context, WIDGET_SATELLITES);

            mContext = context;

            mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            mGpsStatusListener = new GpsStatus.Listener() {
                GpsStatus mStatus;
                @Override
                public void onGpsStatusChanged(int event) {
                    if(event == GpsStatus.GPS_EVENT_SATELLITE_STATUS){
                        mStatus = mLocationManager.getGpsStatus(mStatus);
                        int used = 0,
                                total = 0;
                        final Iterable<GpsSatellite> satellites = mStatus.getSatellites();
                        for(GpsSatellite sat : satellites){
                            used += sat.usedInFix() ? 1 : 0;
                            total += 1;
                        }
                        setFormattedValue(used + "/" + total);
                    }
                }
            };

            setCategoryImage(R.drawable.category_gps);
        }

        @Override
        public void onResume(){
            super.onResume();

            mLocationManager.addGpsStatusListener(mGpsStatusListener);
        }

        @Override
        public void onPause(){
            super.onPause();

            mLocationManager.removeGpsStatusListener(mGpsStatusListener);
        }
    }

    abstract static class IntentWidget extends DashboardWidget {

        private final Context mContext;
        protected IntentFilter mIntentFilter;
        protected BroadcastReceiver mBroadcastReceiver;

        public IntentWidget(Context context, int widgetType) {
            super(context, widgetType);
            mContext = context;
            init();
            onResume();
        }

        protected abstract void init();

        @Override
        public void onResume() {
            super.onResume();
            if(mContext != null)
                mContext.registerReceiver(mBroadcastReceiver, mIntentFilter);
        }

        @Override
        public void onPause() {
            super.onPause();
            if(mContext != null)
                mContext.unregisterReceiver(mBroadcastReceiver);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mBroadcastReceiver = null;
        }
    }

    static class BatteryLevelWidget extends IntentWidget {

        public BatteryLevelWidget(Context context) {
            super(context, WIDGET_BATTERY_LEVEL);
        }

        @Override
        protected void init() {
            mIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())){
                        float value = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                        setValue(value);
                    }
                }
            };
            final Resources res = getResources();
            if (res != null) {
                setUnitsText(res.getString(R.string.units_percent));
            }
            setCategoryImage(R.drawable.category_battery);
        }
    }

    static class BatteryTemperatureWidget extends IntentWidget {

        public BatteryTemperatureWidget(Context context) {
            super(context, WIDGET_BATTERY_TEMP);
        }

        @Override
        protected void init() {
            mIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())){
                        float value = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
                        setValue(value);
                    }
                }
            };
            final Resources res = getResources();
            if (res != null) {
                setUnitsText(res.getString(R.string.units_temp));
            }
            setCategoryImage(R.drawable.category_battery);
        }
    }

    static class BatteryVoltageWidget extends IntentWidget {

        public BatteryVoltageWidget(Context context) {
            super(context, WIDGET_BATTERY_VOLT);
        }

        @Override
        protected void init() {
            mIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())){
                        float value = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f;
                        setValue(value);
                    }
                }
            };
            final Resources res = getResources();
            if (res != null) {
                setUnitsText(res.getString(R.string.units_volt));
            }
            setValueFormat("%.2f");
            setCategoryImage(R.drawable.category_battery);
        }
    }

    static class DateTimeWidget extends DashboardWidget {

        private final Context mContext;
        private final Handler mHandler;
        private final Runnable mRunnable;

        public DateTimeWidget(Context context, final int widgetType){
            super(context, widgetType);
            mContext = context;
            mHandler = new Handler();
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    final Date now = new Date();
                    DateFormat dateFormatter = null;
                    switch (widgetType){
                        case WIDGET_DATE:
                            dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
//                            dateFormatter = DateFormat.getDateInstance();
                            break;
                        case WIDGET_TIME:
                            dateFormatter = new SimpleDateFormat("HH:mm:ss");
//                            dateFormatter = DateFormat.getTimeInstance();
                            break;
                        case WIDGET_DATETIME:
                            dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
//                            dateFormatter = DateFormat.getDateTimeInstance();
                            break;
                    }
                    if (dateFormatter != null) {
                        setFormattedValue(dateFormatter.format(now));
                    }
                    mHandler.postDelayed(this, DATE_UPDATE_INTERVAL);
                }
            };
        }

        @Override
        public void onResume(){
            super.onResume();
            mHandler.postDelayed(mRunnable, 0);
        }

        @Override
        public void onPause(){
            super.onPause();
            mHandler.removeCallbacks(mRunnable);
        }
    }

    static class SensorWidget extends DashboardWidget {

        private final SensorManager mSensorManager;
        private Sensor mSensor = null;
        private SensorEventListener mListener;

        public SensorWidget(Context context, final int widgetType) {
            super(context, widgetType);
            mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);

            switch (widgetType){
                case WIDGET_PROXIMITY:
                    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                    setUnitsText(context.getText(R.string.units_cm));
                    break;
                case WIDGET_LIGHT:
                    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
                    setUnitsText(context.getText(R.string.units_lx));
                    break;
                case WIDGET_AMBIENT_TEMPERATURE:
                    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
                    setUnitsText(context.getText(R.string.units_temp));
                    break;
                case WIDGET_PRESSURE:
                    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
                    setUnitsText(context.getText(R.string.units_pressure));
                    break;
                case WIDGET_RELATIVE_HUMIDITY:
                    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
                    setUnitsText(context.getText(R.string.units_percent));
                    break;
                case WIDGET_ACCELERATION:
                    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    setUnitsText(context.getText(R.string.units_acceleration));
                    setValueFormat("%.2f");
                    break;
                case WIDGET_GRAVITY:
                    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
                    setUnitsText(context.getText(R.string.units_acceleration));
                    setValueFormat("%.2f");
                    break;
                case WIDGET_LINEAR_ACCELERATION:
                    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
                    setUnitsText(context.getText(R.string.units_acceleration));
                    setValueFormat("%.2f");
                    break;
            }

            mListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    switch(widgetType){
                        case WIDGET_ACCELERATION:
                        case WIDGET_GRAVITY:
                        case WIDGET_LINEAR_ACCELERATION:
                            final double value = Math.sqrt(
                                    event.values[0]*event.values[0] +
                                    event.values[1]*event.values[1] +
                                    event.values[2]*event.values[2]);
                            setValue(value);
                            break;
                    default:
                        setValue(event.values[0]);
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {

                }
            };
        }

        @Override
        public void onResume() {
            super.onResume();
            mSensorManager.registerListener(mListener, mSensor, SensorManager.SENSOR_DELAY_UI);
        }

        @Override
        public void onPause() {
            super.onPause();
            mSensorManager.unregisterListener(mListener);
        }
    }

    static class CscWidget extends DashboardWidget {
        private final Context mContext;
        private final int mWidgetType;
        private double mLastSensorSpeed;
        private double mLastCadence;

        public CscWidget(Context context, int widgetType) {
            super(context, widgetType);
            mContext = context;
            mWidgetType = widgetType;

            if(mWidgetType == WIDGET_CSC_CADENCE){
                setUnitsText("rpm");
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        }

        @Override
        public void onPause() {
            super.onPause();
            mContext.unregisterReceiver(mGattUpdateReceiver);
        }

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
//                    Log.d(LOG_TAG, "BLE Connected");
                } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
//                    Log.d(LOG_TAG, "BLE Disconnected");
                    mLastSensorSpeed = 0;
                    mLastCadence = 0;
                } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//                    Log.d(LOG_TAG, "BLE Discovered services");
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                Log.d(LOG_TAG, "BLE Data: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                    mLastSensorSpeed = intent.getDoubleExtra(BluetoothLeService.EXTRA_SPEED, 0);
                    mLastCadence = intent.getDoubleExtra(BluetoothLeService.EXTRA_CADENCE, 0);
//                    Log.d(LOG_TAG, "Speed: " + mLastSensorSpeed + " Cadence: " + mLastCadence);
                }

                switch (mWidgetType){
                    case WIDGET_CSC_SPEED:
                        setValue(mLastSensorSpeed);
                        break;
                    case WIDGET_CSC_CADENCE:
                        setValue(mLastCadence);
                        break;
                }
            }
        };

        private static IntentFilter makeGattUpdateIntentFilter() {
            final IntentFilter intentFilter = new IntentFilter();
//            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
//            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
            return intentFilter;
        }
    }
}
