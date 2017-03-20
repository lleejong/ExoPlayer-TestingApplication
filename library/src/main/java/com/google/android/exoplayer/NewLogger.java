package com.google.android.exoplayer;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by lleej on 2016-11-29.
 */

public class NewLogger {
    private static ArrayList<SegmentLog> segmentLogList = new ArrayList<SegmentLog>();
    private static ArrayList<ByteLog> byteLogList = new ArrayList<ByteLog>();

    public static long sessionStartTimeMs;

    public static void init(){
        segmentLogList = new ArrayList<SegmentLog>();
        byteLogList = new ArrayList<ByteLog>();
    }
    public static void addSegmentLog(SegmentLog newLog){
        segmentLogList.add(newLog);
    }
    public static void addBytesLog(int bytes, long ms){
        String endTime = getSessionTimeToStringByte(ms - sessionStartTimeMs);
        byteLogList.add(new ByteLog(endTime, bytes));
    }
    public static void updateLoadDuration(int index, long ms){
       segmentLogList.get(index - 1).loadDuration = getSessionTimeToString(ms);
    }




    public static ArrayList<SegmentLog> getSegmentLogList(){
        return segmentLogList;
    }
    public static ArrayList<ByteLog> getByteLogList(){
        return byteLogList;
    }
    private static final NumberFormat TIME_FORMAT;
    static {
        TIME_FORMAT = NumberFormat.getInstance(Locale.US);
        TIME_FORMAT.setMinimumFractionDigits(2);
        TIME_FORMAT.setMaximumFractionDigits(2);
    }

    private static final NumberFormat BYTE_TIME_FORMAT;
    static{
        BYTE_TIME_FORMAT = NumberFormat.getInstance(Locale.US);
        BYTE_TIME_FORMAT.setMinimumFractionDigits(6);
        BYTE_TIME_FORMAT.setMaximumFractionDigits(6);
    }

    private static String getSessionTimeToString(long ms){
        return TIME_FORMAT.format((ms) / 1000f);
    }
    private static String getSessionTimeToStringByte(long ms){
        return BYTE_TIME_FORMAT.format((ms)/1000f);
    }
}
