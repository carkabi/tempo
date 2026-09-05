package com.cappielloantonio.tempo.repository.tropikeau.models;

import com.google.gson.annotations.SerializedName;

public class MusicRequest {
    @SerializedName("source_url")
    private String sourceUrl;

    public MusicRequest(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }
}