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

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;
import com.google.android.exoplayer.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.NewLogger;
import com.google.android.exoplayer.SegmentLog;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.demo.Log.BandwidthLogData;
import com.google.android.exoplayer.demo.Log.Bytes;
import com.google.android.exoplayer.demo.Log.LogData;
import com.google.android.exoplayer.demo.Log.Score;
import com.google.android.exoplayer.demo.Log.VideoSize;
import com.google.android.exoplayer.demo.player.DashRendererBuilder;
import com.google.android.exoplayer.demo.player.DemoPlayer;
import com.google.android.exoplayer.demo.player.DemoPlayer.RendererBuilder;
import com.google.android.exoplayer.demo.player.ExtractorRendererBuilder;
import com.google.android.exoplayer.demo.player.HlsRendererBuilder;
import com.google.android.exoplayer.demo.player.SessionTimer;
import com.google.android.exoplayer.demo.player.SmoothStreamingRendererBuilder;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.metadata.id3.GeobFrame;
import com.google.android.exoplayer.metadata.id3.Id3Frame;
import com.google.android.exoplayer.metadata.id3.PrivFrame;
import com.google.android.exoplayer.metadata.id3.TxxxFrame;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.google.android.exoplayer.util.DebugTextViewHelper;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.Util;
import com.google.android.exoplayer.util.VerboseLogUtil;

import android.Manifest.permission;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.accessibility.CaptioningManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * An activity that plays media using {@link DemoPlayer}.
 */
public class PlayerActivity extends Activity implements SurfaceHolder.Callback, OnClickListener,
    DemoPlayer.Listener, DemoPlayer.CaptionListener, DemoPlayer.Id3MetadataListener,
    AudioCapabilitiesReceiver.Listener, SessionTimer.OnSessionTimeListener {

  // For use within demo app code.
  public static final String CONTENT_ID_EXTRA = "content_id";
  public static final String CONTENT_TYPE_EXTRA = "content_type";
  public static final String PROVIDER_EXTRA = "provider";

  // For use when launching the demo app using adb.
  private static final String CONTENT_EXT_EXTRA = "type";

  private static final String TAG = "PlayerActivity";
  private static final int MENU_GROUP_TRACKS = 1;
  private static final int ID_OFFSET = 2;


  private static final CookieManager defaultCookieManager;
  static {
    defaultCookieManager = new CookieManager();
    defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
  }

  private static final NumberFormat TIME_FORMAT;
  static {
    TIME_FORMAT = NumberFormat.getInstance(Locale.US);
    TIME_FORMAT.setMinimumFractionDigits(2);
    TIME_FORMAT.setMaximumFractionDigits(2);
  }

  private static EventLogger eventLogger;
  private MediaController mediaController;
  private View debugRootView;
  private View shutterView;
  private AspectRatioFrameLayout videoFrame;
  private SurfaceView surfaceView;
  private TextView debugTextView;
  private TextView playerStateTextView;
  private SubtitleLayout subtitleLayout;
  private Button videoButton;
  private Button audioButton;
  private Button textButton;
  private Button retryButton;

  private DemoPlayer player;
  private DebugTextViewHelper debugViewHelper;
  private boolean playerNeedsPrepare;

  private long playerPosition;
  private boolean enableBackgroundAudio;

  private Uri contentUri;
  private int contentType;
  private String contentId;
  private String provider;
  private int selectedMode;

  private AudioCapabilitiesReceiver audioCapabilitiesReceiver;

  //private ArrayList<LogData> logList = null;
  private int id;
  private String tag;
  private SessionTimer sessionTimer;

  private CompetingFlowClient cfClient;





  // Activity lifecycle

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.player_activity);

    Log.d("LLEEJ", "AAAAA");

    View root = findViewById(R.id.root);
    root.setOnTouchListener(new OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
          toggleControlsVisibility();
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
          view.performClick();
        }
        return true;
      }
    });
    root.setOnKeyListener(new OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE
            || keyCode == KeyEvent.KEYCODE_MENU) {
          return false;
        }
        return mediaController.dispatchKeyEvent(event);
      }
    });

    shutterView = findViewById(R.id.shutter);
    debugRootView = findViewById(R.id.controls_root);

    videoFrame = (AspectRatioFrameLayout) findViewById(R.id.video_frame);
    surfaceView = (SurfaceView) findViewById(R.id.surface_view);
    surfaceView.getHolder().addCallback(this);
    debugTextView = (TextView) findViewById(R.id.debug_text_view);

    playerStateTextView = (TextView) findViewById(R.id.player_state_view);
    subtitleLayout = (SubtitleLayout) findViewById(R.id.subtitles);

    mediaController = new KeyCompatibleMediaController(this);
    mediaController.setAnchorView(root);
    retryButton = (Button) findViewById(R.id.retry_button);
    retryButton.setOnClickListener(this);
    videoButton = (Button) findViewById(R.id.video_controls);
    audioButton = (Button) findViewById(R.id.audio_controls);
    textButton = (Button) findViewById(R.id.text_controls);

    CookieHandler currentHandler = CookieHandler.getDefault();
    if (currentHandler != defaultCookieManager) {
      CookieHandler.setDefault(defaultCookieManager);
    }

    audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(this, this);
    audioCapabilitiesReceiver.register();
  }



  @Override
  public void onNewIntent(Intent intent) {
    releasePlayer();
    playerPosition = 0;
    setIntent(intent);
  }

  @Override
  public void onResume() {
    super.onResume();
    Intent intent = getIntent();
    contentUri = intent.getData();
    contentType = intent.getIntExtra(CONTENT_TYPE_EXTRA,
            inferContentType(contentUri, intent.getStringExtra(CONTENT_EXT_EXTRA)));
    contentId = intent.getStringExtra(CONTENT_ID_EXTRA);
    provider = intent.getStringExtra(PROVIDER_EXTRA);
    id = intent.getIntExtra("id", -1);
    tag = intent.getStringExtra("tag");
    selectedMode = intent.getIntExtra("mode", -1);
    configureSubtitleView();


    if (player == null) {
      //if (!maybeRequestPermission()) {
        preparePlayer(true);
      //}
    } else {
      player.setBackgrounded(false);
    }

    if(id == -1){
      Log.d("LLEEJ", "id is negative. Error");
      finish();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (!enableBackgroundAudio) {
      releasePlayer();
    } else {
      player.setBackgrounded(true);
    }
    shutterView.setVisibility(View.VISIBLE);
    releasePlayer();
    //setResult(-1);
    //finish();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    audioCapabilitiesReceiver.unregister();
    if(Configure.COMPETING_FLOW_EXPERI)
      cfClient.sendMessage(CompetingFlowClient.MSG_TYPE_IPERF_END);
    //setResult(-1);
    releasePlayer();
    //finish();
  }

  // OnClickListener methods

  @Override
  public void onClick(View view) {
    if (view == retryButton) {
      preparePlayer(true);
    }
  }

  public void onSessionTimeEnded(){
    onEndState();
  }

  // AudioCapabilitiesReceiver.Listener methods

  @Override
  public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
    if (player == null) {
      return;
    }
    boolean backgrounded = player.getBackgrounded();
    boolean playWhenReady = player.getPlayWhenReady();
    releasePlayer();
    preparePlayer(playWhenReady);
    player.setBackgrounded(backgrounded);
  }


  // Internal methods

  /*
  private RendererBuilder getRendererBuilder() {
    String userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
    switch (contentType) {
      case Util.TYPE_SS:
        return new SmoothStreamingRendererBuilder(this, userAgent, contentUri.toString(),
            new SmoothStreamingTestMediaDrmCallback());
      case Util.TYPE_DASH:
        return new DashRendererBuilder(this, userAgent, contentUri.toString(),
            new WidevineTestMediaDrmCallback(contentId, provider), selectedMode);
      case Util.TYPE_HLS:
        return new HlsRendererBuilder(this, userAgent, contentUri.toString());
      case Util.TYPE_OTHER:
        return new ExtractorRendererBuilder(this, userAgent, contentUri);
      default:
        throw new IllegalStateException("Unsupported type: " + contentType);
    }
  }
  */

  private RendererBuilder getRendererBuilder(){
    String userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
    switch(selectedMode){
      case SampleChooserActivity.MODE_DASH:
      case SampleChooserActivity.MODE_BBA:
      case SampleChooserActivity.MODE_DASH_RB_2S:
      case SampleChooserActivity.MODE_BBA_RB_2S:
      case SampleChooserActivity.MODE_DASH_RB_4S:
      case SampleChooserActivity.MODE_BBA_RB_4S:
        case SampleChooserActivity.MODE_DASH_RB_6S:
        case SampleChooserActivity.MODE_BBA_RB_6S:
      case SampleChooserActivity.MODE_DASH_RB_10S:
      case SampleChooserActivity.MODE_BBA_RB_10S:
      case SampleChooserActivity.MODE_DASH_RB_15S:
      case SampleChooserActivity.MODE_BBA_RB_15S:
        return new DashRendererBuilder(this, userAgent, contentUri.toString(),
                new WidevineTestMediaDrmCallback(contentId, provider), selectedMode);

      case SampleChooserActivity.MODE_HLS:
        return new HlsRendererBuilder(this, userAgent, contentUri.toString());

      case SampleChooserActivity.MODE_SS:
        return new SmoothStreamingRendererBuilder(this, userAgent, contentUri.toString(),
                new SmoothStreamingTestMediaDrmCallback());

      default:
        throw new IllegalStateException("Unsupported type: " + contentType);
    }
  }


  private void preparePlayer(boolean playWhenReady) {
    if (player == null) {
      player = new DemoPlayer(getRendererBuilder());
      player.addListener(this);
      player.setCaptionListener(this);
      player.setMetadataListener(this);
      player.seekTo(playerPosition);
      playerNeedsPrepare = true;
      mediaController.setMediaPlayer(player.getPlayerControl());
      mediaController.setEnabled(true);
      eventLogger = new EventLogger();
      eventLogger.startSession();
      player.addListener(eventLogger);
      player.setInfoListener(eventLogger);
      player.setInternalErrorListener(eventLogger);
      debugViewHelper = new DebugTextViewHelper(player, debugTextView);
      debugViewHelper.start();
    }
    if (playerNeedsPrepare) {
      player.prepare();
      playerNeedsPrepare = false;
      updateButtonVisibilities();
    }
    player.setSurface(surfaceView.getHolder().getSurface());
    player.setPlayWhenReady(playWhenReady);

    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    StrictMode.setThreadPolicy(policy);

    if(Configure.COMPETING_FLOW_EXPERI) {
      cfClient = new CompetingFlowClient();
      sessionTimer = new SessionTimer(player, this,cfClient);
    }
    else {
      sessionTimer = new SessionTimer(player, this, null);
    }
    sessionTimer.start();
  }

  private void releasePlayer() {
    if (player != null) {
      debugViewHelper.stop();
      debugViewHelper = null;
      playerPosition = player.getCurrentPosition();
      player.release();
      player = null;
      eventLogger.endSession();
      eventLogger = null;
      sessionTimer.interrupt();
    }
  }

  private int bitrateToIdx(double bitrate){
    ArrayList<VideoSize> availableVideoSize = EventLogger.getAvailableVideoSize();

    for(int i = 0; i < availableVideoSize.size(); i++){
      VideoSize videoSize = availableVideoSize.get(i);
      if(videoSize.getBps()/1000.0 == bitrate)
        return i;
    }
    return -1;
  }


  // DemoPlayer.Listener implementation
  private int heightToIdx(String str){
    ArrayList<VideoSize> availableVideoSize = EventLogger.getAvailableVideoSize();

    int height = Integer.parseInt(str);
    for(int i = 0; i < availableVideoSize.size(); i++){
      VideoSize videoSize = availableVideoSize.get(i);
      if(videoSize.getHeight() == height)
        return i;
    }
    return -1;
  }
  private void doFinalLogging(){
    //Log.d("LLEEJ1", "PlayerActivity,doFinalLogging() : " + logList.size());
    //eventLogger.updateNewLogDataList(logList);
    eventLogger.updateNewSegmentLogList(NewLogger.getSegmentLogList());
    eventLogger.updateNewByteLogList(NewLogger.getByteLogList());
    ArrayList<SegmentLog> segmentLogList = NewLogger.getSegmentLogList();
    NewLogger.init();

    if(Configure.LOGGING_BYTES_DATA)
      eventLogger.updateNewBytesDataList();


    Score newScore = new Score();

    newScore.numRebuffering = eventLogger.getRebufferingCount();
    newScore.durationRebuffering = eventLogger.getDurationRebuffering();
    newScore.startupDelay = eventLogger.getStartUpDealy();

    //average

    SegmentLog preLog = null;

    for(SegmentLog log : segmentLogList){
      newScore.avgBitrate += log.bitrate;
      if(preLog != null && preLog.bitrate != log.bitrate){
        newScore.numSwitching++;
        Log.d("LLEEJ, magSwitching", "bitrate : " + log.bitrate + " , idx : " + bitrateToIdx(log.bitrate));
        Log.d("LLEEJ, magSwitching", "bitrate : " + preLog.bitrate + " , idx : " + bitrateToIdx(preLog.bitrate));
        newScore.magSwitching += Math.abs(bitrateToIdx(log.bitrate) - bitrateToIdx(preLog.bitrate));
      }
      preLog = log;
    }
    newScore.avgBitrate /= segmentLogList.size();

    for(SegmentLog log : segmentLogList){
      newScore.varBitrate += (log.bitrate - newScore.avgBitrate) * (log.bitrate - newScore.avgBitrate);
    }
    newScore.varBitrate /= (segmentLogList.size()-1);



//    LogData pre = null;
//    for(LogData data : logList){
//      newScore.numSwitching++;
//      if(pre != null){
//        newScore.magSwitching += Math.abs(heightToIdx(data.getLog()) - heightToIdx(pre.getLog()));
//        double diff = data.getTimeToDouble() - pre.getTimeToDouble();
//        newScore.avgBitrate += diff * SampleChooserActivity.convert(pre.getLog());
//      }
//      pre = data;
//    }
//    double diff2 = SampleChooserActivity.VIDEO_DURATION - pre.getTimeToDouble();
//    newScore.avgBitrate += diff2 * SampleChooserActivity.convert(pre.getLog());
//    newScore.avgBitrate += logList.get(0).getTimeToDouble() * SampleChooserActivity.convert(logList.get(0).getLog());
//
//    newScore.avgBitrate = newScore.avgBitrate / (double) SampleChooserActivity.VIDEO_DURATION;
//
//    pre = null;
//    for(LogData data : logList){
//      if(pre != null){
//        double diff = data.getTimeToDouble() - pre.getTimeToDouble();
//        newScore.varBitrate += diff * (SampleChooserActivity.convert(pre.getLog()) - newScore.avgBitrate) * (SampleChooserActivity.convert(pre.getLog()) - newScore.avgBitrate);
//      }
//      pre = data;
//    }
//
//    newScore.varBitrate += diff2 * (SampleChooserActivity.convert(pre.getLog()) - newScore.avgBitrate) * (SampleChooserActivity.convert(pre.getLog()) - newScore.avgBitrate);
//    newScore.varBitrate += (SampleChooserActivity.convert(logList.get(0).getLog()) - newScore.avgBitrate) * (SampleChooserActivity.convert(logList.get(0).getLog()) - newScore.avgBitrate);
//    newScore.varBitrate = newScore.varBitrate /(double) SampleChooserActivity.VIDEO_DURATION;
//    newScore.varBitrate = Math.sqrt(newScore.varBitrate);

    eventLogger.updateNewScore(newScore);

  }
  /*
  private void writeFileToDevice(){
    //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    //String fileName = dateFormat.format(new Date()).toString();

    //fileName += ".csv";
    Log.d("LLEEJ", "PlayerActivity::start write to File, id = " + id);




    File baseDir = new File(Environment.getExternalStorageDirectory() + "/DASH_LOG/tmpByLLEEJ");

    File bytesDataBaseDir = new File(Environment.getExternalStorageDirectory() + "/DASH_LOG/tmpBytesData");

    if(!baseDir.exists())
      baseDir.mkdirs();

    if(!bytesDataBaseDir.exists())
      bytesDataBaseDir.mkdirs();

    try {
      //Bytes Data Writting
      File newBytesDataFile = new File(bytesDataBaseDir.getPath()+"/"+id+".csv");
      newBytesDataFile.createNewFile();
      PrintWriter printBytesDataWriter = new PrintWriter(newBytesDataFile);
      ArrayList<Bytes> bytesArrayList = eventLogger.getBytesArrayList();

      String bytesTimestampLine = "";
      String bytesLine = "";
      for(Bytes bytes : bytesArrayList){
        bytesTimestampLine += bytes.getTimestamp()+",";
        bytesLine += bytes.getBytes()+",";
      }

      bytesTimestampLine = bytesTimestampLine.substring(0, bytesTimestampLine.length() - 1);
      bytesLine = bytesLine.substring(0, bytesLine.length() - 1);
      printBytesDataWriter.println(bytesTimestampLine);
      printBytesDataWriter.println(bytesLine);
      printBytesDataWriter.close();

      //end


      //bitrate Data Writting
      File newFile = new File(baseDir.getPath()+"/"+id+".csv");
      newFile.createNewFile();
      PrintWriter printWriter = new PrintWriter(newFile);

      String timestampLine = "";
      for(LogData log : logList){
        timestampLine += log.getTimestamp() + ",";
      }
      timestampLine += "DroppedFrame Count,Rebuffering Count";
      //timestampLine = timestampLine.substring(0, timestampLine.length() - 1);

      printWriter.println(timestampLine);

      String logLine = "";
      for(LogData log : logList){
        logLine += log.getLog() + ",";
      }
      logLine += eventLogger.getDroppedFrameCount() + ","+ eventLogger.getRebufferingCount();
      //logLine = logLine.substring(0, logLine.length() - 1);
      printWriter.println(logLine);

      printWriter.close();

      if(Configure.BANDWIDTH_ESTIMATE_DEBUG) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String fileName = dateFormat.format(new Date()).toString();
        File bitrateDir = new File(Environment.getExternalStorageDirectory() + "/DASH_LOG/BANDWIDTH_DEBUG");
        if(!bitrateDir.exists())
          bitrateDir.mkdirs();
        File newBitrateFile = new File(bitrateDir.getPath() + "/" + fileName +"_BANDWIDTH_"+ tag +"_" + SampleChooserActivity.getModeToString(selectedMode) + ".csv");
        newBitrateFile.createNewFile();
        PrintWriter bitrateWriter = new PrintWriter(newBitrateFile);
        ArrayList<BandwidthLogData> bandwidthDataList = eventLogger.getBandwidthLogDataList();
        Log.d("LLEEJ","BANDWIDTH : "+bandwidthDataList.size() +"");

        String header = "MS,Byte,Bitrate,bps";
        bitrateWriter.println(header);
        for(BandwidthLogData data : bandwidthDataList){
          String line = data.getElapsedMs() + "," + data.getByteAccumulates() + "," + data.getBitrateEstimated() + ","+data.getBitsPerSecond();
          bitrateWriter.println(line);
        }
        bitrateWriter.close();

      }


      Log.d("LLEEJ", "PlayerActivity::End write to File, id = " + id);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  */

  public void onEndState(){
    Log.d("LLEEJ", "END STATE");
    Intent intent = new Intent();
    intent.putExtra("success", true);
    setResult(0, intent);
    //writeFileToDevice();
    doFinalLogging();
    //logList = null;
    Log.d("LLEEJ","END STATE");
    finish();
  }


  @Override
  public void onStateChanged(boolean playWhenReady, int playbackState) {
    if (playbackState == ExoPlayer.STATE_ENDED) {
      showControls();
    }
    String text = "playWhenReady=" + playWhenReady + ", playbackState=";
    switch(playbackState) {
      case ExoPlayer.STATE_BUFFERING:
        text += "buffering";
        break;
      case ExoPlayer.STATE_ENDED:
        text += "ended";
        onEndState();
        break;
      case ExoPlayer.STATE_IDLE:
        text += "idle";
        break;
      case ExoPlayer.STATE_PREPARING:
        text += "preparing";
        break;
      case ExoPlayer.STATE_READY:
        text += "ready";
        break;
      default:
        text += "unknown";
        break;
    }
    playerStateTextView.setText(text);
    updateButtonVisibilities();
  }

  @Override
  public void onError(Exception e) {
    String errorString = null;
    if (e instanceof UnsupportedDrmException) {
      // Special case DRM failures.
      UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
      errorString = getString(Util.SDK_INT < 18 ? R.string.error_drm_not_supported
          : unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
          ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
    } else if (e instanceof ExoPlaybackException
        && e.getCause() instanceof DecoderInitializationException) {
      // Special case for decoder initialization failures.
      DecoderInitializationException decoderInitializationException =
          (DecoderInitializationException) e.getCause();
      if (decoderInitializationException.decoderName == null) {
        if (decoderInitializationException.getCause() instanceof DecoderQueryException) {
          errorString = getString(R.string.error_querying_decoders);
        } else if (decoderInitializationException.secureDecoderRequired) {
          errorString = getString(R.string.error_no_secure_decoder,
              decoderInitializationException.mimeType);
        } else {
          errorString = getString(R.string.error_no_decoder,
              decoderInitializationException.mimeType);
        }
      } else {
        errorString = getString(R.string.error_instantiating_decoder,
            decoderInitializationException.decoderName);
      }
    }
    if (errorString != null) {
      Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_LONG).show();
    }
    playerNeedsPrepare = true;
    updateButtonVisibilities();
    showControls();
  }

  private String getTimeString(long timeMs) {
    return TIME_FORMAT.format((timeMs) / 1000f);
  }



  @Override
  public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
      float pixelWidthAspectRatio, long time) {
    shutterView.setVisibility(View.GONE);
    videoFrame.setAspectRatio(
            height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);

    //if(logList == null)
      //logList = new ArrayList<LogData>();


    //logList.add(new LogData(getTimeString(player.getCurrentPosition()), height+""));
  }

  // User controls

  private void updateButtonVisibilities() {
    retryButton.setVisibility(playerNeedsPrepare ? View.VISIBLE : View.GONE);
    videoButton.setVisibility(haveTracks(DemoPlayer.TYPE_VIDEO) ? View.VISIBLE : View.GONE);
    audioButton.setVisibility(haveTracks(DemoPlayer.TYPE_AUDIO) ? View.VISIBLE : View.GONE);
    textButton.setVisibility(haveTracks(DemoPlayer.TYPE_TEXT) ? View.VISIBLE : View.GONE);
  }

  private boolean haveTracks(int type) {
    return player != null && player.getTrackCount(type) > 0;
  }

  public void showVideoPopup(View v) {
    PopupMenu popup = new PopupMenu(this, v);
    configurePopupWithTracks(popup, null, DemoPlayer.TYPE_VIDEO);
    popup.show();
  }

  public void showAudioPopup(View v) {
    PopupMenu popup = new PopupMenu(this, v);
    Menu menu = popup.getMenu();
    menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.enable_background_audio);
    final MenuItem backgroundAudioItem = menu.findItem(0);
    backgroundAudioItem.setCheckable(true);
    backgroundAudioItem.setChecked(enableBackgroundAudio);
    OnMenuItemClickListener clickListener = new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        if (item == backgroundAudioItem) {
          enableBackgroundAudio = !item.isChecked();
          return true;
        }
        return false;
      }
    };
    configurePopupWithTracks(popup, clickListener, DemoPlayer.TYPE_AUDIO);
    popup.show();
  }

  public void showTextPopup(View v) {
    PopupMenu popup = new PopupMenu(this, v);
    configurePopupWithTracks(popup, null, DemoPlayer.TYPE_TEXT);
    popup.show();
  }

  public void showVerboseLogPopup(View v) {
    PopupMenu popup = new PopupMenu(this, v);
    Menu menu = popup.getMenu();
    menu.add(Menu.NONE, 0, Menu.NONE, R.string.logging_normal);
    menu.add(Menu.NONE, 1, Menu.NONE, R.string.logging_verbose);
    menu.setGroupCheckable(Menu.NONE, true, true);
    menu.findItem((VerboseLogUtil.areAllTagsEnabled()) ? 1 : 0).setChecked(true);
    popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == 0) {
          VerboseLogUtil.setEnableAllTags(false);
        } else {
          VerboseLogUtil.setEnableAllTags(true);
        }
        return true;
      }
    });
    popup.show();
  }

  private void configurePopupWithTracks(PopupMenu popup,
      final OnMenuItemClickListener customActionClickListener,
      final int trackType) {
    if (player == null) {
      return;
    }
    int trackCount = player.getTrackCount(trackType);
    if (trackCount == 0) {
      return;
    }
    popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        return (customActionClickListener != null
            && customActionClickListener.onMenuItemClick(item))
            || onTrackItemClick(item, trackType);
      }
    });
    Menu menu = popup.getMenu();
    // ID_OFFSET ensures we avoid clashing with Menu.NONE (which equals 0).
    menu.add(MENU_GROUP_TRACKS, DemoPlayer.TRACK_DISABLED + ID_OFFSET, Menu.NONE, R.string.off);
    for (int i = 0; i < trackCount; i++) {
      menu.add(MENU_GROUP_TRACKS, i + ID_OFFSET, Menu.NONE,
          buildTrackName(player.getTrackFormat(trackType, i)));
    }
    menu.setGroupCheckable(MENU_GROUP_TRACKS, true, true);
    menu.findItem(player.getSelectedTrack(trackType) + ID_OFFSET).setChecked(true);
  }

  private static String buildTrackName(MediaFormat format) {
    if (format.adaptive) {
      return "auto";
    }
    String trackName;
    if (MimeTypes.isVideo(format.mimeType)) {
      trackName = joinWithSeparator(joinWithSeparator(buildResolutionString(format),
          buildBitrateString(format)), buildTrackIdString(format));
    } else if (MimeTypes.isAudio(format.mimeType)) {
      trackName = joinWithSeparator(joinWithSeparator(joinWithSeparator(buildLanguageString(format),
          buildAudioPropertyString(format)), buildBitrateString(format)),
          buildTrackIdString(format));
    } else {
      trackName = joinWithSeparator(joinWithSeparator(buildLanguageString(format),
          buildBitrateString(format)), buildTrackIdString(format));
    }
    return trackName.length() == 0 ? "unknown" : trackName;
  }

  private static String buildResolutionString(MediaFormat format) {
    return format.width == MediaFormat.NO_VALUE || format.height == MediaFormat.NO_VALUE
        ? "" : format.width + "x" + format.height;
  }

  private static String buildAudioPropertyString(MediaFormat format) {
    return format.channelCount == MediaFormat.NO_VALUE || format.sampleRate == MediaFormat.NO_VALUE
        ? "" : format.channelCount + "ch, " + format.sampleRate + "Hz";
  }

  private static String buildLanguageString(MediaFormat format) {
    return TextUtils.isEmpty(format.language) || "und".equals(format.language) ? ""
        : format.language;
  }

  private static String buildBitrateString(MediaFormat format) {
    return format.bitrate == MediaFormat.NO_VALUE ? ""
        : String.format(Locale.US, "%.2fMbit", format.bitrate / 1000000f);
  }

  private static String joinWithSeparator(String first, String second) {
    return first.length() == 0 ? second : (second.length() == 0 ? first : first + ", " + second);
  }

  private static String buildTrackIdString(MediaFormat format) {
    return format.trackId == null ? "" : " (" + format.trackId + ")";
  }

  private boolean onTrackItemClick(MenuItem item, int type) {
    if (player == null || item.getGroupId() != MENU_GROUP_TRACKS) {
      return false;
    }
    player.setSelectedTrack(type, item.getItemId() - ID_OFFSET);
    return true;
  }

  private void toggleControlsVisibility()  {
    if (mediaController.isShowing()) {
      mediaController.hide();
      debugRootView.setVisibility(View.GONE);
    } else {
      showControls();
    }
  }

  private void showControls() {
    mediaController.show(0);
    debugRootView.setVisibility(View.VISIBLE);
  }

  // DemoPlayer.CaptionListener implementation

  @Override
  public void onCues(List<Cue> cues) {
    subtitleLayout.setCues(cues);
  }

  // DemoPlayer.MetadataListener implementation

  @Override
  public void onId3Metadata(List<Id3Frame> id3Frames) {
    for (Id3Frame id3Frame : id3Frames) {
      if (id3Frame instanceof TxxxFrame) {
        TxxxFrame txxxFrame = (TxxxFrame) id3Frame;
        Log.i(TAG, String.format("ID3 TimedMetadata %s: description=%s, value=%s", txxxFrame.id,
            txxxFrame.description, txxxFrame.value));
      } else if (id3Frame instanceof PrivFrame) {
        PrivFrame privFrame = (PrivFrame) id3Frame;
        Log.i(TAG, String.format("ID3 TimedMetadata %s: owner=%s", privFrame.id, privFrame.owner));
      } else if (id3Frame instanceof GeobFrame) {
        GeobFrame geobFrame = (GeobFrame) id3Frame;
        Log.i(TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
            geobFrame.id, geobFrame.mimeType, geobFrame.filename, geobFrame.description));
      } else {
        Log.i(TAG, String.format("ID3 TimedMetadata %s", id3Frame.id));
      }
    }
  }

  // SurfaceHolder.Callback implementation

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    if (player != null) {
      player.setSurface(holder.getSurface());
    }
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    // Do nothing.
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    if (player != null) {
      player.blockingClearSurface();
    }
  }

  private void configureSubtitleView() {
    CaptionStyleCompat style;
    float fontScale;
    if (Util.SDK_INT >= 19) {
      style = getUserCaptionStyleV19();
      fontScale = getUserCaptionFontScaleV19();
    } else {
      style = CaptionStyleCompat.DEFAULT;
      fontScale = 1.0f;
    }
    subtitleLayout.setStyle(style);
    subtitleLayout.setFractionalTextSize(SubtitleLayout.DEFAULT_TEXT_SIZE_FRACTION * fontScale);
  }

  @TargetApi(19)
  private float getUserCaptionFontScaleV19() {
    CaptioningManager captioningManager =
        (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);
    return captioningManager.getFontScale();
  }

  @TargetApi(19)
  private CaptionStyleCompat getUserCaptionStyleV19() {
    CaptioningManager captioningManager =
        (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);
    return CaptionStyleCompat.createFromCaptionStyle(captioningManager.getUserStyle());
  }

  /**
   * Makes a best guess to infer the type from a media {@link Uri} and an optional overriding file
   * extension.
   *
   * @param uri The {@link Uri} of the media.
   * @param fileExtension An overriding file extension.
   * @return The inferred type.
   */
  private static int inferContentType(Uri uri, String fileExtension) {
    String lastPathSegment = !TextUtils.isEmpty(fileExtension) ? "." + fileExtension
        : uri.getLastPathSegment();
    return Util.inferContentType(lastPathSegment);
  }


  private static final class KeyCompatibleMediaController extends MediaController {

    private MediaController.MediaPlayerControl playerControl;

    public KeyCompatibleMediaController(Context context) {
      super(context);
    }

    @Override
    public void setMediaPlayer(MediaController.MediaPlayerControl playerControl) {
      super.setMediaPlayer(playerControl);
      this.playerControl = playerControl;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
      int keyCode = event.getKeyCode();
      if (playerControl.canSeekForward() && keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
          playerControl.seekTo(playerControl.getCurrentPosition() + 15000); // milliseconds
          show();
        }
        return true;
      } else if (playerControl.canSeekBackward() && keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
          playerControl.seekTo(playerControl.getCurrentPosition() - 5000); // milliseconds
          show();
        }
        return true;
      }
      return super.dispatchKeyEvent(event);
    }
  }



}
