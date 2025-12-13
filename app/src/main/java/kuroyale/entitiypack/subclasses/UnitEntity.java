package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.subclasses.UnitCard;

public class UnitEntity extends AliveEntity {
    private final UnitCard card;
    private int ticksSinceLastMove = 0;

    public UnitEntity(UnitCard card, boolean isPlayer) {
        super(card, isPlayer);
        this.card = card;
    }

    protected String getSpeed() {
        return card.getSpeed();
    }

    public int getTicksSinceLastMove() {
        return ticksSinceLastMove;
    }

    public void resetTicksSinceLastMove() {
        ticksSinceLastMove = 0;
    }

    public void incTicksSinceLastMove() {
        ticksSinceLastMove++;
    }
}
