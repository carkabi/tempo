package com.cappielloantonio.tempo.repository.tropikeau.models;

import com.google.gson.annotations.SerializedName;

public class MusicRequestData {
    @SerializedName("id")
    private int id;

    @SerializedName("status")
    private String status;

    @SerializedName("source_platform")
    private String sourcePlatform;

    @SerializedName("source_type")
    private String sourceType;

    @SerializedName("source_url")
    private String sourceUrl;

    @SerializedName("title")
    private String title;

    @SerializedName("artist")
    private String artist;

    @SerializedName("queue_position")
    private Integer queuePosition;

    @SerializedName("created_at")
    private String createdAt;

    public Integer getId() { return id; }
    public String getStatus() { return status; }
    public String getSourcePlatform() { return sourcePlatform; }
    public String getSourceType() { return sourceType; }
    public String getSourceUrl() { return sourceUrl; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public Integer getQueuePosition() { return queuePosition; }
    public String getCreatedAt() { return createdAt; }
}