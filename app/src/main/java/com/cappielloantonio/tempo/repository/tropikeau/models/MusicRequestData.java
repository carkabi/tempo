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

    @SerializedName("progress_stage")
    private String progressStage;

    @SerializedName("total_items")
    private Integer totalItems;

    @SerializedName("completed_items")
    private Integer completedItems;

    @SerializedName("failed_items")
    private Integer failedItems;

    @SerializedName("current_title")
    private String currentTitle;

    @SerializedName("is_terminal")
    private boolean isTerminal;

    @SerializedName("error_message")
    private String errorMessage;

    @SerializedName("started_at")
    private String startedAt;

    @SerializedName("finished_at")
    private String finishedAt;

    @SerializedName("visible_until")
    private String visibleUntil;

    public Integer getId() { return id; }
    public String getStatus() { return status; }
    public String getProgressStage() { return progressStage; }
    public String getSourcePlatform() { return sourcePlatform; }
    public String getSourceType() { return sourceType; }
    public String getSourceUrl() { return sourceUrl; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public Integer getQueuePosition() { return queuePosition; }
    public Integer getTotalItems() { return totalItems; }
    public Integer getCompletedItems() { return completedItems; }
    public Integer getFailedItems() { return failedItems; }
    public String getCurrentTitle() { return currentTitle; }
    public boolean isTerminal() { return isTerminal; }
    public String getErrorMessage() { return errorMessage; }
    public String getCreatedAt() { return createdAt; }
    public String getStartedAt() { return startedAt; }
    public String getFinishedAt() { return finishedAt; }
    public String getVisibleUntil() { return visibleUntil; }
}