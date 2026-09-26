package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class PeachUpdate implements Serializable {
    private static final long serialVersionUID = 1L;
    @SerializedName("available")
    private boolean available;

    @SerializedName("mandatory")
    private boolean mandatory;

    @SerializedName("id")
    private int id;

    @SerializedName("channel")
    private String channel;

    @SerializedName("version_code")
    private int versionCode;

    @SerializedName("version_name")
    private String versionName;

    @SerializedName("title")
    private String title;

    @SerializedName("changelog")
    private String changelog;

    @SerializedName("minimum_supported_version_code")
    private int minimumSupportedVersionCode;

    @SerializedName("size_bytes")
    private Long sizeBytes;

    @SerializedName("sha256")
    private String sha256;

    @SerializedName("published_at")
    private String publishedAt;

    @SerializedName("download_url")
    private String downloadUrl;

    public boolean isAvailable() { return available; }
    public boolean isMandatory() { return mandatory; }
    public int getId() { return id; }
    public String getChannel() { return channel; }
    public int getVersionCode() { return versionCode; }
    public String getVersionName() { return versionName; }
    public String getTitle() { return title; }
    public String getChangelog() { return changelog; }
    public int getMinimumSupportedVersionCode() { return minimumSupportedVersionCode; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public String getPublishedAt() { return publishedAt; }
    public String getDownloadUrl() { return downloadUrl; }
}
