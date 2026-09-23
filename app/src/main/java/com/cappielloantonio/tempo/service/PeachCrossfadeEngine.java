package com.cappielloantonio.tempo.service;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DecoderReuseEvaluation;
import androidx.media3.exoplayer.ExoPlayer;

import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import com.cappielloantonio.tempo.repository.peach.models.RadioProgramItem;
import com.cappielloantonio.tempo.util.DownloadUtil;
import com.cappielloantonio.tempo.util.MusicUtil;
import com.cappielloantonio.tempo.util.PeachRadioCache;

import java.io.File;

@OptIn(markerClass = UnstableApi.class)
public class PeachCrossfadeEngine {

    private static final String TAG = "PEACH_RADIO";

    private final Context context;
    private final AudioManager audioManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ExoPlayer playerA;
    private ExoPlayer playerB;

    private ExoPlayer primaryPlayer;
    private ExoPlayer secondaryPlayer;

    private RadioProgramItem primaryTrack;
    private RadioProgramItem secondaryTrack;

    private boolean isCrossfading = false;

    public PeachCrossfadeEngine(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        initPlayers();
    }

    private void initPlayers() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build();

        DefaultMediaSourceFactory mediaSourceFactory =
                new DefaultMediaSourceFactory(context)
                        .setDataSourceFactory(DownloadUtil.getDataSourceFactory(context));

        playerA = new ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setAudioAttributes(audioAttributes, true)
                .build();

        playerB = new ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setAudioAttributes(audioAttributes, true)
                .build();

        attachListeners(playerA, "playerA");
        attachListeners(playerB, "playerB");

        primaryPlayer = playerA;
        secondaryPlayer = playerB;
    }

    private void attachListeners(ExoPlayer player, String name) {
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                String stateName = playbackState == Player.STATE_READY ? "READY" : playbackState == Player.STATE_BUFFERING ? "BUFFERING" : playbackState == Player.STATE_ENDED ? "ENDED" : "IDLE";
                Log.i(TAG, name + " playbackState = " + stateName + " (" + playbackState + ")");
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.i(TAG, name + " isPlaying = " + isPlaying);
            }

            @Override
            public void onTracksChanged(Tracks tracks) {
                for (Tracks.Group group : tracks.getGroups()) {
                    if (group.getType() == C.TRACK_TYPE_AUDIO) {
                        for (int i = 0; i < group.length; i++) {
                            if (group.isTrackSelected(i)) {
                                Format format = group.getTrackFormat(i);
                                Log.i(TAG, name + " Selected Audio Track: mime=" + format.sampleMimeType + ", codec=" + format.codecs + ", sampleRate=" + format.sampleRate + "Hz, channels=" + format.channelCount);
                            }
                        }
                    }
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, name + " playback error = " + error.getMessage() + " (errorCode=" + error.errorCode + ")", error);
            }
        });

        player.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onAudioDecoderInitialized(@NonNull EventTime eventTime, @NonNull String decoderName, long initializedTimestampMs, long initializationDurationMs) {
                Log.i(TAG, name + " audio decoder initialized = " + decoderName);
            }

            @Override
            public void onAudioInputFormatChanged(@NonNull EventTime eventTime, @NonNull Format format, @Nullable DecoderReuseEvaluation decoderReuseEvaluation) {
                Log.i(TAG, name + " audio input format changed = mime:" + format.sampleMimeType + ", sampleRate:" + format.sampleRate + ", channels:" + format.channelCount);
            }

            @Override
            public void onAudioSessionIdChanged(@NonNull EventTime eventTime, int audioSessionId) {
                Log.i(TAG, name + " audio session id = " + audioSessionId);
            }

            @Override
            public void onAudioSinkError(@NonNull EventTime eventTime, @NonNull Exception audioSinkError) {
                Log.e(TAG, name + " audio sink error = " + audioSinkError.getMessage(), audioSinkError);
            }

            @Override
            public void onAudioUnderrun(@NonNull EventTime eventTime, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
                Log.w(TAG, name + " audio underrun: bufferSize=" + bufferSize + ", bufferSizeMs=" + bufferSizeMs);
            }
        });
    }

    public static float calculateOutgoingVolume(float progress) {
        float x = Math.max(0f, Math.min(1f, progress));
        return (float) Math.cos(x * Math.PI / 2.0);
    }

    public static float calculateIncomingVolume(float progress) {
        float x = Math.max(0f, Math.min(1f, progress));
        return (float) Math.sin(x * Math.PI / 2.0);
    }

    public void playSingleTrackLocalFile(RadioProgramItem track, File localFile, long positionMs) {
        if (track == null || localFile == null || !localFile.exists()) return;

        stopCrossfade();

        primaryTrack = track;
        secondaryTrack = null;

        Uri localUri = Uri.fromFile(localFile);
        Log.i(TAG, "PeachCrossfadeEngine: primaryPlayer playing LOCAL file " + localFile.getAbsolutePath() + " (" + localFile.length() + " bytes) at " + positionMs + " ms");
        Log.i(TAG, "ExoPlayer source = LOCAL");

        logAudioOutputDiagnostics("t=0 (local file play)");

        MediaItem mediaItem = new MediaItem.Builder()
                .setMediaId("peach_local_" + track.getNavidromeTrackId())
                .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
                .setUri(localUri)
                .build();

        primaryPlayer.stop();
        primaryPlayer.clearMediaItems();
        primaryPlayer.setMediaItem(mediaItem);
        primaryPlayer.setVolume(1.0f);
        primaryPlayer.prepare();
        primaryPlayer.seekTo(positionMs);
        primaryPlayer.play();

        secondaryPlayer.stop();
        secondaryPlayer.clearMediaItems();

        logPlayerMetricsAtTime("t=0", primaryPlayer);
        mainHandler.postDelayed(() -> logPlayerMetricsAtTime("t=3s", primaryPlayer), 3000);
    }

    public void playSingleTrack(RadioProgramItem track) {
        if (track == null) return;

        stopCrossfade();

        primaryTrack = track;
        secondaryTrack = null;

        long positionMs = PeachRadioCache.calculateStreamPositionMs(track);
        Uri streamUri = MusicUtil.getStreamUri(track.getNavidromeTrackId());

        String maskedUri = streamUri.getScheme() + "://" + streamUri.getHost() + streamUri.getPath() + "?id=" + track.getNavidromeTrackId();
        Log.i(TAG, "PeachCrossfadeEngine: primaryPlayer starting single track " + track.getTitle() + " at " + positionMs + " ms, streamUri = " + maskedUri);

        logAudioOutputDiagnostics("t=0 (before play)");

        MediaItem mediaItem = new MediaItem.Builder()
                .setMediaId("peach_crossfade_" + track.getNavidromeTrackId())
                .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
                .setUri(streamUri)
                .build();

        primaryPlayer.stop();
        primaryPlayer.clearMediaItems();
        primaryPlayer.setMediaItem(mediaItem);
        primaryPlayer.setVolume(1.0f);
        primaryPlayer.prepare();
        primaryPlayer.seekTo(positionMs);
        primaryPlayer.play();

        secondaryPlayer.stop();
        secondaryPlayer.clearMediaItems();

        logPlayerMetricsAtTime("t=0", primaryPlayer);
        mainHandler.postDelayed(() -> logPlayerMetricsAtTime("t=3s", primaryPlayer), 3000);
    }

    public void startMidCrossfade(RadioProgramItem outgoingTrack, RadioProgramItem incomingTrack, float progress) {
        if (outgoingTrack == null || incomingTrack == null) return;

        isCrossfading = true;
        primaryTrack = outgoingTrack;
        secondaryTrack = incomingTrack;

        float outVol = calculateOutgoingVolume(progress);
        float inVol = calculateIncomingVolume(progress);

        long outPosMs = PeachRadioCache.calculateStreamPositionMs(outgoingTrack);
        long inPosMs = PeachRadioCache.calculateStreamPositionMs(incomingTrack);

        Uri outUri = MusicUtil.getStreamUri(outgoingTrack.getNavidromeTrackId());
        Uri inUri = MusicUtil.getStreamUri(incomingTrack.getNavidromeTrackId());

        Log.i(TAG, "PeachCrossfadeEngine: midCrossfade start progress = " + progress + " (outVol=" + outVol + ", inVol=" + inVol + ")");
        logAudioOutputDiagnostics("t=0 (midCrossfade)");

        MediaItem outItem = new MediaItem.Builder()
                .setMediaId("peach_out_" + outgoingTrack.getNavidromeTrackId())
                .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
                .setUri(outUri)
                .build();

        MediaItem inItem = new MediaItem.Builder()
                .setMediaId("peach_in_" + incomingTrack.getNavidromeTrackId())
                .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
                .setUri(inUri)
                .build();

        primaryPlayer.stop();
        primaryPlayer.clearMediaItems();
        primaryPlayer.setMediaItem(outItem);
        primaryPlayer.setVolume(outVol);
        primaryPlayer.prepare();
        primaryPlayer.seekTo(outPosMs);
        primaryPlayer.play();

        secondaryPlayer.stop();
        secondaryPlayer.clearMediaItems();
        secondaryPlayer.setMediaItem(inItem);
        secondaryPlayer.setVolume(inVol);
        secondaryPlayer.prepare();
        secondaryPlayer.seekTo(inPosMs);
        secondaryPlayer.play();

        logPlayerMetricsAtTime("t=0 (primary)", primaryPlayer);
        logPlayerMetricsAtTime("t=0 (secondary)", secondaryPlayer);
        mainHandler.postDelayed(() -> {
            logPlayerMetricsAtTime("t=3s (primary)", primaryPlayer);
            logPlayerMetricsAtTime("t=3s (secondary)", secondaryPlayer);
        }, 3000);
    }

    public void preloadNextTrack(RadioProgramItem nextTrack) {
        if (nextTrack == null || secondaryTrack != null) return;

        secondaryTrack = nextTrack;
        Uri streamUri = MusicUtil.getStreamUri(nextTrack.getNavidromeTrackId());

        Log.i(TAG, "PeachCrossfadeEngine: secondaryPlayer preloading " + nextTrack.getTitle());

        MediaItem mediaItem = new MediaItem.Builder()
                .setMediaId("peach_preload_" + nextTrack.getNavidromeTrackId())
                .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
                .setUri(streamUri)
                .build();

        secondaryPlayer.stop();
        secondaryPlayer.clearMediaItems();
        secondaryPlayer.setMediaItem(mediaItem);
        secondaryPlayer.setVolume(0.0f);
        secondaryPlayer.prepare();
    }

    public void updateCrossfadeVolumes(float progress) {
        float outVol = calculateOutgoingVolume(progress);
        float inVol = calculateIncomingVolume(progress);

        if (primaryPlayer != null) primaryPlayer.setVolume(outVol);
        if (secondaryPlayer != null) secondaryPlayer.setVolume(inVol);

        if (progress >= 1.0f) {
            completeCrossfade();
        }
    }

    private void completeCrossfade() {
        isCrossfading = false;

        Log.i(TAG, "PeachCrossfadeEngine: completeCrossfade swapping players");

        primaryPlayer.stop();
        primaryPlayer.clearMediaItems();

        ExoPlayer tempPlayer = primaryPlayer;
        primaryPlayer = secondaryPlayer;
        secondaryPlayer = tempPlayer;

        primaryTrack = secondaryTrack;
        secondaryTrack = null;

        if (primaryPlayer != null) {
            primaryPlayer.setVolume(1.0f);
        }
    }

    private void logAudioOutputDiagnostics(String label) {
        if (audioManager != null) {
            int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            boolean isMute = audioManager.isStreamMute(AudioManager.STREAM_MUSIC);
            boolean bluetoothOn = audioManager.isBluetoothA2dpOn();
            boolean headsetOn = audioManager.isWiredHeadsetOn();

            Log.i(TAG, "AudioOutputDiagnostics [" + label + "]: systemMediaVolume = " + currentVol + "/" + maxVol
                    + ", isMuted = " + isMute + ", bluetoothA2dp = " + bluetoothOn + ", wiredHeadset = " + headsetOn
                    + ", primaryPlayerVolume = " + (primaryPlayer != null ? primaryPlayer.getVolume() : "null")
                    + ", secondaryPlayerVolume = " + (secondaryPlayer != null ? secondaryPlayer.getVolume() : "null"));
        }
    }

    private void logPlayerMetricsAtTime(String timeLabel, ExoPlayer player) {
        if (player == null) return;
        boolean playing = player.isPlaying();
        int state = player.getPlaybackState();
        String stateStr = state == Player.STATE_READY ? "READY" : state == Player.STATE_BUFFERING ? "BUFFERING" : state == Player.STATE_ENDED ? "ENDED" : "IDLE";
        long pos = player.getCurrentPosition();
        long buf = player.getBufferedPosition();
        float vol = player.getVolume();

        Log.i(TAG, "PlayerMetrics [" + timeLabel + "]: isPlaying=" + playing + ", playbackState=" + stateStr + " (" + state + "), currentPosition=" + pos + " ms, bufferedPosition=" + buf + " ms, volume=" + vol);
    }

    public void stopCrossfade() {
        isCrossfading = false;
        if (primaryPlayer != null) {
            primaryPlayer.stop();
            primaryPlayer.clearMediaItems();
        }
        if (secondaryPlayer != null) {
            secondaryPlayer.stop();
            secondaryPlayer.clearMediaItems();
        }
        primaryTrack = null;
        secondaryTrack = null;
    }

    public void release() {
        stopCrossfade();
        if (playerA != null) {
            playerA.release();
            playerA = null;
        }
        if (playerB != null) {
            playerB.release();
            playerB = null;
        }
    }

    public boolean isCrossfading() { return isCrossfading; }
    public ExoPlayer getPrimaryPlayer() { return primaryPlayer; }
    public ExoPlayer getSecondaryPlayer() { return secondaryPlayer; }
    public RadioProgramItem getPrimaryTrack() { return primaryTrack; }
    public RadioProgramItem getSecondaryTrack() { return secondaryTrack; }
}
