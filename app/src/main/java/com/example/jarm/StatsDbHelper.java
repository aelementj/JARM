package com.example.jarm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class StatsDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "JramStats.db";

    public static abstract class TicTacToeEntry implements BaseColumns {
        public static final String TABLE_NAME = "tic_tac_toe_games";
        public static final String COLUMN_NAME_WINNER = "winner";
        public static final String COLUMN_NAME_MODE = "mode";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }

    private static final String SQL_CREATE_TIC_TAC_TOE_TABLE =
            "CREATE TABLE " + TicTacToeEntry.TABLE_NAME + " (" +
                    TicTacToeEntry._ID + " INTEGER PRIMARY KEY," +
                    TicTacToeEntry.COLUMN_NAME_WINNER + " TEXT," +
                    TicTacToeEntry.COLUMN_NAME_MODE + " TEXT," +
                    TicTacToeEntry.COLUMN_NAME_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)";

    private static final String SQL_DELETE_TIC_TAC_TOE_TABLE =
            "DROP TABLE IF EXISTS " + TicTacToeEntry.TABLE_NAME;


    public static abstract class RpsEntry implements BaseColumns {
        public static final String TABLE_NAME = "rps_rounds";
        public static final String COLUMN_NAME_PLAYER_CHOICE = "player_choice";
        public static final String COLUMN_NAME_COMPUTER_CHOICE = "computer_choice";
        public static final String COLUMN_NAME_ROUND_OUTCOME = "round_outcome";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }

    private static final String SQL_CREATE_RPS_TABLE =
            "CREATE TABLE " + RpsEntry.TABLE_NAME + " (" +
                    RpsEntry._ID + " INTEGER PRIMARY KEY," +
                    RpsEntry.COLUMN_NAME_PLAYER_CHOICE + " TEXT," +
                    RpsEntry.COLUMN_NAME_COMPUTER_CHOICE + " TEXT," +
                    RpsEntry.COLUMN_NAME_ROUND_OUTCOME + " TEXT," +
                    RpsEntry.COLUMN_NAME_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)";

    private static final String SQL_DELETE_RPS_TABLE =
            "DROP TABLE IF EXISTS " + RpsEntry.TABLE_NAME;


    public static abstract class ArithmeticSessionEntry implements BaseColumns {
        public static final String TABLE_NAME = "arithmetic_sessions";
        public static final String COLUMN_NAME_DIFFICULTY = "difficulty";
        public static final String COLUMN_NAME_FINAL_SCORE = "final_score";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }

    private static final String SQL_CREATE_ARITHMETIC_SESSIONS_TABLE =
            "CREATE TABLE " + ArithmeticSessionEntry.TABLE_NAME + " (" +
                    ArithmeticSessionEntry._ID + " INTEGER PRIMARY KEY," +
                    ArithmeticSessionEntry.COLUMN_NAME_DIFFICULTY + " TEXT," +
                    ArithmeticSessionEntry.COLUMN_NAME_FINAL_SCORE + " INTEGER," +
                    ArithmeticSessionEntry.COLUMN_NAME_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)";

    private static final String SQL_DELETE_ARITHMETIC_SESSIONS_TABLE =
            "DROP TABLE IF EXISTS " + ArithmeticSessionEntry.TABLE_NAME;


    public static abstract class ArithmeticRoundEntry implements BaseColumns {
        public static final String TABLE_NAME = "arithmetic_rounds";
        public static final String COLUMN_NAME_SESSION_ID = "session_id";
        public static final String COLUMN_NAME_TIME_TAKEN_MS = "time_taken_ms";
        public static final String COLUMN_NAME_WAS_CORRECT = "was_correct";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }

    private static final String SQL_CREATE_ARITHMETIC_ROUNDS_TABLE =
            "CREATE TABLE " + ArithmeticRoundEntry.TABLE_NAME + " (" +
                    ArithmeticRoundEntry._ID + " INTEGER PRIMARY KEY," +
                    ArithmeticRoundEntry.COLUMN_NAME_SESSION_ID + " INTEGER," +
                    ArithmeticRoundEntry.COLUMN_NAME_TIME_TAKEN_MS + " INTEGER," +
                    ArithmeticRoundEntry.COLUMN_NAME_WAS_CORRECT + " INTEGER," +
                    ArithmeticRoundEntry.COLUMN_NAME_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(" + ArithmeticRoundEntry.COLUMN_NAME_SESSION_ID + ") REFERENCES " +
                    ArithmeticSessionEntry.TABLE_NAME + "(" + ArithmeticSessionEntry._ID + "))";

    private static final String SQL_DELETE_ARITHMETIC_ROUNDS_TABLE =
            "DROP TABLE IF EXISTS " + ArithmeticRoundEntry.TABLE_NAME;


    public StatsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TIC_TAC_TOE_TABLE);
        db.execSQL(SQL_CREATE_RPS_TABLE);
        db.execSQL(SQL_CREATE_ARITHMETIC_SESSIONS_TABLE);
        db.execSQL(SQL_CREATE_ARITHMETIC_ROUNDS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TIC_TAC_TOE_TABLE);
        db.execSQL(SQL_DELETE_RPS_TABLE);
        db.execSQL(SQL_DELETE_ARITHMETIC_ROUNDS_TABLE);
        db.execSQL(SQL_DELETE_ARITHMETIC_SESSIONS_TABLE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
