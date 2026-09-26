package com.cappielloantonio.tempo.repository.peach;

import androidx.annotation.NonNull;

import com.cappielloantonio.tempo.BuildConfig;
import com.cappielloantonio.tempo.repository.peach.models.ContactErrorResponse;
import com.cappielloantonio.tempo.repository.peach.models.ContactRequest;
import com.cappielloantonio.tempo.repository.peach.models.ContactResponse;
import com.cappielloantonio.tempo.repository.peach.models.PeachBootstrapResponse;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PeachRepository {

    private final PeachApiService apiService;

    public PeachRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://tropikeau.fr/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build())
                .build();
        apiService = retrofit.create(PeachApiService.class);
    }

    public PeachRepository(PeachApiService apiService) {
        this.apiService = apiService;
    }

    public static String getUpdateChannel() {
        return BuildConfig.DEBUG
                ? "beta"
                : "stable";
    }

    public void bootstrap(PeachCallback callback) {
        String channel = getUpdateChannel();
        apiService.bootstrap(channel, BuildConfig.VERSION_CODE).enqueue(new Callback<PeachBootstrapResponse>() {
            @Override
            public void onResponse(@NonNull Call<PeachBootstrapResponse> call, @NonNull Response<PeachBootstrapResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(response.code(), "Erreur serveur Tropikeau (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<PeachBootstrapResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Impossible de se connecter au serveur";
                callback.onError(-1, "Erreur réseau Tropikeau : " + errorMsg);
            }
        });
    }

    public PeachBootstrapResponse bootstrapSync() throws IOException {
        String channel = getUpdateChannel();
        Response<PeachBootstrapResponse> response = apiService.bootstrap(channel, BuildConfig.VERSION_CODE).execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        }
        return null;
    }

    public void sendContactMessage(ContactRequest request, ContactCallback callback) {
        apiService.sendContactMessage(request).enqueue(new Callback<ContactResponse>() {
            @Override
            public void onResponse(@NonNull Call<ContactResponse> call, @NonNull Response<ContactResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else if (response.code() == 422 && response.errorBody() != null) {
                    try {
                        String errorJson = response.errorBody().string();
                        ContactErrorResponse errorObj = new Gson().fromJson(errorJson, ContactErrorResponse.class);
                        String msg = (errorObj != null && errorObj.getMessage() != null) ? errorObj.getMessage() : "Erreur de validation (422)";
                        callback.onValidationError(msg, errorObj);
                    } catch (Exception e) {
                        callback.onError(response.code(), "Erreur de validation (" + response.code() + ")");
                    }
                } else if (response.code() == 429) {
                    callback.onError(429, "Trop de messages envoyés. Veuillez patienter avant de réessayer.");
                } else {
                    callback.onError(response.code(), "Erreur serveur (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ContactResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Erreur réseau";
                callback.onError(-1, errorMsg);
            }
        });
    }

    public interface PeachCallback {
        void onSuccess(PeachBootstrapResponse response);
        void onError(int code, String message);
    }

    public interface ContactCallback {
        void onSuccess(ContactResponse response);
        void onValidationError(String message, ContactErrorResponse errorResponse);
        void onError(int code, String message);
    }
}
