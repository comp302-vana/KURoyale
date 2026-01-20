package kuroyale.entitiypack;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardTarget;
import kuroyale.cardpack.CardType;
import kuroyale.entitiypack.subclasses.AliveEntity;

public class Entity {
    private final Card card;
    private final boolean isPlayer;
    private static kuroyale.mainpack.managers.ComboManager comboManager;

    public Entity(Card card, boolean isPlayer) {
        this.card = card;
        this.isPlayer = isPlayer;
    }
    
    public static void setComboManager(kuroyale.mainpack.managers.ComboManager comboManager) {
        Entity.comboManager = comboManager;
    }

    public void attack(AliveEntity other) {
        double damage = getDamage();
        // Apply combo damage multiplier if this entity has combo effects
        if (comboManager != null && this instanceof AliveEntity) {
            damage *= comboManager.getDamageMultiplier((AliveEntity) this);
        }
        other.reduceHP(damage);
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
    
    
}
