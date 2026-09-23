package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RadioManifestResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("server_time")
    private String serverTime;

    @SerializedName("package_id")
    private String packageId;

    @SerializedName("current_grid_checksum")
    private String currentGridChecksum;

    @SerializedName("next_grid_checksum")
    private String nextGridChecksum;

    @SerializedName("radios")
    private List<PeachRadioStation> radios;

    public String getStatus() { return status; }
    public String getServerTime() { return serverTime; }
    public String getPackageId() { return packageId; }
    public String getCurrentGridChecksum() { return currentGridChecksum; }
    public String getNextGridChecksum() { return nextGridChecksum; }
    public List<PeachRadioStation> getRadios() { return radios; }
}
