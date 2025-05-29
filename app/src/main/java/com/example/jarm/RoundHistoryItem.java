package com.example.jarm;

public class RoundHistoryItem {
    public int roundNumber;
    public String problemExpression; // e.g., "10 + 5"
    public int correctAnswer;
    public String userAnswer;
    public boolean wasCorrect;
    public boolean wasTimeUp;

    public RoundHistoryItem(int roundNumber, String problemExpression, int correctAnswer, String userAnswer, boolean wasCorrect, boolean wasTimeUp) {
        this.roundNumber = roundNumber;
        this.problemExpression = problemExpression;
        this.correctAnswer = correctAnswer;
        this.userAnswer = userAnswer;
        this.wasCorrect = wasCorrect;
        this.wasTimeUp = wasTimeUp;
    }
}