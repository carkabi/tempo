package com.cappielloantonio.tempo.ui.fragment;

import android.content.ClipData;
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

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.FragmentAddMusicBinding;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.viewmodel.AddMusicViewModel;

import java.util.Objects;

@UnstableApi
public class AddMusicFragment extends Fragment {

    private FragmentAddMusicBinding bind;
    private AddMusicViewModel viewModel;
    private MainActivity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        bind = FragmentAddMusicBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(AddMusicViewModel.class);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initClickListeners();
        observeViewModel();
        checkArguments();
    }

    private void checkArguments() {
        if (getArguments() != null) {
            String sharedUrl = getArguments().getString("shared_url");
            if (sharedUrl != null && !sharedUrl.isEmpty()) {
                bind.spotifyUrlEditText.setText(sharedUrl);
                viewModel.addMusic(sharedUrl);
            }
        }
    }

    private void initClickListeners() {
        bind.pasteButton.setOnClickListener(v -> pasteFromClipboard());
        bind.addButton.setOnClickListener(v -> {
            String url = Objects.requireNonNull(bind.spotifyUrlEditText.getText()).toString().trim();
            if (validateSpotifyUrl(url)) {
                viewModel.addMusic(url);
            } else {
                bind.spotifyUrlInputLayout.setError(getString(R.string.add_music_error_invalid));
            }
        });
        bind.spotifyUrlEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                bind.statusCard.setVisibility(View.GONE);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case IDLE:
                    setLoading(false);
                    bind.statusCard.setVisibility(View.GONE);
                    break;
                case LOADING:
                    setLoading(true);
                    bind.statusCard.setVisibility(View.GONE);
                    bind.spotifyUrlInputLayout.setError(null);
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
            }
        });
    }

    private void setLoading(boolean isLoading) {
        bind.loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        bind.addButton.setEnabled(!isLoading);
        bind.pasteButton.setEnabled(!isLoading);
        bind.spotifyUrlInputLayout.setEnabled(!isLoading);
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence text = clip.getItemAt(0).getText();
                if (text != null) {
                    bind.spotifyUrlEditText.setText(text);
                }
            }
        }
    }

    private boolean validateSpotifyUrl(String url) {
        return url.contains("spotify.com") && (url.contains("/track/") || url.contains("/album/") || url.contains("/artist/"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }
}