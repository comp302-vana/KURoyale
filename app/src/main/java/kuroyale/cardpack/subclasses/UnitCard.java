package kuroyale.cardpack.subclasses;

import kuroyale.cardpack.CardType;
import kuroyale.cardpack.CardCategory;
import kuroyale.cardpack.CardTarget;

public class UnitCard extends AliveCard {
    private final String speed;
    private int count;

    public UnitCard(
            int id, String name, String description, int cost,
            CardCategory category, CardTarget target, CardType type,
            double hp, double damage, double actSpeed, double range, String speed, double radius, int count) {

        super(id, name, description, cost, hp, range, damage, actSpeed, radius, type, category, target);
        this.speed = speed;
        this.count = count;
    }

    public String getSpeed() {
        return speed;
    }

    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
}
