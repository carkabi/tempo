package com.cappielloantonio.tempo.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.repository.tropikeau.TropikeauRepository;
import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequestResponse;

public class AddMusicViewModel extends AndroidViewModel {

    public enum State {
        IDLE, LOADING, SUCCESS, ERROR
    }

    private final MutableLiveData<State> uiState = new MutableLiveData<>(State.IDLE);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("");
    private final MutableLiveData<Integer> queuePosition = new MutableLiveData<>(-1);
    private final TropikeauRepository repository = new TropikeauRepository();

    public AddMusicViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<State> getUiState() {
        return uiState;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<Integer> getQueuePosition() {
        return queuePosition;
    }

    public void addMusic(String spotifyUrl) {
        if (uiState.getValue() == State.LOADING) return;

        uiState.setValue(State.LOADING);
        statusMessage.setValue("");
        queuePosition.setValue(-1);

        repository.addMusic(spotifyUrl, new TropikeauRepository.TropikeauCallback() {
            @Override
            public void onSuccess(MusicRequestResponse response) {
                String msg = response.getMessage();
                if (response.getRequest() != null) {
                    String title = response.getRequest().getTitle() != null ? response.getRequest().getTitle() : "Titre inconnu";
                    String status = response.getRequest().getStatus() != null ? response.getRequest().getStatus() : "En attente";
                    Integer pos = response.getRequest().getQueuePosition();
                    
                    if (pos != null) {
                        msg = String.format(getApplication().getString(R.string.add_music_success_details),
                                title, status, pos);
                        queuePosition.setValue(pos);
                    } else {
                        msg = response.getMessage() + "\n" + title;
                        queuePosition.setValue(-1);
                    }
                }
                statusMessage.setValue(msg);
                uiState.setValue(State.SUCCESS);
            }

            @Override
            public void onError(int code, String message) {
                statusMessage.setValue(message);
                uiState.setValue(State.ERROR);
            }
        });
    }

    public void resetState() {
        uiState.setValue(State.IDLE);
        statusMessage.setValue("");
        queuePosition.setValue(-1);
    }
}