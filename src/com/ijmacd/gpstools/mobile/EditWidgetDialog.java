package com.ijmacd.gpstools.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;

public class EditWidgetDialog extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = getLayoutInflater();
        setContentView(R.layout.widget_edit);

        setTitle(R.string.edit_widget_title);

        Button discardButton = (Button)findViewById(R.id.button_discard);
        Button doneButton = (Button)findViewById(R.id.button_done);

        NumberPicker rowsPicker = (NumberPicker)findViewById(R.id.picker_rows);
        NumberPicker colsPicker = (NumberPicker)findViewById(R.id.picker_cols);

        Spinner unitsSpinner = (Spinner)findViewById(R.id.spinner_units);

        Intent intent = getIntent();
        int cols = intent.getIntExtra(DashboardActivity.EXTRA_COLS, 1);
        int rows = intent.getIntExtra(DashboardActivity.EXTRA_ROWS, 1);

        colsPicker.setValue(cols);
        rowsPicker.setValue(rows);

        rowsPicker.setMaxValue(10);
        colsPicker.setMaxValue(10);

        int units = intent.getIntExtra(DashboardActivity.EXTRA_UNITS, DashboardWidget.UNITS_DEFAULT);

        unitsSpinner.setSelection(units);

        discardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save
                finish();
            }
        });

    }

}