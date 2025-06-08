package com.example.jarm;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jarm.databinding.ActivityArithmeticGameBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ArithmeticGameActivity extends AppCompatActivity {

    private ActivityArithmeticGameBinding binding;
    private String currentDifficulty;

    private int currentRound = 0;
    private int totalRounds = 10;
    private int score = 0;
    private String currentProblemExpression = "";
    private int currentCorrectAnswer = 0;

    private CountDownTimer roundTimer;
    private long timePerRoundMillisAssigned = 30000;
    private long timeLeftInMillis = 0;
    private long bonusTimeForNextRoundMillis = 0;

    private RecyclerView recyclerViewRoundHistory;
    private RoundHistoryAdapter roundHistoryAdapter;
    private List<RoundHistoryItem> roundHistoryList = new ArrayList<>();

    private Random random = new Random();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());


    private static final long TIME_PER_NUMBER_EASY_MS = 2500L;
    private static final long BASE_TIME_EASY_MS = 25000L;
    private static final long TIME_PER_NUMBER_MEDIUM_MS = 5000L;
    private static final long BASE_TIME_MEDIUM_MS = 20000L;
    private static final long TIME_PER_NUMBER_HARD_MS = 7500L;
    private static final long BASE_TIME_HARD_MS = 15000L;

    private int generationAttemptCounter = 0;

    private StatsDbHelper dbHelper;
    private long currentSessionId = -1;
    private static final String TAG = "ArithmeticGameActivity";

    private View dedicatedPauseOverlayContainer;
    private View buttonDedicatedPauseResume, buttonDedicatedPauseRestart, buttonDedicatedPauseMainMenu;
    private boolean isGamePausedByDedicatedMenu = false;

    private View helpOverlayContainerView;
    private TextView textViewHelpOverlayTitle;
    private TextView textViewHelpOverlayContent;
    private Button buttonHelpOverlayClose;

    private boolean gameEnded = false;
    private Menu activityMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArithmeticGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new StatsDbHelper(this);

        Toolbar toolbar = binding.toolbarArithmeticGame;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        currentDifficulty = getIntent().getStringExtra(ArithmeticChallengeActivity.EXTRA_DIFFICULTY);
        if (currentDifficulty == null) {
            currentDifficulty = getString(R.string.difficulty_easy);
            Toast.makeText(this, "Error: Difficulty not received, defaulting to Easy.", Toast.LENGTH_LONG).show();
        }
        binding.textViewCurrentDifficulty.setText(getString(R.string.label_difficulty_prefix, currentDifficulty));

        initializeHelpOverlay();
        initializeDedicatedPauseOverlay();

        setupRecyclerView();
        setupListeners();
        initializeGameParameters();
        startNewRound();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_arithmetic_game, menu);
        this.activityMenu = menu;
        updateToolbarButtonsVisibility();
        return true;
    }

    private void updateToolbarButtonsVisibility() {
        if (activityMenu != null) {
            MenuItem pauseItem = activityMenu.findItem(R.id.action_pause_arithmetic_game);
            MenuItem helpItem = activityMenu.findItem(R.id.action_help_arithmetic_game);

            if (pauseItem != null) {
                pauseItem.setVisible(!gameEnded && !isGamePausedByDedicatedMenu && !isHelpOverlayVisible() && currentRound <= totalRounds);
            }
            if (helpItem != null) {
                helpItem.setEnabled(!isGamePausedByDedicatedMenu);
            }
        }
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
            Log.e(TAG + "Help", "Help overlay container not found.");
        }
    }

    protected void showHelpOverlay(String title, String content) {
        if (isGamePausedByDedicatedMenu) {
            resumeGameFromDedicatedPause();
        }
        if (helpOverlayContainerView != null && textViewHelpOverlayTitle != null && textViewHelpOverlayContent != null) {
            if (!gameEnded && roundTimer != null) {
                roundTimer.cancel();
            }
            mainHandler.removeCallbacksAndMessages(null);

            textViewHelpOverlayTitle.setText(title);
            textViewHelpOverlayContent.setText(content);
            helpOverlayContainerView.setVisibility(View.VISIBLE);
            setGameInteractionEnabled(false);
            updateToolbarButtonsVisibility();
        } else {
            Log.e(TAG + "Help", "Help overlay views not properly initialized for showing.");
        }
    }

    protected void hideHelpOverlay() {
        if (helpOverlayContainerView != null) {
            helpOverlayContainerView.setVisibility(View.GONE);
            setGameInteractionEnabled(true);
            updateToolbarButtonsVisibility();

            if (!gameEnded && timeLeftInMillis > 0 && currentRound <= totalRounds) {
                startTimer(timeLeftInMillis);
            }
        }
    }

    protected boolean isHelpOverlayVisible() {
        return helpOverlayContainerView != null && helpOverlayContainerView.getVisibility() == View.VISIBLE;
    }

    private void setGameInteractionEnabled(boolean enabled) {
        binding.editTextAnswer.setEnabled(enabled && !gameEnded && !isGamePausedByDedicatedMenu && !isHelpOverlayVisible());
        binding.buttonSubmitAnswer.setEnabled(enabled && !gameEnded && !isGamePausedByDedicatedMenu && !isHelpOverlayVisible());

        if (activityMenu != null) {
            for (int i = 0; i < activityMenu.size(); i++) {
                MenuItem item = activityMenu.getItem(i);
                if (item.getItemId() != R.id.action_help_arithmetic_game) {
                    item.setEnabled(enabled);
                } else {
                    item.setEnabled(true);
                }
            }
        }
        updateToolbarButtonsVisibility();
    }


    private void initializeDedicatedPauseOverlay() {
        dedicatedPauseOverlayContainer = findViewById(R.id.pause_overlay_container);
        if (dedicatedPauseOverlayContainer != null) {
            buttonDedicatedPauseResume = dedicatedPauseOverlayContainer.findViewById(R.id.button_pause_resume);
            buttonDedicatedPauseRestart = dedicatedPauseOverlayContainer.findViewById(R.id.button_pause_restart);
            buttonDedicatedPauseMainMenu = dedicatedPauseOverlayContainer.findViewById(R.id.button_pause_main_menu);

            TextView restartLabel = dedicatedPauseOverlayContainer.findViewById(R.id.label_pause_restart);
            if(restartLabel != null) restartLabel.setText(R.string.pause_overlay_restart_arithmetic);

            if (buttonDedicatedPauseResume != null) buttonDedicatedPauseResume.setOnClickListener(v -> resumeGameFromDedicatedPause());
            if (buttonDedicatedPauseRestart != null) buttonDedicatedPauseRestart.setOnClickListener(v -> {
                resumeGameFromDedicatedPause();
                Toast.makeText(this, "Restarting Arithmetic Challenge.", Toast.LENGTH_SHORT).show();
                initializeGameParameters();
                startNewRound();
            });
            if (buttonDedicatedPauseMainMenu != null) buttonDedicatedPauseMainMenu.setOnClickListener(v -> {
                resumeGameFromDedicatedPause();
                finish();
            });
            dedicatedPauseOverlayContainer.setVisibility(View.GONE);
        }
    }


    private void setupRecyclerView() {
        recyclerViewRoundHistory = binding.recyclerViewRoundHistory;
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewRoundHistory.setLayoutManager(layoutManager);
        roundHistoryAdapter = new RoundHistoryAdapter(roundHistoryList, this);
        recyclerViewRoundHistory.setAdapter(roundHistoryAdapter);
    }

    private void initializeGameParameters() {
        score = 0;
        currentRound = 0;
        bonusTimeForNextRoundMillis = 0;
        totalRounds = 10;
        gameEnded = false;
        isGamePausedByDedicatedMenu = false;

        if(helpOverlayContainerView != null && helpOverlayContainerView.getVisibility() == View.VISIBLE){
            hideHelpOverlay();
        }
        if(dedicatedPauseOverlayContainer != null && dedicatedPauseOverlayContainer.getVisibility() == View.VISIBLE){
            dedicatedPauseOverlayContainer.setVisibility(View.GONE);
        }


        roundHistoryList.clear();
        if (roundHistoryAdapter != null) {
            roundHistoryAdapter.notifyDataSetChanged();
        }

        updateScoreDisplay();
        updateRoundCounterDisplay();
        setGameInteractionEnabled(true);
        updateToolbarButtonsVisibility();

        startNewDbSession();
    }


    private void startNewDbSession() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(StatsDbHelper.ArithmeticSessionEntry.COLUMN_NAME_DIFFICULTY, currentDifficulty);
        values.put(StatsDbHelper.ArithmeticSessionEntry.COLUMN_NAME_FINAL_SCORE, 0);

        currentSessionId = db.insert(StatsDbHelper.ArithmeticSessionEntry.TABLE_NAME, null, values);
        if (currentSessionId == -1) {
            Log.e(TAG, "Error creating new Arithmetic session in DB.");
            Toast.makeText(this, "Error starting game session stats.", Toast.LENGTH_SHORT).show();
        } else {
            Log.i(TAG, "New Arithmetic session started with ID: " + currentSessionId + " Difficulty: " + currentDifficulty);
        }
    }

    private void setupListeners() {
        binding.buttonSubmitAnswer.setOnClickListener(v -> {
            if (!binding.buttonSubmitAnswer.isEnabled() || isHelpOverlayVisible() || isGamePausedByDedicatedMenu) {
                return;
            }
            handleSubmit();
        });

        binding.editTextAnswer.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (!binding.buttonSubmitAnswer.isEnabled() || isHelpOverlayVisible() || isGamePausedByDedicatedMenu) {
                    return true;
                }
                handleSubmit();
                return true;
            }
            return false;
        });
    }
    private void startNewRound() {
        if (gameEnded || isGamePausedByDedicatedMenu || isHelpOverlayVisible()) return;

        currentRound++;
        updateRoundCounterDisplay();
        updateToolbarButtonsVisibility();

        if (currentRound > totalRounds) {
            endGame();
            return;
        }

        binding.buttonSubmitAnswer.setEnabled(true);

        binding.editTextAnswer.setText("");
        binding.editTextAnswer.requestFocus();
        showKeyboard();

        generationAttemptCounter = 0;
        generateNewProblem();

        long currentRoundTimeWithBonus = timePerRoundMillisAssigned + bonusTimeForNextRoundMillis;
        bonusTimeForNextRoundMillis = 0;
        if (currentRoundTimeWithBonus < 5000) currentRoundTimeWithBonus = 5000;

        startTimer(currentRoundTimeWithBonus);
    }

    private void updateRoundCounterDisplay() {
        binding.textViewRoundCounter.setText(getString(R.string.label_round_counter_format, Math.min(currentRound, totalRounds), totalRounds));
    }

    private void updateScoreDisplay() {
        binding.textViewScore.setText(getString(R.string.label_score_format, score));
    }
    private void generateNewProblem() {
        generationAttemptCounter++;
        if (generationAttemptCounter > 20) {
            Toast.makeText(this, "Problem generation fallback.", Toast.LENGTH_SHORT).show();
            int n1 = random.nextInt(9) + 1;
            int n2 = random.nextInt(9) + 1;
            currentProblemExpression = n1 + " + " + n2;
            currentCorrectAnswer = n1 + n2;
            timePerRoundMillisAssigned = (TIME_PER_NUMBER_EASY_MS * 2) + BASE_TIME_EASY_MS;
            binding.textViewProblem.setText(String.format(Locale.getDefault(), "%s = ?", currentProblemExpression));
            generationAttemptCounter = 0;
            return;
        }

        int numOperators;
        int minNumbersInOperation;
        int maxNumbersInOperation;
        int minDigitsPerNumberOverall;
        int maxDigitsPerNumberOverall;

        if (currentDifficulty.equals(getString(R.string.difficulty_easy))) {
            numOperators = 1;
            minNumbersInOperation = 2; maxNumbersInOperation = 2;
            minDigitsPerNumberOverall = 1; maxDigitsPerNumberOverall = 2;
        } else if (currentDifficulty.equals(getString(R.string.difficulty_medium))) {
            numOperators = random.nextInt(2) + 1;
            minNumbersInOperation = 2; maxNumbersInOperation = 3;
            minDigitsPerNumberOverall = 1; maxDigitsPerNumberOverall = 3;
        } else {
            numOperators = random.nextInt(3) + 1;
            minNumbersInOperation = 2; maxNumbersInOperation = 4;
            minDigitsPerNumberOverall = 1; maxDigitsPerNumberOverall = 4;
        }

        List<Integer> numbers = new ArrayList<>();
        List<Character> chosenOperators = new ArrayList<>();
        StringBuilder problemBuilder = new StringBuilder();

        boolean isMultiplication = false;
        boolean isDivision = false;

        if (numOperators == 1) {
            int opChoice = random.nextInt(4);
            if (opChoice == 0) chosenOperators.add('+');
            else if (opChoice == 1) chosenOperators.add('-');
            else if (opChoice == 2) { chosenOperators.add('*'); isMultiplication = true; }
            else { chosenOperators.add('/'); isDivision = true; }
        } else {
            for (int i = 0; i < numOperators; i++) {
                chosenOperators.add(random.nextBoolean() ? '+' : '-');
            }
        }

        int actualNumbersInProblem;

        if (isMultiplication) {
            actualNumbersInProblem = 2;
            int num1 = generateNumberForDifficulty(minDigitsPerNumberOverall, maxDigitsPerNumberOverall);
            int num2;
            if (currentDifficulty.equals(getString(R.string.difficulty_medium))) {
                num2 = generateNumberForDifficulty(1, Math.min(maxDigitsPerNumberOverall, 2));
                if (num2 > 12) num2 = random.nextInt(12) + 1;
            } else if (currentDifficulty.equals(getString(R.string.difficulty_hard))) {
                num2 = generateNumberForDifficulty(1, Math.min(maxDigitsPerNumberOverall, 2));
                if (num2 > 20) num2 = random.nextInt(20) + 1;
            } else {
                num2 = generateNumberForDifficulty(1, Math.min(maxDigitsPerNumberOverall, 1));
                if (num2 > 9) num2 = random.nextInt(9)+1;
            }
            if (num2 == 0) num2 = 1;

            numbers.add(num1);
            numbers.add(num2);
            problemBuilder.append(num1).append(" × ").append(num2);
            currentCorrectAnswer = num1 * num2;
        } else if (isDivision) {
            actualNumbersInProblem = 2;
            int divisor, dividend, quotient;
            int attempts = 0;
            do {
                int divisorMaxDigits = Math.min(maxDigitsPerNumberOverall, 2);
                if (currentDifficulty.equals(getString(R.string.difficulty_easy))) divisorMaxDigits = 1;
                divisor = generateNumberForDifficulty(1, divisorMaxDigits);
                if (divisor == 0) divisor = 1;

                int maxPossibleQuotient = (int) (Math.pow(10, maxDigitsPerNumberOverall) / divisor);
                if (maxPossibleQuotient <= 0) maxPossibleQuotient = 1;
                int qMin = 1;
                int qMax = Math.max(qMin, random.nextInt(Math.min(maxPossibleQuotient, currentDifficulty.equals(getString(R.string.difficulty_easy)) ? 10 : (currentDifficulty.equals(getString(R.string.difficulty_medium)) ? 20 : 50))) + qMin);


                quotient = random.nextInt(qMax - qMin + 1) + qMin;
                dividend = divisor * quotient;
                attempts++;
                if(attempts > 50) {
                    divisor = generateNumberForDifficulty(1,1); if(divisor == 0) divisor = 1;
                    quotient = generateNumberForDifficulty(1,1);
                    dividend = divisor*quotient;
                    Log.w(TAG, "Division generation fallback triggered.");
                    break;
                }
            } while (String.valueOf(dividend).length() > maxDigitsPerNumberOverall || String.valueOf(dividend).length() < minDigitsPerNumberOverall || dividend < 0 );

            if (dividend < 0) {
                generateNewProblem(); return;
            }

            numbers.add(dividend);
            numbers.add(divisor);
            problemBuilder.append(dividend).append(" ÷ ").append(divisor);
            currentCorrectAnswer = quotient;

        } else {
            actualNumbersInProblem = random.nextInt(maxNumbersInOperation - minNumbersInOperation + 1) + minNumbersInOperation;
            actualNumbersInProblem = Math.min(actualNumbersInProblem, numOperators + 1);

            for (int i = 0; i < actualNumbersInProblem; i++) {
                if (currentDifficulty.equals(getString(R.string.difficulty_hard))) {
                    int numDigitsForThisNumber = random.nextInt(maxDigitsPerNumberOverall) + 1;
                    numbers.add(generateNumberForDifficulty(1, numDigitsForThisNumber));
                } else {
                    numbers.add(generateNumberForDifficulty(minDigitsPerNumberOverall, maxDigitsPerNumberOverall));
                }
            }

            currentCorrectAnswer = numbers.get(0);
            problemBuilder.append(numbers.get(0));

            int operatorsToUseInLoop = Math.min(chosenOperators.size(), actualNumbersInProblem - 1);

            for (int i = 0; i < operatorsToUseInLoop; i++) {
                char op = chosenOperators.get(i);
                int nextNum = numbers.get(i + 1);

                if (op == '+') {
                    problemBuilder.append(" + ").append(nextNum);
                    currentCorrectAnswer += nextNum;
                } else {
                    if (currentCorrectAnswer < nextNum && !currentDifficulty.equals(getString(R.string.difficulty_hard))) {
                        generateNewProblem(); return;
                    }
                    problemBuilder.append(" - ").append(nextNum);
                    currentCorrectAnswer -= nextNum;
                }
            }
            if (currentCorrectAnswer < 0 && !currentDifficulty.equals(getString(R.string.difficulty_hard))) {
                generateNewProblem(); return;
            }
        }

        currentProblemExpression = problemBuilder.toString();
        long timePerNumConstant = TIME_PER_NUMBER_EASY_MS;
        long baseTimeConstant = BASE_TIME_EASY_MS;

        if (currentDifficulty.equals(getString(R.string.difficulty_medium))) {
            timePerNumConstant = TIME_PER_NUMBER_MEDIUM_MS;
            baseTimeConstant = BASE_TIME_MEDIUM_MS;
        } else if (currentDifficulty.equals(getString(R.string.difficulty_hard))) {
            timePerNumConstant = TIME_PER_NUMBER_HARD_MS;
            baseTimeConstant = BASE_TIME_HARD_MS;
        }
        timePerRoundMillisAssigned = (timePerNumConstant * actualNumbersInProblem) + baseTimeConstant;

        binding.textViewProblem.setText(String.format(Locale.getDefault(), "%s = ?", currentProblemExpression));
        generationAttemptCounter = 0;
    }

    private int generateNumberForDifficulty(int minDigits, int maxDigits) {
        if (minDigits < 1) minDigits = 1;
        if (maxDigits < minDigits) maxDigits = minDigits;
        long minVal = (long) Math.pow(10, minDigits - 1);
        if (minDigits == 1) {
            minVal = 1;
        }
        long maxVal = (long) Math.pow(10, maxDigits) - 1;
        if (maxVal < minVal) maxVal = minVal;

        if (maxVal - minVal + 1 <= 0) return (int) minVal;
        return (int) (random.nextInt((int) (maxVal - minVal + 1)) + minVal);
    }

    private void startTimer(long millisInFuture) {
        if (roundTimer != null) {
            roundTimer.cancel();
        }

        final long timerStartTime = millisInFuture;
        timeLeftInMillis = timerStartTime;

        binding.progressBarTime.setMax((int) (timerStartTime / 1000));
        roundTimer = new CountDownTimer(timerStartTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isGamePausedByDedicatedMenu || isHelpOverlayVisible()) {
                    cancel();

                    return;
                }
                timeLeftInMillis = millisUntilFinished;
                int secondsLeft = (int) (millisUntilFinished / 1000);
                binding.progressBarTime.setProgress(secondsLeft);
                binding.textViewTimeValue.setText(String.format(Locale.getDefault(), "%ds", secondsLeft));
            }
            @Override
            public void onFinish() {
                if (isGamePausedByDedicatedMenu || isHelpOverlayVisible()) return;
                timeLeftInMillis = 0;
                binding.progressBarTime.setProgress(0);
                binding.textViewTimeValue.setText("0s");
                handleAnswer(false, true, getString(R.string.history_item_time_up));
            }
        }.start();
    }

    private void handleSubmit() {
        binding.buttonSubmitAnswer.setEnabled(false);

        if (roundTimer != null) {
            roundTimer.cancel();
        }
        hideKeyboard();

        String userAnswerStr = binding.editTextAnswer.getText().toString().trim();
        if (userAnswerStr.isEmpty()) {
            Toast.makeText(this, "Please enter an answer.", Toast.LENGTH_SHORT).show();
            handleAnswer(false, false, getString(R.string.history_item_no_answer));
            return;
        }

        try {
            int userAnswer = Integer.parseInt(userAnswerStr);
            boolean isCorrect = (userAnswer == currentCorrectAnswer);
            handleAnswer(isCorrect, false, userAnswerStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format.", Toast.LENGTH_SHORT).show();
            handleAnswer(false, false, userAnswerStr + " (Invalid)");
        }
    }

    private void handleAnswer(boolean isCorrect, boolean timeUp, String userAnswerText) {
        long timeTakenMs = timePerRoundMillisAssigned - timeLeftInMillis;
        if (timeUp) {
            timeTakenMs = timePerRoundMillisAssigned;
        }
        if (timeTakenMs < 0) timeTakenMs = 0;

        saveArithmeticRoundResult(timeTakenMs, isCorrect && !timeUp);

        RoundHistoryItem historyItem = new RoundHistoryItem(
                currentRound,
                currentProblemExpression,
                currentCorrectAnswer,
                userAnswerText,
                isCorrect && !timeUp,
                timeUp
        );
        roundHistoryList.add(0, historyItem);
        if (roundHistoryAdapter != null) {
            roundHistoryAdapter.notifyItemInserted(0);
            if (!roundHistoryList.isEmpty()) {
                binding.recyclerViewRoundHistory.scrollToPosition(0);
            }
        }

        if (isCorrect && !timeUp) {
            score += 10;
            if (currentDifficulty.equals(getString(R.string.difficulty_easy))) {
                bonusTimeForNextRoundMillis = 5000L;
            } else if (currentDifficulty.equals(getString(R.string.difficulty_medium))) {
                bonusTimeForNextRoundMillis += 10000L;
            } else if (currentDifficulty.equals(getString(R.string.difficulty_hard))) {
                bonusTimeForNextRoundMillis += 15000L;
            }
        } else {
            if (currentDifficulty.equals(getString(R.string.difficulty_medium))) {
                bonusTimeForNextRoundMillis -= 5000L;
            } else if (currentDifficulty.equals(getString(R.string.difficulty_hard))) {
                bonusTimeForNextRoundMillis -= 10000L;
            }
        }
        updateScoreDisplay();
        mainHandler.postDelayed(() -> {
            if (currentRound < totalRounds && !gameEnded) {
                startNewRound();
            } else if (!gameEnded) {
                endGame();
            }
        }, 1500);
    }

    private void saveArithmeticRoundResult(long timeTakenMs, boolean wasCorrect) {
        if (currentSessionId == -1) {
            Log.e(TAG, "Cannot save arithmetic round, invalid session ID.");
            return;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(StatsDbHelper.ArithmeticRoundEntry.COLUMN_NAME_SESSION_ID, currentSessionId);
        values.put(StatsDbHelper.ArithmeticRoundEntry.COLUMN_NAME_TIME_TAKEN_MS, timeTakenMs);
        values.put(StatsDbHelper.ArithmeticRoundEntry.COLUMN_NAME_WAS_CORRECT, wasCorrect ? 1 : 0);

        long newRowId = db.insert(StatsDbHelper.ArithmeticRoundEntry.TABLE_NAME, null, values);
        if (newRowId == -1) {
            Log.e(TAG, "Error saving Arithmetic round result to DB.");
        } else {
            Log.i(TAG, "Arithmetic round result saved for session " + currentSessionId + " (Row ID: " + newRowId + "), Time: " + timeTakenMs + "ms, Correct: " + wasCorrect);
        }
    }

    private void endGame() {
        if (gameEnded) return;
        gameEnded = true;

        if (roundTimer != null) {
            roundTimer.cancel();
        }
        updateToolbarButtonsVisibility();

        String finalMessage = String.format(Locale.getDefault(), "Game Over! Final Score: %d", score);
        binding.textViewProblem.setText(finalMessage);

        setGameInteractionEnabled(false);
        hideKeyboard();
        updateDbSessionWithFinalScore();

        new AlertDialog.Builder(this)
                .setTitle("Game Over!")
                .setMessage(String.format(Locale.getDefault(),"Your final score is: %d", score))
                .setPositiveButton("Play Again", (dialog, which) -> {
                    initializeGameParameters();
                    startNewRound();
                })
                .setNegativeButton("Main Menu", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
    private void updateDbSessionWithFinalScore() {
        if (currentSessionId == -1) {
            Log.e(TAG, "Cannot update session score, invalid session ID.");
            return;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(StatsDbHelper.ArithmeticSessionEntry.COLUMN_NAME_FINAL_SCORE, score);

        String selection = StatsDbHelper.ArithmeticSessionEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(currentSessionId) };

        int count = db.update(
                StatsDbHelper.ArithmeticSessionEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);

        if (count > 0) {
            Log.i(TAG, "Arithmetic session " + currentSessionId + " updated with final score: " + score);
        } else {
            Log.e(TAG, "Error updating final score for session " + currentSessionId);
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(binding.editTextAnswer, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = this.getCurrentFocus();
        if (view == null) view = new View(this);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    private void toggleDedicatedPauseState() {
        if (gameEnded || isHelpOverlayVisible()) return;

        if (isGamePausedByDedicatedMenu) {
            resumeGameFromDedicatedPause();
        } else {
            pauseGameForDedicatedMenu();
        }
        updateToolbarButtonsVisibility();
    }

    private void pauseGameForDedicatedMenu() {
        if (gameEnded) return;
        isGamePausedByDedicatedMenu = true;
        if (roundTimer != null) {
            roundTimer.cancel();
        }
        mainHandler.removeCallbacksAndMessages(null);

        if (dedicatedPauseOverlayContainer != null) {
            dedicatedPauseOverlayContainer.setVisibility(View.VISIBLE);
        }
        setGameInteractionEnabled(false);
    }

    private void resumeGameFromDedicatedPause() {
        if (gameEnded) return;
        isGamePausedByDedicatedMenu = false;
        if (dedicatedPauseOverlayContainer != null) {
            dedicatedPauseOverlayContainer.setVisibility(View.GONE);
        }
        setGameInteractionEnabled(true);

        if (timeLeftInMillis > 0 && currentRound <= totalRounds && !gameEnded) {
            startTimer(timeLeftInMillis);
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (isHelpOverlayVisible()) {
            if (itemId == R.id.action_help_arithmetic_game || itemId == android.R.id.home) {
                hideHelpOverlay();
                return true;
            }
            return false;
        }

        if (isGamePausedByDedicatedMenu) {
            if (itemId == R.id.action_pause_arithmetic_game || itemId == android.R.id.home) {
            }
            else {
                return false;
            }
        }


        if (itemId == android.R.id.home) {
            if (isGamePausedByDedicatedMenu) resumeGameFromDedicatedPause();
            if (currentSessionId != -1 && !gameEnded && score >= 0) {
                updateDbSessionWithFinalScore();
            }
            if (roundTimer != null) roundTimer.cancel();
            finish();
            return true;
        } else if (itemId == R.id.action_pause_arithmetic_game) {
            toggleDedicatedPauseState();
            return true;
        } else if (itemId == R.id.action_help_arithmetic_game) {
            if (isGamePausedByDedicatedMenu) resumeGameFromDedicatedPause();
            showHelpOverlay(getString(R.string.help_title_arithmetic_game), getString(R.string.help_content_arithmetic_game));
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
            updateToolbarButtonsVisibility();
        } else {
            if (currentSessionId != -1 && !gameEnded && score >= 0) {
                updateDbSessionWithFinalScore();
            }
            if (roundTimer != null) roundTimer.cancel();
            super.onBackPressed();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roundTimer != null) roundTimer.cancel();
        if (dbHelper != null) {
            dbHelper.close();
        }
        mainHandler.removeCallbacksAndMessages(null);
    }
}