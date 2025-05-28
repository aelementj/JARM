package com.example.jarm; // Ensure this matches your package name

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity; // Make sure it extends AppCompatActivity
import androidx.appcompat.widget.Toolbar;

import com.example.jarm.databinding.ActivityTicTacToeBinding; // Import ViewBinding class

public class TicTacToeActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityTicTacToeBinding binding; // ViewBinding instance

    // Game state
    private static final int PLAYER_X = 1;
    private static final int PLAYER_O = 2;
    private static final int EMPTY_CELL = 0;

    private final int[][] board = new int[3][3]; // 0 for empty, 1 for X, 2 for O
    private int currentPlayer;
    private boolean gameActive;

    private int scoreX = 0;
    private int scoreO = 0;
    private int scoreTies = 0;

    private final ImageButton[][] cellButtons = new ImageButton[3][3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTicTacToeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbarTicTacToe; // Access via binding
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            // Title is set by the TextView in XML, so no setTitle here
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        initializeBoard();
        setupClickListeners();
        startGame();
    }

    private void initializeBoard() {
        // Store buttons in a 2D array for easier access
        cellButtons[0][0] = binding.button00;
        cellButtons[0][1] = binding.button01;
        cellButtons[0][2] = binding.button02;
        cellButtons[1][0] = binding.button10;
        cellButtons[1][1] = binding.button11;
        cellButtons[1][2] = binding.button12;
        cellButtons[2][0] = binding.button20;
        cellButtons[2][1] = binding.button21;
        cellButtons[2][2] = binding.button22;
    }

    private void setupClickListeners() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                cellButtons[i][j].setOnClickListener(this);
                // Set tags to identify row and column, useful in onClick
                cellButtons[i][j].setTag(R.id.tag_row, i);
                cellButtons[i][j].setTag(R.id.tag_col, j);
            }
        }
        binding.buttonResetGame.setOnClickListener(v -> resetBoard()); // Clear Board button
        binding.buttonResetScoreboard.setOnClickListener(v -> resetScores()); // Clear Scores button
    }

    private void startGame() {
        currentPlayer = PLAYER_X;
        gameActive = true;
        clearBoardUI(); // Clear visual board
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = EMPTY_CELL; // Clear logical board
            }
        }
        updateStatusText();
        updateScoreTextViews(); // Update scores in case they were reset
    }

    private void clearBoardUI() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                cellButtons[i][j].setImageDrawable(null);
                cellButtons[i][j].setEnabled(true);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (!gameActive || !(v instanceof ImageButton)) {
            return;
        }

        ImageButton clickedCell = (ImageButton) v;

        // Retrieve row and col from tags
        Integer rowTag = (Integer) clickedCell.getTag(R.id.tag_row);
        Integer colTag = (Integer) clickedCell.getTag(R.id.tag_col);

        if (rowTag == null || colTag == null) return; // Should not happen if tags are set

        int row = rowTag;
        int col = colTag;


        if (board[row][col] == EMPTY_CELL) {
            board[row][col] = currentPlayer;
            if (currentPlayer == PLAYER_X) {
                clickedCell.setImageResource(R.drawable.ic_tic_tac_toe_x); // Your X drawable
            } else {
                clickedCell.setImageResource(R.drawable.ic_tic_tac_toe_o); // Your O drawable
            }
            clickedCell.setEnabled(false);

            if (checkForWin(row, col)) {
                gameActive = false;
                if (currentPlayer == PLAYER_X) {
                    binding.textViewStatus.setText(getString(R.string.tic_tac_toe_status_winner, "X"));
                    scoreX++;
                } else {
                    binding.textViewStatus.setText(getString(R.string.tic_tac_toe_status_winner, "O"));
                    scoreO++;
                }
                updateScoreTextViews();
            } else if (isBoardFull()) {
                gameActive = false;
                binding.textViewStatus.setText(getString(R.string.tic_tac_toe_status_draw));
                scoreTies++;
                updateScoreTextViews();
            } else {
                switchPlayer();
                updateStatusText();
            }
        }
    }

    private boolean checkForWin(int lastRow, int lastCol) {
        // Check row
        if (board[lastRow][0] == currentPlayer && board[lastRow][1] == currentPlayer && board[lastRow][2] == currentPlayer) {
            return true;
        }
        // Check column
        if (board[0][lastCol] == currentPlayer && board[1][lastCol] == currentPlayer && board[2][lastCol] == currentPlayer) {
            return true;
        }
        // Check diagonals
        if (lastRow == lastCol) { // On main diagonal
            if (board[0][0] == currentPlayer && board[1][1] == currentPlayer && board[2][2] == currentPlayer) {
                return true;
            }
        }
        if (lastRow + lastCol == 2) { // On anti-diagonal
            return board[0][2] == currentPlayer && board[1][1] == currentPlayer && board[2][0] == currentPlayer;
        }
        return false;
    }

    private boolean isBoardFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == EMPTY_CELL) {
                    return false;
                }
            }
        }
        return true;
    }

    private void switchPlayer() {
        currentPlayer = (currentPlayer == PLAYER_X) ? PLAYER_O : PLAYER_X;
    }

    private void updateStatusText() {
        if (currentPlayer == PLAYER_X) {
            binding.textViewStatus.setText(getString(R.string.tic_tac_toe_status_player_turn, "X"));
        } else {
            binding.textViewStatus.setText(getString(R.string.tic_tac_toe_status_player_turn, "O"));
        }
    }

    private void updateScoreTextViews() {
        binding.textViewScoreX.setText(getString(R.string.tic_tac_toe_score_x, scoreX));
        binding.textViewScoreO.setText(getString(R.string.tic_tac_toe_score_o, scoreO));
        binding.textViewScoreTies.setText(getString(R.string.tic_tac_toe_score_ties, scoreTies));
    }

    private void resetBoard() { // This is "Clear Board"
        startGame(); // startGame already handles resetting the board and player
    }

    private void resetScores() { // This is "Clear Scores"
        scoreX = 0;
        scoreO = 0;
        scoreTies = 0;
        updateScoreTextViews();
        resetBoard();

        if (!gameActive) {
            updateStatusText(); // Show current player's turn again
        }
    }

    // --- Toolbar Menu Handling ---
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
            finish();
            return true;
        } else if (itemId == R.id.action_help_ttt) {
            Toast.makeText(this, "Help for Tic Tac Toe clicked!", Toast.LENGTH_SHORT).show();
            // TODO: Implement actual help action (e.g., show a dialog with rules)
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}