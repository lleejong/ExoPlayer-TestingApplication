package com.google.android.exoplayer.demo.Log;

/**
 * Created by lleej on 2016-07-27.
 */
public class Bytes {
    private String timestamp;
    private String bytes;


    public Bytes(String timestamp, String bytes){
        this.timestamp = timestamp;
        this.bytes = bytes;
    }

    public String getTimestamp(){
        return timestamp;
    }

    public String getBytes(){
        return bytes;
    }
}
