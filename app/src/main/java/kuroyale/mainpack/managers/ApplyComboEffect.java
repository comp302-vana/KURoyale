package kuroyale.mainpack.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import kuroyale.arenapack.ArenaMap;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.BuildingEntity;
import kuroyale.entitiypack.subclasses.UnitEntity;
import kuroyale.mainpack.managers.ComboDetector;
import kuroyale.mainpack.managers.ComboDetector.CardPlayRecord;
import kuroyale.mainpack.models.Combo.ComboType;

/**
 * Handles applying combo effects to entities.
 * Manages effect multipliers and visual indicators.
 */
public class ApplyComboEffect {
    private final ArenaMap arenaMap;
    private ComboVisualEffects visualEffects;
    private final int tileSize;

    // Track entities with combo effects applied
    private final Map<AliveEntity, List<ComboEffect>> entityComboEffects = new HashMap<>();

    public ApplyComboEffect(ArenaMap arenaMap) {
        this.arenaMap = arenaMap;
        this.tileSize = 32; // Default tile size
    }

    /**
     * Set the visual effects manager.
     */
    public void setVisualEffects(ComboVisualEffects visualEffects) {
        this.visualEffects = visualEffects;
    }

    /**
     * Find entity position in the ArenaMap and return [row, col].
     * Returns null if entity not found.
     */
    private int[] findEntityPosition(AliveEntity entity) {
        if (entity == null)
            return null;

        for (int r = 0; r < ArenaMap.getRows(); r++) {
            for (int c = 0; c < ArenaMap.getCols(); c++) {
                if (arenaMap.getEntity(r, c) == entity) {
                    return new int[] { r, c };
                }
            }
        }
        return null;
    }

    /**
     * Schedule a visual effect with delay so entity is properly positioned.
     */
    private void scheduleVisualEffect(AliveEntity entity, Runnable effectAction) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(effectAction);
                timer.cancel();
            }
        }, 300); // 300ms delay
    }

    public void applyComboEffect(ComboType comboType, CardPlayRecord card1, CardPlayRecord card2) {
        switch (comboType) {
            case TANK_SUPPORT:
                applyTankSupportCombo(card1, card2);
                break;
            case SWARM_ATTACK:
                applySwarmAttackCombo(card1, card2);
                break;
            case BUILDING_DEFENSE:
                applyBuildingDefenseCombo(card1, card2);
                break;
            case AIR_ASSAULT:
                applyAirAssaultCombo(card1, card2);
                break;
            case ROYAL_COMBO:
                applyRoyalCombo(card1, card2);
                break;
            case SIEGE_MODE:
                applySiegeModeCombo(card1, card2);
                break;
            case RUSH_ATTACK:
                applyRushAttackCombo(card1, card2);
                break;
            case SPELL_SYNERGY:
                // Elixir refund handled separately in EntityPlacementManager
                break;
            default:
                break;
        }
    }

    private void applyTankSupportCombo(CardPlayRecord card1, CardPlayRecord card2) {
        // Find the ranged troop entity and apply +15% damage
        AliveEntity rangedTroop = isRangedTroop(card1.cardID) ? card1.entity : card2.entity;
        if (rangedTroop != null && rangedTroop instanceof UnitEntity) {
            applyEffect(rangedTroop, ComboEffect.DAMAGE_BOOST_15);

            // Show gold glow visual effect with delay
            if (visualEffects != null) {
                final AliveEntity entity = rangedTroop;
                scheduleVisualEffect(entity, () -> {
                    int[] pos = findEntityPosition(entity);
                    if (pos != null) {
                        double x = pos[1] * tileSize;
                        double y = pos[0] * tileSize;
                        visualEffects.showGoldGlow(entity, x, y);
                    }
                });
            }
        }
    }

    private void applySwarmAttackCombo(CardPlayRecord card1, CardPlayRecord card2) {
        // Apply +10% movement speed to all swarm units
        List<AliveEntity> swarmUnits = new ArrayList<>();
        if (card1.entity != null && isSwarmCard(card1.cardID)) {
            addSwarmUnitsFromEntity(card1.entity, swarmUnits);
        }
        if (card2.entity != null && isSwarmCard(card2.cardID)) {
            addSwarmUnitsFromEntity(card2.entity, swarmUnits);
        }

        for (AliveEntity unit : swarmUnits) {
            if (unit instanceof UnitEntity) {
                applyEffect(unit, ComboEffect.SPEED_BOOST_10);

                // Show speed lines visual effect with delay
                if (visualEffects != null) {
                    final AliveEntity entity = unit;
                    scheduleVisualEffect(entity, () -> {
                        int[] pos = findEntityPosition(entity);
                        if (pos != null) {
                            double x = pos[1] * tileSize;
                            double y = pos[0] * tileSize;
                            visualEffects.showSpeedLines(entity, x, y);
                        }
                    });
                }
            }
        }
    }

    private void applyBuildingDefenseCombo(CardPlayRecord card1, CardPlayRecord card2) {
        // Apply +20% HP to both buildings
        if (card1.entity != null && card1.entity instanceof BuildingEntity) {
            applyEffect(card1.entity, ComboEffect.HP_BOOST_20);
            card1.entity.setHP(card1.entity.getHP() * 1.20);

            // Show shield icon visual effect with delay
            if (visualEffects != null) {
                final AliveEntity entity = card1.entity;
                scheduleVisualEffect(entity, () -> {
                    int[] pos = findEntityPosition(entity);
                    if (pos != null) {
                        double x = pos[1] * tileSize;
                        double y = pos[0] * tileSize;
                        visualEffects.showShieldIcon(entity, x, y);
                    }
                });
            }
        }
        if (card2.entity != null && card2.entity instanceof BuildingEntity) {
            applyEffect(card2.entity, ComboEffect.HP_BOOST_20);
            card2.entity.setHP(card2.entity.getHP() * 1.20);

            // Show shield icon visual effect with delay
            if (visualEffects != null) {
                final AliveEntity entity = card2.entity;
                scheduleVisualEffect(entity, () -> {
                    int[] pos = findEntityPosition(entity);
                    if (pos != null) {
                        double x = pos[1] * tileSize;
                        double y = pos[0] * tileSize;
                        visualEffects.showShieldIcon(entity, x, y);
                    }
                });
            }
        }
    }

    private void applyAirAssaultCombo(CardPlayRecord card1, CardPlayRecord card2) {
        // Apply +15% damage to all air units
        List<AliveEntity> airUnits = new ArrayList<>();
        if (card1.entity != null && card1.entity.canFly()) {
            addSwarmUnitsFromEntity(card1.entity, airUnits);
        }
        if (card2.entity != null && card2.entity.canFly()) {
            addSwarmUnitsFromEntity(card2.entity, airUnits);
        }

        for (AliveEntity unit : airUnits) {
            if (unit.canFly()) {
                applyEffect(unit, ComboEffect.DAMAGE_BOOST_15);
            }
        }
    }

    private void applyRoyalCombo(CardPlayRecord card1, CardPlayRecord card2) {
        // Apply +100 HP to Knight
        AliveEntity knight = (card1.cardID == 1) ? card1.entity : (card2.cardID == 1) ? card2.entity : null;
        if (knight != null && knight instanceof UnitEntity) {
            applyEffect(knight, ComboEffect.HP_BOOST_FIXED_100);
            knight.setHP(knight.getHP() + 100);
        }
    }

    private void applySiegeModeCombo(CardPlayRecord card1, CardPlayRecord card2) {
        // Apply +2 tiles range to Mortar
        AliveEntity mortar = (card1.cardID == 18) ? card1.entity : (card2.cardID == 18) ? card2.entity : null;
        if (mortar != null && mortar instanceof BuildingEntity) {
            applyEffect(mortar, ComboEffect.RANGE_BOOST_2);
        }
    }

    private void applyRushAttackCombo(CardPlayRecord card1, CardPlayRecord card2) {
        // Apply +20% speed to Hog Rider
        AliveEntity hogRider = (card1.cardID == 5) ? card1.entity : (card2.cardID == 5) ? card2.entity : null;
        if (hogRider != null && hogRider instanceof UnitEntity) {
            applyEffect(hogRider, ComboEffect.SPEED_BOOST_20);
        }
    }

    private void addSwarmUnitsFromEntity(AliveEntity entity, List<AliveEntity> list) {
        // For swarm cards, add the entity itself
        // Note: Swarm cards create a single UnitEntity with a count property
        // The count represents multiple units but they're handled as one entity
        // So we apply the effect to the single entity
        if (entity != null && entity instanceof UnitEntity) {
            list.add(entity);
        }
    }

    /**
     * Apply a combo effect to an entity.
     */
    private void applyEffect(AliveEntity entity, ComboEffect effect) {
        if (entity == null || entity.getHP() <= 0)
            return;

        // Store effect on entity
        entityComboEffects.computeIfAbsent(entity, k -> new ArrayList<>()).add(effect);
    }

    /**
     * Get damage multiplier for an entity.
     */
    public double getDamageMultiplier(AliveEntity entity) {
        List<ComboEffect> effects = entityComboEffects.get(entity);
        if (effects == null)
            return 1.0;

        double multiplier = 1.0;
        for (ComboEffect effect : effects) {
            switch (effect) {
                case DAMAGE_BOOST_15:
                    multiplier *= 1.15;
                    break;
                default:
                    break;
            }
        }
        return multiplier;
    }

    /**
     * Get speed multiplier for an entity.
     */
    public double getSpeedMultiplier(AliveEntity entity) {
        List<ComboEffect> effects = entityComboEffects.get(entity);
        if (effects == null)
            return 1.0;

        double multiplier = 1.0;
        for (ComboEffect effect : effects) {
            switch (effect) {
                case SPEED_BOOST_10:
                    multiplier *= 1.10;
                    break;
                case SPEED_BOOST_20:
                    multiplier *= 1.20;
                    break;
                default:
                    break;
            }
        }
        return multiplier;
    }

    /**
     * Get range boost for a building.
     */
    public double getRangeBoost(AliveEntity entity) {
        List<ComboEffect> effects = entityComboEffects.get(entity);
        if (effects == null)
            return 0.0;

        double boost = 0.0;
        for (ComboEffect effect : effects) {
            if (effect == ComboEffect.RANGE_BOOST_2) {
                boost += 2.0;
            }
        }
        return boost;
    }

    /**
     * Remove effects from destroyed entities.
     */
    public void cleanupDestroyedEntity(AliveEntity entity) {
        entityComboEffects.remove(entity);
    }

    /**
     * Reset all effects for a new match.
     */
    public void reset() {
        entityComboEffects.clear();
    }

    // Helper methods
    private boolean isRangedTroop(int cardID) {
        return cardID == 2 || cardID == 12 || cardID == 11 || cardID == 8;
    }

    private boolean isSwarmCard(int cardID) {
        return cardID == 9 || cardID == 10 || cardID == 11 || cardID == 12 ||
                cardID == 13 || cardID == 14 || cardID == 15;
    }

    /**
     * Enum for combo effects.
     */
    public enum ComboEffect {
        DAMAGE_BOOST_15, // +15% damage
        SPEED_BOOST_10, // +10% speed
        SPEED_BOOST_20, // +20% speed
        HP_BOOST_20, // +20% HP
        HP_BOOST_FIXED_100, // +100 HP
        RANGE_BOOST_2 // +2 tiles range
    }
}