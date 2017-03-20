/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer.demo;

import com.google.android.exoplayer.ByteLog;
import com.google.android.exoplayer.SegmentLog;
import com.google.android.exoplayer.demo.Log.Bytes;
import com.google.android.exoplayer.demo.Log.LogData;
import com.google.android.exoplayer.demo.Log.Score;
import com.google.android.exoplayer.demo.Log.VideoSize;
import com.google.android.exoplayer.demo.Samples.Sample;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An activity for selecting from a number of samples.
 */
public class SampleChooserActivity extends Activity implements View.OnClickListener{

  public static final int MODE_DASH = 0;
  public static final int MODE_DASH_RB_2S = 1;
  public static final int MODE_DASH_RB_4S = 2;
  public static final int MODE_DASH_RB_6S = 3;
  public static final int MODE_DASH_RB_10S = 4;
  public static final int MODE_DASH_RB_15S = 5;
  public static final int MODE_BBA = 6;
  public static final int MODE_BBA_RB_2S= 7;
  public static final int MODE_BBA_RB_4S = 8;
  public static final int MODE_BBA_RB_6S = 9;
  public static final int MODE_BBA_RB_10S = 10;
  public static final int MODE_BBA_RB_15S = 11;
  public static final int MODE_HLS = 12;
  public static final int MODE_SS = 13;
  public static final int MODE_DASH_ROUTINE = 14;
  public static final int MODE_BBA_ROUTINE = 15;

  public static int VIDEO_DURATION = 300;

  private TextView statusView;
  private Button startButton;
  private NumberPicker numberPicker;
  private EditText tagEdit;
  private EditText durationEdit;

  private int confirmedCount = 0;
  private int remainedCount = 0;

  private int selectedMode;
  private int routineIndex = -1;
  private String tagText;
  private Spinner s;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d("LLEEJONG","Kernel Version : " + System.getProperty("os.version"));
    setContentView(R.layout.testing);
    numberPicker = (NumberPicker)findViewById(R.id.numberpicker);

    numberPicker.setMinValue(1);
    numberPicker.setMaxValue(30);
    numberPicker.setValue(1);

    statusView = (TextView) findViewById(R.id.statusView);


    startButton = (Button) findViewById(R.id.startButton);
    startButton.setOnClickListener(this);




    tagEdit = (EditText) findViewById(R.id.tagEdit);

    final String[] items = {"DASH", "DASH_RB_2S", "DASH_RB_4S","DASH_RB_6S","DASH_RB_10S", "DASH_RB_15S", "BBA", "BBA_RB_2S","BBA_RB_4S", "BBA_RB_6S","BBA_RB_10S", "BBA_RB_15S", "HLS", "SS", "DASH_ROUTINE", "BBA_ROUTINE"};

    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner,items);
    s = (Spinner)findViewById(R.id.spinner);
    s.setAdapter(adapter);

    durationEdit = (EditText) findViewById(R.id.durationEdit);
    durationEdit.setText(VIDEO_DURATION + "");


    BluetoothAdapter mBTAdapter = BluetoothAdapter.getDefaultAdapter();


    Set<BluetoothDevice> pairedDevices = mBTAdapter.getBondedDevices();
    if(pairedDevices.size() > 0)
      Configure.BT_ON = true;


    Log.d("LLEEJ", Configure.BT_ON + "");

    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    StrictMode.setThreadPolicy(policy);

  }



  private void updateStatusView(String str){
    statusView.append(str + "\n");
  }

  private void deleteTempBytesDataFiles(){
    File baseDir = new File(Environment.getExternalStorageDirectory() + "/DASH_LOG/tmpBytesData");
    File[] list = baseDir.listFiles();
    if(list.length > 0) {
      for (int i = 0; i < list.length; i++){
        list[i].delete();
      }
    }
  }
  private void deleteTempFiles(){
    File baseDir = new File(Environment.getExternalStorageDirectory() + "/DASH_LOG/tmpByLLEEJ");
    File[] list = baseDir.listFiles();
    if(list.length > 0) {
      for (int i = 0; i < list.length; i++){
        list[i].delete();
      }
    }

  }

  private ArrayList<String> readBytesDataToDevice(){
    File baseDir = new File(Environment.getExternalStorageDirectory() + "/DASH_LOG/tmpBytesData");
    File[] list = baseDir.listFiles();

    ArrayList<File> actualFileList = new ArrayList<File>();
    ArrayList<String> wholeLogList = new ArrayList<String>();

    for(int i = 0; i < list.length; i++){
      File file = list[i];
      if(file.isFile() || file.getName().contains(".csv")){
        actualFileList.add(file);
      }
    }
    try {
      for(File readFile : actualFileList){
        BufferedReader reader = new BufferedReader(new FileReader(readFile));
        String firstLine = reader.readLine();
        wholeLogList.add(firstLine);
        String secondLine = reader.readLine();
        wholeLogList.add(secondLine);
        reader.close();
      }
      deleteTempBytesDataFiles();

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return wholeLogList;
  }

  private ArrayList<String> readLogToDevice(){
    File baseDir = new File(Environment.getExternalStorageDirectory() + "/DASH_LOG/tmpByLLEEJ");
    File[] list = baseDir.listFiles();

    ArrayList<File> actualFileList = new ArrayList<File>();
    ArrayList<String> wholeLogList = new ArrayList<String>();

    for(int i = 0; i < list.length; i++){
      File file = list[i];
      if(file.isFile() || file.getName().contains(".csv")){
        actualFileList.add(file);
      }
    }
    try {
      for(File readFile : actualFileList){
        BufferedReader reader = new BufferedReader(new FileReader(readFile));
        String firstLine = reader.readLine();
        wholeLogList.add(firstLine);
        String secondLine = reader.readLine();
        wholeLogList.add(secondLine);
        reader.close();
      }
      deleteTempFiles();

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return wholeLogList;
  }

  // DASH

  private static String convert(int size) { // System.out.println(size+"");
    ArrayList<VideoSize> availableVideoSize = EventLogger.getAvailableVideoSize();
    for(VideoSize videoSize : availableVideoSize){
      if(videoSize.getHeight() == size)
        return (videoSize.getBps() / 1000.0) + "";
    }
    return "";
  }


  public static double convert(String str) { // System.out.println(size+"");
    ArrayList<VideoSize> availableVideoSize = EventLogger.getAvailableVideoSize();
    int size = Integer.parseInt(str);
    for(VideoSize videoSize : availableVideoSize){
      if(videoSize.getHeight() == size)
        return (videoSize.getBps() / 1000.0);
    }
    return -1;

  }



  private void writeLogToDevice(){
//    if(wholeLogList.size() == 0) {
//      startButton.setEnabled(true);
//      return;
//    }

    updateStatusView("Start write files...");

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    String fileName = dateFormat.format(new Date()).toString();
    if(!tagText.equals("")){
      if(Configure.BT_COMPENSATION_TEST)
        fileName += "_"+tagText + " " + Configure.BT_COMPENSATION_PARAMETER;
      else
        fileName += "_"+tagText;
    }
    //fileName += ".csv";
    File baseDir = new File(Environment.getExternalStorageDirectory() + "/DASH_LOG");

    if(!baseDir.exists())
      baseDir.mkdirs();
    //File newFolder = new File(folderName);
    //File newFolder = new File(folderPath);
    File newFile = null;
    File segmentInfoFile = null;
    File newBytesDataFile = null;
    File newRequestDataFile = null;
    File newByteLogFile = null;

    try {

      //LogFile
      newFile = new File(baseDir.getPath()+"/"+fileName+".csv");
      newFile.createNewFile();
      PrintWriter printWriter = new PrintWriter(newFile);

      //ArrayList<ArrayList<LogData>> totalList = EventLogger.getLogDataList();

      ArrayList<ArrayList<SegmentLog>> totalList = EventLogger.getSegmentLogList();
      ArrayList<ArrayList<SegmentLog>> tempList = new ArrayList<ArrayList<SegmentLog>>();

      for(ArrayList<SegmentLog> segmentLogList : totalList){
        SegmentLog pre = null;
        ArrayList<SegmentLog> list = new ArrayList<SegmentLog>();
        for(SegmentLog log : segmentLogList){
          if(pre != null){
            if(pre.bitrate != log.bitrate){
              list.add(pre);
              list.add(new SegmentLog(-1, pre.bitrate,log.requestedTime));
              list.add(log);
            }
          }
          pre = log;
        }
        tempList.add(list);
      }
      int cursor = 0;
      HashSet<Integer> completedSet = new HashSet<Integer>();
      while(true){
        String line = "";
        if(completedSet.size() == tempList.size())
          break;
        for(int i = 0; i < tempList.size(); i ++){
          if(completedSet.contains(i)){
            line += ",,";
            continue;
          }
          if(tempList.get(i).size() == cursor){
            completedSet.add(i);
            line += ",,";
            continue;
          }
          SegmentLog log = tempList.get(i).get(cursor);
          line += log.requestedTime + "," + log.bitrate + ",";
        }
        line = line.substring(0, line.length() - 1);
        printWriter.println(line);
        cursor++;
      }
      printWriter.close();

      segmentInfoFile = new File(baseDir.getPath()+"/"+fileName+"_Summary.csv");
      segmentInfoFile.createNewFile();

      printWriter = new PrintWriter(segmentInfoFile);
      ArrayList<ArrayList<SegmentLog>> segmentLogList = EventLogger.getSegmentLogList();

      int cursor1 = 0;
      HashSet<Integer> completedSet1 = new HashSet<Integer>();
      while(true){
        String line = "";
        if(completedSet1.size() == segmentLogList.size())
          break;
        for(int i = 0; i < segmentLogList.size(); i ++){
          if(completedSet1.contains(i)){
            line += ",,,";
            continue;
          }
          if(segmentLogList.get(i).size() == cursor1){
            completedSet1.add(i);
            line += ",,,";
            continue;
          }
          SegmentLog log = segmentLogList.get(i).get(cursor1);
          line += log.requestedTime + "," + log.bitrate + "," + ",";
       }
        line = line.substring(0, line.length() - 1);
        printWriter.println(line);
        cursor1++;
      }

      printWriter.println();
      printWriter.println();

      String avgLine = "Avg.Bitrate,";
      String varLine = "Var.Bitrate,";
      String numSwitchingLine = "Num.Switching,";
      String magSwitchingLine = "Mag.Switching,";
      String startupDelayLine = "StartUp Delay,";
      String numRebufferingLine = "Num.Rebuffering,";
      String durRebufferingLine = "Dur.Rebuffering,";

      ArrayList<Score> scores = EventLogger.getScoreList();

      for(int i = 0; i < scores.size(); i++){
        avgLine += scores.get(i).avgBitrate + ",";
        varLine += scores.get(i).varBitrate + ",";
        numSwitchingLine += scores.get(i).numSwitching + ",";
        magSwitchingLine += scores.get(i).magSwitching + ",";
        startupDelayLine += scores.get(i).startupDelay + ",";
        numRebufferingLine += scores.get(i).numRebuffering + ",";
        durRebufferingLine += scores.get(i).durationRebuffering + ",";
      }
      avgLine = avgLine.substring(0, avgLine.length() - 1);
      varLine = varLine.substring(0, varLine.length() - 1);
      numSwitchingLine = numSwitchingLine.substring(0, numSwitchingLine.length() - 1);
      magSwitchingLine = magSwitchingLine.substring(0, magSwitchingLine.length() - 1);
      startupDelayLine = startupDelayLine.substring(0, startupDelayLine.length() - 1);
      numRebufferingLine = numRebufferingLine.substring(0, numRebufferingLine.length() - 1);
      durRebufferingLine = durRebufferingLine.substring(0, durRebufferingLine.length() - 1);

      printWriter.println(avgLine);
      printWriter.println(varLine);
      printWriter.println(numSwitchingLine);
      printWriter.println(magSwitchingLine);
      printWriter.println(startupDelayLine);
      printWriter.println(numRebufferingLine);
      printWriter.println(durRebufferingLine);

      printWriter.close();

      newByteLogFile = new File(baseDir.getPath()+"/"+fileName+"_Byte.csv");
      newByteLogFile.createNewFile();

      printWriter = new PrintWriter(newByteLogFile);
      ArrayList<ArrayList<ByteLog>> newByteLogList = EventLogger.getBytesLogList();

      int cursor2 = 0;
      HashSet<Integer> completedSet2 = new HashSet<Integer>();
      while(true){
        String line = "";
        if(completedSet2.size() == newByteLogList.size())
          break;
        for(int i = 0; i < newByteLogList.size(); i ++){
          if(completedSet2.contains(i)){
            line += ",,,";
            continue;
          }
          if(newByteLogList.get(i).size() == cursor2){
            completedSet2.add(i);
            line += ",,,";
            continue;
          }
          ByteLog log = newByteLogList.get(i).get(cursor2);
          line += log.endTime + "," + log.bytes + "," + ",";
        }
        line = line.substring(0, line.length() - 1);
        printWriter.println(line);
        cursor2++;
      }
      printWriter.close();

      //Bytes Data

//      if(Configure.LOGGING_BYTES_DATA) {
//        newBytesDataFile = new File(baseDir.getPath() + "/" + fileName + "_bytesData.csv");
//        newBytesDataFile.createNewFile();
//        PrintWriter bytesDataPrintWriter = new PrintWriter(newBytesDataFile);
//
//        //ArrayList<String> wholeBytesDataList = readBytesDataToDevice();
//
//        //ArrayList<ArrayList<Bytes>> totalBytesDataList = new ArrayList<ArrayList<Bytes>>();
//        ArrayList<ArrayList<Bytes>> totalBytesDataList = EventLogger.getBytesDataList();
//
//        String bytesDataFileHeader = "";
//        for (int i = 0; i < totalBytesDataList.size(); i++) {
//          bytesDataFileHeader += "Time(s),Bytes,";
//        }
//        bytesDataFileHeader = bytesDataFileHeader.substring(0, bytesDataFileHeader.length() - 1);
//        bytesDataPrintWriter.println(bytesDataFileHeader);
//
//        int cursor = 0;
//        int completed = 0;
//        while (true) {
//          String line = "";
//          completed = 0;
//          for (int i = 0; i < totalBytesDataList.size(); i++) {
//            if (totalBytesDataList.get(i).size() <= cursor) {
//              line += ",,";
//              completed++;
//            } else {
//              Bytes bytes = totalBytesDataList.get(i).get(cursor);
//              line += bytes.getTimestamp() + "," + bytes.getBytes() + ",";
//            }
//          }
//          if(completed == totalBytesDataList.size())
//            break;
//          line = line.substring(0, line.length() - 1);
//          bytesDataPrintWriter.println(line);
//          cursor++;
//        }
//
//        bytesDataPrintWriter.close();
//      }



      updateStatusView("End write files...");


    } catch (IOException e) {
      e.printStackTrace();
    }

    updateStatusView("Start transfer files to Server...");



    if(Configure.LOGGING_SERVER) {
      try {
        Socket socket = new Socket("192.9.81.145", 9999);
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.writeUTF(fileName + ".csv");
        Log.d("LLEEJ", " Try to send" + fileName + ".csv");
        dos.writeUTF(newFile.length() + "");
        Log.d("LLEEJ", "FileSize : " + newFile.length());

        InputStream fis = new FileInputStream(newFile);
        BufferedInputStream bis = new BufferedInputStream(fis);

        Log.d("LLEEJ", "Sending...");

        byte[] buff = new byte[4096];
        int len = 0;
        while ((len = bis.read(buff)) > 0) {
          dos.write(buff, 0, len);
        }

        dos.flush();
        dos.close();
        bis.close();
        fis.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
//      if (Configure.LOGGING_BYTES_DATA) {
//        try {
//          Socket socket = new Socket("192.9.81.145", 9999);
//          DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//          dos.writeUTF(fileName + "_bytesData.csv");
//          Log.d("LLEEJ", " Try to send" + fileName + "_bytesData.csv");
//          dos.writeUTF(newBytesDataFile.length() + "");
//          Log.d("LLEEJ", "FileSize : " + newBytesDataFile.length());
//
//          InputStream fis = new FileInputStream(newBytesDataFile);
//          BufferedInputStream bis = new BufferedInputStream(fis);
//
//          Log.d("LLEEJ", "Sending...");
//
//          byte[] buff = new byte[4096];
//          int len = 0;
//          while ((len = bis.read(buff)) > 0) {
//            dos.write(buff, 0, len);
//          }
//
//          dos.flush();
//          dos.close();
//          bis.close();
//          fis.close();
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//      }
      try {
        Socket socket = new Socket("192.9.81.145", 9999);
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.writeUTF(fileName + "_Summary.csv");
        Log.d("LLEEJ", " Try to send" + fileName + "_Summary.csv");
        dos.writeUTF(segmentInfoFile.length() + "");
        Log.d("LLEEJ", "FileSize : " + segmentInfoFile.length());

        InputStream fis = new FileInputStream(segmentInfoFile);
        BufferedInputStream bis = new BufferedInputStream(fis);

        Log.d("LLEEJ", "Sending...");

        byte[] buff = new byte[4096];
        int len = 0;
        while ((len = bis.read(buff)) > 0) {
          dos.write(buff, 0, len);
        }

        dos.flush();
        dos.close();
        bis.close();
        fis.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
//      try {
//        Socket socket = new Socket("192.9.81.145", 9999);
//        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//        dos.writeUTF(fileName + "_Byte.csv");
//        Log.d("LLEEJ", " Try to send" + fileName + "_Byte.csv");
//        dos.writeUTF(newByteLogFile.length() + "");
//        Log.d("LLEEJ", "FileSize : " + newByteLogFile.length());
//
//        InputStream fis = new FileInputStream(newByteLogFile);
//        BufferedInputStream bis = new BufferedInputStream(fis);
//
//        Log.d("LLEEJ", "Sending...");
//
//        byte[] buff = new byte[4096];
//        int len = 0;
//        while ((len = bis.read(buff)) > 0) {
//          dos.write(buff, 0, len);
//        }
//
//        dos.flush();
//        dos.close();
//        bis.close();
//        fis.close();
//      } catch (IOException e) {
//        e.printStackTrace();
//      }
    }




    updateStatusView("Success Testing");
    startButton.setEnabled(true);
    EventLogger.init();

  }

  private void handleAfterTesting(ArrayList<LogData> logList) {
    updateStatusView(confirmedCount - remainedCount + " testing is ended.");
    Log.d("LLEEJ", confirmedCount - remainedCount + " testing is ended.");
    //wholeLogList.add(logList);
    if(remainedCount == 0) {
      writeLogToDevice();
      if(routineIndex != -1){
        if(routineIndex != MODE_DASH_RB_15S && routineIndex != MODE_BBA_RB_15S){
          routineIndex++;
          selectedMode = routineIndex;
          remainedCount = confirmedCount;
          executeTest();
        }
        else{
          confirmedCount = 0;
          tagText = "";
        }
      }
      else {
        confirmedCount = 0;
        tagText = "";
      }

    }
    else
      executeTest();
  }

  private void onSampleSelected(Sample sample) {
    Intent mpdIntent = new Intent(this, PlayerActivity.class)
        .setData(Uri.parse(sample.uri))
        .putExtra(PlayerActivity.CONTENT_ID_EXTRA, sample.contentId)
        .putExtra(PlayerActivity.CONTENT_TYPE_EXTRA, sample.type)
        .putExtra(PlayerActivity.PROVIDER_EXTRA, sample.provider);
    startActivity(mpdIntent);
  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data){
    super.onActivityResult(requestCode,resultCode, data);
    Log.d("LLEEJ", "confirmendCount : " + confirmedCount);
    Log.d("LLEEJ", "remainedCount : " + remainedCount);
    Log.d("LLEEJ", confirmedCount- remainedCount +" TEST doned");
    if(data == null){
      remainedCount = 0;
      writeLogToDevice();
    }
    else {
      ArrayList<LogData> logList = (ArrayList<LogData>) data.getSerializableExtra("log");
      handleAfterTesting(logList);
    }
  }

  private void executeTest(){
    remainedCount--;

    Log.d("LLEEJ", "confirmendCount : " + confirmedCount);
    Log.d("LLEEJ", "remainedCount : " + remainedCount);
    Log.d("LLEEJ", confirmedCount- remainedCount +" TEST start");

    Sample sample = null;

    Log.d("LLEEJ","selected Mode " + selectedMode );

    if(selectedMode == SampleChooserActivity.MODE_DASH_ROUTINE || selectedMode == SampleChooserActivity.MODE_BBA_ROUTINE){
      switch(selectedMode){
        case SampleChooserActivity.MODE_DASH_ROUTINE:
          routineIndex = MODE_DASH_RB_2S;
          selectedMode = MODE_DASH_RB_2S;
          break;
        case SampleChooserActivity.MODE_BBA_ROUTINE:
          routineIndex = MODE_BBA_RB_2S;
          selectedMode = MODE_BBA_RB_2S;
          break;
      }
    }

    Log.d("LLEEJ","selected Mode " + selectedMode );

      switch (selectedMode) {
        case SampleChooserActivity.MODE_DASH:
        case SampleChooserActivity.MODE_BBA:

          sample = Samples.YOUTUBE_DASH_MP4[5];
          break;
        case SampleChooserActivity.MODE_DASH_RB_2S:
        case SampleChooserActivity.MODE_BBA_RB_2S:
          sample = Samples.YOUTUBE_DASH_MP4[0];
          break;
        case SampleChooserActivity.MODE_DASH_RB_4S:
        case SampleChooserActivity.MODE_BBA_RB_4S:
          sample = Samples.YOUTUBE_DASH_MP4[1];
          break;
        case SampleChooserActivity.MODE_DASH_RB_6S:
        case SampleChooserActivity.MODE_BBA_RB_6S:
          sample = Samples.YOUTUBE_DASH_MP4[2];
          break;
        case SampleChooserActivity.MODE_DASH_RB_10S:
        case SampleChooserActivity.MODE_BBA_RB_10S:
          sample = Samples.YOUTUBE_DASH_MP4[3];
          break;
        case SampleChooserActivity.MODE_DASH_RB_15S:
        case SampleChooserActivity.MODE_BBA_RB_15S:
          sample = Samples.YOUTUBE_DASH_MP4[4];
          break;
        case SampleChooserActivity.MODE_HLS:
          sample = Samples.HLS[5];
          break;
        case SampleChooserActivity.MODE_SS:
          sample = Samples.SMOOTHSTREAMING[0];
          break;
        default:
          sample = null;
          break;
      }
    executeTest(sample);
  }
  private void executeTest(Sample sample){
    Log.d("LLEEJ", "confirmendCount : " + confirmedCount);
    Log.d("LLEEJ", "remainedCount : " + remainedCount);
    Log.d("LLEEJ", confirmedCount- remainedCount +" TEST start");

    Intent mpdIntent = new Intent(this, PlayerActivity.class)
            .setData(Uri.parse(sample.uri))
            .putExtra(PlayerActivity.CONTENT_ID_EXTRA, sample.contentId)
            .putExtra(PlayerActivity.CONTENT_TYPE_EXTRA, sample.type)
            .putExtra(PlayerActivity.PROVIDER_EXTRA, sample.provider)
            .putExtra("id", confirmedCount - remainedCount)
            .putExtra("tag", tagText)
            .putExtra("mode", selectedMode);


    updateStatusView(confirmedCount - remainedCount + " testing is started.");
    Log.d("LLEEJ",confirmedCount - remainedCount + " testing is started.");
    startActivityForResult(mpdIntent,0);
  }

  @Override
  public void onClick(View v) {

    statusView.setText("");

    startButton.setEnabled(false);
    confirmedCount = numberPicker.getValue();
    remainedCount = confirmedCount;

    tagText = tagEdit.getText().toString();
    VIDEO_DURATION = Integer.parseInt(durationEdit.getText().toString());


    Log.d("LLEEJ","Test " + confirmedCount);
    selectedMode = s.getSelectedItemPosition();
    executeTest();
  }

  private static final class SampleAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private final List<SampleGroup> sampleGroups;

    public SampleAdapter(Context context, List<SampleGroup> sampleGroups) {
      this.context = context;
      this.sampleGroups = sampleGroups;
    }

    @Override
    public Sample getChild(int groupPosition, int childPosition) {
      return getGroup(groupPosition).samples.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
      return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
        View convertView, ViewGroup parent) {
      View view = convertView;
      if (view == null) {
        view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent,
            false);
      }
      ((TextView) view).setText(getChild(groupPosition, childPosition).name);
      return view;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
      return getGroup(groupPosition).samples.size();
    }

    @Override
    public SampleGroup getGroup(int groupPosition) {
      return sampleGroups.get(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
      return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
        ViewGroup parent) {
      View view = convertView;
      if (view == null) {
        view = LayoutInflater.from(context).inflate(R.layout.sample_chooser_inline_header, parent,
            false);
      }
      ((TextView) view).setText(getGroup(groupPosition).title);
      return view;
    }

    @Override
    public int getGroupCount() {
      return sampleGroups.size();
    }

    @Override
    public boolean hasStableIds() {
      return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
      return true;
    }

  }

  private static final class SampleGroup {

    public final String title;
    public final List<Sample> samples;

    public SampleGroup(String title) {
      this.title = title;
      this.samples = new ArrayList<>();
    }

    public void addAll(Sample[] samples) {
      Collections.addAll(this.samples, samples);
    }

  }

}
