package com.unique.simplealarmclock.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.unique.simplealarmclock.util.DayUtil;
import com.unique.simplealarmclock.R;
import com.unique.simplealarmclock.broadcastreciever.AlarmBroadcastReceiver;
import com.unique.simplealarmclock.activities.WakeUpCheckActivity;

import java.io.Serializable;
import java.util.Calendar;

@Entity(tableName = "alarm_table")
public class Alarm implements Serializable {
    @PrimaryKey
    @NonNull
    private int alarmId;
    private int hour, minute;
    private boolean started, recurring;
    private boolean monday, tuesday, wednesday, thursday, friday, saturday, sunday;
    private String title;
    private String tone;
    private boolean vibrate;
    private int snoozeLimit; // Maximum number of times user can snooze
    private int snoozeInterval; // Snooze duration in minutes
    private int snoozedCount; // Current number of times snoozed
    private boolean wakeUpCheckEnabled; // New field for wake-up check feature
    private String qrCode; // Store custom QR code text
    private boolean requiresQR; // Whether this alarm requires QR code to dismiss

    public Alarm(int alarmId, int hour, int minute, String title, boolean started, boolean recurring, 
                boolean monday, boolean tuesday, boolean wednesday, boolean thursday, boolean friday, 
                boolean saturday, boolean sunday, String tone, boolean vibrate, int snoozeLimit, int snoozeInterval) {
        this.alarmId = alarmId;
        this.hour = hour;
        this.minute = minute;
        this.started = started;
        this.recurring = recurring;
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
        this.sunday = sunday;
        this.title = title;
        this.vibrate = vibrate;
        this.tone = tone;
        this.snoozeLimit = snoozeLimit;
        this.snoozeInterval = snoozeInterval;
        this.snoozedCount = 0;
        this.wakeUpCheckEnabled = false;
        this.qrCode = ""; // Default empty QR code
        this.requiresQR = false; // Default to not requiring QR
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public int getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(int alarmId) {
        this.alarmId = alarmId;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public boolean isMonday() {
        return monday;
    }

    public boolean isTuesday() {
        return tuesday;
    }

    public boolean isWednesday() {
        return wednesday;
    }

    public boolean isThursday() {
        return thursday;
    }

    public boolean isFriday() {
        return friday;
    }

    public boolean isSaturday() {
        return saturday;
    }

    public boolean isSunday() {
        return sunday;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public boolean isVibrate() {
        return vibrate;
    }

    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }

    public int getSnoozeLimit() {
        return snoozeLimit;
    }

    public void setSnoozeLimit(int snoozeLimit) {
        this.snoozeLimit = snoozeLimit;
    }

    public int getSnoozeInterval() {
        return snoozeInterval;
    }

    public void setSnoozeInterval(int snoozeInterval) {
        this.snoozeInterval = snoozeInterval;
    }

    public int getSnoozedCount() {
        return snoozedCount;
    }

    public void setSnoozedCount(int snoozedCount) {
        this.snoozedCount = snoozedCount;
    }

    public boolean canSnooze() {
        return snoozedCount < snoozeLimit;
    }

    public void schedule(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmBroadcastReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(context.getString(R.string.arg_alarm_obj), this);
        intent.putExtra(context.getString(R.string.bundle_alarm_obj), bundle);
        
        // Create a unique ID for each snooze to prevent PendingIntent reuse
        int uniqueId = this.alarmId;
        if (this.snoozedCount > 0) {
            // Use a large multiplier to avoid ID conflicts
            uniqueId = this.alarmId * 100000 + this.snoozedCount;
            Log.d("Alarm", "Creating snooze alarm with ID: " + uniqueId);
        }
        
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, uniqueId, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // if alarm time has already passed, increment day by 1
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
        }

        if (!recurring) {
            String toastText = null;
            try {
                if (snoozedCount > 0) {
                    toastText = String.format("Snooze alarm #%d scheduled for %02d:%02d", 
                        snoozedCount, hour, minute);
                } else {
                    toastText = String.format("One Time Alarm %s scheduled for %s at %02d:%02d", 
                        title, DayUtil.toDay(calendar.get(Calendar.DAY_OF_WEEK)), hour, minute);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show();
            Log.d("Alarm", toastText);

            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    alarmPendingIntent
            );
        } else {
            String toastText = String.format("Recurring Alarm %s scheduled for %s at %02d:%02d", 
                title, getRecurringDaysText(), hour, minute);
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show();
            Log.d("Alarm", toastText);

            final long RUN_DAILY = 24 * 60 * 60 * 1000;
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    RUN_DAILY,
                    alarmPendingIntent
            );
        }

        this.started = true;
    }

    public void cancelAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmBroadcastReceiver.class);
        Intent wakeUpIntent = new Intent(context, WakeUpCheckActivity.class);
        
        // Cancel the main alarm
        PendingIntent originalAlarmIntent = PendingIntent.getBroadcast(context, alarmId, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(originalAlarmIntent);
        
        // Cancel any snooze alarms
        for (int i = 1; i <= snoozeLimit; i++) {
            int snoozeId = alarmId * 100000 + i;
            PendingIntent snoozeIntent = PendingIntent.getBroadcast(context, snoozeId, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.cancel(snoozeIntent);
            Log.d("Alarm", "Cancelling snooze alarm with ID: " + snoozeId);
        }

        // Cancel wake-up check if it exists
        int wakeUpCheckId = alarmId * 100000 + 99999;
        PendingIntent wakeUpCheckIntent = PendingIntent.getActivity(context, wakeUpCheckId, wakeUpIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(wakeUpCheckIntent);
        
        this.started = false;
        String toastText;
        if (snoozedCount > 0) {
            toastText = String.format("Snooze alarm #%d cancelled", snoozedCount);
        } else {
            toastText = String.format("Alarm cancelled for %02d:%02d", hour, minute);
        }
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show();
        Log.d("Alarm", toastText);
    }

    public String getRecurringDaysText() {
        if (!recurring) {
            return null;
        }

        String days = "";
        if (monday) {
            days += "Mo ";
        }
        if (tuesday) {
            days += "Tu ";
        }
        if (wednesday) {
            days += "We ";
        }
        if (thursday) {
            days += "Th ";
        }
        if (friday) {
            days += "Fr ";
        }
        if (saturday) {
            days += "Sa ";
        }
        if (sunday) {
            days += "Su ";
        }

        return days;
    }

    public String getTitle() {
        return title;
    }

    public boolean isWakeUpCheckEnabled() {
        return wakeUpCheckEnabled;
    }

    public void setWakeUpCheckEnabled(boolean wakeUpCheckEnabled) {
        this.wakeUpCheckEnabled = wakeUpCheckEnabled;
    }

    public void scheduleWakeUpCheck(Context context) {
        if (!wakeUpCheckEnabled) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WakeUpCheckActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(context.getString(R.string.arg_alarm_obj), this);
        intent.putExtra(context.getString(R.string.bundle_alarm_obj), bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Create unique ID for wake-up check
        int wakeUpCheckId = alarmId * 100000 + 99999; // Use a unique number pattern
        
        PendingIntent wakeUpCheckIntent = PendingIntent.getActivity(context, wakeUpCheckId, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT);

        // Schedule for 5 minutes after current time
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MINUTE, 5);

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.getTimeInMillis(),
            wakeUpCheckIntent
        );

        Log.d("Alarm", "Wake-up check scheduled for " + calendar.get(Calendar.HOUR_OF_DAY) + 
                      ":" + calendar.get(Calendar.MINUTE));
    }

    // Add getters and setters for QR code fields
    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public boolean isRequiresQR() {
        return requiresQR;
    }

    public void setRequiresQR(boolean requiresQR) {
        this.requiresQR = requiresQR;
    }
}
