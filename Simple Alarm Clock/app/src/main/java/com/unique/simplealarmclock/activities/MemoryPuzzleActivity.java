package com.unique.simplealarmclock.activities;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.unique.simplealarmclock.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MemoryPuzzleActivity extends AppCompatActivity {
    private GridLayout gridLayout;
    private TextView statusTextView;
    private List<Button> buttons;
    private List<Integer> numbers;
    private int firstChoice = -1;
    private int secondChoice = -1;
    private int matches = 0;
    private Handler handler;
    private static final int TOTAL_PAIRS = 4;
    private boolean isProcessing = false;
    private boolean isForSnooze = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_puzzle);

        gridLayout = findViewById(R.id.grid_layout);
        statusTextView = findViewById(R.id.status_text_view);
        buttons = new ArrayList<>();
        numbers = new ArrayList<>();
        handler = new Handler(Looper.getMainLooper());
        
        // Check if this puzzle is for snoozing
        if (getIntent() != null) {
            isForSnooze = getIntent().getBooleanExtra("isForSnooze", false);
        }

        // Set up restart button
        Button restartButton = findViewById(R.id.restart_button);
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        setupGrid();
        startGame();
    }

    private void setupGrid() {
        gridLayout.setColumnCount(3);
        gridLayout.setRowCount(3);
        
        // Calculate button size based on screen width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int buttonSize = (screenWidth - 100) / 3; // Adjust for 3 columns
        
        // Create 9 buttons (8 for pairs + 1 empty center)
        for (int i = 0; i < 9; i++) {
            Button button = new Button(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = buttonSize;
            params.height = buttonSize;
            params.setMargins(4, 4, 4, 4);
            button.setLayoutParams(params);
            
            // Make center button (index 4) empty and disabled
            if (i == 4) {
                button.setVisibility(View.INVISIBLE);
                button.setEnabled(false);
            } else {
                button.setText("?");
                button.setTextSize(20);
                button.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                button.setTextColor(getResources().getColor(android.R.color.white));
                button.setAlpha(0.7f);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    button.setStateListAnimator(null);
                }
                button.setPadding(0, 0, 0, 0);
                button.setAllCaps(false);
                
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleButtonClick((Button) v);
                    }
                });
            }
            
            buttons.add(button);
            gridLayout.addView(button);
        }
    }

    private void startGame() {
        numbers.clear();
        for (int i = 1; i <= TOTAL_PAIRS; i++) {
            numbers.add(i);
            numbers.add(i);
        }
        Collections.shuffle(numbers);
        
        // Insert null at center position (index 4)
        numbers.add(4, null);
        
        matches = 0;
        firstChoice = -1;
        secondChoice = -1;
        isProcessing = false;
        updateStatus();
        
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            if (i != 4) { // Skip center button
                button.setText("?");
                button.setEnabled(true);
                button.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                button.setAlpha(0.7f);
            }
        }
    }

    private void handleButtonClick(Button button) {
        // Prevent clicks during processing of a pair
        if (isProcessing) return;
        
        int index = buttons.indexOf(button);
        // Prevent clicking already matched or selected buttons
        if (index == firstChoice || index == secondChoice || !button.isEnabled()) return;

        if (firstChoice == -1) {
            // First card selection
            firstChoice = index;
            button.setText(String.valueOf(numbers.get(index)));
            button.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
            button.setAlpha(1.0f);
        } else {
            // Second card selection
            secondChoice = index;
            button.setText(String.valueOf(numbers.get(index)));
            button.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
            button.setAlpha(1.0f);
            
            // Set processing flag to prevent further clicks until pair is processed
            isProcessing = true;

            if (numbers.get(firstChoice).equals(numbers.get(secondChoice))) {
                // Matched pair
                matches++;
                updateStatus();
                
                // Change color of matched pair to match the screenshot
                final int color = getResources().getColor(android.R.color.holo_blue_light);
                buttons.get(firstChoice).setBackgroundColor(color);
                buttons.get(secondChoice).setBackgroundColor(color);
                buttons.get(firstChoice).setAlpha(1.0f);
                buttons.get(secondChoice).setAlpha(1.0f);
                
                // Disable the matched pair
                buttons.get(firstChoice).setEnabled(false);
                buttons.get(secondChoice).setEnabled(false);
                
                // Reset selection and processing flag
                firstChoice = -1;
                secondChoice = -1;
                isProcessing = false;
                
                if (matches == TOTAL_PAIRS) {
                    // Game completed
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            String message = isForSnooze ? 
                                "Puzzle solved! Alarm will be snoozed." : 
                                "Congratulations! You solved the puzzle.";
                            Toast.makeText(MemoryPuzzleActivity.this, message, Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        }
                    }, 500); // Small delay before showing success message
                }
            } else {
                // Not a match
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Hide cards again
                        buttons.get(firstChoice).setText("?");
                        buttons.get(firstChoice).setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                        buttons.get(firstChoice).setAlpha(0.7f);
                        
                        buttons.get(secondChoice).setText("?");
                        buttons.get(secondChoice).setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                        buttons.get(secondChoice).setAlpha(0.7f);
                        
                        // Reset selection and processing flag
                        firstChoice = -1;
                        secondChoice = -1;
                        isProcessing = false;
                    }
                }, 1500); // Increased delay to 1.5 seconds to give users time to see the cards
            }
        }
    }

    private void updateStatus() {
        statusTextView.setText("Matches: " + matches + "/" + TOTAL_PAIRS);
    }
    
    @Override
    public void onBackPressed() {
        // Don't allow back button to exit if puzzle is not solved
        if (matches < TOTAL_PAIRS) {
            Toast.makeText(this, "Solve the puzzle to proceed", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }
} 