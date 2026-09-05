package com.cappielloantonio.tempo.repository.tropikeau;

import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequest;
import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequestResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface TropikeauApiService {
    @POST("api/peach/v1/music-requests")
    Call<MusicRequestResponse> addMusicRequest(
            @Header("X-Peach-Username") String username,
            @Header("X-Peach-Token") String token,
            @Header("X-Peach-Salt") String salt,
            @Body MusicRequest request
    );
}