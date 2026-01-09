package kuroyale.mainpack.managers;

import java.util.ArrayList;
import java.util.List;
import kuroyale.mainpack.models.Achievement;
import kuroyale.mainpack.models.PlayerStatistics;
import kuroyale.mainpack.models.GameMode;
import kuroyale.cardpack.CardRarity;
import kuroyale.cardpack.CardRarityMapper;

/**
 * Manages permanent achievements: initializes it, tracks progress and completion.
 */
public class AchievementManager {
    private List<Achievement> achievements = new ArrayList<>();
    
    /**
     * Constructor for achievements. Unlike Quests, achievements are permanent
     */
    public AchievementManager() {
        initializeAchievements();
    }
    
    /**
     * Initializes all achievement types with default values.
     */
    private void initializeAchievements() {
        for (Achievement.AchievementType type : Achievement.AchievementType.values()) {
            achievements.add(new Achievement(type, 0, false, false));
        }
    }
    
    /**
     * getter for the list of all achievements.
     * returns a copy of the achievements list
     */
    public List<Achievement> getAchievements() {
        return new ArrayList<>(achievements);
    }
    
    /**
     * setter the achievements list
     * "achievements": the achievements to set
     */
    public void setAchievements(List<Achievement> achievements) {
        this.achievements = new ArrayList<>(achievements);
    }
    
    /**
     * Updates all achievements based on player statistics.
     * Call this periodically or after significant events.
     * "stats": the player's lifetime statistics
     */
    public void updateFromStatistics(PlayerStatistics stats) {
        for (Achievement achievement : achievements) {
            if (achievement.getClaimed() || achievement.getCompleted()) {
                continue; // Skip achievements that are already claimed/completed
            }
            
            Achievement.AchievementType type = achievement.getAchievementType();
            
            switch (type) {
                case FIRST_BLOOD:
                    if (stats.getTotalMatchesWon() >= 1) {
                        updateAchievementProgress(achievement, 1);
                    }
                    break;
                    
                case TOWER_HUNTER:
                    updateAchievementProgress(achievement, stats.getTotalCrownTowersDestroyed() / 9);
                    break;
                    
                case CHALLENGE_MASTER:
                    updateAchievementProgress(achievement, stats.getChallengesCompleted());
                    break;
                    
                case THREE_STAR_HERO:
                    if (stats.getChallengesWithThreeStars() >= 1) {
                        updateAchievementProgress(achievement, 1);
                    }
                    break;
                    
                case NETWORK_WARRIOR:
                    updateAchievementProgress(achievement, stats.getNetworkMultiplayerWins());
                    break;
                    
                case ARMY_BUILDER:
                    updateAchievementProgress(achievement, stats.getTotalSwarmTroopsDeployed());
                    break;
                    
                case SPELL_MASTER:
                    updateAchievementProgress(achievement, stats.getTotalSpellDamageDealt());
                    break;
                    
                case GOLD_HOARDER:
                    updateAchievementProgress(achievement, stats.getTotalGoldEarned());
                    break;
                    
                case VETERAN_PLAYER:
                    updateAchievementProgress(achievement, stats.getTotalMatchesPlayed());
                    break;
                    
                case COMBO_EXPERT:
                    updateAchievementProgress(achievement, stats.getTotalCardCombos());
                    break;
                    
                case UNDEFEATED:
                    // Use CURRENT win streak, not max win streak
                    int currentStreak = stats.getCurrentWinStreak();
                    int progress = Math.min(currentStreak, 5);
                    updateAchievementProgress(achievement, progress);
                    break;
                    
                default:
                    break;
            }
        }
    }
    
    /**
     * Updates achievements depending on the match results.
     * "won": true if player won, false if lost/draw
     * "gameMode": the game mode that was played
     * "stats": player statistics for lifetime tracking
     */
    public void onMatchEnded(boolean won, GameMode gameMode, PlayerStatistics stats) {
        for (Achievement achievement : achievements) {
            if (achievement.getClaimed() || achievement.getCompleted()) {
                continue;
            }
            //gets achievements and updates them according to the result
            Achievement.AchievementType type = achievement.getAchievementType();
            
            switch (type) {
                case FIRST_BLOOD:
                    if (won && stats.getTotalMatchesWon() == 1) {
                        updateAchievementProgress(achievement, 1);
                    }
                    break;
                    
                case NETWORK_WARRIOR:
                    if (won && gameMode == GameMode.NETWORK_MULTIPLAYER) {
                        updateAchievementProgress(achievement, stats.getNetworkMultiplayerWins());
                    }
                    break;
                    
                case UNDEFEATED:
                    if (won) {
                        // Always update progress to current streak (capped at 5) when won
                        int currentStreak = stats.getCurrentWinStreak();
                        int progress = Math.min(currentStreak, 5);
                        updateAchievementProgress(achievement, progress);
                    }
                    break;
                    
                default:
                    break;
            }
        }
        
        // Update all achievements from statistics after match
        updateFromStatistics(stats);
    }
    
    /**
     * Called when a card is upgraded. Checks for LEGENDARY_COLLECTOR achievement.
     * "cardId": the ID of the card that was upgraded
     * "newLevel": the new level of the card
     */
    public void onCardUpgraded(int cardId, int newLevel) {
        // Check if a legendary card reached level 3
        CardRarity rarity = CardRarityMapper.getRarity(cardId);
        if (rarity == CardRarity.LEGENDARY && newLevel == 3) {
            for (Achievement achievement : achievements) {
                if (achievement.getClaimed() || achievement.getCompleted()) {
                    continue;
                }
                
                if (achievement.getAchievementType() == Achievement.AchievementType.LEGENDARY_COLLECTOR) {
                    updateAchievementProgress(achievement, 1);
                    break;
                }
            }
        }
    }
    
    /**
     * Gets a list of achievements that were just completed now.
     */
    public java.util.List<Achievement> getNewlyCompletedAchievements() {
        java.util.List<Achievement> newlyCompleted = new java.util.ArrayList<>();
        for (Achievement achievement : achievements) {
            if (achievement.getCompleted() && !achievement.getClaimed()) {
                newlyCompleted.add(achievement);
            }
        }
        return newlyCompleted;
    }

    /**
     * Called when a challenge is completed. Updates challenge-related achievements.
     * "stars": the number of stars earned (0-3)
     * "stats": player statistics for tracking
     */
    public void onChallengeCompleted(int stars, PlayerStatistics stats) {
        for (Achievement achievement : achievements) {
            if (achievement.getClaimed() || achievement.getCompleted()) {
                continue;
            }
            
            Achievement.AchievementType type = achievement.getAchievementType();
            
            switch (type) {
                case CHALLENGE_MASTER:
                    updateAchievementProgress(achievement, stats.getChallengesCompleted());
                    break;
                    
                case THREE_STAR_HERO:
                    if (stars == 3) {
                        updateAchievementProgress(achievement, 1);
                    }
                    break;
                    
                default:
                    break;
            }
        }
    }
    
    /**
     * Called when a card combo is triggered. Updates COMBO_EXPERT achievement.
     * "stats": player statistics for tracking
     */
    public void onCardComboTriggered(PlayerStatistics stats) {
        for (Achievement achievement : achievements) {
            if (achievement.getClaimed() || achievement.getCompleted()) {
                continue;
            }
            
            if (achievement.getAchievementType() == Achievement.AchievementType.COMBO_EXPERT) {
                updateAchievementProgress(achievement, stats.getTotalCardCombos());
            }
        }
    }
    
    /**
     * Called when gold is earned. Updates GOLD_HOARDER achievement.
     * "amount": the amount of gold earned
     * "stats": player statistics for tracking
     */
    public void onGoldEarned(int amount, PlayerStatistics stats) {
        for (Achievement achievement : achievements) {
            if (achievement.getClaimed() || achievement.getCompleted()) {
                continue;
            }
            
            if (achievement.getAchievementType() == Achievement.AchievementType.GOLD_HOARDER) {
                updateAchievementProgress(achievement, stats.getTotalGoldEarned());
            }
        }
    }
    
    /**
     * Updates an achievement's progress and checks for completion.
     * "achievement": the achievement to update
     * "newProgress": the new progress value
     */
    private boolean updateAchievementProgress(Achievement achievement, int newProgress) {
        achievement.setCurrentProgress(newProgress);
        
        // Check if achievement is completed, prompt a message if so
        if (newProgress >= achievement.getAchievementType().getTargetValue() && !achievement.getCompleted()) {
            achievement.setCompleted(true);
            System.out.println("Achievement unlocked: " + achievement.getAchievementType().getName() + 
                             " - " + achievement.getAchievementType().getDescription() +
                             " (+" + achievement.getAchievementType().getGoldReward() + " gold)");
            return true;
        }
        return false;
    }
    
    /**
     * Claims an achievement reward. Returns the gold reward amount.
     * "achievement": the achievement to claim
     * returns the gold reward amount, or 0 if it cannot be claimed
     */
    public int claimAchievementReward(Achievement achievement) {
        if (!achievement.getCompleted() || achievement.getClaimed()) {
            return 0; // Cannot claim incomplete or already claimed achievement
        }
        
        achievement.setClaimed(true);
        return achievement.getAchievementType().getGoldReward();
    }
    
    /**
     * Checks if any achievements are completed but not yet claimed.
     * returns true if there are unclaimed completed achievements
     */
    public boolean hasUnclaimedAchievements() {
        for (Achievement achievement : achievements) {
            if (achievement.getCompleted() && !achievement.getClaimed()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets an achievement by its type.
     * "type": the achievement type to find
     * returns the achievement, or null if not found
     */
    public Achievement getAchievementByType(Achievement.AchievementType type) {
        for (Achievement achievement : achievements) {
            if (achievement.getAchievementType() == type) {
                return achievement;
            }
        }
        return null;
    }
}