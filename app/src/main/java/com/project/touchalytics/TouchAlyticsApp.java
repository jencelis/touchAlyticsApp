package com.project.touchalytics;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class TouchAlyticsApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Force light mode for entire app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}