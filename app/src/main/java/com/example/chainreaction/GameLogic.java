package com.example.chainreaction;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Queue;
import java.util.LinkedList;

public class GameLogic {
    private Cell[][] board;
    private List<Player> players;
    private int currentPlayerIndex;
    private int rows;
    private int cols;
    private boolean gameOver;
    private OnGameStateChangeListener listener;
    private Queue<ExplosionEvent> explosionQueue;
    private boolean isProcessingExplosion;

    public interface OnGameStateChangeListener {
        void onGameStateChanged();
        void onGameOver(int winnerId);
        void onExplosionStarted(int row, int col);
        void onExplosionCompleted();
        void onPlayerEliminated(int playerId);
    }

    private static class ExplosionEvent {
        int row;
        int col;
        int playerId;

        ExplosionEvent(int row, int col, int playerId) {
            this.row = row;
            this.col = col;
            this.playerId = playerId;
        }
    }

    public GameLogic(int rows, int cols, int numPlayers, List<String> playerNames) {
        this.rows = rows;
        this.cols = cols;
        this.board = new Cell[rows][cols];
        this.players = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.gameOver = false;
        this.explosionQueue = new LinkedList<>();
        this.isProcessingExplosion = false;
        initializeBoard();
        initializePlayers(numPlayers, playerNames);
    }

    private void initializeBoard() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int maxCapacity = calculateMaxCapacity(i, j);
                board[i][j] = new Cell(i, j, maxCapacity);
            }
        }
    }

    private int calculateMaxCapacity(int row, int col) {
        int neighbors = 0;
        if (row > 0) neighbors++;
        if (row < rows - 1) neighbors++;
        if (col > 0) neighbors++;
        if (col < cols - 1) neighbors++;
        return neighbors;
    }

    private void initializePlayers(int numPlayers, List<String> playerNames) {
        int[] colors = {
                0xFFFF0000, // Red
                0xFF00FF00, // Green
                0xFF0000FF, // Blue
                0xFFFFA500  // Yellow
        };

        for (int i = 0; i < numPlayers; i++) {
            String name = i < playerNames.size() ? playerNames.get(i) : "Player " + (i + 1);
            players.add(new Player(i, colors[i], name));
        }
    }

    public boolean placeAtom(int row, int col) {
        if (gameOver || row < 0 || row >= rows || col < 0 || col >= cols || isProcessingExplosion) {
            return false;
        }

        Cell cell = board[row][col];
        Player currentPlayer = players.get(currentPlayerIndex);

        // Can only place on empty cell or own cell
        if (cell.getOwnerPlayerId() != -1 && cell.getOwnerPlayerId() != currentPlayer.getId()) {
            return false;
        }

        // If it's a new cell or different player, reset click count
        if (cell.getOwnerPlayerId() != currentPlayer.getId()) {
            cell.resetClickCount();
        }

        cell.incrementClickCount();
        int atomsToAdd = cell.getClickCount();

        // Add atoms based on click count
        for (int i = 0; i < atomsToAdd; i++) {
            cell.addAtom(currentPlayer.getId());
            currentPlayer.setAtomCount(currentPlayer.getAtomCount() + 1);
        }

        // Notify state change before potential explosion
        if (listener != null) {
            listener.onGameStateChanged();
        }

        if (cell.isFull()) {
            startChainReaction(row, col);
        } else {
            // Only switch turns if no explosion occurred
            nextTurn();
            // Notify state change after turn switch
            if (listener != null) {
                listener.onGameStateChanged();
            }
        }

        return true;
    }

    private void startChainReaction(int row, int col) {
        isProcessingExplosion = true;
        explosionQueue.clear();
        explosionQueue.add(new ExplosionEvent(row, col, board[row][col].getOwnerPlayerId()));
        processNextExplosion();
    }

    private void processNextExplosion() {
        if (explosionQueue.isEmpty()) {
            isProcessingExplosion = false;
            checkGameOver();
            if (!gameOver) {
                nextTurn();
                // Notify state change after turn switch
                if (listener != null) {
                    listener.onGameStateChanged();
                }
            }
            if (listener != null) {
                listener.onExplosionCompleted();
            }
            return;
        }

        ExplosionEvent event = explosionQueue.poll();
        if (listener != null) {
            listener.onExplosionStarted(event.row, event.col);
        }

        Cell cell = board[event.row][event.col];
        int playerId = cell.getOwnerPlayerId();

        // Reset the exploding cell
        cell.reset();

        // Distribute atoms to neighbors and change their ownership
        if (event.row > 0) {
            Cell neighbor = board[event.row-1][event.col];
            // Reset the neighbor and add new atom
            neighbor.reset();
            neighbor.addAtom(playerId);
            if (neighbor.isFull()) {
                explosionQueue.add(new ExplosionEvent(event.row-1, event.col, playerId));
            }
        }
        if (event.row < rows - 1) {
            Cell neighbor = board[event.row+1][event.col];
            neighbor.reset();
            neighbor.addAtom(playerId);
            if (neighbor.isFull()) {
                explosionQueue.add(new ExplosionEvent(event.row+1, event.col, playerId));
            }
        }
        if (event.col > 0) {
            Cell neighbor = board[event.row][event.col-1];
            neighbor.reset();
            neighbor.addAtom(playerId);
            if (neighbor.isFull()) {
                explosionQueue.add(new ExplosionEvent(event.row, event.col-1, playerId));
            }
        }
        if (event.col < cols - 1) {
            Cell neighbor = board[event.row][event.col+1];
            neighbor.reset();
            neighbor.addAtom(playerId);
            if (neighbor.isFull()) {
                explosionQueue.add(new ExplosionEvent(event.row, event.col+1, playerId));
            }
        }

        // Notify state change after distributing atoms
        if (listener != null) {
            listener.onGameStateChanged();
        }

        // Process next explosion after a delay
        new android.os.Handler().postDelayed(this::processNextExplosion, 300);
    }

    private void checkGameOver() {
        int activePlayers = 0;
        int lastActivePlayerId = -1;
        int totalAtoms = 0;

        // Count total atoms and active players
        for (Player player : players) {
            int playerAtoms = 0;
            // Count atoms for this player
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    Cell cell = board[i][j];
                    if (cell.getOwnerPlayerId() == player.getId()) {
                        playerAtoms += cell.getAtomCount();
                    }
                }
            }

            if (playerAtoms > 0) {
                activePlayers++;
                lastActivePlayerId = player.getId();
                totalAtoms += playerAtoms;
            } else if (player.isActive()) {
                // Player has no atoms left, eliminate them
                player.setActive(false);
                if (listener != null) {
                    listener.onPlayerEliminated(player.getId());
                }
            }
        }

        // Game is over if there's only one player with atoms or no atoms left
        if (activePlayers <= 1 || totalAtoms == 0) {
            gameOver = true;
            if (listener != null) {
                listener.onGameOver(lastActivePlayerId);
            }
        }
    }

    private void nextTurn() {
        int originalIndex = currentPlayerIndex;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            // If we've gone through all players and none are active, break to prevent infinite loop
            if (currentPlayerIndex == originalIndex) {
                break;
            }
        } while (!players.get(currentPlayerIndex).isActive());
    }

    public Cell[][] getBoard() {
        return board;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isProcessingExplosion() {
        return isProcessingExplosion;
    }

    public void setOnGameStateChangeListener(OnGameStateChangeListener listener) {
        this.listener = listener;
    }
}