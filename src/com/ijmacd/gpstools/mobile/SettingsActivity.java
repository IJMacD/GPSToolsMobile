package com.ijmacd.gpstools.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

public class SettingsActivity extends SherlockPreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private String mPreferenceUnitsKey;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Resources res = getResources();
        mPreferenceUnitsKey = res.getString(R.string.pref_units_key);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        addPreferencesFromResource(R.xml.preferences);

        setUnitsSummary();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(mPreferenceUnitsKey)){
            setUnitsSummary();
        }
    }

    private void setUnitsSummary() {
        final Resources res = getResources();
        Preference defaultUnits = findPreference(mPreferenceUnitsKey);
        String[] labels = res.getStringArray(R.array.pref_unit_types);
        String[] values = res.getStringArray(R.array.pref_unit_values);
        String value = mSharedPreferences.getString(mPreferenceUnitsKey, res.getInteger(R.integer.default_unit)+"");
        for(int i = values.length - 1; i >= 0; i--){
            if(value.equals(values[i])){
                defaultUnits.setSummary(labels[i]);
            }
        }
    }
}
