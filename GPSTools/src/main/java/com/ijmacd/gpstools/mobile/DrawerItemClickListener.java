package com.ijmacd.gpstools.mobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class DrawerItemClickListener implements android.widget.AdapterView.OnItemClickListener {
    private final Activity mActivity;
    private final DrawerLayout mDrawerLayout;
    private static final Class[] Activities = new Class[]{
            DashboardActivity.class,
            TrackListActivity.class
    };

    public DrawerItemClickListener(Activity activity, DrawerLayout drawerLayout) {
        mActivity = activity;
        mDrawerLayout = drawerLayout;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        ListView drawerList = (ListView)adapterView;
        mDrawerLayout.closeDrawer(drawerList);
        drawerList.clearChoices();
        drawerList.requestLayout();
        if(position < Activities.length){
            Class cls = Activities[position];
            if(!cls.isInstance(mActivity)){
                Intent intent = new Intent(mActivity, cls);
                //                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                // API 11+, just using finish() achieves the same result as long
                // as there are never stacks more than 1 deep
                mActivity.startActivity(intent);
                mActivity.finish();
            }
        }
    }
}
