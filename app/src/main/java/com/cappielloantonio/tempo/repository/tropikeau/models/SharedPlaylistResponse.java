package com.cappielloantonio.tempo.repository.tropikeau.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SharedPlaylistResponse {
    private String status;
    private String name;
    private List<SharedPlaylistItem> items;
    private SharedPlaylistItem item;
    private Boolean duplicate;

    @SerializedName("removed_id")
    private Long removedId;

    public String getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }
    public List<SharedPlaylistItem> getItems() {
        return items;
    }

    public SharedPlaylistItem getItem() {
        return item;
    }

    public Boolean getDuplicate() {
        return duplicate;
    }

    public Long getRemovedId() {
        return removedId;
    }
}
