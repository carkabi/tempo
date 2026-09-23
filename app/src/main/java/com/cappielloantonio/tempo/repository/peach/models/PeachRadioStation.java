package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.annotations.SerializedName;

public class PeachRadioStation {
    @SerializedName("id")
    private String id;

    @SerializedName("slug")
    private String slug;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("cover_image_url")
    private String coverImageUrl;

    public PeachRadioStation(String id, String slug, String name, String description, String coverImageUrl) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
    }

    public String getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCoverImageUrl() { return coverImageUrl; }
}
