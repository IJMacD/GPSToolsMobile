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

    private static final String KEY_TYPE = "type";
    private static final String KEY_NAME = "name";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_CATEGORY = "category";

    private static String ns = null;

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

        mWidgetTypes = parse(R.xml.widget_types);
        ListView list = (ListView)findViewById(android.R.id.list);
        list.setAdapter(new SimpleAdapter(mContext, mWidgetTypes,
                android.R.layout.simple_list_item_2,
                new String[]{KEY_NAME, KEY_DESCRIPTION},
                new int[]{android.R.id.text1, android.R.id.text2}));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    DashboardActivity activity = (DashboardActivity)mContext;
                    int type = Integer.parseInt(mWidgetTypes.get(position).get(KEY_TYPE));
                    activity.addWidget(type);
                    dismiss();
                }
                finally { }
            }
        });
    }

    private List parse(int resId) {
        try {
            XmlPullParser parser = mContext.getResources().getXml(resId);
            //parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.next();
            parser.next();
            return readFeed(parser);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList();
    }

    private List readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List entries = new ArrayList<Map<String, String>>();

        parser.require(XmlPullParser.START_TAG, ns, "widget_types");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("widget")) {
                entries.add(readWidget(parser));
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
// to their respective "read" methods for processing. Otherwise, skips the tag.
    private Map<String, String> readWidget(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "widget");
        String type = null;
        String name = null;
        String description = null;
        String category = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tag = parser.getName();
            if (tag.equals("type")) {
                type = readText("type", parser);
            } else if (tag.equals("name")) {
                name = readText("name", parser);
            } else if (tag.equals("description")) {
                description = readText("description", parser);
            } else if (tag.equals("category")) {
                category = readText("category", parser);
            } else {
                skip(parser);
            }
        }
        Map<String, String> widget =  new HashMap<String, String>();
        widget.put(KEY_TYPE, type);
        widget.put(KEY_NAME, name);
        widget.put(KEY_DESCRIPTION, description);
        widget.put(KEY_CATEGORY, category);
        return widget;
    }

    // Processes title tags in the feed.
    private String readText(String tagName, XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, tagName);
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, tagName);
        return title;
    }

    // For the tags title and summary, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}