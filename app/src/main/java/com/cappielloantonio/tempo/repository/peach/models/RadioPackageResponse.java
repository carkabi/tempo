package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RadioPackageResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("server_time")
    private String serverTime;

    @SerializedName("package_id")
    private String packageId;

    @SerializedName("radios")
    private List<PeachPackageRadio> radios;

    public String getStatus() { return status; }
    public String getServerTime() { return serverTime; }
    public String getPackageId() { return packageId; }
    public List<PeachPackageRadio> getRadios() { return radios; }
}
