package com.ijmacd.gpstools.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private String mPreferenceUnitsKey;
    private String mPreferenceDisplayFrequencyKey;
    private String mPreferenceRecordFrequencyKey;
    private String mPreferenceOrientationKey;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Resources res = getResources();
        mPreferenceUnitsKey = res.getString(R.string.pref_units_key);
        mPreferenceDisplayFrequencyKey = res.getString(R.string.pref_display_freq_key);
        mPreferenceRecordFrequencyKey = res.getString(R.string.pref_record_freq_key);
        mPreferenceOrientationKey = res.getString(R.string.pref_orientation_key);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        addPreferencesFromResource(R.xml.preferences);

        setSummary(mPreferenceUnitsKey,
                res.getStringArray(R.array.pref_unit_types),
                res.getStringArray(R.array.pref_unit_values)
        );

        setSummary(mPreferenceDisplayFrequencyKey,
                res.getStringArray(R.array.pref_freq),
                res.getStringArray(R.array.pref_freq_values)
        );

        setSummary(mPreferenceRecordFrequencyKey,
                res.getStringArray(R.array.pref_freq),
                res.getStringArray(R.array.pref_freq_values)
        );

        setSummary(mPreferenceOrientationKey,
                res.getStringArray(R.array.pref_orientation),
                res.getStringArray(R.array.pref_orientation_values)
        );

//        ActionBar actionBar = getSupportActionBar();
//        actionBar.setDisplayHomeAsUpEnabled(true);
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
        final Resources res = getResources();
        if(key.equals(mPreferenceUnitsKey)){
            setSummary(key,
                    res.getStringArray(R.array.pref_unit_types),
                    res.getStringArray(R.array.pref_unit_values)
            );
        }
        else if(key.equals(mPreferenceDisplayFrequencyKey) ||
                key.equals(mPreferenceRecordFrequencyKey)){
            setSummary(key,
                    res.getStringArray(R.array.pref_freq),
                    res.getStringArray(R.array.pref_freq_values)
            );
        }
        else if(key.equals(mPreferenceOrientationKey)){
            setSummary(key,
                    res.getStringArray(R.array.pref_orientation),
                    res.getStringArray(R.array.pref_orientation_values)
            );
        }
    }

    private void setSummary(String key, String[] labels, String[] values) {
        final Resources res = getResources();
        Preference pref = findPreference(key);
        String value = mSharedPreferences.getString(key, "");
        for(int i = values.length - 1; i >= 0; i--){
            if(value.equals(values[i])){
                pref.setSummary(labels[i]);
            }
        }
    }
}
