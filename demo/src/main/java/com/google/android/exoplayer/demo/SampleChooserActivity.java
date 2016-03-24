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
import com.google.android.exoplayer.demo.Samples.Sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * An activity for selecting from a number of samples.
 */
public class SampleChooserActivity extends Activity implements View.OnClickListener{

  private TextView statusView;
  private Button startButton;
  private NumberPicker numberPicker;
  private EditText tagEdit;

  private int confirmedCount = 0;
  private int remainedCount = 0;
  private String tagText;

  private ArrayList<ArrayList<LogData>> wholeLogList;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.testing);
    numberPicker = (NumberPicker)findViewById(R.id.numberpicker);

    numberPicker.setMinValue(1);
    numberPicker.setMaxValue(20);
    numberPicker.setValue(10);

    statusView = (TextView) findViewById(R.id.statusView);


    startButton = (Button) findViewById(R.id.startButton);
    startButton.setOnClickListener(this);

    wholeLogList = new ArrayList<ArrayList<LogData>>();


    tagEdit = (EditText) findViewById(R.id.tagEdit);


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

  private void writeLogToDevice(){
    if(wholeLogList.size() == 0) {
      startButton.setEnabled(true);
      return;
    }

    updateStatusView("Start write files...");

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    String fileName = dateFormat.format(new Date()).toString();
    if(!tagText.equals("")){
      fileName += "_"+tagText;
    }
    fileName += ".csv";
    File baseDir = new File(Environment.getExternalStorageDirectory() + "/DASH_LOG");

    if(!baseDir.exists())
      baseDir.mkdirs();
    //File newFolder = new File(folderName);
    //File newFolder = new File(folderPath);
    try {

      File newFile = new File(baseDir.getPath()+"/"+fileName);
      newFile.createNewFile();
      PrintWriter printWriter = new PrintWriter(newFile);

      ArrayList<LogData> timestampList = wholeLogList.get(0);
      String timestampLine="";
      for(LogData log : timestampList){
        timestampLine += log.getTimestamp() + ",";
      }
      timestampLine = timestampLine.substring(0, timestampLine.length() - 1);



      printWriter.println(timestampLine);

      for(ArrayList<LogData> logList : wholeLogList){
        String line = "";
        for(LogData log : logList){
          line += log.getLog() + ",";
        }
        line = line.substring(0,line.length() - 1);

        printWriter.println(line);
      }
      printWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    updateStatusView("End write files...");
    updateStatusView("Success Testing");
    startButton.setEnabled(true);

  }

  private void handleAfterTesting(ArrayList<LogData> logList){
    updateStatusView(confirmedCount - remainedCount + " testing is ended.");
    wholeLogList.add(logList);
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


    Sample sample = Samples.YOUTUBE_DASH_MP4[0];
    Intent mpdIntent = new Intent(this, PlayerActivity.class)
            .setData(Uri.parse(sample.uri))
            .putExtra(PlayerActivity.CONTENT_ID_EXTRA, sample.contentId)
            .putExtra(PlayerActivity.CONTENT_TYPE_EXTRA, sample.type)
            .putExtra(PlayerActivity.PROVIDER_EXTRA, sample.provider);

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
