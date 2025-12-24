package kuroyale.mainpack.managers;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

/**
 * Manages game state: timer, elixir, pause/speed controls.
 * High cohesion: All game state management in one place.
 */
public class GameStateManager {
    private int totalSeconds = 180;
    private double timePassedSinceLastSecond = 0;
    private double currentElixir = 5.0;
    private final double MAX_ELIXIR = 10;
    private final double ELIXIR_REGEN_RATE = 1.0 / 2.8;
    private final double DOUBLE_ELIXIR_REGEN_RATE = 1.0 / 1.4;
    
    private boolean paused = false;
    private boolean speed2x = false;

    private Label gameTimerLabel;
    private Label elixirCountLabel;
    private ProgressBar elixirProgressBar;

    public GameStateManager(Label gameTimerLabel, Label elixirCountLabel, ProgressBar elixirProgressBar) {
        this.gameTimerLabel = gameTimerLabel;
        this.elixirCountLabel = elixirCountLabel;
        this.elixirProgressBar = elixirProgressBar;
    }

    public int getTotalSeconds() { return totalSeconds; }
    public void setTotalSeconds(int seconds) { this.totalSeconds = seconds; }
    public double getCurrentElixir() { return currentElixir; }
    public void setCurrentElixir(double elixir) { this.currentElixir = elixir; }
    public double getMaxElixir() { return MAX_ELIXIR; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
    public boolean isSpeed2x() { return speed2x; }
    public void setSpeed2x(boolean speed2x) { this.speed2x = speed2x; }

    public void updateElixir(double tickDuration) {
        if (currentElixir < MAX_ELIXIR) {
            if (totalSeconds >= 60) {
                currentElixir += (ELIXIR_REGEN_RATE * tickDuration);
                if (currentElixir > MAX_ELIXIR)
                    currentElixir = MAX_ELIXIR;
            } else {
                currentElixir += (DOUBLE_ELIXIR_REGEN_RATE * tickDuration);
                if (currentElixir > MAX_ELIXIR)
                    currentElixir = MAX_ELIXIR;
            }
        }
        updateElixirUI();
    }

    public boolean updateTimer(double tickDuration) {
        timePassedSinceLastSecond += tickDuration;
        if (timePassedSinceLastSecond >= 1.0) {
            if (totalSeconds > 0) {
                totalSeconds--;
                updateTimerLabel();
            } else {
                updateTimerLabel();
                timePassedSinceLastSecond = 0;
                return true; // Time's up
            }
            timePassedSinceLastSecond = 0;
        }
        return false;
    }

    public void updateElixirUI() {
        if (elixirProgressBar != null) {
            elixirProgressBar.setProgress(currentElixir / MAX_ELIXIR);
        }
        if (elixirCountLabel != null) {
            elixirCountLabel.setText(String.format("%d", (int) Math.floor(currentElixir)));
        }
    }

    private void updateTimerLabel() {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timeText = String.format("%02d:%02d", minutes, seconds);
        if (gameTimerLabel != null) {
            gameTimerLabel.setText(timeText);
        }
    }

    public void consumeElixir(double amount) {
        currentElixir -= amount;
        updateElixirUI();
    }
}
