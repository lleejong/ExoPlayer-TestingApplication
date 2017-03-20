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
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;
import com.google.android.exoplayer.NewLogger;
import com.google.android.exoplayer.SegmentLog;
import com.google.android.exoplayer.TimeRange;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.demo.Log.BandwidthLogData;
import com.google.android.exoplayer.demo.Log.Bytes;
import com.google.android.exoplayer.demo.Log.LogData;
import com.google.android.exoplayer.demo.Log.Score;
import com.google.android.exoplayer.demo.Log.VideoSize;
import com.google.android.exoplayer.demo.player.DemoPlayer;
import com.google.android.exoplayer.util.VerboseLogUtil;

import android.media.MediaCodec.CryptoException;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Logs player events using {@link Log}.
 */
public class EventLogger implements DemoPlayer.Listener, DemoPlayer.InfoListener,
    DemoPlayer.InternalErrorListener, DemoPlayer.BytesListener{

  private static final String TAG = "EventLogger";
  private static final NumberFormat TIME_FORMAT;
  static {
    TIME_FORMAT = NumberFormat.getInstance(Locale.US);
    TIME_FORMAT.setMinimumFractionDigits(2);
    TIME_FORMAT.setMaximumFractionDigits(2);
  }
  //private static ArrayList<ArrayList<LogData>> logDataList = new ArrayList<ArrayList<LogData>>();
  private static ArrayList<Score> scoreList = new ArrayList<Score>();
  private static ArrayList<ArrayList<Bytes>> bytesDataList = new ArrayList<ArrayList<Bytes>>();
  private static ArrayList<VideoSize> availableVideoSize = null;
  private static ArrayList<ArrayList<SegmentLog>> segmentLogList = new ArrayList<ArrayList<SegmentLog>>();
  private static ArrayList<ArrayList<ByteLog>> bytesLogList = new ArrayList<ArrayList<ByteLog>>();

  private long sessionStartTimeMs;
  private long[] loadStartTimeMs;
  private long[] availableRangeValuesUs;

  private int droppedFrameCount = 0;
  private int rebufferingCount = 0;
  private long durationRebuffering = 0;
  private long localDurationRebuffering = 0;
  private long rebufferingStartTimeMs;
  private boolean rebufferingFlag = false;
  private long startUpDealy = 0;

  private ArrayList<Bytes> bytesArrayList = new ArrayList<Bytes>();

  double preDataTime = 0;
  long bytesAccumulates = 0;

  boolean isFirst = true;




  public EventLogger() {
    loadStartTimeMs = new long[DemoPlayer.RENDERER_COUNT];
  }

  public static void init(){
    //logDataList = new ArrayList<ArrayList<LogData>>();
    segmentLogList = new ArrayList<ArrayList<SegmentLog>>();
    bytesLogList = new ArrayList<ArrayList<ByteLog>>();
    scoreList = new ArrayList<Score>();
    if(Configure.LOGGING_BYTES_DATA)
      bytesDataList = new ArrayList<ArrayList<Bytes>>();
    availableVideoSize = null;


  }
  public static boolean isInitiatedVideoSize(){
    if(availableVideoSize == null)
      return false;
    else
      return true;
  }


  public static void addNewVideoSize(VideoSize videoSize){
    if(availableVideoSize == null)
      availableVideoSize = new ArrayList<VideoSize>();

    availableVideoSize.add(videoSize);
    String str = "";
    for(VideoSize size : availableVideoSize){
      str += size.getHeight() + " , ";
    }
  }

  public static ArrayList<VideoSize> getAvailableVideoSize(){
    return availableVideoSize;
  }

  public void updateNewScore(Score score){
    scoreList.add(score);
  }
  public void updateNewBytesDataList(){
    checkAdditionalBytesData();
    bytesDataList.add(bytesArrayList);
  }
//  public void updateNewLogDataList(ArrayList<LogData> newLogData){
//    Log.d("LLEEJ1","EventLogger,updateNewLogData() : " + newLogData.size());
//    logDataList.add(newLogData);
//    Log.d("LLEEJ1", "EventLogger,updateNewLogData() : " + logDataList.get(logDataList.size()-1).size());
//  }
  public void updateNewSegmentLogList(ArrayList<SegmentLog> newLogData){
    segmentLogList.add(newLogData);
  }
  public void updateNewByteLogList(ArrayList<ByteLog> newByteLogData){
    bytesLogList.add(newByteLogData);
  }

  public void startSession() {
    sessionStartTimeMs = SystemClock.elapsedRealtime();
    NewLogger.sessionStartTimeMs = sessionStartTimeMs;
    Log.d(TAG, "start [0]");
  }

  public void endSession() {
    Log.d(TAG, "end [" + getSessionTimeString() + "]");
  }

  // DemoPlayer.Listener

  @Override
  public void onStateChanged(boolean playWhenReady, int state) {
    Log.d(TAG, "state [" + getSessionTimeString() + ", " + playWhenReady + ", "
            + getStateString(state) + "]");
  }

  @Override
  public void onError(Exception e) {
    Log.e(TAG, "playerFailed [" + getSessionTimeString() + "]", e);
  }

  @Override
  public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
      float pixelWidthHeightRatio, long time) {
    Log.d(TAG, "videoSizeChanged [" + width + ", " + height + ", " + unappliedRotationDegrees
        + ", " + pixelWidthHeightRatio + "]");
    Log.d("BBA DEBUG", "videoSizeChanged [" + width + ", " + height + ", " + unappliedRotationDegrees
            + ", " + pixelWidthHeightRatio + "]");
  }

  // DemoPlayer.InfoListener

  @Override
  public void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate, float bitsPerSecond) {
    if(Configure.BANDWIDTH_ESTIMATE_DEBUG)
    Log.d(TAG, "bandwidth [" + getSessionTimeString() + ", " + bytes + ", "
            + getTimeString(elapsedMs) + ", " + bitrateEstimate + ", " + bitsPerSecond + "]");
  }

  @Override
  public void onDroppedFrames(int count, long elapsed) {
    Log.d(TAG, "droppedFrames [" + getSessionTimeString() + ", " + count + "]");
    droppedFrameCount = count;
  }

  @Override
  public void onLoadStarted(int sourceId, long length, int type, int trigger, Format format,
      long mediaStartTimeMs, long mediaEndTimeMs) {
    loadStartTimeMs[sourceId] = SystemClock.elapsedRealtime();
    if (VerboseLogUtil.isTagEnabled(TAG)) {
      Log.v(TAG, "loadStart [" + getSessionTimeString() + ", " + sourceId + ", " + type
          + ", " + mediaStartTimeMs + ", " + mediaEndTimeMs + "]");
    }
  }

  @Override
  public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format,
       long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs) {
    if (VerboseLogUtil.isTagEnabled(TAG)) {
      long downloadTime = SystemClock.elapsedRealtime() - loadStartTimeMs[sourceId];
      Log.v(TAG, "loadEnd [" + getSessionTimeString() + ", " + sourceId + ", " + downloadTime
          + "]");
    }
  }

  @Override
  public void onVideoFormatEnabled(Format format, int trigger, long mediaTimeMs) {
    Log.d(TAG, "videoFormat [" + getSessionTimeString() + ", " + format.id + ", "
        + Integer.toString(trigger) + "]");
  }

  @Override
  public void onAudioFormatEnabled(Format format, int trigger, long mediaTimeMs) {
    Log.d(TAG, "audioFormat [" + getSessionTimeString() + ", " + format.id + ", "
            + Integer.toString(trigger) + "]");
  }

  // DemoPlayer.InternalErrorListener

  @Override
  public void onLoadError(int sourceId, IOException e) {
    printInternalError("loadError", e);
  }

  @Override
  public void onRendererInitializationError(Exception e) {
    printInternalError("rendererInitError", e);
  }

  @Override
  public void onDrmSessionManagerError(Exception e) {
    printInternalError("drmSessionManagerError", e);
  }

  @Override
  public void onDecoderInitializationError(DecoderInitializationException e) {
    printInternalError("decoderInitializationError", e);
  }

  @Override
  public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {
    printInternalError("audioTrackInitializationError", e);
  }

  @Override
  public void onAudioTrackWriteError(AudioTrack.WriteException e) {
    printInternalError("audioTrackWriteError", e);
  }

  @Override
  public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
    printInternalError("audioTrackUnderrun [" + bufferSize + ", " + bufferSizeMs + ", "
        + elapsedSinceLastFeedMs + "]", null);
  }

  @Override
  public void onCryptoError(CryptoException e) {
    printInternalError("cryptoError", e);
  }

  @Override
  public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
      long initializationDurationMs) {
    Log.d(TAG, "decoderInitialized [" + getSessionTimeString() + ", " + decoderName + "]");
  }

  @Override
  public void onAvailableRangeChanged(int sourceId, TimeRange availableRange) {
    availableRangeValuesUs = availableRange.getCurrentBoundsUs(availableRangeValuesUs);
    Log.d(TAG, "availableRange [" + availableRange.isStatic() + ", " + availableRangeValuesUs[0]
            + ", " + availableRangeValuesUs[1] + "]");
  }

  @Override
  public void onSwitchToSteadyState(long elapsedMs) {
    Log.d("onSwitchToSteadyStateBBA","onSwitchToSteadyState : " + elapsedMs);
  }

  @Override
  public void onAllChunksDownloaded(long totalBytes) {
    Log.d("onAllChunksDownloaded","onAllChunksDownloaded : " + totalBytes);
  }

  @Override
  public void onBufferLoadChanged(long bufferDurationMs) {
    Log.d("onBufferLoadChanged", "onBufferLoadChanged : " + bufferDurationMs);
  }

  //LLEEJ: InterGET
  @Override
  public void onGetRequestPatched(long elapsedMs){
    //long realTimeMs = elapsedMs - sessionStartTimeMs;
    //requestList.add(realTimeMs);
  }

  private void printInternalError(String type, Exception e) {
    Log.e(TAG, "internalError [" + getSessionTimeString() + ", " + type + "]", e);
  }

  private String getStateString(int state) {
    switch (state) {
      case ExoPlayer.STATE_BUFFERING:
        Log.d("LLEEJ_STATE","B");
        if(!isFirst)
          rebufferingCount++;
        rebufferingStartTimeMs = SystemClock.elapsedRealtime();
        rebufferingFlag = true;
        return "B";
      case ExoPlayer.STATE_ENDED:
        Log.d("LLEEJ_STATE","E");
        return "E";
      case ExoPlayer.STATE_IDLE:
        Log.d("LLEEJ_STATE","I");
        return "I";
      case ExoPlayer.STATE_PREPARING:
        Log.d("LLEEJ_STATE","P");
        return "P";
      case ExoPlayer.STATE_READY:
        Log.d("LLEEJ_STATE","R");
        if(isFirst){
          startUpDealy = SystemClock.elapsedRealtime() - rebufferingStartTimeMs;
          rebufferingFlag = false;
          isFirst = false;
        }
        else if(rebufferingFlag) {
          rebufferingFlag = false;
          localDurationRebuffering = SystemClock.elapsedRealtime() - rebufferingStartTimeMs;
          durationRebuffering += localDurationRebuffering;
          Log.d("LLEEJ, buffering", durationRebuffering + " ");
        }
        return "R";
      default:
        Log.d("LLEEJ_STATE","?");
        return "?";
    }
  }

  private String getSessionTimeString() {
    return getTimeString(SystemClock.elapsedRealtime() - sessionStartTimeMs);
  }

  private String getTimeString(long timeMs) {
    return TIME_FORMAT.format((timeMs) / 1000f);
  }

  public int getDroppedFrameCount(){
    return droppedFrameCount;
  }

  public int getRebufferingCount(){
    return rebufferingCount;
  }



  @Override
  public void onBytesTransferred(int elapsedMs, long bytes) {
    if(Configure.LOGGING_BYTES_DATA) {
      double time = Double.parseDouble(getSessionTimeString());
      if (preDataTime == time) {
        bytesAccumulates += bytes;
      } else {
        bytesArrayList.add(new Bytes(preDataTime + "", bytesAccumulates + ""));
        preDataTime = time;
        bytesAccumulates = 0;
      }
    }
  }

  private void checkAdditionalBytesData(){
    if(bytesAccumulates != 0)
      bytesArrayList.add(new Bytes(preDataTime + "",bytesAccumulates + ""));

    bytesAccumulates = 0;
    preDataTime = 0;
  }

  public ArrayList<Bytes> getBytesArrayList(){
    checkAdditionalBytesData();
    return bytesArrayList;
  }
  public long getDurationRebuffering(){
    return durationRebuffering;
  }
  public long getStartUpDealy(){
    return startUpDealy;
  }

//  public static ArrayList<ArrayList<LogData>> getLogDataList(){
//    return logDataList;
//  }
  public static ArrayList<ArrayList<Bytes>> getBytesDataList(){
    return bytesDataList;
  }
  public static ArrayList<Score> getScoreList() {
    return scoreList;
  }
  public static ArrayList<ArrayList<SegmentLog>> getSegmentLogList(){
    return segmentLogList;
  }
  public static ArrayList<ArrayList<ByteLog>> getBytesLogList(){
    return bytesLogList;
  }
}
