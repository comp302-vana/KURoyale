package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.subclasses.BuildingCard;

public class BuildingEntity extends AliveEntity {
    private final BuildingCard card;

    public BuildingEntity(BuildingCard card, boolean isPlayer) {
        super(card, isPlayer);
        this.card = card;
    }

    public void reduceLifetime(double deltaTime) {
        if (getLifetime() == 0) {
            return;
        } else {
            reduceHP(deltaTime / getLifetime());
        }
    }

    protected double getLifetime() {
        return card.getLifetime();
    }
}
