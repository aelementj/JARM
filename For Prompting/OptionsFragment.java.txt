package com.example.jarm;

import android.content.Context;
// import android.content.DialogInterface; // No longer needed directly for this setup
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
// import android.net.Uri; // No longer needed
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.jarm.databinding.FragmentOptionsBinding;

public class OptionsFragment extends Fragment {

    private FragmentOptionsBinding binding;
    private StatsDbHelper dbHelper;
    private static final String TAG = "OptionsFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOptionsBinding.inflate(inflater, container, false);
        dbHelper = new StatsDbHelper(getContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupButtonListeners();
        displayAppVersion();
    }

    private void setupButtonListeners() {
        // Reset All Stats Button
        binding.buttonResetAllStats.setOnClickListener(v -> showResetStatsConfirmationDialog());

        // Developer Info Click
        binding.layoutDeveloperInfo.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Developed by: " + getString(R.string.developer_name_placeholder), Toast.LENGTH_LONG).show();
        });

        // REMOVED Privacy Policy Click Listener

        // REMOVED Open Source Licenses Click Listener
    }

    private void displayAppVersion() {
        Context context = getContext();
        if (context != null) {
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                String version = pInfo.versionName;
                binding.textAppVersionValue.setText(version);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Could not get package info: " + e.getMessage());
                binding.textAppVersionValue.setText("N/A");
            }
        }
    }

    private void showResetStatsConfirmationDialog() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.options_dialog_reset_stats_title))
                .setMessage(getString(R.string.options_dialog_reset_stats_message))
                .setPositiveButton(getString(R.string.options_dialog_button_reset), (dialog, which) -> {
                    resetAllGameStats();
                })
                .setNegativeButton(getString(R.string.options_dialog_button_cancel), null)
                .setIcon(R.drawable.ic_warning)
                .show();
    }

    private void resetAllGameStats() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean success = true;
        try {
            db.delete(StatsDbHelper.TicTacToeEntry.TABLE_NAME, null, null);
            db.delete(StatsDbHelper.RpsEntry.TABLE_NAME, null, null);
            db.delete(StatsDbHelper.ArithmeticRoundEntry.TABLE_NAME, null, null);
            db.delete(StatsDbHelper.ArithmeticSessionEntry.TABLE_NAME, null, null);
            Log.i(TAG, "All game stats tables cleared.");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing database tables: " + e.getMessage());
            success = false;
        }

        if (success) {
            Toast.makeText(getContext(), getString(R.string.options_stats_reset_success), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), getString(R.string.options_stats_reset_failure), Toast.LENGTH_LONG).show();
        }
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
