package com.example.chainreaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.graphics.Color;
import com.shawnlin.numberpicker.NumberPicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.textfield.TextInputEditText;
import android.view.LayoutInflater;
import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.RequestConfiguration;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private TextView playerPickerButton;
    private Button startButton;
    private int selectedPlayers = 2; // default
    private List<String> playerNames = new ArrayList<>();
    private AdView adView;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "AdMob SDK initialized");
        });

        // Initialize AdView
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.setAdListener(new com.google.android.gms.ads.AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Ad loaded successfully");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                Log.e(TAG, "Ad failed to load: " + loadAdError.getMessage());
                // Retry loading the ad after a delay
                adView.postDelayed(() -> {
                    adView.loadAd(new AdRequest.Builder().build());
                }, 5000); // Retry after 5 seconds
            }
        });
        adView.loadAd(adRequest);

        playerPickerButton = findViewById(R.id.playerPickerButton);
        startButton = findViewById(R.id.startButton);

        playerPickerButton.setText("Select Players: " + selectedPlayers);

        // Make the entire card clickable
        View playerPickerCard = findViewById(R.id.playerPickerCard);
        playerPickerCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlayerPickerDialog();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playerNames.size() == selectedPlayers) {
                    Intent intent = new Intent(MainActivity.this, GameActivity.class);
                    intent.putExtra("numPlayers", selectedPlayers);
                    intent.putStringArrayListExtra("playerNames", new ArrayList<>(playerNames));
                    startActivity(intent);
                } else {
                    showPlayerNamesDialog();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    private void showPlayerPickerDialog() {
        final NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(2);
        picker.setMaxValue(4);
        picker.setValue(selectedPlayers);

        // Set text and divider colors to black using the new library methods
        picker.setTextColor(Color.BLACK);
        picker.setDividerColor(Color.BLACK);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded);
        builder.setTitle("Select Number of Players");
        builder.setView(picker);
        builder.setBackground(getResources().getDrawable(R.drawable.dialog_background));
        builder.setPositiveButton("OK", (dialog, which) -> {
            selectedPlayers = picker.getValue();
            playerPickerButton.setText("Select Players: " + selectedPlayers);
            showPlayerNamesDialog();
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showPlayerNamesDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_player_names, null);
        
        // Get references to all input fields
        TextInputEditText player1Input = dialogView.findViewById(R.id.player1Input);
        TextInputEditText player2Input = dialogView.findViewById(R.id.player2Input);
        TextInputEditText player3Input = dialogView.findViewById(R.id.player3Input);
        TextInputEditText player4Input = dialogView.findViewById(R.id.player4Input);
        
        // Show/hide input fields based on number of players
        View player3Layout = dialogView.findViewById(R.id.player3Layout);
        View player4Layout = dialogView.findViewById(R.id.player4Layout);
        
        player3Layout.setVisibility(selectedPlayers >= 3 ? View.VISIBLE : View.GONE);
        player4Layout.setVisibility(selectedPlayers >= 4 ? View.VISIBLE : View.GONE);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded);
        builder.setView(dialogView);
        builder.setBackground(getResources().getDrawable(R.drawable.dialog_background));
        builder.setPositiveButton("Start Game", null); // Set to null initially
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button click to validate input
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Get player names
            String player1Name = player1Input.getText().toString().trim();
            String player2Name = player2Input.getText().toString().trim();
            String player3Name = selectedPlayers >= 3 ? player3Input.getText().toString().trim() : "";
            String player4Name = selectedPlayers >= 4 ? player4Input.getText().toString().trim() : "";

            // Validate input
            if (player1Name.isEmpty() || player2Name.isEmpty() || 
                (selectedPlayers >= 3 && player3Name.isEmpty()) || 
                (selectedPlayers >= 4 && player4Name.isEmpty())) {
                // Show error message
                new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                    .setTitle("Error")
                    .setMessage("Please enter names for all players")
                    .setPositiveButton("OK", null)
                    .show();
                return;
            }

            // Store player names
            playerNames.clear();
            playerNames.add(player1Name);
            playerNames.add(player2Name);
            if (selectedPlayers >= 3) playerNames.add(player3Name);
            if (selectedPlayers >= 4) playerNames.add(player4Name);

            // Start the game
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("numPlayers", selectedPlayers);
            intent.putStringArrayListExtra("playerNames", new ArrayList<>(playerNames));
            startActivity(intent);
            dialog.dismiss();
        });
    }
}