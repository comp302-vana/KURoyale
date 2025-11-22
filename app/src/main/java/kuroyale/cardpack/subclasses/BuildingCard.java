package kuroyale.cardpack.subclasses;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardType;
import kuroyale.cardpack.CardCategory;
import kuroyale.cardpack.CardTarget;


public class BuildingCard extends Card {

    private int hp;
    private int damage;
    private double range;
    private double lifetime;

    public BuildingCard(
            int id, String name, String description, int cost,
            CardCategory category, CardTarget target,
            int hp, int damage, double range, double lifetime) {

        super(id, name, description, cost, CardType.BUILDING, category, target);
        this.hp = hp;
        this.damage = damage;
        this.range = range;
        this.lifetime = lifetime;
    }

        public int getHp() {
        return hp;
    }

    public int getDamage() {
        return damage;
    }

    public double getRange() {
        return range;
    }

    public double getLifetime() {
        return lifetime;
    }

}