package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.subclasses.UnitCard;

public class UnitEntity extends AliveEntity {
    private final UnitCard card;

    public UnitEntity(UnitCard card, boolean isPlayer) {
        super(card, isPlayer);
        this.card = card;
    }

    protected String getSpeed() {
        return card.getSpeed();
    }
}
