package com.unique.simplealarmclock.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.unique.simplealarmclock.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PatternPuzzleActivity extends AppCompatActivity {
    private TextView patternTextView;
    private TextView questionTextView;
    private Button[] optionButtons;
    private int correctAnswer;
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pattern_puzzle);

        patternTextView = findViewById(R.id.pattern_text_view);
        questionTextView = findViewById(R.id.question_text_view);
        optionButtons = new Button[4];
        optionButtons[0] = findViewById(R.id.option1_button);
        optionButtons[1] = findViewById(R.id.option2_button);
        optionButtons[2] = findViewById(R.id.option3_button);
        optionButtons[3] = findViewById(R.id.option4_button);
        random = new Random();

        for (Button button : optionButtons) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleOptionClick((Button) v);
                }
            });
        }

        generatePattern();
    }

    private void generatePattern() {
        // Generate a sequence of 3 numbers
        List<Integer> sequence = new ArrayList<>();
        int step = random.nextInt(3) + 1; // Random step between 1 and 3
        int start = random.nextInt(10) + 1; // Random starting number between 1 and 10

        for (int i = 0; i < 3; i++) {
            sequence.add(start + (i * step));
        }

        // Generate the correct answer (next number in sequence)
        correctAnswer = start + (3 * step);

        // Generate 3 wrong answers
        List<Integer> wrongAnswers = new ArrayList<>();
        while (wrongAnswers.size() < 3) {
            int wrong = correctAnswer + random.nextInt(5) - 2;
            if (wrong != correctAnswer && !wrongAnswers.contains(wrong)) {
                wrongAnswers.add(wrong);
            }
        }

        // Combine all answers and shuffle
        List<Integer> allAnswers = new ArrayList<>();
        allAnswers.add(correctAnswer);
        allAnswers.addAll(wrongAnswers);
        java.util.Collections.shuffle(allAnswers);

        // Set the pattern text
        patternTextView.setText(sequence.get(0) + " → " + sequence.get(1) + " → " + sequence.get(2) + " → ?");
        questionTextView.setText("What comes next in the pattern?");

        // Set button texts
        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText(String.valueOf(allAnswers.get(i)));
        }
    }

    private void handleOptionClick(Button button) {
        int selectedAnswer = Integer.parseInt(button.getText().toString());
        if (selectedAnswer == correctAnswer) {
            Toast.makeText(this, "Correct! You can snooze the alarm.", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Wrong answer. Try again!", Toast.LENGTH_SHORT).show();
            generatePattern();
        }
    }
} 