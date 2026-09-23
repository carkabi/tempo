package com.cappielloantonio.tempo.repository.tropikeau.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MusicRequestResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("request")
    private MusicRequestData request;

    @SerializedName("requests")
    private List<MusicRequestData> requests;

    @SerializedName("meta")
    private MusicRequestMeta meta;

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public MusicRequestData getRequest() { return request; }
    public List<MusicRequestData> getRequests() { return requests; }
    public MusicRequestMeta getMeta() { return meta; }
}