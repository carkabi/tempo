package com.cappielloantonio.tempo.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.cappielloantonio.tempo.repository.AlbumRepository;
import com.cappielloantonio.tempo.repository.ArtistRepository;
import com.cappielloantonio.tempo.repository.SongRepository;
import com.cappielloantonio.tempo.subsonic.models.AlbumID3;
import com.cappielloantonio.tempo.subsonic.models.ArtistID3;
import com.cappielloantonio.tempo.subsonic.models.Child;

import java.util.List;

public class FavoriteViewModel extends AndroidViewModel {

    private final SongRepository songRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;

    public FavoriteViewModel(@NonNull Application application) {
        super(application);

        songRepository = new SongRepository();
        albumRepository = new AlbumRepository();
        artistRepository = new ArtistRepository();
    }

    public LiveData<List<Child>> getFavoriteSongs() {
        return songRepository.getStarredSongs(false, -1);
    }

    public LiveData<List<AlbumID3>> getFavoriteAlbums() {
        return albumRepository.getStarredAlbums(false, -1);
    }

    public LiveData<List<ArtistID3>> getFavoriteArtists() {
        return artistRepository.getStarredArtists(false, -1);
    }
}
