package kuroyale.mainpack.challengeHelpers;

/**
 * Decorator Pattern: Interface for applying challenge-specific modifications to rules in game.
 * Allows challenges to modify challenges.
 */
public interface ChallengeModifier {
    //gets the modified elixir cost for a card
    //"originalCost": The original card cost
    //"cardId": the card id
    //returns the new cost if modified.
    int getModifiedCost(int originalCost, int cardId);

    //displayes the specific challenge rules
    //returns the rule description to display during the match
    String getRuleDisplayText();
}
