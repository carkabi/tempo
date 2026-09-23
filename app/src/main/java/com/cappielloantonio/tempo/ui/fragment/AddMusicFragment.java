package com.cappielloantonio.tempo.ui.fragment;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cappielloantonio.tempo.databinding.FragmentAddMusicBinding;
import com.cappielloantonio.tempo.ui.adapter.MusicRequestAdapter;
import com.cappielloantonio.tempo.viewmodel.AddMusicViewModel;

@UnstableApi
public class AddMusicFragment extends Fragment {

    private FragmentAddMusicBinding bind;
    private AddMusicViewModel viewModel;
    private MusicRequestAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentAddMusicBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AddMusicViewModel.class);

        initRecyclerView();
        initClickListeners();
        observeViewModel();
        checkArguments();
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.startAutoRefresh();
    }

    @Override
    public void onStop() {
        super.onStop();
        viewModel.stopAutoRefresh();
    }

    private void initRecyclerView() {
        adapter = new MusicRequestAdapter(requireContext());
        bind.requestsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.requestsRecyclerView.setAdapter(adapter);
    }

    private void checkArguments() {
        if (getArguments() != null && getArguments().containsKey("shared_url")) {
            String sharedUrl = getArguments().getString("shared_url");
            bind.spotifyUrlEditText.setText(sharedUrl);
        }
    }

    private void initClickListeners() {
        bind.pasteButton.setOnClickListener(v -> pasteFromClipboard());
        bind.addButton.setOnClickListener(v -> {
            String url = bind.spotifyUrlEditText.getText() != null ? bind.spotifyUrlEditText.getText().toString().trim() : "";
            if (validateSpotifyUrl(url)) {
                viewModel.addMusic(url);
            } else {
                bind.spotifyUrlInputLayout.setError("Veuillez entrer un lien Spotify valide");
            }
        });
        bind.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadHistory();
            bind.swipeRefresh.setRefreshing(false);
        });
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case LOADING:
                    setLoading(true);
                    bind.statusCard.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    setLoading(false);
                    bind.statusCard.setVisibility(View.VISIBLE);
                    bind.statusTextView.setText(viewModel.getStatusMessage().getValue());
                    bind.spotifyUrlEditText.setText("");
                    break;
                case ERROR:
                    setLoading(false);
                    bind.statusCard.setVisibility(View.VISIBLE);
                    bind.statusTextView.setText(viewModel.getStatusMessage().getValue());
                    break;
                default:
                    setLoading(false);
            }
        });

        viewModel.getRequestsList().observe(getViewLifecycleOwner(), requests -> {
            if (requests != null) {
                adapter.setItems(requests);
                bind.emptyHistoryLayout.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setLoading(boolean isLoading) {
        bind.loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        bind.addButton.setEnabled(!isLoading);
        bind.pasteButton.setEnabled(!isLoading);
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
            CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (text != null) {
                bind.spotifyUrlEditText.setText(text);
            }
        }
    }

    private boolean validateSpotifyUrl(String url) {
        return url != null && (url.contains("spotify.com") || url.contains("open.spotify.com"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }
}