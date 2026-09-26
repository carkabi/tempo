package com.cappielloantonio.tempo.helper;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.cappielloantonio.tempo.R;

public class ThemeHelper {

    public static final String DARK_PEACH = "dark_peach";
    public static final String DARK_TROPICAL = "dark_tropical";
    public static final String NIGHT_NEON = "night_neon";
    public static final String SUNSET = "sunset";
    public static final String OCEAN = "ocean";
    public static final String LIGHT_PEACH = "light_peach";
    public static final String LIGHT_TROPICAL = "light_tropical";
    public static final String DEFAULT_MODE = DARK_PEACH;

    public static void applyTheme(@NonNull String themePref) {
        switch (themePref) {
            case LIGHT_PEACH:
            case LIGHT_TROPICAL:
            case "light":
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO
                );
                break;

            case DARK_PEACH:
            case DARK_TROPICAL:
            case NIGHT_NEON:
            case SUNSET:
            case OCEAN:
            case "dark":
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_YES
                );
                break;

            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    );
                } else {
                    AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                    );
                }
                break;
        }
    }

    public static void applyActivityTheme(
            Activity activity,
            String themePref
    ) {
        if (activity == null) return;
        if (themePref == null) themePref = DEFAULT_MODE;

        switch (themePref) {
            case DARK_TROPICAL:
                activity.setTheme(R.style.AppTheme_DarkTropical);
                break;
            case NIGHT_NEON:
                activity.setTheme(R.style.AppTheme_NightNeon);
                break;
            case SUNSET:
                activity.setTheme(R.style.AppTheme_Sunset);
                break;
            case OCEAN:
                activity.setTheme(R.style.AppTheme_Ocean);
                break;
            case LIGHT_PEACH:
                activity.setTheme(R.style.AppTheme_LightPeach);
                break;
            case LIGHT_TROPICAL:
                activity.setTheme(R.style.AppTheme_LightTropical);
                break;
            case DARK_PEACH:
            case "dark":
            default:
                activity.setTheme(R.style.AppTheme_DarkPeach);
                break;
        }
    }
}
