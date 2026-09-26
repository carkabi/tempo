package com.cappielloantonio.tempo.repository.tropikeau;

import androidx.annotation.NonNull;

import com.cappielloantonio.tempo.repository.peach.models.RadioManifestResponse;
import com.cappielloantonio.tempo.repository.peach.models.RadioPackageResponse;
import com.cappielloantonio.tempo.repository.tropikeau.models.ArtistFollowResponse;
import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequest;
import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequestResponse;
import com.cappielloantonio.tempo.repository.tropikeau.models.SharedPlaylistAddRequest;
import com.cappielloantonio.tempo.repository.tropikeau.models.SharedPlaylistResponse;
import com.cappielloantonio.tempo.repository.tropikeau.models.SharedPlaylistVoteRequest;
import com.cappielloantonio.tempo.subsonic.utils.StringUtil;
import com.cappielloantonio.tempo.util.Preferences;

import java.io.IOException;
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

    public void getStatus(int requestId, TropikeauCallback callback) {
        String username = Preferences.getUser();
        String password = Preferences.getPassword();

        if (username == null || password == null) {
            callback.onError(401, "Authentification Navidrome manquante.");
            return;
        }

        String salt = generateSalt();
        String token = StringUtil.tokenize(password + salt);

        apiService.getRequestStatus(username, token, salt, requestId).enqueue(new Callback<MusicRequestResponse>() {
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

    public void getHistory(int limit, TropikeauCallback callback) {
        String username = Preferences.getUser();
        String password = Preferences.getPassword();

        if (username == null || password == null) {
            callback.onError(401, "Authentification Navidrome manquante.");
            return;
        }

        String salt = generateSalt();
        String token = StringUtil.tokenize(password + salt);

        apiService.getHistory(username, token, salt, limit).enqueue(new Callback<MusicRequestResponse>() {
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

    public void getArtistFollow(
            String artistId,
            ArtistFollowCallback callback
    ) {
        artistFollowCall(
                artistId,
                callback,
                0
        );
    }

    public void followArtist(
            String artistId,
            ArtistFollowCallback callback
    ) {
        artistFollowCall(
                artistId,
                callback,
                1
        );
    }

    public void unfollowArtist(
            String artistId,
            ArtistFollowCallback callback
    ) {
        artistFollowCall(
                artistId,
                callback,
                -1
        );
    }

    private void artistFollowCall(
            String artistId,
            ArtistFollowCallback callback,
            int action
    ) {
        String username = Preferences.getUser();
        String password = Preferences.getPassword();

        if (username == null || password == null) {
            callback.onError(
                    401,
                    "Authentification Navidrome manquante."
            );
            return;
        }

        String salt = generateSalt();
        String token = StringUtil.tokenize(password + salt);

        Call<ArtistFollowResponse> call;

        if (action > 0) {
            call = apiService.followArtist(
                    username,
                    token,
                    salt,
                    artistId
            );
        } else if (action < 0) {
            call = apiService.unfollowArtist(
                    username,
                    token,
                    salt,
                    artistId
            );
        } else {
            call = apiService.getArtistFollow(
                    username,
                    token,
                    salt,
                    artistId
            );
        }

        call.enqueue(new Callback<ArtistFollowResponse>() {
            @Override
            public void onResponse(
                    @NonNull Call<ArtistFollowResponse> call,
                    @NonNull Response<ArtistFollowResponse> response
            ) {
                if (response.isSuccessful()
                        && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(
                            response.code(),
                            getErrorMessage(response.code())
                    );
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<ArtistFollowResponse> call,
                    @NonNull Throwable t
            ) {
                callback.onError(
                        -1,
                        "Erreur réseau : " + t.getMessage()
                );
            }
        });
    }

    public void getSharedPlaylist(SharedPlaylistCallback callback) {
        String username = Preferences.getUser();
        String password = Preferences.getPassword();

        if (username == null || password == null) {
            callback.onError(401, "Authentification Navidrome manquante.");
            return;
        }

        String salt = generateSalt();
        String token = StringUtil.tokenize(password + salt);

        apiService.getSharedPlaylist(username, token, salt)
                .enqueue(sharedPlaylistCallback(callback));
    }

    public void addSharedTrack(String trackId, SharedPlaylistCallback callback) {
        String username = Preferences.getUser();
        String password = Preferences.getPassword();

        if (username == null || password == null) {
            callback.onError(401, "Authentification Navidrome manquante.");
            return;
        }

        String salt = generateSalt();
        String token = StringUtil.tokenize(password + salt);

        apiService.addSharedPlaylistTrack(
                username,
                token,
                salt,
                new SharedPlaylistAddRequest(trackId)
        ).enqueue(sharedPlaylistCallback(callback));
    }

    public void voteSharedTrack(
            long itemId,
            int vote,
            SharedPlaylistCallback callback
    ) {
        String username = Preferences.getUser();
        String password = Preferences.getPassword();

        if (username == null || password == null) {
            callback.onError(401, "Authentification Navidrome manquante.");
            return;
        }

        String salt = generateSalt();
        String token = StringUtil.tokenize(password + salt);

        apiService.voteSharedPlaylistTrack(
                username,
                token,
                salt,
                itemId,
                new SharedPlaylistVoteRequest(vote)
        ).enqueue(sharedPlaylistCallback(callback));
    }

    public void removeSharedTrack(long itemId, SharedPlaylistCallback callback) {
        String username = Preferences.getUser();
        String password = Preferences.getPassword();

        if (username == null || password == null) {
            callback.onError(401, "Authentification Navidrome manquante.");
            return;
        }

        String salt = generateSalt();
        String token = StringUtil.tokenize(password + salt);

        apiService.removeSharedPlaylistTrack(username, token, salt, itemId)
                .enqueue(sharedPlaylistCallback(callback));
    }

    private Callback<SharedPlaylistResponse> sharedPlaylistCallback(
            SharedPlaylistCallback callback
    ) {
        return new Callback<SharedPlaylistResponse>() {
            @Override
            public void onResponse(
                    @NonNull Call<SharedPlaylistResponse> call,
                    @NonNull Response<SharedPlaylistResponse> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(
                            response.code(),
                            getErrorMessage(response.code())
                    );
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<SharedPlaylistResponse> call,
                    @NonNull Throwable t
            ) {
                callback.onError(
                        -1,
                        "Erreur réseau : " + t.getMessage()
                );
            }
        };
    }

    public void getRadioManifest(RadioManifestCallback callback) {
        String username = Preferences.getUser();
        String password = Preferences.getPassword();

        if (username == null || password == null) {
            callback.onError(401, "Authentification Navidrome manquante.");
            return;
        }

        String salt = generateSalt();
        String token = StringUtil.tokenize(password + salt);

        apiService.getRadioManifest(username, token, salt).enqueue(new Callback<RadioManifestResponse>() {
            @Override
            public void onResponse(@NonNull Call<RadioManifestResponse> call, @NonNull Response<RadioManifestResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(response.code(), getErrorMessage(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<RadioManifestResponse> call, @NonNull Throwable t) {
                callback.onError(-1, "Erreur réseau : " + t.getMessage());
            }
        });
    }

    public void getRadioPackage(RadioPackageCallback callback) {
        String username = Preferences.getUser();
        String password = Preferences.getPassword();

        if (username == null || password == null) {
            callback.onError(401, "Authentification Navidrome manquante.");
            return;
        }

        String salt = generateSalt();
        String token = StringUtil.tokenize(password + salt);

        apiService.getRadioPackage(username, token, salt).enqueue(new Callback<RadioPackageResponse>() {
            @Override
            public void onResponse(@NonNull Call<RadioPackageResponse> call, @NonNull Response<RadioPackageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(response.code(), getErrorMessage(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<RadioPackageResponse> call, @NonNull Throwable t) {
                callback.onError(-1, "Erreur réseau : " + t.getMessage());
            }
        });
    }

    public RadioManifestResponse getRadioManifestSync() throws IOException {
        String username = Preferences.getUser();
        String password = Preferences.getPassword();

        if (username == null || password == null) return null;

        String salt = generateSalt();
        String token = StringUtil.tokenize(password + salt);

        Response<RadioManifestResponse> response = apiService.getRadioManifest(username, token, salt).execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        }
        return null;
    }

    public RadioPackageResponse getRadioPackageSync() throws IOException {
        String username = Preferences.getUser();
        String password = Preferences.getPassword();

        if (username == null || password == null) return null;

        String salt = generateSalt();
        String token = StringUtil.tokenize(password + salt);

        Response<RadioPackageResponse> response = apiService.getRadioPackage(username, token, salt).execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        }
        return null;
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
            case 404: return "Ressource introuvable sur le serveur.";
            case 422: return "Données invalides.";
            case 429: return "Trop de demandes. Veuillez patienter.";
            case 503: return "Serveur Navidrome inaccessible.";
            default: return "Erreur serveur (" + code + ").";
        }
    }

    public interface TropikeauCallback {
        void onSuccess(MusicRequestResponse response);
        void onError(int code, String message);
    }

    public interface ArtistFollowCallback {
        void onSuccess(ArtistFollowResponse response);
        void onError(int code, String message);
    }

    public interface SharedPlaylistCallback {
        void onSuccess(SharedPlaylistResponse response);
        void onError(int code, String message);
    }

    public interface RadioManifestCallback {
        void onSuccess(RadioManifestResponse response);
        void onError(int code, String message);
    }

    public interface RadioPackageCallback {
        void onSuccess(RadioPackageResponse response);
        void onError(int code, String message);
    }
}
