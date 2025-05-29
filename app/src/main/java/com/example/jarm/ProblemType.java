package com.example.jarm;

public enum ProblemType {
    // Easy
    EASY_ADD_SUB_1D_1D,         // Add/Sub (1-digit) and (1-digit)
    EASY_ADD_SUB_1D_2D,         // Add/Sub (1-digit) and (2-digit)
    EASY_ADD_SUB_2D_2D,         // Add/Sub (2-digit) and (2-digit)

    // Medium
    MEDIUM_ADD_SUB_2_2D,        // Add/Sub 2 two-digit numbers
    MEDIUM_ADD_SUB_3_2D,        // Add/Sub 3 two-digit numbers
    MEDIUM_ADD_SUB_2_3D,        // Add/Sub 2 three-digit numbers
    MEDIUM_MIXED_3_2D,          // Mixed Add/Sub 3 two-digit numbers
    MEDIUM_MIXED_2_3D,          // Mixed Add/Sub 2 three-digit numbers
    MEDIUM_MUL_2D_1D,           // Mul (2-digit) x (1-digit)
    MEDIUM_MUL_2D_2D,           // Mul (2-digit) x (2-digit)
    MEDIUM_DIV_2D_1D,           // Div (2-digit) / (1-digit)

    // Hard
    HARD_ADD_3_3D,              // Add 3 three-digit numbers
    HARD_ADD_2_4D,              // Add 2 four-digit numbers
    HARD_SUB_2_3D,              // Sub 2 three-digit numbers
    HARD_SUB_2_4D,              // Sub 2 four-digit numbers
    HARD_MIXED_3_3D,            // Mixed Add/Sub 3 three-digit numbers
    HARD_MIXED_2_4D,            // Mixed Add/Sub 2 four-digit numbers
    HARD_MUL_2D_3D,             // Mul (2-digit) x (3-digit)
    HARD_MUL_3D_3D,             // Mul (3-digit) x (3-digit)
    HARD_DIV_3D_1D,             // Div (3-digit) / (1-digit)
    HARD_DIV_4D_2D              // Div (4-digit) / (2-digit)
}