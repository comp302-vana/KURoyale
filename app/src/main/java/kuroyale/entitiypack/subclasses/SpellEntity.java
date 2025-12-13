package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.subclasses.SpellCard;
import kuroyale.entitiypack.Entity;

public class SpellEntity extends Entity {
    private final SpellCard card;

    public SpellEntity(SpellCard card, boolean isPlayer) {
        super(card, isPlayer);
        this.card = card;
    }

    public SpellCard getCard() {
        return card;
    }
}
