package com.cappielloantonio.tempo.service;

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
    private static final long MONITOR_INTERVAL_MS = 15000;
    private static final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private static Runnable monitorRunnable;

    private static PeachCrossfadeEngine crossfadeEngine;

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

        playLiveCurrentTrack(context, mediaBrowserFuture, true);
        startMonitoring(context, mediaBrowserFuture);
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

    public static boolean playLiveCurrentTrack(Context context, ListenableFuture<MediaBrowser> mediaBrowserFuture, boolean forceSeek) {
        if (activeRadioSlug == null) return false;

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

        downloadTrackLocally(context, currentItem.getNavidromeTrackId(), new TrackDownloadCallback() {
            @Override
            public void onDownloaded(File localFile, int statusCode, long sizeBytes) {
                Log.i(TAG, "Local download complete (" + sizeBytes + " bytes), starting ExoPlayer playback from file: " + localFile.getAbsolutePath());
                mainHandler.post(() -> executeExoPlayerPlayback(context, mediaBrowserFuture, currentItem, nextItem, localFile, forceSeek));
            }

            @Override
            public void onError(int statusCode, String message) {
                Log.e(TAG, "Local download failed (" + statusCode + ": " + message + "), falling back to streaming URL...");
                mainHandler.post(() -> executeExoPlayerPlayback(context, mediaBrowserFuture, currentItem, nextItem, null, forceSeek));
            }
        });

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

    private static void executeExoPlayerPlayback(Context context, ListenableFuture<MediaBrowser> mediaBrowserFuture, RadioProgramItem currentItem, RadioProgramItem nextItem, File localFile, boolean forceSeek) {
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

        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(currentItem.getTitle())
                .setArtist(currentItem.getArtist() != null ? currentItem.getArtist() : (activeStation != null ? activeStation.getName() : "Peach Radio"))
                .setAlbumTitle(activeStation != null ? activeStation.getName() : "Peach Radio")
                .setArtworkUri(activeStation != null && activeStation.getCoverImageUrl() != null ? Uri.parse(activeStation.getCoverImageUrl()) : null)
                .build();

        Bundle bundle = new Bundle();
        bundle.putString("type", Constants.MEDIA_TYPE_RADIO);
        bundle.putString("id", currentItem.getNavidromeTrackId());
        bundle.putString("peach_radio_slug", activeRadioSlug);

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(mediaUri)
                .setMediaId("peach_radio_" + currentItem.getNavidromeTrackId())
                .setMediaMetadata(metadata)
                .setCustomCacheKey(currentItem.getNavidromeTrackId())
                .setTag(bundle)
                .build();

        mediaBrowserFuture.addListener(() -> {
            try {
                MediaBrowser browser = mediaBrowserFuture.get();
                if (browser != null) {
                    browser.setMediaItem(mediaItem);
                    browser.prepare();
                    if (forceSeek || Math.abs(browser.getCurrentPosition() - seekPositionMs) > 3000) {
                        browser.seekTo(seekPositionMs);
                    }
                    browser.play();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error starting MediaBrowser playback for Peach Radio: " + e.getMessage(), e);
            }
        }, MoreExecutors.directExecutor());
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
                        playLiveCurrentTrack(context, mediaBrowserFuture, true);
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
