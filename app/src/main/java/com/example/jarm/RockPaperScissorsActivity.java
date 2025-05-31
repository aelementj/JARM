package com.example.jarm;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.jarm.databinding.ActivityRockPaperScissorsBinding;

import java.util.Random;

public class RockPaperScissorsActivity extends AppCompatActivity {

    private ActivityRockPaperScissorsBinding binding;
    private Random random;

    private enum Choice {ROCK, PAPER, SCISSORS}

    private int playerScore = 0;
    private int computerScore = 0;

    // Pause Overlay Views
    private View pauseOverlayContainer;
    private View buttonPauseResume, buttonPauseRestart, buttonPauseMainMenu;
    private boolean isGamePaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRockPaperScissorsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        random = new Random();

        Toolbar toolbar = binding.toolbarRockPaperScissors;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        setupChoiceListeners();
        // REMOVE: setupResetListeners(); // This method will be removed
        initializePauseOverlay();
        updateScoreDisplay();
        resetRoundUI(true); // Initial reset
    }

    private void setupChoiceListeners() {
        binding.buttonRock.setOnClickListener(v -> playGame(Choice.ROCK));
        binding.buttonPaper.setOnClickListener(v -> playGame(Choice.PAPER));
        binding.buttonScissors.setOnClickListener(v -> playGame(Choice.SCISSORS));
    }

    // REMOVE: private void setupResetListeners() { ... }

    private void initializePauseOverlay() {
        pauseOverlayContainer = binding.getRoot().findViewById(R.id.pause_overlay_container);
        if (pauseOverlayContainer != null) {
            buttonPauseResume = pauseOverlayContainer.findViewById(R.id.button_pause_resume);
            buttonPauseRestart = pauseOverlayContainer.findViewById(R.id.button_pause_restart);
            buttonPauseMainMenu = pauseOverlayContainer.findViewById(R.id.button_pause_main_menu);

            buttonPauseResume.setOnClickListener(v -> resumeGame());

            // UPDATE Restart button listener
            buttonPauseRestart.setOnClickListener(v -> {
                resumeGame(); // Hide overlay first
                // Now, reset scores AND the game round
                playerScore = 0;
                computerScore = 0;
                updateScoreDisplay();
                resetRoundUI(true); // true for a full reset including status to "Make your move!"
            });

            buttonPauseMainMenu.setOnClickListener(v -> goToMainMenu());

            pauseOverlayContainer.setVisibility(View.GONE);
        }
    }


    private void playGame(Choice playerChoice) {
        if (isGamePaused) return;

        Choice computerChoice = Choice.values()[random.nextInt(Choice.values().length)];
        String result;
        // int playerChoiceDrawable = getDrawableForChoice(playerChoice); // Not directly used anymore for display logic
        // int computerChoiceDrawable = getDrawableForChoice(computerChoice); // Not directly used anymore

        if (playerChoice == computerChoice) {
            result = "It's a Tie!";
        } else if ((playerChoice == Choice.ROCK && computerChoice == Choice.SCISSORS) ||
                (playerChoice == Choice.PAPER && computerChoice == Choice.ROCK) ||
                (playerChoice == Choice.SCISSORS && computerChoice == Choice.PAPER)) {
            result = "You Win!";
            playerScore++;
        } else {
            result = "Computer Wins!";
            computerScore++;
        }

        updateScoreDisplay();
        displayResults(playerChoice, computerChoice, result);
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
            case ROCK: return "Rock";
            case PAPER: return "Paper";
            case SCISSORS: return "Scissors";
            default: return "";
        }
    }

    private void updateScoreDisplay() {
        binding.textViewScorePlayer.setText("Player: " + playerScore);
        binding.textViewScoreComputer.setText("Computer: " + computerScore);
    }

    private void displayResults(Choice player, Choice computer, String gameResult) {
        binding.textViewPlayerChoiceLabel.setText("Player: " + getChoiceString(player));
        binding.textViewComputerChoiceLabel.setText("Computer: " + getChoiceString(computer));
        binding.textViewVerdict.setText(gameResult);

        binding.textViewPlayerChoiceLabel.setVisibility(View.VISIBLE);
        binding.textViewVerdict.setVisibility(View.VISIBLE);
        binding.textViewComputerChoiceLabel.setVisibility(View.VISIBLE);
        binding.computerChoiceDisplayContainer.setVisibility(View.VISIBLE);

        binding.imageComputerSlotLeft.setImageDrawable(null);
        binding.imageComputerSlotCenter.setImageDrawable(null);
        binding.imageComputerSlotRight.setImageDrawable(null);

        int computerChoiceDrawable = getDrawableForChoice(computer);
        if (player == Choice.ROCK) {
            binding.imageComputerSlotLeft.setImageResource(computerChoiceDrawable);
        } else if (player == Choice.PAPER) {
            binding.imageComputerSlotCenter.setImageResource(computerChoiceDrawable);
        } else {
            binding.imageComputerSlotRight.setImageResource(computerChoiceDrawable);
        }

        binding.textViewRpsStatus.setText("Play Again?");
    }

    private void resetRoundUI(boolean fullReset) {
        binding.textViewPlayerChoiceLabel.setVisibility(View.GONE);
        binding.textViewVerdict.setVisibility(View.GONE);
        binding.textViewComputerChoiceLabel.setVisibility(View.GONE);
        binding.computerChoiceDisplayContainer.setVisibility(View.GONE);

        binding.imageComputerSlotLeft.setImageDrawable(null);
        binding.imageComputerSlotCenter.setImageDrawable(null);
        binding.imageComputerSlotRight.setImageDrawable(null);

        if (fullReset) { // If it's a full reset (like after scores are reset)
            binding.textViewRpsStatus.setText("Make your move!");
        } else if (playerScore > 0 || computerScore > 0 || binding.textViewVerdict.getVisibility() == View.VISIBLE) {
            // If a game has been played or scores exist, prompt to play again, otherwise keep "Make your move!"
            binding.textViewRpsStatus.setText("Play Again?");
        } else {
            binding.textViewRpsStatus.setText("Make your move!");
        }
    }

    // REMOVE: private void resetGame() { ... }
    // REMOVE: private void resetScores() { ... }

    private void togglePauseState() {
        if (isGamePaused) {
            resumeGame();
        } else {
            pauseGame();
        }
    }

    private void pauseGame() {
        isGamePaused = true;
        if (pauseOverlayContainer != null) {
            pauseOverlayContainer.setVisibility(View.VISIBLE);
        }
        binding.buttonRock.setEnabled(false);
        binding.buttonPaper.setEnabled(false);
        binding.buttonScissors.setEnabled(false);
        // REMOVE: binding.buttonResetRpsGame.setEnabled(false);
        // REMOVE: binding.buttonResetRpsScores.setEnabled(false);
    }

    private void resumeGame() {
        isGamePaused = false;
        if (pauseOverlayContainer != null) {
            pauseOverlayContainer.setVisibility(View.GONE);
        }
        binding.buttonRock.setEnabled(true);
        binding.buttonPaper.setEnabled(true);
        binding.buttonScissors.setEnabled(true);
        // REMOVE: binding.buttonResetRpsGame.setEnabled(true);
        // REMOVE: binding.buttonResetRpsScores.setEnabled(true);
    }

    private void goToMainMenu() {
        if (isGamePaused) {
            resumeGame();
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_rps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_help_rps) {
            Toast.makeText(this, "Rock beats Scissors, Scissors beats Paper, Paper beats Rock!", Toast.LENGTH_LONG).show();
            return true;
        } else if (itemId == R.id.action_pause_rps) {
            togglePauseState();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isGamePaused) {
            resumeGame();
        } else {
            super.onBackPressed();
        }
    }
}