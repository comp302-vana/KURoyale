package kuroyale.cardpack.subclasses;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardType;
import kuroyale.cardpack.CardCategory;
import kuroyale.cardpack.CardTarget;

public class SpellCard extends Card {

    private int areaDamage;
    private double radius;

    public SpellCard(
            int id, String name, String description, int cost,
            CardCategory category, CardTarget target,
            int areaDamage, double radius) {

        super(id, name, description, cost, CardType.SPELL, category, target);
        this.areaDamage = areaDamage;
        this.radius = radius;
    }

    public int getAreaDamage() {
        return areaDamage;
    }

    public double getRadius() {
        return radius;
    }
}
