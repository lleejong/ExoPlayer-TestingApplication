package com.google.android.exoplayer.demo.Log;

/**
 * Created by lleej on 2016-07-13.
 */
public class Score {
    public int numSwitching;
    public int magSwitching;
    public double avgBitrate;
    public double varBitrate;
    public int numRebuffering;
    public double durationRebuffering;

    public Score(){
        numSwitching = 0;
        magSwitching = 0;
        avgBitrate = 0;
        varBitrate = 0;
        numRebuffering = 0;
        durationRebuffering = 0;
    }
}
