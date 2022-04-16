package com.ijmacd.gpstools.mobile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import java.io.*;
import java.text.SimpleDateFormat;


/**
 * Created with IntelliJ IDEA.
 * User: Iain
 * Date: 28/06/13
 * Time: 17:21
 * To change this template use File | Settings | File Templates.
 */
public class TrackDetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = "TrackDetailActivity";
    TextView mNameText;
    TextView mStartText;
    TextView mDistanceText;
    TextView mDurationText;
    TextView mSpeedText;

    private Track mTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.track_details);

        mNameText = (TextView)findViewById(R.id.name_text);
        mStartText = (TextView)findViewById(R.id.start_text);
        mDistanceText = (TextView)findViewById(R.id.distance_text);
        mDurationText = (TextView)findViewById(R.id.duration_text);
        mSpeedText = (TextView)findViewById(R.id.speed_text);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        long trackId = intent.getLongExtra(DashboardActivity.EXTRA_TRACK, 0);

        if(trackId > 0){
            try{
                mTrack = Track.getTrack(this, trackId);
                mNameText.setText(mTrack.getName());
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                mStartText.setText(formatter.format(mTrack.getStartDate()));
                final float distance = mTrack.getDistance();
                mDistanceText.setText(String.format("%.1f km", distance/1000f));
                final int t = (int)mTrack.getDuration();
                mDurationText.setText(String.format("%d:%02d:%02d", t/3600, (t%3600)/60, t%60));
                mSpeedText.setText(String.format("%.1f km/h", distance / t * 3.6));
            }
            catch (Track.TrackException e){
                mNameText.setText(R.string.invalid_track);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.track_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_export:
                if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
                    File directory = new File("/storage/sdcard0");
                    if(!directory.isDirectory())
                        directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if(GPXExport.exportGPX(mTrack, directory)){
                        Toast.makeText(this, R.string.track_saved, Toast.LENGTH_SHORT).show();
                        Log.d(LOG_TAG, "Track exported to " + directory.getAbsolutePath());
                    }
                }
                else {
                    Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.delete_track)
                        .setMessage(R.string.confirm_delete)
                        .setCancelable(true)
                        .setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mTrack.delete();
                                NavUtils.navigateUpFromSameTask(TrackDetailActivity.this);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
