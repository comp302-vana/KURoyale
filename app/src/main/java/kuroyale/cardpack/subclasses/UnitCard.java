package kuroyale.cardpack.subclasses;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardType;
import kuroyale.cardpack.CardCategory;
import kuroyale.cardpack.CardTarget;

public class UnitCard extends Card {

    private int hp;
    private int damage;
    private double hitSpeed;  
    private double range;     
    private String speed;     


       public UnitCard(
            int id, String name, String description, int cost,
            CardCategory category, CardTarget target, CardType type,
            int hp, int damage, double hitSpeed, double range, String speed) {

        super(id, name, description, cost, type, category, target);
        this.hp = hp;
        this.damage = damage;
        this.hitSpeed = hitSpeed;
        this.range = range;
        this.speed = speed;
    }

     public int getHp() { 
        return hp; 
     }

    public int getDamage() { 
        return damage; 
     }
    public double getHitSpeed() { 
        return hitSpeed; 
     }
    public double getRange() { 
        return range;
     }
    public String getSpeed() { 
        return speed; 
     }

}
