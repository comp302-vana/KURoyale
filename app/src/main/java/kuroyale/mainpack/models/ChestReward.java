package kuroyale.mainpack.models;

import kuroyale.cardpack.Card;

/**
 * Represents the reward from opening a chest.
 */
public class ChestReward {
    private final int goldAmount;
    private final Card unlockedCard;
    private final boolean isNewCard;

    public ChestReward(int goldAmount, Card unlockedCard, boolean isNewCard) {
        this.goldAmount = goldAmount;
        this.unlockedCard = unlockedCard;
        this.isNewCard = isNewCard;
    }

    public int getGoldAmount() {
        return goldAmount;
    }

    public Card getUnlockedCard() {
        return unlockedCard;
    }

    public boolean isNewCard() {
        return isNewCard;
    }

    public boolean hasCard() {
        return unlockedCard != null;
    }
}
