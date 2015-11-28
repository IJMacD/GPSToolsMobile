package com.ijmacd.gpstools.mobile;

/**
 * Created by Iain on 28/11/2015.
 */
public class CscAnalyser {
    private double mWheelSize = 0.7d; // Default 700mm

    private SpeedCadenceMeasurement prevMeasurement;
    private double mSpeed;
    private double mCadence;

    public void addData(SpeedCadenceMeasurement currMeasurement) {
        if(currMeasurement == null) return;

        if(prevMeasurement != null) {
            double wheelTimeDiff = currMeasurement.lastWheelEventTime - prevMeasurement.lastWheelEventTime;
            if (wheelTimeDiff > 0){
                long revs = currMeasurement.cumulativeWheelRevolutions - prevMeasurement.cumulativeWheelRevolutions;

                mSpeed = revs * Math.PI * mWheelSize / wheelTimeDiff;
            }else {
                mSpeed = 0;
            }

            double crankTimeDiff = currMeasurement.lastCrankEventTime - prevMeasurement.lastCrankEventTime;
            if (crankTimeDiff > 0){
                long revs = currMeasurement.cumulativeCrankRevolutions - prevMeasurement.cumulativeCrankRevolutions;

                mCadence = revs * 60d / crankTimeDiff;
            }
            else {
                mCadence = 0;
            }
        }

        prevMeasurement = currMeasurement;
    }

    public void setWheelSize(double size){
        mWheelSize = size;
    }

    public double getSpeed(){
        return mSpeed;
    }

    public double getCadence(){
        return mCadence;
    }

}
