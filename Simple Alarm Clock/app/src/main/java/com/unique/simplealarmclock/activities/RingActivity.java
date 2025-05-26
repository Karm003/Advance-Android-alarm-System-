package com.unique.simplealarmclock.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import com.unique.simplealarmclock.R;
import com.unique.simplealarmclock.databinding.ActivityRingBinding;
import com.unique.simplealarmclock.model.Alarm;
import com.unique.simplealarmclock.service.AlarmService;
import com.unique.simplealarmclock.viewmodel.AlarmListViewModel;

import java.util.Calendar;

public class RingActivity extends AppCompatActivity {
    private static final String TAG = "RingActivity";
    private ActivityRingBinding ringActivityViewBinding;
    private AlarmListViewModel alarmsListViewModel;
    private Alarm alarm;
    private LinearLayout puzzleOptionsContainer;
    private TextView snoozeInfoText;
    private Button snoozeButton;
    private static final int PUZZLE_REQUEST_CODE = 100;
    private static final int PUZZLE_REQUEST_CODE_SNOOZE = 101;
    private boolean isPuzzleSolved = false;
    private static final int REQUEST_CODE_MEMORY_PUZZLE = 1;
    private static final int REQUEST_CODE_MATH_PUZZLE = 2;
    private static final int REQUEST_CODE_PATTERN_PUZZLE = 3;
    private static final int REQUEST_CODE_QR_SCANNER = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        try {
            ringActivityViewBinding = ActivityRingBinding.inflate(getLayoutInflater());
            setContentView(ringActivityViewBinding.getRoot());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
            } else {
                getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                );
            }

            alarmsListViewModel = ViewModelProviders.of(this).get(AlarmListViewModel.class);
            Bundle bundle = getIntent().getBundleExtra(getString(R.string.bundle_alarm_obj));
            if (bundle != null) {
                alarm = (Alarm) bundle.getSerializable(getString(R.string.arg_alarm_obj));
                Log.d(TAG, "Alarm received from bundle: " + (alarm != null ? "yes" : "no"));
                if (alarm != null) {
                    Log.d(TAG, "Alarm details - Hour: " + alarm.getHour() + 
                          ", Minute: " + alarm.getMinute() + 
                          ", Snooze count: " + alarm.getSnoozedCount() +
                          ", Snooze limit: " + alarm.getSnoozeLimit());
                }
            } else {
                Log.e(TAG, "No bundle received in intent");
            }

            initializeViews();
            setupClickListeners();
            updateSnoozeInfo();
            animateClock();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: ", e);
            Toast.makeText(this, "Error starting alarm screen", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        Log.d(TAG, "initializeViews started");
        try {
            puzzleOptionsContainer = findViewById(R.id.puzzle_options_container);
            snoozeInfoText = findViewById(R.id.snooze_info_text);
            snoozeButton = findViewById(R.id.snooze_button);

            Log.d(TAG, "Views found - Container: " + (puzzleOptionsContainer != null) + 
                      ", InfoText: " + (snoozeInfoText != null) + 
                      ", Button: " + (snoozeButton != null));

            if (puzzleOptionsContainer == null || snoozeInfoText == null || snoozeButton == null) {
                Log.e(TAG, "One or more views not found in layout");
                Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Update UI to show puzzle requirement
            TextView instructionText = findViewById(R.id.instruction_text);
            if (instructionText != null) {
                instructionText.setText("Solve a puzzle to dismiss or snooze the alarm");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in initializeViews: ", e);
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupClickListeners() {
        Log.d(TAG, "setupClickListeners started");
        try {
            View mathPuzzleButton = findViewById(R.id.math_puzzle_button);
            View memoryPuzzleButton = findViewById(R.id.memory_puzzle_button);
            View patternPuzzleButton = findViewById(R.id.pattern_puzzle_button);
            View qrScannerButton = findViewById(R.id.qr_scanner_button);

            Log.d(TAG, "Puzzle buttons found - Math: " + (mathPuzzleButton != null) + 
                      ", Memory: " + (memoryPuzzleButton != null) + 
                      ", Pattern: " + (patternPuzzleButton != null) +
                      ", QR Scanner: " + (qrScannerButton != null));

            if (mathPuzzleButton != null) {
                mathPuzzleButton.setOnClickListener(v -> startPuzzleActivity(MathPuzzleActivity.class, false));
            }

            if (memoryPuzzleButton != null) {
                memoryPuzzleButton.setOnClickListener(v -> startPuzzleActivity(MemoryPuzzleActivity.class, false));
            }

            if (patternPuzzleButton != null) {
                patternPuzzleButton.setOnClickListener(v -> startPuzzleActivity(PatternPuzzleActivity.class, false));
            }

            if (qrScannerButton != null) {
                qrScannerButton.setOnClickListener(v -> {
                    if (alarm != null && alarm.isRequiresQR() && !alarm.getQrCode().isEmpty()) {
                        Intent intent = new Intent(RingActivity.this, QRScannerActivity.class);
                        intent.putExtra("expected_qr_code", alarm.getQrCode());
                        startActivityForResult(intent, REQUEST_CODE_QR_SCANNER);
                    } else {
                        Toast.makeText(RingActivity.this, 
                            "This alarm is not configured for QR code scanning", 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (snoozeButton != null) {
                snoozeButton.setOnClickListener(v -> handleSnooze());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setupClickListeners: ", e);
        }
    }

    private void updateSnoozeInfo() {
        Log.d(TAG, "updateSnoozeInfo started");
        if (alarm != null && snoozeInfoText != null && snoozeButton != null) {
            try {
                int remaining = alarm.getSnoozeLimit() - alarm.getSnoozedCount();
                int total = alarm.getSnoozeLimit();
                Log.d(TAG, "Snooze info - Remaining: " + remaining + ", Total: " + total);

                // Check if this is the last available snooze
                if (remaining == 1) {
                    snoozeInfoText.setText("LAST SNOOZE WARNING!");
                    snoozeInfoText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    snoozeButton.setText("Final Snooze");
                    snoozeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                } else {
                    snoozeInfoText.setText(String.format("Snoozes remaining: %d/%d", remaining, total));
                    snoozeInfoText.setTextColor(getResources().getColor(android.R.color.white));
                    snoozeButton.setText("Snooze");
                    snoozeButton.setBackgroundColor(getResources().getColor(R.color.translucent_grey2));
                }
                
                boolean canSnooze = alarm.canSnooze();
                Log.d(TAG, "Can snooze: " + canSnooze);
                snoozeButton.setEnabled(canSnooze);
                if (!canSnooze) {
                    snoozeButton.setText(R.string.no_snoozes_left);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating snooze info: ", e);
                Toast.makeText(this, "Error updating snooze info", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Cannot update snooze info - Alarm: " + (alarm != null) + 
                      ", InfoText: " + (snoozeInfoText != null) + 
                      ", Button: " + (snoozeButton != null));
        }
    }

    private void handleSnooze() {
        if (alarm == null) {
            Toast.makeText(this, "Error: Alarm not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!alarm.canSnooze()) {
            Toast.makeText(this, "No snoozes remaining", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start puzzle activity for snooze
        startPuzzleActivity(MathPuzzleActivity.class, true);
    }

    private void startPuzzleActivity(Class<?> puzzleActivity, boolean isForSnooze) {
        try {
            Intent intent = new Intent(this, puzzleActivity);
            intent.putExtra("isForSnooze", isForSnooze);
            startActivityForResult(intent, isForSnooze ? PUZZLE_REQUEST_CODE_SNOOZE : PUZZLE_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Error starting puzzle", e);
            Toast.makeText(this, "Error starting puzzle", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_MEMORY_PUZZLE:
                case REQUEST_CODE_MATH_PUZZLE:
                case REQUEST_CODE_PATTERN_PUZZLE:
                case REQUEST_CODE_QR_SCANNER:
                    dismissAlarm();
                    break;
                case PUZZLE_REQUEST_CODE:
                    stopAlarm();
                    break;
                case PUZZLE_REQUEST_CODE_SNOOZE:
                    performSnooze();
                    break;
            }
        }
    }

    private void performSnooze() {
        if (alarm == null) {
            Log.e(TAG, "Cannot snooze - alarm is null");
            Toast.makeText(this, "Error: Alarm not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // First update the snooze count in the main alarm
            int currentCount = alarm.getSnoozedCount();
            int newCount = currentCount + 1;
            
            if (newCount >= alarm.getSnoozeLimit()) {
                Log.d(TAG, "No more snoozes allowed. Count: " + newCount + ", Limit: " + alarm.getSnoozeLimit());
                Toast.makeText(this, "No snoozes remaining", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a new temporary alarm for the snooze
            Alarm snoozeAlarm = new Alarm(
                alarm.getAlarmId(),  // Keep same ID but will use different PendingIntent ID
                alarm.getHour(),
                alarm.getMinute(),
                alarm.getTitle() + " (Snooze)",
                true,
                false,  // Snooze alarms are not recurring
                false, false, false, false, false, false, false,  // No recurring days
                alarm.getTone(),
                alarm.isVibrate(),
                alarm.getSnoozeLimit(),
                alarm.getSnoozeInterval()
            );

            // Set the snooze count in both alarms
            snoozeAlarm.setSnoozedCount(newCount);
            alarm.setSnoozedCount(newCount);
            
            // Update main alarm in database to track snooze count
            alarmsListViewModel.update(alarm);
            Log.d(TAG, "Updated snooze count in main alarm: " + alarm.getSnoozedCount());

            // Calculate snooze time
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.MINUTE, alarm.getSnoozeInterval());

            // Set the time for the snooze alarm
            snoozeAlarm.setHour(calendar.get(Calendar.HOUR_OF_DAY));
            snoozeAlarm.setMinute(calendar.get(Calendar.MINUTE));
            
            // Schedule the snooze alarm
            snoozeAlarm.schedule(getApplicationContext());

            // Show a special warning message if this is the last snooze
            if (alarm.getSnoozeLimit() - newCount == 1) {
                String message = "FINAL SNOOZE WARNING: Alarm will use loud ringtone next time!";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                Log.d(TAG, message);
            } else {
                String message = String.format("Alarm snoozed for %d minutes. Snoozes remaining: %d/%d", 
                    alarm.getSnoozeInterval(), 
                    alarm.getSnoozeLimit() - newCount,
                    alarm.getSnoozeLimit());
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                Log.d(TAG, message);
            }

            stopAlarmService();
        } catch (Exception e) {
            Log.e(TAG, "Error performing snooze", e);
            Toast.makeText(this, "Error snoozing alarm", Toast.LENGTH_SHORT).show();
        }
    }

    private void animateClock() {
        try {
            // Load the wobble animation
            Animation wobbleAnimation = AnimationUtils.loadAnimation(this, R.anim.wobble_animation);
            
            // Apply some custom properties for better visual effect
            wobbleAnimation.setRepeatMode(Animation.RESTART);
            wobbleAnimation.setRepeatCount(Animation.INFINITE);
            
            // Get a reference to the clock image view
            if (ringActivityViewBinding.activityRingClock != null) {
                // Stop any previous animation
                ringActivityViewBinding.activityRingClock.clearAnimation();
                
                // Make sure the image is visible
                ringActivityViewBinding.activityRingClock.setVisibility(View.VISIBLE);
                
                // Apply a slight scale to make the clock more prominent
                ringActivityViewBinding.activityRingClock.setScaleX(1.1f);
                ringActivityViewBinding.activityRingClock.setScaleY(1.1f);
                
                // Start the wobble animation
                ringActivityViewBinding.activityRingClock.startAnimation(wobbleAnimation);
                
                // Set a more vibrant tint to the alarm icon when it's animating
                if (alarm != null && alarm.getSnoozedCount() > 0 && 
                    alarm.getSnoozeLimit() - alarm.getSnoozedCount() == 1) {
                    // Use red tint for last snooze warning
                    ringActivityViewBinding.activityRingClock.setColorFilter(
                            getResources().getColor(android.R.color.holo_red_light));
                } else {
                    // Use normal color for regular alarms
                    ringActivityViewBinding.activityRingClock.setColorFilter(
                            getResources().getColor(R.color.white));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error animating clock", e);
        }
    }

    private void stopAlarm() {
        if (alarm != null) {
            try {
                alarm.setStarted(false);
                alarm.setSnoozedCount(0);  // Reset snooze count when alarm is stopped
                alarm.cancelAlarm(getBaseContext());
                alarmsListViewModel.update(alarm);  // Make sure to update in database
                Log.d(TAG, "Alarm stopped and snooze count reset");

                // Schedule wake-up check if enabled
                if (alarm.isWakeUpCheckEnabled()) {
                    alarm.scheduleWakeUpCheck(getApplicationContext());
                    Toast.makeText(this, "Wake-up check scheduled in 5 minutes", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error stopping alarm", e);
                Toast.makeText(this, "Error stopping alarm", Toast.LENGTH_SHORT).show();
            }
        }
        stopAlarmService();
    }

    private void stopAlarmService() {
        try {
            Intent intentService = new Intent(getApplicationContext(), AlarmService.class);
            getApplicationContext().stopService(intentService);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping alarm service", e);
            Toast.makeText(this, "Error stopping alarm service", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(false);
                setTurnScreenOn(false);
            } else {
                getWindow().clearFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent back button from dismissing the alarm
        Toast.makeText(this, "Solve a puzzle to dismiss the alarm", Toast.LENGTH_SHORT).show();
    }

    private void showPuzzleOptions() {
        String[] options = new String[]{"Memory Puzzle", "Math Puzzle", "Pattern Puzzle", "Scan QR Code"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose a method to dismiss alarm");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        startActivityForResult(new Intent(RingActivity.this, MemoryPuzzleActivity.class), REQUEST_CODE_MEMORY_PUZZLE);
                        break;
                    case 1:
                        startActivityForResult(new Intent(RingActivity.this, MathPuzzleActivity.class), REQUEST_CODE_MATH_PUZZLE);
                        break;
                    case 2:
                        startActivityForResult(new Intent(RingActivity.this, PatternPuzzleActivity.class), REQUEST_CODE_PATTERN_PUZZLE);
                        break;
                    case 3:
                        startActivityForResult(new Intent(RingActivity.this, QRScannerActivity.class), REQUEST_CODE_QR_SCANNER);
                        break;
                }
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void dismissAlarm() {
        if (alarm != null) {
            try {
                // Stop the alarm
                alarm.setStarted(false);
                alarm.setSnoozedCount(0);  // Reset snooze count
                alarm.cancelAlarm(getBaseContext());
                alarmsListViewModel.update(alarm);
                
                // Show success message
                String message = alarm.isRecurring() ? 
                    "Alarm dismissed. Next alarm in " + alarm.getRecurringDaysText() :
                    "Alarm dismissed";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                // Schedule wake-up check if enabled
                if (alarm.isWakeUpCheckEnabled()) {
                    alarm.scheduleWakeUpCheck(getApplicationContext());
                    Toast.makeText(this, "Wake-up check scheduled in 5 minutes", Toast.LENGTH_LONG).show();
                }

                // Stop the alarm service and finish activity
                stopAlarmService();
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing alarm", e);
                Toast.makeText(this, "Error dismissing alarm", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Cannot dismiss - alarm is null");
            stopAlarmService();
        }
    }
}