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

    public double getDamage() {
        return card.getDamage();
    }

    public double getRadius() {
        return card.getRadius();
    }

    public Card getCard() {
        return card;
    }

    public int getId() {
        return card.getId();
    }
    
    public CardType getType() {
        return card.getType();
    }

    public CardTarget getTarget() {
        return card.getTarget();
    }

    public boolean isPlayer() { return isPlayer; }

    public int getRow() { 
        return card.getRow(); 
    }

    public int getCol() { 
        return card.getCol(); 
    }

    
    
}
