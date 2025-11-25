package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.subclasses.UnitCard;

public class UnitEntity extends AliveEntity {
    private final UnitCard card;

    public UnitEntity(UnitCard card) {
        super(card);
        this.card = card;
    }

    protected String getSpeed() {
        return card.getSpeed();
    }
}
