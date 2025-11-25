package kuroyale.entitiypack;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardTarget;
import kuroyale.cardpack.CardType;

public class Entity {
    private final Card card;

    public Entity(Card card) {
        this.card = card;
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
