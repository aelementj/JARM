package com.example.jarm;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.jarm.databinding.ActivityArithmeticChallengeBinding;

public class ArithmeticChallengeActivity extends AppCompatActivity {

    private ActivityArithmeticChallengeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArithmeticChallengeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar
        Toolbar toolbar = binding.toolbarArithmeticChallenge;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.game_title_arithmetic_challenge);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // TODO: Implement listeners for Play button
        // binding.buttonArithmeticPlay.setOnClickListener(v -> { /* Start game logic */ });

        // TODO: Implement listener for How to Play button
        // binding.buttonArithmeticHowToPlay.setOnClickListener(v -> { /* Show rules */ });

        // TODO: Implement logic for difficulty selection
        // binding.radioGroupDifficulty.setOnCheckedChangeListener((group, checkedId) -> { /* Handle difficulty change */ });

        // Placeholder for testing button clicks during development
        binding.buttonArithmeticPlay.setOnClickListener(v -> Toast.makeText(ArithmeticChallengeActivity.this, "Play clicked!", Toast.LENGTH_SHORT).show());
        binding.buttonArithmeticHowToPlay.setOnClickListener(v -> Toast.makeText(ArithmeticChallengeActivity.this, "How to Play clicked!", Toast.LENGTH_SHORT).show());
        binding.radioGroupDifficulty.setOnCheckedChangeListener((group, checkedId) -> {
            String difficulty = "";
            if (checkedId == R.id.radio_button_easy) {
                difficulty = "Easy";
            } else if (checkedId == R.id.radio_button_medium) {
                difficulty = "Medium";
            } else if (checkedId == R.id.radio_button_hard) {
                difficulty = "Hard";
            }
            Toast.makeText(ArithmeticChallengeActivity.this, "Difficulty set to: " + difficulty, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close this activity and return to previous one (if any)
            return true;
        }
        // TODO: Add menu items for Arithmetic Challenge (e.g., Help) if needed
        return super.onOptionsItemSelected(item);
    }
}
