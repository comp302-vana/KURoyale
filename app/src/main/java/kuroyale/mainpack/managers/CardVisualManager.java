package kuroyale.mainpack.managers;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import kuroyale.cardpack.CardRarity;

/**
 * Manages visual enhancements for cards: rarity borders and level indicators.
 * High cohesion: All card visual styling logic in one place.
 */
public class CardVisualManager {

    /**
     * Gets the border color for a card rarity.
     */
    public static String getRarityBorderColor(CardRarity rarity) {
        switch (rarity) {
            case COMMON:
                return "#CCCCCC"; // Gray/White
            case RARE:
                return "#4A90E2"; // Blue
            case EPIC:
                return "#9C27B0"; // Purple
            case LEGENDARY:
                return "#FF9800"; // Orange/Gold
            default:
                return "#333333"; // Default gray
        }
    }

    /**
     * Applies rarity border styling to a card button.
     * 
     * @param cardButton The button node to style
     * @param rarity     The card's rarity
     */
    public static void applyRarityBorder(javafx.scene.control.Button cardButton, CardRarity rarity) {
        if (cardButton == null)
            return;

        String borderColor = getRarityBorderColor(rarity);
        String currentStyle = cardButton.getStyle();

        // Replace border color in existing style, or add it if not present
        if (currentStyle.contains("-fx-border-color:")) {
            // Replace existing border color
            currentStyle = currentStyle.replaceAll("-fx-border-color:\\s*[^;]+;",
                    "-fx-border-color: " + borderColor + ";");
        } else {
            // Add border color to style
            currentStyle += " -fx-border-color: " + borderColor + ";";
        }

        cardButton.setStyle(currentStyle);
    }

    /**
     * Creates a level indicator node showing "Level X" text.
     * 
     * @param level Card level (1-3)
     * @return A Label node displaying the level
     */
    public static Node createLevelIndicator(int level) {
        Label levelLabel = new Label();

        // Display level as text: "Level 1", "Level 2", etc.
        levelLabel.setText("Level " + level);

        levelLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        levelLabel.setTextFill(Color.WHITE);
        levelLabel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.7); " +
                        "-fx-background-radius: 3; " +
                        "-fx-padding: 2 6 2 6;");

        return levelLabel;
    }

    /**
     * Adds a level indicator to a card container.
     * Positioned in the top-right corner.
     * 
     * @param cardContainer The AnchorPane containing the card
     * @param level         The card's level (1-3)
     */
    public static void applyLevelIndicator(AnchorPane cardContainer, int level) {
        if (cardContainer == null)
            return;

        Node levelIndicator = createLevelIndicator(level);
        cardContainer.getChildren().add(levelIndicator);

        // Position in top-right corner
        AnchorPane.setTopAnchor(levelIndicator, 5.0);
        AnchorPane.setRightAnchor(levelIndicator, 5.0);
    }

    /**
     * Updates or adds level indicator to a card container.
     * Removes existing level indicator first if present.
     */
    public static void updateLevelIndicator(AnchorPane cardContainer, int level) {
        if (cardContainer == null)
            return;

        // Remove existing level indicator (look for Label with "Level" text)
        cardContainer.getChildren().removeIf(node -> node instanceof Label &&
                ((Label) node).getText() != null &&
                ((Label) node).getText().startsWith("Level"));

        // Add new level indicator
        applyLevelIndicator(cardContainer, level);
    }
}
