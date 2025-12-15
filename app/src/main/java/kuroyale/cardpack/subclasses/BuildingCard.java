package kuroyale.cardpack.subclasses;

import kuroyale.cardpack.CardType;
import kuroyale.cardpack.CardCategory;
import kuroyale.cardpack.CardTarget;

public class BuildingCard extends AliveCard {
    private final double lifetime;

    public BuildingCard(
            int id, String name, String description, int cost,
            CardCategory category, CardTarget target,
            double hp, double damage, double actSpeed, double range, double radius, double lifetime) {

        super(id, name, description, cost, hp, range, damage, actSpeed, radius, CardType.BUILDING, category, target);
        this.lifetime = lifetime;
    }

    public double getLifetime() {
        return lifetime;
    }
}