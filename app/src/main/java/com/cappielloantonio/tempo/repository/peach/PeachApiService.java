package com.cappielloantonio.tempo.repository.peach;

import com.cappielloantonio.tempo.repository.peach.models.ContactRequest;
import com.cappielloantonio.tempo.repository.peach.models.ContactResponse;
import com.cappielloantonio.tempo.repository.peach.models.PeachBootstrapResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PeachApiService {
    @GET("api/peach/v1/app/bootstrap")
    Call<PeachBootstrapResponse> bootstrap(
            @Query("channel") String channel,
            @Query("version_code") int versionCode
    );

    @POST("api/peach/v1/contact-messages")
    Call<ContactResponse> sendContactMessage(
            @Body ContactRequest request
    );
}
