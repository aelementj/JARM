package com.example.jarm;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

    private static final int PLAYER_X = 1;
    private static final int PLAYER_O = 2;
    private static final int EMPTY_CELL = 0;
    private static final int BOT_PLAYER = PLAYER_O;

    private final int[][] board = new int[3][3];
    private int currentPlayer;
    private boolean gameActive;
    private boolean seriesInProgress;
    private final ImageButton[][] cellButtons = new ImageButton[3][3];

    private int selectedFirstPlayerConfig = PLAYER_X;
    private int gamesToWinSeries = 1;
    private boolean selectedVsBotMode = false;

    private int playerXSeriesScore = 0;
    private int playerOSeriesScore = 0;
    private int tiesSeriesScore = 0;
    private int currentGameInSeries = 0;

    private boolean isGamePaused = false;
    private View pauseOverlayContainer;
    private View buttonPauseResume, buttonPauseRestart, buttonPauseMainMenu;

    private View helpOverlayContainerView;
    private TextView textViewHelpOverlayTitle;
    private TextView textViewHelpOverlayContent;
    private Button buttonHelpOverlayClose;


    private StatsDbHelper dbHelper;
    private static final String TAG = "TicTacToeActivity";
    private Menu activityMenu;


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

        dbHelper = new StatsDbHelper(this);

        initializeBoardButtons();
        setupConfigPanelListeners();
        initializeDedicatedPauseOverlay();
        initializeHelpOverlay();
        updateScoreTextViews();
        setInitialUiState();
        updateFirstPlayerSelectorUI();
        updateGameModeSelectorUI();
    }

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
            if (seriesInProgress && !isGamePaused) {
                mainHandler.removeCallbacksAndMessages(null);
            }

            textViewHelpOverlayTitle.setText(title);
            textViewHelpOverlayContent.setText(content);
            helpOverlayContainerView.setVisibility(View.VISIBLE);

            setGameInteractionEnabled(false);
            if (activityMenu != null) {
                for (int i = 0; i < activityMenu.size(); i++) {
                    if(activityMenu.getItem(i).getItemId() != R.id.action_help_ttt) {
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

            if (seriesInProgress && !isGamePaused && selectedVsBotMode && currentPlayer == BOT_PLAYER && gameActive) {
                binding.textViewStatus.setText(getString(R.string.ttt_status_bot_thinking));
                mainHandler.postDelayed(this::botMove, 800);
            }
        }
    }

    protected boolean isHelpOverlayVisible() {
        return helpOverlayContainerView != null && helpOverlayContainerView.getVisibility() == View.VISIBLE;
    }

    private void setGameInteractionEnabled(boolean enabled) {
        if (binding.gridLayoutTicTacToeBoard != null) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (cellButtons[i][j] != null) {
                        cellButtons[i][j].setEnabled(enabled && board[i][j] == EMPTY_CELL);
                    }
                }
            }
        }
        boolean enableConfigInteractions = enabled && !isHelpOverlayVisible() && !isGamePaused;
        binding.buttonStartGameTtt.setEnabled(enableConfigInteractions);
        binding.buttonConfigDecreaseRoundsTtt.setEnabled(enableConfigInteractions);
        binding.buttonConfigIncreaseRoundsTtt.setEnabled(enableConfigInteractions);
        binding.textSelectPlayerX.setEnabled(enableConfigInteractions);
        binding.textSelectPlayerO.setEnabled(enableConfigInteractions);
        binding.textSelectPlayerRandom.setEnabled(enableConfigInteractions);
        binding.textSelectMode2Player.setEnabled(enableConfigInteractions);
        binding.textSelectModeVsBot.setEnabled(enableConfigInteractions);
        binding.edittextConfigRoundsTtt.setAlpha(enableConfigInteractions ? 1.0f : 0.5f);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_tic_tac_toe, menu);
        this.activityMenu = menu;
        updatePauseButtonVisibility();
        return true;
    }

    private void updatePauseButtonVisibility() {
        if (activityMenu != null) {
            MenuItem pauseItem = activityMenu.findItem(R.id.action_pause_ttt);
            if (pauseItem != null) {
                pauseItem.setVisible(seriesInProgress && !isGamePaused && !isHelpOverlayVisible());
            }
        }
    }


    private void setInitialUiState() {
        binding.configPanelTtt.setVisibility(View.VISIBLE);
        binding.gridLayoutTicTacToeBoard.setVisibility(View.GONE);
        binding.textViewStatus.setText(getString(R.string.ttt_status_setup_game));
        binding.textViewRoundCounterTtt.setVisibility(View.GONE);
        gameActive = false;
        seriesInProgress = false;
        isGamePaused = false;
        if (pauseOverlayContainer != null) {
            pauseOverlayContainer.setVisibility(View.GONE);
        }
        if (helpOverlayContainerView != null) {
            hideHelpOverlay();
        } else {
            setGameInteractionEnabled(true);
        }
        updatePauseButtonVisibility();

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
            if (isHelpOverlayVisible() || isGamePaused) return;
            readConfiguration();
            binding.configPanelTtt.setVisibility(View.GONE);
            binding.gridLayoutTicTacToeBoard.setVisibility(View.VISIBLE);
            binding.textViewRoundCounterTtt.setVisibility(View.VISIBLE);
            seriesInProgress = true;
            updatePauseButtonVisibility();
            startNewSeries();
        });

        binding.buttonConfigDecreaseRoundsTtt.setOnClickListener(v -> { if (!isHelpOverlayVisible() && !isGamePaused) updateRoundsToWin(-1); });
        binding.buttonConfigIncreaseRoundsTtt.setOnClickListener(v -> { if (!isHelpOverlayVisible() && !isGamePaused) updateRoundsToWin(1); });

        binding.textSelectPlayerX.setOnClickListener(v -> {
            if (!isHelpOverlayVisible() && !isGamePaused) {
                selectedFirstPlayerConfig = PLAYER_X;
                updateFirstPlayerSelectorUI();
            }
        });
        binding.textSelectPlayerO.setOnClickListener(v -> {
            if (!isHelpOverlayVisible() && !isGamePaused) {
                selectedFirstPlayerConfig = PLAYER_O;
                updateFirstPlayerSelectorUI();
            }
        });
        binding.textSelectPlayerRandom.setOnClickListener(v -> {
            if (!isHelpOverlayVisible() && !isGamePaused) {
                selectedFirstPlayerConfig = -1;
                updateFirstPlayerSelectorUI();
            }
        });

        binding.textSelectMode2Player.setOnClickListener(v -> {
            if (!isHelpOverlayVisible() && !isGamePaused) {
                selectedVsBotMode = false;
                updateGameModeSelectorUI();
            }
        });
        binding.textSelectModeVsBot.setOnClickListener(v -> {
            if (!isHelpOverlayVisible() && !isGamePaused) {
                selectedVsBotMode = true;
                updateGameModeSelectorUI();
            }
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
        if (!seriesInProgress) return;

        currentGameInSeries++;
        clearBoardUIAndLogic();
        setGameInteractionEnabled(true);
        gameActive = true;

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
                if(cellButtons[i][j] != null) cellButtons[i][j].setImageDrawable(null);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (isHelpOverlayVisible() || isGamePaused) return;

        if (!gameActive || !(v instanceof ImageButton)) {
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
            gameActive = false;
            handleGameWin(player);
        } else if (isBoardFull()) {
            gameActive = false;
            handleGameDraw();
        } else {
            if (gameActive) {
                switchPlayer();
                updateStatusTextForNewGame();
            }
        }
    }

    private void botMove() {
        if (!gameActive || currentPlayer != BOT_PLAYER || isGamePaused || isHelpOverlayVisible()) return;

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
        if (!gameActive || !seriesInProgress) return;
        String playerName = (currentPlayer == PLAYER_X) ? "X" : "O";
        binding.textViewStatus.setText(getString(R.string.ttt_status_player_turn_series, playerName));
    }

    private void handleGameWin(int winner) {
        String winnerString = (winner == PLAYER_X) ? "X" : "O";
        if (winner == PLAYER_X) {
            playerXSeriesScore++;
            binding.textViewStatus.setText(getString(R.string.ttt_status_winner_series, "X"));
        } else {
            playerOSeriesScore++;
            binding.textViewStatus.setText(getString(R.string.ttt_status_winner_series, "O"));
        }
        updateScoreTextViews();
        saveGameResult(winnerString);
        proceedToNextStepOrEndSeries();
    }

    private void handleGameDraw() {
        tiesSeriesScore++;
        binding.textViewStatus.setText(getString(R.string.ttt_status_draw_series));
        updateScoreTextViews();
        saveGameResult("DRAW");
        proceedToNextStepOrEndSeries();
    }

    private void saveGameResult(String winnerString) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(StatsDbHelper.TicTacToeEntry.COLUMN_NAME_WINNER, winnerString);
        String mode = selectedVsBotMode ? "VsBot" : "2P";
        values.put(StatsDbHelper.TicTacToeEntry.COLUMN_NAME_MODE, mode);

        long newRowId = db.insert(StatsDbHelper.TicTacToeEntry.TABLE_NAME, null, values);
        if (newRowId == -1) {
            Log.e(TAG, "Error saving TTT game result to DB.");
            Toast.makeText(this, "Error saving game stats.", Toast.LENGTH_SHORT).show();
        } else {
            Log.i(TAG, "TTT Game result saved with row ID: " + newRowId);
        }
    }


    private void proceedToNextStepOrEndSeries() {
        disableBoardCells();
        setGameInteractionEnabled(false);
        if (isSeriesOver()) {
            displaySeriesResult();
            binding.textViewRoundCounterTtt.setVisibility(View.GONE);
            seriesInProgress = false;
            gameActive = false;
            updatePauseButtonVisibility();
            mainHandler.postDelayed(this::setInitialUiState, 3000);
        } else {
            mainHandler.postDelayed(this::startNewGame, 2000);
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
                if(cellButtons[i][j] != null) cellButtons[i][j].setEnabled(false);
            }
        }
    }

    private void initializeDedicatedPauseOverlay() {
        pauseOverlayContainer = findViewById(R.id.pause_overlay_container);
        if (pauseOverlayContainer != null) {
            buttonPauseResume = pauseOverlayContainer.findViewById(R.id.button_pause_resume);
            buttonPauseRestart = pauseOverlayContainer.findViewById(R.id.button_pause_restart);
            buttonPauseMainMenu = pauseOverlayContainer.findViewById(R.id.button_pause_main_menu);

            ((TextView)pauseOverlayContainer.findViewById(R.id.label_pause_restart)).setText(R.string.pause_overlay_restart_ttt);

            if (buttonPauseResume != null) buttonPauseResume.setOnClickListener(v -> resumeGameFromDedicatedPause());
            if (buttonPauseRestart != null) buttonPauseRestart.setOnClickListener(v -> {
                resumeGameFromDedicatedPause();
                if (seriesInProgress) {
                    Toast.makeText(this, "Restarting match.", Toast.LENGTH_SHORT).show();
                    startNewSeries();
                } else {
                    setInitialUiState();
                }
            });
            if (buttonPauseMainMenu != null) buttonPauseMainMenu.setOnClickListener(v -> {
                resumeGameFromDedicatedPause();
                Toast.makeText(this, "Returning to game setup.", Toast.LENGTH_SHORT).show();
                setInitialUiState();
            });
            pauseOverlayContainer.setVisibility(View.GONE);
        }
    }

    private void toggleDedicatedPauseState() {
        if (!seriesInProgress || pauseOverlayContainer == null || isHelpOverlayVisible()) {
            return;
        }
        if (isGamePaused) {
            resumeGameFromDedicatedPause();
        } else {
            pauseGameForDedicatedMenu();
        }
    }

    private void pauseGameForDedicatedMenu() {
        isGamePaused = true;
        if (pauseOverlayContainer != null) {
            pauseOverlayContainer.setVisibility(View.VISIBLE);
        }
        mainHandler.removeCallbacksAndMessages(null);
        setGameInteractionEnabled(false);
        updatePauseButtonVisibility();
    }

    private void resumeGameFromDedicatedPause() {
        isGamePaused = false;
        if (pauseOverlayContainer != null) {
            pauseOverlayContainer.setVisibility(View.GONE);
        }
        setGameInteractionEnabled(true);
        updatePauseButtonVisibility();

        if (selectedVsBotMode && currentPlayer == BOT_PLAYER && gameActive && seriesInProgress) {
            binding.textViewStatus.setText(getString(R.string.ttt_status_bot_thinking));
            mainHandler.postDelayed(this::botMove, 800);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (isHelpOverlayVisible()) {
            if (itemId == R.id.action_help_ttt || itemId == android.R.id.home) {
                hideHelpOverlay();
                return true;
            }
            return false;
        }

        if (isGamePaused) {
            if (itemId == R.id.action_pause_ttt || itemId == android.R.id.home) {
            } else {
                return false;
            }
        }


        if (itemId == android.R.id.home) {
            if (isGamePaused) {
                resumeGameFromDedicatedPause();
            } else if (seriesInProgress) {
                Toast.makeText(this, "Match in progress. Returning to game setup.", Toast.LENGTH_SHORT).show();
                setInitialUiState();
            } else {
                finish();
            }
            return true;
        } else if (itemId == R.id.action_help_ttt) {
            if (isGamePaused) {
                resumeGameFromDedicatedPause();
            }
            showHelpOverlay(getString(R.string.help_title_tic_tac_toe), getString(R.string.help_content_tic_tac_toe));
            return true;
        } else if (itemId == R.id.action_pause_ttt) {
            toggleDedicatedPauseState();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isHelpOverlayVisible()) {
            hideHelpOverlay();
        } else if (isGamePaused) {
            resumeGameFromDedicatedPause();
        } else if (seriesInProgress) {
            Toast.makeText(this, "Match in progress. Returning to game setup.", Toast.LENGTH_SHORT).show();
            setInitialUiState();
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
