package com.cappielloantonio.tempo.ui.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.cappielloantonio.tempo.BuildConfig;
import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.helper.ThemeHelper;
import com.cappielloantonio.tempo.interfaces.DialogClickCallback;
import com.cappielloantonio.tempo.interfaces.ScanCallback;
import com.cappielloantonio.tempo.repository.peach.PeachRepository;
import com.cappielloantonio.tempo.repository.peach.models.PeachBootstrapResponse;
import com.cappielloantonio.tempo.repository.peach.models.PeachUpdate;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.ui.dialog.DeleteDownloadStorageDialog;
import com.cappielloantonio.tempo.ui.dialog.DownloadStorageDialog;
import com.cappielloantonio.tempo.ui.dialog.PeachUpdateDialog;
import com.cappielloantonio.tempo.ui.dialog.StarredSyncDialog;
import com.cappielloantonio.tempo.ui.dialog.StreamingCacheStorageDialog;
import com.cappielloantonio.tempo.util.DownloadUtil;
import com.cappielloantonio.tempo.util.Preferences;
import com.cappielloantonio.tempo.util.UIUtil;
import com.cappielloantonio.tempo.viewmodel.SettingViewModel;

import java.util.Locale;
import java.util.Map;

@OptIn(markerClass = UnstableApi.class)
public class SettingsFragment extends PreferenceFragmentCompat {
    private MainActivity activity;
    private SettingViewModel settingViewModel;
    private ActivityResultLauncher<Intent> someActivityResultLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        someActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {});
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (!isGranted && getContext() != null) {
                Toast.makeText(getContext(), "Permission de notification refusée", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        View view = super.onCreateView(inflater, container, savedInstanceState);
        settingViewModel = new ViewModelProvider(requireActivity()).get(SettingViewModel.class);
        if (view != null && getListView() != null) {
            getListView().setPadding(0, 0, 0, (int) getResources().getDimension(R.dimen.global_padding_bottom));
        }
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        activity.setBottomNavigationBarVisibility(false);
        activity.setBottomSheetVisibility(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkEqualizer();
        checkCacheStorage();
        checkStorage();
        setStreamingCacheSize();
        setAppLanguage();
        setVersion();
        setupPeachPreferences();
        actionLogout();
        actionScan();
        actionSyncStarredTracks();
        actionChangeStreamingCacheStorage();
        actionChangeDownloadStorage();
        actionDeleteDownloadStorage();
        actionKeepScreenOn();
    }

    @Override
    public void onStop() {
        super.onStop();
        activity.setBottomSheetVisibility(true);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        String prefXml = getArguments() != null ? getArguments().getString("pref_xml") : null;
        if ("pref_general".equals(prefXml)) { setPreferencesFromResource(R.xml.pref_general, null); }
        else if ("pref_audio".equals(prefXml)) { setPreferencesFromResource(R.xml.pref_audio, null); }
        else if ("pref_storage".equals(prefXml)) { setPreferencesFromResource(R.xml.pref_storage, null); }
        else if ("pref_interface".equals(prefXml)) { setPreferencesFromResource(R.xml.pref_interface, null); }
        else if ("pref_connectivity".equals(prefXml)) { setPreferencesFromResource(R.xml.pref_connectivity, null); }
        else { setPreferencesFromResource(R.xml.global_preferences, rootKey); }

        ListPreference themePreference = findPreference(Preferences.THEME);
        if (themePreference != null) {
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                ThemeHelper.applyTheme((String) newValue);
                if (getActivity() != null) {
                    getActivity().recreate();
                }
                return true;
            });
        }
    }

    private void setupPeachPreferences() {
        SwitchPreference notifPref = findPreference("peach_notifications_enabled");
        Preference checkUpdatePref = findPreference("check_peach_update");

        if ("peach".equals(BuildConfig.FLAVOR)) {
            if (notifPref != null) {
                notifPref.setVisible(true);
                notifPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    Preferences.setPeachNotificationEnabled(enabled);
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        }
                    }
                    return true;
                });
            }

            if (checkUpdatePref != null) {
                checkUpdatePref.setVisible(true);
                checkUpdatePref.setOnPreferenceClickListener(preference -> {
                    Toast.makeText(getContext(), "Recherche de mise à jour...", Toast.LENGTH_SHORT).show();
                    new PeachRepository().bootstrap(new PeachRepository.PeachCallback() {
                        @Override
                        public void onSuccess(PeachBootstrapResponse response) {
                            if (getContext() == null) return;
                            PeachUpdate update = response.getUpdate();
                            if (response.getApplication() != null && BuildConfig.APPLICATION_ID.equals(response.getApplication().getAndroidApplicationId())) {
                                if (update != null && update.isAvailable() && update.getVersionCode() > BuildConfig.VERSION_CODE) {
                                    new PeachUpdateDialog(update).show(getParentFragmentManager(), "PeachUpdateDialog");
                                    return;
                                }
                            }
                            Toast.makeText(getContext(), "Votre application est à jour.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(int code, String message) {
                            if (getContext() == null) return;
                            Toast.makeText(getContext(), "Erreur : " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                    return true;
                });
            }
        } else {
            if (notifPref != null) notifPref.setVisible(false);
            if (checkUpdatePref != null) checkUpdatePref.setVisible(false);
        }
    }

    private void checkEqualizer() {
        Preference equalizer = findPreference("equalizer");
        if (equalizer == null) return;
        Intent intent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        if ((intent.resolveActivity(requireActivity().getPackageManager()) != null)) {
            equalizer.setOnPreferenceClickListener(preference -> { someActivityResultLauncher.launch(intent); return true; });
        } else { equalizer.setVisible(false); }
    }

    private void checkCacheStorage() {
        Preference storage = findPreference("streaming_cache_storage");
        if (storage == null) return;
        try {
            if (requireContext().getExternalFilesDirs(null).length > 1 && requireContext().getExternalFilesDirs(null)[1] != null) {
                storage.setSummary(Preferences.getDownloadStoragePreference() == 0 ? R.string.download_storage_internal_dialog_negative_button : R.string.download_storage_external_dialog_positive_button);
            } else {
                storage.setVisible(false);
            }
        } catch (Exception e) { storage.setVisible(false); }
    }

    private void checkStorage() {
        Preference storage = findPreference("download_storage");
        if (storage == null) return;
        try {
            if (requireContext().getExternalFilesDirs(null).length > 1 && requireContext().getExternalFilesDirs(null)[1] != null) {
                storage.setSummary(Preferences.getDownloadStoragePreference() == 0 ? R.string.download_storage_internal_dialog_negative_button : R.string.download_storage_external_dialog_positive_button);
            } else {
                storage.setVisible(false);
            }
        } catch (Exception e) { storage.setVisible(false); }
    }

    private void setStreamingCacheSize() {
        ListPreference pref = findPreference("streaming_cache_size");
        if (pref != null) {
            pref.setSummaryProvider(preference -> {
                CharSequence entry = ((ListPreference)preference).getEntry();
                if (entry == null) return null;
                long sizeMb = DownloadUtil.getStreamingCacheSize(requireActivity()) / (1024 * 1024);
                return getString(R.string.settings_summary_streaming_cache_size, entry, String.valueOf(sizeMb));
            });
        }
    }

    private void setAppLanguage() {
        ListPreference localePref = findPreference("language");
        if (localePref == null) return;
        Map<String, String> locales = UIUtil.getLangPreferenceDropdownEntries(requireContext());
        if (locales == null || locales.isEmpty()) return;
        CharSequence[] entries = locales.keySet().toArray(new CharSequence[0]);
        CharSequence[] entryValues = locales.values().toArray(new CharSequence[0]);
        localePref.setEntries(entries);
        localePref.setEntryValues(entryValues);
        if (localePref.getValue() == null) localePref.setValueIndex(0);
        localePref.setSummary(Locale.forLanguageTag(localePref.getValue()).getDisplayLanguage());
        localePref.setOnPreferenceChangeListener((preference, newValue) -> {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags((String) newValue));
            return true;
        });
    }

    private void setVersion() {
        Preference pref = findPreference("version");
        if (pref != null) {
            pref.setSummary(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        }
    }

    private void actionLogout() {
        Preference pref = findPreference("logout");
        if (pref != null) pref.setOnPreferenceClickListener(p -> { activity.quit(); return true; });
    }

    private void actionScan() {
        Preference pref = findPreference("scan_library");
        if (pref == null) return;
        pref.setOnPreferenceClickListener(p -> {
            settingViewModel.launchScan(new ScanCallback() {
                @Override public void onError(Exception e) { pref.setSummary(e.getMessage()); }
                @Override public void onSuccess(boolean isScanning, long count) {
                    pref.setSummary("Scanning: counting " + count + " tracks");
                    if (isScanning) getScanStatus();
                }
            });
            return true;
        });
    }

    private void actionSyncStarredTracks() {
        Preference pref = findPreference("sync_starred_tracks_for_offline_use");
        if (pref != null) {
            pref.setOnPreferenceChangeListener((p, newValue) -> {
                if (newValue instanceof Boolean && (Boolean) newValue) { new StarredSyncDialog().show(activity.getSupportFragmentManager(), null); }
                return true;
            });
        }
    }

    private void actionChangeStreamingCacheStorage() {
        Preference pref = findPreference("streaming_cache_storage");
        if (pref != null) {
            pref.setOnPreferenceClickListener(p -> {
                new StreamingCacheStorageDialog(new DialogClickCallback() {
                    @Override public void onPositiveClick() { pref.setSummary(R.string.streaming_cache_storage_external_dialog_positive_button); }
                    @Override public void onNegativeClick() { pref.setSummary(R.string.streaming_cache_storage_internal_dialog_negative_button); }
                }).show(activity.getSupportFragmentManager(), null);
                return true;
            });
        }
    }

    private void actionChangeDownloadStorage() {
        Preference pref = findPreference("download_storage");
        if (pref != null) {
            pref.setOnPreferenceClickListener(p -> {
                new DownloadStorageDialog(new DialogClickCallback() {
                    @Override public void onPositiveClick() { pref.setSummary(R.string.download_storage_external_dialog_positive_button); }
                    @Override public void onNegativeClick() { pref.setSummary(R.string.download_storage_internal_dialog_negative_button); }
                }).show(activity.getSupportFragmentManager(), null);
                return true;
            });
        }
    }

    private void actionDeleteDownloadStorage() {
        Preference pref = findPreference("delete_download_storage");
        if (pref != null) pref.setOnPreferenceClickListener(p -> { new DeleteDownloadStorageDialog().show(activity.getSupportFragmentManager(), null); return true; });
    }

    private void getScanStatus() {
        Preference pref = findPreference("scan_library");
        if (pref == null) return;
        settingViewModel.getScanStatus(new ScanCallback() {
            @Override public void onError(Exception e) { pref.setSummary(e.getMessage()); }
            @Override public void onSuccess(boolean isScanning, long count) {
                pref.setSummary("Scanning: counting " + count + " tracks");
                if (isScanning) getScanStatus();
            }
        });
    }

    private void actionKeepScreenOn() {
        Preference pref = findPreference("always_on_display");
        if (pref != null) {
            pref.setOnPreferenceChangeListener((p, newValue) -> {
                if (newValue instanceof Boolean) {
                    if ((Boolean) newValue) activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    else activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                return true;
            });
        }
    }
}
