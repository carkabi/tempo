package com.cappielloantonio.tempo.repository.tropikeau.models;

import com.google.gson.annotations.SerializedName;

public class SharedPlaylistAddRequest {
    @SerializedName("track_id")
    private final String trackId;

    public SharedPlaylistAddRequest(String trackId) {
        this.trackId = trackId;
    }
}
