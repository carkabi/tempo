package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.annotations.SerializedName;

public class ContactDiagnostics {
    @SerializedName("app_version_name")
    private String appVersionName;

    @SerializedName("app_version_code")
    private int appVersionCode;

    @SerializedName("flavor")
    private String flavor;

    @SerializedName("android_version")
    private String androidVersion;

    @SerializedName("device_model")
    private String deviceModel;

    public ContactDiagnostics(String appVersionName, int appVersionCode, String flavor, String androidVersion, String deviceModel) {
        this.appVersionName = appVersionName;
        this.appVersionCode = appVersionCode;
        this.flavor = flavor;
        this.androidVersion = androidVersion;
        this.deviceModel = deviceModel;
    }

    public String getAppVersionName() { return appVersionName; }
    public int getAppVersionCode() { return appVersionCode; }
    public String getFlavor() { return flavor; }
    public String getAndroidVersion() { return androidVersion; }
    public String getDeviceModel() { return deviceModel; }
}
