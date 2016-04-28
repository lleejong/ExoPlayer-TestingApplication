package com.google.android.exoplayer.demo.Log;


import java.io.Serializable;

/**
 * Created by lleej on 2016-03-24.
 */
public class BandwidthLogData implements Serializable{
    private String elapsedMs;
    private String byteAccumulates;
    private String bitrateEstimated;
    private String bitsPerSecond;

    public BandwidthLogData(String elapsedMs, String byteAccumulates, String bitrateEstimated, String bitsPerSecond){
        this.elapsedMs = elapsedMs;
        this.byteAccumulates = byteAccumulates;
        this.bitrateEstimated = bitrateEstimated;
        this.bitsPerSecond = bitsPerSecond;
    }


    public String getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(String elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public String getByteAccumulates() {
        return byteAccumulates;
    }

    public void setByteAccumulates(String byteAccumulates) {
        this.byteAccumulates = byteAccumulates;
    }

    public String getBitrateEstimated() {
        return bitrateEstimated;
    }

    public void setBitrateEstimated(String bitrateEstimated) {
        this.bitrateEstimated = bitrateEstimated;
    }

    public String getBitsPerSecond() {
        return bitsPerSecond;
    }

    public void setBitsPerSecond(String bitsPerSecond) {
        this.bitsPerSecond = bitsPerSecond;
    }
}
