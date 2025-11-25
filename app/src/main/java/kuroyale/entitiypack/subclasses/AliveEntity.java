package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.subclasses.AliveCard;
import kuroyale.entitiypack.Entity;

public class AliveEntity extends Entity {
    private AliveCard card;
    private double HP;

    public AliveEntity(AliveCard card) {
        super(card);
        this.card = card;
        this.HP = card.getHp();
    }

    public void attack(AliveEntity other) {
        other.reduceHP(getDamage());
    }

    public void reduceHP(double damage) {
        HP -= damage;
        if (HP < 0) {

        }
    }

    protected double getHP() {
        return HP;
    }

    protected void setHP(double value) {
        HP = value;
    }

    protected double getRange() {
        return card.getRange();
    }
}
