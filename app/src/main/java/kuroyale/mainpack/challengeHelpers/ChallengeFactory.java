package kuroyale.mainpack.challengeHelpers;

import kuroyale.mainpack.models.Challenge;

/**
 * Factory Pattern: Creates challenge instances based on challenge type.
 * Encapsulates the construction process.
 */
public class ChallengeFactory {
    /**
     * Factory Method: Creates a challenge instance based on the challenge type.
     * "type": The challenge type
     * returns An AbstractChallenge instance that implements the specific challenge rules
     */
    public static AbstractChallenge createChallenge(Challenge.ChallengeType type) {
        switch (type) {
            case SWARM_MASTER:
                return new SwarmMasterChallenge();
            case SPELL_BARRAGE:
                return new SpellBarrageChallenge();
            case NO_BUILDINGS_ALLOWED:
                return new NoBuildingsAllowedChallenge();
            case BUDGET_BATTLE:
                return new BudgetBattleChallenge();
            case TANK_RUSH:
                return new TankRushChallenge();
            default:
                throw new IllegalArgumentException("Unknown challenge type: " + type);
        }
    }
}