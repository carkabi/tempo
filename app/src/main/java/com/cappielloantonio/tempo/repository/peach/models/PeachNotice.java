package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.annotations.SerializedName;

public class PeachNotice {
    @SerializedName("id")
    private int id;

    @SerializedName("notification_key")
    private String notificationKey;

    @SerializedName("category")
    private String category;

    @SerializedName("level")
    private String level;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("send_notification")
    private boolean sendNotification;

    @SerializedName("starts_at")
    private String startsAt;

    @SerializedName("ends_at")
    private String endsAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("url")
    private String url;

    public int getId() { return id; }
    public String getNotificationKey() { return notificationKey; }
    public String getCategory() { return category; }
    public String getLevel() { return level; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isSendNotification() { return sendNotification; }
    public String getStartsAt() { return startsAt; }
    public String getEndsAt() { return endsAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getUrl() { return url; }
}
