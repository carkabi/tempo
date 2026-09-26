package com.cappielloantonio.tempo.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;
import androidx.navigation.Navigation;

import com.cappielloantonio.tempo.BuildConfig;
import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.FragmentLibraryBinding;
import com.cappielloantonio.tempo.ui.dialog.AiPlaylistDialog;
import com.cappielloantonio.tempo.util.Constants;

@UnstableApi
public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding bind;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentLibraryBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (bind == null) return;

        bind.libraryLatestReleasesButton.setOnClickListener(this::showLatestReleasesMenu);
        bind.libraryArtistsButton.setOnClickListener(v -> navigate(v, R.id.action_libraryFragment_to_artistCatalogueFragment, null));
        bind.libraryAlbumsButton.setOnClickListener(v -> navigate(v, R.id.action_libraryFragment_to_albumCatalogueFragment, null));
        bind.librarySongsButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.MEDIA_STARRED, Constants.MEDIA_STARRED);
            navigate(v, R.id.action_libraryFragment_to_songListPageFragment, bundle);
        });
        bind.libraryGenresButton.setOnClickListener(v -> navigate(v, R.id.action_libraryFragment_to_genreCatalogueFragment, null));
        bind.libraryPlaylistsButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.PLAYLIST_ALL, Constants.PLAYLIST_ALL);
            navigate(v, R.id.action_libraryFragment_to_playlistCatalogueFragment, bundle);
        });

        if ("peach".equals(BuildConfig.FLAVOR)) {
            bind.librarySharedPlaylistButton.setVisibility(View.VISIBLE);
            bind.librarySharedPlaylistButton.setOnClickListener(
                    v -> navigate(
                            v,
                            R.id.action_libraryFragment_to_sharedPlaylistFragment,
                            null
                    )
            );
        } else {
            bind.librarySharedPlaylistButton.setVisibility(View.GONE);
        }

        bind.libraryDownloadsButton.setOnClickListener(v -> navigate(v, R.id.downloadFragment, null));
        bind.libraryFoldersButton.setOnClickListener(v -> generateAiPlaylist());
    }

    private void showLatestReleasesMenu(View anchorView) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenu().add(0, 1, 0, "💿 Par albums");
        popup.getMenu().add(0, 2, 1, "👤 Par artistes");
        popup.getMenu().add(0, 3, 2, "🎵 Par titres");
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                Bundle bundle = new Bundle();
                bundle.putString(Constants.ALBUM_NEW_RELEASES, Constants.ALBUM_NEW_RELEASES);
                navigate(anchorView, R.id.action_libraryFragment_to_albumListPageFragment, bundle);
                return true;
            } else if (itemId == 2) {
                Bundle bundle = new Bundle();
                bundle.putString(Constants.ALBUM_RECENTLY_ADDED, Constants.ALBUM_RECENTLY_ADDED);
                navigate(anchorView, R.id.action_libraryFragment_to_albumListPageFragment, bundle);
                return true;
            } else if (itemId == 3) {
                Bundle bundle = new Bundle();
                bundle.putString(Constants.MEDIA_RECENTLY_ADDED, Constants.MEDIA_RECENTLY_ADDED);
                navigate(anchorView, R.id.action_libraryFragment_to_songListPageFragment, bundle);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void generateAiPlaylist() {
        new AiPlaylistDialog().show(getChildFragmentManager(), "AiPlaylistDialog");
    }

    private void navigate(View view, int destinationId, Bundle bundle) {
        try {
            Navigation.findNavController(view).navigate(destinationId, bundle);
        } catch (Exception e) {
            try {
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(destinationId, bundle);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() != null && getActivity() instanceof com.cappielloantonio.tempo.ui.activity.MainActivity) {
            ((com.cappielloantonio.tempo.ui.activity.MainActivity) getActivity()).setBottomNavigationBarVisibility(true);
            ((com.cappielloantonio.tempo.ui.activity.MainActivity) getActivity()).setBottomSheetVisibility(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }
}
