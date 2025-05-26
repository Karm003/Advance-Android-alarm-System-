package com.unique.simplealarmclock;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

public class App extends Application {
    private static final String TAG = "App";
    public static final String CHANNEL_ID = "ALARM_SERVICE_CHANNEL";
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        try {
            super.onCreate();
            Log.d(TAG, "onCreate started");

            createNotificationChannel();

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean dn = sharedPreferences.getBoolean(getString(R.string.dayNightTheme), true);
            Log.d(TAG, "Theme setting: " + (dn ? "Day" : "Night"));
            
            if (dn) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            Log.d(TAG, "Theme applied successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            // Set default theme in case of error
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Get the notification manager
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager == null) {
                    Log.e(TAG, "NotificationManager is null");
                    return;
                }
                
                // Check if channel already exists - if it does, delete it to update settings
                if (manager.getNotificationChannel(CHANNEL_ID) != null) {
                    manager.deleteNotificationChannel(CHANNEL_ID);
                    Log.d(TAG, "Deleted existing notification channel to update settings");
                }
                
                // Create the alarm notification channel with highest importance
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.app_name) + " Alarm",
                        NotificationManager.IMPORTANCE_HIGH
                );
                
                // Configure channel for maximum visibility and non-dismissible behavior
                serviceChannel.setDescription("Channel for critical alarm notifications that cannot be dismissed");
                serviceChannel.setSound(null, null); // Sound is handled separately by MediaPlayer
                serviceChannel.enableLights(true);
                serviceChannel.enableVibration(true);
                serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                serviceChannel.setBypassDnd(true); // Bypass Do Not Disturb mode
                serviceChannel.setShowBadge(true);
                
                // Create the channel
                manager.createNotificationChannel(serviceChannel);
                Log.d(TAG, "Notification channel created successfully with non-dismissible settings");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channel", e);
        }
    }
}
