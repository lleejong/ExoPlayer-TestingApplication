package com.google.android.exoplayer.demo.player;

import android.os.Handler;

import com.google.android.exoplayer.demo.PlayerActivity;

/**
 * Created by lleej on 2016-04-28.
 */
public class SessionTimer extends Thread{
    private Handler mHandler = new Handler();
    private DemoPlayer player;
    private OnSessionTimeListener listener;

    public interface OnSessionTimeListener{
        public void onSessionTimeEnded();
    }

    public SessionTimer(DemoPlayer player, OnSessionTimeListener listener){
        this.player = player;
        this.listener = listener;

    }

    public void run(){
        while(!Thread.currentThread().isInterrupted()) {
            double timeMs = player.getCurrentPosition() / 1000f;
            if (timeMs > 136) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onSessionTimeEnded();
                    }
                });
                break;
            }
        }
    }
}