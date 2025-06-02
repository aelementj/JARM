package com.example.jarm;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.jarm.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private View helpOverlayContainer;
    private TextView textViewHelpOverlayTitle;
    private TextView textViewHelpOverlayContent;
    private Button buttonHelpOverlayClose;
    private int currentSelectedItemId = R.id.navigation_games;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeHelpOverlay();

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainActivityRoot, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.appBarLayout.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            binding.bottomNavigation.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        if (savedInstanceState == null) {
            replaceFragment(new GamesFragment(), R.id.navigation_games);
            binding.bottomNavigation.setSelectedItemId(R.id.navigation_games);
        } else {
            currentSelectedItemId = savedInstanceState.getInt("currentSelectedItemId", R.id.navigation_games);
        }

        setupBottomNavigationListener();
        setupTopAppBarListener();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentSelectedItemId", currentSelectedItemId);
    }

    protected void initializeHelpOverlay() {
        helpOverlayContainer = findViewById(R.id.help_overlay_container);
        if (helpOverlayContainer != null) {
            textViewHelpOverlayTitle = findViewById(R.id.textView_help_overlay_title);
            textViewHelpOverlayContent = findViewById(R.id.textView_help_overlay_content);
            buttonHelpOverlayClose = findViewById(R.id.button_help_overlay_close);

            if (buttonHelpOverlayClose != null) {
                buttonHelpOverlayClose.setOnClickListener(v -> hideHelpOverlay());
            }
            helpOverlayContainer.setOnClickListener(v -> {
                if (v.getId() == R.id.help_overlay_container) {
                    hideHelpOverlay();
                }
            });
        } else {
            Log.e("MainActivityHelp", "Help overlay container not found.");
        }
    }

    protected void showHelpOverlay(String title, String content) {
        if (helpOverlayContainer != null && textViewHelpOverlayTitle != null && textViewHelpOverlayContent != null) {
            textViewHelpOverlayTitle.setText(title);
            textViewHelpOverlayContent.setText(content);
            helpOverlayContainer.setVisibility(View.VISIBLE);
            setUiInteractionEnabled(false);
        } else {
            Log.e("MainActivityHelp", "Help overlay views not properly initialized for showing.");
            Toast.makeText(this, "Error showing help.", Toast.LENGTH_SHORT).show();
        }
    }

    protected void hideHelpOverlay() {
        if (helpOverlayContainer != null) {
            helpOverlayContainer.setVisibility(View.GONE);
            setUiInteractionEnabled(true);
        }
    }

    protected boolean isHelpOverlayVisible() {
        return helpOverlayContainer != null && helpOverlayContainer.getVisibility() == View.VISIBLE;
    }

    private void setUiInteractionEnabled(boolean enabled) {
        binding.bottomNavigation.setEnabled(enabled);
        if (binding.topAppBar.getMenu() != null) {
            for (int i = 0; i < binding.topAppBar.getMenu().size(); i++) {
                if(binding.topAppBar.getMenu().getItem(i).getItemId() != R.id.action_help) {
                    binding.topAppBar.getMenu().getItem(i).setEnabled(enabled);
                } else {
                    binding.topAppBar.getMenu().getItem(i).setEnabled(true);
                }
            }
        }
    }

    private void replaceFragment(Fragment fragment, int itemId) {
        currentSelectedItemId = itemId;
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_fragment_container, fragment);
        fragmentTransaction.commit();
    }

    private void setupBottomNavigationListener() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (isHelpOverlayVisible()) {
                hideHelpOverlay();
                return binding.bottomNavigation.getSelectedItemId() == item.getItemId();
            }

            int itemId = item.getItemId();
            if (currentSelectedItemId == itemId && getSupportFragmentManager().findFragmentById(R.id.main_fragment_container) != null) {
                if (getSupportFragmentManager().findFragmentById(R.id.main_fragment_container).getClass().getSimpleName()
                        .equals(getFragmentClassSimpleName(itemId))) {
                    return true;
                }
            }

            Fragment selectedFragment = null;
            if (itemId == R.id.navigation_games) {
                selectedFragment = new GamesFragment();
            } else if (itemId == R.id.navigation_stats) {
                selectedFragment = new StatsFragment();
            } else if (itemId == R.id.navigation_options) {
                selectedFragment = new OptionsFragment();
            }

            if (selectedFragment != null) {
                replaceFragment(selectedFragment, itemId);
                return true;
            }
            return false;
        });
    }

    private String getFragmentClassSimpleName(int itemId) {
        if (itemId == R.id.navigation_games) return GamesFragment.class.getSimpleName();
        if (itemId == R.id.navigation_stats) return StatsFragment.class.getSimpleName();
        if (itemId == R.id.navigation_options) return OptionsFragment.class.getSimpleName();
        return "";
    }

    private void setupTopAppBarListener() {
        binding.topAppBar.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.action_help) {
                if (isHelpOverlayVisible()) {
                    hideHelpOverlay();
                } else {
                    if (currentSelectedItemId == R.id.navigation_games) {
                        showHelpOverlay(getString(R.string.help_title_main_activity), getString(R.string.help_content_main_activity));
                    } else if (currentSelectedItemId == R.id.navigation_stats) {
                        showHelpOverlay(getString(R.string.help_title_stats_fragment), getString(R.string.help_content_stats_fragment));
                    } else if (currentSelectedItemId == R.id.navigation_options) {
                        showHelpOverlay(getString(R.string.help_title_options_fragment), getString(R.string.help_content_options_fragment));
                    } else {
                        showHelpOverlay(getString(R.string.help_title_main_activity), getString(R.string.help_content_main_activity));
                    }
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        if (isHelpOverlayVisible()) {
            hideHelpOverlay();
        } else {
            if (currentSelectedItemId != R.id.navigation_games) {
                binding.bottomNavigation.setSelectedItemId(R.id.navigation_games);
            } else {
                super.onBackPressed();
            }
        }
    }

    public int getCurrentSelectedTabId() {
        return currentSelectedItemId;
    }
}