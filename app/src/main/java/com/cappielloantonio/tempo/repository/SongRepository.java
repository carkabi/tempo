package com.cappielloantonio.tempo.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.cappielloantonio.tempo.App;
import com.cappielloantonio.tempo.subsonic.base.ApiResponse;
import com.cappielloantonio.tempo.subsonic.models.Child;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SongRepository {
    private static final String TAG = "SongRepository";

    public MutableLiveData<List<Child>> getStarredSongs(boolean random, int size) {
        MutableLiveData<List<Child>> starredSongs = new MutableLiveData<>(Collections.emptyList());

        App.getSubsonicClientInstance(false)
                .getAlbumSongListClient()
                .getStarred2()
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getSubsonicResponse().getStarred2() != null) {
                            List<Child> songs = response.body().getSubsonicResponse().getStarred2().getSongs();

                            if (songs != null) {
                                if (!random) {
                                    starredSongs.setValue(songs);
                                } else {
                                    Collections.shuffle(songs);
                                    starredSongs.setValue(songs.subList(0, Math.min(size, songs.size())));
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {

                    }
                });

        return starredSongs;
    }

    public MutableLiveData<List<Child>> getInstantMix(String id, int count) {
        MutableLiveData<List<Child>> instantMix = new MutableLiveData<>();

        App.getSubsonicClientInstance(false)
                .getBrowsingClient()
                .getSimilarSongs2(id, count)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getSubsonicResponse().getSimilarSongs2() != null) {
                            instantMix.setValue(response.body().getSubsonicResponse().getSimilarSongs2().getSongs());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                        instantMix.setValue(null);
                    }
                });

        return instantMix;
    }

    public MutableLiveData<List<Child>> getRandomSample(int number, Integer fromYear, Integer toYear) {
        MutableLiveData<List<Child>> randomSongsSample = new MutableLiveData<>();

        App.getSubsonicClientInstance(false)
                .getAlbumSongListClient()
                .getRandomSongs(number, fromYear, toYear)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                        List<Child> songs = new ArrayList<>();

                        if (response.isSuccessful() && response.body() != null && response.body().getSubsonicResponse().getRandomSongs() != null && response.body().getSubsonicResponse().getRandomSongs().getSongs() != null) {
                            songs.addAll(response.body().getSubsonicResponse().getRandomSongs().getSongs());
                        }

                        randomSongsSample.setValue(songs);
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {

                    }
                });

        return randomSongsSample;
    }

    public void scrobble(String id, boolean submission) {
        App.getSubsonicClientInstance(false)
                .getMediaAnnotationClient()
                .scrobble(id, submission)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {

                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {

                    }
                });
    }

    public void setRating(String id, int rating) {
        App.getSubsonicClientInstance(false)
                .getMediaAnnotationClient()
                .setRating(id, rating)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {

                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {

                    }
                });
    }

    public MutableLiveData<List<Child>> getSongsByGenre(String id, int page) {
        MutableLiveData<List<Child>> songsByGenre = new MutableLiveData<>();

        App.getSubsonicClientInstance(false)
                .getAlbumSongListClient()
                .getSongsByGenre(id, 100, 100 * page)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getSubsonicResponse().getSongsByGenre() != null) {
                            songsByGenre.setValue(response.body().getSubsonicResponse().getSongsByGenre().getSongs());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {

                    }
                });

        return songsByGenre;
    }

    public MutableLiveData<List<Child>> getSongsByGenres(
            ArrayList<String> genresId
    ) {
        return getSongsByGenres(genresId, 0, 100);
    }

    public MutableLiveData<List<Child>> getSongsByGenres(
            ArrayList<String> genresId,
            int page,
            int pageSize
    ) {
        MutableLiveData<List<Child>> songsByGenre =
                new MutableLiveData<>();

        if (genresId == null || genresId.isEmpty()) {
            songsByGenre.setValue(Collections.emptyList());
            return songsByGenre;
        }

        Map<String, Child> merged =
                Collections.synchronizedMap(
                        new LinkedHashMap<>()
                );
        AtomicInteger pending =
                new AtomicInteger(genresId.size());

        for (String id : genresId) {
            App.getSubsonicClientInstance(false)
                    .getAlbumSongListClient()
                    .getSongsByGenre(
                            id,
                            pageSize,
                            pageSize * page
                    )
                    .enqueue(new Callback<ApiResponse>() {
                        @Override
                        public void onResponse(
                                @NonNull Call<ApiResponse> call,
                                @NonNull Response<ApiResponse> response
                        ) {
                            if (response.isSuccessful()
                                    && response.body() != null
                                    && response.body()
                                    .getSubsonicResponse()
                                    .getSongsByGenre() != null
                                    && response.body()
                                    .getSubsonicResponse()
                                    .getSongsByGenre()
                                    .getSongs() != null) {

                                synchronized (merged) {
                                    for (Child child : response.body()
                                            .getSubsonicResponse()
                                            .getSongsByGenre()
                                            .getSongs()) {
                                        merged.put(child.getId(), child);
                                    }
                                }
                            }

                            publishWhenComplete();
                        }

                        @Override
                        public void onFailure(
                                @NonNull Call<ApiResponse> call,
                                @NonNull Throwable t
                        ) {
                            publishWhenComplete();
                        }

                        private void publishWhenComplete() {
                            if (pending.decrementAndGet() != 0) {
                                return;
                            }

                            List<Child> result;

                            synchronized (merged) {
                                result = new ArrayList<>(
                                        merged.values()
                                );
                            }

                            result.sort(
                                    Comparator.comparing(
                                            child -> child.getTitle() == null
                                                    ? ""
                                                    : child.getTitle(),
                                            String.CASE_INSENSITIVE_ORDER
                                    )
                            );

                            songsByGenre.postValue(result);
                        }
                    });
        }

        return songsByGenre;
    }

    public MutableLiveData<Child> getSong(String id) {
        MutableLiveData<Child> song = new MutableLiveData<>();

        App.getSubsonicClientInstance(false)
                .getBrowsingClient()
                .getSong(id)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            song.setValue(response.body().getSubsonicResponse().getSong());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {

                    }
                });

        return song;
    }

    public MutableLiveData<String> getSongLyrics(Child song) {
        MutableLiveData<String> lyrics = new MutableLiveData<>(null);

        App.getSubsonicClientInstance(false)
                .getMediaRetrievalClient()
                .getLyrics(song.getArtist(), song.getTitle())
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getSubsonicResponse().getLyrics() != null) {
                            lyrics.setValue(response.body().getSubsonicResponse().getLyrics().getValue());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {

                    }
                });

        return lyrics;
    }
}
