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

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;
import com.google.android.exoplayer.TimeRange;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.demo.Log.BandwidthLogData;
import com.google.android.exoplayer.demo.Log.Bytes;
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

  private long sessionStartTimeMs;
  private long[] loadStartTimeMs;
  private long[] availableRangeValuesUs;

  private int droppedFrameCount = 0;
  private int rebufferingCount = 0;
  private ArrayList<BandwidthLogData> bandwidthLogDataList;

  private ArrayList<Bytes> bytesArrayList = new ArrayList<Bytes>();

  double preDataTime = 0;
  long bytesAccumulates = 0;





  public EventLogger() {
    loadStartTimeMs = new long[DemoPlayer.RENDERER_COUNT];
  }

  public void startSession() {
    sessionStartTimeMs = SystemClock.elapsedRealtime();
    if(Configure.BANDWIDTH_ESTIMATE_DEBUG)
      bandwidthLogDataList = new ArrayList<BandwidthLogData>();
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
      bandwidthLogDataList.add(new BandwidthLogData(getSessionTimeString()+"", bytes+"", bitrateEstimate+"", bitsPerSecond+""));
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

  private void printInternalError(String type, Exception e) {
    Log.e(TAG, "internalError [" + getSessionTimeString() + ", " + type + "]", e);
  }

  private String getStateString(int state) {
    switch (state) {
      case ExoPlayer.STATE_BUFFERING:
        rebufferingCount++;
        return "B";
      case ExoPlayer.STATE_ENDED:
        return "E";
      case ExoPlayer.STATE_IDLE:
        return "I";
      case ExoPlayer.STATE_PREPARING:
        return "P";
      case ExoPlayer.STATE_READY:
        return "R";
      default:
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

  public ArrayList<BandwidthLogData> getBandwidthLogDataList(){
    return bandwidthLogDataList;
  }

  @Override
  public void onBytesTransferred(int elapsedMs, long bytes) {
    double time = Double.parseDouble(getSessionTimeString());
    if(preDataTime == time){
      bytesAccumulates += bytes;
    }
    else{
      //Log.d("LLEEJ BYTE DEBUG", preDataTime + " , " + bytesAccumulates);
      bytesArrayList.add(new Bytes(preDataTime + "", bytesAccumulates + ""));
      preDataTime = time;
      bytesAccumulates = 0;
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
}
