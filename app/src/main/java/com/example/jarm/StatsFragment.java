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

        int playerWinsVsBotAsX = 0;
        int playerWinsVsBotAsO = 0;
        int botWinsVsPlayerX = 0;
        int botWinsVsPlayerO = 0;
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
                            playerWinsVsBotAsX++;
                        } else if ("O".equals(winner)) {
                            botWinsVsPlayerX++;
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


        int decisiveGames = totalGames - draws;
        double tttWinRateX = (decisiveGames > 0) ? ((double) playerXWins / decisiveGames) * 100 : 0.0;
        if(playerXWins == 0 && decisiveGames == 0 && draws > 0 && totalGames > 0) tttWinRateX = 0.0;
        else if(playerXWins == 0 && decisiveGames == 0 && totalGames == 0) tttWinRateX = 0.0;

        double tttWinRateO = (decisiveGames > 0) ? ((double) playerOWins / decisiveGames) * 100 : 0.0;
        if(playerOWins == 0 && decisiveGames == 0 && draws > 0 && totalGames > 0) tttWinRateO = 0.0;
        else if(playerOWins == 0 && decisiveGames == 0 && totalGames == 0) tttWinRateO = 0.0;

        binding.textStatsTttPlayerXWinrateValue.setText(String.format(Locale.getDefault(), "%.1f%%", tttWinRateX));
        binding.textStatsTttPlayerOWinrateValue.setText(String.format(Locale.getDefault(), "%.1f%%", tttWinRateO));


        int botWinsAsO_vsBotMode = 0;
        int playerXWins_vsBotMode = 0;

        SQLiteDatabase db2 = dbHelper.getReadableDatabase();
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
                    if ("O".equals(winner)) {
                        botWinsAsO_vsBotMode++;
                    } else if ("X".equals(winner)) {
                        playerXWins_vsBotMode++;
                    }
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
        binding.textStatsTttBotDrawsValue.setText(String.valueOf(drawsVsBot));
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
        if (playerWins + playerLosses > 0) {
            rpsWinRate = ((double) playerWins / (playerWins + playerLosses)) * 100;
        }

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
                        roundsCount += roundCursor.getCount();
                        do {
                            long timeTaken = roundCursor.getLong(roundCursor.getColumnIndexOrThrow(StatsDbHelper.ArithmeticRoundEntry.COLUMN_NAME_TIME_TAKEN_MS));
                            totalTimeTakenMs += timeTaken;
                            if (timeTaken < fastestTimeMs) fastestTimeMs = timeTaken;
                            if (timeTaken > slowestTimeMs) slowestTimeMs = timeTaken;
                        } while (roundCursor.moveToNext());
                    }
                    if (roundCursor != null) roundCursor.close();

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

        if (roundsCount == 0) slowestTimeMs = 0;
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
