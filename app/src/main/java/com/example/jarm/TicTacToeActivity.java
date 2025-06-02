package com.example.jarm;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.jarm.databinding.ActivityTicTacToeBinding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TicTacToeActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityTicTacToeBinding binding;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Random random = new Random();

    // Game constants
    private static final int PLAYER_X = 1;
    private static final int PLAYER_O = 2;
    private static final int EMPTY_CELL = 0;
    private static final int BOT_PLAYER = PLAYER_O;

    // Game state variables
    private final int[][] board = new int[3][3];
    private int currentPlayer;
    private boolean gameActive; // Is a single game active?
    private boolean seriesInProgress; // Is a series (match) in progress?
    private final ImageButton[][] cellButtons = new ImageButton[3][3];

    // Series and Configuration state variables
    private int selectedFirstPlayerConfig = PLAYER_X;
    private int gamesToWinSeries = 1;
    private boolean selectedVsBotMode = false;

    private int playerXSeriesScore = 0;
    private int playerOSeriesScore = 0;
    private int tiesSeriesScore = 0;
    private int currentGameInSeries = 0;

    // Pause state
    private boolean isGamePaused = false;
    private View pauseOverlayContainer;
    private View buttonPauseResume, buttonPauseRestart, buttonPauseMainMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTicTacToeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbarTicTacToe;
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        initializeBoardButtons();
        setupConfigPanelListeners();
        initializePauseOverlay();
        updateScoreTextViews();
        setInitialUiState(); // Start on the config screen
        updateFirstPlayerSelectorUI();
        updateGameModeSelectorUI();
    }

    private void setInitialUiState() {
        binding.configPanelTtt.setVisibility(View.VISIBLE);
        binding.gridLayoutTicTacToeBoard.setVisibility(View.GONE);
        binding.textViewStatus.setText(getString(R.string.ttt_status_setup_game));
        binding.textViewRoundCounterTtt.setVisibility(View.GONE);
        gameActive = false;
        seriesInProgress = false; // No series is active when on config screen
        isGamePaused = false;
        if (pauseOverlayContainer != null) {
            pauseOverlayContainer.setVisibility(View.GONE);
        }

        // Reset scores only when going back to config screen, ensures a fresh setup
        playerXSeriesScore = 0;
        playerOSeriesScore = 0;
        tiesSeriesScore = 0;
        currentGameInSeries = 0;
        updateScoreTextViews();
    }

    private void initializeBoardButtons() {
        cellButtons[0][0] = binding.button00; cellButtons[0][1] = binding.button01; cellButtons[0][2] = binding.button02;
        cellButtons[1][0] = binding.button10; cellButtons[1][1] = binding.button11; cellButtons[1][2] = binding.button12;
        cellButtons[2][0] = binding.button20; cellButtons[2][1] = binding.button21; cellButtons[2][2] = binding.button22;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                cellButtons[i][j].setOnClickListener(this);
                cellButtons[i][j].setTag(R.id.tag_row, i);
                cellButtons[i][j].setTag(R.id.tag_col, j);
            }
        }
    }

    private void setupConfigPanelListeners() {
        binding.buttonStartGameTtt.setOnClickListener(v -> {
            readConfiguration();
            binding.configPanelTtt.setVisibility(View.GONE);
            binding.gridLayoutTicTacToeBoard.setVisibility(View.VISIBLE);
            binding.textViewRoundCounterTtt.setVisibility(View.VISIBLE);
            seriesInProgress = true; // Series starts now
            startNewSeries();
        });

        binding.buttonConfigDecreaseRoundsTtt.setOnClickListener(v -> updateRoundsToWin(-1));
        binding.buttonConfigIncreaseRoundsTtt.setOnClickListener(v -> updateRoundsToWin(1));

        binding.textSelectPlayerX.setOnClickListener(v -> {
            selectedFirstPlayerConfig = PLAYER_X;
            updateFirstPlayerSelectorUI();
        });
        binding.textSelectPlayerO.setOnClickListener(v -> {
            selectedFirstPlayerConfig = PLAYER_O;
            updateFirstPlayerSelectorUI();
        });
        binding.textSelectPlayerRandom.setOnClickListener(v -> {
            selectedFirstPlayerConfig = -1;
            updateFirstPlayerSelectorUI();
        });

        binding.textSelectMode2Player.setOnClickListener(v -> {
            selectedVsBotMode = false;
            updateGameModeSelectorUI();
        });
        binding.textSelectModeVsBot.setOnClickListener(v -> {
            selectedVsBotMode = true;
            updateGameModeSelectorUI();
        });
    }

    private void updateFirstPlayerSelectorUI() {
        TextView[] playerSelectors = {binding.textSelectPlayerX, binding.textSelectPlayerO, binding.textSelectPlayerRandom};
        for (TextView selector : playerSelectors) {
            selector.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_dark));
            selector.setTypeface(null, Typeface.NORMAL);
        }

        if (selectedFirstPlayerConfig == PLAYER_X) {
            binding.textSelectPlayerX.setTextColor(ContextCompat.getColor(this, R.color.purple_500));
            binding.textSelectPlayerX.setTypeface(null, Typeface.BOLD);
        } else if (selectedFirstPlayerConfig == PLAYER_O) {
            binding.textSelectPlayerO.setTextColor(ContextCompat.getColor(this, R.color.purple_500));
            binding.textSelectPlayerO.setTypeface(null, Typeface.BOLD);
        } else {
            binding.textSelectPlayerRandom.setTextColor(ContextCompat.getColor(this, R.color.purple_500));
            binding.textSelectPlayerRandom.setTypeface(null, Typeface.BOLD);
        }
    }

    private void updateGameModeSelectorUI() {
        if (selectedVsBotMode) {
            binding.textSelectModeVsBot.setTextColor(ContextCompat.getColor(this, R.color.purple_500));
            binding.textSelectModeVsBot.setTypeface(null, Typeface.BOLD);
            binding.textSelectMode2Player.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_dark));
            binding.textSelectMode2Player.setTypeface(null, Typeface.NORMAL);
        } else {
            binding.textSelectMode2Player.setTextColor(ContextCompat.getColor(this, R.color.purple_500));
            binding.textSelectMode2Player.setTypeface(null, Typeface.BOLD);
            binding.textSelectModeVsBot.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_dark));
            binding.textSelectModeVsBot.setTypeface(null, Typeface.NORMAL);
        }
    }


    private void readConfiguration() {
        try {
            gamesToWinSeries = Integer.parseInt(binding.edittextConfigRoundsTtt.getText().toString());
            if (gamesToWinSeries < 1) gamesToWinSeries = 1;
        } catch (NumberFormatException e) {
            gamesToWinSeries = 1;
        }
        binding.edittextConfigRoundsTtt.setText(String.valueOf(gamesToWinSeries));
    }

    private void updateRoundsToWin(int delta) {
        try {
            int currentRounds = Integer.parseInt(binding.edittextConfigRoundsTtt.getText().toString());
            currentRounds += delta;
            if (currentRounds < 1) currentRounds = 1;
            if (currentRounds > 99) currentRounds = 99;
            binding.edittextConfigRoundsTtt.setText(String.valueOf(currentRounds));
        } catch (NumberFormatException e) {
            binding.edittextConfigRoundsTtt.setText("1");
        }
    }

    private void startNewSeries() {
        playerXSeriesScore = 0;
        playerOSeriesScore = 0;
        tiesSeriesScore = 0;
        currentGameInSeries = 0;
        updateScoreTextViews();
        startNewGame();
    }

    private void startNewGame() {
        if (!seriesInProgress) return; // Don't start a new game if no series is active (e.g., on config screen)

        currentGameInSeries++;
        clearBoardUIAndLogic();
        gameActive = true; // A single game is now active

        binding.textViewRoundCounterTtt.setText(getString(R.string.ttt_round_counter_format, currentGameInSeries, gamesToWinSeries));

        if (selectedFirstPlayerConfig == -1) {
            currentPlayer = random.nextBoolean() ? PLAYER_X : PLAYER_O;
        } else {
            if (currentGameInSeries == 1) {
                currentPlayer = selectedFirstPlayerConfig;
            } else {
                if (selectedFirstPlayerConfig == PLAYER_X) {
                    currentPlayer = (currentGameInSeries % 2 == 1) ? PLAYER_X : PLAYER_O;
                } else {
                    currentPlayer = (currentGameInSeries % 2 == 1) ? PLAYER_O : PLAYER_X;
                }
            }
        }

        updateStatusTextForNewGame();

        if (selectedVsBotMode && currentPlayer == BOT_PLAYER) {
            binding.textViewStatus.setText(getString(R.string.ttt_status_bot_thinking));
            mainHandler.postDelayed(this::botMove, 1000);
        }
    }


    private void clearBoardUIAndLogic() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = EMPTY_CELL;
                cellButtons[i][j].setImageDrawable(null);
                cellButtons[i][j].setEnabled(true);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (!gameActive || isGamePaused || !(v instanceof ImageButton)) {
            return;
        }
        if (selectedVsBotMode && currentPlayer == BOT_PLAYER) return;

        ImageButton clickedCell = (ImageButton) v;
        Integer rowTag = (Integer) clickedCell.getTag(R.id.tag_row);
        Integer colTag = (Integer) clickedCell.getTag(R.id.tag_col);

        if (rowTag == null || colTag == null) return;
        int row = rowTag;
        int col = colTag;

        if (board[row][col] == EMPTY_CELL) {
            makeMove(row, col, currentPlayer);
            if (!gameActive) return;

            if (selectedVsBotMode && currentPlayer == BOT_PLAYER && gameActive) {
                binding.textViewStatus.setText(getString(R.string.ttt_status_bot_thinking));
                mainHandler.postDelayed(this::botMove, 800);
            }
        }
    }

    private void makeMove(int row, int col, int player) {
        board[row][col] = player;
        cellButtons[row][col].setImageResource(player == PLAYER_X ? R.drawable.ic_tic_tac_toe_x : R.drawable.ic_tic_tac_toe_o);
        cellButtons[row][col].setEnabled(false);

        if (checkForWin(row, col)) {
            gameActive = false; // Single game ends
            handleGameWin(player);
        } else if (isBoardFull()) {
            gameActive = false; // Single game ends
            handleGameDraw();
        } else {
            if (gameActive) { // Should always be true here unless something went wrong
                switchPlayer();
                updateStatusTextForNewGame();
            }
        }
    }

    private void botMove() {
        if (!gameActive || currentPlayer != BOT_PLAYER || isGamePaused) return;

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[r][c] == EMPTY_CELL) {
                    board[r][c] = BOT_PLAYER;
                    if (checkForWin(r, c)) {
                        makeMove(r, c, BOT_PLAYER);
                        return;
                    }
                    board[r][c] = EMPTY_CELL;
                }
            }
        }

        int humanPlayer = (BOT_PLAYER == PLAYER_X) ? PLAYER_O : PLAYER_X;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[r][c] == EMPTY_CELL) {
                    board[r][c] = humanPlayer;
                    if (checkForWin(r, c)) {
                        board[r][c] = EMPTY_CELL;
                        makeMove(r, c, BOT_PLAYER);
                        return;
                    }
                    board[r][c] = EMPTY_CELL;
                }
            }
        }

        List<int[]> emptyCells = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == EMPTY_CELL) {
                    emptyCells.add(new int[]{i, j});
                }
            }
        }

        if (!emptyCells.isEmpty()) {
            Collections.shuffle(emptyCells);
            int[] randomCell = emptyCells.get(0);
            makeMove(randomCell[0], randomCell[1], BOT_PLAYER);
        }
    }


    private boolean checkForWin(int lastRow, int lastCol) {
        int player = board[lastRow][lastCol];
        if (player == EMPTY_CELL) return false;

        if (board[lastRow][0] == player && board[lastRow][1] == player && board[lastRow][2] == player) return true;
        if (board[0][lastCol] == player && board[1][lastCol] == player && board[2][lastCol] == player) return true;
        if (lastRow == lastCol) {
            if (board[0][0] == player && board[1][1] == player && board[2][2] == player) return true;
        }
        if (lastRow + lastCol == 2) {
            if (board[0][2] == player && board[1][1] == player && board[2][0] == player) return true;
        }
        return false;
    }

    private boolean isBoardFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == EMPTY_CELL) return false;
            }
        }
        return true;
    }

    private void switchPlayer() {
        currentPlayer = (currentPlayer == PLAYER_X) ? PLAYER_O : PLAYER_X;
    }

    private void updateStatusTextForNewGame() {
        if (!gameActive || !seriesInProgress) return; // Only update if a game and series are active
        String playerName = (currentPlayer == PLAYER_X) ? "X" : "O";
        binding.textViewStatus.setText(getString(R.string.ttt_status_player_turn_series, playerName));
    }

    private void handleGameWin(int winner) {
        if (winner == PLAYER_X) {
            playerXSeriesScore++;
            binding.textViewStatus.setText(getString(R.string.ttt_status_winner_series, "X"));
        } else {
            playerOSeriesScore++;
            binding.textViewStatus.setText(getString(R.string.ttt_status_winner_series, "O"));
        }
        updateScoreTextViews();
        proceedToNextStepOrEndSeries();
    }

    private void handleGameDraw() {
        tiesSeriesScore++;
        binding.textViewStatus.setText(getString(R.string.ttt_status_draw_series));
        updateScoreTextViews();
        proceedToNextStepOrEndSeries();
    }

    private void proceedToNextStepOrEndSeries() {
        disableBoardCells(); // Disable after current game ends
        if (isSeriesOver()) {
            displaySeriesResult();
            binding.textViewRoundCounterTtt.setVisibility(View.GONE);
            seriesInProgress = false; // Series ends here
            gameActive = false; // No single game is active either
            mainHandler.postDelayed(this::setInitialUiState, 3000); // Go to config after showing result
        } else {
            mainHandler.postDelayed(this::startNewGame, 2000); // Start next game in the series
        }
    }

    private boolean isSeriesOver() {
        return playerXSeriesScore >= gamesToWinSeries || playerOSeriesScore >= gamesToWinSeries;
    }

    private void displaySeriesResult() {
        if (playerXSeriesScore > playerOSeriesScore) {
            binding.textViewStatus.setText(getString(R.string.ttt_status_match_winner, "X"));
        } else if (playerOSeriesScore > playerXSeriesScore) {
            binding.textViewStatus.setText(getString(R.string.ttt_status_match_winner, "O"));
        } else {
            binding.textViewStatus.setText(getString(R.string.ttt_status_match_draw));
        }
    }


    private void updateScoreTextViews() {
        binding.textViewScoreX.setText(getString(R.string.tic_tac_toe_score_x, playerXSeriesScore));
        binding.textViewScoreO.setText(getString(R.string.tic_tac_toe_score_o, playerOSeriesScore));
        binding.textViewScoreTies.setText(getString(R.string.tic_tac_toe_score_ties, tiesSeriesScore));
    }

    private void disableBoardCells() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                cellButtons[i][j].setEnabled(false);
            }
        }
    }

    private void initializePauseOverlay() {
        pauseOverlayContainer = findViewById(R.id.pause_overlay_container);
        if (pauseOverlayContainer != null) {
            buttonPauseResume = pauseOverlayContainer.findViewById(R.id.button_pause_resume);
            buttonPauseRestart = pauseOverlayContainer.findViewById(R.id.button_pause_restart);
            buttonPauseMainMenu = pauseOverlayContainer.findViewById(R.id.button_pause_main_menu);

            ((TextView)pauseOverlayContainer.findViewById(R.id.label_pause_restart)).setText(R.string.pause_overlay_restart_ttt);

            if (buttonPauseResume != null) buttonPauseResume.setOnClickListener(v -> resumeGame());
            if (buttonPauseRestart != null) buttonPauseRestart.setOnClickListener(v -> {
                resumeGame();
                if (seriesInProgress) { // Check if a series was in progress
                    Toast.makeText(this, "Restarting match.", Toast.LENGTH_SHORT).show();
                    startNewSeries(); // This will reset scores, game count and start game 1
                } else {
                    // If no series was in progress (e.g., on config screen), just ensure config is shown
                    setInitialUiState();
                }
            });
            if (buttonPauseMainMenu != null) buttonPauseMainMenu.setOnClickListener(v -> {
                resumeGame();
                Toast.makeText(this, "Returning to game setup.", Toast.LENGTH_SHORT).show();
                setInitialUiState(); // Go back to config screen, resetting the match
            });
            pauseOverlayContainer.setVisibility(View.GONE);
        }
    }

    private void togglePauseState() {
        if (!seriesInProgress || pauseOverlayContainer == null) { // Only allow pause if a series is active
            return;
        }
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
        mainHandler.removeCallbacksAndMessages(null);
    }

    private void resumeGame() {
        isGamePaused = false;
        if (pauseOverlayContainer != null) {
            pauseOverlayContainer.setVisibility(View.GONE);
        }
        if (selectedVsBotMode && currentPlayer == BOT_PLAYER && gameActive && seriesInProgress) {
            binding.textViewStatus.setText(getString(R.string.ttt_status_bot_thinking));
            mainHandler.postDelayed(this::botMove, 800);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_tic_tac_toe, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            if (seriesInProgress) { // If a series/match is active (grid is visible)
                Toast.makeText(this, "Returning to game setup.", Toast.LENGTH_SHORT).show();
                setInitialUiState(); // Go to config, reset match
            } else { // On config screen
                finish(); // Go back to MainActivity
            }
            return true;
        } else if (itemId == R.id.action_help_ttt) {
            Toast.makeText(this, "Get 3 in a row to win. Win 'Games to Win Match' to win the series!", Toast.LENGTH_LONG).show();
            return true;
        } else if (itemId == R.id.action_pause_ttt) {
            togglePauseState();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isGamePaused) {
            resumeGame();
        } else if (seriesInProgress) { // If a series/match is active (grid is or was visible)
            Toast.makeText(this, "Returning to game setup.", Toast.LENGTH_SHORT).show();
            setInitialUiState(); // Go to config, reset match
        }
        else { // On config screen
            super.onBackPressed(); // Default: finish activity
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
    }
}