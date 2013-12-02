package com.ijmacd.gpstools.mobile;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;


/**
 * Created with IntelliJ IDEA.
 * User: Iain
 * Date: 28/06/13
 * Time: 17:20
 * To change this template use File | Settings | File Templates.
 */
public class TrackListActivity extends ActionBarActivity implements ActionBar.OnNavigationListener {

    private SimpleCursorAdapter mAdapter;
    private ActionMode mActionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.track_list);

        final ListView listView = (ListView)findViewById(R.id.track_list);

        DatabaseHelper helper = new DatabaseHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.query(
                DatabaseHelper.TRACK_TABLE_NAME,
                new String[]{
                        DatabaseHelper.ID_COLUMN,
                        DatabaseHelper.NAME_COLUMN,
                        DatabaseHelper.DATE_COLUMN,
                        DatabaseHelper.DISTANCE_COLUMN,
                        DatabaseHelper.DURATION_COLUMN,
                        DatabaseHelper.COMPLETE_COLUMN},
                null, null,
                null, null,
                DatabaseHelper.DATE_COLUMN + " DESC"
        );
        startManagingCursor(cursor);

        mAdapter = new SimpleCursorAdapter(this, R.layout.track_list_item, cursor,
                new String[]{
                        DatabaseHelper.NAME_COLUMN,
                        DatabaseHelper.DISTANCE_COLUMN,
                        DatabaseHelper.DURATION_COLUMN,
                        DatabaseHelper.COMPLETE_COLUMN
                },
                new int[]{
                        R.id.name_text,
                        R.id.distance_text,
                        R.id.duration_text,
                        R.id.error_text
                });

        mAdapter.notifyDataSetInvalidated();

        SimpleCursorAdapter.ViewBinder binder = new SimpleCursorAdapter.ViewBinder(){

            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                final TextView textView = (TextView)view;

                int index = cursor.getColumnIndex(DatabaseHelper.DISTANCE_COLUMN);
                if(columnIndex == index){
                    final float distance = cursor.getFloat(columnIndex) / 1000f;
                    textView.setText(String.format("%.1f km", distance));
                    return true;
                }

                index = cursor.getColumnIndex(DatabaseHelper.DURATION_COLUMN);
                if(columnIndex == index){
                    final int t = (int)cursor.getFloat(columnIndex);
                    textView.setText(String.format("%d:%02d:%02d", t/3600, (t%3600)/60, t%60));
                    return true;
                }

                index = cursor.getColumnIndex(DatabaseHelper.COMPLETE_COLUMN);
                if(columnIndex == index){
                    final int complete = cursor.getInt(columnIndex);
                    if(complete == 0){
                        textView.setText("Incomplete");
                        textView.setTextColor(Color.rgb(255,128,0));
                    }
                    else {
                        textView.setText("");
                    }
                    return true;
                }

                return false;
            }
        };

        mAdapter.setViewBinder(binder);

        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mActionMode != null){
                    if(listView.getCheckedItemCount() == 0){
                        mActionMode.finish();
                    }
                }
                else {
                    Intent intent = new Intent(TrackListActivity.this, TrackDetailActivity.class);
                    intent.putExtra(DashboardActivity.EXTRA_TRACK, id);
                    startActivity(intent);
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                if (mActionMode != null) {
                    return false;
                }

                // Start the CAB using the ActionMode.Callback defined above
                mActionMode = startSupportActionMode(mActionModeCallback);
                //view.setSelected(true);
                listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
                listView.setItemChecked(position, true);
                //mSelectedItems.add(id);

                return true;
            }
        });

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.navigation_labels, R.layout.spinner_view);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(adapter, this);

        setTitle("");
    }
    @Override
    protected void onResume(){
        super.onResume();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setSelectedNavigationItem(1);
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if(itemPosition == 0){
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        return true;
    }


    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.action_track_list, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is java.lang.Objectinvalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    deleteSelectedItems();
                    mAdapter.notifyDataSetChanged();
                    mAdapter.getCursor().requery();
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
            ListView listView = (ListView)findViewById(R.id.track_list);
            //listView.clearChoices();
            listView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
            listView.setAdapter(mAdapter);
        }
    };

    private void deleteSelectedItems() {
        ListView listView = (ListView)findViewById(R.id.track_list);
        long[] ids = listView.getCheckedItemIds();
        DatabaseHelper helper = new DatabaseHelper(this);
        SQLiteDatabase db = helper.getWritableDatabase();
//        db.delete(DatabaseHelper.TRACK_TABLE_NAME, DatabaseHelper.ID_COLUMN + " IN (?)",
//                new String[]{ join(",", ids)});
        for(int i = 0; i < ids.length; i++){
            db.delete(DatabaseHelper.TRACK_TABLE_NAME, DatabaseHelper.ID_COLUMN + " = ?",
                    new String[] { String.valueOf(ids[i]) });
        }
        db.close();
    }

    private String join(String glue, long[] array){
        StringBuilder builder = new StringBuilder();
        builder.append(array[0]);
        for(int i = 1; i < array.length; i++){
            builder.append(glue).append(array[i]);
        }
        return builder.toString();
    }

}
