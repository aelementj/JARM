package com.example.jarm;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

    private TextView selectedGridItemView;
    private List<TextView> gridItemViews;

    private Drawable defaultGridItemBackground;
    private Drawable selectedGridItemBackgroundConnected;

    public static final String EXTRA_DIFFICULTY = "com.example.jarm.DIFFICULTY";

    // Help Overlay Views
    private View helpOverlayContainerView;
    private TextView textViewHelpOverlayTitle;
    private TextView textViewHelpOverlayContent;
    private Button buttonHelpOverlayClose;

    private static final String TAG = "ArithmeticChallenge";

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

        initializeHelpOverlay(); // Initialize help overlay

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

        binding.buttonPlayArithmetic.setOnClickListener(view -> {
            if (isHelpOverlayVisible()) return;

            Intent intent = new Intent(ArithmeticChallengeActivity.this, ArithmeticGameActivity.class);
            intent.putExtra(EXTRA_DIFFICULTY, selectedDifficulty);
            startActivity(intent);
        });

        // MODIFIED: "How to Play" button now shows gameplay help
        binding.buttonHowToPlayArithmetic.setOnClickListener(view -> {
            if (isHelpOverlayVisible()) { // If already visible, maybe clicking again closes it or does nothing
                hideHelpOverlay(); // Option: close it if already showing this help
                return;
            }
            // Show gameplay help
            showHelpOverlay(getString(R.string.help_title_arithmetic_game), getString(R.string.help_content_arithmetic_game));
        });
    }

    // --- Help Overlay Methods ---
    protected void initializeHelpOverlay() {
        helpOverlayContainerView = findViewById(R.id.help_overlay_container);
        if (helpOverlayContainerView != null) {
            textViewHelpOverlayTitle = helpOverlayContainerView.findViewById(R.id.textView_help_overlay_title);
            textViewHelpOverlayContent = helpOverlayContainerView.findViewById(R.id.textView_help_overlay_content);
            buttonHelpOverlayClose = helpOverlayContainerView.findViewById(R.id.button_help_overlay_close);

            if (buttonHelpOverlayClose != null) {
                buttonHelpOverlayClose.setOnClickListener(v -> hideHelpOverlay());
            }
            helpOverlayContainerView.setOnClickListener(v -> {
                if (v.getId() == R.id.help_overlay_container) {
                    hideHelpOverlay();
                }
            });
        } else {
            Log.e(TAG + "Help", "Help overlay container not found. Ensure it's included in activity layout.");
        }
    }

    protected void showHelpOverlay(String title, String content) {
        if (helpOverlayContainerView != null && textViewHelpOverlayTitle != null && textViewHelpOverlayContent != null) {
            textViewHelpOverlayTitle.setText(title);
            textViewHelpOverlayContent.setText(content);
            helpOverlayContainerView.setVisibility(View.VISIBLE);
            setUiInteractionEnabled(false); // Disable underlying UI
        } else {
            Log.e(TAG + "Help", "Help overlay views not properly initialized for showing.");
            Toast.makeText(this, "Error showing help.", Toast.LENGTH_SHORT).show();
        }
    }

    protected void hideHelpOverlay() {
        if (helpOverlayContainerView != null) {
            helpOverlayContainerView.setVisibility(View.GONE);
            setUiInteractionEnabled(true); // Re-enable underlying UI
        }
    }

    protected boolean isHelpOverlayVisible() {
        return helpOverlayContainerView != null && helpOverlayContainerView.getVisibility() == View.VISIBLE;
    }
    // --- End of Help Overlay Methods ---

    private void setUiInteractionEnabled(boolean enabled) {
        binding.buttonPlayArithmetic.setEnabled(enabled);
        binding.buttonHowToPlayArithmetic.setEnabled(enabled);
        binding.gridItemEasy.setEnabled(enabled);
        binding.gridItemMedium.setEnabled(enabled);
        binding.gridItemHard.setEnabled(enabled);

        Toolbar toolbar = findViewById(R.id.toolbar_arithmetic_challenge);
        if (toolbar != null) {
            Menu menu = toolbar.getMenu();
            if (menu != null) {
                for (int i = 0; i < menu.size(); i++) {
                    // Keep help icon itself enabled if it's the one being used to close overlay
                    if (menu.getItem(i).getItemId() != R.id.action_help_arithmetic) {
                        menu.getItem(i).setEnabled(enabled);
                    } else {
                        menu.getItem(i).setEnabled(true); // Always allow help icon to be clicked
                    }
                }
            }
        }
    }


    @Override
    public void onClick(View v) { // For difficulty grid items
        if (isHelpOverlayVisible()) return;

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

        if (isHelpOverlayVisible()) {
            // If help is visible, only allow clicking the same help icon (to close it) or home button
            if (itemId == R.id.action_help_arithmetic || itemId == android.R.id.home) {
                hideHelpOverlay();
                return true;
            }
            return false; // Consume other menu item clicks
        }

        // If help is not visible:
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_help_arithmetic) {
            // Toolbar help icon shows help for the setup/difficulty screen
            showHelpOverlay(getString(R.string.help_title_arithmetic_challenge), getString(R.string.help_content_arithmetic_challenge_setup));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isHelpOverlayVisible()) {
            hideHelpOverlay();
        } else {
            super.onBackPressed();
        }
    }
}
