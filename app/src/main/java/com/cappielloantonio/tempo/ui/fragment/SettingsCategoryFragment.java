package com.cappielloantonio.tempo.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;
import androidx.navigation.Navigation;

import com.cappielloantonio.tempo.BuildConfig;
import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.FragmentSettingsCategoriesBinding;
import com.cappielloantonio.tempo.repository.peach.PeachRepository;
import com.cappielloantonio.tempo.repository.peach.models.PeachBootstrapResponse;
import com.cappielloantonio.tempo.repository.peach.models.PeachNews;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.util.Preferences;

@UnstableApi
public class SettingsCategoryFragment extends Fragment {

    private FragmentSettingsCategoriesBinding bind;
    private MainActivity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        bind = FragmentSettingsCategoriesBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View newsContainer = view.findViewById(R.id.settings_news_container);
        View newsButton = view.findViewById(R.id.settings_news_button);
        View contactContainer = view.findViewById(R.id.settings_contact_container);
        View contactButton = view.findViewById(R.id.settings_contact_button);
        View supportContainer = view.findViewById(R.id.settings_support_container);
        View supportButton = view.findViewById(R.id.settings_support_button);

        if ("peach".equals(BuildConfig.FLAVOR)) {
            if (newsContainer != null) newsContainer.setVisibility(View.VISIBLE);
            if (newsButton != null) {
                newsButton.setOnClickListener(v ->
                        Navigation.findNavController(v).navigate(R.id.action_settingsCategoryFragment_to_newsFragment)
                );
            }

            if (contactContainer != null) contactContainer.setVisibility(View.VISIBLE);
            if (contactButton != null) {
                contactButton.setOnClickListener(v ->
                        Navigation.findNavController(v).navigate(R.id.action_settingsCategoryFragment_to_contactFragment)
                );
            }

            if (supportContainer != null) supportContainer.setVisibility(View.VISIBLE);
            if (supportButton != null) {
                supportButton.setOnClickListener(v ->
                        Navigation.findNavController(v).navigate(R.id.action_settingsCategoryFragment_to_peachSupportFragment)
                );
            }
        } else {
            if (newsContainer != null) newsContainer.setVisibility(View.GONE);
            if (contactContainer != null) contactContainer.setVisibility(View.GONE);
            if (supportContainer != null) supportContainer.setVisibility(View.GONE);
        }

        bind.settingsGeneralButton.setOnClickListener(v -> navigateToSettings(v, "pref_general"));
        bind.settingsAudioButton.setOnClickListener(v -> navigateToSettings(v, "pref_audio"));
        bind.settingsDataButton.setOnClickListener(v -> navigateToSettings(v, "pref_storage"));
        bind.settingsUiButton.setOnClickListener(v -> navigateToSettings(v, "pref_interface"));
        bind.settingsConnectivityButton.setOnClickListener(v -> navigateToSettings(v, "pref_connectivity"));

        bind.settingsLogoutButton.setOnClickListener(v -> {
            if (activity != null) activity.quit();
        });
    }

    private void checkUnreadNews() {
        if (!"peach".equals(BuildConfig.FLAVOR) || bind == null) return;

        View badgeDot = bind.getRoot().findViewById(R.id.news_badge_dot);

        new PeachRepository().bootstrap(new PeachRepository.PeachCallback() {
            @Override
            public void onSuccess(PeachBootstrapResponse response) {
                if (bind == null || badgeDot == null) return;
                if (response.getNews() != null && !response.getNews().isEmpty()) {
                    int maxId = 0;
                    for (PeachNews news : response.getNews()) {
                        if (news.getId() > maxId) {
                            maxId = news.getId();
                        }
                    }
                    if (maxId > Preferences.getLastSeenNewsId()) {
                        badgeDot.setVisibility(View.VISIBLE);
                    } else {
                        badgeDot.setVisibility(View.GONE);
                    }
                } else {
                    badgeDot.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(int code, String message) {
                if (badgeDot != null) {
                    badgeDot.setVisibility(View.GONE);
                }
            }
        });
    }

    private void navigateToSettings(View v, String prefXml) {
        Bundle bundle = new Bundle();
        bundle.putString("pref_xml", prefXml);
        Navigation.findNavController(v).navigate(R.id.action_settingsCategoryFragment_to_settingsFragment, bundle);
    }

    @Override
    public void onStart() {
        super.onStart();
        activity.setBottomNavigationBarVisibility(true);
        activity.setBottomSheetVisibility(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkUnreadNews();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }
}
