package com.example.chainreaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import android.animation.AnimatorInflater;
import android.animation.Animator;
import java.util.ArrayList;

public class GameActivity extends AppCompatActivity implements GameLogic.OnGameStateChangeListener {
    private GameView gameView;
    private GameLogic gameLogic;
    private TextView turnIndicator;
    private Button restartButton;
    private Button menuButton;
    private int numPlayers;
    private View winnerDialogView;
    private ImageView winnerTrophy;
    private TextView winnerText;
    private MaterialButton playAgainButton;
    private MaterialButton mainMenuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        numPlayers = getIntent().getIntExtra("numPlayers", 2);

        gameView = findViewById(R.id.gameView);
        turnIndicator = findViewById(R.id.turnIndicator);
        restartButton = findViewById(R.id.restartButton);
        menuButton = findViewById(R.id.menuButton);

        initializeGame();
        setupButtons();
    }

    @Override
    public void onBackPressed() {
        showBackConfirmationDialog();
    }

    private void initializeGame() {
        // Stop any ongoing animations
        if (gameView != null) {
            gameView.stopAnimation();
        }

        // Get player names from intent
        ArrayList<String> playerNames = getIntent().getStringArrayListExtra("playerNames");

        // Initialize game with 6x9 grid
        gameLogic = new GameLogic(6, 9, numPlayers, playerNames);
        gameLogic.setOnGameStateChangeListener(this);
        gameView.setGameLogic(gameLogic);
        updateTurnIndicator();
    }

    private void setupButtons() {
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRestartConfirmationDialog();
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBackConfirmationDialog();
            }
        });
    }

    private void showRestartConfirmationDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded);
        builder.setTitle("Restart Game")
                .setMessage("Are you sure you want to restart the game?")
                .setBackground(getResources().getDrawable(R.drawable.dialog_background))
                .setPositiveButton("Yes", (dialog, which) -> {
                    initializeGame();
                })
                .setNegativeButton("No", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Customize button colors
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        positiveButton.setTextColor(getResources().getColor(R.color.purple_500));
        negativeButton.setTextColor(getResources().getColor(R.color.purple_500));
    }

    private void showBackConfirmationDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded);
        builder.setTitle("Exit Game")
                .setMessage("Are you sure you want to go back to the main menu?")
                .setBackground(getResources().getDrawable(R.drawable.dialog_background))
                .setPositiveButton("Yes", (dialog, which) -> {
                    finish();
                })
                .setNegativeButton("No", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Customize button colors
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        positiveButton.setTextColor(getResources().getColor(R.color.purple_500));
        negativeButton.setTextColor(getResources().getColor(R.color.purple_500));
    }

    private void showWinnerDialog(int winnerId) {
        // Inflate the winner dialog layout
        winnerDialogView = getLayoutInflater().inflate(R.layout.dialog_winner, null);
        winnerTrophy = winnerDialogView.findViewById(R.id.winnerTrophy);
        winnerText = winnerDialogView.findViewById(R.id.winnerText);
        playAgainButton = winnerDialogView.findViewById(R.id.playAgainButton);
        mainMenuButton = winnerDialogView.findViewById(R.id.mainMenuButton);
        FirecrackerView firecrackerView = winnerDialogView.findViewById(R.id.firecrackerView);

        // Set winner text
        String message = winnerId >= 0 ?
                gameLogic.getPlayers().get(winnerId).getName() + " wins!" :
                getString(R.string.game_over);
        winnerText.setText(message);

        // Set winner text color and animations
        if (winnerId >= 0) {
            int winnerColor = gameLogic.getPlayers().get(winnerId).getColor();
            winnerText.setTextColor(winnerColor);
            
            // Apply glow animation
            Animation glowAnimation = AnimationUtils.loadAnimation(this, R.anim.text_glow_animation);
            winnerText.startAnimation(glowAnimation);
            
            // Apply color animation
            Animator colorAnim = AnimatorInflater.loadAnimator(this, R.anim.color_animation);
            colorAnim.setTarget(winnerText);
            colorAnim.start();
        }

        // Create and show the dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded);
        builder.setView(winnerDialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Start trophy animation
        Animation trophyAnimation = AnimationUtils.loadAnimation(this, R.anim.trophy_animation);
        winnerTrophy.startAnimation(trophyAnimation);

        // Start firecracker animation
        firecrackerView.startFirecrackerAnimation();

        // Setup button click listeners
        playAgainButton.setOnClickListener(v -> {
            dialog.dismiss();
            initializeGame();
        });

        mainMenuButton.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
    }

    private void updateTurnIndicator() {
        if (gameLogic == null || gameLogic.isGameOver()) {
            turnIndicator.setVisibility(View.GONE);
            return;
        }

        turnIndicator.setVisibility(View.VISIBLE);
        Player currentPlayer = gameLogic.getPlayers().get(gameLogic.getCurrentPlayerIndex());
        String turnText = currentPlayer.getName() + "'s Turn";
        turnIndicator.setText(turnText);
        turnIndicator.setTextColor(currentPlayer.getColor());

        // Add a subtle animation to make the turn indicator more noticeable
        turnIndicator.setAlpha(0.7f);
        turnIndicator.animate()
                .alpha(1f)
                .setDuration(500)
                .start();
    }

    @Override
    public void onGameStateChanged() {
        runOnUiThread(this::updateTurnIndicator);
    }

    @Override
    public void onExplosionStarted(int row, int col) {
        runOnUiThread(() -> gameView.startExplosionAnimation(row, col));
    }

    @Override
    public void onExplosionCompleted() {
        runOnUiThread(() -> {
            gameView.updateAtoms();
            updateTurnIndicator();
        });
    }

    @Override
    public void onGameOver(int winnerId) {
        runOnUiThread(() -> {
            showWinnerDialog(winnerId);
            updateTurnIndicator(); // Hide turn indicator when game is over
        });
    }

    @Override
    public void onPlayerEliminated(int playerId) {
        runOnUiThread(() -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded);
            builder.setTitle("Player Eliminated")
                    .setMessage("Player " + (playerId + 1) + " has been eliminated!")
                    .setBackground(getResources().getDrawable(R.drawable.dialog_background))
                    .setPositiveButton("OK", null);

            AlertDialog dialog = builder.create();
            dialog.show();

            // Customize button color
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(getResources().getColor(R.color.purple_500));
        });
    }
}