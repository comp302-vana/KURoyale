package kuroyale.mainpack.challengeHelpers;

import java.util.List;
import java.util.Set;

import kuroyale.cardpack.Card;
import kuroyale.mainpack.challengeHelpers.ChallengeValidator.ValidationResult;
import kuroyale.mainpack.models.Challenge.ChallengeType;

/**
 * Three patterns are used together for the implementation of challenges
 * Strategy Pattern: Concrete implementation of Spell Barrage challenge validation strategy.
 * Template Method Pattern: Implements validateDeckImpl() hook method.
 * Decorator Pattern: Implements getModifiedCostImpl() to reduce spell costs by 1.
 */
public class SpellBarrageChallenge extends AbstractChallenge{
    //IDs of the spell cards: 25 (Zap), 26 (Arrows), 27 (Fireball), 28 (Rocket)
    private static final Set<Integer> REQUIRED_SPELL_IDS = Set.of(25, 26, 27, 28);

    //this class is the super of "SPELL_BARRAGE" in Challange.java
    public SpellBarrageChallenge() {
        super(ChallengeType.SPELL_BARRAGE);
    }

    @Override
    protected ValidationResult validateDeckImpl(List<Card> cards) {
        int spellCount = 0;
        for (Card card : cards) {
            if (REQUIRED_SPELL_IDS.contains(card.getId())) {
                spellCount++;
            }
        }
        
        if (spellCount != 4) {
            return ValidationResult.failed(
                "Spell Barrage: Deck must contain ALL 4 spell cards");
        }
        
        return ValidationResult.successful();
    }

    @Override
    protected int getModifiedCostImpl(int originalCost, int cardId) {
        //Decorator Pattern: Spell costs are reduced by 1
        if (REQUIRED_SPELL_IDS.contains(cardId)) {
                return (originalCost - 1);
            }
        return originalCost;
    }

    @Override
    public String getRuleDisplayText() {
        return "Must Include ALL Spell Cards!";
    }
}
