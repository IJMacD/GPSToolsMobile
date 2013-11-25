package com.ijmacd.gpstools.mobile;

import android.content.Context;
import android.graphics.*;
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class DragLayer extends FrameLayout {

    private final Context mContext;
    private AppWidgetResizeFrame mWidgetResizeFrame;
    private int mXDown;
    private int mYDown;
    private boolean mResizing = false;

    public DragLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (handleTouchDown(ev, true)) {
                return true;
            }
        }
        //removeResizeFrame();
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            if(handleTouchDown(event, false)){
                return true;
            }
        }

        int action = event.getAction();

        if(mWidgetResizeFrame != null && mResizing){
            int x = (int)event.getX();
            int y = (int)event.getY();
            switch (action){
                case MotionEvent.ACTION_MOVE:
                    mWidgetResizeFrame.visualizeResizeForDelta(x - mXDown, y - mYDown);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mWidgetResizeFrame.commitResizeForDelta(x - mXDown, y - mYDown);
                    mResizing = false;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private boolean handleTouchDown(MotionEvent event, boolean intercept){
        int x = (int)event.getX();
        int y = (int)event.getY();

        if(mWidgetResizeFrame != null){
            if(mWidgetResizeFrame.beginResizeIfPointInRegion(x, y)){
                mResizing = true;
                mXDown = x;
                mYDown = y;
                return true;
            }
        }
        return false;
    }

    public void addResizeFrame(DashboardWidget widget, GridLayout gridLayout){
        if(mWidgetResizeFrame != null){
            removeResizeFrame();
        }

        int screenWidth = gridLayout.getWidth() - 16;
        int columnCount = gridLayout.getColumnCount();
        int widgetWidth = screenWidth / columnCount;
        int widgetHeight = (int)(90f * getResources().getDisplayMetrics().density);
        int rowCount = gridLayout.getHeight() / widgetHeight;
        AppWidgetResizeFrame.CellParameters params =
                new AppWidgetResizeFrame.CellParameters(widgetWidth, widgetHeight, columnCount, rowCount);
        params.colSpan = widget.getWidth() / widgetWidth;
        params.rowSpan = widget.getHeight() / widgetHeight;
        params.colStart = widget.getLeft() / widgetWidth;
        params.rowStart = widget.getTop() / widgetHeight;
        mWidgetResizeFrame = new AppWidgetResizeFrame(mContext, widget, params);
        LayoutParams lp = new LayoutParams(-1, -1);
        lp.customPosition = true;
        addView(mWidgetResizeFrame, lp);
        mWidgetResizeFrame.snapToWidget(false);
    }

    public void removeResizeFrame(){
        removeView(mWidgetResizeFrame);
        mWidgetResizeFrame = null;
    }


    public static class LayoutParams extends FrameLayout.LayoutParams {
        public int x, y;
        public boolean customPosition = false;

        /**
         * {@inheritDoc}
         */
        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getWidth() {
            return width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getHeight() {
            return height;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getX() {
            return x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getY() {
            return y;
        }
    }

}
