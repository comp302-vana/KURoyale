package kuroyale.mainpack.managers;

import java.util.HashMap;
import java.util.Map;
import kuroyale.entitiypack.subclasses.AliveEntity;

/**
 * Manages combat-related state: attack cooldowns and stun durations.
 * High cohesion: All combat timing state in one place.
 */
public class CombatManager {
    private final Map<AliveEntity, Double> attackCooldowns = new HashMap<>();
    private final Map<AliveEntity, Double> stunDurations = new HashMap<>();
    private final double ENTITY_UPDATE_INTERVAL;

    public CombatManager(double entityUpdateInterval) {
        this.ENTITY_UPDATE_INTERVAL = entityUpdateInterval;
    }

    public boolean isStunned(AliveEntity entity) {
        return stunDurations.containsKey(entity) && stunDurations.get(entity) > 0;
    }

    public double getAttackCooldown(AliveEntity entity) {
        return attackCooldowns.getOrDefault(entity, 0.0);
    }

    public void setAttackCooldown(AliveEntity entity, double cooldown) {
        attackCooldowns.put(entity, cooldown);
    }

    public void setStunDuration(AliveEntity entity, double duration) {
        stunDurations.put(entity, duration);
    }

    public void updateCooldowns(java.util.List<AliveEntity> activeEntities) {
        // Update attack cooldowns
        for (AliveEntity entity : new java.util.ArrayList<>(attackCooldowns.keySet())) {
            if (entity.getHP() <= 0 || !activeEntities.contains(entity)) {
                attackCooldowns.remove(entity);
            } else {
                double currentCooldown = attackCooldowns.get(entity);
                double newCooldown = Math.max(0, currentCooldown - ENTITY_UPDATE_INTERVAL);
                if (newCooldown > 0) {
                    attackCooldowns.put(entity, newCooldown);
                } else {
                    attackCooldowns.remove(entity);
                }
            }
        }

        // Update stun durations
        for (AliveEntity entity : new java.util.ArrayList<>(stunDurations.keySet())) {
            if (entity.getHP() <= 0 || !activeEntities.contains(entity)) {
                stunDurations.remove(entity);
            } else {
                double currentStun = stunDurations.get(entity);
                double newStun = Math.max(0, currentStun - ENTITY_UPDATE_INTERVAL);
                if (newStun > 0) {
                    stunDurations.put(entity, newStun);
                } else {
                    stunDurations.remove(entity);
                }
            }
        }
    }

    public void removeEntity(AliveEntity entity) {
        attackCooldowns.remove(entity);
        stunDurations.remove(entity);
    }
}
