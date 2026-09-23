package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.annotations.SerializedName;

public class PeachApplication {
    @SerializedName("slug")
    private String slug;

    @SerializedName("name")
    private String name;

    @SerializedName("android_application_id")
    private String androidApplicationId;

    @SerializedName("channel")
    private String channel;

    @SerializedName("current_version_code")
    private int currentVersionCode;

    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getAndroidApplicationId() { return androidApplicationId; }
    public String getChannel() { return channel; }
    public int getCurrentVersionCode() { return currentVersionCode; }
}