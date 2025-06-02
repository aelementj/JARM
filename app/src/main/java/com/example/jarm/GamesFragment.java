package com.example.jarm;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.jarm.databinding.FragmentGamesBinding;

public class GamesFragment extends Fragment {

    private FragmentGamesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGamesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupGameCardListeners();
    }

    private String getGameDescription(int gameTitleResId, int defaultDescResId, int statsDescResId) {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity.getCurrentSelectedTabId() == R.id.navigation_stats) {
                return getString(statsDescResId);
            }
        }
        return getString(defaultDescResId);
    }

    private void setupGameCardListeners() {
        // Tic Tac Toe Card
        binding.cardCompactTicTacToe.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TicTacToeActivity.class);
            startActivity(intent);
        });

        binding.cardCompactTicTacToe.setOnLongClickListener(v -> {
            String description = getGameDescription(
                    R.string.game_title_tic_tac_toe,
                    R.string.game_desc_tic_tac_toe,
                    R.string.game_desc_tic_tac_toe_stats_context
            );
            Toast.makeText(getContext(), description, Toast.LENGTH_LONG).show();
            return true;
        });

        // Rock Paper Scissors Card
        binding.cardCompactRps.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RockPaperScissorsActivity.class);
            startActivity(intent);
        });

        binding.cardCompactRps.setOnLongClickListener(v -> {
            String description = getGameDescription(
                    R.string.game_title_rps,
                    R.string.game_desc_rps,
                    R.string.game_desc_rps_stats_context
            );
            Toast.makeText(getContext(), description, Toast.LENGTH_LONG).show();
            return true;
        });

        // Arithmetic Challenge Card
        binding.cardCompactArithmetic.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ArithmeticChallengeActivity.class);
            startActivity(intent);
        });

        binding.cardCompactArithmetic.setOnLongClickListener(v -> {
            String description = getGameDescription(
                    R.string.game_title_arithmetic,
                    R.string.game_desc_arithmetic,
                    R.string.game_desc_arithmetic_stats_context
            );
            Toast.makeText(getContext(), description, Toast.LENGTH_LONG).show();
            return true;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
