package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.annotations.SerializedName;

public class PeachNews {
    @SerializedName("id")
    private int id;

    @SerializedName("category")
    private String category;

    @SerializedName("category_label")
    private String categoryLabel;

    @SerializedName("title")
    private String title;

    @SerializedName("summary")
    private String summary;

    @SerializedName("content")
    private String content;

    @SerializedName("is_pinned")
    private boolean isPinned;

    @SerializedName("cover_image_url")
    private String coverImageUrl;

    @SerializedName("published_at")
    private String publishedAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("url")
    private String url;

    public int getId() { return id; }
    public String getCategory() { return category; }
    public String getCategoryLabel() { return categoryLabel; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }
    public boolean isPinned() { return isPinned; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public String getPublishedAt() { return publishedAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getUrl() { return url; }
}
