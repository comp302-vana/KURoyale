package kuroyale.mainpack.challengeHelpers;

import java.util.List;
import java.util.Set;

import kuroyale.cardpack.Card;
import kuroyale.mainpack.challengeHelpers.ChallengeValidator.ValidationResult;
import kuroyale.mainpack.models.Challenge.ChallengeType;

/**
 * Strategy Pattern + Decorator Pattern: No Buildings Allowed challenge.
 * Template Method Pattern: Implements validateDeckImpl() and canPlayCardImpl() hooks.
 */
public class NoBuildingsAllowedChallenge extends AbstractChallenge {
    //IDs of the building cards: 16 (Cannon), 17 (Tesla), 18 (Mortar), 19 (Bomb Tower)
    //20 (Inferno Tower), 21 (Tombstone), 22 (Goblin Hut), 23 (Barbarian Hut), 24 (Elixir Collector)
    private static final Set<Integer> FORBIDDEN_BUILDING_IDS = Set.of(16,17,18,19,20,21,22,23,24);

    //this class is the super of "NO_BUILDINGS_ALLOWED" in Challange.java
    public NoBuildingsAllowedChallenge() {
        super(ChallengeType.NO_BUILDINGS_ALLOWED);
    }

    @Override
    protected ValidationResult validateDeckImpl(List<Card> cards) {
        for (Card card : cards) {
            if (FORBIDDEN_BUILDING_IDS.contains(card.getId())) {
                return ValidationResult.failed(
                    "No Buildings Allowed: Deck SHOULDN'T contain any building cards!");
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
        return "No building allowed!";
    }
}
