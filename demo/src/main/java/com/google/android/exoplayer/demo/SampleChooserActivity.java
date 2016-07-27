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

import com.google.android.exoplayer.demo.Log.LogData;
import com.google.android.exoplayer.demo.Log.Score;
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
  public static final int MODE_DASH_FIXED = 1;
  public static final int MODE_DASH_TEST1 = 2;
  public static final int MODE_HLS = 3;
  public static final int MODE_SS = 4;
  public static final int MODE_BBA = 5;

  private TextView statusView;
  private Button startButton;
  private NumberPicker numberPicker;
  private EditText tagEdit;

  private int confirmedCount = 0;
  private int remainedCount = 0;
  private int selectedMode;
  private String tagText;
  private Spinner s;


  public static String getModeToString(int mode){
    switch(mode){
      case 0:
        return "DASH";
      case 1:
        return "DASH_FIXED";
      case 2:
        return "DASH_TEST1";
      case 3:
        return "HLS";
      case 4:
        return "SS";
      case 5:
        return "BBA";

      default:
        return "";
    }
  }




  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d("LLEEJONG","Kernel Version : " + System.getProperty("os.version"));
    setContentView(R.layout.testing);
    numberPicker = (NumberPicker)findViewById(R.id.numberpicker);

    numberPicker.setMinValue(1);
    numberPicker.setMaxValue(20);
    numberPicker.setValue(1);

    statusView = (TextView) findViewById(R.id.statusView);


    startButton = (Button) findViewById(R.id.startButton);
    startButton.setOnClickListener(this);




    tagEdit = (EditText) findViewById(R.id.tagEdit);

    final String[] items = {"DASH", "DASH_FIXED" , "DASH_TEST1", "HLS", "SmoothStreaming", "BBA"};

    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner,items);
    s = (Spinner)findViewById(R.id.spinner);
    s.setAdapter(adapter);


    BluetoothAdapter mBTAdapter = BluetoothAdapter.getDefaultAdapter();


    Set<BluetoothDevice> pairedDevices = mBTAdapter.getBondedDevices();
    if(pairedDevices.size() > 0)
      Configure.BT_ON = true;


    Log.d("LLEEJ", Configure.BT_ON + "");

    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    StrictMode.setThreadPolicy(policy);



    /*
    setContentView(R.layout.sample_chooser_activity);


    final List<SampleGroup> sampleGroups = new ArrayList<>();
    SampleGroup group = new SampleGroup("YouTube DASH");
    group.addAll(Samples.YOUTUBE_DASH_MP4);
    group.addAll(Samples.YOUTUBE_DASH_WEBM);
    sampleGroups.add(group);
    group = new SampleGroup("Widevine DASH Policy Tests (GTS)");
    group.addAll(Samples.WIDEVINE_GTS);
    sampleGroups.add(group);
    group = new SampleGroup("Widevine HDCP Capabilities Tests");
    group.addAll(Samples.WIDEVINE_HDCP);
    sampleGroups.add(group);
    group = new SampleGroup("Widevine DASH: MP4,H264");
    group.addAll(Samples.WIDEVINE_H264_MP4_CLEAR);
    group.addAll(Samples.WIDEVINE_H264_MP4_SECURE);
    sampleGroups.add(group);
    group = new SampleGroup("Widevine DASH: WebM,VP9");
    group.addAll(Samples.WIDEVINE_VP9_WEBM_CLEAR);
    group.addAll(Samples.WIDEVINE_VP9_WEBM_SECURE);
    sampleGroups.add(group);
    group = new SampleGroup("Widevine DASH: MP4,H265");
    group.addAll(Samples.WIDEVINE_H265_MP4_CLEAR);
    group.addAll(Samples.WIDEVINE_H265_MP4_SECURE);
    sampleGroups.add(group);
    group = new SampleGroup("SmoothStreaming");
    group.addAll(Samples.SMOOTHSTREAMING);
    sampleGroups.add(group);
    group = new SampleGroup("HLS");
    group.addAll(Samples.HLS);
    sampleGroups.add(group);
    group = new SampleGroup("Misc");
    group.addAll(Samples.MISC);
    sampleGroups.add(group);
    ExpandableListView sampleList = (ExpandableListView) findViewById(R.id.sample_list);
    sampleList.setAdapter(new SampleAdapter(this, sampleGroups));
    sampleList.setOnChildClickListener(new OnChildClickListener() {
      @Override
      public boolean onChildClick(ExpandableListView parent, View view, int groupPosition,
          int childPosition, long id) {
        onSampleSelected(sampleGroups.get(groupPosition).samples.get(childPosition));
        return true;
      }
    });

    */



  }



  private void updateStatusView(String str){
    statusView.append(str + "\n");
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
    if (size == 144) {
      return "110.578";
    } else if (size == 240) {
      return "246.236";
    } else if (size == 360) {
      return "617.173";
    } else if (size == 480) {
      return "1118.095";
    } else if (size == 720) {
      return "2270.192";
    }
    return "";

  }


  private static double convert(String str) { // System.out.println(size+"");
    int size = Integer.parseInt(str);
    if (size == 144) {
      return 110.578;
    } else if (size == 240) {
      return 246.236;
    } else if (size == 360) {
      return 617.173;
    } else if (size == 480) {
      return 1118.095;
    } else if (size == 720) {
      return 2270.192;
    }
    return -1;

  }

  private int widthToIdx(String str){
    int width = Integer.parseInt(str);
    switch(width){
      case 144:
        return 0;
      case 240:
        return 1;
      case 360:
        return 2;
      case 480:
        return 3;
      case 720:
        return 4;
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
    fileName += ".csv";
    File baseDir = new File(Environment.getExternalStorageDirectory() + "/DASH_LOG");

    if(!baseDir.exists())
      baseDir.mkdirs();
    //File newFolder = new File(folderName);
    //File newFolder = new File(folderPath);
    File newFile = null;

    try {

      newFile = new File(baseDir.getPath()+"/"+fileName);
      newFile.createNewFile();
      PrintWriter printWriter = new PrintWriter(newFile);

      ArrayList<String> wholeLogList = readLogToDevice();

      boolean flag = true;
      String[] buffer = null;
      ArrayList<ArrayList<LogData>> totalList = new ArrayList<ArrayList<LogData>>();
      for(String line : wholeLogList){
        String[] temp = line.split(",");
        if(flag){
          buffer = temp;
          flag = false;
        }
        else{
          ArrayList<LogData> logList = new ArrayList<LogData>();
          if(temp.length != buffer.length){
            Log.d("LLEEJ", "FileWriter :: could not occur");
          }
          for(int j = 0; j < temp.length; j++){
            if(!buffer[j].contains("Count")){
              logList.add(new LogData(buffer[j], temp[j]));
            }
          }
          buffer = null;
          totalList.add(logList);
          flag = true;
        }
      }

      String[][] forWrite = new String[1000][totalList.size() + 1];
      HashMap<Double, Double> map = new HashMap<Double, Double>();


      ArrayList<Score> scores = new ArrayList<Score>();

      int numTask = totalList.size();

      for(ArrayList<LogData> subList : totalList) {
        Score newScore = new Score();
        LogData pre = null;
        for (LogData data : subList) {
          if (pre != null) {
            newScore.numSwitching++;
            newScore.magSwitching += Math.abs(widthToIdx(data.getLog()) - widthToIdx(pre.getLog()));
            double diff = data.getTimeToDouble() - pre.getTimeToDouble();
            newScore.avgBitrate += diff * SampleChooserActivity.convert(pre.getLog());
          }
          if (!map.containsKey(data.getTimeToDouble())) {
            map.put(data.getTimeToDouble(), data.getTimeToDouble());
            //Log.d("LLEEJ DEBUG", data.getTimeToDouble() + " , " + data.getLog());
          }
          pre = data;
        }
        double diff2 = 136 - pre.getTimeToDouble();
        newScore.avgBitrate += diff2 * SampleChooserActivity.convert(pre.getLog());
        newScore.avgBitrate = newScore.avgBitrate / 136.0;

        pre = null;
        for(LogData data : subList){
          if(pre != null){
            double diff = data.getTimeToDouble() - pre.getTimeToDouble();
            newScore.varBitrate += diff * (SampleChooserActivity.convert(pre.getLog()) - newScore.avgBitrate) * (SampleChooserActivity.convert(pre.getLog()) - newScore.avgBitrate);
          }
          pre = data;
        }

        newScore.varBitrate += diff2 * (SampleChooserActivity.convert(pre.getLog()) - newScore.avgBitrate) * (SampleChooserActivity.convert(pre.getLog()) - newScore.avgBitrate);
        newScore.varBitrate = newScore.avgBitrate / 136.0;
        scores.add(newScore);
      }


      ArrayList<Double> tempList = new ArrayList<Double>(map.values());
      Collections.sort(tempList);

      ArrayList<Double> timestampList = new ArrayList<Double>();
      timestampList.add(0.0);
      for(int x = 1; x < tempList.size(); x++){
        timestampList.add(tempList.get(x));
        timestampList.add(tempList.get(x));
      }

      timestampList.add(136.0);
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
      String numSwitchingLine = "NumSwitching,";
      String magSwitchingLine = "MagSwitching,";

      for(int i = 0; i < scores.size(); i++){
        avgLine += scores.get(i).avgBitrate + ",";
        varLine += scores.get(i).varBitrate + ",";
        numSwitchingLine += scores.get(i).numSwitching + ",";
        magSwitchingLine += scores.get(i).magSwitching + ",";
      }
      avgLine = avgLine.substring(0, avgLine.length() - 1);
      varLine = varLine.substring(0, varLine.length() - 1);
      numSwitchingLine = numSwitchingLine.substring(0, numSwitchingLine.length() - 1);
      magSwitchingLine = magSwitchingLine.substring(0, magSwitchingLine.length() - 1);

      printWriter.println(avgLine);
      printWriter.println(varLine);
      printWriter.println(numSwitchingLine);
      printWriter.println(magSwitchingLine);

      printWriter.close();



      //ArrayList<LogData> timestampList = wholeLogList.get(0);

//      String timestampLine="";
//      for(LogData log : timestampList){
//        timestampLine += log.getTimestamp() + ",";
//      }
//      timestampLine = timestampLine.substring(0, timestampLine.length() - 1);



      //printWriter.println(timestampLine);

      /*
      for(String line : wholeLogList){
        //line = line.substring(0,line.length() - 1);
        printWriter.println(line);
      }
      */
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
        dos.writeUTF(fileName);
        Log.d("LLEEJ", " Try to send" + fileName);
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
    }
    updateStatusView("Success Testing");
    startButton.setEnabled(true);

  }

  private void handleAfterTesting(ArrayList<LogData> logList) {
    updateStatusView(confirmedCount - remainedCount + " testing is ended.");
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

    selectedMode = s.getSelectedItemPosition();

    Sample sample;
    switch(selectedMode){
      case SampleChooserActivity.MODE_DASH:
      case SampleChooserActivity.MODE_DASH_TEST1:
      case SampleChooserActivity.MODE_DASH_FIXED:
      case SampleChooserActivity.MODE_BBA:
        sample = Samples.YOUTUBE_DASH_MP4[0];
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


    Log.d("LLEEJ","Test " + confirmedCount);
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
