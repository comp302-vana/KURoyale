package kuroyale.cardpack;

import java.util.ArrayList;
import java.util.List;

import kuroyale.cardpack.subclasses.UnitCard;
import kuroyale.cardpack.subclasses.BuildingCard;
import kuroyale.cardpack.subclasses.SpellCard;


public class CardFactory {

 public static Card createCard(int id) {
        switch (id) {

            case 1: return new UnitCard(1, "Knight", 
                "A tough soldier with a sword. Good for soaking up damage.",
                3, CardCategory.SINGLE_TARGET, CardTarget.GROUND, CardType.GROUND,
                600, 75, 1.1, 0, "Medium");

            case 2: return new UnitCard(2, "Musketeer",
                "A ranged shooter. Can hit ground and air targets.",
                4, CardCategory.SINGLE_TARGET, CardTarget.AIR_GROUND, CardType.GROUND,
                340, 100, 1.1, 6.5, "Medium");

            case 3: return new UnitCard(3, "Mini P.E.K.K.A",
                "A powerful armored warrior. Slow but deals massive damage.",
                4, CardCategory.SINGLE_TARGET, CardTarget.GROUND, CardType.GROUND,
                600, 325, 1.8, 0, "Slow");

            case 4: return new UnitCard(4, "Giant",
                "A huge tank unit. Ignores soldiers and attacks buildings/towers only.",
                5, CardCategory.SINGLE_TARGET, CardTarget.BUILDINGS, CardType.GROUND,
                2000, 126, 1.5, 0, "Very Slow");

            case 5: return new UnitCard(5, "Hog Rider",
                "Fast unit that rushes toward buildings. Ignores soldiers.",
                4, CardCategory.SINGLE_TARGET, CardTarget.BUILDINGS, CardType.GROUND,
                800, 160, 1.5, 0, "Fast");

            case 6: return new UnitCard(6, "Bomber",
                "Throws bombs that explode on impact.",
                3, CardCategory.AOE, CardTarget.GROUND, CardType.GROUND,
                150, 100, 1.9, 5, "Medium");

            case 7: return new UnitCard(7, "Valkyrie",
                "Spins and damages all nearby enemies.",
                4, CardCategory.AOE, CardTarget.GROUND, CardType.GROUND,
                880, 120, 1.5, 0, "Medium");

            case 8: return new UnitCard(8, "Wizard",
                "Shoots fireballs that explode.",
                5, CardCategory.AOE, CardTarget.AIR_GROUND, CardType.GROUND,
                340, 130, 1.7, 5, "Medium");

            case 9: return new UnitCard(9, "Skeletons",
                "Spawns 4 very weak but very fast soldiers.",
                1, CardCategory.SWARM, CardTarget.GROUND, CardType.GROUND,
                30, 30, 1.1, 0, "Very Fast");

            case 10: return new UnitCard(10, "Goblins",
                "Spawns 3 fast, weak melee fighters.",
                2, CardCategory.SWARM, CardTarget.GROUND, CardType.GROUND,
                80, 50, 1.1, 0, "Fast");

            case 11: return new UnitCard(11, "Spear Goblins",
                "Spawns 3 ranged goblins (can hit air).",
                2, CardCategory.SWARM, CardTarget.AIR_GROUND, CardType.GROUND,
                52, 24, 1.7, 5.5, "Fast");

            case 12: return new UnitCard(12, "Archers",
                "Spawns 2 ranged soldiers.",
                3, CardCategory.SWARM, CardTarget.AIR_GROUND, CardType.GROUND,
                125, 40, 0.9, 5.5, "Medium");

            case 13: return new UnitCard(13, "Minions",
                "Spawns 3 flying units.",
                3, CardCategory.SWARM, CardTarget.AIR_GROUND, CardType.AIR,
                90, 40, 1.2, 2.5, "Very Fast");

            case 14: return new UnitCard(14, "Minion Horde",
                "Spawns 6 flying units.",
                5, CardCategory.SWARM, CardTarget.AIR_GROUND, CardType.AIR,
                90, 40, 1.2, 2.5, "Very Fast");

            case 15: return new UnitCard(15, "Barbarians",
                "Spawns 4 tough melee fighters.",
                5, CardCategory.SWARM, CardTarget.GROUND, CardType.GROUND,
                300, 75, 1.3, 0, "Fast");


            case 16: return new BuildingCard(16, "Cannon",
                "Basic defensive tower.",
                3, CardCategory.DEFENSIVE_BUILDING, CardTarget.GROUND,
                400, 60, 5.5, 30);

            case 17: return new BuildingCard(17, "Tesla",
                "Defensive tower that can hit both air and ground.",
                4, CardCategory.DEFENSIVE_BUILDING, CardTarget.AIR_GROUND,
                400, 64, 5.5, 40);

            case 18: return new BuildingCard(18, "Mortar",
                "Long-range artillery.",
                4, CardCategory.DEFENSIVE_BUILDING, CardTarget.GROUND,
                600, 108, 11, 30);

            case 19: return new BuildingCard(19, "Bomb Tower",
                "Defensive tower with explosive shells.",
                5, CardCategory.DEFENSIVE_BUILDING, CardTarget.GROUND,
                900, 100, 6, 40);

            case 20: return new BuildingCard(20, "Inferno Tower",
                "Shoots a laser that grows stronger over time.",
                5, CardCategory.DEFENSIVE_BUILDING, CardTarget.AIR_GROUND,
                800, 200, 6, 40);

            case 21: return new BuildingCard(21, "Tombstone",
                "Spawns 1 Skeleton every 2.9s.",
                3, CardCategory.SPAWNER_BUILDING, CardTarget.GROUND,
                200, 0, 0, 40);

            case 22: return new BuildingCard(22, "Goblin Hut",
                "Spawns 1 Spear Goblin every 4.9s.",
                5, CardCategory.SPAWNER_BUILDING, CardTarget.GROUND,
                700, 0, 0, 60);

            case 23: return new BuildingCard(23, "Barbarian Hut",
                "Spawns 2 Barbarians every 14s.",
                7, CardCategory.SPAWNER_BUILDING, CardTarget.GROUND,
                640, 0, 0, 60);

            case 24: return new BuildingCard(24, "Elixir Collector",
                "Generates elixir over time.",
                5, CardCategory.SPECIAL_BUILDING, CardTarget.NONE,
                640, 0, 0, 70);

            case 25: return new SpellCard(25, "Zap",
                "Small area damage + stuns enemies.",
                2, CardCategory.SPELL, CardTarget.NONE,
                80, 2.5);

            case 26: return new SpellCard(26, "Arrows",
                "Medium area damage. Good for killing swarms.",
                3, CardCategory.SPELL, CardTarget.NONE,
                115, 4);

            case 27: return new SpellCard(27, "Fireball",
                "Large area damage. Good for clusters of enemies.",
                4, CardCategory.SPELL, CardTarget.NONE,
                325, 2.5);

            case 28: return new SpellCard(28, "Rocket",
                "Massive damage in a small area.",
                6, CardCategory.SPELL, CardTarget.NONE,
                700, 2);

            default: return null;
        }
    }


  public static List<Card> getAllCards() {
        List<Card> list = new ArrayList<>();
        for (int i = 1; i <= 28; i++) {
            list.add(createCard(i));
        }
        return list;
    }

}
