package kuroyale.mainpack.challengeHelpers;

import java.util.List;

import kuroyale.cardpack.Card;
import kuroyale.mainpack.challengeHelpers.ChallengeValidator.ValidationResult;
import kuroyale.mainpack.models.Challenge.ChallengeType;

/**
 * Strategy Pattern + Decorator Pattern: Budget Battle challenge.
 */
public class BudgetBattleChallenge extends AbstractChallenge {
    //IDs of the cards that cost less than 3 elixir: case 1: 1 (Knight), 6 (Bomber), 9 (Skeletons)
    //10 (Goblins), 11 (Spear Goblins), 12 (Archers), 13 (Minions), 16 (Cannon), 21 (Tombstone)
    //25 (Zap), 26 (Arrows)
    private static final int MAX_ELIXIR_COST = 3;
    //this class is the super of "BUDGET_BATTLE" in Challange.java
    public BudgetBattleChallenge() {
        super(ChallengeType.BUDGET_BATTLE);
    }

    @Override
    protected ValidationResult validateDeckImpl(List<Card> cards) {
        for (Card card : cards) {
            if (card.getCost() > MAX_ELIXIR_COST) {
                return ValidationResult.failed(
                    "Budget Battle: Deck can't have a card that costs more than 3 elixirs!!");
            }
        }

        return ValidationResult.successful();
    }

    @Override
    protected int getModifiedCostImpl(int originalCost, int cardId) {
        return originalCost;
    }

    @Override
    public String getRuleDisplayText() {
        return "Card's elixir cost can't be more than 3!";
    }
}
