package com.example.jarm;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.jarm.databinding.FragmentStatsBinding;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.Locale;

public class StatsFragment extends Fragment {

    private FragmentStatsBinding binding;
    private StatsDbHelper dbHelper;
    private String currentArithmeticDifficulty = "Easy";
    private static final String TAG = "StatsFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        dbHelper = new StatsDbHelper(getContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        currentArithmeticDifficulty = getString(R.string.difficulty_easy);
        loadAllStats();

        binding.toggleGroupArithmeticDifficulty.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (isChecked) {
                    if (checkedId == R.id.button_stats_arithmetic_easy) {
                        currentArithmeticDifficulty = getString(R.string.difficulty_easy);
                    } else if (checkedId == R.id.button_stats_arithmetic_medium) {
                        currentArithmeticDifficulty = getString(R.string.difficulty_medium);
                    } else if (checkedId == R.id.button_stats_arithmetic_hard) {
                        currentArithmeticDifficulty = getString(R.string.difficulty_hard);
                    }
                    loadArithmeticStats(currentArithmeticDifficulty);
                }
            }
        });
        binding.toggleGroupArithmeticDifficulty.check(R.id.button_stats_arithmetic_easy);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllStats();
    }

    private void loadAllStats() {
        loadTicTacToeStats();
        loadRpsStats();
        loadArithmeticStats(currentArithmeticDifficulty);
    }

    private void loadTicTacToeStats() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        int totalGames = 0;
        int playerXWins = 0;
        int playerOWins = 0;
        int draws = 0;

        // For VsBot specific stats
        int playerWinsVsBotAsX = 0; // Player X wins vs Bot
        int playerWinsVsBotAsO = 0; // Player O wins vs Bot
        int botWinsVsPlayerX = 0;   // Bot (as O) wins vs Player X
        int botWinsVsPlayerO = 0;   // Bot (as X) wins vs Player O
        int drawsVsBot = 0;

        try {
            cursor = db.query(
                    StatsDbHelper.TicTacToeEntry.TABLE_NAME,
                    new String[]{StatsDbHelper.TicTacToeEntry.COLUMN_NAME_WINNER, StatsDbHelper.TicTacToeEntry.COLUMN_NAME_MODE},
                    null, null, null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                totalGames = cursor.getCount();
                do {
                    String winner = cursor.getString(cursor.getColumnIndexOrThrow(StatsDbHelper.TicTacToeEntry.COLUMN_NAME_WINNER));
                    String mode = cursor.getString(cursor.getColumnIndexOrThrow(StatsDbHelper.TicTacToeEntry.COLUMN_NAME_MODE));

                    if ("X".equals(winner)) playerXWins++;
                    else if ("O".equals(winner)) playerOWins++;
                    else if ("DRAW".equals(winner)) draws++;

                    if ("VsBot".equals(mode)) {
                        if ("X".equals(winner)) {
                            playerWinsVsBotAsX++; // Human played as X and won
                        } else if ("O".equals(winner)) {
                            botWinsVsPlayerX++; // Bot (as O) won against Human (X) OR Human played as O and won (this needs careful thought based on your TicTacToeActivity bot logic)
                            // Let's assume Bot is always O in TicTacToeActivity for simplicity of stats here.
                            // So, if O wins in VsBot, it's a bot win.
                            // If your bot can be X or O, this logic needs refinement in TicTacToeActivity save and here.
                            // For now, assuming Bot is always O.
                            // If winner is O in VsBot, it means Bot won.
                        } else if ("DRAW".equals(winner)) {
                            drawsVsBot++;
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error querying TTT stats: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        binding.textStatsTttPlayerXWinsValue.setText(String.valueOf(playerXWins));
        binding.textStatsTttPlayerOWinsValue.setText(String.valueOf(playerOWins));
        binding.textStatsTttDrawsValue.setText(String.valueOf(draws));

        // Player X Winrate: (X wins) / (Total games where X played and didn't draw)
        // Player O Winrate: (O wins) / (Total games where O played and didn't draw)
        // "Total games played by X" = totalGames - games_where_O_won_vs_bot_if_X_was_not_playing - draws
        // This can get complicated if players switch. For simplicity:
        // Winrate for X = X_wins / (TotalGames - Draws)
        // Winrate for O = O_wins / (TotalGames - Draws)
        // This definition means their winrates are based on decisive games they could have won.

        int decisiveGames = totalGames - draws;
        double tttWinRateX = (decisiveGames > 0) ? ((double) playerXWins / decisiveGames) * 100 : 0.0;
        if(playerXWins == 0 && decisiveGames == 0 && draws > 0 && totalGames > 0) tttWinRateX = 0.0; // only draws, no decisive games for X
        else if(playerXWins == 0 && decisiveGames == 0 && totalGames == 0) tttWinRateX = 0.0; // no games

        double tttWinRateO = (decisiveGames > 0) ? ((double) playerOWins / decisiveGames) * 100 : 0.0;
        if(playerOWins == 0 && decisiveGames == 0 && draws > 0 && totalGames > 0) tttWinRateO = 0.0; // only draws, no decisive games for O
        else if(playerOWins == 0 && decisiveGames == 0 && totalGames == 0) tttWinRateO = 0.0; // no games


        binding.textStatsTttPlayerXWinrateValue.setText(String.format(Locale.getDefault(), "%.1f%%", tttWinRateX));
        binding.textStatsTttPlayerOWinrateValue.setText(String.format(Locale.getDefault(), "%.1f%%", tttWinRateO));

        // Vs Bot Stats
        // Assuming Bot is always Player O in TicTacToeActivity for these stats.
        // Bot Wins (as O) = Count of (winner='O' AND mode='VsBot')
        // Bot Losses (as O) = Count of (winner='X' AND mode='VsBot') Human player X won
        // Bot Draws = Count of (winner='DRAW' AND mode='VsBot')

        int botWinsAsO_vsBotMode = 0;
        int playerXWins_vsBotMode = 0; // Human as X wins
        // drawsVsBot is already calculated

        // Re-query for more specific VsBot stats if needed, or refine the first loop.
        // For simplicity with current loop:
        // Bot Wins (You as P1/P2): This label is a bit ambiguous.
        // Let's interpret it as:
        // "Bot Wins": How many times the Bot won. (Calculated as 'O' wins in 'VsBot' mode)
        // "Bot Losses": How many times the Bot lost (i.e., human player won against Bot). (Calculated as 'X' wins in 'VsBot' mode)

        SQLiteDatabase db2 = dbHelper.getReadableDatabase(); // Use a new instance or ensure the first one is still open
        Cursor vsBotCursor = null;
        try {
            String selection = StatsDbHelper.TicTacToeEntry.COLUMN_NAME_MODE + " = ?";
            String[] selectionArgs = {"VsBot"};
            vsBotCursor = db2.query(
                    StatsDbHelper.TicTacToeEntry.TABLE_NAME,
                    new String[]{StatsDbHelper.TicTacToeEntry.COLUMN_NAME_WINNER},
                    selection, selectionArgs, null, null, null
            );
            if (vsBotCursor != null && vsBotCursor.moveToFirst()) {
                do {
                    String winner = vsBotCursor.getString(vsBotCursor.getColumnIndexOrThrow(StatsDbHelper.TicTacToeEntry.COLUMN_NAME_WINNER));
                    if ("O".equals(winner)) { // Assuming Bot is O
                        botWinsAsO_vsBotMode++;
                    } else if ("X".equals(winner)) { // Assuming Human is X
                        playerXWins_vsBotMode++;
                    }
                    // drawsVsBot is already counted from the main loop if mode is VsBot
                } while (vsBotCursor.moveToNext());
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error querying TTT VsBot specific stats: " + e.getMessage());
        } finally {
            if (vsBotCursor != null) {
                vsBotCursor.close();
            }
        }


        binding.textStatsTttBotWinsValue.setText(String.valueOf(botWinsAsO_vsBotMode));
        binding.textStatsTttBotLossesValue.setText(String.valueOf(playerXWins_vsBotMode));
        binding.textStatsTttBotDrawsValue.setText(String.valueOf(drawsVsBot)); // Already calculated from the first loop correctly for draws in VsBot
    }


    private void loadRpsStats() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        int totalRounds = 0;
        int playerWins = 0;
        int playerLosses = 0;
        int ties = 0;
        int rockPicks = 0;
        int paperPicks = 0;
        int scissorsPicks = 0;

        try {
            cursor = db.query(
                    StatsDbHelper.RpsEntry.TABLE_NAME,
                    new String[]{
                            StatsDbHelper.RpsEntry.COLUMN_NAME_PLAYER_CHOICE,
                            StatsDbHelper.RpsEntry.COLUMN_NAME_ROUND_OUTCOME
                    },
                    null, null, null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                totalRounds = cursor.getCount();
                do {
                    String playerChoice = cursor.getString(cursor.getColumnIndexOrThrow(StatsDbHelper.RpsEntry.COLUMN_NAME_PLAYER_CHOICE));
                    String outcome = cursor.getString(cursor.getColumnIndexOrThrow(StatsDbHelper.RpsEntry.COLUMN_NAME_ROUND_OUTCOME));

                    if ("WIN".equals(outcome)) playerWins++;
                    else if ("LOSE".equals(outcome)) playerLosses++;
                    else if ("TIE".equals(outcome)) ties++;

                    if ("ROCK".equals(playerChoice)) rockPicks++;
                    else if ("PAPER".equals(playerChoice)) paperPicks++;
                    else if ("SCISSORS".equals(playerChoice)) scissorsPicks++;

                } while (cursor.moveToNext());
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error querying RPS stats: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        binding.textStatsRpsWinsValue.setText(String.valueOf(playerWins));
        binding.textStatsRpsLossesValue.setText(String.valueOf(playerLosses));
        binding.textStatsRpsTiesValue.setText(String.valueOf(ties));

        binding.textStatsRpsRockPicksValue.setText(String.valueOf(rockPicks));
        binding.textStatsRpsPaperPicksValue.setText(String.valueOf(paperPicks));
        binding.textStatsRpsScissorsPicksValue.setText(String.valueOf(scissorsPicks));

        double rpsWinRate = 0.0;
        if (playerWins + playerLosses > 0) { // Only calculate if there are wins or losses
            rpsWinRate = ((double) playerWins / (playerWins + playerLosses)) * 100;
        }
        // If totalRounds > 0 and (playerWins + playerLosses == 0), it means only ties, so winrate is 0.
        // If totalRounds == 0, winrate is 0.

        binding.textStatsRpsWinrateValue.setText(String.format(Locale.getDefault(), "%.1f%%", rpsWinRate));
    }

    private void loadArithmeticStats(String difficulty) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor sessionCursor = null;
        Cursor roundCursor = null;

        double totalPoints = 0;
        int sessionCount = 0;
        long totalTimeTakenMs = 0;
        int roundsCount = 0;
        long fastestTimeMs = Long.MAX_VALUE;
        long slowestTimeMs = 0;

        try {
            String sessionSelection = StatsDbHelper.ArithmeticSessionEntry.COLUMN_NAME_DIFFICULTY + " = ?";
            String[] sessionSelectionArgs = { difficulty };

            sessionCursor = db.query(
                    StatsDbHelper.ArithmeticSessionEntry.TABLE_NAME,
                    new String[]{StatsDbHelper.ArithmeticSessionEntry.COLUMN_NAME_FINAL_SCORE, StatsDbHelper.ArithmeticSessionEntry._ID},
                    sessionSelection, sessionSelectionArgs, null, null, null
            );

            if (sessionCursor != null && sessionCursor.moveToFirst()) {
                sessionCount = sessionCursor.getCount();
                do {
                    totalPoints += sessionCursor.getInt(sessionCursor.getColumnIndexOrThrow(StatsDbHelper.ArithmeticSessionEntry.COLUMN_NAME_FINAL_SCORE));
                    long sessionId = sessionCursor.getLong(sessionCursor.getColumnIndexOrThrow(StatsDbHelper.ArithmeticSessionEntry._ID));

                    String roundSelection = StatsDbHelper.ArithmeticRoundEntry.COLUMN_NAME_SESSION_ID + " = ?";
                    String[] roundSelectionArgs = { String.valueOf(sessionId) };
                    roundCursor = db.query(
                            StatsDbHelper.ArithmeticRoundEntry.TABLE_NAME,
                            new String[]{StatsDbHelper.ArithmeticRoundEntry.COLUMN_NAME_TIME_TAKEN_MS},
                            roundSelection, roundSelectionArgs, null, null, null
                    );

                    boolean sessionHasRounds = false;
                    if (roundCursor != null && roundCursor.moveToFirst()) {
                        sessionHasRounds = true;
                        roundsCount += roundCursor.getCount(); // Add to total rounds count for this difficulty
                        do {
                            long timeTaken = roundCursor.getLong(roundCursor.getColumnIndexOrThrow(StatsDbHelper.ArithmeticRoundEntry.COLUMN_NAME_TIME_TAKEN_MS));
                            totalTimeTakenMs += timeTaken;
                            if (timeTaken < fastestTimeMs) fastestTimeMs = timeTaken;
                            if (timeTaken > slowestTimeMs) slowestTimeMs = timeTaken;
                        } while (roundCursor.moveToNext());
                    }
                    if (roundCursor != null) roundCursor.close();
                    // If a session has no rounds, its times won't contribute, which is correct.

                } while (sessionCursor.moveToNext());
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error querying Arithmetic stats: " + e.getMessage());
        } finally {
            if (sessionCursor != null) {
                sessionCursor.close();
            }
        }

        double avgPoints = (sessionCount > 0) ? totalPoints / sessionCount : 0;
        binding.textStatsArithmeticAvgPointsValue.setText(String.format(Locale.getDefault(), "%.0f", avgPoints));

        double avgSpeedSeconds = (roundsCount > 0) ? (totalTimeTakenMs / 1000.0) / roundsCount : 0;
        binding.textStatsArithmeticAvgSpeedValue.setText(String.format(Locale.getDefault(), "%.1fs", avgSpeedSeconds));

        if (fastestTimeMs == Long.MAX_VALUE || roundsCount == 0) fastestTimeMs = 0;
        binding.textStatsArithmeticFastestAnswerValue.setText(String.format(Locale.getDefault(), "%.1fs", fastestTimeMs / 1000.0));

        if (roundsCount == 0) slowestTimeMs = 0; // Ensure slowest is 0 if no rounds
        binding.textStatsArithmeticSlowestAnswerValue.setText(String.format(Locale.getDefault(), "%.1fs", slowestTimeMs / 1000.0));
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dbHelper != null) {
            dbHelper.close();
        }
        binding = null;
    }
}
