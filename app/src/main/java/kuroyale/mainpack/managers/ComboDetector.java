package kuroyale.mainpack.managers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardFactory;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.mainpack.models.Combo.ComboType;

public class ComboDetector {
    private static final double COMBO_WINDOW_SECONDS = 5.0;
    private static final int MAX_TRACKED_CARDS = 10;

    final List<CardPlayRecord> recentCardPlays = new ArrayList<>();
    private final Set<ComboType> triggeredCombosInWindow = new HashSet<>();
    private long lastComboWindowStart = 0;

    /**
     * Record a card play and check for combo triggers.
     * "cardID": Card ID that was played
     * "playedEntity": The entity that was spawned (null for spells)
     * "playerId": Player who played the card (1 or 2)
     * Returns ComboType if a combo was detected, null otherwise
     */
    /**
     * Record a card play and check for combo triggers.
     * "cardID": Card ID that was played
     * "playedEntity": The entity that was spawned (null for spells)
     * "playerId": Player who played the card (1 or 2)
     * "row": Target row
     * "col": Target col
     * Returns ComboType if a combo was detected, null otherwise
     */
    public ComboType onCardPlayed(int cardID, AliveEntity playedEntity, int playerId, int row, int col) {
        long currentTime = System.currentTimeMillis();

        // Clean old card plays outside the window
        cleanOldCardPlays(currentTime);

        // Reset combo window
        if (currentTime - lastComboWindowStart > COMBO_WINDOW_SECONDS * 1000) {
            triggeredCombosInWindow.clear();
            lastComboWindowStart = currentTime;
        }

        // Record the card play
        CardPlayRecord record = new CardPlayRecord(cardID, currentTime, playedEntity, playerId, row, col);
        recentCardPlays.add(record);

        if (recentCardPlays.size() > MAX_TRACKED_CARDS) {
            recentCardPlays.remove(0);
        }

        // Combo trigger check
        return checkForCombos(record);
    }

    /**
     * Remove card plays older than COMBO_WINDOW_SECONDS.
     */
    private void cleanOldCardPlays(long currentTime) {
        recentCardPlays.removeIf(record -> (currentTime - record.timestamp) > COMBO_WINDOW_SECONDS * 1000);
    }

    // Store the two cards that triggered the last combo
    private CardPlayRecord lastComboCard1;
    private CardPlayRecord lastComboCard2;

    /**
     * Check if the newly played card triggers any combos with recent cards.
     */
    private ComboType checkForCombos(CardPlayRecord newRecord) {
        for (CardPlayRecord oldRecord : recentCardPlays) {
            if (oldRecord == newRecord)
                continue;

            // Check if within combo window
            double timeDiff = (newRecord.timestamp - oldRecord.timestamp) / 1000.0;
            if (timeDiff > COMBO_WINDOW_SECONDS)
                continue;

            // Check each combo type
            ComboType combo = detectCombo(oldRecord, newRecord);
            if (combo != null && !triggeredCombosInWindow.contains(combo)) {
                triggeredCombosInWindow.add(combo);
                // Store the two cards that triggered this combo
                lastComboCard1 = oldRecord;
                lastComboCard2 = newRecord;
                return combo;
            }
        }
        return null;
    }

    /**
     * Get the two cards that triggered the last combo.
     */
    public CardPlayRecord[] getLastComboCards() {
        if (lastComboCard1 != null && lastComboCard2 != null) {
            return new CardPlayRecord[] { lastComboCard1, lastComboCard2 };
        }
        return null;
    }

    /**
     * Detect if two card plays form a combo.
     */
    private ComboType detectCombo(CardPlayRecord card1, CardPlayRecord card2) {
        int id1 = card1.cardID;
        int id2 = card2.cardID;

        // 1. Tank + Support Combo
        if ((id1 == 4 || id1 == 1) && isRangedTroop(id2)) {
            return ComboType.TANK_SUPPORT;
        }
        if ((id2 == 4 || id2 == 1) && isRangedTroop(id1)) {
            return ComboType.TANK_SUPPORT;
        }

        // 2. Spell Synergy Combo
        if (isSpell(id1) && isSpell(id2) && id1 != id2) {
            return ComboType.SPELL_SYNERGY;
        }

        // 3. Swarm Attack Combo
        if (isSwarmCard(id1) && isSwarmCard(id2)) {
            return ComboType.SWARM_ATTACK;
        }

        // 4. Building Defense Combo
        if (isBuilding(id1) && isBuilding(id2)) {
            return ComboType.BUILDING_DEFENSE;
        }

        // 5. Air Assault Combo
        if ((id1 == 13 && id2 == 14) || (id1 == 14 && id2 == 13)) {
            return ComboType.AIR_ASSAULT;
        }

        // 6. Royal Combo
        if ((id1 == 1 && id2 == 12) || (id1 == 12 && id2 == 1)) {
            return ComboType.ROYAL_COMBO;
        }

        // 7. Siege Mode Combo
        if (id1 == 18 && isDefensiveBuilding(id2)) {
            return ComboType.SIEGE_MODE;
        }
        if (id2 == 18 && isDefensiveBuilding(id1)) {
            return ComboType.SIEGE_MODE;
        }

        // 8. Rush Attack Combo
        if (id1 == 5 && isLowCostCard(id2)) {
            return ComboType.RUSH_ATTACK;
        }
        if (id2 == 5 && isLowCostCard(id1)) {
            return ComboType.RUSH_ATTACK;
        }

        return null;
    }

    // Helper methods for card type checking
    private boolean isRangedTroop(int cardID) {
        return cardID == 2 || cardID == 12 || cardID == 11 || cardID == 8; // Musketeer, Archers, Spear Goblins, Wizard
    }

    private boolean isSpell(int cardID) {
        return cardID >= 25 && cardID <= 28; // Zap, Arrows, Fireball, Rocket
    }

    private boolean isSwarmCard(int cardID) {
        return cardID == 9 || cardID == 10 || cardID == 11 || cardID == 12 ||
                cardID == 13 || cardID == 14 || cardID == 15; // Skeletons, Goblins, Spear Goblins, Archers, Minions,
                                                              // Minion Horde, Barbarians
    }

    private boolean isBuilding(int cardID) {
        return cardID >= 16 && cardID <= 24;
    }

    private boolean isDefensiveBuilding(int cardID) {
        return cardID == 16 || cardID == 17 || cardID == 19 || cardID == 20; // Cannon, Tesla, Bomb Tower, Inferno Tower
    }

    private boolean isLowCostCard(int cardID) {
        Card card = CardFactory.createCard(cardID);
        return card != null && card.getCost() <= 2;
    }

    public void reset() {
        recentCardPlays.clear();
        triggeredCombosInWindow.clear();
        lastComboWindowStart = 0;
        lastComboCard1 = null;
        lastComboCard2 = null;
    }

    /**
     * Inner class to track card plays.
     */
    public static class CardPlayRecord {
        public final int cardID;
        public final long timestamp;
        public final AliveEntity entity;
        public final int playerId;
        public final int row;
        public final int col;

        public CardPlayRecord(int cardID, long timestamp, AliveEntity entity, int playerId, int row, int col) {
            this.cardID = cardID;
            this.timestamp = timestamp;
            this.entity = entity;
            this.playerId = playerId;
            this.row = row;
            this.col = col;
        }
    }
}
