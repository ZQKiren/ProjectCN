package com.example.myapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Utility class for managing app theme settings.
 */
public class ThemeUtils {

    private static final String PREFERENCES_NAME = "theme_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    /**
     * Initializes the app theme based on saved preferences.
     * Call this method in your Application class or main activity.
     */
    public static void initTheme(Context context) {
        boolean isDarkMode = getDarkModePreference(context);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    /**
     * Checks if dark mode is currently active.
     */
    public static boolean isDarkModeActive(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Gets the saved dark mode preference.
     * If no preference is saved, it returns the system default.
     */
    public static boolean getDarkModePreference(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);

        // Check if a preference exists
        if (sharedPreferences.contains(KEY_DARK_MODE)) {
            return sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        } else {
            // If no preference exists, check the system default
            int currentNightMode = context.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
        }
    }

    /**
     * Saves the dark mode preference.
     */
    public static void saveDarkModePreference(Context context, boolean isDarkMode) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_DARK_MODE, isDarkMode);
        editor.apply();
    }

    /**
     * Toggles between light and dark themes.
     */
    public static void toggleDarkMode(Context context) {
        boolean currentMode = getDarkModePreference(context);
        boolean newMode = !currentMode;

        saveDarkModePreference(context, newMode);

        if (newMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}