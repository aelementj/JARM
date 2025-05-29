package com.example.jarm;

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
    private String selectedDifficulty = "Easy";

    private TextView selectedGridItemView; // Renamed for clarity
    private List<TextView> gridItemViews; // Renamed for clarity

    private Drawable defaultGridItemBackground;
    private Drawable selectedGridItemBackgroundConnected; // New drawable for selected state

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

        // Load drawables
        defaultGridItemBackground = ContextCompat.getDrawable(this, R.drawable.bg_grid_difficulty_item_default);
        selectedGridItemBackgroundConnected = ContextCompat.getDrawable(this, R.drawable.bg_grid_difficulty_item_selected);

        gridItemViews = new ArrayList<>();
        gridItemViews.add(binding.gridItemEasy);
        gridItemViews.add(binding.gridItemMedium);
        gridItemViews.add(binding.gridItemHard);

        for (TextView item : gridItemViews) {
            item.setOnClickListener(this);
        }

        // Set initial selection
        if (!gridItemViews.isEmpty()) {
            updateSelectedGridItemUI(binding.gridItemEasy); // Default to Easy
            updateModifierText(binding.gridItemEasy.getId());
        }


        binding.buttonPlayArithmetic.setOnClickListener(view -> {
            Toast.makeText(ArithmeticChallengeActivity.this, "Play button clicked! Difficulty: " + selectedDifficulty, Toast.LENGTH_SHORT).show();
        });

        binding.buttonHowToPlayArithmetic.setOnClickListener(view -> {
            Toast.makeText(ArithmeticChallengeActivity.this, "How to Play button clicked!", Toast.LENGTH_SHORT).show();
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
                // Apply selected state
                item.setBackground(selectedGridItemBackgroundConnected);
                item.setTextColor(ContextCompat.getColor(this, R.color.grid_item_text_selected_color));
            } else {
                // Apply default/unselected state
                item.setBackground(defaultGridItemBackground);
                item.setTextColor(ContextCompat.getColor(this, R.color.grid_item_text_default_color));
            }
        }
        this.selectedGridItemView = newlySelectedTextView; // Keep track of the selected view
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