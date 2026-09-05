package com.cappielloantonio.tempo.repository.tropikeau;

import androidx.annotation.NonNull;

import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequest;
import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequestResponse;
import com.cappielloantonio.tempo.subsonic.utils.StringUtil;
import com.cappielloantonio.tempo.util.Preferences;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TropikeauRepository {

    private final TropikeauApiService apiService;
    private final SecureRandom secureRandom = new SecureRandom();

    public TropikeauRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://tropikeau.fr/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder()
                        .connectTimeout(20, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .build())
                .build();
        apiService = retrofit.create(TropikeauApiService.class);
    }

    public void addMusic(String spotifyUrl, TropikeauCallback callback) {
        String username = Preferences.getUser();
        String password = Preferences.getPassword();

        if (username == null || password == null) {
            callback.onError(401, "Authentification Navidrome manquante.");
            return;
        }

        String salt = generateSalt();
        String token = StringUtil.tokenize(password + salt);

        MusicRequest request = new MusicRequest(spotifyUrl);

        apiService.addMusicRequest(username, token, salt, request).enqueue(new Callback<MusicRequestResponse>() {
            @Override
            public void onResponse(@NonNull Call<MusicRequestResponse> call, @NonNull Response<MusicRequestResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(response.code(), getErrorMessage(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MusicRequestResponse> call, @NonNull Throwable t) {
                callback.onError(-1, "Erreur réseau : " + t.getMessage());
            }
        });
    }

    private String generateSalt() {
        byte[] saltBytes = new byte[8];
        secureRandom.nextBytes(saltBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : saltBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String getErrorMessage(int code) {
        switch (code) {
            case 401: return "Connexion Navidrome refusée ou incomplète.";
            case 403: return "Compte Tropikeau non lié ou accès refusé.";
            case 422: return "Lien invalide ou métadonnées indisponibles.";
            case 429: return "Trop de demandes. Veuillez patienter.";
            case 503: return "Serveur Navidrome inaccessible.";
            default: return "Erreur serveur (" + code + ").";
        }
    }

    public interface TropikeauCallback {
        void onSuccess(MusicRequestResponse response);
        void onError(int code, String message);
    }
}