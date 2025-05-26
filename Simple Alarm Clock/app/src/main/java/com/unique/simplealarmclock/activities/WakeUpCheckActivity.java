package com.unique.simplealarmclock.activities;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.unique.simplealarmclock.R;
import com.unique.simplealarmclock.model.Alarm;

public class WakeUpCheckActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "WakeUpCheckActivity";
    private static final float SHAKE_THRESHOLD = 12.0f;
    private static final int MIN_SHAKES = 5;
    private static final int CHECK_DURATION = 30000; // 30 seconds to complete the check

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private PowerManager.WakeLock wakeLock;
    private Vibrator vibrator;
    private TextView shakeCountText;
    private TextView timerText;
    private Handler handler;
    private int shakeCount = 0;
    private long lastShakeTime = 0;
    private boolean isChecking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wake_up_check);

        // Keep screen on and show when locked
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        // Initialize sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize views
        shakeCountText = findViewById(R.id.shake_count_text);
        timerText = findViewById(R.id.timer_text);

        // Initialize wake lock
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SimpleAlarmClock:WakeUpCheckLock"
        );
        wakeLock.acquire(CHECK_DURATION);

        handler = new Handler();
        startWakeUpCheck();
    }

    private void startWakeUpCheck() {
        isChecking = true;
        shakeCount = 0;
        updateShakeCountText();

        // Register sensor listener
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "No accelerometer found on device", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Start countdown timer
        handler.postDelayed(this::timeUp, CHECK_DURATION);
        updateTimer(CHECK_DURATION);
    }

    private void updateShakeCountText() {
        String text = String.format("Shakes: %d/%d", shakeCount, MIN_SHAKES);
        shakeCountText.setText(text);
    }

    private void updateTimer(long remainingTime) {
        String text = String.format("Time remaining: %d seconds", remainingTime / 1000);
        timerText.setText(text);

        if (remainingTime > 1000) {
            handler.postDelayed(() -> updateTimer(remainingTime - 1000), 1000);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isChecking || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

        long currentTime = System.currentTimeMillis();
        if (acceleration > SHAKE_THRESHOLD) {
            if (currentTime - lastShakeTime > 500) { // Prevent counting rapid shakes
                shakeCount++;
                lastShakeTime = currentTime;
                vibrate();
                updateShakeCountText();

                if (shakeCount >= MIN_SHAKES) {
                    wakeUpCheckCompleted();
                }
            }
        }
    }

    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }
    }

    private void wakeUpCheckCompleted() {
        isChecking = false;
        Toast.makeText(this, "Wake-up check completed!", Toast.LENGTH_SHORT).show();
        handler.removeCallbacksAndMessages(null);
        cleanup();
        finish();
    }

    private void timeUp() {
        if (isChecking) {
            isChecking = false;
            Toast.makeText(this, "Wake-up check failed! Please stay awake.", Toast.LENGTH_LONG).show();
            cleanup();
            finish();
        }
    }

    private void cleanup() {
        sensorManager.unregisterListener(this);
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBackPressed() {
        // Prevent back button from closing the activity
        Toast.makeText(this, "Please complete the wake-up check", Toast.LENGTH_SHORT).show();
    }
} 