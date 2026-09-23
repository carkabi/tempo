package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RadioSchedule {
    @SerializedName("id")
    private int id;

    @SerializedName("version")
    private String version;

    @SerializedName("checksum")
    private String checksum;

    @SerializedName("tracks_count")
    private int tracksCount;

    @SerializedName("tracks")
    private List<RadioProgramItem> tracks;

    public int getId() { return id; }
    public String getVersion() { return version; }
    public String getChecksum() { return checksum; }
    public int getTracksCount() { return tracksCount; }
    public List<RadioProgramItem> getTracks() { return tracks; }
}
