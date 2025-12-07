package kuroyale.cardpack.subclasses;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardCategory;
import kuroyale.cardpack.CardTarget;
import kuroyale.cardpack.CardType;

public abstract class AliveCard extends Card{
    private final double hp;
    private final double actSpeed;
    private final double range;
    private final int row;
    private final int col;

    public AliveCard(
            int id, String name, String description, int cost,double hp, double range, double damage,
            double actSpeed, double radius, CardType type, CardCategory category, CardTarget target, int row, int col) {

        super(id, name, description, cost, damage, radius, type, category, target, row, col);
        this.hp = hp;
        this.actSpeed = actSpeed;
        this.range = range;
        this.row = row;
        this.col = col;
    }

    public double getHp() {
        return hp;
    }

    public double getActSpeed() {
        return actSpeed;
    }

    public double getRange() {
        return range;
    }

    public int getRow() { 
        return row; 
    }
    public int getCol() { 
        return col; 
    }
}
