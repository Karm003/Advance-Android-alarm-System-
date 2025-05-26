package com.unique.simplealarmclock.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.unique.simplealarmclock.model.Alarm;
import com.unique.simplealarmclock.R;
import com.unique.simplealarmclock.activities.RingActivity;

import java.io.IOException;

import static com.unique.simplealarmclock.App.CHANNEL_ID;

public class AlarmService extends Service {
    private static final String TAG = "AlarmService";
    private static final int NOTIFICATION_ID = 1;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private AudioManager audioManager;
    private PowerManager.WakeLock wakeLock;
    private Handler handler;
    private Runnable periodicUpdate;
    Alarm alarm;
    Uri ringtone;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize media player with proper settings
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setLooping(true);
        
        // Acquire wake lock to keep CPU running
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK | 
                PowerManager.ACQUIRE_CAUSES_WAKEUP | 
                PowerManager.ON_AFTER_RELEASE,
                "AlarmService::WakeLock");
        wakeLock.acquire(10*60*1000L /*10 minutes*/);
        
        // Initialize audio manager to request audio focus
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        // Set up vibrator
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        // Default ringtone
        ringtone = RingtoneManager.getActualDefaultRingtoneUri(this.getBaseContext(), RingtoneManager.TYPE_ALARM);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if we already have an alarm running to prevent duplicate notifications
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            Log.d(TAG, "Alarm already playing, not creating duplicate");
            return START_STICKY;
        }
        
        Bundle bundle = intent.getBundleExtra(getString(R.string.bundle_alarm_obj));
        if (bundle != null)
            alarm = (Alarm) bundle.getSerializable(getString(R.string.arg_alarm_obj));
            
        // Request audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
                    
            AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setWillPauseWhenDucked(false)
                    .build();
                    
            audioManager.requestAudioFocus(focusRequest);
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN);
        }
        
        // Create intent for notification
        Intent notificationIntent = new Intent(this, RingActivity.class);
        notificationIntent.putExtra(getString(R.string.bundle_alarm_obj), bundle);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                    Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        
        // Using FLAG_IMMUTABLE for security on newer Android versions
        int pendingIntentFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlag = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            pendingIntentFlag = PendingIntent.FLAG_UPDATE_CURRENT;
        }
            
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlag);
        
        String alarmTitle = getString(R.string.alarm_title);
        if (alarm != null) {
            alarmTitle = alarm.getTitle();
            
            try {
                // Check if this is the last snooze (1 snooze remaining)
                if (alarm.getSnoozedCount() > 0 && alarm.getSnoozeLimit() - alarm.getSnoozedCount() == 1) {
                    Log.d(TAG, "Last snooze detected! Playing strange ringtone");
                    // Use the special ringtone from raw resources
                    mediaPlayer.setDataSource(this.getBaseContext(), 
                            Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.strange_ringtone));
                } else {
                    // Use the normal tone set for the alarm
                    mediaPlayer.setDataSource(this.getBaseContext(), Uri.parse(alarm.getTone()));
                }
                
                // Set audio attributes for alarm
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AudioAttributes attributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build();
                    mediaPlayer.setAudioAttributes(attributes);
                } else {
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                }
                
                mediaPlayer.prepareAsync();
            } catch (IOException ex) {
                ex.printStackTrace();
                Log.e(TAG, "Error setting ringtone: " + ex.getMessage());
                
                // Fallback to default ringtone if there's an error
                try {
                    mediaPlayer.setDataSource(this.getBaseContext(), ringtone);
                    mediaPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                mediaPlayer.setDataSource(this.getBaseContext(), ringtone);
                
                // Set audio attributes for alarm
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AudioAttributes attributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build();
                    mediaPlayer.setAudioAttributes(attributes);
                } else {
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                }
                
                mediaPlayer.prepareAsync();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        // Create a notification that cannot be dismissed by the user
        Notification notification = createNonDismissibleNotification(alarmTitle, pendingIntent);
        
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
            }
        });

        // Start vibration pattern for alarm
        if (alarm != null && alarm.isVibrate()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createWaveform(
                        new long[]{0, 400, 1000, 600, 1000, 800}, 
                        new int[]{0, 255, 0, 255, 0, 255}, 
                        0);
                vibrator.vibrate(effect);
            } else {
                long[] pattern = {0, 400, 1000, 600, 1000, 800};
                vibrator.vibrate(pattern, 0);
            }
        }
        
        // Start as a foreground service with a non-dismissible notification
        startForeground(NOTIFICATION_ID, notification);

        return START_STICKY;
    }
    
    /**
     * Updates the notification to keep it from being dismissed
     * This method is no longer called periodically to prevent multiple notifications
     */
    private void updateNotification() {
        if (alarm != null) {
            try {
                // Get the notification manager
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                
                // Create a new notification with updated time
                Intent notificationIntent = new Intent(this, RingActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable(getString(R.string.arg_alarm_obj), alarm);
                notificationIntent.putExtra(getString(R.string.bundle_alarm_obj), bundle);
                
                int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : 
                        PendingIntent.FLAG_UPDATE_CURRENT;
                        
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flag);
                
                // Update the notification
                Notification notification = createNonDismissibleNotification(alarm.getTitle(), pendingIntent);
                
                // Notify with the same ID to replace the existing notification
                notificationManager.notify(NOTIFICATION_ID, notification);
            } catch (Exception e) {
                Log.e(TAG, "Error updating notification", e);
            }
        }
    }
    
    /**
     * Creates a notification that cannot be dismissed by the user
     */
    private Notification createNonDismissibleNotification(String alarmTitle, PendingIntent contentIntent) {
        // Create a style that makes the notification more persistent
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
            .setBigContentTitle("⏰ ALARM ACTIVE ⏰")
            .bigText(alarmTitle + "\nTap to open alarm screen");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ ALARM ACTIVE ⏰")
            .setContentText(alarmTitle)
            .setStyle(bigTextStyle)
            .setSmallIcon(R.drawable.ic_alarm_white_24dp)
            .setSound(null) // Sound is handled by MediaPlayer
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            // These flags make the notification persistent
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setUsesChronometer(true)
            // Ensure it's shown as heads-up notification
            .setVibrate(new long[]{0, 1, 1000, 1});
        
        // Add flags directly to make notification non-clearable
        if (Build.VERSION.SDK_INT >= 31) {
            // For Android 12+, foreground service notification cannot have action buttons
            // The full screen intent will handle interactions
        } else {
            // Add a prominent button to open the alarm screen  
            Intent openIntent = new Intent(this, RingActivity.class);
            openIntent.putExtra("action", "open_alarm");
            if (alarm != null) {
                Bundle bundle = new Bundle();
                bundle.putSerializable(getString(R.string.arg_alarm_obj), alarm);
                openIntent.putExtra(getString(R.string.bundle_alarm_obj), bundle);
            }
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                                Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : 
                    PendingIntent.FLAG_UPDATE_CURRENT;
                    
            PendingIntent openPendingIntent = PendingIntent.getActivity(this, 100, openIntent, flag);
            
            builder.addAction(android.R.drawable.ic_menu_view, "Open Alarm", openPendingIntent);
        }
        
        Notification notification = builder.build();
        
        // Set multiple flags directly on the notification to make it non-dismissible
        notification.flags |= Notification.FLAG_NO_CLEAR |     // Cannot be cleared
                              Notification.FLAG_ONGOING_EVENT | // Ongoing event
                              Notification.FLAG_INSISTENT |    // Insistent alarm
                              Notification.FLAG_FOREGROUND_SERVICE; // Foreground service
        
        return notification;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "AlarmService onDestroy");
        super.onDestroy();
        
        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        // Remove any handlers or callbacks
        if (handler != null && periodicUpdate != null) {
            handler.removeCallbacks(periodicUpdate);
        }
        
        // Release audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(
                    new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).build());
        } else {
            audioManager.abandonAudioFocus(null);
        }
        
        // Stop and release media player
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        
        // Stop vibration
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    // If system kills service, restart it
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "onTaskRemoved: rescheduling alarm service restart");
        
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        
        if (alarm != null) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(getString(R.string.arg_alarm_obj), alarm);
            restartServiceIntent.putExtra(getString(R.string.bundle_alarm_obj), bundle);
        }
        
        PendingIntent restartServicePendingIntent = PendingIntent.getService(
                getApplicationContext(), 
                1, 
                restartServiceIntent, 
                PendingIntent.FLAG_ONE_SHOT | 
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
                
        getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        
        // Schedule service restart in 1 second
        alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000,
                restartServicePendingIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
