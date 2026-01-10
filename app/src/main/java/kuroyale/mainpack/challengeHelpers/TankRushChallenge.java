package kuroyale.mainpack.challengeHelpers;

import java.util.List;
import java.util.Set;

import kuroyale.cardpack.Card;
import kuroyale.mainpack.challengeHelpers.ChallengeValidator.ValidationResult;
import kuroyale.mainpack.models.Challenge.ChallengeType;

public class TankRushChallenge extends AbstractChallenge{
    // High-hp Unit IDs: 1 (Knight), 3 (Mini P.E.K.K.A), 4 (Giant), 
    // 7 (Valkyrie), 15 (Barbarians)
    private static final Set<Integer> HIGH_HP_UNIT_IDS = Set.of(1,3,4,7,15);

    //IDs of the building cards: 16 (Cannon), 17 (Tesla), 18 (Mortar), 19 (Bomb Tower)
    //20 (Inferno Tower), 21 (Tombstone), 22 (Goblin Hut), 23 (Barbarian Hut), 24 (Elixir Collector)
    private static final Set<Integer> FORBIDDEN_BUILDING_IDS = Set.of(16,17,18,19,20,21,22,23,24);

    //IDs of the spell cards: 25 (Zap), 26 (Arrows), 27 (Fireball), 28 (Rocket)
    private static final Set<Integer> FORBIDDEN_SPELL_IDS = Set.of(25, 26, 27, 28);
    
    //this class is the super of "TANK_RUSH" in Challange.java
    public TankRushChallenge() {
        super(ChallengeType.TANK_RUSH);
    }
    
    @Override
    protected ValidationResult validateDeckImpl(List<Card> cards) {
        int tankCount = 0;
        for (Card card : cards) {
            if(HIGH_HP_UNIT_IDS.contains(card.getId())) {
                tankCount++;
            }

            if(FORBIDDEN_BUILDING_IDS.contains(card.getId())){
                return ValidationResult.failed(
                    "Tank Rush: You can't have a building card in your deck!");
            }

            if(FORBIDDEN_SPELL_IDS.contains(card.getId())){
                return ValidationResult.failed(
                    "Tank Rush: You can't have a spell card in your deck!");
            }
        }
        
        if (tankCount < 5) {
            return ValidationResult.failed(
                "Tank Rush: Your deck should include all 5 High-hp units");
        }
        
        return ValidationResult.successful();
    }
    
    @Override
    protected int getModifiedCostImpl(int originalCost, int cardId) {
        return originalCost; //No modification
    }
    
    
    @Override
    public String getRuleDisplayText() {
        return "We need to gather ALL High-HP units that we can!";
    }
}
