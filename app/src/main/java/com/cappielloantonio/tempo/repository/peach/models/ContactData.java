package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.annotations.SerializedName;

public class ContactData {
    @SerializedName("id")
    private int id;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
