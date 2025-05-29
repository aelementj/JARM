package com.example.jarm;

import android.content.Intent; // Make sure Intent is imported
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.jarm.databinding.ActivityArithmeticChallengeBinding;

import java.util.ArrayList;
import java.util.List;

public class ArithmeticChallengeActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityArithmeticChallengeBinding binding;
    private String selectedDifficulty = "Easy"; // Default difficulty

    private TextView selectedGridItemView;
    private List<TextView> gridItemViews;

    private Drawable defaultGridItemBackground;
    private Drawable selectedGridItemBackgroundConnected;

    // Key for passing difficulty to the next activity
    public static final String EXTRA_DIFFICULTY = "com.example.jarm.DIFFICULTY";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArithmeticChallengeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbarArithmeticChallenge;
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        defaultGridItemBackground = ContextCompat.getDrawable(this, R.drawable.bg_grid_difficulty_item_default);
        selectedGridItemBackgroundConnected = ContextCompat.getDrawable(this, R.drawable.bg_grid_difficulty_item_selected);

        gridItemViews = new ArrayList<>();
        gridItemViews.add(binding.gridItemEasy);
        gridItemViews.add(binding.gridItemMedium);
        gridItemViews.add(binding.gridItemHard);

        for (TextView item : gridItemViews) {
            item.setOnClickListener(this);
        }

        if (!gridItemViews.isEmpty()) {
            updateSelectedGridItemUI(binding.gridItemEasy);
            updateModifierText(binding.gridItemEasy.getId());
        }

        // MODIFIED: Set OnClickListener for the Play button
        binding.buttonPlayArithmetic.setOnClickListener(view -> {
            // Toast.makeText(ArithmeticChallengeActivity.this, "Play button clicked! Difficulty: " + selectedDifficulty, Toast.LENGTH_SHORT).show(); // Optional: keep for debugging
            Intent intent = new Intent(ArithmeticChallengeActivity.this, ArithmeticGameActivity.class);
            intent.putExtra(EXTRA_DIFFICULTY, selectedDifficulty); // Pass the selected difficulty
            startActivity(intent);
        });

        binding.buttonHowToPlayArithmetic.setOnClickListener(view -> {
            Toast.makeText(ArithmeticChallengeActivity.this, "How to Play button clicked!", Toast.LENGTH_SHORT).show();
            // Consider showing a DialogFragment or another Activity for "How to Play"
        });
    }

    @Override
    public void onClick(View v) {
        if (v instanceof TextView && gridItemViews.contains(v)) {
            updateSelectedGridItemUI((TextView) v);
            updateModifierText(v.getId());
        }
    }

    private void updateSelectedGridItemUI(TextView newlySelectedTextView) {
        for (TextView item : gridItemViews) {
            if (item == newlySelectedTextView) {
                item.setBackground(selectedGridItemBackgroundConnected);
                item.setTextColor(ContextCompat.getColor(this, R.color.grid_item_text_selected_color));
            } else {
                item.setBackground(defaultGridItemBackground);
                item.setTextColor(ContextCompat.getColor(this, R.color.grid_item_text_default_color));
            }
        }
        this.selectedGridItemView = newlySelectedTextView;
    }

    private void updateModifierText(int selectedItemId) {
        if (selectedItemId == R.id.grid_item_easy) {
            selectedDifficulty = getString(R.string.difficulty_easy);
            binding.textviewDifficultyModifiers.setText(getString(R.string.modifiers_easy_placeholder));
        } else if (selectedItemId == R.id.grid_item_medium) {
            selectedDifficulty = getString(R.string.difficulty_medium);
            binding.textviewDifficultyModifiers.setText(getString(R.string.modifiers_medium_placeholder));
        } else if (selectedItemId == R.id.grid_item_hard) {
            selectedDifficulty = getString(R.string.difficulty_hard);
            binding.textviewDifficultyModifiers.setText(getString(R.string.modifiers_hard_placeholder));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_arithmetic_challenge, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_help_arithmetic) {
            Toast.makeText(this, "Help for Arithmetic Challenge clicked!", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
