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
import java.util.List;
import java.util.Set;

/**
 * An activity for selecting from a number of samples.
 */
public class SampleChooserActivity extends Activity implements View.OnClickListener{

  public static final int MODE_DASH = 0;
  public static final int MODE_DASH_RB_2S = 1;
  public static final int MODE_DASH_RB_4S = 2;
  public static final int MODE_DASH_RB_10S = 3;
  public static final int MODE_DASH_RB_15S = 4;
  public static final int MODE_BBA = 5;
  public static final int MODE_BBA_RB_2S= 6;
  public static final int MODE_BBA_RB_4S = 7;
  public static final int MODE_BBA_RB_10S = 8;
  public static final int MODE_BBA_RB_15S = 9;
  public static final int MODE_HLS = 10;
  public static final int MODE_SS = 11;

  public static int VIDEO_DURATION = 120;

  private TextView statusView;
  private Button startButton;
  private NumberPicker numberPicker;
  private EditText tagEdit;
  private EditText durationEdit;

  private int confirmedCount = 0;
  private int remainedCount = 0;

  private int selectedMode;
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

    final String[] items = {"DASH", "DASH_RB_2S", "DASH_RB_4S", "DASH_RB_10S", "DASH_RB_15S", "BBA", "BBA_RB_2S","BBA_RB_4S", "BBA_RB_10S", "BBA_RB_15S", "HLS", "SS"};

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
    File newBytesDataFile = null;
    File newRequestDataFile = null;

    try {

      //LogFile
      newFile = new File(baseDir.getPath()+"/"+fileName+".csv");
      newFile.createNewFile();
      PrintWriter printWriter = new PrintWriter(newFile);

      ArrayList<ArrayList<LogData>> totalList = EventLogger.getLogDataList();

      String[][] forWrite = new String[1000][totalList.size() + 1];
      HashMap<Double, Double> map = new HashMap<Double, Double>();

      for(ArrayList<LogData> subList: totalList){
        for(LogData logData: subList){
          if(!map.containsKey(logData.getTimeToDouble()))
            map.put(logData.getTimeToDouble(),logData.getTimeToDouble());
        }
      }



      ArrayList<Double> tempList = new ArrayList<Double>(map.values());
      Collections.sort(tempList);

      ArrayList<Double> timestampList = new ArrayList<Double>();
      timestampList.add(0.0);
      for(int x = 1; x < tempList.size(); x++){
        timestampList.add(tempList.get(x));
        timestampList.add(tempList.get(x));
      }

      timestampList.add((double)VIDEO_DURATION);
      Collections.sort(timestampList);

      for(int y = 0; y < timestampList.size(); y++){
        forWrite[y+1][0] = timestampList.get(y) + "";
      }

      String name = "Exp";
      for(int x = 0; x < totalList.size(); x++){
        forWrite[0][x+1] = name+ (x+1);
      }

      for(int x = 0; x < totalList.size(); x++){
        ArrayList<LogData> subList = totalList.get(x);
        for(int y = 0; y < subList.size(); y++){
          LogData data = subList.get(y);
          int idx = timestampList.lastIndexOf(data.getTimeToDouble());
          forWrite[idx+1][x+1] = convert(Integer.parseInt(data.getLog()));
        }
      }

      for(int x = 0; x < totalList.size(); x++){
        ArrayList<LogData> subList = totalList.get(x);
        for(int y = 1; y < subList.size(); y++){
          LogData predata = subList.get(y-1);
          LogData data = subList.get(y);

          int lastIdx = timestampList.lastIndexOf(predata.getTimeToDouble());
          int idx = timestampList.lastIndexOf(data.getTimeToDouble());

          for(int z = lastIdx + 2; z < idx + 1; z++){
            forWrite[z][x+1] = convert(Integer.parseInt(predata.getLog()));
          }
        }

        LogData data = subList.get(subList.size() - 1);
        int idx = timestampList.lastIndexOf(data.getTimeToDouble());
        for(int z = idx + 2; z < timestampList.size() + 1; z++){
          forWrite[z][x+1] = convert(Integer.parseInt(data.getLog()));
        }
      }


      for(int x = 0; x < timestampList.size() + 1; x++){
        String line= "";
        for(int y = 0; y < totalList.size() + 1; y++){
          if(forWrite[x][y] == null){
            line += ",";
          } else{
            line += forWrite[x][y] + ",";
          }
        }
        line = line.substring(0, line.length() - 1);
        printWriter.println(line);
      }

      printWriter.println();
      printWriter.println();

      String avgLine = "Avg.Bitrate,";
      String varLine = "Var.Bitrate,";
      String numSwitchingLine = "Num.Switching,";
      String magSwitchingLine = "Mag.Switching,";
      String numRebufferingLine = "Num.Rebuffering,";
      String durRebufferingLine = "Dur.Rebuffering,";

      ArrayList<Score> scores = EventLogger.getScoreList();

      for(int i = 0; i < scores.size(); i++){
        avgLine += scores.get(i).avgBitrate + ",";
        varLine += scores.get(i).varBitrate + ",";
        numSwitchingLine += scores.get(i).numSwitching + ",";
        magSwitchingLine += scores.get(i).magSwitching + ",";
        numRebufferingLine += scores.get(i).numRebuffering + ",";
        durRebufferingLine += scores.get(i).durationRebuffering + ",";
      }
      avgLine = avgLine.substring(0, avgLine.length() - 1);
      varLine = varLine.substring(0, varLine.length() - 1);
      numSwitchingLine = numSwitchingLine.substring(0, numSwitchingLine.length() - 1);
      magSwitchingLine = magSwitchingLine.substring(0, magSwitchingLine.length() - 1);
      numRebufferingLine = numRebufferingLine.substring(0, numRebufferingLine.length() - 1);
      durRebufferingLine = durRebufferingLine.substring(0, durRebufferingLine.length() - 1);

      printWriter.println(avgLine);
      printWriter.println(varLine);
      printWriter.println(numSwitchingLine);
      printWriter.println(magSwitchingLine);
      printWriter.println(numRebufferingLine);
      printWriter.println(durRebufferingLine);

      printWriter.close();

      //Bytes Data

      if(Configure.LOGGING_BYTES_DATA) {
        newBytesDataFile = new File(baseDir.getPath() + "/" + fileName + "_bytesData.csv");
        newBytesDataFile.createNewFile();
        PrintWriter bytesDataPrintWriter = new PrintWriter(newBytesDataFile);

        //ArrayList<String> wholeBytesDataList = readBytesDataToDevice();

        //ArrayList<ArrayList<Bytes>> totalBytesDataList = new ArrayList<ArrayList<Bytes>>();
        ArrayList<ArrayList<Bytes>> totalBytesDataList = EventLogger.getBytesDataList();

        String bytesDataFileHeader = "";
        for (int i = 0; i < totalBytesDataList.size(); i++) {
          bytesDataFileHeader += "Time(s),Bytes,";
        }
        bytesDataFileHeader = bytesDataFileHeader.substring(0, bytesDataFileHeader.length() - 1);
        bytesDataPrintWriter.println(bytesDataFileHeader);

        int cursor = 0;
        int completed = 0;
        while (completed != totalBytesDataList.size()) {
          String line = "";
          completed = 0;
          for (int i = 0; i < totalBytesDataList.size(); i++) {
            if (totalBytesDataList.get(i).size() <= cursor) {
              line += ",,";
              completed++;
            } else {
              Bytes bytes = totalBytesDataList.get(i).get(cursor);
              line += bytes.getTimestamp() + "," + bytes.getBytes() + ",";
            }
          }
          line = line.substring(0, line.length() - 1);
          bytesDataPrintWriter.println(line);
          cursor++;
        }

        bytesDataPrintWriter.close();
      }

      newRequestDataFile = new File(baseDir.getPath() + "/" + fileName + "_requestData.csv");
      newRequestDataFile.createNewFile();
      PrintWriter requestDataPrintWriter = new PrintWriter(newRequestDataFile);

      ArrayList<ArrayList<Long>> totalRequestDataList = EventLogger.getRequestLists();

      int cursor = 0;
      int completed = 0;
      HashMap<Integer,Integer> completedMap = new HashMap<Integer,Integer>();
      while (completed != totalRequestDataList.size()) {
        String line = "";
        for (int i = 0; i < totalRequestDataList.size(); i++) {
          if (totalRequestDataList.get(i).size() <= cursor) {
            line += ",";
            if(!completedMap.containsKey(i)) {
              completed++;
              completedMap.put(i, i);
            }
          } else {
            long stamp = totalRequestDataList.get(i).get(cursor);
            line += stamp + ",";
          }
        }
        line = line.substring(0, line.length() - 1);
        requestDataPrintWriter.println(line);
        cursor++;
      }

      requestDataPrintWriter.close();
      printWriter.close();

      updateStatusView("End write files...");


    } catch (IOException e) {
      e.printStackTrace();
    }

    updateStatusView("Start transfer files to Server...");



    if(Configure.LOGGING_SERVER) {
      try {
        Socket socket = new Socket("192.9.81.40", 9999);
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
      if (Configure.LOGGING_BYTES_DATA) {
        try {
          Socket socket = new Socket("192.9.81.40", 9999);
          DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
          dos.writeUTF(fileName + "_bytesData.csv");
          Log.d("LLEEJ", " Try to send" + fileName + "_bytesData.csv");
          dos.writeUTF(newBytesDataFile.length() + "");
          Log.d("LLEEJ", "FileSize : " + newBytesDataFile.length());

          InputStream fis = new FileInputStream(newBytesDataFile);
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
      }
      try {
        Socket socket = new Socket("192.9.81.40", 9999);
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.writeUTF(fileName + "_requestData.csv");
        Log.d("LLEEJ", " Try to send" + fileName + "_requestData.csv");
        dos.writeUTF(newRequestDataFile.length() + "");
        Log.d("LLEEJ", "FileSize : " + newRequestDataFile.length());

        InputStream fis = new FileInputStream(newRequestDataFile);
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
        confirmedCount = 0;
        tagText = "";
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



    Sample sample;
    switch(selectedMode){
      case SampleChooserActivity.MODE_DASH:
      case SampleChooserActivity.MODE_BBA:
        sample = Samples.YOUTUBE_DASH_MP4[4];
        break;
      case SampleChooserActivity.MODE_DASH_RB_2S:
      case SampleChooserActivity.MODE_BBA_RB_2S:
        sample = Samples.YOUTUBE_DASH_MP4[0];
        break;
      case SampleChooserActivity.MODE_DASH_RB_4S:
      case SampleChooserActivity.MODE_BBA_RB_4S:
        sample = Samples.YOUTUBE_DASH_MP4[1];
        break;
      case SampleChooserActivity.MODE_DASH_RB_10S:
      case SampleChooserActivity.MODE_BBA_RB_10S:
        sample = Samples.YOUTUBE_DASH_MP4[2];
        break;
      case SampleChooserActivity.MODE_DASH_RB_15S:
      case SampleChooserActivity.MODE_BBA_RB_15S:
        sample = Samples.YOUTUBE_DASH_MP4[3];
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
