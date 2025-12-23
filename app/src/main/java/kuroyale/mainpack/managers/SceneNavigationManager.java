package kuroyale.mainpack.managers;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

/**
 * Handles scene navigation and UI button handlers.
 * High cohesion: All scene/UI navigation logic in one place.
 */
public class SceneNavigationManager {
    private final GridPane arenaGrid;
    private final GameStateManager gameStateManager;

    public SceneNavigationManager(GridPane arenaGrid, GameStateManager gameStateManager) {
        this.arenaGrid = arenaGrid;
        this.gameStateManager = gameStateManager;
    }

    public void switchToStartBattleScene(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/StartBattleScene.fxml"));
        root.setStyle("-fx-background-color: BD7FFF;");
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, Color.web("0xBD7FFF"));
        stage.setScene(scene);
        stage.show();
    }

    public void showGameEndScreen(boolean playerWon, boolean isDraw, javafx.animation.Timeline gameLoop) {
        // Stop the game loop
        if (gameLoop != null) {
            gameLoop.stop();
        }

        // Show game end screen
        javafx.application.Platform.runLater(() -> {
            try {
                Alert alert = new Alert(
                        isDraw ? Alert.AlertType.INFORMATION
                                : (playerWon ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING));
                alert.setTitle("Game Over");
                alert.setHeaderText(isDraw ? "Draw" : (playerWon ? "Victory!" : "Defeat!"));
                alert.setContentText(isDraw ? "Time is up and tower health is same."
                        : (playerWon ? "You destroyed the enemy king!" : "Your king has been destroyed!"));

                // Show and wait
                alert.showAndWait();

                // Switch back to start battle scene
                Parent root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/StartBattleScene.fxml"));
                root.setStyle("-fx-background-color: BD7FFF;");
                Stage stage = (Stage) arenaGrid.getScene().getWindow();
                Scene scene = new Scene(root, 1280, 720, Color.web("0xBD7FFF"));
                stage.setScene(scene);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void handlePauseButton(ActionEvent event, javafx.animation.Timeline gameLoop) {
        if (gameLoop == null) return;

        if (!gameStateManager.isPaused()) {
            gameLoop.pause();
            gameStateManager.setPaused(true);
            ((Button) event.getSource()).setText("Resume");
        } else {
            gameLoop.play();
            gameStateManager.setPaused(false);
            ((Button) event.getSource()).setText("Pause");
        }
    }

    public void handleSpeedButton(ActionEvent event, javafx.animation.Timeline gameLoop) {
        if (gameLoop == null) return;

        if (!gameStateManager.isSpeed2x()) {
            gameLoop.setRate(2.0);
            gameStateManager.setSpeed2x(true);
            ((Button) event.getSource()).setText("1x");
        } else {
            gameLoop.setRate(1.0);
            gameStateManager.setSpeed2x(false);
            ((Button) event.getSource()).setText("2x");
        }
    }
}
