package com.google.android.exoplayer.demo;

import android.util.Log;

import com.google.android.exoplayer.chunk.Chunk;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.chunk.FormatEvaluator;
import com.google.android.exoplayer.chunk.MediaChunk;
import com.google.android.exoplayer.upstream.BandwidthMeter;

import java.util.List;

/**
 * Created by lleej on 2016-05-20.
 */
public class LLEEJFormatEvaluator {


    public static final class AdaptiveEvaluator implements FormatEvaluator {

        public static final int DEFAULT_MAX_INITIAL_BITRATE = 800000;

        public static final int DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS = 10000;
        public static final int DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS = 25000;
        public static final int DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS = 25000;
        public static final float DEFAULT_BANDWIDTH_FRACTION = 0.75f;

        private final BandwidthMeter bandwidthMeter;

        private final int maxInitialBitrate;
        private final long minDurationForQualityIncreaseUs;
        private final long maxDurationForQualityDecreaseUs;
        private final long minDurationToRetainAfterDiscardUs;
        private final float bandwidthFraction;

        private boolean isInit = true;

        /**
         * @param bandwidthMeter Provides an estimate of the currently available bandwidth.
         */
        public AdaptiveEvaluator(BandwidthMeter bandwidthMeter) {
            this (bandwidthMeter, DEFAULT_MAX_INITIAL_BITRATE,
                    DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS,
                    DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
                    DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS, DEFAULT_BANDWIDTH_FRACTION);
        }

        /**
         * @param bandwidthMeter Provides an estimate of the currently available bandwidth.
         * @param maxInitialBitrate The maximum bitrate in bits per second that should be assumed
         *     when bandwidthMeter cannot provide an estimate due to playback having only just started.
         * @param minDurationForQualityIncreaseMs The minimum duration of buffered data required for
         *     the evaluator to consider switching to a higher quality format.
         * @param maxDurationForQualityDecreaseMs The maximum duration of buffered data required for
         *     the evaluator to consider switching to a lower quality format.
         * @param minDurationToRetainAfterDiscardMs When switching to a significantly higher quality
         *     format, the evaluator may discard some of the media that it has already buffered at the
         *     lower quality, so as to switch up to the higher quality faster. This is the minimum
         *     duration of media that must be retained at the lower quality.
         * @param bandwidthFraction The fraction of the available bandwidth that the evaluator should
         *     consider available for use. Setting to a value less than 1 is recommended to account
         *     for inaccuracies in the bandwidth estimator.
         */
        public AdaptiveEvaluator(BandwidthMeter bandwidthMeter,
                                 int maxInitialBitrate,
                                 int minDurationForQualityIncreaseMs,
                                 int maxDurationForQualityDecreaseMs,
                                 int minDurationToRetainAfterDiscardMs,
                                 float bandwidthFraction) {
            this.bandwidthMeter = bandwidthMeter;
            this.maxInitialBitrate = maxInitialBitrate;
            this.minDurationForQualityIncreaseUs = minDurationForQualityIncreaseMs * 1000L;
            this.maxDurationForQualityDecreaseUs = maxDurationForQualityDecreaseMs * 1000L;
            this.minDurationToRetainAfterDiscardUs = minDurationToRetainAfterDiscardMs * 1000L;
            this.bandwidthFraction = bandwidthFraction;
        }

        @Override
        public void enable() {
            // Do nothing.
        }

        @Override
        public void disable() {
            // Do nothing.
        }


        //LLEEJ : Evalutation 발생
        @Override
        public void evaluate(List<? extends MediaChunk> queue, long playbackPositionUs,
                             Format[] formats, Evaluation evaluation) {
            long bufferedDurationUs = queue.isEmpty() ? 0
                    : queue.get(queue.size() - 1).endTimeUs - playbackPositionUs;
            Format current = evaluation.format;
            Format ideal = determineIdealFormat(formats, bandwidthMeter.getBitrateEstimate());
            if(isInit) {
                for (int i = 0; i < formats.length; i++){
                    Log.d("LLEEJ","AVAILABLE FORMAT : " + formats[i].width +"x" + formats[i].height + " , " + formats[i].bitrate);
                }

                isInit = false;
            }

            boolean isHigher = ideal != null && current != null && ideal.bitrate > current.bitrate; // ideal
            boolean isLower = ideal != null && current != null && ideal.bitrate < current.bitrate;
            if (isHigher) {
                if (bufferedDurationUs < minDurationForQualityIncreaseUs) {
                    // The ideal format is a higher quality, but we have insufficient buffer to
                    // safely switch up. Defer switching up for now.
                    ideal = current;
                } else if (bufferedDurationUs >= minDurationToRetainAfterDiscardUs) {
                    // We're switching from an SD stream to a stream of higher resolution. Consider
                    // discarding already buffered media chunks. Specifically, discard media chunks starting
                    // from the first one that is of lower bandwidth, lower resolution and that is not HD.
                    for (int i = 1; i < queue.size(); i++) {
                        MediaChunk thisChunk = queue.get(i);
                        long durationBeforeThisSegmentUs = thisChunk.startTimeUs - playbackPositionUs;
                        if (durationBeforeThisSegmentUs >= minDurationToRetainAfterDiscardUs
                                && thisChunk.format.bitrate < ideal.bitrate
                                && thisChunk.format.height < ideal.height
                                && thisChunk.format.height < 720
                                && thisChunk.format.width < 1280) {
                            // Discard chunks from this one onwards.
                            evaluation.queueSize = i;
                            break;
                        }
                    }
                }
            } else if (isLower && current != null
                    && bufferedDurationUs >= maxDurationForQualityDecreaseUs) {
                // The ideal format is a lower quality, but we have sufficient buffer to defer switching
                // down for now.
                ideal = current;
            }
            if (current != null && ideal != current) {
                evaluation.trigger = Chunk.TRIGGER_ADAPTIVE;
            }

            isInit = false;
            evaluation.format = ideal;

        }

        /**
         * Compute the ideal format ignoring buffer health.
         */
        private Format determineIdealFormat(Format[] formats, long bitrateEstimate) {

            long effectiveBitrate = bitrateEstimate == BandwidthMeter.NO_ESTIMATE
                    ? maxInitialBitrate : (long) (bitrateEstimate * bandwidthFraction);

            if(Configure.BT_ON)
                effectiveBitrate = (long)(effectiveBitrate * Configure.BT_COMPENSATION_PARAMETER);

            for (int i = 0; i < formats.length; i++) {
                Format format = formats[i];
                if (format.bitrate <= effectiveBitrate) {
                    return format;
                }
            }
            // We didn't manage to calculate a suitable format. Return the lowest quality format.
            return formats[formats.length - 1];
        }

    }


    public static final class AdaptiveEvaluatorTest implements FormatEvaluator {

        public static final int DEFAULT_MAX_INITIAL_BITRATE = 800000;

        public static final int DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS = 10000;
        public static final int DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS = 25000;
        public static final int DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS = 25000;
        public static final float DEFAULT_BANDWIDTH_FRACTION = 0.75f;

        private final BandwidthMeter bandwidthMeter;

        private final int maxInitialBitrate;
        private final long minDurationForQualityIncreaseUs;
        private final long maxDurationForQualityDecreaseUs;
        private final long minDurationToRetainAfterDiscardUs;
        private final float bandwidthFraction;

        /**
         * @param bandwidthMeter Provides an estimate of the currently available bandwidth.
         */
        public AdaptiveEvaluatorTest(BandwidthMeter bandwidthMeter) {
            this(bandwidthMeter, DEFAULT_MAX_INITIAL_BITRATE,
                    DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS,
                    DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
                    DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS, DEFAULT_BANDWIDTH_FRACTION);
        }

        /**
         * @param bandwidthMeter                    Provides an estimate of the currently available bandwidth.
         * @param maxInitialBitrate                 The maximum bitrate in bits per second that should be assumed
         *                                          when bandwidthMeter cannot provide an estimate due to playback having only just started.
         * @param minDurationForQualityIncreaseMs   The minimum duration of buffered data required for
         *                                          the evaluator to consider switching to a higher quality format.
         * @param maxDurationForQualityDecreaseMs   The maximum duration of buffered data required for
         *                                          the evaluator to consider switching to a lower quality format.
         * @param minDurationToRetainAfterDiscardMs When switching to a significantly higher quality
         *                                          format, the evaluator may discard some of the media that it has already buffered at the
         *                                          lower quality, so as to switch up to the higher quality faster. This is the minimum
         *                                          duration of media that must be retained at the lower quality.
         * @param bandwidthFraction                 The fraction of the available bandwidth that the evaluator should
         *                                          consider available for use. Setting to a value less than 1 is recommended to account
         *                                          for inaccuracies in the bandwidth estimator.
         */
        public AdaptiveEvaluatorTest(BandwidthMeter bandwidthMeter,
                                     int maxInitialBitrate,
                                     int minDurationForQualityIncreaseMs,
                                     int maxDurationForQualityDecreaseMs,
                                     int minDurationToRetainAfterDiscardMs,
                                     float bandwidthFraction) {
            this.bandwidthMeter = bandwidthMeter;
            this.maxInitialBitrate = maxInitialBitrate;
            this.minDurationForQualityIncreaseUs = minDurationForQualityIncreaseMs * 1000L;
            this.maxDurationForQualityDecreaseUs = maxDurationForQualityDecreaseMs * 1000L;
            this.minDurationToRetainAfterDiscardUs = minDurationToRetainAfterDiscardMs * 1000L;
            this.bandwidthFraction = bandwidthFraction;
        }

        @Override
        public void enable() {
            // Do nothing.
        }

        @Override
        public void disable() {
            // Do nothing.
        }


        //LLEEJ : Evalutation 발생
        @Override
        public void evaluate(List<? extends MediaChunk> queue, long playbackPositionUs,
                             Format[] formats, Evaluation evaluation) {
            long bufferedDurationUs = queue.isEmpty() ? 0
                    : queue.get(queue.size() - 1).endTimeUs - playbackPositionUs;
            Format current = evaluation.format;
            Format ideal = determineIdealFormat(formats, bandwidthMeter.getBitrateEstimate());
            //LLEEJ :
            //Format fixed = formats[0];


            //Log.d("LLEEJ","ideal : " + ideal.height);
            boolean isHigher = ideal != null && current != null && ideal.bitrate > current.bitrate; // ideal
            boolean isLower = ideal != null && current != null && ideal.bitrate < current.bitrate;
            if (isHigher) {
                if (bufferedDurationUs < minDurationForQualityIncreaseUs) {
                    // The ideal format is a higher quality, but we have insufficient buffer to
                    // safely switch up. Defer switching up for now.
                    ideal = current;
                } else if (bufferedDurationUs >= minDurationToRetainAfterDiscardUs) {
                    // We're switching from an SD stream to a stream of higher resolution. Consider
                    // discarding already buffered media chunks. Specifically, discard media chunks starting
                    // from the first one that is of lower bandwidth, lower resolution and that is not HD.
                    for (int i = 1; i < queue.size(); i++) {
                        MediaChunk thisChunk = queue.get(i);
                        long durationBeforeThisSegmentUs = thisChunk.startTimeUs - playbackPositionUs;
                        if (durationBeforeThisSegmentUs >= minDurationToRetainAfterDiscardUs
                                && thisChunk.format.bitrate < ideal.bitrate
                                && thisChunk.format.height < ideal.height
                                && thisChunk.format.height < 720
                                && thisChunk.format.width < 1280) {
                            // Discard chunks from this one onwards.
                            evaluation.queueSize = i;
                            break;
                        }
                    }
                }
            } else if (isLower && current != null
                    && bufferedDurationUs >= maxDurationForQualityDecreaseUs) {
                // The ideal format is a lower quality, but we have sufficient buffer to defer switching
                // down for now.
                ideal = current;
            }
            if (current != null && ideal != current) {
                evaluation.trigger = Chunk.TRIGGER_ADAPTIVE;
            }

            evaluation.format = ideal;

        }

        /**
         * Compute the ideal format ignoring buffer health.
         */
        private Format determineIdealFormat(Format[] formats, long bitrateEstimate) {
            double newFraction = 1.2;
            long effectiveBitrate = bitrateEstimate == BandwidthMeter.NO_ESTIMATE
                    ? maxInitialBitrate : (long) (bitrateEstimate * newFraction);
            for (int i = 0; i < formats.length; i++) {
                Format format = formats[i];
                if (format.bitrate <= effectiveBitrate) {
                    return format;
                }
            }
            // We didn't manage to calculate a suitable format. Return the lowest quality format.
            return formats[formats.length - 1];
        }
    }


    public static final class AdaptiveEvaluatorFixed implements FormatEvaluator {

        public static final int DEFAULT_MAX_INITIAL_BITRATE = 800000;

        public static final int DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS = 10000;
        public static final int DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS = 25000;
        public static final int DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS = 25000;
        public static final float DEFAULT_BANDWIDTH_FRACTION = 0.75f;

        private final BandwidthMeter bandwidthMeter;

        private final int maxInitialBitrate;
        private final long minDurationForQualityIncreaseUs;
        private final long maxDurationForQualityDecreaseUs;
        private final long minDurationToRetainAfterDiscardUs;
        private final float bandwidthFraction;

        private boolean isInit = true;

        /**
         * @param bandwidthMeter Provides an estimate of the currently available bandwidth.
         */
        public AdaptiveEvaluatorFixed(BandwidthMeter bandwidthMeter) {
            this (bandwidthMeter, DEFAULT_MAX_INITIAL_BITRATE,
                    DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS,
                    DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
                    DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS, DEFAULT_BANDWIDTH_FRACTION);
        }

        /**
         * @param bandwidthMeter Provides an estimate of the currently available bandwidth.
         * @param maxInitialBitrate The maximum bitrate in bits per second that should be assumed
         *     when bandwidthMeter cannot provide an estimate due to playback having only just started.
         * @param minDurationForQualityIncreaseMs The minimum duration of buffered data required for
         *     the evaluator to consider switching to a higher quality format.
         * @param maxDurationForQualityDecreaseMs The maximum duration of buffered data required for
         *     the evaluator to consider switching to a lower quality format.
         * @param minDurationToRetainAfterDiscardMs When switching to a significantly higher quality
         *     format, the evaluator may discard some of the media that it has already buffered at the
         *     lower quality, so as to switch up to the higher quality faster. This is the minimum
         *     duration of media that must be retained at the lower quality.
         * @param bandwidthFraction The fraction of the available bandwidth that the evaluator should
         *     consider available for use. Setting to a value less than 1 is recommended to account
         *     for inaccuracies in the bandwidth estimator.
         */
        public AdaptiveEvaluatorFixed(BandwidthMeter bandwidthMeter,
                                      int maxInitialBitrate,
                                      int minDurationForQualityIncreaseMs,
                                      int maxDurationForQualityDecreaseMs,
                                      int minDurationToRetainAfterDiscardMs,
                                      float bandwidthFraction) {
            this.bandwidthMeter = bandwidthMeter;
            this.maxInitialBitrate = maxInitialBitrate;
            this.minDurationForQualityIncreaseUs = minDurationForQualityIncreaseMs * 1000L;
            this.maxDurationForQualityDecreaseUs = maxDurationForQualityDecreaseMs * 1000L;
            this.minDurationToRetainAfterDiscardUs = minDurationToRetainAfterDiscardMs * 1000L;
            this.bandwidthFraction = bandwidthFraction;
        }

        @Override
        public void enable() {
            // Do nothing.
        }

        @Override
        public void disable() {
            // Do nothing.
        }


        //LLEEJ : Evalutation 발생
        @Override
        public void evaluate(List<? extends MediaChunk> queue, long playbackPositionUs,
                             Format[] formats, Evaluation evaluation) {
            long bufferedDurationUs = queue.isEmpty() ? 0
                    : queue.get(queue.size() - 1).endTimeUs - playbackPositionUs;
            Format current = evaluation.format;
            Format ideal = determineIdealFormat(formats, bandwidthMeter.getBitrateEstimate());
            //LLEEJ :
            Format fixed = formats[0];


            Log.d("LLEEJ", "ideal : " + ideal.height);
            boolean isHigher = ideal != null && current != null && ideal.bitrate > current.bitrate; // ideal
            boolean isLower = ideal != null && current != null && ideal.bitrate < current.bitrate;
            if (isHigher) {
                if (bufferedDurationUs < minDurationForQualityIncreaseUs) {
                    // The ideal format is a higher quality, but we have insufficient buffer to
                    // safely switch up. Defer switching up for now.
                    ideal = current;
                } else if (bufferedDurationUs >= minDurationToRetainAfterDiscardUs) {
                    // We're switching from an SD stream to a stream of higher resolution. Consider
                    // discarding already buffered media chunks. Specifically, discard media chunks starting
                    // from the first one that is of lower bandwidth, lower resolution and that is not HD.
                    for (int i = 1; i < queue.size(); i++) {
                        MediaChunk thisChunk = queue.get(i);
                        long durationBeforeThisSegmentUs = thisChunk.startTimeUs - playbackPositionUs;
                        if (durationBeforeThisSegmentUs >= minDurationToRetainAfterDiscardUs
                                && thisChunk.format.bitrate < ideal.bitrate
                                && thisChunk.format.height < ideal.height
                                && thisChunk.format.height < 720
                                && thisChunk.format.width < 1280) {
                            // Discard chunks from this one onwards.
                            evaluation.queueSize = i;
                            break;
                        }
                    }
                }
            } else if (isLower && current != null
                    && bufferedDurationUs >= maxDurationForQualityDecreaseUs) {
                // The ideal format is a lower quality, but we have sufficient buffer to defer switching
                // down for now.
                ideal = current;
            }
            if (current != null && ideal != current) {
                evaluation.trigger = Chunk.TRIGGER_ADAPTIVE;
            }
            if(ideal.height == fixed.height)
                isInit = false;

            if(isInit)
                evaluation.format = ideal;
            else {
                evaluation.format = fixed;
                Log.d("LLEEJ", evaluation.format.height + " , Height fixed");
            }
        }

        /**
         * Compute the ideal format ignoring buffer health.
         */
        private Format determineIdealFormat(Format[] formats, long bitrateEstimate) {
            //LLEEJ : 평소 DASH의 정책, 측정된 bandwidth의 75%만 사용.
            //LLEEJ : BT ON일때, bandwidth의 90%까지 사용하게끔 시도
            long effectiveBitrate = bitrateEstimate == BandwidthMeter.NO_ESTIMATE
                    ? maxInitialBitrate : (long) (bitrateEstimate * bandwidthFraction);
            for (int i = 0; i < formats.length; i++) {
                Format format = formats[i];
                if (format.bitrate <= effectiveBitrate) {
                    return format;
                }
            }
            // We didn't manage to calculate a suitable format. Return the lowest quality format.
            return formats[formats.length - 1];
        }

    }

    //LLEEJ : BBA

    public static class BufferBasedAdaptiveEvaluator implements FormatEvaluator {

        public static final int DEFAULT_BUFFER_DURATION_MS = 30000;
        public static final int DEFAULT_RESERVOIR_DURATION_MS = 10000;

        public enum BufferState {
            STARTUP_STATE, STEADY_STATE
        }

        private BufferState bufferState;
        private final BandwidthMeter bandwidthMeter;

        //To check if we have to switch from startup to steady state
        private long prevBufferDurationUs;

        //when the video approach the end and we have the whole chunks in the buffer,
        //buffer occupancy starts decreasing, but it should not trigger f function to reduce
        // bandwidth
        private long videoDurationUs;


        public BufferBasedAdaptiveEvaluator(BandwidthMeter bandwidthMeter, long videoDurationMs) {
            this.bufferState = BufferState.STARTUP_STATE;
            this.bandwidthMeter = bandwidthMeter;
            this.prevBufferDurationUs = -1;
            this.videoDurationUs = videoDurationMs * 1000;
        }

        @Override
        public void enable() {

        }

        @Override
        public void disable() {

        }

        //Google Glass bitrates in formats array: 2235503 1119643 610891 247132 110307
        @Override
        public void evaluate(List<? extends MediaChunk> queue,
                             long playbackPositionUs, Format[] formats, Evaluation evaluation) {
            long bufferedDurationUs = queue.isEmpty() ? 0
                    : queue.get(queue.size() - 1).endTimeUs - playbackPositionUs;


            long bufferedEndTimeUs = queue.isEmpty() ? 0
                    : queue.get(queue.size() - 1).endTimeUs;

            Format current = evaluation.format;
            Format ideal;


            if (bufferState == BufferState.STARTUP_STATE) {
                if (prevBufferDurationUs != -1 && prevBufferDurationUs > bufferedDurationUs) {
                    bufferState = BufferState.STEADY_STATE;

                } else if (determineBufferBasedIdealFormat(formats, current, bufferedDurationUs).bitrate > determineCapacityBasedIdealFormat(formats, current, bufferedDurationUs).bitrate) {
                    bufferState = BufferState.STEADY_STATE;

                }
            }
            prevBufferDurationUs = bufferedDurationUs;


            if (videoDurationUs - bufferedEndTimeUs < 500000) {
                ideal = current;

            } else {
                if (bufferState == BufferState.STARTUP_STATE) {
                    ideal = determineCapacityBasedIdealFormat(formats, current, bufferedDurationUs);
                } else {
                    ideal = determineBufferBasedIdealFormat(formats, current, bufferedDurationUs);
                }
            }
            evaluation.format = ideal;

        }

        private int bitrateToFormatIndex(int bitrate, Format[] formats) {
            for (int i = 0; i < formats.length; i++) {
                if (bitrate == formats[i].bitrate) {
                    return i;
                }
            }
            return -1;
        }

        private Format determineBufferBasedIdealFormat(Format[] formats, Format current,
                                                       long bufferedDurationUs) {
            return formats[bufferOccupancyToFormatIndex(formats.length, bufferedDurationUs)];
        }

        private Format determineCapacityBasedIdealFormat(Format[] formats, Format current, long bufferedDurationUs) {
            /* //LLEEJ : Original BBA
            if (current == null) {
                return formats[formats.length - 1];
            } else if (bandwidthMeter.getBitrateEstimate() > bufferToStartupCoeff(bufferedDurationUs) * current.bitrate) {
                int idealIndex = bitrateToFormatIndex(current.bitrate, formats) - 1;
                if (idealIndex < 0) {
                    return formats[0];
                }
                return formats[idealIndex];

            }
            return current;
            */

            if (current == null) {
                return formats[formats.length - 1];
            } else{
                if(Configure.BT_ON){
                    if (bandwidthMeter.getBitrateEstimate() > bufferToStartupCoeff(bufferedDurationUs) * current.bitrate) {
                        int idealIndex = bitrateToFormatIndex(current.bitrate, formats) - 1;
                        if (idealIndex < 0) {
                            return formats[0];
                        }
                        return formats[idealIndex];

                    }
                    else
                        return current;
                }
                else{
                    if (bandwidthMeter.getBitrateEstimate() * Configure.BT_COMPENSATION_PARAMETER > bufferToStartupCoeff(bufferedDurationUs) * current.bitrate) {
                        int idealIndex = bitrateToFormatIndex(current.bitrate, formats) - 1;
                        if (idealIndex < 0) {
                            return formats[0];
                        }
                        return formats[idealIndex];

                    }
                    else
                        return current;
                }

            }
        }

        //this is the f function, it converts the buffer occupancy to formats indexes (linear function). formats array is sorted from high bitrate to low bitrate
        private int bufferOccupancyToFormatIndex(int formatsLen, long bufferedDurationUs) {
            /*
            if (bufferedDurationUs < DEFAULT_RESERVOIR_DURATION_MS * 1000) {//제일 안좋을때
                return formatsLen - 1;
            } else if (bufferedDurationUs > (DEFAULT_BUFFER_DURATION_MS * 0.9 * 1000)) { // 제일 좋을때
                return 0;
            } else {
                float bufferDurationIntervalUs = ((DEFAULT_BUFFER_DURATION_MS - DEFAULT_RESERVOIR_DURATION_MS) / (formatsLen - 1)) * 1000;


                return formatsLen - 2 - (int) ((bufferedDurationUs - (DEFAULT_RESERVOIR_DURATION_MS * 1000)) / bufferDurationIntervalUs);
            }
            */
            if(Configure.BT_ON) {
                if (bufferedDurationUs * Configure.BT_COMPENSATION_PARAMETER < DEFAULT_RESERVOIR_DURATION_MS * 1000) {//제일 안좋을때
                    return formatsLen - 1;
                } else if (bufferedDurationUs * Configure.BT_COMPENSATION_PARAMETER > (DEFAULT_BUFFER_DURATION_MS * 0.9 * 1000)) { // 제일 좋을때
                    return 0;
                } else {
                    double bufferDurationIntervalUs = ((DEFAULT_BUFFER_DURATION_MS - DEFAULT_RESERVOIR_DURATION_MS) / (formatsLen - 1)) * 1000;
                    bufferDurationIntervalUs = bufferDurationIntervalUs * Configure.BT_COMPENSATION_PARAMETER;


                    return formatsLen - 2 - (int) ((bufferedDurationUs - (DEFAULT_RESERVOIR_DURATION_MS * 1000)) / bufferDurationIntervalUs);
                }
            }
            else{
                if (bufferedDurationUs < DEFAULT_RESERVOIR_DURATION_MS * 1000) {//제일 안좋을때
                    return formatsLen - 1;
                } else if (bufferedDurationUs > (DEFAULT_BUFFER_DURATION_MS * 0.9 * 1000)) { // 제일 좋을때
                    return 0;
                } else {
                    double bufferDurationIntervalUs = ((DEFAULT_BUFFER_DURATION_MS - DEFAULT_RESERVOIR_DURATION_MS) / (formatsLen - 1)) * 1000;


                    return formatsLen - 2 - (int) ((bufferedDurationUs - (DEFAULT_RESERVOIR_DURATION_MS * 1000)) / bufferDurationIntervalUs);
                }
            }

        }

        //this is described in section 6 and it is for start-up phase
        private int bufferToStartupCoeff(long bufferedDurationUs) {
            if (bufferedDurationUs > DEFAULT_BUFFER_DURATION_MS * 0.9 * 1000) {
                return 2;
            } else if (bufferedDurationUs > DEFAULT_RESERVOIR_DURATION_MS * 1000) {
                return 4;
            } else {
                return 8;
            }

        }
    }
}
