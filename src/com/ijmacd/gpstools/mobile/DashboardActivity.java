package com.ijmacd.gpstools.mobile;

import java.util.ArrayList;

import android.app.Dialog;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.widget.Space;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;

import android.os.Bundle;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class DashboardActivity extends SherlockActivity implements ActionBar.OnNavigationListener {

    private static final String LOG_TAG = "GPSTools";

    private static final long GPS_UPDATE_INTERVAL = 1000;

    GridLayout mGridLayout;
    //FrameLayout mDragLayer;
    Space mSpace;

    ArrayList<DashboardWidget> mWidgets = new ArrayList<DashboardWidget>();
    ArrayList<DashboardWidget> mSelectedWidgets = new ArrayList<DashboardWidget>();

    private DatabaseHelper mDatabase;

    private ActionMode mActionMode;

    private MenuItem mEditItem;
    private MenuItem mRecordItem;

    private int mWidgetWidth;
    private int mWidgetHeight;

    public static final String EXTRA_COLS = "cols";
    public static final String EXTRA_ROWS = "rows";
    public static final String EXTRA_UNITS = "units";
    public static final String EXTRA_TRACK = "track";

    private static final int DIALOG_ADD = 2;
    private static final int DIALOG_EDIT = 1;

    private Track mCurrentTrack;

    private boolean mRecording;
    private boolean mIsBound;
    private TrackService mTrackService;
    private final String[] navigationLabels = new String[]{"Dashboard","Tracks"};

    public DashboardActivity() {
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Log.d(LOG_TAG, "DashboardActivity onCreate");

		setContentView(R.layout.activity_dashboard);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mDatabase = new DatabaseHelper(this);

        mGridLayout = (GridLayout)findViewById(R.id.grid_dashboard);

        mGridLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mActionMode != null){
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


        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        ArrayAdapter<String> nav = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, navigationLabels);
        nav.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(nav, this);
        setTitle("");

        android.graphics.Point size = new android.graphics.Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int screenWidth = size.x - 16;
        int mColumnCount = mGridLayout.getColumnCount();
        mWidgetWidth = screenWidth / mColumnCount;
        mWidgetHeight = (int)(90f * getResources().getDisplayMetrics().density);

        //mCurrentTrack = new Track(this);
        doBindService();

        loadWidgets();
        if(mWidgets.size() == 0){
            addDefaultWidgets();
        }
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

    @Override
	protected void onPause() {
		super.onPause();
        Log.d(LOG_TAG, "onPause()");

        for(DashboardWidget widget : mWidgets){
            widget.onPause();
        }
	}

	@Override
	protected void onResume() {
		super.onResume();
        Log.d(LOG_TAG, "onResume()");

        ActionBar actionBar = getSupportActionBar();
        actionBar.setSelectedNavigationItem(0);

        for(DashboardWidget widget : mWidgets){
            widget.onResume();
        }
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy()");
        doUnbindService();
        if(isFinishing()){
            Log.d(LOG_TAG, "isFinishing");
            stopService(new Intent(this, TrackService.class));
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

        DashboardWidget widget = new DashboardWidget(this, widgetType, unitsType);

        GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED, colSpan);
        GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED, rowSpan);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
        params.width = mWidgetWidth * colSpan;
        params.height = mWidgetHeight * rowSpan;

        Log.d(LOG_TAG, "Widget Added: " + widgetType + " (" + colSpan + " x " + rowSpan + ")");

        widget.setLayoutParams(params);

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
    }

    public Track getCurrentTrack(){
        return mTrackService.getTrack();
        //return mCurrentTrack;
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

    private void saveWidgets(){
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(DatabaseHelper.WIDGET_TABLE_NAME, null, null);

            ContentValues values;

            int i = 0;
            int l = mGridLayout.getChildCount();
            for(;i<l;i++){
                DashboardWidget widget = (DashboardWidget)mGridLayout.getChildAt(i);
                values = new ContentValues();
                values.put(DatabaseHelper.ORDER_COLUMN, i);
                values.put(DatabaseHelper.TYPE_COLUMN, widget.getWidgetType());
                values.put(DatabaseHelper.WIDTH_COLUMN, getWidgetColsSpan(widget));
                values.put(DatabaseHelper.HEIGHT_COLUMN, getWidgetRowsSpan(widget));
                values.put(DatabaseHelper.UNITS_COLUMN, widget.getUnits());
                db.insert(DatabaseHelper.WIDGET_TABLE_NAME, DatabaseHelper.ID_COLUMN, values);
            }

            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();

            db.close();
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.dashboard, menu);
        mRecordItem = menu.findItem(R.id.action_record);
        if(mRecording){
            setRecording(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_add:
                showDialog(DIALOG_ADD);
                break;
            case R.id.action_record:
                if(mRecording){
                    mTrackService.stopLogging();
                    Intent intent = new Intent(this, TrackDetailActivity.class);
                    intent.putExtra(EXTRA_TRACK, mCurrentTrack.getID());
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
        return false;
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


    private int mDragStartIndex;
    private View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {

            if(mActionMode != null){
                mActionMode.finish();
            }

            mDragStartIndex = mGridLayout.indexOfChild(view);
            mGridLayout.addView(mSpace, mDragStartIndex);
            view.startDrag(null, new View.DragShadowBuilder(view), view, 0);
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            mGridLayout.removeView(view);

            return true;
        }
    };

    private View.OnClickListener mClickListener = new View.OnClickListener() {
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
                        mLastIndex = mDragStartIndex;
                    }

                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:

                    if(!v.equals(mSpace)){
                        final int index = mGridLayout.indexOfChild(v);
                        mGridLayout.removeView(mSpace);
                        mGridLayout.addView(mSpace, index);
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
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

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if(itemPosition == 1){
            Intent intent = new Intent(this, TrackListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        return true;
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
        Log.d(LOG_TAG, "Service Bound");

    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            Log.d(LOG_TAG, "Service Unbound");
        }
    }
}
