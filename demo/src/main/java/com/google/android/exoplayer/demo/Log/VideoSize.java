package com.google.android.exoplayer.demo.Log;

/**
 * Created by lleej on 2016-08-22.
 */
public class VideoSize {
    private int width;
    private int height;
    private int bps;

    public VideoSize(int width, int height, int bps){
        this.width = width;
        this.height = height;
        this.bps = bps;
    }

    public int getBps(){
        return bps;
    }
    public int getWidth(){
        return width;
    }
    public int getHeight(){
        return height;
    }
}
