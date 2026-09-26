package com.cappielloantonio.tempo.ui.fragment;

import android.content.ComponentName;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaBrowser;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.FragmentSharedPlaylistBinding;
import com.cappielloantonio.tempo.repository.tropikeau.TropikeauRepository;
import com.cappielloantonio.tempo.repository.tropikeau.models.SharedPlaylistItem;
import com.cappielloantonio.tempo.repository.tropikeau.models.SharedPlaylistResponse;
import com.cappielloantonio.tempo.service.MediaManager;
import com.cappielloantonio.tempo.service.MediaService;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.ui.adapter.SharedPlaylistAdapter;
import com.cappielloantonio.tempo.util.SharedPlaylistMapper;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@UnstableApi
public class SharedPlaylistFragment extends Fragment
        implements SharedPlaylistAdapter.Callback {

    private FragmentSharedPlaylistBinding binding;
    private MainActivity activity;
    private SharedPlaylistAdapter adapter;
    private final TropikeauRepository repository =
            new TropikeauRepository();

    private ListenableFuture<MediaBrowser> mediaBrowserFuture;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSharedPlaylistBinding.inflate(
                inflater,
                container,
                false
        );
        activity = (MainActivity) requireActivity();

        initToolbar();
        initList();
        initActions();
        loadPlaylist();

        return binding.getRoot();
    }
    @Override
    public void onStart() {
        super.onStart();
        mediaBrowserFuture = new MediaBrowser.Builder(
                requireContext(),
                new SessionToken(
                        requireContext(),
                        new ComponentName(
                                requireContext(),
                                MediaService.class
                        )
                )
        ).buildAsync();

        mediaBrowserFuture.addListener(() -> {
            try {
                MediaBrowser browser = mediaBrowserFuture.get();
                updateCurrentTrack(browser);

                browser.addListener(new Player.Listener() {
                    @Override
                    public void onMediaMetadataChanged(
                            @NonNull MediaMetadata mediaMetadata
                    ) {
                        updateCurrentTrack(browser);
                    }

                    @Override
                    public void onMediaItemTransition(
                            @Nullable androidx.media3.common.MediaItem item,
                            int reason
                    ) {
                        updateCurrentTrack(browser);
                    }
                });
            } catch (Exception ignored) {
            }
        }, MoreExecutors.directExecutor());
    }

    private void updateCurrentTrack(
            MediaBrowser browser
    ) {
        if (adapter == null || browser == null) {
            return;
        }

        MediaMetadata metadata =
                browser.getMediaMetadata();

        String trackId =
                metadata != null
                        && metadata.extras != null
                        ? metadata.extras.getString("id")
                        : null;

        if (
                (trackId == null || trackId.trim().isEmpty())
                        && browser.getCurrentMediaItem() != null
        ) {
            trackId =
                    browser.getCurrentMediaItem().mediaId;
        }

        final String currentTrackId = trackId;

        if (getActivity() != null) {
            requireActivity().runOnUiThread(
                    () -> adapter.setCurrentTrackId(
                            currentTrackId
                    )
            );
        }
    }

    @Override
    public void onStop() {
        if (mediaBrowserFuture != null) {
            MediaBrowser.releaseFuture(mediaBrowserFuture);
            mediaBrowserFuture = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private void initToolbar() {
        activity.setSupportActionBar(binding.toolbar);
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar()
                    .setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(
                v -> activity.navController.navigateUp()
        );
    }
    private void initList() {
        adapter = new SharedPlaylistAdapter(this);
        binding.sharedPlaylistRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        binding.sharedPlaylistRecyclerView.setAdapter(adapter);
        binding.sharedPlaylistRecyclerView.setHasFixedSize(true);
    }

    private void initActions() {
        binding.sharedPlaylistRefresh.setOnRefreshListener(
                this::loadPlaylist
        );

        binding.playAllButton.setOnClickListener(v -> {
            if (adapter.getItemCount() > 0) {
                playFrom(0, false);
            }
        });

        binding.shuffleButton.setOnClickListener(v -> {
            if (adapter.getItemCount() > 0) {
                playFrom(0, true);
            }
        });
    }

    private void loadPlaylist() {
        if (binding == null) return;

        binding.sharedPlaylistRefresh.setRefreshing(true);

        repository.getSharedPlaylist(
                new TropikeauRepository.SharedPlaylistCallback() {
                    @Override
                    public void onSuccess(
                            SharedPlaylistResponse response
                    ) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (binding == null) return;
                            List<SharedPlaylistItem> items =
                                    response.getItems();
                            adapter.setItems(items);
                            updateEmptyState(items);
                            binding.sharedPlaylistRefresh
                                    .setRefreshing(false);
                        });
                    }
                    @Override
                    public void onError(int code, String message) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (binding == null) return;
                            binding.sharedPlaylistRefresh
                                    .setRefreshing(false);
                            Toast.makeText(
                                    requireContext(),
                                    message,
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }
                }
        );
    }

    private void updateEmptyState(List<SharedPlaylistItem> items) {
        boolean empty = items == null || items.isEmpty();
        binding.sharedPlaylistEmpty.setVisibility(
                empty ? View.VISIBLE : View.GONE
        );
        binding.playAllButton.setEnabled(!empty);
        binding.shuffleButton.setEnabled(!empty);

        int count = empty ? 0 : items.size();
        binding.sharedPlaylistCount.setText(
                getResources().getQuantityString(
                        R.plurals.shared_playlist_count,
                        count,
                        count
                )
        );
    }
    private void playFrom(int position, boolean shuffle) {
        List<SharedPlaylistItem> source = adapter.getItems();
        if (source.isEmpty()) return;

        if (shuffle) {
            Collections.shuffle(source);
            position = 0;
        }

        ArrayList<com.cappielloantonio.tempo.subsonic.models.Child>
                songs = SharedPlaylistMapper.toChildren(source);

        if (songs.isEmpty() || mediaBrowserFuture == null) return;

        MediaManager.startQueue(
                mediaBrowserFuture,
                songs,
                Math.min(position, songs.size() - 1)
        );
        activity.setBottomSheetInPeek(true);
    }

    @Override
    public void onPlay(int position) {
        playFrom(position, false);
    }

    @Override
    public void onVote(
            SharedPlaylistItem item,
            int vote
    ) {
        repository.voteSharedTrack(
                item.getId(),
                vote,
                new TropikeauRepository.SharedPlaylistCallback() {
                    @Override
                    public void onSuccess(
                            SharedPlaylistResponse response
                    ) {
                        if (!isAdded()) return;

                        requireActivity().runOnUiThread(
                                SharedPlaylistFragment.this::loadPlaylist
                        );
                    }

                    @Override
                    public void onError(
                            int code,
                            String message
                    ) {
                        if (!isAdded()) return;

                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(
                                        requireContext(),
                                        message,
                                        Toast.LENGTH_LONG
                                ).show()
                        );
                    }
                }
        );
    }

    @Override
    public void onRemove(SharedPlaylistItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.shared_playlist_remove_title)
                .setMessage(
                        getString(
                                R.string.shared_playlist_remove_message,
                                item.getTitle()
                        )
                )
                .setNegativeButton(
                        android.R.string.cancel,
                        null
                )
                .setPositiveButton(
                        R.string.shared_playlist_remove_action,
                        (dialog, which) -> removeItem(item)
                )
                .show();
    }
    private void removeItem(SharedPlaylistItem item) {
        repository.removeSharedTrack(
                item.getId(),
                new TropikeauRepository.SharedPlaylistCallback() {
                    @Override
                    public void onSuccess(
                            SharedPlaylistResponse response
                    ) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(
                                    requireContext(),
                                    R.string.shared_playlist_removed,
                                    Toast.LENGTH_SHORT
                            ).show();
                            loadPlaylist();
                        });
                    }

                    @Override
                    public void onError(int code, String message) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(
                                        requireContext(),
                                        message,
                                        Toast.LENGTH_LONG
                                ).show()
                        );
                    }
                }
        );
    }
}
