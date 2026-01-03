package kuroyale.mainpack.models;

public class PlayerStatistics {
    private int totalMatchesPlayed;
    private int totalMatchesWon;
    private int totalCrownTowersDestroyed;
    private int totalKingTowersDestroyed;
    private int totalSpellCardsPlayed;
    private int totalTroopCardsDeployed;
    private int totalBuildingCardsPlayed;
    private int totalElixirSpent;
    private int totalSpellDamageDealt;
    private int totalSwarmTroopsDeployed;
    private int totalGoldEarned;
    private int totalCardCombos;
    private int currentWinStreak;
    private int maxWinStreak;
    private int challengesCompleted;
    private int challengesWithThreeStars;
    private int networkMultiplayerWins;
    private int pvpMatchWins;

    // constructor for PlayerStatistics (all initialized to 0)
    public PlayerStatistics() {
        this.totalMatchesPlayed = 0;
        this.totalMatchesWon = 0;
        this.totalCrownTowersDestroyed = 0;
        this.totalKingTowersDestroyed = 0;
        this.totalSpellCardsPlayed = 0;
        this.totalTroopCardsDeployed = 0;
        this.totalBuildingCardsPlayed = 0;
        this.totalElixirSpent = 0;
        this.totalSpellDamageDealt = 0;
        this.totalSwarmTroopsDeployed = 0;
        this.totalGoldEarned = 0;
        this.totalCardCombos = 0;
        this.currentWinStreak = 0;
        this.maxWinStreak = 0;
        this.challengesCompleted = 0;
        this.challengesWithThreeStars = 0;
        this.networkMultiplayerWins = 0;
        this.pvpMatchWins = 0;
    }

    // all getter methods
    
    public int getTotalMatchesPlayed() {
        return totalMatchesPlayed;
    }

    public int getTotalMatchesWon() {
        return totalMatchesWon;
    }

    public int getTotalCrownTowersDestroyed() {
        return totalCrownTowersDestroyed;
    }

    public int getTotalKingTowersDestroyed() {
        return totalKingTowersDestroyed;
    }

    public int getTotalSpellCardsPlayed() {
        return totalSpellCardsPlayed;
    }

    public int getTotalTroopCardsDeployed() {
        return totalTroopCardsDeployed;
    }

    public int getTotalBuildingCardsPlayed() {
        return totalBuildingCardsPlayed;
    }

    public int getTotalElixirSpent() {
        return totalElixirSpent;
    }

    public int getTotalSpellDamageDealt() {
        return totalSpellDamageDealt;
    }

    public int getTotalSwarmTroopsDeployed() {
        return totalSwarmTroopsDeployed;
    }

    public int getTotalGoldEarned() {
        return totalGoldEarned;
    }

    public int getTotalCardCombos() {
        return totalCardCombos;
    }

    public int getCurrentWinStreak() {
        return currentWinStreak;
    }

    public int getMaxWinStreak() {
        return maxWinStreak;
    }

    public int getChallengesCompleted() {
        return challengesCompleted;
    }

    public int getChallengesWithThreeStars() {
        return challengesWithThreeStars;
    }

    public int getNetworkMultiplayerWins() {
        return networkMultiplayerWins;
    }

    public int getPvPMatchWins() {
        return pvpMatchWins;
    }

    // all setter methods
    // used math max to ensure the stats don't become negative
    public void setTotalMatchesPlayed(int totalMatchesPlayed) {
        this.totalMatchesPlayed = Math.max(0, totalMatchesPlayed);
    }

    public void setTotalMatchesWon(int totalMatchesWon) {
        this.totalMatchesWon = Math.max(0, totalMatchesWon);
    }

    public void setTotalCrownTowersDestroyed(int totalCrownTowersDestroyed) {
        this.totalCrownTowersDestroyed = Math.max(0, totalCrownTowersDestroyed);
    }

    public void setTotalKingTowersDestroyed(int totalKingTowersDestroyed) {
        this.totalKingTowersDestroyed = Math.max(0, totalKingTowersDestroyed);
    }

    public void setTotalSpellCardsPlayed(int totalSpellCardsPlayed) {
        this.totalSpellCardsPlayed = Math.max(0, totalSpellCardsPlayed);
    }

    public void setTotalTroopCardsDeployed(int totalTroopCardsDeployed) {
        this.totalTroopCardsDeployed = Math.max(0, totalTroopCardsDeployed);
    }

    public void setTotalBuildingCardsPlayed(int totalBuildingCardsPlayed) {
        this.totalBuildingCardsPlayed = Math.max(0, totalBuildingCardsPlayed);
    }

    public void setTotalElixirSpent(int totalElixirSpent) {
        this.totalElixirSpent = Math.max(0, totalElixirSpent);
    }

    public void setTotalSpellDamageDealt(int totalSpellDamageDealt) {
        this.totalSpellDamageDealt = Math.max(0, totalSpellDamageDealt);
    }

    public void setTotalSwarmTroopsDeployed(int totalSwarmTroopsDeployed) {
        this.totalSwarmTroopsDeployed = Math.max(0, totalSwarmTroopsDeployed);
    }

    public void setTotalGoldEarned(int totalGoldEarned) {
        this.totalGoldEarned = Math.max(0, totalGoldEarned);
    }

    public void setTotalCardCombos(int totalCardCombos) {
        this.totalCardCombos = Math.max(0, totalCardCombos);
    }

    public void setCurrentWinStreak(int currentWinStreak) {
        this.currentWinStreak = Math.max(0, currentWinStreak);
        // Update max win streak if current one is better
        if (this.currentWinStreak > this.maxWinStreak) {
            setMaxWinStreak(currentWinStreak);
        }
    }

    public void setMaxWinStreak(int maxWinStreak) {
        this.maxWinStreak = Math.max(0, maxWinStreak);
    }

    public void setChallengesCompleted(int challengesCompleted) {
        this.challengesCompleted = Math.max(0, challengesCompleted);
    }

    public void setChallengesWithThreeStars(int challengesWithThreeStars) {
        this.challengesWithThreeStars = Math.max(0, challengesWithThreeStars);
    }

    public void setNetworkMultiplayerWins(int networkMultiplayerWins) {
        this.networkMultiplayerWins = Math.max(0, networkMultiplayerWins);
    }

    public void setPvPMatchWins(int pvpMatchWins) {
        this.pvpMatchWins = Math.max(0, pvpMatchWins);
    }

    // Methods to increment tracked statistics
    
    public void incrementTotalMatchesPlayed() {
        this.totalMatchesPlayed++;
    }

    public void incrementTotalMatchesPlayed(int amount) {
        this.totalMatchesPlayed = Math.max(0, this.totalMatchesPlayed + amount);
    }

    public void incrementTotalMatchesWon() {
        this.totalMatchesWon++;
    }

    public void incrementTotalMatchesWon(int amount) {
        this.totalMatchesWon = Math.max(0, this.totalMatchesWon + amount);
    }

    public void incrementTotalCrownTowersDestroyed() {
        this.totalCrownTowersDestroyed++;
    }

    public void incrementTotalCrownTowersDestroyed(int amount) {
        this.totalCrownTowersDestroyed = Math.max(0, this.totalCrownTowersDestroyed + amount);
    }

    public void incrementTotalKingTowersDestroyed() {
        this.totalKingTowersDestroyed++;
    }

    public void incrementTotalKingTowersDestroyed(int amount) {
        this.totalKingTowersDestroyed = Math.max(0, this.totalKingTowersDestroyed + amount);
    }

    public void incrementTotalSpellCardsPlayed() {
        this.totalSpellCardsPlayed++;
    }

    public void incrementTotalSpellCardsPlayed(int amount) {
        this.totalSpellCardsPlayed = Math.max(0, this.totalSpellCardsPlayed + amount);
    }

    public void incrementTotalTroopCardsDeployed() {
        this.totalTroopCardsDeployed++;
    }

    public void incrementTotalTroopCardsDeployed(int amount) {
        this.totalTroopCardsDeployed = Math.max(0, this.totalTroopCardsDeployed + amount);
    }

    public void incrementTotalBuildingCardsPlayed() {
        this.totalBuildingCardsPlayed++;
    }

    public void incrementTotalBuildingCardsPlayed(int amount) {
        this.totalBuildingCardsPlayed = Math.max(0, this.totalBuildingCardsPlayed + amount);
    }

    public void incrementTotalElixirSpent(int amount) {
        this.totalElixirSpent = Math.max(0, this.totalElixirSpent + amount);
    }

    public void incrementTotalSpellDamageDealt(int amount) {
        this.totalSpellDamageDealt = Math.max(0, this.totalSpellDamageDealt + amount);
    }

    public void incrementTotalSwarmTroopsDeployed() {
        this.totalSwarmTroopsDeployed++;
    }

    public void incrementTotalSwarmTroopsDeployed(int amount) {
        this.totalSwarmTroopsDeployed = Math.max(0, this.totalSwarmTroopsDeployed + amount);
    }

    public void incrementTotalGoldEarned(int amount) {
        this.totalGoldEarned = Math.max(0, this.totalGoldEarned + amount);
    }

    public void incrementTotalCardCombos() {
        this.totalCardCombos++;
    }

    public void incrementTotalCardCombos(int amount) {
        this.totalCardCombos = Math.max(0, this.totalCardCombos + amount);
    }

    public void incrementCurrentWinStreak() {
        this.currentWinStreak++;
        if (this.currentWinStreak > this.maxWinStreak) {
            this.maxWinStreak = this.currentWinStreak;
        }
    }

    public void resetCurrentWinStreak() {
        this.currentWinStreak = 0;
    }

    public void incrementChallengesCompleted() {
        this.challengesCompleted++;
    }

    public void incrementChallengesCompleted(int amount) {
        this.challengesCompleted = Math.max(0, this.challengesCompleted + amount);
    }

    public void incrementChallengesWithThreeStars() {
        this.challengesWithThreeStars++;
    }

    public void incrementChallengesWithThreeStars(int amount) {
        this.challengesWithThreeStars = Math.max(0, this.challengesWithThreeStars + amount);
    }

    public void incrementNetworkMultiplayerWins() {
        this.networkMultiplayerWins++;
    }

    public void incrementNetworkMultiplayerWins(int amount) {
        this.networkMultiplayerWins = Math.max(0, this.networkMultiplayerWins + amount);
    }

    public void incrementPvPMatchWins() {
        this.pvpMatchWins++;
    }

    public void incrementPvPMatchWins(int amount) {
        this.pvpMatchWins = Math.max(0, this.pvpMatchWins + amount);
    }

    // Methods to record match wins
    
    /**
     * Records a match result and updates statistics based on game mode.
     * @param won true if the player won, false if lost/draw
     * @param gameMode the game mode that was played
     */
    public void recordMatchResult(boolean won, GameMode gameMode) {
        incrementTotalMatchesPlayed();
        
        if (won) {
            incrementTotalMatchesWon();
            incrementCurrentWinStreak();
            
            // Update game counters based on game mode
            switch (gameMode) {
                case SINGLE_PLAYER_AI:
                    //no additional tracking needed
                    break;
                case LOCAL_PVP:
                    incrementPvPMatchWins();
                    break;
                case NETWORK_MULTIPLAYER:
                    incrementNetworkMultiplayerWins();
                    break;
            }
        } 
        else {resetCurrentWinStreak();}
    }
}