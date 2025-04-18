package com.example.myapp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Utility class for managing app theme settings.
 */
public class ThemeUtils {
    private static final String TAG = "ThemeUtils";
    private static final String PREFERENCES_NAME = "theme_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    /**
     * Initializes the app theme based on saved preferences.
     * Call this method in your Application class or main activity.
     */
    public static void initTheme(Context context) {
        try {
            boolean isDarkMode = getDarkModePreference(context);
            if (isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing theme: " + e.getMessage());
            // Fall back to system default
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    /**
     * Checks if dark mode is currently active.
     */
    public static boolean isDarkModeActive(Context context) {
        try {
            int currentNightMode = context.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
        } catch (Exception e) {
            Log.e(TAG, "Error checking dark mode: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the saved dark mode preference.
     * If no preference is saved, it returns the system default.
     */
    public static boolean getDarkModePreference(Context context) {
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error getting dark mode preference: " + e.getMessage());
            return false;
        }
    }

    /**
     * Saves the dark mode preference.
     */
    public static void saveDarkModePreference(Context context, boolean isDarkMode) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(
                    PREFERENCES_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_DARK_MODE, isDarkMode);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving dark mode preference: " + e.getMessage());
        }
    }

    /**
     * Toggles between light and dark themes.
     * This version includes error handling and prevents extra animations.
     */
    public static void toggleDarkMode(Context context) {
        try {
            boolean currentMode = isDarkModeActive(context);
            boolean newMode = !currentMode;

            Log.d(TAG, "Toggling theme from " + (currentMode ? "dark" : "light")
                    + " to " + (newMode ? "dark" : "light"));

            // Save new preference
            saveDarkModePreference(context, newMode);

            // Apply theme change with a slight delay to ensure UI operations complete
            new Handler(Looper.getMainLooper()).post(() -> {
                if (newMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error toggling dark mode: " + e.getMessage());
        }
    }
}