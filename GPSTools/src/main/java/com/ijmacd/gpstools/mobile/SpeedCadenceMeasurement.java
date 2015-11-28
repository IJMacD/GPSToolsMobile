package com.ijmacd.gpstools.mobile;

import com.movisens.smartgattlib.GattByteBuffer;

/**
 * Created by Iain on 28/11/2015.
 */
public class SpeedCadenceMeasurement {
    private static final double WHEEL_DIAMETER = 0.7d;
    public final Long cumulativeWheelRevolutions;
    public final float lastWheelEventTime;
    public final int cumulativeCrankRevolutions;
    public final float lastCrankEventTime;

    public SpeedCadenceMeasurement(byte[] data) {
        GattByteBuffer bb = GattByteBuffer.wrap(data);

        int flags = bb.getInt8();

        if(hasWheelData(flags)){
            cumulativeWheelRevolutions = bb.getUint32();
            lastWheelEventTime = (float)bb.getUint16() / 1024.0F;
        }
        else {
            cumulativeWheelRevolutions = 0L;
            lastWheelEventTime = 0;
        }

        if (hasCrankData(flags)) {
            cumulativeCrankRevolutions = bb.getUint16();
            lastCrankEventTime = bb.getUint16() / 1024.0F;
        }
        else {
            cumulativeCrankRevolutions = 0;
            lastCrankEventTime = 0;
        }
    }

    private boolean hasWheelData(int flags) {
        return (flags & 1) != 0;
    }

    private boolean hasCrankData(int flags) {
        return (flags & 2) != 0;
    }
}
