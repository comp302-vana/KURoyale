package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.subclasses.BuildingCard;

public class BuildingEntity extends AliveEntity {
    private final BuildingCard card;
    private final double initialHealth;

    public BuildingEntity(BuildingCard card, boolean isPlayer) {
        super(card, isPlayer);
        this.card = card;
        this.initialHealth = getHP();
    }

    public void reduceLifetime(double deltaTime) {
        if (getLifetime() == 0) {
            return;
        } else {
            double hpLoss = initialHealth / getLifetime();
            reduceHP(deltaTime * hpLoss);
        }
    }

    protected double getLifetime() {
        return card.getLifetime();
    }
}
