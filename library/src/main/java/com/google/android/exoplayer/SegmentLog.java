package com.google.android.exoplayer;

import java.io.Serializable;

/**
 * Created by lleej on 2016-12-01.
 */

public class SegmentLog{
    public int index;
    public String requestedTime;
    public String loadDuration;
    public double bitrate;

    public SegmentLog(int index, int bitrate, String requestedTime){
        this.index = index;
        this.bitrate = bitrate/1000.0;
        this.requestedTime = requestedTime;
    }
    public SegmentLog(int index, double bitrate, String requestedTime){
        this.index = index;
        this.bitrate = bitrate;
        this.requestedTime = requestedTime;
    }
}
