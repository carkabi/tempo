package com.cappielloantonio.tempo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.cappielloantonio.tempo.github.Github;
import com.cappielloantonio.tempo.helper.PeachNotificationHelper;
import com.cappielloantonio.tempo.helper.ThemeHelper;
import com.cappielloantonio.tempo.subsonic.Subsonic;
import com.cappielloantonio.tempo.subsonic.SubsonicPreferences;
import com.cappielloantonio.tempo.util.Preferences;
import com.cappielloantonio.tempo.worker.PeachUpdateWorker;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class App extends Application {
    private static App instance;
    private static Context context;
    private static Subsonic subsonic;
    private static Github github;
    private static SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String themePref = sharedPreferences.getString(Preferences.THEME, ThemeHelper.DEFAULT_MODE);
        ThemeHelper.applyTheme(themePref);

        instance = this;
        context = getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if ("peach".equals(BuildConfig.FLAVOR)) {
            PeachNotificationHelper.createNotificationChannels(this);
            schedulePeachUpdateWorker();
        }
    }

    private void schedulePeachUpdateWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(PeachUpdateWorker.class, 6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PeachUpdateWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }

    public static App getInstance() {
        if (instance == null) {
            instance = new App();
        }

        return instance;
    }

    public static Context getContext() {
        if (context == null) {
            context = getInstance();
        }

        return context;
    }

    public static Subsonic getSubsonicClientInstance(boolean override) {
        if (subsonic == null || override) {
            subsonic = getSubsonicClient();
        }
        return subsonic;
    }

    public static Github getGithubClientInstance() {
        if (github == null) {
            github = new Github();
        }
        return github;
    }

    public SharedPreferences getPreferences() {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
        }

        return preferences;
    }

    public static void refreshSubsonicClient() {
        subsonic = getSubsonicClient();
    }

    private static Subsonic getSubsonicClient() {
        SubsonicPreferences preferences = getSubsonicPreferences();

        if (preferences.getAuthentication() != null) {
            if (preferences.getAuthentication().getPassword() != null)
                Preferences.setPassword(preferences.getAuthentication().getPassword());
            if (preferences.getAuthentication().getToken() != null)
                Preferences.setToken(preferences.getAuthentication().getToken());
            if (preferences.getAuthentication().getSalt() != null)
                Preferences.setSalt(preferences.getAuthentication().getSalt());
        }

        return new Subsonic(preferences);
    }

    @NonNull
    private static SubsonicPreferences getSubsonicPreferences() {
        String server = Preferences.getInUseServerAddress();
        String username = Preferences.getUser();
        String password = Preferences.getPassword();
        String token = Preferences.getToken();
        String salt = Preferences.getSalt();
        boolean isLowSecurity = Preferences.isLowScurity();

        SubsonicPreferences preferences = new SubsonicPreferences();
        preferences.setServerUrl(server);
        preferences.setUsername(username);
        preferences.setAuthentication(password, token, salt, isLowSecurity);

        return preferences;
    }
}
