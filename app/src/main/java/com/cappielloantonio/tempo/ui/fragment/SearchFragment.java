package com.cappielloantonio.tempo.ui.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaBrowser;
import androidx.media3.session.SessionToken;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cappielloantonio.tempo.BuildConfig;
import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.FragmentSearchBinding;
import com.cappielloantonio.tempo.helper.recyclerview.CustomLinearSnapHelper;
import com.cappielloantonio.tempo.interfaces.ClickCallback;
import com.cappielloantonio.tempo.repository.GenreRepository;
import com.cappielloantonio.tempo.repository.PlaylistRepository;
import com.cappielloantonio.tempo.repository.peach.models.PeachRadioStation;
import com.cappielloantonio.tempo.subsonic.models.Playlist;
import com.cappielloantonio.tempo.service.MediaManager;
import com.cappielloantonio.tempo.service.MediaService;
import com.cappielloantonio.tempo.service.PeachRadioPlayerManager;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.ui.adapter.AlbumAdapter;
import com.cappielloantonio.tempo.ui.adapter.ArtistAdapter;
import com.cappielloantonio.tempo.ui.adapter.PlaylistHorizontalAdapter;
import com.cappielloantonio.tempo.ui.adapter.SongHorizontalAdapter;
import com.cappielloantonio.tempo.util.Constants;
import com.cappielloantonio.tempo.util.PeachGenreCatalog;
import com.cappielloantonio.tempo.viewmodel.HomeViewModel;
import com.cappielloantonio.tempo.viewmodel.RadioViewModel;
import com.cappielloantonio.tempo.viewmodel.SearchViewModel;
import com.google.android.material.chip.Chip;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@UnstableApi
public class SearchFragment extends Fragment implements ClickCallback {
    private FragmentSearchBinding bind;
    private MainActivity activity;
    private SearchViewModel searchViewModel;
    private HomeViewModel homeViewModel;
    private RadioViewModel radioViewModel;

    private final PlaylistRepository playlistRepository =
            new PlaylistRepository();
    private final GenreRepository genreRepository =
            new GenreRepository();

    private ArtistAdapter artistAdapter;
    private AlbumAdapter albumAdapter;
    private PlaylistHorizontalAdapter playlistAdapter;
    private SongHorizontalAdapter songHorizontalAdapter;

    private List<Playlist> allPlaylists = new ArrayList<>();
    private List<PeachGenreCatalog.Group> peachGenreGroups =
            new ArrayList<>();
    private List<PeachRadioStation> peachRadios =
            new ArrayList<>();

    private ListenableFuture<MediaBrowser> mediaBrowserListenableFuture;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        bind = FragmentSearchBinding.inflate(inflater, container, false);
        searchViewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        radioViewModel = new ViewModelProvider(requireActivity()).get(RadioViewModel.class);

        initSearchResultView();
        initSearchInput();
        initFilterView();
        loadInitialContent();
        return bind.getRoot();
    }

    private void initSearchInput() {
        bind.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                bind.clearSearchIcon.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                if (isQueryValid(query)) {
                    performSearch(query);
                } else if (query.isEmpty()) {
                    loadInitialContent();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        bind.clearSearchIcon.setOnClickListener(v -> {
            bind.searchEditText.setText("");
            loadInitialContent();
        });

        bind.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            hideKeyboard(v);
            return true;
        });
    }

    private void loadInitialContent() {
        if (bind == null) return;
        bind.searchEmptyPlaceholder.setVisibility(View.VISIBLE);
        bind.searchArtistSector.setVisibility(View.GONE);
        bind.searchAlbumSector.setVisibility(View.GONE);
        bind.searchPlaylistSector.setVisibility(View.GONE);
        bind.searchPeachSector.setVisibility(View.GONE);
        bind.searchSongSector.setVisibility(View.GONE);

        if ("peach".equals(BuildConfig.FLAVOR)) {
            updateUniversalResults("");
        }

        // On affiche des morceaux aléatoires pour donner de la vie à la page
        homeViewModel.getRandomShuffleSample().observe(getViewLifecycleOwner(), songs -> {
            if (bind == null || songs == null || songs.isEmpty()) return;

            int checkedId = bind.searchFilterGroup.getCheckedChipId();
            boolean showSongs =
                    checkedId == R.id.filter_all
                            || checkedId == R.id.filter_song;

            if (!showSongs) {
                bind.searchSongSector.setVisibility(View.GONE);
                return;
            }

            bind.searchEmptyPlaceholder.setVisibility(View.GONE);
            bind.searchSongSector.setVisibility(View.VISIBLE);
            songHorizontalAdapter.setItems(
                    songs.subList(0, Math.min(30, songs.size()))
            );
            bind.searchResultLayout.setVisibility(View.VISIBLE);
        });
    }

    private void initFilterView() {
        bind.searchFilterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            String query = bind.searchEditText.getText().toString();
            if (isQueryValid(query)) {
                performSearch(query);
            } else {
                loadInitialContent();
            }
        });

        if (!"peach".equals(BuildConfig.FLAVOR)) {
            bind.filterPlaylist.setVisibility(View.GONE);
            bind.filterPeach.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        initializeMediaBrowser();
        activity.setBottomNavigationBarVisibility(true);
        activity.setBottomSheetVisibility(true);
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

    private void initSearchResultView() {
        bind.searchResultArtistRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        bind.searchResultArtistRecyclerView.setHasFixedSize(true);
        artistAdapter = new ArtistAdapter(this, false, false);
        bind.searchResultArtistRecyclerView.setAdapter(artistAdapter);
        new CustomLinearSnapHelper().attachToRecyclerView(bind.searchResultArtistRecyclerView);

        bind.searchResultAlbumRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        bind.searchResultAlbumRecyclerView.setHasFixedSize(true);
        albumAdapter = new AlbumAdapter(this);
        bind.searchResultAlbumRecyclerView.setAdapter(albumAdapter);
        new CustomLinearSnapHelper().attachToRecyclerView(bind.searchResultAlbumRecyclerView);

        bind.searchResultPlaylistRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        bind.searchResultPlaylistRecyclerView.setHasFixedSize(true);
        playlistAdapter = new PlaylistHorizontalAdapter(this);
        bind.searchResultPlaylistRecyclerView.setAdapter(
                playlistAdapter
        );

        bind.searchResultTracksRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        bind.searchResultTracksRecyclerView.setHasFixedSize(true);
        songHorizontalAdapter =
                new SongHorizontalAdapter(this, true, false, null);
        bind.searchResultTracksRecyclerView.setAdapter(
                songHorizontalAdapter
        );

        if ("peach".equals(BuildConfig.FLAVOR)) {
            playlistRepository.getPlaylists(false, -1)
                    .observe(getViewLifecycleOwner(), playlists -> {
                        allPlaylists = playlists != null
                                ? new ArrayList<>(playlists)
                                : new ArrayList<>();
                        updateUniversalResults(
                                bind.searchEditText.getText().toString()
                        );
                    });

            genreRepository.getGenres(false, -1)
                    .observe(getViewLifecycleOwner(), genres -> {
                        peachGenreGroups = new ArrayList<>(
                                PeachGenreCatalog.build(genres)
                        );
                        updateUniversalResults(
                                bind.searchEditText.getText().toString()
                        );
                    });

            radioViewModel.getPeachRadioStations()
                    .observe(getViewLifecycleOwner(), radios -> {
                        peachRadios = radios != null
                                ? new ArrayList<>(radios)
                                : new ArrayList<>();
                        updateUniversalResults(
                                bind.searchEditText.getText().toString()
                        );
                    });
        }
    }

    private void performSearch(String query) {
        searchViewModel.search3(query).observe(getViewLifecycleOwner(), result -> {
            if (bind == null || result == null) return;
            bind.searchEmptyPlaceholder.setVisibility(View.GONE);
            int checkedId = bind.searchFilterGroup.getCheckedChipId();
            boolean showArtists =
                    checkedId == R.id.filter_all
                            || checkedId == R.id.filter_artist;
            boolean showAlbums =
                    checkedId == R.id.filter_all
                            || checkedId == R.id.filter_album;
            boolean showSongs =
                    checkedId == R.id.filter_all
                            || checkedId == R.id.filter_song;

            bind.searchArtistSector.setVisibility(
                    showArtists
                            && result.getArtists() != null
                            && !result.getArtists().isEmpty()
                            ? View.VISIBLE
                            : View.GONE
            );
            if (showArtists && result.getArtists() != null) {
                artistAdapter.setItems(result.getArtists());
            }

            bind.searchAlbumSector.setVisibility(
                    showAlbums
                            && result.getAlbums() != null
                            && !result.getAlbums().isEmpty()
                            ? View.VISIBLE
                            : View.GONE
            );
            if (showAlbums && result.getAlbums() != null) {
                albumAdapter.setItems(result.getAlbums());
            }

            bind.searchSongSector.setVisibility(
                    showSongs
                            && result.getSongs() != null
                            && !result.getSongs().isEmpty()
                            ? View.VISIBLE
                            : View.GONE
            );
            if (showSongs && result.getSongs() != null) {
                songHorizontalAdapter.setItems(result.getSongs());
            }

            if ("peach".equals(BuildConfig.FLAVOR)) {
                updateUniversalResults(query);
            }
        });
    }

    private void updateUniversalResults(String rawQuery) {
        if (bind == null || !"peach".equals(BuildConfig.FLAVOR)) {
            return;
        }

        String query = normalizeSearch(rawQuery);
        int checkedId = bind.searchFilterGroup.getCheckedChipId();
        boolean showPlaylists =
                checkedId == R.id.filter_all
                        || checkedId == R.id.filter_playlist;
        boolean showPeach =
                checkedId == R.id.filter_all
                        || checkedId == R.id.filter_peach;

        List<Playlist> playlistMatches = new ArrayList<>();

        if (showPlaylists) {
            for (Playlist playlist : allPlaylists) {
                String name = playlist.getName() != null
                        ? normalizeSearch(playlist.getName())
                        : "";

                if (query.isEmpty() || name.contains(query)) {
                    playlistMatches.add(playlist);
                }

                if (query.isEmpty() && playlistMatches.size() >= 6) {
                    break;
                }
            }
        }

        playlistAdapter.setItems(playlistMatches);
        bind.searchPlaylistSector.setVisibility(
                showPlaylists && !playlistMatches.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );

        bind.searchPeachChipGroup.removeAllViews();

        if (!showPeach) {
            bind.searchPeachSector.setVisibility(View.GONE);
            return;
        }

        boolean broadQuery = query.isEmpty();

        if (broadQuery
                || query.contains("playlist")
                || query.contains("partag")
                || query.contains("shared")) {
            addPeachChip(
                    getString(R.string.search_peach_shared),
                    v -> Navigation.findNavController(v).navigate(
                            R.id.sharedPlaylistFragment
                    )
            );
        }

        int genreCount = 0;
        for (PeachGenreCatalog.Group group : peachGenreGroups) {
            String title = normalizeSearch(group.getTitle());
            boolean matches = broadQuery || title.contains(query);

            if (!matches) {
                for (String raw : group.getRawGenres()) {
                    if (normalizeSearch(raw).contains(query)) {
                        matches = true;
                        break;
                    }
                }
            }

            if (matches) {
                addGenreChip(group);
                genreCount++;
            }

            if (broadQuery && genreCount >= 5) {
                break;
            }
        }

        int radioCount = 0;
        for (PeachRadioStation station : peachRadios) {
            String searchable = normalizeSearch(
                    (station.getName() != null
                            ? station.getName()
                            : "")
                            + " "
                            + (station.getDescription() != null
                            ? station.getDescription()
                            : "")
            );

            if (broadQuery || searchable.contains(query)
                    || query.contains("radio")) {
                addRadioChip(station);
                radioCount++;
            }

            if (broadQuery && radioCount >= 3) {
                break;
            }
        }

        bind.searchPeachSector.setVisibility(
                bind.searchPeachChipGroup.getChildCount() > 0
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void addGenreChip(PeachGenreCatalog.Group group) {
        addPeachChip(
                getString(
                        R.string.search_peach_genre_prefix,
                        group.getTitle()
                ),
                v -> {
                    Bundle bundle = new Bundle();
                    bundle.putString(
                            Constants.MEDIA_BY_GENRES,
                            Constants.MEDIA_BY_GENRES
                    );
                    bundle.putStringArrayList(
                            "filters_list",
                            group.getRawGenres()
                    );
                    ArrayList<String> names = new ArrayList<>();
                    names.add(group.getTitle());
                    bundle.putStringArrayList(
                            "filter_name_list",
                            names
                    );
                    Navigation.findNavController(v).navigate(
                            R.id.songListPageFragment,
                            bundle
                    );
                }
        );
    }

    private void addRadioChip(PeachRadioStation station) {
        addPeachChip(
                getString(
                        R.string.search_peach_radio_prefix,
                        station.getName()
                ),
                v -> {
                    PeachRadioPlayerManager.startPeachRadio(
                            requireContext(),
                            mediaBrowserListenableFuture,
                            station
                    );
                    activity.setBottomSheetInPeek(true);
                }
        );
    }

    private void addPeachChip(
            String label,
            View.OnClickListener listener
    ) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(false);
        chip.setClickable(true);
        chip.setOnClickListener(listener);
        bind.searchPeachChipGroup.addView(chip);
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(
                value,
                Normalizer.Form.NFD
        ).replaceAll("\\p{M}", "");

        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    private boolean isQueryValid(String query) {
        return query != null && !query.trim().isEmpty();
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void initializeMediaBrowser() {
        mediaBrowserListenableFuture = new MediaBrowser.Builder(requireContext(), new SessionToken(requireContext(), new ComponentName(requireContext(), MediaService.class))).buildAsync();
    }

    private void releaseMediaBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture);
    }

    @Override public void onMediaClick(Bundle b) {
        MediaManager.startQueue(mediaBrowserListenableFuture, b.getParcelableArrayList(Constants.TRACKS_OBJECT), b.getInt(Constants.ITEM_POSITION));
        activity.setBottomSheetInPeek(true);
    }
    @Override public void onMediaLongClick(Bundle b) { Navigation.findNavController(requireView()).navigate(R.id.songBottomSheetDialog, b); }
    @Override public void onAlbumClick(Bundle b) { Navigation.findNavController(requireView()).navigate(R.id.albumPageFragment, b); }
    @Override public void onAlbumLongClick(Bundle b) { Navigation.findNavController(requireView()).navigate(R.id.albumBottomSheetDialog, b); }
    @Override public void onArtistClick(Bundle b) { Navigation.findNavController(requireView()).navigate(R.id.artistPageFragment, b); }
    @Override public void onArtistLongClick(Bundle b) { Navigation.findNavController(requireView()).navigate(R.id.artistBottomSheetDialog, b); }
    @Override public void onPlaylistClick(Bundle b) {
        b.putBoolean("is_offline", false);
        Navigation.findNavController(requireView()).navigate(
                R.id.playlistPageFragment,
                b
        );
    }
    @Override public void onPlaylistLongClick(Bundle b) {
        onPlaylistClick(b);
    }
}