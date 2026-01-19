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
        // Set reference so ComboUI can get actual combo count
        this.comboUI.setComboManager(this);
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
        
        // Update combo counter first
        comboUI.updateComboCounter();
        
        // Show visual feedback
        comboUI.showComboTrigger(comboType);
        
        // Get the two cards that triggered the combo from detector
        ComboDetector.CardPlayRecord[] comboCards = comboDetector.getLastComboCards();
        if (comboCards != null && comboCards.length == 2) {
            // Apply combo effects with the correct two cards
            effectApplier.applyComboEffect(comboType, comboCards[0], comboCards[1]);
        } else {
            // Fallback: use last two cards if detector didn't store them
            List<ComboDetector.CardPlayRecord> recentPlays = comboDetector.recentCardPlays;
            if (recentPlays.size() >= 2) {
                ComboDetector.CardPlayRecord card1 = recentPlays.get(recentPlays.size() - 2);
                ComboDetector.CardPlayRecord card2 = recentPlays.get(recentPlays.size() - 1);
                effectApplier.applyComboEffect(comboType, card1, card2);
            }
        }
    }
    
    /**
     * Get the last card play record (for spell synergy refund).
     */
    public ComboDetector.CardPlayRecord getLastCardPlay() {
        List<ComboDetector.CardPlayRecord> recentPlays = comboDetector.recentCardPlays;
        if (!recentPlays.isEmpty()) {
            return recentPlays.get(recentPlays.size() - 1);
        }
        return null;
    }
    
    /**
     * Check if last spell should get refund (for Spell Synergy combo).
     * This checks if the last card play triggered a Spell Synergy combo.
     */
    public boolean shouldRefundLastSpell() {
        ComboDetector.CardPlayRecord lastPlay = getLastCardPlay();
        if (lastPlay == null) return false;
        
        // Check if last play was a spell
        if (lastPlay.cardID < 25 || lastPlay.cardID > 28) {
            return false;
        }
        
        // Check if Spell Synergy combo was just triggered (check recent plays)
        List<ComboDetector.CardPlayRecord> recentPlays = comboDetector.recentCardPlays;
        if (recentPlays.size() < 2) {
            return false;
        }
        
        // Get last two plays
        ComboDetector.CardPlayRecord secondLast = recentPlays.get(recentPlays.size() - 2);
        ComboDetector.CardPlayRecord last = recentPlays.get(recentPlays.size() - 1);
        
        // Check if both are spells and different
        boolean secondLastIsSpell = secondLast.cardID >= 25 && secondLast.cardID <= 28;
        boolean lastIsSpell = last.cardID >= 25 && last.cardID <= 28;
        boolean differentSpells = secondLast.cardID != last.cardID;
        
        // Check if within combo window (5 seconds)
        double timeDiff = (last.timestamp - secondLast.timestamp) / 1000.0;
        boolean withinWindow = timeDiff <= 5.0;
        
        return secondLastIsSpell && lastIsSpell && differentSpells && withinWindow;
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
