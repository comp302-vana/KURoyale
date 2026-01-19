package kuroyale.mainpack.managers;

import javafx.animation.FadeTransition;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import kuroyale.mainpack.models.Combo.ComboType;

/**
 * Handles visual feedback for combo system.
 */
public class ComboUI {
    private final Pane rootPane;
    private final Text comboCounterText;
    private ComboManager comboManager; // Reference to get actual combo count
    
    public ComboUI(Pane rootPane) {
        this.rootPane = rootPane;
        
        // Create combo counter text (top-left corner, white)
        comboCounterText = new Text("Combos: 0");
        comboCounterText.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        comboCounterText.setFill(Color.WHITE);
        comboCounterText.setLayoutX(10); // Top-left
        comboCounterText.setLayoutY(30);
        rootPane.getChildren().add(comboCounterText);
    }
    
    public void setComboManager(ComboManager comboManager) {
        this.comboManager = comboManager;
    }

    /**
     * Show combo trigger animation.
     */
    public void showComboTrigger(ComboType combo) {
        javafx.application.Platform.runLater(() -> {
            // Create "COMBO!" text
            Text comboText = new Text("COMBO!");
            comboText.setFont(Font.font("Arial", FontWeight.BOLD, 72));
            comboText.setFill(Color.GOLD);
            comboText.setStroke(Color.ORANGE);
            comboText.setStrokeWidth(2);
            
            double bridgeAreaX = rootPane.getWidth() * 0.55; // 60% from left (similar to bridge area)
            double comboX = bridgeAreaX - comboText.getBoundsInLocal().getWidth() / 2;
            double centerY = rootPane.getHeight() / 2 - 50;
            comboText.setLayoutX(comboX);
            comboText.setLayoutY(centerY);
            
            // Create combo name text
            Text comboNameText = new Text(combo.getName() + "!");
            comboNameText.setFont(Font.font("Arial", FontWeight.BOLD, 36));
            comboNameText.setFill(Color.WHITE);
            comboNameText.setLayoutX(bridgeAreaX - comboNameText.getBoundsInLocal().getWidth() / 2);
            comboNameText.setLayoutY(centerY + 80);
            
            rootPane.getChildren().addAll(comboText, comboNameText);
            
            // Fade out animation
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.0), comboText);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> rootPane.getChildren().remove(comboText));
            fadeOut.play();
            
            FadeTransition fadeOutName = new FadeTransition(Duration.seconds(1.0), comboNameText);
            fadeOutName.setFromValue(1.0);
            fadeOutName.setToValue(0.0);
            fadeOutName.setOnFinished(e -> rootPane.getChildren().remove(comboNameText));
            fadeOutName.play();
        });
    }

    /**
     * Show visual effect on entity.
     */
    public void showEntityEffect(kuroyale.entitiypack.subclasses.AliveEntity entity, String effectType) {
        System.out.println("Showing " + effectType + " effect on " + entity.getCard().getName());
    }
    
    /**
     * Update combo counter display.
     */
    public void updateComboCounter() {
        javafx.application.Platform.runLater(() -> {
            // Get actual unique combo count from ComboManager
            int actualCount = (comboManager != null) ? comboManager.getUniqueComboCount() : 0;
            comboCounterText.setText("Combos: " + actualCount);
        });
    }
    
    /**
     * Reset UI for new match.
     */
    public void reset() {
        updateComboCounter();
    }
}
