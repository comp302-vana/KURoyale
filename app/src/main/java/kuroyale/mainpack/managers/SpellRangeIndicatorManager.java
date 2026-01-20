package kuroyale.mainpack.managers;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardFactory;
import kuroyale.cardpack.CardType;

/**
 * Manages the visual indicator for spell effect area during card drag.
 * Shows a semi-transparent circle representing the spell's radius when
 * a spell card is being dragged over the arena.
 */
public class SpellRangeIndicatorManager {

    private final Pane overlayPane;
    private final Circle rangeIndicator;
    private final int tileSize;

    // Currently tracked spell card ID (-1 means no spell being dragged)
    private int currentSpellId = -1;

    // Spell colors for different spell types
    private static final Color ZAP_COLOR = Color.rgb(255, 255, 100, 0.35); // Yellow
    private static final Color ARROWS_COLOR = Color.rgb(200, 100, 255, 0.35); // Purple
    private static final Color FIREBALL_COLOR = Color.rgb(255, 100, 50, 0.35); // Orange-red
    private static final Color ROCKET_COLOR = Color.rgb(255, 50, 50, 0.35); // Red
    private static final Color DEFAULT_SPELL_COLOR = Color.rgb(150, 100, 255, 0.35); // Default purple

    public SpellRangeIndicatorManager(Pane overlayPane, int tileSize) {
        this.overlayPane = overlayPane;
        this.tileSize = tileSize;

        // Create the range indicator circle
        this.rangeIndicator = new Circle();
        this.rangeIndicator.setFill(DEFAULT_SPELL_COLOR);
        this.rangeIndicator.setStroke(Color.rgb(255, 255, 255, 0.6));
        this.rangeIndicator.setStrokeWidth(2);
        this.rangeIndicator.setVisible(false);
        this.rangeIndicator.setMouseTransparent(true); // Don't interfere with drag events

        // Add to overlay pane
        overlayPane.getChildren().add(rangeIndicator);
    }

    /**
     * Check if the given card ID is a spell card (IDs 25-28).
     */
    public static boolean isSpellCard(int cardId) {
        return cardId >= 25 && cardId <= 28;
    }

    /**
     * Get the appropriate color for a spell.
     */
    private Color getSpellColor(int spellId) {
        switch (spellId) {
            case 25:
                return ZAP_COLOR;
            case 26:
                return ARROWS_COLOR;
            case 27:
                return FIREBALL_COLOR;
            case 28:
                return ROCKET_COLOR;
            default:
                return DEFAULT_SPELL_COLOR;
        }
    }

    /**
     * Show the spell range indicator for the given spell card at the specified
     * position.
     * 
     * @param spellCardId The ID of the spell card (25-28)
     * @param centerX     The X coordinate (in pixels) for the center of the
     *                    indicator
     * @param centerY     The Y coordinate (in pixels) for the center of the
     *                    indicator
     */
    public void showIndicator(int spellCardId, double centerX, double centerY) {
        if (!isSpellCard(spellCardId)) {
            hideIndicator();
            return;
        }

        // Get the spell card to retrieve its radius
        Card card = CardFactory.createCard(spellCardId);
        if (card == null || card.getType() != CardType.SPELL) {
            hideIndicator();
            return;
        }

        double radius = card.getRadius();
        double radiusInPixels = radius * tileSize;

        // Update indicator properties
        currentSpellId = spellCardId;
        rangeIndicator.setRadius(radiusInPixels);
        rangeIndicator.setCenterX(centerX);
        rangeIndicator.setCenterY(centerY);
        rangeIndicator.setFill(getSpellColor(spellCardId));
        rangeIndicator.setVisible(true);

        // Ensure indicator is on top
        rangeIndicator.toFront();
    }

    /**
     * Update the position of the spell range indicator.
     * 
     * @param centerX The new X coordinate (in pixels)
     * @param centerY The new Y coordinate (in pixels)
     */
    public void updatePosition(double centerX, double centerY) {
        if (rangeIndicator.isVisible()) {
            rangeIndicator.setCenterX(centerX);
            rangeIndicator.setCenterY(centerY);
        }
    }

    /**
     * Hide the spell range indicator.
     */
    public void hideIndicator() {
        rangeIndicator.setVisible(false);
        currentSpellId = -1;
    }

    /**
     * Check if an indicator is currently visible.
     */
    public boolean isVisible() {
        return rangeIndicator.isVisible();
    }

    /**
     * Get the currently tracked spell card ID.
     */
    public int getCurrentSpellId() {
        return currentSpellId;
    }
}
