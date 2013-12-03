package com.ijmacd.gpstools.mobile;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.*;

public class AddWidgetDialog extends Dialog {

    Context mContext;
    List<Map<String, String>> mWidgetTypes;

    public AddWidgetDialog(Context context) {
        super(context);
        mContext = context;

//        Map<String, String> widget = new HashMap<String, String>();
//        widget.put(KEY_TYPE, "16");
//        widget.put(KEY_NAME, "Speed");
//        widget.put(KEY_DESCRIPTION, "Current Speed");
//        widget.put(KEY_CATEGORY, "gps");
//        mWidgetTypes.add(widget);
//        widget = new HashMap<String, String>();
//        widget.put(KEY_TYPE, "1");
//        widget.put(KEY_NAME, "Date");
//        widget.put(KEY_DESCRIPTION, "Most recent GPS Date");
//        widget.put(KEY_CATEGORY, "gps");
//        mWidgetTypes.add(widget);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(android.R.layout.list_content);

        setTitle(R.string.add_widget_title);

        WidgetTypesHelper typesHelper = new WidgetTypesHelper(mContext);
        mWidgetTypes = typesHelper.getWidgetTypesMap();
        ListView list = (ListView)findViewById(android.R.id.list);
        list.setAdapter(new SimpleAdapter(mContext, mWidgetTypes,
                android.R.layout.simple_list_item_2,
                new String[]{WidgetTypesHelper.KEY_NAME, WidgetTypesHelper.KEY_DESCRIPTION},
                new int[]{android.R.id.text1, android.R.id.text2}));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    DashboardActivity activity = (DashboardActivity)mContext;
                    int type = Integer.parseInt(mWidgetTypes.get(position).get(WidgetTypesHelper.KEY_TYPE));
                    activity.addWidget(type);
                    dismiss();
                }
                finally { }
            }
        });
    }
}