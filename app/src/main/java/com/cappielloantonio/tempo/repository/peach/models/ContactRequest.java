package com.cappielloantonio.tempo.repository.peach.models;

import com.google.gson.annotations.SerializedName;

public class ContactRequest {
    @SerializedName("category")
    private String category;

    @SerializedName("name")
    private String name;

    @SerializedName("reply_email")
    private String replyEmail;

    @SerializedName("subject")
    private String subject;

    @SerializedName("message")
    private String message;

    @SerializedName("diagnostics")
    private ContactDiagnostics diagnostics;

    public ContactRequest(String category, String name, String replyEmail, String subject, String message, ContactDiagnostics diagnostics) {
        this.category = category;
        this.name = name;
        this.replyEmail = replyEmail;
        this.subject = subject;
        this.message = message;
        this.diagnostics = diagnostics;
    }

    public String getCategory() { return category; }
    public String getName() { return name; }
    public String getReplyEmail() { return replyEmail; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public ContactDiagnostics getDiagnostics() { return diagnostics; }
}
