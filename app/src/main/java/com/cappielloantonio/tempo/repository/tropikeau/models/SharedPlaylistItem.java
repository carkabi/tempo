package com.cappielloantonio.tempo.repository.tropikeau.models;

import com.google.gson.annotations.SerializedName;

public class SharedPlaylistItem {
    private long id;

    @SerializedName("track_id")
    private String trackId;

    private String title;
    private String artist;
    private String album;

    @SerializedName("cover_art_id")
    private String coverArtId;

    private int duration;
    @SerializedName("added_by")
    private String addedBy;

    @SerializedName("added_at")
    private String addedAt;

    @SerializedName("can_remove")
    private boolean canRemove;

    public long getId() {
        return id;
    }

    public String getTrackId() {
        return trackId;
    }

    public String getTitle() {
        return title;
    }
    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getCoverArtId() {
        return coverArtId;
    }

    public int getDuration() {
        return duration;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public String getAddedAt() {
        return addedAt;
    }

    public boolean canRemove() {
        return canRemove;
    }
}
