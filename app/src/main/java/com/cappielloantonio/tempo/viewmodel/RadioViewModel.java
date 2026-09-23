package com.cappielloantonio.tempo.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cappielloantonio.tempo.repository.RadioRepository;
import com.cappielloantonio.tempo.repository.peach.models.PeachRadioStation;
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation;
import com.cappielloantonio.tempo.util.PeachRadioCache;

import java.util.List;

public class RadioViewModel extends AndroidViewModel {
    private final RadioRepository radioRepository;

    private final MutableLiveData<List<InternetRadioStation>> internetRadioStations = new MutableLiveData<>(null);
    private final MutableLiveData<List<PeachRadioStation>> peachRadioStations = new MutableLiveData<>(PeachRadioCache.getRadioStations());

    public RadioViewModel(@NonNull Application application) {
        super(application);
        radioRepository = new RadioRepository();
    }

    public LiveData<List<InternetRadioStation>> getInternetRadioStations(LifecycleOwner owner) {
        radioRepository.getInternetRadioStations().observe(owner, internetRadioStations::postValue);
        return internetRadioStations;
    }

    public void refreshInternetRadioStations(LifecycleOwner owner) {
        radioRepository.getInternetRadioStations().observe(owner, internetRadioStations::postValue);
    }

    public LiveData<List<PeachRadioStation>> getPeachRadioStations() {
        radioRepository.syncPeachRadios(peachRadioStations);
        return peachRadioStations;
    }

    public void refreshPeachRadios() {
        radioRepository.syncPeachRadios(peachRadioStations);
    }
}
