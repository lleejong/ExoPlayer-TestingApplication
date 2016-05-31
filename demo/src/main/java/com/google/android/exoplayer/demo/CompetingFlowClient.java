package com.google.android.exoplayer.demo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by lleej on 2016-05-11.
 */
public class CompetingFlowClient {
    public static final String SERVER_IP = "192.168.0.3";
    public static final int SERVER_PORT = 9999;

    public static final int MSG_TYPE_IPERF_START = 1;
    public static final int MSG_TYPE_IPERF_END = 2;

    public static final String MSG_IPERF_START = "CSTART";
    public static final String MSG_IPERF_END = "CEND";

    private PrintWriter writer;
    private Socket socket;

    public CompetingFlowClient(){
        setSocket();
    }

    private void setSocket(){
        try {
            socket = new Socket(SERVER_IP,SERVER_PORT);
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(int type){
        if(type == MSG_TYPE_IPERF_START)
            writer.println(MSG_IPERF_START);
        else if(type == MSG_TYPE_IPERF_END)
            writer.println(MSG_IPERF_END);
    }


}
