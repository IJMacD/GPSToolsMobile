package com.ijmacd.gpstools.mobile;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.os.Bundle;
import androidx.gridlayout.widget.GridLayout;
import androidx.legacy.widget.Space;

import android.util.Log;
import android.view.ActionMode;
import android.view.DragEvent;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import com.movisens.smartgattlib.Characteristic;
import com.movisens.smartgattlib.Descriptor;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity implements ActionBar.OnNavigationListener {

    private static final String LOG_TAG = "GPSTools";

    private static final int DIALOG_EDIT = 1;

    private static final int DIALOG_ADD = 2;

    public static final String EXTRA_COLS = "cols";

    public static final String EXTRA_ROWS = "rows";

    public static final String EXTRA_UNITS = "units";

    public static final String EXTRA_TRACK = "track";

    private static final int REQUEST_PERMISSION_LOCATION = 1;

    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

    private static final String PREF_ORIENTATION = "pref_orientation";

    private TrackService mTrackService;

    private Track mCurrentTrack;

    private ArrayList<DashboardWidget> mWidgets = new ArrayList<DashboardWidget>();

    private ArrayList<DashboardWidget> mSelectedWidgets = new ArrayList<DashboardWidget>();

    private boolean mIsBound;

    private boolean mRecording;

    private MenuItem mRecordItem;

    private MenuItem mEditItem;

    private DatabaseHelper mDatabase;

    private GridLayout mGridLayout;

    // private FrameLayout mDragLayer;

    private Space mSpace;

    private ActionMode mActionMode;

    private int mWidgetWidth;

    private int mWidgetHeight;

    final WidgetTypesHelper mWidgetTypesHelper = new WidgetTypesHelper(this);
    private SharedPreferences mPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener mPreferencesReceiver;
    private CharSequence mTitle;
    private CharSequence mDrawerTitle;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        updateOrientation();

        mPreferencesReceiver = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(PREF_ORIENTATION)){
                    updateOrientation();
                }
            }
        };

        mDatabase = new DatabaseHelper(this);

        mGridLayout = (GridLayout)findViewById(R.id.grid_dashboard);

        mGridLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActionMode != null) {
                    mActionMode.finish();
                }
            }
        });

        mSpace = new Space(this);
        // Bastard! This line took some source code digging to find
        // ViewGroups do not pass on dragEvents to children which are not visible
        // Spaces set themselves to not be visible in their constructors
        mSpace.setVisibility(View.VISIBLE);
        mSpace.setOnDragListener(mDragListener);
//        mDragLayer = (FrameLayout)findViewById(R.id.drag_layer);

        mTitle = mDrawerTitle = getTitle();

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, new String[]{"Dashboard","+ Add New"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(adapter, this);

        final String[] navigationLabels = getResources().getStringArray(R.array.navigation_labels);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, navigationLabels));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener(this, mDrawerLayout));

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                null, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                actionBar.setTitle(mTitle);
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                actionBar.setTitle(mDrawerTitle);
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                actionBar.setDisplayShowTitleEnabled(true);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        android.graphics.Point size = new android.graphics.Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int screenWidth = size.x - 16;
        mWidgetWidth = screenWidth / mGridLayout.getColumnCount();
        mWidgetHeight = (int)(90f * getResources().getDisplayMetrics().density);

        //mCurrentTrack = new Track(this);
        doBindService();

        loadWidgets();
        if(mWidgets.size() == 0){
            addDefaultWidgets();
        }
    }

    private void updateOrientation() {
        int orientation = Integer.parseInt(
                mPreferences.getString(PREF_ORIENTATION,
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR + ""));
        setRequestedOrientation(orientation);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_add).setVisible(!drawerOpen);
        menu.findItem(R.id.action_record).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause()");

        mPreferences.unregisterOnSharedPreferenceChangeListener(mPreferencesReceiver);

        for(DashboardWidget widget : mWidgets){
            widget.onPause();
        }

        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume()");

        ActionBar actionBar = getSupportActionBar();
        //actionBar.setSelectedNavigationItem(0);

        mPreferences.registerOnSharedPreferenceChangeListener(mPreferencesReceiver);
        updateOrientation();

        for(DashboardWidget widget : mWidgets){
            widget.onResume();
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(LOG_TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy()");
        doUnbindService();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            getSupportActionBar().setSelectedNavigationItem(
                    savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
                getSupportActionBar().getSelectedNavigationIndex());
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.dashboard, menu);
        mRecordItem = menu.findItem(R.id.action_record);
        if(mRecording){
            setRecording(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //
        int id = item.getItemId();
        switch (id){
            case R.id.action_add:
                showDialog(DIALOG_ADD);
                break;
            case R.id.action_record:
                if(mRecording){
                    mTrackService.stopLogging();
                    Intent intent = new Intent(this, TrackDetailActivity.class);
                    intent.putExtra(EXTRA_TRACK, mCurrentTrack.getID());
//                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//                    stackBuilder.addParentStack(TrackDetailActivity.class);
//                    stackBuilder.addNextIntent(intent);
//                    stackBuilder.startActivities();
                    startActivity(intent);
                }
                else {
                    mCurrentTrack = mTrackService.startLogging();
                    for(DashboardWidget widget : mWidgets){
                        widget.setTrack(mCurrentTrack);
                    }
                }
                setRecording(!mRecording);
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        // When the given dropdown item is selected, show its contents in the
        // container view.

        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id){
            case DIALOG_ADD:
                return createAddDialog();
            case DIALOG_EDIT:
                return createEditDialog();
        }
        return super.onCreateDialog(id);
    }


    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id){
            case DIALOG_ADD:
                prepareAddDialog(dialog);
                break;
            case DIALOG_EDIT:
                prepareEditDialog(dialog);
        }
        super.onPrepareDialog(id, dialog);
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mTrackService = ((TrackService.LocalBinder)service).getService();

            setRecording(mTrackService.isRecording());

            mCurrentTrack = mTrackService.getTrack();
            for(DashboardWidget widget : mWidgets){
                widget.setTrack(mCurrentTrack);
            }

            Log.d(LOG_TAG, "Service Connected");
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mTrackService = null;
            Log.d(LOG_TAG, "Service Discconnected");
        }
    };

    void doBindService() {
        Intent intent = new Intent(this, TrackService.class);
        startService(intent);
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(intent, mConnection, 0);
        mIsBound = true;
        Log.d(LOG_TAG, "Track Service Bound");

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            Log.d(LOG_TAG, "Service Unbound");

            unbindService(mServiceConnection);
            mBluetoothLeService = null;
        }
    }

    private BluetoothLeService mBluetoothLeService;

    private String mDeviceAddress = "C9:DC:71:75:48:B7";
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(LOG_TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
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
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                Log.d(LOG_TAG, "BLE Discovered services");

                List<BluetoothGattService> services = mBluetoothLeService.getSupportedGattServices();
                for(BluetoothGattService service : services){
                    BluetoothGattCharacteristic chara = service.getCharacteristic(Characteristic.CSC_MEASUREMENT);
                    if(chara != null){
                        mBluetoothLeService.setCharacteristicNotification(chara, true);
                        Log.d(LOG_TAG, "Set Characteristic Notification");
                        break;
                    }
                }


            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
               // Log.d(LOG_TAG, "BLE Data: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void setRecording(boolean recording) {
        if(mRecordItem != null){
            //AnimationDrawable icon = (AnimationDrawable)mRecordItem.getIcon();
            if(recording){
                //icon.start();
                mRecordItem.setIcon(R.drawable.ic_menu_pause);
            }
            else {
                //icon.stop();
                // Set the animation back to the beginning
                //mRecordItem.setIcon(null);
                mRecordItem.setIcon(R.drawable.ic_menu_record);
            }
        }
        mRecording = recording;
    }

    private int mDragStartIndex;
    private int mLastXTouch;
    private int mLastYTouch;

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN: {
                    mLastXTouch = (int) event.getX();
                    mLastYTouch = (int) event.getY();
                    break;
                }
            }
            return false;
        }
    };

    private View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {

            if(mActionMode != null){
                mActionMode.finish();
            }

            mDragStartIndex = mGridLayout.indexOfChild(view);
            mGridLayout.addView(mSpace, mDragStartIndex);
            final float scale = 1.1f;
            final int viewWidth = view.getWidth(),
                      viewHeight = view.getHeight(),
                      shadowWidth = (int)Math.floor(viewWidth*scale),
                      shadowHeight = (int)Math.floor(viewHeight*scale),
                      dcx = (shadowWidth - viewWidth) / 2,
                      dcy = (shadowHeight - viewHeight) / 2;
            View.DragShadowBuilder dragShadowBuilder = new View.DragShadowBuilder(view){
                @Override
                public void onProvideShadowMetrics(android.graphics.Point dimensions, android.graphics.Point touch_point){
                    dimensions.x = shadowWidth;
                    dimensions.y = shadowHeight;
                    touch_point.x = mLastXTouch + dcx;
                    touch_point.y = mLastYTouch + dcy;
                }

                @Override
                public void onDrawShadow(Canvas canvas) {
                    canvas.scale(scale,scale);
                    super.onDrawShadow(canvas);
                }
            };
            view.startDrag(null, dragShadowBuilder, view, 0);
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            mGridLayout.removeView(view);

            return true;
        }
    };

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        Toast mToast;
        @Override
        public void onClick(View view) {
            if(mActionMode != null){
//                mDragLayer.removeResizeFrame();
                if(view.isSelected()){
                    view.setSelected(false);
                    mSelectedWidgets.remove(view);
                    final int selectedSize = mSelectedWidgets.size();
                    if(selectedSize == 0){
                        mActionMode.finish();
                    }
                    else if(selectedSize > 1){
                        mEditItem.setVisible(false);
                    }
                    else {
                        mEditItem.setVisible(true);
                    }
                }
                else {
                    view.setSelected(true);
                    mSelectedWidgets.add((DashboardWidget) view);
                    mEditItem.setVisible(false);
                }
            }
            else {
                final DashboardWidget widget = (DashboardWidget)view;
                final int type = widget.getWidgetType();
                final WidgetTypesHelper.WidgetDescription widgetDescription = mWidgetTypesHelper.getWidget(type);
                if(widgetDescription != null){
                    if(mToast == null){
                        mToast = Toast.makeText(DashboardActivity.this, widgetDescription.name, Toast.LENGTH_SHORT);
                    }
                    mToast.setText(widgetDescription.name);
                    mToast.show();
                }
            }
        }
    };

    private View.OnDragListener mDragListener = new View.OnDragListener() {
        GridLayout.LayoutParams mParams;
        View mDraggedView;
        int mLastIndex;

        @Override
        public boolean onDrag(View v, DragEvent event) {
            final int action = event.getAction();

            switch (action){
                case DragEvent.ACTION_DRAG_STARTED:

                    if(mParams == null){
                        mDraggedView = (View)event.getLocalState();
                        mParams = (GridLayout.LayoutParams)mDraggedView.getLayoutParams();
                        mSpace.setLayoutParams(mParams);
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        mLastIndex = mDragStartIndex;
                    }

                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:

                    if(!v.equals(mSpace)){
                        final int index = mGridLayout.indexOfChild(v);
                        mGridLayout.removeView(mSpace);
                        mGridLayout.addView(mSpace, index);
                        mLastIndex = index;
                    }

                    break;

                case DragEvent.ACTION_DRAG_EXITED:
                    break;

                case DragEvent.ACTION_DROP:
                    // Insert dragged view before this view
                    mGridLayout.removeView(mSpace);
                    mGridLayout.addView(mDraggedView, mLastIndex);

                    return true;

                case DragEvent.ACTION_DRAG_ENDED:

                    // mParams is our handy marker object, making sure things only happen once!
                    if(mParams != null){


                        // If dragging was cancelled just drop the view where it last was
                        // so that it is not lost or leaked
                        if(!event.getResult()){
                            mGridLayout.addView(mDraggedView, mLastIndex);
                        }

                        saveWidgets();

                        if (mActionMode == null) {

                            // Start the CAB using the ActionMode.Callback defined above
                            mActionMode = startActionMode(mActionModeCallback);
                            mDraggedView.setSelected(true);
                            mSelectedWidgets.add((DashboardWidget) mDraggedView);

//                            if(mSelectedWidgets.size() == 1){
//                                mDragLayer.addResizeFrame(mSelectedWidgets.get(0), mGridLayout);
//                            }
                        }

                        // clean up mParams so that they can be re-set at the next drag event;
                        mParams = null;
                    }
            }
            return false;
        }
    };

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.action_menu, menu);
            mEditItem = menu.findItem(R.id.action_edit);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is java.lang.Objectinvalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
//            for(DashboardWidget widget : mWidgets){
//                widget.setClickable(true);
//            }
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_edit:
//                    Intent intent = new Intent(DashboardActivity.this, EditWidgetDialog.class);
//                    intent.putExtra(EXTRA_ROWS, 1);
//                    intent.putExtra(EXTRA_COLS, 1);
//                    intent.putExtra(EXTRA_UNITS, 0);
//                    startActivityForResult(intent, R.id.action_edit);
                    showDialog(DIALOG_EDIT);
                    return true;
                case R.id.action_delete:
                    deleteSelectedItems();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            for(DashboardWidget widget : mSelectedWidgets){
                //widget.setClickable(false);
                widget.setSelected(false);
            }
            mSelectedWidgets.clear();
        }
    };

    private void saveWidgets(){
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(DatabaseHelper.WIDGET_TABLE_NAME, null, null);

            ContentValues values;
            DashboardWidget widget;

            int i = 0;
            int l = mGridLayout.getChildCount();
            for(;i<l;i++){
                try {
                    widget = (DashboardWidget)mGridLayout.getChildAt(i);
                    values = new ContentValues();
                    values.put(DatabaseHelper.ORDER_COLUMN, i);
                    values.put(DatabaseHelper.TYPE_COLUMN, widget.getWidgetType());
                    values.put(DatabaseHelper.WIDTH_COLUMN, getWidgetColsSpan(widget));
                    values.put(DatabaseHelper.HEIGHT_COLUMN, getWidgetRowsSpan(widget));
                    values.put(DatabaseHelper.UNITS_COLUMN, widget.getUnits());
                    db.insert(DatabaseHelper.WIDGET_TABLE_NAME, DatabaseHelper.ID_COLUMN, values);
                } catch (ClassCastException e){}
            }

            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();

            db.close();
        }
    }

    private void loadWidgets(){
        mWidgets.clear();
        mGridLayout.removeAllViews();

        SQLiteDatabase db = mDatabase.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.WIDGET_TABLE_NAME, new String[]{
                DatabaseHelper.TYPE_COLUMN,
                DatabaseHelper.WIDTH_COLUMN,
                DatabaseHelper.HEIGHT_COLUMN,
                DatabaseHelper.UNITS_COLUMN
        }, null, null, null, null, DatabaseHelper.ORDER_COLUMN);

        while(cursor.moveToNext()){
            int type = cursor.getInt(0);
            int colSpan = cursor.getInt(1);
            int rowSpan = cursor.getInt(2);
            int units = cursor.getInt(3);

            addWidget(type, colSpan, rowSpan, units);
        }
    }
    public void addWidget(int type){
        addWidget(type, 1, 1);
        saveWidgets();
    }

    private void addWidget(int type, int colSpan, int rowSpan){
        addWidget(type, colSpan, rowSpan, DashboardWidget.UNITS_DEFAULT);
    }

    private void addWidget(int widgetType, int colSpan, int rowSpan, int unitsType) {

        DashboardWidget widget = DashboardWidget.WidgetFactory(this, widgetType, unitsType);


        GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED, colSpan);
        GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED, rowSpan);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
        params.width = mWidgetWidth * colSpan;
        params.height = mWidgetHeight * rowSpan;

        Log.d(LOG_TAG, "Widget Added: " + widgetType + " (" + colSpan + " x " + rowSpan + ")");

        widget.setLayoutParams(params);

        widget.setOnTouchListener(mOnTouchListener);
        widget.setOnLongClickListener(mLongClickListener);
        widget.setOnClickListener(mClickListener);
        widget.setOnDragListener(mDragListener);
        widget.setLongClickable(true);
        widget.setClickable(false);

        if(mCurrentTrack != null){
            widget.setTrack(mCurrentTrack);
        }

        mWidgets.add(widget);
        mGridLayout.addView(widget);

        if ((widget instanceof DashboardWidget.GPSLocationWidget
                || widget instanceof DashboardWidget.GPSStatusWidget)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
            }, REQUEST_PERMISSION_LOCATION);
            return;
        }

        widget.onResume();
    }

    private void deleteSelectedItems(){
        for(DashboardWidget widget : mSelectedWidgets){
            mWidgets.remove(widget);
            mGridLayout.removeView(widget);
            widget.destroy();
        }
        mSelectedWidgets.clear();

        // Now save current configuration
        saveWidgets();
    }

    private void addDefaultWidgets() {
        addWidget(DashboardWidget.WIDGET_SPEED, 2, 2);
        addWidget(DashboardWidget.WIDGET_LATITUDE);
        addWidget(DashboardWidget.WIDGET_LONGITUDE);
        addWidget(DashboardWidget.WIDGET_HEADING);
        addWidget(DashboardWidget.WIDGET_BATTERY_LEVEL);
        addWidget(DashboardWidget.WIDGET_ACCURACY);
        addWidget(DashboardWidget.WIDGET_SPEED, 1, 1, DashboardWidget.UNITS_IMPERIAL);
        addWidget(DashboardWidget.WIDGET_SPEED, 1, 1, DashboardWidget.UNITS_SI);
        addWidget(DashboardWidget.WIDGET_ALTITUDE);

        saveWidgets();
    }

    private int getWidgetColsSpan(DashboardWidget widget){
        return widget.getLayoutParams().width / mWidgetWidth;
    }

    private int getWidgetRowsSpan(DashboardWidget widget){
        return widget.getLayoutParams().height / mWidgetHeight;
    }


    private Dialog createEditDialog(){
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Holo_Light_Dialog);
        dialog.setContentView(R.layout.widget_edit);
        dialog.setTitle(R.string.edit_widget_title);

        final NumberPicker colsPicker = (NumberPicker)dialog.findViewById(R.id.picker_cols);
        colsPicker.setMaxValue(10);

        final NumberPicker rowsPicker = (NumberPicker)dialog.findViewById(R.id.picker_rows);
        rowsPicker.setMaxValue(10);

        Button discardButton = (Button)dialog.findViewById(R.id.button_discard);
        discardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActionMode != null) {
                    mActionMode.finish();
                }
                dialog.dismiss();
            }
        });

        return dialog;
    }

    private void prepareEditDialog(final Dialog dialog){
        if(mSelectedWidgets.size() == 0){
            dialog.dismiss();
            return;
        }
        final DashboardWidget widget = mSelectedWidgets.get(0);

        final NumberPicker colsPicker = (NumberPicker)dialog.findViewById(R.id.picker_cols);
        colsPicker.setMaxValue(10);
        colsPicker.setValue(getWidgetColsSpan(widget));

        final NumberPicker rowsPicker = (NumberPicker)dialog.findViewById(R.id.picker_rows);
        rowsPicker.setMaxValue(10);
        rowsPicker.setValue(getWidgetRowsSpan(widget));

        final Spinner unitsSpinner = (Spinner)dialog.findViewById(R.id.spinner_units);
        unitsSpinner.setSelection(widget.getUnits());

        Button doneButton = (Button)dialog.findViewById(R.id.button_done);
        doneButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                GridLayout.LayoutParams params = (GridLayout.LayoutParams)widget.getLayoutParams();
                int colsSpan = colsPicker.getValue();
                int rowsSpan = rowsPicker.getValue();
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, colsSpan);
                params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, rowsSpan);
                params.width = mWidgetWidth * colsSpan;
                params.height = mWidgetHeight * rowsSpan;
                widget.setUnits(unitsSpinner.getSelectedItemPosition());
                saveWidgets();
                if(mActionMode != null){
                    mActionMode.finish();
                }
                dialog.dismiss();
            }
        });
    }
    private Dialog createAddDialog() {
        return new AddWidgetDialog(this);
    }
    private void prepareAddDialog(Dialog dialog) {

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
