package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.annotations.SerializedName;

public class ContactResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("contact")
    private ContactData contact;

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public ContactData getContact() { return contact; }
}
