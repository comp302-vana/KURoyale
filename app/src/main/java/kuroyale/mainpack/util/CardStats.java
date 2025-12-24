package kuroyale.mainpack.util;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardFactory;
import kuroyale.cardpack.subclasses.AliveCard;

/**
 * Utility class for calculating card stats at different levels.
 */
public class CardStats {
    
    /**
     * Gets the HP for a card at a specific level.
     */
    public static double getHP(int cardId, int level) {
        Card baseCard = CardFactory.createCard(cardId);
        if (baseCard instanceof AliveCard) {
            double baseHP = ((AliveCard) baseCard).getHp();
            return LevelCalculator.calculateStat(baseHP, level);
        }
        return 0;
    }
    
    /**
     * Gets the Damage for a card at a specific level.
     */
    public static double getDamage(int cardId, int level) {
        Card baseCard = CardFactory.createCard(cardId);
        if (baseCard != null) {
            double baseDamage = baseCard.getDamage();
            return LevelCalculator.calculateDamage(baseDamage, level);
        }
        return 0;
    }
    
    /**
     * Gets the HP rounded to integer for display.
     */
    public static int getHPRounded(int cardId, int level) {
        return (int) Math.round(getHP(cardId, level));
    }
    
    /**
     * Gets the Damage as integer.
     */
    public static int getDamageRounded(int cardId, int level) {
        return (int) Math.round(getDamage(cardId, level));
    }
}
