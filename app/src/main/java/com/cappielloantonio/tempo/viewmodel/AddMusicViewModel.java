package com.cappielloantonio.tempo.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cappielloantonio.tempo.repository.tropikeau.TropikeauRepository;
import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequestData;
import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequestResponse;

import java.util.ArrayList;
import java.util.List;

public class AddMusicViewModel extends AndroidViewModel {

    public enum State {
        IDLE, LOADING, SUCCESS, ERROR
    }

    private final MutableLiveData<State> uiState = new MutableLiveData<>(State.IDLE);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("");
    private final MutableLiveData<List<MusicRequestData>> requestsList = new MutableLiveData<>(new ArrayList<>());
    
    private final TropikeauRepository repository = new TropikeauRepository();
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private boolean isRefreshing = false;

    public AddMusicViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<State> getUiState() { return uiState; }
    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<List<MusicRequestData>> getRequestsList() { return requestsList; }

    public void addMusic(String spotifyUrl) {
        if (uiState.getValue() == State.LOADING) return;

        uiState.setValue(State.LOADING);
        statusMessage.setValue("");

        repository.addMusic(spotifyUrl, new TropikeauRepository.TropikeauCallback() {
            @Override
            public void onSuccess(MusicRequestResponse response) {
                uiState.postValue(State.SUCCESS);
                statusMessage.postValue(response.getMessage());
                loadHistory(); // Refresh immediately after success
            }

            @Override
            public void onError(int code, String message) {
                uiState.postValue(State.ERROR);
                statusMessage.postValue(message);
            }
        });
    }

    public void loadHistory() {
        repository.getHistory(10, new TropikeauRepository.TropikeauCallback() {
            @Override
            public void onSuccess(MusicRequestResponse response) {
                if (response.getRequests() != null) {
                    requestsList.postValue(response.getRequests());
                }
                
                if (response.getMeta() != null && response.getMeta().isHasActive()) {
                    scheduleRefresh(response.getMeta().getRefreshAfterSeconds() > 0 ? response.getMeta().getRefreshAfterSeconds() * 1000 : 10000);
                } else {
                    stopAutoRefresh();
                }
            }

            @Override
            public void onError(int code, String message) {
                // If silent refresh failed, don't clear list, just stop polling for now
                stopAutoRefresh();
            }
        });
    }

    public void startAutoRefresh() {
        if (isRefreshing) return;
        isRefreshing = true;
        loadHistory();
    }

    public void stopAutoRefresh() {
        isRefreshing = false;
        if (refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
            refreshRunnable = null;
        }
    }

    private void scheduleRefresh(int delayMs) {
        if (!isRefreshing) return;
        if (refreshRunnable != null) refreshHandler.removeCallbacks(refreshRunnable);
        refreshRunnable = this::loadHistory;
        refreshHandler.postDelayed(refreshRunnable, delayMs);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopAutoRefresh();
    }
}