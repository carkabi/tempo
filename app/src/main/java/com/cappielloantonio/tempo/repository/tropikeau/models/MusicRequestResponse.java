package com.cappielloantonio.tempo.repository.tropikeau.models;

import com.google.gson.annotations.SerializedName;

public class MusicRequestResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("request")
    private MusicRequestData request;

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public MusicRequestData getRequest() { return request; }
}