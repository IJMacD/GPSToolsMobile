package com.ijmacd.gpstools.mobile;

import android.location.Location;

/**
 * Created with IntelliJ IDEA.
 * User: Iain
 * Date: 27/06/13
 * Time: 22:42
 * To change this template use File | Settings | File Templates.
 */
public class Point {
    private Location mLocation;
    private double mSensorSpeed;
    private double mCadence;

    public Point(Location location) {
        mLocation = location;
    }

    public void setSensorSpeed(double speed){
        mSensorSpeed = speed;
    }

    public void setCadence(double cadence){
        mCadence = cadence;
    }

    public float distanceTo(Point point) {
        return mLocation.distanceTo(point.mLocation);
    }

    public long getTime() {
        return mLocation.getTime();
    }

    public double getLatitude() {
        return mLocation.getLatitude();
    }

    public double getLongitude() {
        return mLocation.getLongitude();
    }

    public double getAltitude() {
        return mLocation.getAltitude();
    }

    public float getAccuracy() {
        return mLocation.getAccuracy();
    }

    public float getSpeed() {
        return mLocation.getSpeed();
    }

    public float getHeading() {
        return mLocation.getBearing();
    }

    public double getSensorSpeed() { return mSensorSpeed; }

    public double getCadence() { return mCadence; }
}
