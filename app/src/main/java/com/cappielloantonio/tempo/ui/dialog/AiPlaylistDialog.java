package com.cappielloantonio.tempo.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.media3.common.util.UnstableApi;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.DialogAiPlaylistBinding;
import com.cappielloantonio.tempo.repository.GenreRepository;
import com.cappielloantonio.tempo.repository.SongRepository;
import com.cappielloantonio.tempo.service.MediaManager;
import com.cappielloantonio.tempo.ui.activity.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@UnstableApi
public class AiPlaylistDialog extends DialogFragment {

    private DialogAiPlaylistBinding bind;

    private static final List<String> COMMON_GENRES = Arrays.asList(
            "Pop",
            "Rock",
            "Hip-Hop / Rap",
            "Chanson Française",
            "Electro / Dance",
            "Jazz",
            "Blues",
            "Classique",
            "Reggae",
            "R&B / Soul",
            "Metal",
            "Indie / Alternative",
            "Variété",
            "Funk / Disco",
            "Latin",
            "Soundtrack / Musique de film"
    );

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        bind = DialogAiPlaylistBinding.inflate(LayoutInflater.from(getContext()));

        setupSpinners();

        bind.aiCancelButton.setOnClickListener(v -> dismiss());
        bind.aiGenerateButton.setOnClickListener(v -> generate());

        return new AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
                .setView(bind.getRoot())
                .create();
    }

    private void setupSpinners() {
        // Prepare initial list with common genres
        Set<String> genreSet = new LinkedHashSet<>();
        genreSet.add("Tous les genres");
        genreSet.addAll(COMMON_GENRES);

        List<String> initialNames = new ArrayList<>(genreSet);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, initialNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bind.aiGenreSpinner.setAdapter(adapter);

        // Merge with server genres if available
        new GenreRepository().getGenres(false, 0).observe(this, genres -> {
            if (genres != null && !genres.isEmpty() && getContext() != null) {
                Set<String> mergedSet = new LinkedHashSet<>();
                mergedSet.add("Tous les genres");
                for (com.cappielloantonio.tempo.subsonic.models.Genre g : genres) {
                    if (g.getGenre() != null && !g.getGenre().trim().isEmpty()) {
                        mergedSet.add(g.getGenre().trim());
                    }
                }
                mergedSet.addAll(COMMON_GENRES);

                List<String> updatedNames = new ArrayList<>(mergedSet);
                ArrayAdapter<String> updatedAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, updatedNames);
                updatedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                bind.aiGenreSpinner.setAdapter(updatedAdapter);
            }
        });

        // Decades
        List<String> decades = new ArrayList<>();
        decades.add("Toutes les années");
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int startDecade = (currentYear / 10) * 10;
        for (int i = startDecade; i >= 1950; i -= 10) {
            decades.add(i + "s");
        }
        ArrayAdapter<String> decadeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, decades);
        decadeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bind.aiDecadeSpinner.setAdapter(decadeAdapter);
    }

    private void generate() {
        if (bind == null || bind.aiGenreSpinner.getSelectedItem() == null || bind.aiDecadeSpinner.getSelectedItem() == null) {
            return;
        }

        String genre = bind.aiGenreSpinner.getSelectedItem().toString();
        String decadeStr = bind.aiDecadeSpinner.getSelectedItem().toString();

        Integer fromYear = null;
        Integer toYear = null;
        if (!decadeStr.equals("Toutes les années")) {
            try {
                fromYear = Integer.parseInt(decadeStr.replace("s", ""));
                toYear = fromYear + 9;
            } catch (Exception ignored) {}
        }

        String finalGenre = genre.equals("Tous les genres") ? null : genre;

        new SongRepository().getRandomSample(50, fromYear, toYear).observe(this, songs -> {
            if (songs != null && !songs.isEmpty()) {
                List<com.cappielloantonio.tempo.subsonic.models.Child> filtered = songs;
                if (finalGenre != null) {
                    filtered = new ArrayList<>();
                    String searchKey = finalGenre.toLowerCase();
                    // Split key for composite genres like "Hip-Hop / Rap" or "Electro / Dance"
                    String[] parts = searchKey.split("/");

                    for (com.cappielloantonio.tempo.subsonic.models.Child s : songs) {
                        if (s.getGenre() != null) {
                            String songGenre = s.getGenre().toLowerCase();
                            boolean matches = false;
                            for (String p : parts) {
                                if (songGenre.contains(p.trim())) {
                                    matches = true;
                                    break;
                                }
                            }
                            if (matches) {
                                filtered.add(s);
                            }
                        }
                    }
                }

                if (!filtered.isEmpty()) {
                    MediaManager.startQueue(((MainActivity) requireActivity()).getMediaBrowserListenableFuture(), filtered, 0);
                    ((MainActivity) requireActivity()).setBottomSheetInPeek(true);
                    dismiss();
                } else {
                    // Fallback to random sample if no match for strict genre filter
                    MediaManager.startQueue(((MainActivity) requireActivity()).getMediaBrowserListenableFuture(), songs, 0);
                    ((MainActivity) requireActivity()).setBottomSheetInPeek(true);
                    dismiss();
                }
            }
        });
    }
}
