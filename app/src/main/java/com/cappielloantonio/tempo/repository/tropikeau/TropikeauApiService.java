package com.cappielloantonio.tempo.repository.tropikeau;

import com.cappielloantonio.tempo.repository.peach.models.RadioManifestResponse;
import com.cappielloantonio.tempo.repository.peach.models.RadioPackageResponse;
import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequest;
import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequestResponse;
import com.cappielloantonio.tempo.repository.tropikeau.models.SharedPlaylistAddRequest;
import com.cappielloantonio.tempo.repository.tropikeau.models.SharedPlaylistResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TropikeauApiService {
    @POST("api/peach/v1/music-requests")
    Call<MusicRequestResponse> addMusicRequest(
            @Header("X-Peach-Username") String username,
            @Header("X-Peach-Token") String token,
            @Header("X-Peach-Salt") String salt,
            @Body MusicRequest request
    );

    @GET("api/peach/v1/music-requests/{requestId}/status")
    Call<MusicRequestResponse> getRequestStatus(
            @Header("X-Peach-Username") String username,
            @Header("X-Peach-Token") String token,
            @Header("X-Peach-Salt") String salt,
            @Path("requestId") int requestId
    );

    @GET("api/peach/v1/music-requests")
    Call<MusicRequestResponse> getHistory(
            @Header("X-Peach-Username") String username,
            @Header("X-Peach-Token") String token,
            @Header("X-Peach-Salt") String salt,
            @Query("limit") int limit
    );

    @GET("api/peach/v1/radios/manifest")
    Call<RadioManifestResponse> getRadioManifest(
            @Header("X-Peach-Username") String username,
            @Header("X-Peach-Token") String token,
            @Header("X-Peach-Salt") String salt
    );

    @GET("api/peach/v1/radios/package")
    Call<RadioPackageResponse> getRadioPackage(
            @Header("X-Peach-Username") String username,
            @Header("X-Peach-Token") String token,
            @Header("X-Peach-Salt") String salt
    );

    @GET("api/peach/v1/shared-playlist")
    Call<SharedPlaylistResponse> getSharedPlaylist(
            @Header("X-Peach-Username") String username,
            @Header("X-Peach-Token") String token,
            @Header("X-Peach-Salt") String salt
    );

    @POST("api/peach/v1/shared-playlist")
    Call<SharedPlaylistResponse> addSharedPlaylistTrack(
            @Header("X-Peach-Username") String username,
            @Header("X-Peach-Token") String token,
            @Header("X-Peach-Salt") String salt,
            @Body SharedPlaylistAddRequest request
    );

    @DELETE("api/peach/v1/shared-playlist/{itemId}")
    Call<SharedPlaylistResponse> removeSharedPlaylistTrack(
            @Header("X-Peach-Username") String username,
            @Header("X-Peach-Token") String token,
            @Header("X-Peach-Salt") String salt,
            @Path("itemId") long itemId
    );
}
