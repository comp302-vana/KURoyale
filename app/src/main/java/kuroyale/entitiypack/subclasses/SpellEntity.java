package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.subclasses.SpellCard;
import kuroyale.entitiypack.Entity;

public class SpellEntity extends Entity {
    private final SpellCard card;

    public SpellEntity(SpellCard card) {
        super(card);
        this.card = card;
    }
}
