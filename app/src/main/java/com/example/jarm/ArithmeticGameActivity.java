package com.example.jarm;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

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
    private long timePerRoundMillis = 30000;
    private long timeLeftInMillis = 0;
    private long bonusTimeForNextRoundMillis = 0;

    private RecyclerView recyclerViewRoundHistory;
    private RoundHistoryAdapter roundHistoryAdapter;
    private List<RoundHistoryItem> roundHistoryList = new ArrayList<>();

    private Random random = new Random();

    // Using the constants you provided in the last interaction
    private static final long TIME_PER_NUMBER_EASY_MS = 2500L;
    private static final long BASE_TIME_EASY_MS = 25000L; // Note: string.xml said 30s base for Easy
    private static final long TIME_PER_NUMBER_MEDIUM_MS = 5000L;
    private static final long BASE_TIME_MEDIUM_MS = 20000L;
    private static final long TIME_PER_NUMBER_HARD_MS = 7500L;
    private static final long BASE_TIME_HARD_MS = 15000L;

    private int generationAttemptCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArithmeticGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

        setupRecyclerView();
        setupListeners();
        initializeGameParameters();
        startNewRound();
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

        roundHistoryList.clear();
        if (roundHistoryAdapter != null) {
            roundHistoryAdapter.notifyDataSetChanged();
        }

        updateScoreDisplay();
        updateRoundCounterDisplay();
        binding.buttonSubmitAnswer.setEnabled(true);
        binding.editTextAnswer.setEnabled(true);
    }


    private void setupListeners() {
        binding.buttonSubmitAnswer.setOnClickListener(v -> {
            if (!binding.buttonSubmitAnswer.isEnabled()) {
                return;
            }
            handleSubmit();
        });

        binding.editTextAnswer.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (!binding.buttonSubmitAnswer.isEnabled()) {
                    return true;
                }
                handleSubmit();
                return true;
            }
            return false;
        });
    }

    private void startNewRound() {
        currentRound++;
        updateRoundCounterDisplay();
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

        long currentRoundTimeWithBonus = timePerRoundMillis + bonusTimeForNextRoundMillis;
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
            timePerRoundMillis = (TIME_PER_NUMBER_EASY_MS * 2) + BASE_TIME_EASY_MS;
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
        } else { // Hard
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
                // Medium: 2nd number 1-2 digits (capped by overall), then max 12
                int num2MaxDigitsForMedium = Math.min(maxDigitsPerNumberOverall, 2);
                num2 = generateNumberForDifficulty(1, num2MaxDigitsForMedium);
                if (num2 > 12) num2 = random.nextInt(12) + 1; // Cap 1-12
            } else if (currentDifficulty.equals(getString(R.string.difficulty_hard))) {
                // Hard: 2nd number 1-2 digits (capped by overall), then max 20
                int num2MaxDigitsForHard = Math.min(maxDigitsPerNumberOverall, 2);
                num2 = generateNumberForDifficulty(1, num2MaxDigitsForHard);
                if (num2 > 20) num2 = random.nextInt(20) + 1; // Cap 1-20
            } else { // Easy (if multiplication were to occur, which is rare with 1 op random choice)
                int num2MaxDigitsForEasy = Math.min(maxDigitsPerNumberOverall, 2);
                num2 = generateNumberForDifficulty(1, num2MaxDigitsForEasy);
                if (num2 > 20) num2 = random.nextInt(20) + 1; // Default cap
            }
            if (num2 == 0) num2 = 1; // Ensure not zero after capping


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
                divisor = generateNumberForDifficulty(1, divisorMaxDigits);
                if (divisor == 0) divisor = 1;

                int maxPossibleQuotient = (int) (Math.pow(10, maxDigitsPerNumberOverall) / divisor);
                if (maxPossibleQuotient <= 0) maxPossibleQuotient = 1;
                int qMin = 1;
                int qMax = Math.max(qMin, random.nextInt(Math.min(maxPossibleQuotient, 100)) + qMin);

                quotient = random.nextInt(qMax - qMin + 1) + qMin;
                dividend = divisor * quotient;
                attempts++;
                if(attempts > 50) {
                    divisor = generateNumberForDifficulty(1,1);
                    if(divisor == 0) divisor = 1;
                    quotient = generateNumberForDifficulty(1,1);
                    dividend = divisor*quotient;
                    break;
                }
            } while (String.valueOf(dividend).length() > maxDigitsPerNumberOverall || String.valueOf(dividend).length() < minDigitsPerNumberOverall || dividend < 0 ); // Added dividend < 0 check

            // Ensure dividend is not negative from an unexpected generation path
            if (dividend < 0) {
                generateNewProblem(); // Regenerate if division results in negative dividend somehow
                return;
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
                    if (currentCorrectAnswer < nextNum) {
                        generateNewProblem();
                        return;
                    }
                    problemBuilder.append(" - ").append(nextNum);
                    currentCorrectAnswer -= nextNum;
                }
            }
            if (currentCorrectAnswer < 0) {
                generateNewProblem();
                return;
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
        timePerRoundMillis = (timePerNumConstant * actualNumbersInProblem) + baseTimeConstant;

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
        timeLeftInMillis = millisInFuture;
        binding.progressBarTime.setMax((int) (millisInFuture / 1000));
        roundTimer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                int secondsLeft = (int) (millisUntilFinished / 1000);
                binding.progressBarTime.setProgress(secondsLeft);
                binding.textViewTimeValue.setText(String.format(Locale.getDefault(), "%ds", secondsLeft));
            }
            @Override
            public void onFinish() {
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
                bonusTimeForNextRoundMillis = -10000L;
            }
        }
        updateScoreDisplay();
        binding.getRoot().postDelayed(() -> {
            if (currentRound < totalRounds) {
                startNewRound();
            } else {
                endGame();
            }
        }, 1500);
    }

    private void endGame() {
        if (roundTimer != null) {
            roundTimer.cancel();
        }
        String finalMessage = String.format(Locale.getDefault(), "Game Over! Final Score: %d", score);
        binding.textViewProblem.setText(finalMessage);

        binding.buttonSubmitAnswer.setEnabled(false);
        binding.editTextAnswer.setEnabled(false);
        hideKeyboard();

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (roundTimer != null) roundTimer.cancel();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roundTimer != null) roundTimer.cancel();
    }
}