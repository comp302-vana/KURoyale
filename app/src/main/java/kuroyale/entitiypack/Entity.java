package kuroyale.entitiypack;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardTarget;
import kuroyale.cardpack.CardType;
import kuroyale.entitiypack.subclasses.AliveEntity;

public class Entity {
    private final Card card;
    private final boolean isPlayer;

    public Entity(Card card, boolean isPlayer) {
        this.card = card;
        this.isPlayer = isPlayer;
    }

    public void attack(AliveEntity other) {
        other.reduceHP(getDamage());
    }

    protected double getDamage() {
        return card.getDamage();
    }

    protected double getRadius() {
        return card.getRadius();
    }

    protected Card getCard() {
        return card;
    }

    protected int getId() {
        return card.getId();
    }
    
    protected CardType getType() {
        return card.getType();
    }

    protected CardTarget getTarget() {
        return card.getTarget();
    }
}
