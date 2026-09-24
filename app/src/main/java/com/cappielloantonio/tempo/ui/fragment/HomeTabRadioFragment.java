package com.cappielloantonio.tempo.ui.fragment;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaBrowser;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cappielloantonio.tempo.BuildConfig;
import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.FragmentHomeTabRadioBinding;
import com.cappielloantonio.tempo.interfaces.ClickCallback;
import com.cappielloantonio.tempo.interfaces.RadioCallback;
import com.cappielloantonio.tempo.repository.peach.models.PeachRadioStation;
import com.cappielloantonio.tempo.repository.peach.models.RadioProgramItem;
import com.cappielloantonio.tempo.service.MediaManager;
import com.cappielloantonio.tempo.service.MediaService;
import com.cappielloantonio.tempo.service.PeachRadioPlayerManager;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.ui.adapter.InternetRadioStationAdapter;
import com.cappielloantonio.tempo.ui.adapter.PeachRadioAdapter;
import com.cappielloantonio.tempo.ui.dialog.RadioEditorDialog;
import com.cappielloantonio.tempo.util.Constants;
import com.cappielloantonio.tempo.util.Preferences;
import com.cappielloantonio.tempo.viewmodel.RadioViewModel;
import com.google.common.util.concurrent.ListenableFuture;

@UnstableApi
public class HomeTabRadioFragment extends Fragment implements ClickCallback, RadioCallback, PeachRadioAdapter.OnPeachRadioClickListener {
    private static final String TAG = "PEACH_RADIO";
    private static final String NAV_TAG = "PEACH_NAV";

    private FragmentHomeTabRadioBinding bind;
    private MainActivity activity;
    private RadioViewModel radioViewModel;

    private InternetRadioStationAdapter internetRadioStationAdapter;
    private PeachRadioAdapter peachRadioAdapter;

    private ListenableFuture<MediaBrowser> mediaBrowserListenableFuture;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(NAV_TAG, "HomeTabRadioFragment.onCreateView()");
        activity = (MainActivity) getActivity();

        bind = FragmentHomeTabRadioBinding.inflate(inflater, container, false);
        View view = bind.getRoot();
        radioViewModel = new ViewModelProvider(requireActivity()).get(RadioViewModel.class);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(NAV_TAG, "HomeTabRadioFragment.onViewCreated()");

        init();
        initRadioStationView();
    }

    @Override
    public void onStart() {
        super.onStart();

        initializeMediaBrowser();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(NAV_TAG, "HomeTabRadioFragment.onResume()");
    }

    @Override
    public void onPause() {
        Log.d(NAV_TAG, "HomeTabRadioFragment.onPause()");
        super.onPause();
    }

    @Override
    public void onStop() {
        releaseMediaBrowser();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.d(NAV_TAG, "HomeTabRadioFragment.onDestroyView()");
        super.onDestroyView();
        bind = null;
    }

    private void init() {
        if ("peach".equals(BuildConfig.FLAVOR)) {
            bind.internetRadioStationPreTextView.setVisibility(View.VISIBLE);
            bind.internetRadioStationPreTextView.setText(
                    R.string.peach_radio_kicker
            );
            bind.internetRadioStationTitleTextView.setText(
                    R.string.peach_radio_title
            );
            bind.internetRadioStationTitleTextView.setOnLongClickListener(null);
        } else {
            bind.internetRadioStationPreTextView.setOnClickListener(v -> {
                RadioEditorDialog dialog = new RadioEditorDialog(this);
                dialog.show(activity.getSupportFragmentManager(), null);
            });

            bind.internetRadioStationTitleTextView.setOnLongClickListener(v -> {
                radioViewModel.getInternetRadioStations(getViewLifecycleOwner());
                return true;
            });
        }

        bind.hideSectionButton.setOnClickListener(v -> Preferences.setRadioSectionHidden());
    }

    private void initRadioStationView() {
        bind.internetRadioStationRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.internetRadioStationRecyclerView.setHasFixedSize(true);

        if ("peach".equals(BuildConfig.FLAVOR)) {
            peachRadioAdapter = new PeachRadioAdapter(this);
            bind.internetRadioStationRecyclerView.setAdapter(peachRadioAdapter);

            radioViewModel.getPeachRadioStations().observe(getViewLifecycleOwner(), stations -> {
                if (bind == null) return;
                Log.i(TAG, "PeachRadioStations observed in fragment, size = " + (stations != null ? stations.size() : 0));
                if (stations == null || stations.isEmpty()) {
                    bind.homeRadioStationSector.setVisibility(View.GONE);
                    bind.emptyRadioStationLayout.setVisibility(View.GONE);
                } else {
                    bind.homeRadioStationSector.setVisibility(View.VISIBLE);
                    bind.emptyRadioStationLayout.setVisibility(View.GONE);
                    peachRadioAdapter.setItems(stations);

                    // Playback starts only after an explicit user action.
                    // This avoids launching a station before Media3 is ready
                    // and prevents unwanted playback when opening the Radio tab.
                }
            });

            PeachRadioPlayerManager.setRadioLiveCallback(new PeachRadioPlayerManager.RadioLiveCallback() {
                @Override
                public void onTrackChanged(PeachRadioStation station, RadioProgramItem currentItem, RadioProgramItem nextItem) {
                    if (peachRadioAdapter != null) {
                        peachRadioAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onLiveResynced(long positionMs) {
                }
            });

        } else {
            internetRadioStationAdapter = new InternetRadioStationAdapter(this);
            bind.internetRadioStationRecyclerView.setAdapter(internetRadioStationAdapter);
            radioViewModel.getInternetRadioStations(getViewLifecycleOwner()).observe(getViewLifecycleOwner(), internetRadioStations -> {
                if (internetRadioStations == null) {
                    if (bind != null) bind.homeRadioStationSector.setVisibility(View.GONE);
                    if (bind != null) bind.emptyRadioStationLayout.setVisibility(View.GONE);
                } else {
                    if (bind != null)
                        bind.homeRadioStationSector.setVisibility(!internetRadioStations.isEmpty() ? View.VISIBLE : View.GONE);
                    if (bind != null)
                        bind.emptyRadioStationLayout.setVisibility(internetRadioStations.isEmpty() ? View.VISIBLE : View.GONE);

                    internetRadioStationAdapter.setItems(internetRadioStations);
                }
            });
        }
    }

    private void initializeMediaBrowser() {
        mediaBrowserListenableFuture = new MediaBrowser.Builder(requireContext(), new SessionToken(requireContext(), new ComponentName(requireContext(), MediaService.class))).buildAsync();
    }

    private void releaseMediaBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture);
    }

    @Override
    public void onPeachRadioClick(PeachRadioStation station) {
        Log.i(TAG, "Step 2: HomeTabRadioFragment.onPeachRadioClick() station = " + (station != null ? station.getSlug() : "null"));
        if (station != null) {
            PeachRadioPlayerManager.startPeachRadio(requireContext(), mediaBrowserListenableFuture, station);
            activity.setBottomSheetInPeek(true);
        }
    }

    @Override
    public void onInternetRadioStationClick(Bundle bundle) {
        MediaManager.startRadio(mediaBrowserListenableFuture, bundle.getParcelable(Constants.INTERNET_RADIO_STATION_OBJECT));
        activity.setBottomSheetInPeek(true);
    }

    @Override
    public void onInternetRadioStationLongClick(Bundle bundle) {
        if (!"peach".equals(BuildConfig.FLAVOR)) {
            RadioEditorDialog dialog = new RadioEditorDialog(new RadioCallback() {
                @Override
                public void onDismiss() {
                    radioViewModel.getInternetRadioStations(getViewLifecycleOwner());
                }
            });
            dialog.setArguments(bundle);
            dialog.show(activity.getSupportFragmentManager(), null);
        }
    }

    @Override
    public void onDismiss() {
        if (!"peach".equals(BuildConfig.FLAVOR)) {
            new Handler().postDelayed(() -> {
                if (radioViewModel != null)
                    radioViewModel.refreshInternetRadioStations(getViewLifecycleOwner());
            }, 1000);
        }
    }
}
