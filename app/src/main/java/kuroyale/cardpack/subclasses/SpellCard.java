package kuroyale.cardpack.subclasses;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardType;
import kuroyale.cardpack.CardCategory;
import kuroyale.cardpack.CardTarget;

public class SpellCard extends Card {
    public SpellCard(int id, String name, String description, int cost, double damage, double radius, int row, int col) {
        super(id, name, description, cost, damage, radius, CardType.SPELL, CardCategory.SPELL, CardTarget.NONE, row, col);
    }
}
