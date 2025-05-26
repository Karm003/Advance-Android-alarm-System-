package com.unique.simplealarmclock.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.unique.simplealarmclock.R;

import java.util.Random;

public class MathPuzzleActivity extends AppCompatActivity {
    private TextView questionTextView;
    private EditText answerEditText;
    private Button submitButton;
    private int correctAnswer;
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_math_puzzle);

        questionTextView = findViewById(R.id.question_text_view);
        answerEditText = findViewById(R.id.answer_edit_text);
        submitButton = findViewById(R.id.submit_button);
        random = new Random();

        generateQuestion();

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String answerStr = answerEditText.getText().toString();
                if (!answerStr.isEmpty()) {
                    try {
                        int userAnswer = Integer.parseInt(answerStr);
                        if (userAnswer == correctAnswer) {
                            Toast.makeText(MathPuzzleActivity.this, "Correct! You can snooze the alarm.", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(MathPuzzleActivity.this, "Wrong answer. Try again!", Toast.LENGTH_SHORT).show();
                            generateQuestion();
                            answerEditText.setText("");
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(MathPuzzleActivity.this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MathPuzzleActivity.this, "Please enter your answer", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void generateQuestion() {
        // Generate two random numbers between 1 and 20
        int num1 = random.nextInt(20) + 1;
        int num2 = random.nextInt(20) + 1;
        
        // Choose operation: 0: addition, 1: subtraction, 2: multiplication
        int operation = random.nextInt(3);
        
        // For subtraction, ensure num1 >= num2 to avoid negative results
        if (operation == 1 && num1 < num2) {
            // Swap the numbers
            int temp = num1;
            num1 = num2;
            num2 = temp;
        }

        String question;
        switch (operation) {
            case 0:
                question = num1 + " + " + num2 + " = ?";
                correctAnswer = num1 + num2;
                break;
            case 1:
                question = num1 + " - " + num2 + " = ?";
                correctAnswer = num1 - num2;
                break;
            default:
                question = num1 + " × " + num2 + " = ?";
                correctAnswer = num1 * num2;
                break;
        }

        questionTextView.setText(question);
        answerEditText.setText("");
        answerEditText.requestFocus();
    }
} 