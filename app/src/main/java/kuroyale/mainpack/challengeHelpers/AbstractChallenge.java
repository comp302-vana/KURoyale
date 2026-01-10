package kuroyale.mainpack.challengeHelpers;

import java.util.List;

import kuroyale.cardpack.Card;
import kuroyale.mainpack.challengeHelpers.ChallengeValidator.ValidationResult;
import kuroyale.mainpack.models.Challenge;
import kuroyale.mainpack.models.Challenge.ChallengeType;

/**
 * Template Method Pattern: Defines the skeleton of the challenge algorithm.
 * Common operations such as validation or modification are defined here with hooks for
 * challenge-specific behavior.
 */
public abstract class AbstractChallenge {
    protected final ChallengeType type;

    public AbstractChallenge(ChallengeType type){
        this.type = type;
    }

    /**
     * Template Method: Validates a deck using the challenge-specific validator.
     * Subclasses implement validateDeckImpl() to provide specific validation logic.
     */
    public ValidationResult validateDeck(List<Card> cards) {
        if (cards == null || cards.size() != 8) {
            return ValidationResult.failed("Deck must contain exactly 8 cards");
        }
        return validateDeckImpl(cards);
    }

    /**
     * Hook method: Challenge-specific deck validation (Strategy Pattern).
     * Each challenge implements this differently.
     */
    protected abstract ValidationResult validateDeckImpl(List<Card> cards);
    
    /**
     * Hook method: Challenge-specific cost modification (Decorator Pattern).
     * Default implementation returns original cost (no modification).
     */
    public int getModifiedCost(int originalCost, int cardId) {
        return getModifiedCostImpl(originalCost, cardId);
    }
    
    protected abstract int getModifiedCostImpl(int originalCost, int cardId);

    /**
     * Hook method: Challenge-specific rule display text.
     */
    public abstract String getRuleDisplayText();
    
    public ChallengeType getType() { return type; }
}
