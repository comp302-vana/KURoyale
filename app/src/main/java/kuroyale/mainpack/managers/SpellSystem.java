package kuroyale.mainpack.managers;

import java.util.HashSet;
import java.util.Set;
import kuroyale.arenapack.ArenaMap;
import kuroyale.cardpack.CardFactory;
import kuroyale.cardpack.subclasses.SpellCard;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.TowerEntity;

/**
 * Handles spell execution and effects.
 * High cohesion: All spell-related logic in one place.
 */
public class SpellSystem {
    private final ArenaMap arenaMap;
    private final CombatManager combatManager;
    private final int rows;
    private final int cols;
    private QuestManager questManager;

    public SpellSystem(ArenaMap arenaMap, CombatManager combatManager, int rows, int cols) {
        this.arenaMap = arenaMap;
        this.combatManager = combatManager;
        this.rows = rows;
        this.cols = cols;
    }

    public void setQuestManager(QuestManager questManager) {
        this.questManager = questManager;
    }

    public void executeSpell(int spellCardID, int targetRow, int targetCol, boolean isPlayerSpell,
                            EntityRenderer entityRenderer) {
        SpellCard spellCard = (SpellCard) CardFactory.createCard(spellCardID);
        double damage = spellCard.getDamage();
        double radius = spellCard.getRadius();
        boolean isZap = (spellCardID == 25);
        
        Set<AliveEntity> affectedEntities = new HashSet<>();
        
        int minRow = Math.max(0, (int)(targetRow - radius - 1));
        int maxRow = Math.min(rows - 1, (int)(targetRow + radius + 1));
        int minCol = Math.max(0, (int)(targetCol - radius - 1));
        int maxCol = Math.min(cols - 1, (int)(targetCol + radius + 1));
        
        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                double rowDist = Math.abs(r - targetRow);
                double colDist = Math.abs(c - targetCol);
                double distance = rowDist + colDist;
                
                if (distance <= radius + 0.5) {
                    AliveEntity entity = arenaMap.getEntity(r, c);
                    if (entity != null && entity.getHP() > 0 && entity.isPlayer() != isPlayerSpell) {
                        affectedEntities.add(entity);
                    }
                }
            }
        }

        int totalDamageDealt = 0;
        for (AliveEntity entity : affectedEntities) {
            double actualDamage = damage;
            if (entity instanceof TowerEntity) {
                actualDamage = damage * 0.4; // towers got affected by %40 of the damage
            }
            
            entity.reduceHP(actualDamage);

            if (isPlayerSpell) {
                totalDamageDealt += (int)actualDamage;
            }

            if (isZap) {
                combatManager.setStunDuration(entity, 0.5);
            }
            
            // Entity removal handled by caller
        }

        if (questManager != null && isPlayerSpell) {
            questManager.onSpellDamageDealt(totalDamageDealt);
        }
        System.out.println("Spell " + spellCard.getName() + " cast at (" + targetRow + ", " + targetCol + ")");
    }
}
