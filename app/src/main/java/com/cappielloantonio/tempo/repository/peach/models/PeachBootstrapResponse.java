package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PeachBootstrapResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("server_time")
    private String serverTime;

    @SerializedName("application")
    private PeachApplication application;

    @SerializedName("update")
    private PeachUpdate update;

    @SerializedName("notices")
    private List<PeachNotice> notices;

    @SerializedName("news")
    private List<PeachNews> news;

    public String getStatus() { return status; }
    public String getServerTime() { return serverTime; }
    public PeachApplication getApplication() { return application; }
    public PeachUpdate getUpdate() { return update; }
    public List<PeachNotice> getNotices() { return notices; }
    public List<PeachNews> getNews() { return news; }
}
