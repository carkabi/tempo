package com.cappielloantonio.tempo.ui.fragment;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaBrowser;
import androidx.media3.session.SessionToken;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.viewpager2.widget.ViewPager2;

import com.cappielloantonio.tempo.BuildConfig;
import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.FragmentHomeTabMusicBinding;
import com.cappielloantonio.tempo.helper.recyclerview.CustomLinearSnapHelper;
import com.cappielloantonio.tempo.helper.recyclerview.DotsIndicatorDecoration;
import com.cappielloantonio.tempo.interfaces.ClickCallback;
import com.cappielloantonio.tempo.interfaces.PlaylistCallback;
import com.cappielloantonio.tempo.model.Chronology;
import com.cappielloantonio.tempo.model.Download;
import com.cappielloantonio.tempo.model.HomeSector;
import com.cappielloantonio.tempo.service.DownloaderManager;
import com.cappielloantonio.tempo.service.MediaManager;
import com.cappielloantonio.tempo.service.MediaService;
import com.cappielloantonio.tempo.subsonic.models.Child;
import com.cappielloantonio.tempo.subsonic.models.Share;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.ui.adapter.AlbumAdapter;
import com.cappielloantonio.tempo.ui.adapter.AlbumHorizontalAdapter;
import com.cappielloantonio.tempo.ui.adapter.ArtistAdapter;
import com.cappielloantonio.tempo.ui.adapter.ArtistHorizontalAdapter;
import com.cappielloantonio.tempo.ui.adapter.DiscoverSongAdapter;
import com.cappielloantonio.tempo.ui.adapter.PlaylistHorizontalAdapter;
import com.cappielloantonio.tempo.ui.adapter.ShareHorizontalAdapter;
import com.cappielloantonio.tempo.ui.adapter.SimilarTrackAdapter;
import com.cappielloantonio.tempo.ui.adapter.SongHorizontalAdapter;
import com.cappielloantonio.tempo.ui.adapter.YearAdapter;
import com.cappielloantonio.tempo.ui.dialog.HomeRearrangementDialog;
import com.cappielloantonio.tempo.ui.dialog.PlaylistEditorDialog;
import com.cappielloantonio.tempo.util.Constants;
import com.cappielloantonio.tempo.util.DownloadUtil;
import com.cappielloantonio.tempo.util.MappingUtil;
import com.cappielloantonio.tempo.util.MusicUtil;
import com.cappielloantonio.tempo.util.Preferences;
import com.cappielloantonio.tempo.util.UIUtil;
import com.cappielloantonio.tempo.viewmodel.HomeViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@UnstableApi
public class HomeTabMusicFragment extends Fragment implements ClickCallback {
    private FragmentHomeTabMusicBinding bind;
    private MainActivity activity;
    private HomeViewModel homeViewModel;
    private ListenableFuture<MediaBrowser> mediaBrowserListenableFuture;

    private DiscoverSongAdapter discoverSongAdapter;
    private SimilarTrackAdapter similarMusicAdapter;
    private ArtistAdapter radioArtistAdapter;
    private SongHorizontalAdapter starredSongAdapter;
    private SongHorizontalAdapter topSongAdapter;
    private SongHorizontalAdapter mostPlayedSongAdapter;
    private AlbumHorizontalAdapter starredAlbumAdapter;
    private ArtistHorizontalAdapter starredArtistAdapter;
    private AlbumAdapter recentlyAddedAlbumAdapter;
    private AlbumAdapter recentlyPlayedAlbumAdapter;
    private AlbumHorizontalAdapter newReleasesAlbumAdapter;
    private YearAdapter yearAdapter;
    private PlaylistHorizontalAdapter playlistHorizontalAdapter;
    private ShareHorizontalAdapter shareHorizontalAdapter;

    private static final String TAG = "PEACH_NAV";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "HomeTabMusicFragment.onCreateView()");
        activity = (MainActivity) getActivity();
        bind = FragmentHomeTabMusicBinding.inflate(inflater, container, false);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        init();
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "HomeTabMusicFragment.onViewCreated()");
        initSyncStarredView();
        initDiscoverSongSlideView();
        initSimilarSongView();
        initArtistRadio();
        initStarredTracksView();
        initStarredAlbumsView();
        initStarredArtistsView();
        initMostPlayedSongView();
        initRecentPlayedAlbumView();
        initNewReleasesView();
        initYearSongView();
        initRecentAddedAlbumView();
        initTopSongsView();
        initPinnedPlaylistsView();
        initSharesView();
        initHomeReorganizer();
        reorder();
    }

    @Override
    public void onStart() {
        super.onStart();
        initializeMediaBrowser();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSharesView();
    }

    @Override
    public void onStop() {
        releaseMediaBrowser();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    private void init() {
        if (bind == null) return;
        bind.gridTracksPreTextView.setOnClickListener(view -> showPopupMenu(view, R.menu.filter_top_songs_popup_menu));

        if ("peach".equals(BuildConfig.FLAVOR)) {
            bind.homeQuickAccessScroll.setVisibility(View.VISIBLE);
            bind.homeQuickSearch.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.searchFragment)
            );
            bind.homeQuickShared.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.sharedPlaylistFragment)
            );
            bind.homeQuickGenres.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.genreCatalogueFragment)
            );
            bind.homeQuickDownloads.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.downloadFragment)
            );
            bind.homeQuickSupport.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.peachSupportFragment)
            );
        } else {
            bind.homeQuickAccessScroll.setVisibility(View.GONE);
        }

        bind.homeRadioButton.setOnClickListener(v -> {
            Fragment parent = getParentFragment();
            if (parent instanceof HomeFragment) {
                ((HomeFragment) parent).showRadioTab();
            }
        });

        bind.discoveryPlayButton.setOnClickListener(v -> playSector(homeViewModel.getDiscoverSongSample(getViewLifecycleOwner())));
        bind.similarTracksPlayButton.setOnClickListener(v -> playSector(homeViewModel.getStarredTracksSample(getViewLifecycleOwner())));
        bind.topSongsPlayButton.setOnClickListener(v -> playChronologySector(homeViewModel.getChronologySample(getViewLifecycleOwner())));
        bind.starredTracksPlayButton.setOnClickListener(v -> playSector(homeViewModel.getStarredTracksSongs()));
        bind.starredAlbumsPlayButton.setOnClickListener(v -> playSector(homeViewModel.getStarredAlbumsSongs()));
        bind.starredArtistsPlayButton.setOnClickListener(v -> playSector(homeViewModel.getStarredArtistsSongs()));
        bind.mostPlayedAlbumsPlayButton.setOnClickListener(v -> playSector(homeViewModel.getMostPlayedAlbumsSongs()));
        bind.recentlyPlayedAlbumsPlayButton.setOnClickListener(v -> playSector(homeViewModel.getRecentlyPlayedAlbumsSongs()));
        bind.recentlyAddedAlbumsPlayButton.setOnClickListener(v -> playSector(homeViewModel.getNewestAlbumsSongs()));

        // Listeners "Voir tout"
        bind.starredTracksTextViewClickable.setOnClickListener(v -> navigateToSongList(v, Constants.MEDIA_STARRED));
        bind.starredAlbumsTextViewClickable.setOnClickListener(v -> navigateToAlbumList(v, Constants.ALBUM_STARRED));
        bind.starredArtistsTextViewClickable.setOnClickListener(v -> navigateToArtistList(v, Constants.ARTIST_STARRED));
        bind.mostPlayedAlbumsTextViewClickable.setOnClickListener(v -> navigateToSongList(v, Constants.MEDIA_MOST_PLAYED));
        bind.recentlyPlayedAlbumsTextViewClickable.setOnClickListener(v -> navigateToAlbumList(v, Constants.ALBUM_RECENTLY_PLAYED));
        bind.recentlyAddedAlbumsTextViewClickable.setOnClickListener(v -> navigateToAlbumList(v, Constants.ALBUM_RECENTLY_ADDED));
    }

    private void navigateToSongList(View v, String type) {
        Bundle bundle = new Bundle();
        bundle.putString(type, type);
        Navigation.findNavController(v).navigate(R.id.songListPageFragment, bundle);
    }

    private void navigateToAlbumList(View v, String type) {
        Bundle bundle = new Bundle();
        bundle.putString(type, type);
        Navigation.findNavController(v).navigate(R.id.albumListPageFragment, bundle);
    }

    private void navigateToArtistList(View v, String type) {
        Bundle bundle = new Bundle();
        bundle.putString(type, type);
        Navigation.findNavController(v).navigate(R.id.artistListPageFragment, bundle);
    }

    private void playSector(LiveData<List<Child>> liveData) {
        liveData.observe(getViewLifecycleOwner(), songs -> {
            if (songs != null && !songs.isEmpty()) {
                MusicUtil.ratingFilter(songs);
                if (!songs.isEmpty()) {
                    List<Child> shuffled = new ArrayList<>(songs);
                    java.util.Collections.shuffle(shuffled);
                    MediaManager.startQueue(mediaBrowserListenableFuture, shuffled, 0);
                    activity.setBottomSheetInPeek(true);
                }
            }
        });
    }

    private void playChronologySector(LiveData<List<Chronology>> liveData) {
        liveData.observe(getViewLifecycleOwner(), chronologies -> {
            if (chronologies != null && !chronologies.isEmpty()) {
                List<Child> songs = chronologies.stream()
                        .map(c -> (Child) c)
                        .collect(Collectors.toList());
                List<Child> shuffled = new ArrayList<>(songs);
                java.util.Collections.shuffle(shuffled);
                MediaManager.startQueue(mediaBrowserListenableFuture, shuffled, 0);
                activity.setBottomSheetInPeek(true);
            }
        });
    }

    private void initSyncStarredView() {
        if (Preferences.isStarredSyncEnabled()) {
            homeViewModel.getAllStarredTracks().observeForever(new Observer<List<Child>>() {
                @Override
                public void onChanged(List<Child> songs) {
                    if (songs != null) {
                        DownloaderManager manager = DownloadUtil.getDownloadTracker(requireContext());
                        List<String> toSync = new ArrayList<>();
                        for (Child song : songs) {
                            if (!manager.isDownloaded(song.getId())) toSync.add(song.getTitle());
                        }
                        if (!toSync.isEmpty()) {
                            bind.homeSyncStarredCard.setVisibility(View.VISIBLE);
                            bind.homeSyncStarredTracksToSync.setText(String.join(", ", toSync));
                        }
                    }
                    homeViewModel.getAllStarredTracks().removeObserver(this);
                }
            });
        }
        bind.homeSyncStarredCancel.setOnClickListener(v -> bind.homeSyncStarredCard.setVisibility(View.GONE));
        bind.homeSyncStarredDownload.setOnClickListener(v -> {
            homeViewModel.getAllStarredTracks().observeForever(new Observer<List<Child>>() {
                @Override
                public void onChanged(List<Child> songs) {
                    if (songs != null) {
                        DownloaderManager manager = DownloadUtil.getDownloadTracker(requireContext());
                        for (Child song : songs) {
                            if (!manager.isDownloaded(song.getId())) manager.download(MappingUtil.mapDownload(song), new Download(song));
                        }
                    }
                    homeViewModel.getAllStarredTracks().removeObserver(this);
                    bind.homeSyncStarredCard.setVisibility(View.GONE);
                }
            });
        });
    }

    private void initDiscoverSongSlideView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_DISCOVERY)) return;
        bind.discoverSongViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        discoverSongAdapter = new DiscoverSongAdapter(this);
        bind.discoverSongViewPager.setAdapter(discoverSongAdapter);
        bind.discoverSongViewPager.setOffscreenPageLimit(1);
        homeViewModel.getDiscoverSongSample(getViewLifecycleOwner()).observe(getViewLifecycleOwner(), songs -> {
            MusicUtil.ratingFilter(songs);
            if (songs == null) { if (bind != null) bind.homeDiscoverSector.setVisibility(View.GONE); }
            else { if (bind != null) bind.homeDiscoverSector.setVisibility(!songs.isEmpty() ? View.VISIBLE : View.GONE); discoverSongAdapter.setItems(songs); }
        });
        setSlideViewOffset(bind.discoverSongViewPager, 20, 16);
    }

    private void initSimilarSongView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_MADE_FOR_YOU)) return;
        bind.similarTracksRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        bind.similarTracksRecyclerView.setHasFixedSize(true);
        similarMusicAdapter = new SimilarTrackAdapter(this);
        bind.similarTracksRecyclerView.setAdapter(similarMusicAdapter);
        homeViewModel.getStarredTracksSample(getViewLifecycleOwner()).observe(getViewLifecycleOwner(), songs -> {
            MusicUtil.ratingFilter(songs);
            if (songs == null) { if (bind != null) bind.homeSimilarTracksSector.setVisibility(View.GONE); }
            else { if (bind != null) bind.homeSimilarTracksSector.setVisibility(!songs.isEmpty() ? View.VISIBLE : View.GONE); similarMusicAdapter.setItems(songs); }
        });
        new CustomLinearSnapHelper().attachToRecyclerView(bind.similarTracksRecyclerView);
    }

    private void initArtistRadio() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_RADIO_STATION)) return;
        bind.radioArtistRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        bind.radioArtistRecyclerView.setHasFixedSize(true);
        radioArtistAdapter = new ArtistAdapter(this, true, false);
        bind.radioArtistRecyclerView.setAdapter(radioArtistAdapter);
        homeViewModel.getStarredArtistsSample(getViewLifecycleOwner()).observe(getViewLifecycleOwner(), artists -> {
            if (artists == null) { if (bind != null) bind.homeRadioArtistSector.setVisibility(View.GONE); }
            else { if (bind != null) { bind.homeRadioArtistSector.setVisibility(!artists.isEmpty() ? View.VISIBLE : View.GONE); bind.afterRadioArtistDivider.setVisibility(!artists.isEmpty() ? View.VISIBLE : View.GONE); } radioArtistAdapter.setItems(artists); }
        });
        new CustomLinearSnapHelper().attachToRecyclerView(bind.radioArtistRecyclerView);
    }

    private void initTopSongsView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_TOP_SONGS)) return;
        bind.topSongsRecyclerView.setHasFixedSize(true);
        topSongAdapter = new SongHorizontalAdapter(this, true, false, null);
        bind.topSongsRecyclerView.setAdapter(topSongAdapter);
        homeViewModel.getChronologySample(getViewLifecycleOwner()).observe(getViewLifecycleOwner(), chronologies -> {
            if (chronologies == null || chronologies.isEmpty()) { if (bind != null) { bind.homeGridTracksSector.setVisibility(View.GONE); bind.afterGridDivider.setVisibility(View.GONE); } }
            else { if (bind != null) { bind.homeGridTracksSector.setVisibility(View.VISIBLE); bind.afterGridDivider.setVisibility(View.VISIBLE); bind.topSongsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), UIUtil.getSpanCount(chronologies.size(), 4), GridLayoutManager.HORIZONTAL, false)); }
                List<Child> topSongs = chronologies.stream().map(c -> (Child) c).collect(Collectors.toList());
                topSongAdapter.setItems(topSongs);
            }
        });
    }

    private void initStarredTracksView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_STARRED_TRACKS)) return;
        bind.starredTracksRecyclerView.setHasFixedSize(true);
        starredSongAdapter = new SongHorizontalAdapter(this, true, false, null);
        bind.starredTracksRecyclerView.setAdapter(starredSongAdapter);
        homeViewModel.getStarredTracks(getViewLifecycleOwner()).observe(getViewLifecycleOwner(), songs -> {
            if (songs == null) { if (bind != null) bind.starredTracksSector.setVisibility(View.GONE); }
            else { if (bind != null) { bind.starredTracksSector.setVisibility(!songs.isEmpty() ? View.VISIBLE : View.GONE); bind.starredTracksRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), UIUtil.getSpanCount(songs.size(), 4), GridLayoutManager.HORIZONTAL, false)); } starredSongAdapter.setItems(songs); }
        });
    }

    private void initStarredAlbumsView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_STARRED_ALBUMS)) return;
        bind.starredAlbumsRecyclerView.setHasFixedSize(true);
        starredAlbumAdapter = new AlbumHorizontalAdapter(this, false);
        bind.starredAlbumsRecyclerView.setAdapter(starredAlbumAdapter);
        homeViewModel.getStarredAlbums(getViewLifecycleOwner()).observe(getViewLifecycleOwner(), albums -> {
            if (albums == null) { if (bind != null) bind.starredAlbumsSector.setVisibility(View.GONE); }
            else { if (bind != null) { bind.starredAlbumsSector.setVisibility(!albums.isEmpty() ? View.VISIBLE : View.GONE); bind.starredAlbumsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), UIUtil.getSpanCount(albums.size(), 4), GridLayoutManager.HORIZONTAL, false)); } starredAlbumAdapter.setItems(albums); }
        });
    }

    private void initStarredArtistsView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_STARRED_ARTISTS)) return;
        bind.starredArtistsRecyclerView.setHasFixedSize(true);
        starredArtistAdapter = new ArtistHorizontalAdapter(this);
        bind.starredArtistsRecyclerView.setAdapter(starredArtistAdapter);
        homeViewModel.getStarredArtists(getViewLifecycleOwner()).observe(getViewLifecycleOwner(), artists -> {
            if (artists == null) { if (bind != null) bind.starredArtistsSector.setVisibility(View.GONE); }
            else { if (bind != null) { bind.starredArtistsSector.setVisibility(!artists.isEmpty() ? View.VISIBLE : View.GONE); bind.afterFavoritesDivider.setVisibility(!artists.isEmpty() ? View.VISIBLE : View.GONE); bind.starredArtistsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), UIUtil.getSpanCount(artists.size(), 4), GridLayoutManager.HORIZONTAL, false)); } starredArtistAdapter.setItems(artists); }
        });
    }

    private void initNewReleasesView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_NEW_RELEASES)) return;
        bind.newReleasesRecyclerView.setHasFixedSize(true);
        newReleasesAlbumAdapter = new AlbumHorizontalAdapter(this, false);
        bind.newReleasesRecyclerView.setAdapter(newReleasesAlbumAdapter);
        homeViewModel.getRecentlyReleasedAlbums(getViewLifecycleOwner()).observe(getViewLifecycleOwner(), albums -> {
            if (albums == null) { if (bind != null) bind.homeNewReleasesSector.setVisibility(View.GONE); }
            else { if (bind != null) { bind.homeNewReleasesSector.setVisibility(!albums.isEmpty() ? View.VISIBLE : View.GONE); bind.newReleasesRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), UIUtil.getSpanCount(albums.size(), 4), GridLayoutManager.HORIZONTAL, false)); } newReleasesAlbumAdapter.setItems(albums); }
        });
    }

    private void initYearSongView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_FLASHBACK)) return;
        bind.yearsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        bind.yearsRecyclerView.setHasFixedSize(true);
        yearAdapter = new YearAdapter(this);
        bind.yearsRecyclerView.setAdapter(yearAdapter);
        homeViewModel.getYearList(getViewLifecycleOwner()).observe(getViewLifecycleOwner(), years -> {
            if (years == null) { if (bind != null) bind.homeFlashbackSector.setVisibility(View.GONE); }
            else { if (bind != null) bind.homeFlashbackSector.setVisibility(!years.isEmpty() ? View.VISIBLE : View.GONE); yearAdapter.setItems(years); }
        });
        new CustomLinearSnapHelper().attachToRecyclerView(bind.yearsRecyclerView);
    }

    private void initMostPlayedSongView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_MOST_PLAYED)) return;
        bind.mostPlayedAlbumsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        bind.mostPlayedAlbumsRecyclerView.setHasFixedSize(true);
        mostPlayedSongAdapter = new SongHorizontalAdapter(this, true, false, null);
        bind.mostPlayedAlbumsRecyclerView.setAdapter(mostPlayedSongAdapter);
        homeViewModel.getRandomShuffleSample().observe(getViewLifecycleOwner(), songs -> {
            if (songs == null) { if (bind != null) bind.homeMostPlayedAlbumsSector.setVisibility(View.GONE); }
            else { if (bind != null) { bind.homeMostPlayedAlbumsSector.setVisibility(!songs.isEmpty() ? View.VISIBLE : View.GONE); bind.mostPlayedAlbumsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), UIUtil.getSpanCount(songs.size(), 4), GridLayoutManager.HORIZONTAL, false)); } mostPlayedSongAdapter.setItems(songs.subList(0, Math.min(20, songs.size()))); }
        });
    }

    private void initRecentPlayedAlbumView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_LAST_PLAYED)) return;
        bind.recentlyPlayedAlbumsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        bind.recentlyPlayedAlbumsRecyclerView.setHasFixedSize(true);
        recentlyPlayedAlbumAdapter = new AlbumAdapter(this);
        bind.recentlyPlayedAlbumsRecyclerView.setAdapter(recentlyPlayedAlbumAdapter);
        homeViewModel.getRecentlyPlayedAlbumList(getViewLifecycleOwner()).observe(getViewLifecycleOwner(), albums -> {
            if (albums == null) { if (bind != null) bind.homeRecentlyPlayedAlbumsSector.setVisibility(View.GONE); }
            else { if (bind != null) bind.homeRecentlyPlayedAlbumsSector.setVisibility(!albums.isEmpty() ? View.VISIBLE : View.GONE); recentlyPlayedAlbumAdapter.setItems(albums); }
        });
        new CustomLinearSnapHelper().attachToRecyclerView(bind.recentlyPlayedAlbumsRecyclerView);
    }

    private void initRecentAddedAlbumView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_RECENTLY_ADDED)) return;
        bind.recentlyAddedAlbumsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        bind.recentlyAddedAlbumsRecyclerView.setHasFixedSize(true);
        recentlyAddedAlbumAdapter = new AlbumAdapter(this);
        bind.recentlyAddedAlbumsRecyclerView.setAdapter(recentlyAddedAlbumAdapter);
        homeViewModel.getMostRecentlyAddedAlbums(getViewLifecycleOwner()).observe(getViewLifecycleOwner(), albums -> {
            if (albums == null) { if (bind != null) bind.homeRecentlyAddedAlbumsSector.setVisibility(View.GONE); }
            else { if (bind != null) bind.homeRecentlyAddedAlbumsSector.setVisibility(!albums.isEmpty() ? View.VISIBLE : View.GONE); recentlyAddedAlbumAdapter.setItems(albums); }
        });
        new CustomLinearSnapHelper().attachToRecyclerView(bind.recentlyAddedAlbumsRecyclerView);
    }

    private void initPinnedPlaylistsView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_PINNED_PLAYLISTS)) return;
        bind.pinnedPlaylistsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.pinnedPlaylistsRecyclerView.setHasFixedSize(true);
        playlistHorizontalAdapter = new PlaylistHorizontalAdapter(this);
        bind.pinnedPlaylistsRecyclerView.setAdapter(playlistHorizontalAdapter);
        homeViewModel.getPinnedPlaylists(getViewLifecycleOwner()).observe(getViewLifecycleOwner(), playlists -> {
            if (playlists == null) { if (bind != null) bind.pinnedPlaylistsSector.setVisibility(View.GONE); }
            else { if (bind != null) bind.pinnedPlaylistsSector.setVisibility(!playlists.isEmpty() ? View.VISIBLE : View.GONE); playlistHorizontalAdapter.setItems(playlists); }
        });
    }

    private void initSharesView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_SHARED)) return;
        bind.sharesRecyclerView.setHasFixedSize(true);
        shareHorizontalAdapter = new ShareHorizontalAdapter(this);
        bind.sharesRecyclerView.setAdapter(shareHorizontalAdapter);
        if (Preferences.isSharingEnabled()) {
            homeViewModel.getShares(getViewLifecycleOwner()).observe(getViewLifecycleOwner(), shares -> {
                if (shares == null) { if (bind != null) bind.sharesSector.setVisibility(View.GONE); }
                else { if (bind != null) { bind.sharesSector.setVisibility(!shares.isEmpty() ? View.VISIBLE : View.GONE); bind.sharesRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), UIUtil.getSpanCount(shares.size(), 4), GridLayoutManager.HORIZONTAL, false)); } shareHorizontalAdapter.setItems(shares); }
            });
        }
    }

    private void initHomeReorganizer() {
        new Handler().postDelayed(() -> { if (bind != null) bind.homeSectorRearrangementButton.setVisibility(View.VISIBLE); }, 5000);
        bind.homeSectorRearrangementButton.setOnClickListener(v -> new HomeRearrangementDialog().show(requireActivity().getSupportFragmentManager(), null));
    }

    private void refreshSharesView() {
        new Handler().postDelayed(() -> { if (getView() != null && bind != null && Preferences.isSharingEnabled()) homeViewModel.refreshShares(getViewLifecycleOwner()); }, 100);
    }

    private void setSlideViewOffset(ViewPager2 viewPager, float pageOffset, float pageMargin) {
        viewPager.setPageTransformer((page, position) -> {
            float myOffset = position * -(2 * pageOffset + pageMargin);
            if (viewPager.getOrientation() == ViewPager2.ORIENTATION_HORIZONTAL) { if (ViewCompat.getLayoutDirection(viewPager) == ViewCompat.LAYOUT_DIRECTION_RTL) page.setTranslationX(-myOffset); else page.setTranslationX(myOffset); }
            else page.setTranslationY(myOffset);
        });
    }

    public void reorder() {
        if (bind != null && homeViewModel.getHomeSectorList() != null) {
            bind.homeLinearLayoutContainer.removeAllViews();
            for (HomeSector sector : homeViewModel.getHomeSectorList()) {
                if (!sector.isVisible()) continue;
                switch (sector.getId()) {
                    case Constants.HOME_SECTOR_DISCOVERY: bind.homeLinearLayoutContainer.addView(bind.homeDiscoverSector); break;
                    case Constants.HOME_SECTOR_MADE_FOR_YOU: bind.homeLinearLayoutContainer.addView(bind.homeSimilarTracksSector); break;
                    case Constants.HOME_SECTOR_RADIO_STATION: bind.homeLinearLayoutContainer.addView(bind.homeRadioArtistSector); break;
                    case Constants.HOME_SECTOR_TOP_SONGS: bind.homeLinearLayoutContainer.addView(bind.homeGridTracksSector); break;
                    case Constants.HOME_SECTOR_STARRED_TRACKS: bind.homeLinearLayoutContainer.addView(bind.starredTracksSector); break;
                    case Constants.HOME_SECTOR_STARRED_ALBUMS: bind.homeLinearLayoutContainer.addView(bind.starredAlbumsSector); break;
                    case Constants.HOME_SECTOR_STARRED_ARTISTS: bind.homeLinearLayoutContainer.addView(bind.starredArtistsSector); break;
                    case Constants.HOME_SECTOR_NEW_RELEASES: bind.homeLinearLayoutContainer.addView(bind.homeNewReleasesSector); break;
                    case Constants.HOME_SECTOR_FLASHBACK: bind.homeLinearLayoutContainer.addView(bind.homeFlashbackSector); break;
                    case Constants.HOME_SECTOR_MOST_PLAYED: bind.homeLinearLayoutContainer.addView(bind.homeMostPlayedAlbumsSector); break;
                    case Constants.HOME_SECTOR_LAST_PLAYED: bind.homeLinearLayoutContainer.addView(bind.homeRecentlyPlayedAlbumsSector); break;
                    case Constants.HOME_SECTOR_RECENTLY_ADDED: bind.homeLinearLayoutContainer.addView(bind.homeRecentlyAddedAlbumsSector); break;
                    case Constants.HOME_SECTOR_PINNED_PLAYLISTS: bind.homeLinearLayoutContainer.addView(bind.pinnedPlaylistsSector); break;
                    case Constants.HOME_SECTOR_SHARED: bind.homeLinearLayoutContainer.addView(bind.sharesSector); break;
                }
            }
            bind.homeLinearLayoutContainer.addView(bind.homeSectorRearrangementButton);
        }
    }

    private void showPopupMenu(View view, int menuResource) {
        PopupMenu popup = new PopupMenu(requireContext(), view);
        popup.getMenuInflater().inflate(menuResource, popup.getMenu());
        popup.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.menu_last_week_name) { homeViewModel.changeChronologyPeriod(getViewLifecycleOwner(), 0); bind.gridTracksPreTextView.setText(getString(R.string.home_title_last_week)); return true; }
            else if (menuItem.getItemId() == R.id.menu_last_month_name) { homeViewModel.changeChronologyPeriod(getViewLifecycleOwner(), 1); bind.gridTracksPreTextView.setText(getString(R.string.home_title_last_month)); return true; }
            else if (menuItem.getItemId() == R.id.menu_last_year_name) { homeViewModel.changeChronologyPeriod(getViewLifecycleOwner(), 2); bind.gridTracksPreTextView.setText(getString(R.string.home_title_last_year)); return true; }
            return false;
        });
        popup.show();
    }

    private void refreshPlaylistView() { new Handler().postDelayed(() -> { if (getView() != null && bind != null && homeViewModel != null) homeViewModel.getPinnedPlaylists(getViewLifecycleOwner()); }, 100); }
    private void initializeMediaBrowser() { mediaBrowserListenableFuture = new MediaBrowser.Builder(requireContext(), new SessionToken(requireContext(), new ComponentName(requireContext(), MediaService.class))).buildAsync(); }
    private void releaseMediaBrowser() { MediaBrowser.releaseFuture(mediaBrowserListenableFuture); }

    @Override public void onMediaClick(Bundle b) {
        if (b.containsKey(Constants.MEDIA_MIX)) {
            MediaManager.startQueue(mediaBrowserListenableFuture, b.getParcelable(Constants.TRACK_OBJECT));
            activity.setBottomSheetInPeek(true);
            if (mediaBrowserListenableFuture != null) homeViewModel.getMediaInstantMix(getViewLifecycleOwner(), b.getParcelable(Constants.TRACK_OBJECT)).observe(getViewLifecycleOwner(), songs -> { MusicUtil.ratingFilter(songs); if (songs != null && !songs.isEmpty()) MediaManager.enqueue(mediaBrowserListenableFuture, songs, true); });
        } else if (b.containsKey(Constants.MEDIA_CHRONOLOGY)) {
            ArrayList<Chronology> chronologies = b.getParcelableArrayList(Constants.TRACKS_OBJECT);
            if (chronologies != null) {
                List<Child> media = chronologies.stream().map(c -> (Child) c).collect(Collectors.toList());
                MediaManager.startQueue(mediaBrowserListenableFuture, media, b.getInt(Constants.ITEM_POSITION));
                activity.setBottomSheetInPeek(true);
            }
        } else {
            MediaManager.startQueue(mediaBrowserListenableFuture, b.getParcelableArrayList(Constants.TRACKS_OBJECT), b.getInt(Constants.ITEM_POSITION));
            activity.setBottomSheetInPeek(true);
        }
    }
    @Override public void onMediaLongClick(Bundle b) { Navigation.findNavController(requireView()).navigate(R.id.songBottomSheetDialog, b); }
    @Override public void onAlbumClick(Bundle b) { Navigation.findNavController(requireView()).navigate(R.id.albumPageFragment, b); }
    @Override public void onAlbumLongClick(Bundle b) { Navigation.findNavController(requireView()).navigate(R.id.albumBottomSheetDialog, b); }
    @Override public void onArtistClick(Bundle b) {
        if (b.containsKey(Constants.MEDIA_MIX) && b.getBoolean(Constants.MEDIA_MIX)) {
            Snackbar.make(requireView(), R.string.artist_adapter_radio_station_starting, Snackbar.LENGTH_LONG).setAnchorView(activity.bind.playerBottomSheet).show();
            if (mediaBrowserListenableFuture != null) homeViewModel.getArtistInstantMix(getViewLifecycleOwner(), b.getParcelable(Constants.ARTIST_OBJECT)).observe(getViewLifecycleOwner(), songs -> { MusicUtil.ratingFilter(songs); if (!songs.isEmpty()) { MediaManager.startQueue(mediaBrowserListenableFuture, songs, 0); activity.setBottomSheetInPeek(true); } });
        } else { Navigation.findNavController(requireView()).navigate(R.id.artistPageFragment, b); }
    }
    @Override public void onArtistLongClick(Bundle b) { Navigation.findNavController(requireView()).navigate(R.id.artistBottomSheetDialog, b); }
    @Override public void onYearClick(Bundle b) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.MEDIA_BY_YEAR, Constants.MEDIA_BY_YEAR);
        bundle.putInt("year_object", b.getInt("year_object"));
        Navigation.findNavController(requireView()).navigate(R.id.songListPageFragment, bundle);
    }
    @Override public void onShareClick(Bundle b) { Share share = b.getParcelable(Constants.SHARE_OBJECT); Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(share.getUrl())).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent); }
    @Override public void onPlaylistClick(Bundle b) { Navigation.findNavController(requireView()).navigate(R.id.playlistPageFragment, b); }
    @Override public void onPlaylistLongClick(Bundle b) {
        PlaylistEditorDialog dialog = new PlaylistEditorDialog(new PlaylistCallback() {
            @Override
            public void onDismiss() {
                refreshPlaylistView();
            }
        });
        dialog.setArguments(b);
        dialog.show(activity.getSupportFragmentManager(), null);
    }
    @Override public void onShareLongClick(Bundle b) { Navigation.findNavController(requireView()).navigate(R.id.shareBottomSheetDialog, b); }
}