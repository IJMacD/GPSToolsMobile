package com.ijmacd.gpstools.mobile;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Iain on 03/12/13.
 */
public class WidgetTypesHelper {

    private final Context mContext;

    private Map<Integer, WidgetDescription> mWidgetList;
    private List<Map<String, String>> mWidgetMap;

    private static String ns = null;

    public static final String KEY_TYPE = "type";
    public static final String KEY_NAME = "name";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_CATEGORY = "category";

    public WidgetTypesHelper(Context context) {
        mContext = context;
    }

    public Map<Integer, WidgetDescription> getWidgetTypes() {
        if(mWidgetList == null){
            mWidgetList = parse(R.xml.widget_types);
        }
        return mWidgetList;
    }

    public List<WidgetDescription> getWidgetTypesList() {
        if(mWidgetList == null){
            mWidgetList = parse(R.xml.widget_types);
        }
        return new ArrayList<WidgetDescription>(mWidgetList.values());
    }

    public List<Map<String, String>> getWidgetTypesMap() {
        if(mWidgetList == null){
            mWidgetList = parse(R.xml.widget_types);
        }
        return mWidgetMap;
    }

    public WidgetDescription getWidget(int type) {
        if(mWidgetList == null){
            mWidgetList = parse(R.xml.widget_types);
        }
        return mWidgetList.get(type);
    }

    private Map<Integer, WidgetDescription> parse(int resId) {
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
        return new HashMap();
    }

    private Map<Integer, WidgetDescription> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {

        // Next two lines are opposite in convention
        Map<Integer, WidgetDescription> entries = new HashMap<Integer, WidgetDescription>();
        mWidgetMap = new ArrayList<Map<String, String>>();

        parser.require(XmlPullParser.START_TAG, ns, "widget_types");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("widget")) {
                WidgetDescription widget = readWidget(parser);
                entries.put(widget.type, widget);
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
// to their respective "read" methods for processing. Otherwise, skips the tag.
    private WidgetDescription readWidget(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "widget");
        WidgetDescription widget = new WidgetDescription();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tag = parser.getName();
            if (tag.equals("type")) {
                widget.type = Integer.parseInt(readText("type", parser));
            } else if (tag.equals("name")) {
                widget.name = readText("name", parser);
            } else if (tag.equals("description")) {
                widget.description = readText("description", parser);
            } else if (tag.equals("category")) {
                widget.category = readText("category", parser);
            } else {
                skip(parser);
            }
        }

        // Keep a copy in this format for SimpleCursorAdapter
        Map<String, String> widgetMap =  new HashMap<String, String>();
        widgetMap.put(KEY_TYPE, String.valueOf(widget.type));
        widgetMap.put(KEY_NAME, widget.name);
        widgetMap.put(KEY_DESCRIPTION, widget.description);
        widgetMap.put(KEY_CATEGORY, widget.category);
        mWidgetMap.add(widgetMap);

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

    public static class WidgetDescription {
        public int type;
        public String name;
        public String description;
        public String category;
    }
}
