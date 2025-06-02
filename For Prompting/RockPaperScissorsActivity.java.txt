package com.example.jarm;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.jarm.databinding.ActivityRockPaperScissorsBinding;

import java.util.Random;

public class RockPaperScissorsActivity extends AppCompatActivity {

    private ActivityRockPaperScissorsBinding binding;
    private Random random;

    private enum Choice {ROCK, PAPER, SCISSORS}
    private enum GameMode { BEST_OF, FIRST_TO }

    private int playerScore = 0;
    private int computerScore = 0;

    // Configuration
    private GameMode currentGameMode = GameMode.BEST_OF;
    private int roundsToPlay = 3;
    private int pointsToWin = 3;
    private int currentRound = 0;

    private final int MIN_ROUNDS_POINTS = 1;
    private final int MAX_ROUNDS_POINTS = 99;

    // Dedicated Pause Overlay Views
    private View dedicatedPauseOverlayContainer;
    private View buttonDedicatedPauseResume, buttonDedicatedPauseRestart, buttonDedicatedPauseMainMenu;
    private boolean isGamePausedByDedicatedMenu = false;

    // Help Overlay Views
    private View helpOverlayContainerView;
    private TextView textViewHelpOverlayTitle;
    private TextView textViewHelpOverlayContent;
    private Button buttonHelpOverlayClose;

    private boolean gameConfigLocked = false;
    private boolean matchOver = false;

    // Database Helper
    private StatsDbHelper dbHelper;
    private static final String TAG = "RPSActivity";
    private Menu activityMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRockPaperScissorsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        random = new Random();
        dbHelper = new StatsDbHelper(this);

        Toolbar toolbar = binding.toolbarRockPaperScissors;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        initializeHelpOverlay();
        initializeDedicatedPauseOverlay();
        setupChoiceListeners();
        setupConfigPanelListeners();
        updateConfigUI();
        updateScoreDisplay();
        resetRoundUI(true);
        binding.buttonPlayAgain.setOnClickListener(v -> {
            if (isHelpOverlayVisible() || isGamePausedByDedicatedMenu) return;
            resetFullGame();
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
            Log.e(TAG + "Help", "Help overlay container not found.");
        }
    }

    protected void showHelpOverlay(String title, String content) {
        if (helpOverlayContainerView != null && textViewHelpOverlayTitle != null && textViewHelpOverlayContent != null) {
            if (isGamePausedByDedicatedMenu) {
                resumeGameFromDedicatedPause(); // Resume dedicated pause before showing help
            }

            textViewHelpOverlayTitle.setText(title);
            textViewHelpOverlayContent.setText(content);
            helpOverlayContainerView.setVisibility(View.VISIBLE);

            setGameInteractionEnabled(false);
            if (activityMenu != null) {
                for (int i = 0; i < activityMenu.size(); i++) {
                    if(activityMenu.getItem(i).getItemId() != R.id.action_help_rps) {
                        activityMenu.getItem(i).setEnabled(false);
                    }
                }
            }
            updatePauseButtonVisibility();
        } else {
            Log.e(TAG + "Help", "Help overlay views not properly initialized for showing.");
            Toast.makeText(this, "Error showing help.", Toast.LENGTH_SHORT).show();
        }
    }

    protected void hideHelpOverlay() {
        if (helpOverlayContainerView != null) {
            helpOverlayContainerView.setVisibility(View.GONE);
            setGameInteractionEnabled(true);
            if (activityMenu != null) {
                for (int i = 0; i < activityMenu.size(); i++) {
                    activityMenu.getItem(i).setEnabled(true);
                }
            }
            updatePauseButtonVisibility();
        }
    }

    protected boolean isHelpOverlayVisible() {
        return helpOverlayContainerView != null && helpOverlayContainerView.getVisibility() == View.VISIBLE;
    }
    // --- End of Help Overlay Methods ---

    private void setGameInteractionEnabled(boolean enabled) {
        boolean canInteractGame = enabled && !matchOver && !isGamePausedByDedicatedMenu && !isHelpOverlayVisible();
        binding.buttonRock.setEnabled(canInteractGame);
        binding.buttonPaper.setEnabled(canInteractGame);
        binding.buttonScissors.setEnabled(canInteractGame);

        boolean canInteractPlayAgain = enabled && matchOver && !isGamePausedByDedicatedMenu && !isHelpOverlayVisible();
        binding.buttonPlayAgain.setEnabled(canInteractPlayAgain);


        boolean enableConfigPanelInteractions = enabled && !gameConfigLocked && !isGamePausedByDedicatedMenu && !isHelpOverlayVisible();
        binding.textConfigBestOf.setEnabled(enableConfigPanelInteractions);
        binding.textConfigFirstTo.setEnabled(enableConfigPanelInteractions);
        binding.buttonConfigDecrease.setEnabled(enableConfigPanelInteractions);
        binding.buttonConfigIncrease.setEnabled(enableConfigPanelInteractions);
        binding.edittextConfigRounds.setAlpha(enableConfigPanelInteractions ? 1.0f : 0.5f);

        updatePauseButtonVisibility(); // Update pause button based on new interaction state
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_rps, menu);
        this.activityMenu = menu;
        updatePauseButtonVisibility();
        return true;
    }

    private void updatePauseButtonVisibility() {
        if (activityMenu != null) {
            MenuItem pauseItem = activityMenu.findItem(R.id.action_pause_rps);
            if (pauseItem != null) {
                pauseItem.setVisible(gameConfigLocked && !matchOver && !isGamePausedByDedicatedMenu && !isHelpOverlayVisible());
            }
        }
    }

    private void initializeDedicatedPauseOverlay() {
        dedicatedPauseOverlayContainer = findViewById(R.id.pause_overlay_container);
        if (dedicatedPauseOverlayContainer != null) {
            buttonDedicatedPauseResume = dedicatedPauseOverlayContainer.findViewById(R.id.button_pause_resume);
            buttonDedicatedPauseRestart = dedicatedPauseOverlayContainer.findViewById(R.id.button_pause_restart);
            buttonDedicatedPauseMainMenu = dedicatedPauseOverlayContainer.findViewById(R.id.button_pause_main_menu);

            if (buttonDedicatedPauseResume != null) buttonDedicatedPauseResume.setOnClickListener(v -> resumeGameFromDedicatedPause());
            if (buttonDedicatedPauseRestart != null) buttonDedicatedPauseRestart.setOnClickListener(v -> {
                resumeGameFromDedicatedPause();
                resetFullGame();
            });
            if (buttonDedicatedPauseMainMenu != null) buttonDedicatedPauseMainMenu.setOnClickListener(v -> goToMainMenu());
            dedicatedPauseOverlayContainer.setVisibility(View.GONE);
        }
    }

    private void setupChoiceListeners() {
        binding.buttonRock.setOnClickListener(v -> { if(binding.buttonRock.isEnabled()) playGame(Choice.ROCK); });
        binding.buttonPaper.setOnClickListener(v -> { if(binding.buttonPaper.isEnabled()) playGame(Choice.PAPER); });
        binding.buttonScissors.setOnClickListener(v -> { if(binding.buttonScissors.isEnabled()) playGame(Choice.SCISSORS); });
    }

    private void setupConfigPanelListeners() {
        binding.textConfigBestOf.setOnClickListener(v -> { if(binding.textConfigBestOf.isEnabled()) selectGameMode(GameMode.BEST_OF); });
        binding.textConfigFirstTo.setOnClickListener(v -> { if(binding.textConfigFirstTo.isEnabled()) selectGameMode(GameMode.FIRST_TO); });
        binding.buttonConfigDecrease.setOnClickListener(v -> { if(binding.buttonConfigDecrease.isEnabled()) changeRoundsOrPoints(-1); });
        binding.buttonConfigIncrease.setOnClickListener(v -> { if(binding.buttonConfigIncrease.isEnabled()) changeRoundsOrPoints(1); });
    }

    private void selectGameMode(GameMode mode) {
        if (gameConfigLocked) return;
        currentGameMode = mode;
        if (currentGameMode == GameMode.BEST_OF) {
            roundsToPlay = 3;
        } else {
            pointsToWin = 3;
        }
        updateConfigUI();
    }

    private void changeRoundsOrPoints(int delta) {
        if (gameConfigLocked) return;
        if (currentGameMode == GameMode.BEST_OF) {
            int newRounds = roundsToPlay + (delta * 2);
            if (newRounds >= MIN_ROUNDS_POINTS && newRounds <= MAX_ROUNDS_POINTS) {
                roundsToPlay = newRounds;
            }
        } else {
            int newPoints = pointsToWin + delta;
            if (newPoints >= MIN_ROUNDS_POINTS && newPoints <= MAX_ROUNDS_POINTS) {
                pointsToWin = newPoints;
            }
        }
        updateConfigUI();
    }

    private void updateConfigUI() {
        if (currentGameMode == GameMode.BEST_OF) {
            binding.textConfigBestOf.setTextColor(ContextCompat.getColor(this, R.color.purple_500));
            binding.textConfigBestOf.setTypeface(null, Typeface.BOLD);
            binding.textConfigFirstTo.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_dark));
            binding.textConfigFirstTo.setTypeface(null, Typeface.NORMAL);
            binding.edittextConfigRounds.setText(String.valueOf(roundsToPlay));
            binding.textConfigRoundsLabel.setText(getString(R.string.rps_config_rounds_label_best_of));
        } else {
            binding.textConfigFirstTo.setTextColor(ContextCompat.getColor(this, R.color.purple_500));
            binding.textConfigFirstTo.setTypeface(null, Typeface.BOLD);
            binding.textConfigBestOf.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_dark));
            binding.textConfigBestOf.setTypeface(null, Typeface.NORMAL);
            binding.edittextConfigRounds.setText(String.valueOf(pointsToWin));
            binding.textConfigRoundsLabel.setText(getString(R.string.rps_config_rounds_label_first_to));
        }
        // Let setGameInteractionEnabled handle actual enabling/disabling
        // This method focuses on visual state of config if it *were* enabled
        binding.edittextConfigRounds.setAlpha(gameConfigLocked ? 0.5f : 1.0f);
        binding.configPanelRps.setAlpha(gameConfigLocked ? 0.5f : 1.0f);
        setGameInteractionEnabled(!isHelpOverlayVisible() && !isGamePausedByDedicatedMenu);
    }


    private void playGame(Choice playerChoice) {
        // Check for overlays is handled by setGameInteractionEnabled and individual listeners
        if (matchOver) return; // Already checked by listener enable state, but good safeguard

        if (!gameConfigLocked) {
            gameConfigLocked = true;
            // updateConfigUI(); // Called by setGameInteractionEnabled after this block
            binding.configPanelRps.setVisibility(View.GONE);
            setGameInteractionEnabled(true); // Enable game buttons, disable config
            // updatePauseButtonVisibility(); // Called by setGameInteractionEnabled
        }
        currentRound++;

        Choice computerChoice = Choice.values()[random.nextInt(Choice.values().length)];
        String roundResultText;
        String roundOutcomeForDb;

        if (playerChoice == computerChoice) {
            roundResultText = getString(R.string.rps_verdict_tie);
            roundOutcomeForDb = "TIE";
        } else if ((playerChoice == Choice.ROCK && computerChoice == Choice.SCISSORS) ||
                (playerChoice == Choice.PAPER && computerChoice == Choice.ROCK) ||
                (playerChoice == Choice.SCISSORS && computerChoice == Choice.PAPER)) {
            roundResultText = getString(R.string.rps_verdict_player_wins);
            playerScore++;
            roundOutcomeForDb = "WIN";
        } else {
            roundResultText = getString(R.string.rps_verdict_computer_wins);
            computerScore++;
            roundOutcomeForDb = "LOSE";
        }

        saveRpsRoundResult(playerChoice, computerChoice, roundOutcomeForDb);
        updateScoreDisplay();
        displayResults(playerChoice, computerChoice, roundResultText);

        if (isGameOver()) {
            matchOver = true;
            binding.textViewRpsStatus.setText(getGameOverMessage());
            setGameInteractionEnabled(false); // Disables choice buttons
            binding.buttonPlayAgain.setVisibility(View.VISIBLE);
            binding.buttonPlayAgain.setEnabled(!isHelpOverlayVisible() && !isGamePausedByDedicatedMenu);
            // updatePauseButtonVisibility(); // Called by setGameInteractionEnabled
        } else {
            if (currentGameMode == GameMode.BEST_OF) {
                binding.textViewRpsStatus.setText(getString(R.string.rps_status_play_again) + " (Round " + currentRound + "/" + roundsToPlay + ")");
            } else {
                String firstToStatus = String.format(getString(R.string.rps_status_first_to_points_needed), pointsToWin);
                binding.textViewRpsStatus.setText(firstToStatus);
            }
            binding.buttonPlayAgain.setVisibility(View.GONE);
        }
    }

    private void saveRpsRoundResult(Choice playerChoice, Choice computerChoice, String roundOutcome) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(StatsDbHelper.RpsEntry.COLUMN_NAME_PLAYER_CHOICE, playerChoice.name());
        values.put(StatsDbHelper.RpsEntry.COLUMN_NAME_COMPUTER_CHOICE, computerChoice.name());
        values.put(StatsDbHelper.RpsEntry.COLUMN_NAME_ROUND_OUTCOME, roundOutcome);
        long newRowId = db.insert(StatsDbHelper.RpsEntry.TABLE_NAME, null, values);
        if (newRowId == -1) {
            Log.e(TAG, "Error saving RPS round result to DB.");
        } else {
            Log.i(TAG, "RPS round result saved: " + playerChoice.name() + ", " + computerChoice.name() + ", " + roundOutcome);
        }
    }

    private boolean isGameOver() {
        if (currentGameMode == GameMode.FIRST_TO) {
            return playerScore >= pointsToWin || computerScore >= pointsToWin;
        } else {
            if (playerScore > roundsToPlay / 2) return true;
            if (computerScore > roundsToPlay / 2) return true;
            int roundsRemaining = roundsToPlay - currentRound;
            if (playerScore + roundsRemaining < computerScore) return true;
            if (computerScore + roundsRemaining < playerScore) return true;
            return currentRound >= roundsToPlay;
        }
    }

    private String getGameOverMessage() {
        if (playerScore > computerScore) return "Player Wins the Match!";
        else if (computerScore > playerScore) return "Computer Wins the Match!";
        else return "Match is a Tie!";
    }

    private void resetFullGame() {
        playerScore = 0;
        computerScore = 0;
        currentRound = 0;
        gameConfigLocked = false;
        matchOver = false;
        isGamePausedByDedicatedMenu = false;

        updateScoreDisplay();
        updateConfigUI(); // This re-evaluates config visibility and interactions
        resetRoundUI(true);

        binding.configPanelRps.setVisibility(View.VISIBLE);
        // setGameInteractionEnabled(true) is called by updateConfigUI via its chain

        if (isHelpOverlayVisible()){ hideHelpOverlay(); }
        if(dedicatedPauseOverlayContainer != null && dedicatedPauseOverlayContainer.getVisibility() == View.VISIBLE){
            dedicatedPauseOverlayContainer.setVisibility(View.GONE);
        }
        // updatePauseButtonVisibility(); // Called by setGameInteractionEnabled via updateConfigUI
    }

    private int getDrawableForChoice(Choice choice) {
        switch (choice) {
            case ROCK: return R.drawable.img_rock_2;
            case PAPER: return R.drawable.img_paper_2;
            case SCISSORS: return R.drawable.img_scissors_2;
            default: return 0;
        }
    }

    private String getChoiceString(Choice choice) {
        switch (choice) {
            case ROCK: return getString(R.string.rps_choice_rock);
            case PAPER: return getString(R.string.rps_choice_paper);
            case SCISSORS: return getString(R.string.rps_choice_scissors);
            default: return "";
        }
    }

    private void updateScoreDisplay() {
        binding.textViewScorePlayer.setText(getString(R.string.rps_score_player, playerScore));
        binding.textViewScoreComputer.setText(getString(R.string.rps_score_computer, computerScore));
    }

    private void displayResults(Choice playerChoice, Choice computerChoice, String roundResultText_NotUsedByButton) {
        binding.textViewPlayerChoiceLabel.setText(getString(R.string.rps_player_choice_label, getChoiceString(playerChoice)));
        binding.textViewComputerChoiceLabel.setText(getString(R.string.rps_computer_choice_label, getChoiceString(computerChoice)));

        binding.textViewPlayerChoiceLabel.setVisibility(View.VISIBLE);
        binding.textViewComputerChoiceLabel.setVisibility(View.VISIBLE);
        binding.computerChoiceDisplayContainer.setVisibility(View.VISIBLE);

        binding.imageComputerSlotLeft.setImageDrawable(null);
        binding.imageComputerSlotCenter.setImageDrawable(null);
        binding.imageComputerSlotRight.setImageDrawable(null);

        int computerChoiceDrawable = getDrawableForChoice(computerChoice);
        if (playerChoice == Choice.ROCK) {
            binding.imageComputerSlotLeft.setImageResource(computerChoiceDrawable);
        } else if (playerChoice == Choice.PAPER) {
            binding.imageComputerSlotCenter.setImageResource(computerChoiceDrawable);
        } else { // SCISSORS
            binding.imageComputerSlotRight.setImageResource(computerChoiceDrawable);
        }
    }

    private void resetRoundUI(boolean fullReset) {
        binding.textViewPlayerChoiceLabel.setVisibility(View.GONE);
        binding.textViewComputerChoiceLabel.setVisibility(View.GONE);
        binding.computerChoiceDisplayContainer.setVisibility(View.GONE);
        binding.buttonPlayAgain.setVisibility(View.GONE);

        binding.imageComputerSlotLeft.setImageDrawable(null);
        binding.imageComputerSlotCenter.setImageDrawable(null);
        binding.imageComputerSlotRight.setImageDrawable(null);

        if (fullReset) {
            binding.textViewRpsStatus.setText(getString(R.string.rps_status_make_move));
        }
    }

    private void toggleDedicatedPauseState() {
        if (!gameConfigLocked || matchOver || isHelpOverlayVisible()) return;

        if (isGamePausedByDedicatedMenu) {
            resumeGameFromDedicatedPause();
        } else {
            pauseGameForDedicatedMenu();
        }
        // updatePauseButtonVisibility(); // Called by pause/resume methods
    }

    private void pauseGameForDedicatedMenu() {
        isGamePausedByDedicatedMenu = true;
        if (dedicatedPauseOverlayContainer != null) {
            dedicatedPauseOverlayContainer.setVisibility(View.VISIBLE);
        }
        setGameInteractionEnabled(false);
        updatePauseButtonVisibility(); // Explicitly call to hide toolbar pause button
    }

    private void resumeGameFromDedicatedPause() {
        isGamePausedByDedicatedMenu = false;
        if (dedicatedPauseOverlayContainer != null) {
            dedicatedPauseOverlayContainer.setVisibility(View.GONE);
        }
        setGameInteractionEnabled(true);
        updatePauseButtonVisibility(); // Explicitly call to show toolbar pause button
    }

    private void goToMainMenu() {
        if (isGamePausedByDedicatedMenu) {
            resumeGameFromDedicatedPause();
        }
        if (isHelpOverlayVisible()){
            hideHelpOverlay();
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (isHelpOverlayVisible()) {
            if (itemId == R.id.action_help_rps || itemId == android.R.id.home) {
                hideHelpOverlay();
                return true;
            }
            return false;
        }
        if (isGamePausedByDedicatedMenu) {
            if (itemId == R.id.action_pause_rps || itemId == android.R.id.home) {
                // Allow these actions
            } else {
                return false;
            }
        }

        if (itemId == android.R.id.home) {
            if (isGamePausedByDedicatedMenu) resumeGameFromDedicatedPause();
            finish();
            return true;
        } else if (itemId == R.id.action_help_rps) {
            if(isGamePausedByDedicatedMenu) resumeGameFromDedicatedPause();
            showHelpOverlay(getString(R.string.help_title_rps), getString(R.string.help_content_rps));
            return true;
        } else if (itemId == R.id.action_pause_rps) {
            toggleDedicatedPauseState();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isHelpOverlayVisible()) {
            hideHelpOverlay();
        } else if (isGamePausedByDedicatedMenu) {
            resumeGameFromDedicatedPause();
            // updatePauseButtonVisibility(); // Called by resume
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
