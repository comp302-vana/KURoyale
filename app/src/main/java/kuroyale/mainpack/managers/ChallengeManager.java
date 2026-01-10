package kuroyale.mainpack.managers;

import java.util.ArrayList;
import java.util.List;

import kuroyale.cardpack.Card;
import kuroyale.mainpack.challengeHelpers.AbstractChallenge;
import kuroyale.mainpack.challengeHelpers.ChallengeFactory;
import kuroyale.mainpack.challengeHelpers.ChallengeValidator;
import kuroyale.mainpack.models.Challenge;
import kuroyale.mainpack.models.PlayerProfile;
import kuroyale.mainpack.models.Challenge.ChallengeType;


//Manages challenge progression, unlocks and completion tracking.
//Uses Factory Pattern to create challenge instances.
public class ChallengeManager {
    private List<Challenge> challenges;
    private Challenge currentChallenge; //Challenges that are currently active.
    private AbstractChallenge currentChallengeRules; //Implementation of challenge rules

    public ChallengeManager(PlayerProfile profile) {
        List<Challenge> profileChallenges = profile.getChallenges();
        this.challenges = new ArrayList<>(profileChallenges);
        // Ensure all challenge types exist
        ensureAllChallengesExist();
    }

    private void ensureAllChallengesExist() {
        java.util.Set<ChallengeType> existingTypes = new java.util.HashSet<>();
        for (Challenge challenge : challenges) {
            existingTypes.add(challenge.getType());
        }
        
        // Add any missing challenge types
        for (ChallengeType type : ChallengeType.values()) {
            if (!existingTypes.contains(type)) {
                challenges.add(new Challenge(type, false, 0, 0, 0));
            }
        }
    }
    
    /**
     * Factory Pattern: Uses ChallengeFactory to create challenge rule instances.
     * Checks if a challenge is unlocked (previous challenge completed).
     */
    public boolean isChallengeUnlocked(ChallengeType type){
        if(type == ChallengeType.SWARM_MASTER){
            return true;//first challenge is always unlocked
        }
        if(type == ChallengeType.SPELL_BARRAGE && getChallenge(ChallengeType.SWARM_MASTER).isCompleted()){
            return true;//these ones are unlocked if the previous one is completed
        }
        if(type == ChallengeType.NO_BUILDINGS_ALLOWED && getChallenge(ChallengeType.SPELL_BARRAGE).isCompleted()){
            return true;
        }
        if(type == ChallengeType.BUDGET_BATTLE && getChallenge(ChallengeType.NO_BUILDINGS_ALLOWED).isCompleted()){
            return true;
        }
        if(type == ChallengeType.TANK_RUSH && getChallenge(ChallengeType.BUDGET_BATTLE).isCompleted()){
            return true;
        }
        return false;
    }

    //Necessary method to get the challenge from the challenge type
    public Challenge getChallenge(ChallengeType type) {
        for (Challenge challenge : challenges) {
            if (challenge.getType() == type) {
                return challenge;
            }
        }
        // Challenge not found - create it
        Challenge newChallenge = new Challenge(type, false, 0, 0, 0);
        challenges.add(newChallenge);
        return newChallenge;
    }

    /**
     * Factory Pattern: Creates challenge rule instance using ChallengeFactory.
     * Sets the current active challenge.
     */
    public boolean startChallenge(ChallengeType type) {
        if(!isChallengeUnlocked(type)){
            return false;
        }

        currentChallenge = getChallenge(type);
        if (currentChallenge != null) {
            //Factory Pattern: Create challenge rules instance
            currentChallengeRules = ChallengeFactory.createChallenge(type);
            currentChallenge.incrementAttempts();
            return true;
        }
        return false;
    }

    //Template Method Pattern: Uses currentChallengeRules (AbstractChallenge)
    //to validate deck using the deck specific validation strategy
    public ChallengeValidator.ValidationResult validateChallengeDeck(List<Card> cards){
        if (currentChallengeRules == null) {
            return ChallengeValidator.ValidationResult.failed("No active challenge");
        }
        // Template Method Pattern: Calls validateDeck() template method
        return currentChallengeRules.validateDeck(cards);
    }

    //Decorator Pattern: Gets modified card cost using challenge-specific modifier.
    public int getModifiedCost(int originalCost, int cardId) {
        if (currentChallengeRules == null) {
            return originalCost;
        }
        // Decorator Pattern: Applies cost modification decorator
        return currentChallengeRules.getModifiedCost(originalCost, cardId);
    }

    public String getCurrentChallengeRuleText() {
        return currentChallengeRules != null ? currentChallengeRules.getRuleDisplayText() : "";
    }

    //method for the calculation of stars for each challenge
    //Template Method Pattern: Common star colculation with challenge specific hooks.
    public int calculateStars(boolean won, int timeSeconds, boolean tookDamage){
        if(!won || currentChallenge == null){
            return 0;
        }

        //1 star: Just completed the challenge
        int stars = 1;
        //2 stars: Completed the challenge under a specific time constraint
        if(timeSeconds < 120){
            stars = 2;
        }
        //3 stars: Completed the challenge under a specific time constraint with no damage taken
        if(!tookDamage && timeSeconds < 110){
            stars = 3;
        }

        return stars;
    }

    public List<Challenge> getAllChallenges() {
        return new ArrayList<>(challenges);
    }
    
    public Challenge getCurrentChallenge() {
        return currentChallenge;
    }
    
    public void clearCurrentChallenge() {
        currentChallenge = null;
        currentChallengeRules = null;
    }
    
    public List<Challenge> getChallenges() {
        return new ArrayList<>(challenges);
    }
}
