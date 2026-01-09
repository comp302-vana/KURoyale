package kuroyale.mainpack.challengeHelpers;

import java.util.List;
import java.util.Set;

import kuroyale.cardpack.Card;
import kuroyale.mainpack.challengeHelpers.ChallengeValidator.ValidationResult;
import kuroyale.mainpack.models.Challenge;

/**
 * Two patterns are used together for the implementation of challenges
 * Strategy Pattern: Concrete implementation of Swarm Master challenge validation strategy.
 * Template Method Pattern: Implements validateDeckImpl() hook method.
 */
public class SwarmMasterChallenge extends AbstractChallenge{
    // Swarm troop card IDs: 9 (Skeletons), 10 (Goblins), 11 (Spear Goblins), 
    // 12 (Archers), 13 (Minions), 14 (Minion Horde), 15 (Barbarians)
    private static final Set<Integer> SWARM_CARD_IDS = Set.of(9, 10, 11, 12, 13, 14, 15);
    
    //this class is the super of "SWARM_MASTER" in Challange.java
    public SwarmMasterChallenge() {
        super(Challenge.ChallengeType.SWARM_MASTER);
    }
    
    @Override
    protected ValidationResult validateDeckImpl(List<Card> cards) {
        int swarmCount = 0;
        for (Card card : cards) {
            if (SWARM_CARD_IDS.contains(card.getId())) {
                swarmCount++;
            }
        }
        
        if (swarmCount < 5) {
            return ValidationResult.failed(
                "Swarm Master: Deck must contain at least 5 swarm cards");
        }
        
        return ValidationResult.successful();
    }
    
    @Override
    protected int getModifiedCostImpl(int originalCost, int cardId) {
        return originalCost; //No modification
    }
    
    
    @Override
    public String getRuleDisplayText() {
        return "Swarm Units Only!";
    }
}
