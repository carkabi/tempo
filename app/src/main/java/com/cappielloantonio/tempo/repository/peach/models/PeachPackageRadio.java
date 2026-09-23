package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.annotations.SerializedName;

public class PeachPackageRadio {
    @SerializedName("id")
    private int id;

    @SerializedName("slug")
    private String slug;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("cover_url")
    private String coverUrl;

    @SerializedName("crossfade_ms")
    private long crossfadeMs;

    @SerializedName("current_schedule")
    private RadioSchedule currentSchedule;

    @SerializedName("next_schedule")
    private RadioSchedule nextSchedule;

    public int getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCoverUrl() { return coverUrl; }
    public long getCrossfadeMs() { return crossfadeMs; }
    public RadioSchedule getCurrentSchedule() { return currentSchedule; }
    public RadioSchedule getNextSchedule() { return nextSchedule; }
}
