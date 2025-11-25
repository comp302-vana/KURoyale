package kuroyale.cardpack.subclasses;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardCategory;
import kuroyale.cardpack.CardTarget;
import kuroyale.cardpack.CardType;

public abstract class AliveCard extends Card{
    private final double hp;
    private final double actSpeed;
    private final double range;

    public AliveCard(
            int id, String name, String description, int cost,double hp, double range, double damage,
            double actSpeed, double radius, CardType type, CardCategory category, CardTarget target) {

        super(id, name, description, cost, damage, radius, type, category, target);
        this.hp = hp;
        this.actSpeed = actSpeed;
        this.range = range;
    }

    public double getHp() {
        return hp;
    }

    public double getActSpeed() {
        return actSpeed;
    }

    public double getRange() {
        return range;
    }
}
