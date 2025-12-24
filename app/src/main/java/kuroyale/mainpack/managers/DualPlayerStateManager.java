package kuroyale.mainpack.managers;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

/**
 * Manages game state for dual players in PvP mode.
 * Each player has independent elixir counter.
 * High cohesion: All dual-player state management in one place.
 */
public class DualPlayerStateManager {
    private static final double MAX_ELIXIR = 10.0;
    private static final double ELIXIR_REGEN_RATE = 1.0 / 2.8;
    private static final double DOUBLE_ELIXIR_REGEN_RATE = 1.0 / 1.4;
    
    // Player 1 elixir state
    private double player1Elixir = 5.0;
    private final Label player1ElixirLabel;
    private final ProgressBar player1ElixirBar;
    
    // Player 2 elixir state
    private double player2Elixir = 5.0;
    private final Label player2ElixirLabel;
    private final ProgressBar player2ElixirBar;
    
    // Shared timer state (same as single player)
    private int totalSeconds = 180;
    private double timePassedSinceLastSecond = 0;
    
    public DualPlayerStateManager(Label player1ElixirLabel, ProgressBar player1ElixirBar,
                                  Label player2ElixirLabel, ProgressBar player2ElixirBar) {
        this.player1ElixirLabel = player1ElixirLabel;
        this.player1ElixirBar = player1ElixirBar;
        this.player2ElixirLabel = player2ElixirLabel;
        this.player2ElixirBar = player2ElixirBar;
        
        updateElixirUI(1);
        updateElixirUI(2);
    }
    
    public double getElixir(int playerId) {
        if (playerId == 1) return player1Elixir;
        if (playerId == 2) return player2Elixir;
        return 0.0;
    }
    
    public void setElixir(int playerId, double elixir) {
        if (playerId == 1) {
            player1Elixir = Math.min(Math.max(0, elixir), MAX_ELIXIR);
            updateElixirUI(1);
        } else if (playerId == 2) {
            player2Elixir = Math.min(Math.max(0, elixir), MAX_ELIXIR);
            updateElixirUI(2);
        }
    }
    
    public void consumeElixir(int playerId, double amount) {
        if (playerId == 1) {
            player1Elixir = Math.max(0, player1Elixir - amount);
            updateElixirUI(1);
        } else if (playerId == 2) {
            player2Elixir = Math.max(0, player2Elixir - amount);
            updateElixirUI(2);
        }
    }
    
    public void updateElixir(int playerId, double tickDuration) {
        double currentElixir = getElixir(playerId);
        if (currentElixir < MAX_ELIXIR) {
            double regenRate = (totalSeconds >= 60) ? ELIXIR_REGEN_RATE : DOUBLE_ELIXIR_REGEN_RATE;
            double newElixir = currentElixir + (regenRate * tickDuration);
            setElixir(playerId, Math.min(newElixir, MAX_ELIXIR));
        }
    }
    
    public void updateBothPlayersElixir(double tickDuration) {
        updateElixir(1, tickDuration);
        updateElixir(2, tickDuration);
    }
    
    private void updateElixirUI(int playerId) {
        double elixir = getElixir(playerId);
        if (playerId == 1) {
            if (player1ElixirBar != null) {
                player1ElixirBar.setProgress(elixir / MAX_ELIXIR);
            }
            if (player1ElixirLabel != null) {
                player1ElixirLabel.setText(String.format("%d", (int) Math.floor(elixir)));
            }
        } else if (playerId == 2) {
            if (player2ElixirBar != null) {
                player2ElixirBar.setProgress(elixir / MAX_ELIXIR);
            }
            if (player2ElixirLabel != null) {
                player2ElixirLabel.setText(String.format("%d", (int) Math.floor(elixir)));
            }
        }
    }
    
    // Timer methods (shared between players)
    public int getTotalSeconds() { return totalSeconds; }
    public void setTotalSeconds(int seconds) { this.totalSeconds = seconds; }
    
    public boolean updateTimer(double tickDuration) {
        timePassedSinceLastSecond += tickDuration;
        if (timePassedSinceLastSecond >= 1.0) {
            if (totalSeconds > 0) {
                totalSeconds--;
                timePassedSinceLastSecond = 0;
                return false;
            } else {
                timePassedSinceLastSecond = 0;
                return true; // Time's up
            }
        }
        return false;
    }
    
    public double getMaxElixir() { return MAX_ELIXIR; }
}
