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
package com.google.android.exoplayer.demo.player;

import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.ChunkSource;
import com.google.android.exoplayer.dash.DashChunkSource;
import com.google.android.exoplayer.dash.DefaultDashTrackSelector;
import com.google.android.exoplayer.dash.mpd.AdaptationSet;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescriptionParser;
import com.google.android.exoplayer.dash.mpd.Period;
import com.google.android.exoplayer.dash.mpd.Representation;
import com.google.android.exoplayer.dash.mpd.UtcTimingElement;
import com.google.android.exoplayer.dash.mpd.UtcTimingElementResolver;
import com.google.android.exoplayer.dash.mpd.UtcTimingElementResolver.UtcTimingCallback;
import com.google.android.exoplayer.demo.LLEEJFormatEvaluator;
import com.google.android.exoplayer.demo.SampleChooserActivity;
import com.google.android.exoplayer.demo.player.DemoPlayer.RendererBuilder;
import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.Util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;

/**
 * A {@link RendererBuilder} for DASH.
 */
public class DashRendererBuilder implements RendererBuilder {

  private static final String TAG = "DashRendererBuilder";

  private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
  //private static final int VIDEO_BUFFER_SEGMENTS = 200;
  private static final int VIDEO_BUFFER_SEGMENTS = 1000;
  private static final int AUDIO_BUFFER_SEGMENTS = 54;
  private static final int TEXT_BUFFER_SEGMENTS = 2;
  private static final int LIVE_EDGE_LATENCY_MS = 30000;

  private static final int SECURITY_LEVEL_UNKNOWN = -1;
  private static final int SECURITY_LEVEL_1 = 1;
  private static final int SECURITY_LEVEL_3 = 3;

  private final Context context;
  private final String userAgent;
  private final String url;
  private final MediaDrmCallback drmCallback;

  private AsyncRendererBuilder currentAsyncBuilder;

  private int selectedMode;

  public DashRendererBuilder(Context context, String userAgent, String url,
      MediaDrmCallback drmCallback, int selectedMode) {
    this.context = context;
    this.userAgent = userAgent;
    this.url = url;
    this.drmCallback = drmCallback;
    this.selectedMode = selectedMode;
  }

  @Override
  public void buildRenderers(DemoPlayer player) {
    currentAsyncBuilder = new AsyncRendererBuilder(context, userAgent, url, drmCallback, player, selectedMode);
    currentAsyncBuilder.init();
  }

  @Override
  public void cancel() {
    if (currentAsyncBuilder != null) {
      currentAsyncBuilder.cancel();
      currentAsyncBuilder = null;
    }
  }

  private static final class AsyncRendererBuilder
      implements ManifestFetcher.ManifestCallback<MediaPresentationDescription>, UtcTimingCallback {

    private final Context context;
    private final String userAgent;
    private final MediaDrmCallback drmCallback;
    private final DemoPlayer player;
    private final ManifestFetcher<MediaPresentationDescription> manifestFetcher;
    private final UriDataSource manifestDataSource;

    private boolean canceled;
    private MediaPresentationDescription manifest;
    private long elapsedRealtimeOffset;
    private int selectedMode;

    public AsyncRendererBuilder(Context context, String userAgent, String url,
        MediaDrmCallback drmCallback, DemoPlayer player, int selectedMode) {
      this.context = context;
      this.userAgent = userAgent;
      this.drmCallback = drmCallback;
      this.player = player;
      this.selectedMode = selectedMode;

      MediaPresentationDescriptionParser parser = new MediaPresentationDescriptionParser();
      //LLEEJ : DataSource
      manifestDataSource = new DefaultUriDataSource(context, userAgent);
      //LLEEJ : ManifestFetcher가 DataSource를 총체적으로 관리
      manifestFetcher = new ManifestFetcher<>(url, manifestDataSource, parser);
    }

    public void init() {
      Log.d("LLEEJ, MPD","DashRendererBulider : " + "AA");
      manifestFetcher.singleLoad(player.getMainHandler().getLooper(), this);
    }

    public void cancel() {
      canceled = true;
    }

    @Override
    public void onSingleManifest(MediaPresentationDescription manifest) {
      if (canceled) {
        return;
      }

      this.manifest = manifest;
      if (manifest.dynamic && manifest.utcTiming != null) {
        Log.d("LLEEJ, MPD","DD");
        UtcTimingElementResolver.resolveTimingElement(manifestDataSource, manifest.utcTiming,
            manifestFetcher.getManifestLoadCompleteTimestamp(), this);
      } else {
        buildRenderers();
      }
    }

    @Override
    public void onSingleManifestError(IOException e) {
      if (canceled) {
        return;
      }

      player.onRenderersError(e);
    }

    @Override
    public void onTimestampResolved(UtcTimingElement utcTiming, long elapsedRealtimeOffset) {
      if (canceled) {
        return;
      }

      this.elapsedRealtimeOffset = elapsedRealtimeOffset;
      buildRenderers();
    }

    @Override
    public void onTimestampError(UtcTimingElement utcTiming, IOException e) {
      if (canceled) {
        return;
      }

      Log.e(TAG, "Failed to resolve UtcTiming element [" + utcTiming + "]", e);
      // Be optimistic and continue in the hope that the device clock is correct.
      buildRenderers();
    }

    private void buildRenderers() {
      Period period = manifest.getPeriod(0);
      Log.d("LLEEJ, MPD","Period : " + manifest.getPeriodCount());
      Handler mainHandler = player.getMainHandler();
      LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
      DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(mainHandler, player);

      boolean hasContentProtection = false;
      for (int i = 0; i < period.adaptationSets.size(); i++) {
        AdaptationSet adaptationSet = period.adaptationSets.get(i);
        for(int j = 0; j < adaptationSet.representations.size(); j++){
          Representation representation = adaptationSet.representations.get(j);
          Log.d("LLEEJ, MPD2","ManifestFetcher onLoadCompleted(): " + representation.getFormat().width +","+representation.getFormat().height);
        }
        if (adaptationSet.type != AdaptationSet.TYPE_UNKNOWN) {
          Log.d("LLEEJ, MPD","AA");
          hasContentProtection |= adaptationSet.hasContentProtection();
        }
      }

      // Check drm support if necessary.
      boolean filterHdContent = false;
      StreamingDrmSessionManager drmSessionManager = null;
      if (hasContentProtection) {
        Log.d("LLEEJ, MPD","BB");
        if (Util.SDK_INT < 18) {
          player.onRenderersError(
              new UnsupportedDrmException(UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME));
          return;
        }
        try {
          Log.d("LLEEJ, MPD","CC");
          drmSessionManager = StreamingDrmSessionManager.newWidevineInstance(
              player.getPlaybackLooper(), drmCallback, null, player.getMainHandler(), player);
          filterHdContent = getWidevineSecurityLevel(drmSessionManager) != SECURITY_LEVEL_1;
        } catch (UnsupportedDrmException e) {
          player.onRenderersError(e);
          return;
        }
      }

      // Build the video renderer.
      //LLEEJ : 수정 전
      /*
      DataSource videoDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
      ChunkSource videoChunkSource = new DashChunkSource(manifestFetcher,
          DefaultDashTrackSelector.newVideoInstance(context, true, filterHdContent),
          videoDataSource, new AdaptiveEvaluator(bandwidthMeter), LIVE_EDGE_LATENCY_MS,
          elapsedRealtimeOffset, mainHandler, player, DemoPlayer.TYPE_VIDEO);
      ChunkSampleSource videoSampleSource = new ChunkSampleSource(videoChunkSource, loadControl,
          VIDEO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mainHandler, player,
          DemoPlayer.TYPE_VIDEO);
      TrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context, videoSampleSource,
          MediaCodecSelector.DEFAULT, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000,
          drmSessionManager, true, mainHandler, player, 50);
          */

      DataSource videoDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
      ChunkSource videoChunkSource = null;
      switch(selectedMode){
        case SampleChooserActivity.MODE_DASH:
        case SampleChooserActivity.MODE_DASH_RB_2S:
        case SampleChooserActivity.MODE_DASH_RB_4S:
          case SampleChooserActivity.MODE_DASH_RB_6S:
        case SampleChooserActivity.MODE_DASH_RB_10S:
        case SampleChooserActivity.MODE_DASH_RB_15S:
          videoChunkSource = new DashChunkSource(manifestFetcher,
                  DefaultDashTrackSelector.newVideoInstance(context, true, filterHdContent),
                  videoDataSource, new LLEEJFormatEvaluator.AdaptiveEvaluator(bandwidthMeter), LIVE_EDGE_LATENCY_MS,
                  elapsedRealtimeOffset, mainHandler, player, DemoPlayer.TYPE_VIDEO,true);
          Log.d("LLEEJ", "DASHRenderer selected Mode DASH");
          break;
//        case SampleChooserActivity.MODE_DASH_FIXED:
//          videoChunkSource = new DashChunkSource(manifestFetcher,
//                  DefaultDashTrackSelector.newVideoInstance(context, true, filterHdContent),
//                  videoDataSource, new LLEEJFormatEvaluator.AdaptiveEvaluatorFixed(bandwidthMeter), LIVE_EDGE_LATENCY_MS,
//                  elapsedRealtimeOffset, mainHandler, player, DemoPlayer.TYPE_VIDEO);
//          Log.d("LLEEJ", "DASHRenderer selected Mode DASH Fixed");
//          break;
//        case SampleChooserActivity.MODE_DASH_TEST1:
//          videoChunkSource = new DashChunkSource(manifestFetcher,
//                  DefaultDashTrackSelector.newVideoInstance(context, true, filterHdContent),
//                  videoDataSource, new LLEEJFormatEvaluator.AdaptiveEvaluatorTest(bandwidthMeter), LIVE_EDGE_LATENCY_MS,
//                  elapsedRealtimeOffset, mainHandler, player, DemoPlayer.TYPE_VIDEO);
//          break;
        case SampleChooserActivity.MODE_BBA:
        case SampleChooserActivity.MODE_BBA_RB_2S:
        case SampleChooserActivity.MODE_BBA_RB_4S:
          case SampleChooserActivity.MODE_BBA_RB_6S:
        case SampleChooserActivity.MODE_BBA_RB_10S:
        case SampleChooserActivity.MODE_BBA_RB_15S:
          videoChunkSource = new DashChunkSource(manifestFetcher,
                  DefaultDashTrackSelector.newVideoInstance(context, true, filterHdContent),
                  videoDataSource, new LLEEJFormatEvaluator.BufferBasedAdaptiveEvaluator(bandwidthMeter,SampleChooserActivity.VIDEO_DURATION, mainHandler, player), LIVE_EDGE_LATENCY_MS,
                  elapsedRealtimeOffset, mainHandler, player, DemoPlayer.TYPE_VIDEO,true);
          break;
      }


      ChunkSampleSource videoSampleSource = new ChunkSampleSource(videoChunkSource, loadControl,
              VIDEO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mainHandler, player,
              DemoPlayer.TYPE_VIDEO);
      TrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context, videoSampleSource,
              MediaCodecSelector.DEFAULT, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000,
              drmSessionManager, true, mainHandler, player, 50);

      // Build the audio renderer.
      DataSource audioDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
      ChunkSource audioChunkSource = new DashChunkSource(manifestFetcher,
          DefaultDashTrackSelector.newAudioInstance(), audioDataSource, null, LIVE_EDGE_LATENCY_MS,
          elapsedRealtimeOffset, mainHandler, player, DemoPlayer.TYPE_AUDIO);
      ChunkSampleSource audioSampleSource = new ChunkSampleSource(audioChunkSource, loadControl,
          AUDIO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mainHandler, player,
          DemoPlayer.TYPE_AUDIO);
      TrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(audioSampleSource,
          MediaCodecSelector.DEFAULT, drmSessionManager, true, mainHandler, player,
          AudioCapabilities.getCapabilities(context), AudioManager.STREAM_MUSIC);

      // Build the text renderer.
      DataSource textDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
      ChunkSource textChunkSource = new DashChunkSource(manifestFetcher,
          DefaultDashTrackSelector.newTextInstance(), textDataSource, null, LIVE_EDGE_LATENCY_MS,
          elapsedRealtimeOffset, mainHandler, player, DemoPlayer.TYPE_TEXT);
      ChunkSampleSource textSampleSource = new ChunkSampleSource(textChunkSource, loadControl,
          TEXT_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mainHandler, player,
          DemoPlayer.TYPE_TEXT);
      TrackRenderer textRenderer = new TextTrackRenderer(textSampleSource, player,
          mainHandler.getLooper());

      // Invoke the callback.
      TrackRenderer[] renderers = new TrackRenderer[DemoPlayer.RENDERER_COUNT];
      renderers[DemoPlayer.TYPE_VIDEO] = videoRenderer;
      renderers[DemoPlayer.TYPE_AUDIO] = audioRenderer;
      renderers[DemoPlayer.TYPE_TEXT] = textRenderer;
      player.onRenderers(renderers, bandwidthMeter);
    }

    private static int getWidevineSecurityLevel(StreamingDrmSessionManager sessionManager) {
      String securityLevelProperty = sessionManager.getPropertyString("securityLevel");
      return securityLevelProperty.equals("L1") ? SECURITY_LEVEL_1 : securityLevelProperty
          .equals("L3") ? SECURITY_LEVEL_3 : SECURITY_LEVEL_UNKNOWN;
    }

  }

}
