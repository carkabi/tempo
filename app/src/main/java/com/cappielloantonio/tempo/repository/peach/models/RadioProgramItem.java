package com.cappielloantonio.tempo.repository.peach.models;

import android.os.Build;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class RadioProgramItem {
    @SerializedName("position")
    private int position;

    @SerializedName("radio_slug")
    private String radioSlug;

    @SerializedName("navidrome_track_id")
    private String navidromeTrackId;

    @SerializedName("title")
    private String title;

    @SerializedName("artist")
    private String artist;

    @SerializedName("album")
    private String album;

    @SerializedName("cover_art_id")
    private String coverArtId;

    @SerializedName("cover_image_url")
    private String coverImageUrl;

    @SerializedName("duration_ms")
    private long durationMs;

    @SerializedName("starts_at")
    private String startsAt;

    @SerializedName("ends_at")
    private String endsAt;

    @SerializedName("play_from_ms")
    private long playFromMs;

    @SerializedName("play_until_ms")
    private long playUntilMs;

    @SerializedName("fade_in_ms")
    private long fadeInMs;

    @SerializedName("fade_out_ms")
    private long fadeOutMs;

    public RadioProgramItem() {}

    public RadioProgramItem(String radioSlug, String navidromeTrackId, String title, String artist, String album, String coverArtId, String coverImageUrl, long durationMs, String startsAt, String endsAt, long playFromMs, long playUntilMs, long fadeInMs, long fadeOutMs) {
        this.radioSlug = radioSlug;
        this.navidromeTrackId = navidromeTrackId;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.coverArtId = coverArtId;
        this.coverImageUrl = coverImageUrl;
        this.durationMs = durationMs;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.playFromMs = playFromMs;
        this.playUntilMs = playUntilMs;
        this.fadeInMs = fadeInMs;
        this.fadeOutMs = fadeOutMs;
    }

    public int getPosition() { return position; }
    public String getRadioSlug() { return radioSlug; }
    public void setRadioSlug(String radioSlug) { this.radioSlug = radioSlug; }
    public String getNavidromeTrackId() { return navidromeTrackId; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getCoverArtId() { return coverArtId; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public long getDurationMs() { return durationMs; }
    public String getStartsAt() { return startsAt; }
    public String getEndsAt() { return endsAt; }
    public long getPlayFromMs() { return playFromMs; }
    public long getPlayUntilMs() { return playUntilMs; }
    public long getFadeInMs() { return fadeInMs; }
    public long getFadeOutMs() { return fadeOutMs; }

    public long getStartsAtMs() {
        return parseTimeMs(startsAt);
    }

    public long getEndsAtMs() {
        return parseTimeMs(endsAt);
    }

    public static long parseTimeMs(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return 0L;
        String s = timeStr.trim();
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli();
                }
            } catch (Exception ignored) {}

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return java.time.Instant.parse(s).toEpochMilli();
                }
            } catch (Exception ignored) {}

            String[] patterns = {
                    "yyyy-MM-dd'T'HH:mm:ssXXX",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd'T'HH:mm:ssZ",
                    "yyyy-MM-dd'T'HH:mm:ss"
            };
            for (String pattern : patterns) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    return sdf.parse(s).getTime();
                } catch (Exception ignored) {}
            }
            return 0L;
        }
    }
}
