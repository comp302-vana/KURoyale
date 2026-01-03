package kuroyale.mainpack.models;

/**
 * Tracks per-match statistics for quest evaluation.
 * This is separate from Quest to maintain single responsibility.
 */
public class MatchState {
    private int cardsPlayed = 0;
    private int elixirSpent = 0;
    private boolean lostCrownTower = false;
    private boolean usedOnlyCommonCards = true;
    
    public MatchState() {
        reset();
    }
    
    /**
     * Resets all match tracking to initial state.
     * Call this at the start of each new match.
     */
    public void reset() {
        this.cardsPlayed = 0;
        this.elixirSpent = 0;
        this.lostCrownTower = false;
        this.usedOnlyCommonCards = true;
    }
    
    // Getters
    public int getCardsPlayed() {
        return cardsPlayed;
    }
    
    public int getElixirSpent() {
        return elixirSpent;
    }
    
    public boolean hasLostCrownTower() {
        return lostCrownTower;
    }
    
    public boolean hasUsedOnlyCommonCards() {
        return usedOnlyCommonCards;
    }
    
    // Setters/Updaters
    public void incrementCardsPlayed() {
        this.cardsPlayed++;
    }
    
    public void addElixirSpent(int amount) {
        this.elixirSpent += amount;
    }
    
    public void setLostCrownTower(boolean lost) {
        this.lostCrownTower = lost;
    }
    
    public void setUsedOnlyCommonCards(boolean onlyCommon) {
        this.usedOnlyCommonCards = onlyCommon;
    }
}