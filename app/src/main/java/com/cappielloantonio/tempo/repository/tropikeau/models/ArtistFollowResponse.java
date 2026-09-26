package com.cappielloantonio.tempo.repository.tropikeau.models;

import com.google.gson.annotations.SerializedName;

public class ArtistFollowResponse {
    private String status;
    private boolean available;
    private boolean following;
    private Boolean created;
    private String message;

    @SerializedName("artist_name")
    private String artistName;

    public String getStatus() { return status; }
    public boolean isAvailable() { return available; }
    public boolean isFollowing() { return following; }
    public Boolean getCreated() { return created; }
    public String getMessage() { return message; }
    public String getArtistName() { return artistName; }
}
