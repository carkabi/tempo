package com.cappielloantonio.tempo.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.cappielloantonio.tempo.App;
import com.cappielloantonio.tempo.repository.peach.models.PeachRadioStation;
import com.cappielloantonio.tempo.repository.peach.models.RadioManifestResponse;
import com.cappielloantonio.tempo.repository.peach.models.RadioPackageResponse;
import com.cappielloantonio.tempo.repository.tropikeau.TropikeauRepository;
import com.cappielloantonio.tempo.subsonic.base.ApiResponse;
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation;
import com.cappielloantonio.tempo.util.PeachRadioCache;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RadioRepository {

    private static final String TAG = "PEACH_RADIO";

    public MutableLiveData<List<InternetRadioStation>> getInternetRadioStations() {
        MutableLiveData<List<InternetRadioStation>> radioStation = new MutableLiveData<>(new ArrayList<>());

        App.getSubsonicClientInstance(false)
                .getInternetRadioClient()
                .getInternetRadioStations()
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getSubsonicResponse().getInternetRadioStations() != null && response.body().getSubsonicResponse().getInternetRadioStations().getInternetRadioStations() != null) {
                            radioStation.setValue(response.body().getSubsonicResponse().getInternetRadioStations().getInternetRadioStations());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {

                    }
                });

        return radioStation;
    }

    public void syncPeachRadios(MutableLiveData<List<PeachRadioStation>> liveData) {
        if (liveData == null) return;

        PeachRadioCache.migrateCorruptedLegacyCache();

        // Post cached Tropikeau radios immediately
        List<PeachRadioStation> cached = PeachRadioCache.getRadioStations();
        liveData.postValue(cached);

        // Perform Tropikeau manifest sync
        TropikeauRepository tropikeauRepo = new TropikeauRepository();
        tropikeauRepo.getRadioManifest(new TropikeauRepository.RadioManifestCallback() {
            @Override
            public void onSuccess(RadioManifestResponse manifest) {
                if (manifest != null) {
                    Log.d(TAG, "manifest received (HTTP 200), radios count = " + (manifest.getRadios() != null ? manifest.getRadios().size() : 0) + ", package_id = " + manifest.getPackageId());
                    String oldPkgId = PeachRadioCache.getPackageId();
                    PeachRadioCache.saveManifest(manifest);

                    if (manifest.getRadios() != null && !manifest.getRadios().isEmpty()) {
                        liveData.postValue(manifest.getRadios());
                    }

                    String newPkgId = manifest.getPackageId();
                    boolean needsPackage = PeachRadioCache.getCachedPackage() == null || (newPkgId != null && !newPkgId.equals(oldPkgId));

                    if (needsPackage) {
                        Log.d(TAG, "fetching package for packageId = " + newPkgId);
                        tropikeauRepo.getRadioPackage(new TropikeauRepository.RadioPackageCallback() {
                            @Override
                            public void onSuccess(RadioPackageResponse pkg) {
                                if (pkg != null) {
                                    Log.d(TAG, "package received (HTTP 200), package_id = " + pkg.getPackageId() + ", radios count = " + (pkg.getRadios() != null ? pkg.getRadios().size() : 0));
                                    PeachRadioCache.savePackage(pkg);
                                    liveData.postValue(PeachRadioCache.getRadioStations());
                                }
                            }

                            @Override
                            public void onError(int code, String message) {
                                Log.e(TAG, "package fetch error (" + code + "): " + message);
                            }
                        });
                    }
                }
            }

            @Override
            public void onError(int code, String message) {
                Log.e(TAG, "manifest fetch error (" + code + "): " + message);
            }
        });
    }

    public void createInternetRadioStation(String name, String streamURL, String homepageURL) {
        App.getSubsonicClientInstance(false)
                .getInternetRadioClient()
                .createInternetRadioStation(streamURL, name, homepageURL)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {

                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {

                    }
                });
    }

    public void updateInternetRadioStation(String id, String name, String streamURL, String homepageURL) {
        App.getSubsonicClientInstance(false)
                .getInternetRadioClient()
                .updateInternetRadioStation(id, streamURL, name, homepageURL)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {

                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {

                    }
                });
    }

    public void deleteInternetRadioStation(String id) {
        App.getSubsonicClientInstance(false)
                .getInternetRadioClient()
                .deleteInternetRadioStation(id)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {

                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {

                    }
                });
    }
}
