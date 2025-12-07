package kuroyale.cardpack.subclasses;

import kuroyale.cardpack.CardType;
import kuroyale.cardpack.CardCategory;
import kuroyale.cardpack.CardTarget;

public class UnitCard extends AliveCard {
    private final String speed;

    public UnitCard(
            int id, String name, String description, int cost,
            CardCategory category, CardTarget target, CardType type,
            double hp, double damage, double actSpeed, double range, String speed, double radius, int row, int col) {

        super(id, name, description, cost, hp, range, damage, actSpeed, radius, type, category, target, row, col);
        this.speed = speed;
    }

    public String getSpeed() {
        return speed;
    }
}
