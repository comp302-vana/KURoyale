package kuroyale.mainpack.managers;

import java.util.*;
import javafx.scene.layout.Pane;
import kuroyale.arenapack.ArenaMap;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.mainpack.models.Combo.ComboType;
/**
 * Main orchestrator for combo system.
 * Coordinates detection, effect application, and UI feedback.
 * Much lighter than before - delegates to specialized classes.
 */
public class ComboManager {
    private final ComboDetector comboDetector;
    private final ApplyComboEffect effectApplier;
    private final ComboUI comboUI;

    // Track unique combos triggered this match
    private final Set<ComboType> uniqueCombosThisMatch = new HashSet<>();
    
    public ComboManager(ArenaMap arenaMap, EntityRenderer entityRenderer, Pane rootPane) {
        this.comboDetector = new ComboDetector();
        this.effectApplier = new ApplyComboEffect(arenaMap);
        this.comboUI = new ComboUI(rootPane);
    }

    /**
     * Called when a card is played. Checks for combo triggers.
     */
    public void onCardPlayed(int cardID, AliveEntity playedEntity, int playerId) {
        ComboType detectedCombo = comboDetector.onCardPlayed(cardID, playedEntity, playerId);
        
        if (detectedCombo != null) {
            triggerCombo(detectedCombo);
        }
    }

    /**
     * Trigger a combo and apply its effects.
     */
    private void triggerCombo(ComboType comboType) {
        System.out.println("COMBO TRIGGERED: " + comboType.getName());
        
        // Track unique combos
        uniqueCombosThisMatch.add(comboType);
        
        // Show visual feedback
        comboUI.showComboTrigger(comboType);
        
        // Get the two cards that triggered the combo
        List<ComboDetector.CardPlayRecord> recentPlays = comboDetector.recentCardPlays;
        if (recentPlays.size() >= 2) {
            ComboDetector.CardPlayRecord card1 = recentPlays.get(recentPlays.size() - 2);
            ComboDetector.CardPlayRecord card2 = recentPlays.get(recentPlays.size() - 1);
            
            // Apply combo effects
            effectApplier.applyComboEffect(comboType, card1, card2);
        }
    }

    /**
     * Get damage multiplier for an entity (delegates to effect applier).
     */
    public double getDamageMultiplier(AliveEntity entity) {
        return effectApplier.getDamageMultiplier(entity);
    }

    /**
     * Get speed multiplier for an entity (delegates to effect applier).
     */
    public double getSpeedMultiplier(AliveEntity entity) {
        return effectApplier.getSpeedMultiplier(entity);
    }
    
    /**
     * Get range boost for a building (delegates to effect applier).
     */
    public double getRangeBoost(AliveEntity entity) {
        return effectApplier.getRangeBoost(entity);
    }
    
    /**
     * Cleanup destroyed entity effects.
     */
    public void cleanupDestroyedEntity(AliveEntity entity) {
        effectApplier.cleanupDestroyedEntity(entity);
    }
    
    /**
     * Reset combo tracking for a new match.
     */
    public void resetForNewMatch() {
        comboDetector.reset();
        effectApplier.reset();
        uniqueCombosThisMatch.clear();
        comboUI.reset();
    }
    
    /**
     * Get number of unique combos triggered this match.
     */
    public int getUniqueComboCount() {
        return uniqueCombosThisMatch.size();
    }

    /**
     * Get gold reward for combos (10 gold per unique combo).
     */
    public int getComboGoldReward() {
        return uniqueCombosThisMatch.size() * 10;
    }
    
    /**
     * Get list of unique combos triggered this match.
     */
    public Set<ComboType> getUniqueCombos() {
        return new HashSet<>(uniqueCombosThisMatch);
    }
}
