package com.example.jarm; // Replace with your actual package name

import android.os.Bundle;
import android.widget.Toast; // For simple interaction testing

import androidx.appcompat.app.AppCompatActivity;
// Remove EdgeToEdge and WindowInsets imports if you're simplifying heavily,
// but they are fine to keep.
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Import your generated ViewBinding class
import com.example.jarm.databinding.ActivityMainBinding; // Make sure this matches your package and XML name

public class MainActivity extends AppCompatActivity {

    // Declare the binding variable
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- EdgeToEdge Setup (Optional for bare minimum display, but good to keep) ---
        EdgeToEdge.enable(this);
        // --- End EdgeToEdge Setup ---

        // Inflate the layout using ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        // Set the root of the binding as the content view
        setContentView(binding.getRoot());


        // --- Window Insets Listener (Optional for bare minimum display, related to EdgeToEdge) ---
        // Note: The ID 'main' needs to exist in your activity_main.xml as the root layout ID.
        // If your root layout in activity_main.xml doesn't have android:id="@+id/main",
        // this part will cause a crash. You can assign the ID or apply insets to binding.getRoot().
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // --- End Window Insets Listener ---


        // ***** BARE MINIMUM FOR FUNCTIONALITY (OPTIONAL FOR JUST DISPLAY) *****
        // If you want to test the tap/long-tap, add listeners here.
        // Otherwise, the UI will just display.

        // Example: Make Tic Tac Toe icon clickable (just to show it works)
        if (binding.cardCompactTicTacToe != null) { // Use the CardView's ID
            binding.cardCompactTicTacToe.setOnClickListener(view -> {
                Toast.makeText(MainActivity.this, "Tic Tac Toe bar tapped!", Toast.LENGTH_SHORT).show();
                // TODO: Play Tic Tac Toe game
            });

            binding.cardCompactTicTacToe.setOnLongClickListener(view -> {
                String description = getString(R.string.game_desc_tic_tac_toe);
                Toast.makeText(MainActivity.this, "Info: " + description, Toast.LENGTH_LONG).show();
                return true; // Consumed
            });
        }

        if (binding.cardCompactRps != null) { // Ensure this ID exists on the CardView in XML
            binding.cardCompactRps.setOnClickListener(view -> {
                Toast.makeText(MainActivity.this, "Rock Paper Scissors bar tapped!", Toast.LENGTH_SHORT).show();
                // TODO: Play Rock Paper Scissors
            });

            binding.cardCompactRps.setOnLongClickListener(view -> {
                String description = getString(R.string.game_desc_rps);
                Toast.makeText(MainActivity.this, "Info: " + description, Toast.LENGTH_LONG).show();
                return true;
            });
        }

        if (binding.cardCompactArithmetic != null) { // Ensure this ID exists on the CardView in XML
            binding.cardCompactArithmetic.setOnClickListener(view -> {
                Toast.makeText(MainActivity.this, "Arithmetic Challenge bar tapped!", Toast.LENGTH_SHORT).show();
                // TODO: Play Arithmetic Challenge
            });

            binding.cardCompactArithmetic.setOnLongClickListener(view -> {
                String description = getString(R.string.game_desc_arithmetic);
                Toast.makeText(MainActivity.this, "Info: " + description, Toast.LENGTH_LONG).show();
                return true;
            });
        }

        // Example: Handle Bottom Navigation (just to show it works)
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_games) {
                    Toast.makeText(MainActivity.this, "Games selected", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.navigation_stats) {
                    Toast.makeText(MainActivity.this, "Stats selected", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.navigation_options) {
                    Toast.makeText(MainActivity.this, "Options selected", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });
        }

        if (binding.topAppBar != null) {
            binding.topAppBar.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.action_help) { // CHECK FOR THE CORRECT ID
                    Toast.makeText(MainActivity.this, "Help icon clicked!", Toast.LENGTH_SHORT).show();
                    // TODO: Implement what happens when help is clicked (e.g., show a dialog, new activity)
                    return true;
                }
                return false;
            });
        }
    }
}