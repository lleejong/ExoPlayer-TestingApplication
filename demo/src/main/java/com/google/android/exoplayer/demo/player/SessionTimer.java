package com.google.android.exoplayer.demo.player;

import android.os.Handler;
import android.util.Log;

import com.google.android.exoplayer.demo.CompetingFlowClient;
import com.google.android.exoplayer.demo.Configure;
import com.google.android.exoplayer.demo.PlayerActivity;

/**
 * Created by lleej on 2016-04-28.
 */
public class SessionTimer extends Thread{
    private Handler mHandler = new Handler();
    private DemoPlayer player;
    private OnSessionTimeListener listener;
    private CompetingFlowClient cfClient;
    private boolean msgFlag = true;

    public interface OnSessionTimeListener{
        public void onSessionTimeEnded();
    }

    public SessionTimer(DemoPlayer player, OnSessionTimeListener listener, CompetingFlowClient cfClient){
        this.player = player;
        this.listener = listener;
        this.cfClient = cfClient;

    }

    public void run(){
        while(!Thread.currentThread().isInterrupted()) {
            double timeMs = player.getCurrentPosition() / 1000f;
            if(Configure.COMPETING_FLOW_EXPERI) {
                if ((timeMs > Configure.T) && msgFlag) {
                    Log.d("LLEEJ", "RUN");
                    cfClient.sendMessage(CompetingFlowClient.MSG_TYPE_IPERF_START);
                    msgFlag = false;
                }
            }

            if (timeMs > 136) {
                if(Configure.COMPETING_FLOW_EXPERI)
                    cfClient.sendMessage(CompetingFlowClient.MSG_TYPE_IPERF_END);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onSessionTimeEnded();
                    }
                });
                msgFlag = true;
                break;
            }
        }
    }
}