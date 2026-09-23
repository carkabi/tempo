package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class ContactErrorResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("code")
    private String code;

    @SerializedName("message")
    private String message;

    @SerializedName("errors")
    private JsonObject errors;

    public String getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public JsonObject getErrors() { return errors; }
}
