package kuroyale.cardpack;

import java.util.ArrayList;
import java.util.List;

import kuroyale.cardpack.subclasses.UnitCard;
import kuroyale.cardpack.subclasses.BuildingCard;
import kuroyale.cardpack.subclasses.SpellCard;
import kuroyale.mainpack.util.LevelCalculator;

public class CardFactory {
    private static CardFactory INSTANCE;
    public static CardFactory getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CardFactory();
        }
        return INSTANCE;
    }

    /**
     * Creates a card at Level 1 (base stats).
     * @param id Card ID (1-28)
     * @return The card with base stats
     */
    public Card createCard(int id) {
        return createCard(id, 1); // Default to Level 1
    }
    
    /**
     * Creates a card at the specified level with adjusted stats.
     * Level 2: +10% HP and Damage
     * Level 3: +20% HP and Damage
     * @param id Card ID (1-28)
     * @param level Card level (1-3)
     * @return The card with stats adjusted for the level
     */
    public Card createCard(int id, int level) {
        if (level < 1 || level > 3) {
            level = 1; // Default to Level 1 if invalid
        }
        
        // Get base card and apply level multipliers
        Card baseCard = createCardBase(id);
        if (baseCard == null) {
            return null;
        }
        
        // If Level 1, return base card directly
        if (level == 1) {
            return baseCard;
        }
        
        // Apply level multipliers to stats
        return createCardWithLevel(baseCard, level);
    }
    
    /**
     * Creates a card with base stats (internal method).
     */
    private Card createCardBase(int id) {
        switch (id) {

            case 1:
                return new UnitCard(1, "Knight",
                        "A tough soldier with a sword. Good for soaking up damage.",
                        3, CardCategory.SINGLE_TARGET, CardTarget.GROUND, CardType.GROUND,
                        600, 75, 1.1, 0, "Medium", 0.0, 1);
            case 2:
                return new UnitCard(2, "Musketeer",
                        "A ranged shooter. Can hit ground and air targets.",
                        4, CardCategory.SINGLE_TARGET, CardTarget.AIR_GROUND, CardType.GROUND,
                        340, 100, 1.1, 6, "Medium", 0.0, 1);
            case 3:
                return new UnitCard(3, "Mini P.E.K.K.A",
                        "A powerful armored warrior. Slow but deals massive damage.",
                        4, CardCategory.SINGLE_TARGET, CardTarget.GROUND, CardType.GROUND,
                        600, 325, 1.8, 0, "Fast", 0.0, 1);
            case 4:
                return new UnitCard(4, "Giant",
                        "A huge tank unit. Ignores soldiers and attacks buildings/towers only.",
                        5, CardCategory.SINGLE_TARGET, CardTarget.BUILDINGS, CardType.GROUND,
                        2000, 126, 1.5, 0, "Slow", 0.0, 1);
            case 5:
                return new UnitCard(5, "Hog Rider",
                        "Fast unit that rushes toward buildings. Ignores soldiers.",
                        4, CardCategory.SINGLE_TARGET, CardTarget.BUILDINGS, CardType.GROUND,
                        800, 160, 1.5, 0, "Very Fast", 0.0, 1);
            case 6:
                return new UnitCard(6, "Bomber",
                        "Throws bombs that explode on impact.",
                        3, CardCategory.AOE, CardTarget.GROUND, CardType.GROUND,
                        150, 100, 1.9, 4.5, "Medium", 0.5, 1);
            case 7:
                return new UnitCard(7, "Valkyrie",
                        "Spins and damages all nearby enemies.",
                        4, CardCategory.AOE, CardTarget.GROUND, CardType.GROUND,
                        880, 120, 1.5, 0, "Medium", 1.0, 1);
            case 8:
                return new UnitCard(8, "Wizard",
                        "Shoots fireballs that explode.",
                        5, CardCategory.AOE, CardTarget.AIR_GROUND, CardType.GROUND,
                        340, 130, 1.7, 5, "Medium", 1.0, 1);
            case 9:
                return new UnitCard(9, "Skeletons",
                        "Spawns 4 very weak but very fast soldiers.",
                        1, CardCategory.SWARM, CardTarget.GROUND, CardType.GROUND,
                        30, 30, 1, 0, "Fast", 0.0, 4);
            case 10:
                return new UnitCard(10, "Goblins",
                        "Spawns 3 fast, weak melee fighters.",
                        2, CardCategory.SWARM, CardTarget.GROUND, CardType.GROUND,
                        80, 50, 1.1, 0, "Very Fast", 0.0, 3);
            case 11:
                return new UnitCard(11, "Spear Goblins",
                        "Spawns 3 ranged goblins (can hit air).",
                        2, CardCategory.SWARM, CardTarget.AIR_GROUND, CardType.GROUND,
                        52, 24, 1.3, 5, "Very Fast", 0.0, 3);
            case 12:
                return new UnitCard(12, "Archers",
                        "Spawns 2 ranged soldiers.",
                        3, CardCategory.SWARM, CardTarget.AIR_GROUND, CardType.GROUND,
                        125, 40, 1.2, 5, "Medium", 0.0, 2);
            case 13:
                return new UnitCard(13, "Minions",
                        "Spawns 3 flying units.",
                        3, CardCategory.SWARM, CardTarget.AIR_GROUND, CardType.AIR,
                        90, 40, 1.2, 2.5, "Fast", 0.0, 3);
            case 14:
                return new UnitCard(14, "Minion Horde",
                        "Spawns 6 flying units.",
                        5, CardCategory.SWARM, CardTarget.AIR_GROUND, CardType.AIR,
                        90, 40, 1, 2, "Fast", 0.0, 6);
            case 15:
                return new UnitCard(15, "Barbarians",
                        "Spawns 4 tough melee fighters.",
                        5, CardCategory.SWARM, CardTarget.GROUND, CardType.GROUND,
                        300, 75, 1.5, 0, "Fast", 0.0, 4);
            case 16:
                return new BuildingCard(16, "Cannon",
                        "Basic defensive tower.",
                        3, CardCategory.DEFENSIVE_BUILDING, CardTarget.GROUND,
                        450, 60, 0.8, 5.5, 0.0, 30, null);
            case 17:
                return new BuildingCard(17, "Tesla",
                        "Defensive tower that can hit both air and ground.",
                        4, CardCategory.DEFENSIVE_BUILDING, CardTarget.AIR_GROUND,
                        400, 64, 1.1, 5.5, 0.0, 40, null);
            case 18:
                return new BuildingCard(18, "Mortar",
                        "Long-range artillery.",
                        4, CardCategory.DEFENSIVE_BUILDING, CardTarget.GROUND,
                        600, 120, 5, 11, 1.0, 30, null);
            case 19:
                return new BuildingCard(19, "Bomb Tower",
                        "Defensive tower with explosive shells.",
                        5, CardCategory.DEFENSIVE_BUILDING, CardTarget.GROUND,
                        900, 100, 1.6, 6, 1.5, 40, null);
            case 20:
                return new BuildingCard(20, "Inferno Tower",
                        "Shoots a laser that grows stronger over time.",
                        5, CardCategory.DEFENSIVE_BUILDING, CardTarget.AIR_GROUND,
                        800, 20, 0.4, 6, 0.0, 40, null);
            case 21:
                return new BuildingCard(21, "Tombstone",
                        "Spawns 1 Skeleton every 2.9s.",
                        3, CardCategory.SPAWNER_BUILDING, CardTarget.NONE,
                        200, 0, 2.9, 0, 0.0, 40, (UnitCard) createCardBase(9));
            case 22:
                return new BuildingCard(22, "Goblin Hut",
                        "Spawns 1 Spear Goblin every 4.9s.",
                        5, CardCategory.SPAWNER_BUILDING, CardTarget.NONE,
                        700, 0, 4.9, 0, 0.0, 60, (UnitCard) createCardBase(11));
            case 23:
                return new BuildingCard(23, "Barbarian Hut",
                        "Spawns 1 Barbarians every 7s.",
                        7, CardCategory.SPAWNER_BUILDING, CardTarget.NONE,
                        1100, 0, 14, 0, 0.0, 60, (UnitCard) createCardBase(15));
            case 24:
                return new BuildingCard(24, "Elixir Collector",
                        "Generates elixir over time.",
                        5, CardCategory.SPECIAL_BUILDING, CardTarget.NONE,
                        640, 0, 10, 0, 0.0, 70, null);
            case 25:
                return new SpellCard(25, "Zap",
                        "Small area damage + stuns enemies.",
                        2, 80.0, 2.5);
            case 26:
                return new SpellCard(26, "Arrows",
                        "Medium area damage. Good for killing swarms.",
                        3, 115.0, 4.0);
            case 27:
                return new SpellCard(27, "Fireball",
                        "Large area damage. Good for clusters of enemies.",
                        4, 325.0, 2.5);
            case 28:
                return new SpellCard(28, "Rocket",
                        "Massive damage in a small area.",
                        6, 700.0, 2.0);
            default:
                return null;
        }
    }
    
    /**
     * Creates a card with level-adjusted stats from a base card.
     */
    private Card createCardWithLevel(Card baseCard, int level) {
        if (baseCard instanceof UnitCard) {
            UnitCard unit = (UnitCard) baseCard;
            double baseHP = unit.getHp();
            double baseDamage = unit.getDamage();
            
            // Apply level multipliers
            double adjustedHP = LevelCalculator.calculateStat(baseHP, level);
            int adjustedDamage = LevelCalculator.calculateDamage(baseDamage, level);
            
            return new UnitCard(
                unit.getId(),
                unit.getName(),
                unit.getDescription(),
                unit.getCost(),
                unit.getCategory(),
                unit.getTarget(),
                unit.getType(),
                adjustedHP,
                adjustedDamage,
                unit.getActSpeed(),
                unit.getRange(),
                unit.getSpeed(),
                unit.getRadius(),
                unit.getCount()
            );
        } else if (baseCard instanceof BuildingCard) {
            BuildingCard building = (BuildingCard) baseCard;
            double baseHP = building.getHp();
            double baseDamage = building.getDamage();
            
            // Apply level multipliers
            double adjustedHP = LevelCalculator.calculateStat(baseHP, level);
            int adjustedDamage = LevelCalculator.calculateDamage(baseDamage, level);
            
            return new BuildingCard(
                building.getId(),
                building.getName(),
                building.getDescription(),
                building.getCost(),
                building.getCategory(),
                building.getTarget(),
                adjustedHP,
                adjustedDamage,
                building.getActSpeed(),
                building.getRange(),
                building.getRadius(),
                building.getLifetime(),
                building.getSpawnedUnit()
            );
        } else if (baseCard instanceof SpellCard) {
            SpellCard spell = (SpellCard) baseCard;
            double baseDamage = spell.getDamage();
            
            // Apply level multipliers to spell damage
            int adjustedDamage = LevelCalculator.calculateDamage(baseDamage, level);
            
            return new SpellCard(
                spell.getId(),
                spell.getName(),
                spell.getDescription(),
                spell.getCost(),
                adjustedDamage,
                spell.getRadius()
            );
        }
        
        // Fallback: return base card if unknown type
        return baseCard;
    }

    public List<Card> getAllCards() {
        List<Card> list = new ArrayList<>();
        for (int i = 1; i <= 28; i++) {
            list.add(createCard(i));
        }
        return list;
    }
}
