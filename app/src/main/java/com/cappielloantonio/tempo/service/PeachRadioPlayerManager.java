package com.cappielloantonio.tempo.service;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaBrowser;
import androidx.media3.session.SessionToken;

import com.cappielloantonio.tempo.repository.peach.models.PeachRadioStation;
import com.cappielloantonio.tempo.repository.peach.models.RadioProgramItem;
import com.cappielloantonio.tempo.util.Constants;
import com.cappielloantonio.tempo.util.MusicUtil;
import com.cappielloantonio.tempo.util.PeachRadioCache;
import com.cappielloantonio.tempo.util.Preferences;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

@OptIn(markerClass = UnstableApi.class)
public class PeachRadioPlayerManager {

    private static final String TAG = "PEACH_RADIO";

    public interface RadioLiveCallback {
        void onTrackChanged(PeachRadioStation station, RadioProgramItem currentItem, RadioProgramItem nextItem);
        void onLiveResynced(long positionMs);
    }

    public interface TrackDownloadCallback {
        void onDownloaded(File localFile, int statusCode, long sizeBytes);
        void onError(int statusCode, String message);
    }

    private static RadioLiveCallback liveCallback;
    private static PeachRadioStation activeStation;
    private static String activeRadioSlug;
    private static RadioProgramItem activeProgramItem;

    private static boolean isMonitoring = false;
    private static final long MONITOR_INTERVAL_MS = 1500;
    private static final long RADIO_FADE_MS = 1500;
    private static long playRequestGeneration = 0L;
    private static final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private static Runnable monitorRunnable;

    private static PeachCrossfadeEngine crossfadeEngine;
    private static ListenableFuture<MediaBrowser> radioMediaBrowserFuture;

    private static synchronized ListenableFuture<MediaBrowser> ensureRadioBrowser(
            Context context
    ) {
        if (radioMediaBrowserFuture == null) {
            Context appContext = context.getApplicationContext();
            radioMediaBrowserFuture = new MediaBrowser.Builder(
                    appContext,
                    new SessionToken(
                            appContext,
                            new ComponentName(
                                    appContext,
                                    MediaService.class
                            )
                    )
            ).buildAsync();
        }
        return radioMediaBrowserFuture;
    }

    public static void setRadioLiveCallback(RadioLiveCallback callback) {
        liveCallback = callback;
    }

    public static String getActiveRadioSlug() {
        return activeRadioSlug;
    }

    public static PeachRadioStation getActiveStation() {
        return activeStation;
    }

    public static RadioProgramItem getActiveProgramItem() {
        return activeProgramItem;
    }

    public static PeachCrossfadeEngine getCrossfadeEngine() {
        return crossfadeEngine;
    }

    public static void startPeachRadio(Context context, ListenableFuture<MediaBrowser> mediaBrowserFuture, PeachRadioStation station) {
        if (station == null || station.getSlug() == null) return;

        Log.i(TAG, "Step 3: PeachRadioPlayerManager.startPeachRadio() station = " + station.getSlug());

        if (crossfadeEngine == null && context != null) {
            crossfadeEngine = new PeachCrossfadeEngine(context);
        }

        activeStation = station;
        activeRadioSlug = station.getSlug();

        ListenableFuture<MediaBrowser> radioBrowser =
                ensureRadioBrowser(context);

        playLiveCurrentTrack(context, radioBrowser, true);
        startMonitoring(context, radioBrowser);
    }

    public static void downloadTrackLocally(Context context, String trackId, TrackDownloadCallback callback) {
        if (context == null || trackId == null) return;

        File cacheDir = new File(context.getCacheDir(), "peach-radio");
        if (!cacheDir.exists()) {
            boolean created = cacheDir.mkdirs();
            if (!created && !cacheDir.exists()) {
                Log.e(TAG, "Failed to create cache directory: " + cacheDir.getAbsolutePath());
            }
        }

        File targetFile = new File(cacheDir, trackId + ".mp3");
        if (targetFile.exists() && targetFile.length() > 0) {
            Log.i(TAG, "Track " + trackId + " already in cache (" + targetFile.length() + " bytes)");
            callback.onDownloaded(targetFile, 200, targetFile.length());
            return;
        }

        Uri streamUri = MusicUtil.getStreamUri(trackId);
        if (streamUri == null || streamUri.toString().startsWith("null") || !streamUri.toString().contains("http")) {
            String serverUrl = Preferences.getInUseServerAddress();
            if (serverUrl == null || serverUrl.isEmpty()) {
                serverUrl = Constants.NAVIDROME_SERVER_URL;
            }
            String user = Preferences.getUser();
            String token = Preferences.getToken();
            String salt = Preferences.getSalt();
            String urlStr = serverUrl + (serverUrl.endsWith("/") ? "" : "/") + "rest/stream?id=" + trackId
                    + "&u=" + (user != null ? user : "") + "&t=" + (token != null ? token : "")
                    + "&s=" + (salt != null ? salt : "") + "&v=1.16.1&c=Tempo";
            streamUri = Uri.parse(urlStr);
        }

        Log.i(TAG, "Downloading track " + trackId + " from URL: " + streamUri.toString().replaceAll("p=[^&]*", "p=REDACTED").replaceAll("t=[^&]*", "t=REDACTED"));

        Request request = new Request.Builder()
                .url(streamUri.toString())
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Download failed for track " + trackId + ": " + e.getMessage());
                callback.onError(-1, e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                int code = response.code();
                if (response.isSuccessful() && response.body() != null) {
                    File tempFile = new File(cacheDir, trackId + ".tmp");
                    try (InputStream is = response.body().byteStream();
                         FileOutputStream fos = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                        fos.flush();
                    }

                    if (tempFile.exists() && tempFile.length() > 0) {
                        if (targetFile.exists()) {
                            boolean deleted = targetFile.delete();
                            if (!deleted) {
                                Log.w(TAG, "Failed to delete existing target file before rename");
                            }
                        }
                        boolean renamed = tempFile.renameTo(targetFile);
                        if (renamed) {
                            Log.i(TAG, "Track " + trackId + " successfully downloaded to " + targetFile.getAbsolutePath() + " (" + targetFile.length() + " bytes)");
                            callback.onDownloaded(targetFile, code, targetFile.length());
                        } else {
                            Log.e(TAG, "Failed to rename temp file to target file for track " + trackId);
                            callback.onError(code, "Rename temp file failed");
                        }
                    } else {
                        Log.e(TAG, "Downloaded temp file is empty or missing for track " + trackId);
                        callback.onError(code, "Temp file empty or missing");
                    }
                } else {
                    Log.e(TAG, "HTTP error " + code + " downloading track " + trackId);
                    callback.onError(code, "HTTP " + code);
                }
            }
        });
    }

    public static boolean playLiveCurrentTrack(
            Context context,
            ListenableFuture<MediaBrowser> mediaBrowserFuture,
            boolean forceSeek
    ) {
        return playLiveCurrentTrack(
                context,
                mediaBrowserFuture,
                forceSeek,
                false
        );
    }

    private static boolean playLiveCurrentTrack(
            Context context,
            ListenableFuture<MediaBrowser> mediaBrowserFuture,
            boolean forceSeek,
            boolean withFade
    ) {
        if (activeRadioSlug == null) return false;

        final long requestGeneration =
                ++playRequestGeneration;

        RadioProgramItem currentItem = PeachRadioCache.findActiveProgramItem(activeRadioSlug);

        if (currentItem == null || currentItem.getNavidromeTrackId() == null) {
            Log.w(TAG, "No active program item found for radio: " + activeRadioSlug);
            return false;
        }

        activeProgramItem = currentItem;
        RadioProgramItem nextItem = PeachRadioCache.findNextProgramItem(activeRadioSlug, currentItem);

        Log.i(TAG, "radio = " + activeRadioSlug);
        Log.i(TAG, "track position = " + currentItem.getPosition());
        Log.i(TAG, "title = " + currentItem.getTitle());

        File cacheDir = new File(context.getCacheDir(), "peach-radio");
        File cachedCurrent = new File(
                cacheDir,
                currentItem.getNavidromeTrackId() + ".mp3"
        );

        File immediateSource =
                cachedCurrent.exists() && cachedCurrent.length() > 0
                        ? cachedCurrent
                        : null;

        // Update Media3 immediately. The player UI now follows the real
        // radio programme without waiting for a complete cache download.
        executeExoPlayerPlayback(
                context,
                mediaBrowserFuture,
                currentItem,
                nextItem,
                immediateSource,
                forceSeek,
                withFade,
                requestGeneration
        );

        if (nextItem != null && nextItem.getNavidromeTrackId() != null) {
            downloadTrackLocally(context, nextItem.getNavidromeTrackId(), new TrackDownloadCallback() {
                @Override
                public void onDownloaded(File localFile, int statusCode, long sizeBytes) {
                    Log.i(TAG, "Pre-downloaded next track " + nextItem.getNavidromeTrackId() + " (" + sizeBytes + " bytes)");
                }

                @Override
                public void onError(int statusCode, String message) {
                    Log.w(TAG, "Pre-download of next track failed: " + message);
                }
            });
        }

        if (liveCallback != null && activeStation != null) {
            liveCallback.onTrackChanged(activeStation, currentItem, nextItem);
        }

        return true;
    }

    private static void executeExoPlayerPlayback(
            Context context,
            ListenableFuture<MediaBrowser> mediaBrowserFuture,
            RadioProgramItem currentItem,
            RadioProgramItem nextItem,
            File localFile,
            boolean forceSeek,
            boolean withFade,
            long requestGeneration
    ) {
        if (mediaBrowserFuture == null) return;

        long seekPositionMs = PeachRadioCache.calculateStreamPositionMs(currentItem);

        Log.i(TAG, "ExoPlayer playback: radio=" + activeRadioSlug
                + ", title=" + currentItem.getTitle()
                + ", navidrome_track_id=" + currentItem.getNavidromeTrackId()
                + ", durationMs=" + currentItem.getDurationMs()
                + ", seekPositionMs=" + seekPositionMs + "ms"
                + (localFile != null ? " (LOCAL FILE: " + localFile.getAbsolutePath() + ")" : " (REMOTE STREAM)"));

        Uri mediaUri;
        if (localFile != null && localFile.exists()) {
            mediaUri = Uri.fromFile(localFile);
        } else {
            mediaUri = MusicUtil.getStreamUri(currentItem.getNavidromeTrackId());
        }

        String displayArtist =
                currentItem.getArtist() != null
                        ? currentItem.getArtist()
                        : (
                        activeStation != null
                                ? activeStation.getName()
                                : "Peach Radio"
                );

        Uri artworkUri = null;
        if (
                currentItem.getCoverImageUrl() != null
                && !currentItem.getCoverImageUrl().trim().isEmpty()
        ) {
            artworkUri = Uri.parse(
                    currentItem.getCoverImageUrl()
            );
        } else if (
                activeStation != null
                && activeStation.getCoverImageUrl() != null
                && !activeStation.getCoverImageUrl().trim().isEmpty()
        ) {
            artworkUri = Uri.parse(
                    activeStation.getCoverImageUrl()
            );
        }

        Bundle bundle = new Bundle();
        bundle.putString("type", Constants.MEDIA_TYPE_RADIO);
        bundle.putString(
                "id",
                currentItem.getNavidromeTrackId()
        );
        bundle.putString(
                "title",
                currentItem.getTitle()
        );
        bundle.putString(
                "artist",
                displayArtist
        );
        bundle.putString(
                "album",
                currentItem.getAlbum()
        );
        bundle.putString(
                "coverArtId",
                currentItem.getCoverArtId()
        );
        bundle.putString(
                "peach_radio_slug",
                activeRadioSlug
        );

        MediaMetadata metadata =
                new MediaMetadata.Builder()
                        .setTitle(currentItem.getTitle())
                        .setArtist(displayArtist)
                        .setAlbumTitle(
                                activeStation != null
                                        ? activeStation.getName()
                                        : "Peach Radio"
                        )
                        .setArtworkUri(artworkUri)
                        .setExtras(bundle)
                        .build();

        MediaItem mediaItem =
                new MediaItem.Builder()
                        .setUri(mediaUri)
                        .setMediaId(
                                "peach_radio_"
                                        + currentItem.getNavidromeTrackId()
                        )
                        .setMediaMetadata(metadata)
                        .setCustomCacheKey(
                                currentItem.getNavidromeTrackId()
                        )
                        .setTag(bundle)
                        .build();

        mediaBrowserFuture.addListener(() -> {
            try {
                MediaBrowser browser =
                        mediaBrowserFuture.get();

                if (
                        browser == null
                        || requestGeneration
                        != playRequestGeneration
                ) {
                    return;
                }

                if (
                        withFade
                        && browser.isPlaying()
                ) {
                    fadeOutAndReplace(
                            browser,
                            mediaItem,
                            seekPositionMs,
                            requestGeneration
                    );
                } else {
                    browser.setVolume(1f);
                    replaceRadioMedia(
                            browser,
                            mediaItem,
                            seekPositionMs,
                            forceSeek
                    );
                }
            } catch (Exception e) {
                Log.e(
                        TAG,
                        "Error starting MediaBrowser playback for Peach Radio: "
                                + e.getMessage(),
                        e
                );
            }
        }, MoreExecutors.directExecutor());
    }

    private static void fadeOutAndReplace(
            MediaBrowser browser,
            MediaItem mediaItem,
            long seekPositionMs,
            long requestGeneration
    ) {
        final int steps = 9;
        final long stepDuration =
                Math.max(40L, RADIO_FADE_MS / steps);

        for (int step = 1; step <= steps; step++) {
            final int currentStep = step;

            mainHandler.postDelayed(
                    () -> {
                        if (
                                requestGeneration
                                != playRequestGeneration
                        ) {
                            return;
                        }

                        float volume =
                                1f - (
                                        currentStep
                                        / (float) steps
                                );

                        browser.setVolume(
                                Math.max(0f, volume)
                        );
                    },
                    currentStep * stepDuration
            );
        }

        mainHandler.postDelayed(
                () -> {
                    if (
                            requestGeneration
                            != playRequestGeneration
                    ) {
                        return;
                    }

                    replaceRadioMedia(
                            browser,
                            mediaItem,
                            seekPositionMs,
                            true
                    );

                    browser.setVolume(0f);

                    for (
                            int step = 1;
                            step <= steps;
                            step++
                    ) {
                        final int currentStep = step;

                        mainHandler.postDelayed(
                                () -> {
                                    if (
                                            requestGeneration
                                            != playRequestGeneration
                                    ) {
                                        return;
                                    }

                                    browser.setVolume(
                                            Math.min(
                                                    1f,
                                                    currentStep
                                                            / (float) steps
                                            )
                                    );
                                },
                                currentStep * stepDuration
                        );
                    }
                },
                RADIO_FADE_MS
        );
    }

    private static void replaceRadioMedia(
            MediaBrowser browser,
            MediaItem mediaItem,
            long seekPositionMs,
            boolean forceSeek
    ) {
        browser.setMediaItem(mediaItem);
        browser.prepare();

        if (
                forceSeek
                || Math.abs(
                        browser.getCurrentPosition()
                                - seekPositionMs
                ) > 3000
        ) {
            browser.seekTo(seekPositionMs);
        }

        browser.play();
    }

    public static void startMonitoring(Context context, ListenableFuture<MediaBrowser> mediaBrowserFuture) {
        if (isMonitoring) return;
        isMonitoring = true;

        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isMonitoring || activeRadioSlug == null) return;

                RadioProgramItem currentActive = PeachRadioCache.findActiveProgramItem(activeRadioSlug);
                if (currentActive != null && activeProgramItem != null) {
                    if (!currentActive.getNavidromeTrackId().equals(activeProgramItem.getNavidromeTrackId()) || currentActive.getPosition() != activeProgramItem.getPosition()) {
                        Log.i(TAG, "Track changed in live schedule, transitioning to next track...");
                        playLiveCurrentTrack(
                                context,
                                mediaBrowserFuture,
                                true,
                                true
                        );
                    }
                }

                mainHandler.postDelayed(this, MONITOR_INTERVAL_MS);
            }
        };

        mainHandler.postDelayed(monitorRunnable, MONITOR_INTERVAL_MS);
    }

    public static void stopMonitoring() {
        isMonitoring = false;
        if (monitorRunnable != null) {
            mainHandler.removeCallbacks(monitorRunnable);
            monitorRunnable = null;
        }
    }
}
