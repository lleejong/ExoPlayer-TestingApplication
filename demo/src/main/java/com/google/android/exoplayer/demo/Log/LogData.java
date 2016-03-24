package com.google.android.exoplayer.demo.Log;


import java.io.Serializable;

/**
 * Created by lleej on 2016-03-24.
 */
public class LogData implements Serializable{
    private String timestamp;
    private String log;

    public LogData(String timestamp, String log){
        this.timestamp = timestamp;
        this.log = log;
    }

    public String getTimestamp(){
        return timestamp;
    }

    public String getLog(){
        return log;
    }
}
