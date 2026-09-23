package com.cappielloantonio.tempo.repository.tropikeau.models;

import com.google.gson.annotations.SerializedName;

public class MusicRequestMeta {
    @SerializedName("count")
    private int count;

    @SerializedName("limit")
    private int limit;

    @SerializedName("has_active")
    private boolean hasActive;

    @SerializedName("refresh_after_seconds")
    private int refreshAfterSeconds;

    public int getCount() { return count; }
    public int getLimit() { return limit; }
    public boolean isHasActive() { return hasActive; }
    public int getRefreshAfterSeconds() { return refreshAfterSeconds; }
}