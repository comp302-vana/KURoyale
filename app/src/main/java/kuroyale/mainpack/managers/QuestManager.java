package kuroyale.mainpack.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import kuroyale.mainpack.models.Quest;
import kuroyale.mainpack.models.PlayerStatistics;
import kuroyale.mainpack.models.GameMode;
import kuroyale.mainpack.models.MatchState;

/**
 * Manages daily quests: generation, progress tracking, and completion.
 * Uses MatchState to track per-match statistics separately from Quest objects.
 */
public class QuestManager {
    private List<Quest> dailyQuests = new ArrayList<>();
    private long lastResetTimestamp;
    private static final long RESET_INTERVAL_MILISECONDS = 24 * 60 * 60 * 1000; // 24 hours
    
    // Per-match state tracking (separate from Quest objects in models MatchState.java)
    private MatchState currentMatchState = new MatchState();
    
    /**
     * Initializes daily quests. Should be called on game startup.
     * Generates new quests if 24 hours have passed since last reset.
     */
    public void initializeDailyQuests() {
        long currentTime = System.currentTimeMillis();
        
        // First time initialization or reset needed
        if (lastResetTimestamp == 0 || (currentTime - lastResetTimestamp >= RESET_INTERVAL_MILISECONDS)) {
            generateNewDailyQuests();
            lastResetTimestamp = currentTime;
        }
    }
    
    /**
     * Generates 3 random daily quests from the available quest types.
     */
    private void generateNewDailyQuests() {
        dailyQuests.clear();
        
        Quest.QuestType[] questTypes = Quest.QuestType.values();
        List<Quest.QuestType> typeList = new ArrayList<>();
        for (Quest.QuestType type : questTypes) {
            typeList.add(type);
        }
        
        Collections.shuffle(typeList, new Random());
        
        int count = Math.min(3, typeList.size());
        for (int i = 0; i < count; i++) {
            Quest.QuestType selectedType = typeList.get(i);
            Quest newQuest = new Quest(selectedType, 0, false, false);
            dailyQuests.add(newQuest);
        }
    }
    
    /**
     * Getter for the list of current daily quests.
     */
    public List<Quest> getDailyQuests() {
        return new ArrayList<>(dailyQuests);
    }
    
    /**
     * Getter for the timestamp of the last quest reset.
     */
    public long getLastResetTimestamp() {
        return lastResetTimestamp;
    }
    
    /**
     * Setter for the last reset timestamp.
     */
    public void setLastResetTimestamp(long timestamp) {
        this.lastResetTimestamp = timestamp;
    }
    
    /**
     * Setter for the daily quests.
     */
    public void setDailyQuests(List<Quest> quests) {
        this.dailyQuests = new ArrayList<>(quests);
    }
    
    /**
     * Resets per every match tracked.
     */
    public void startNewMatch() {
        currentMatchState.reset();
    }
    
    /**
     * Getter for the current match state.
     */
    public MatchState getCurrentMatchState() {
        return currentMatchState;
        //matchstate gets reinitialized for every match
    }
    
    /**
     * Called when a match ends. Updates quest progress based on match result.
     * Uses MatchState for tracking.
     * "won": true if player won, false if lost/draw
     * "gameMode": the game mode that was played
     * "stats": player statistics for lifetime tracking
     */
    public void onMatchEnded(boolean won, GameMode gameMode, PlayerStatistics stats) {
        for (Quest quest : dailyQuests) {
            if (quest.getClaimed() || quest.getCompleted()) {
                continue; // skip already completed/claimed quests
            }
            
            Quest.QuestType type = quest.getQuestType();
            
            switch (type) {
                case WIN_3_MATCHES:
                    if (won) {
                        updateQuestProgress(quest, quest.getCurrentProgress() + 1);
                    }
                    break;
                    
                case WIN_WITHOUT_LOSING_TOWER:
                    if (won && !currentMatchState.hasLostCrownTower()) {
                        updateQuestProgress(quest, quest.getCurrentProgress() + 1);
                    }
                    break;
                    
                case WIN_WITH_COMMON_CARDS_ONLY:
                    if (won && currentMatchState.hasUsedOnlyCommonCards()) {
                        updateQuestProgress(quest, quest.getCurrentProgress() + 1);
                    }
                    break;
                    
                case WIN_2_MATCHES_IN_ROW:
                    if (won && stats.getCurrentWinStreak() >= 2) {
                        updateQuestProgress(quest, Math.min(quest.getCurrentProgress() + 1, type.getTargetValue()));
                    }
                    break;
                    
                case WIN_NETWORK_MULTIPLAYER:
                    if (won && gameMode == GameMode.NETWORK_MULTIPLAYER) {
                        updateQuestProgress(quest, quest.getCurrentProgress() + 1);
                    }
                    break;
                    
                case WIN_PVP_MATCH:
                    if (won && gameMode == GameMode.LOCAL_PVP) {
                        updateQuestProgress(quest, quest.getCurrentProgress() + 1);
                    }
                    break;
                    
                case PLAY_20_CARDS_SINGLE_MATCH:
                    updateQuestProgress(quest, currentMatchState.getCardsPlayed());
                    break;

                default:
                    break;
            }
        }
    }
    
    /**
     * Called when a card is played. Updates quest progress for card-related quests.
     * Updates MatchState for tracking.
     * "cardId": the ID of the card played
     * "isSpell": true if it's a spell card
     * "isBuilding": true if it's a building card
     * "isTroop": true if it's a troop card
     * "cardCost": the elixir cost of the card
     * "isCommonCard": true if the card's rarity is common
     */
    public void onCardPlayed(int cardId, boolean isSpell, boolean isBuilding, boolean isTroop, 
                             int cardCost, boolean isCommonCard) {
        // Update match state
        currentMatchState.incrementCardsPlayed();
        currentMatchState.addElixirSpent(cardCost);
        
        if (!isCommonCard) {
            currentMatchState.setUsedOnlyCommonCards(false);
        }
        
        // Update quest progress
        for (Quest quest : dailyQuests) {
            if (quest.getClaimed() || quest.getCompleted()) {
                continue;
            }
            
            Quest.QuestType type = quest.getQuestType();
            
            switch (type) {
                case PLAY_10_SPELL_CARDS:
                    if (isSpell) {
                        updateQuestProgress(quest, quest.getCurrentProgress() + 1);
                    }
                    break;
                    
                case DEPLOY_15_TROOP_CARDS:
                    if (isTroop) {
                        updateQuestProgress(quest, quest.getCurrentProgress() + 1);
                    }
                    break;
                    
                case PLAY_5_BUILDING_CARDS:
                    if (isBuilding) {
                        updateQuestProgress(quest, quest.getCurrentProgress() + 1);
                    }
                    break;
                //to avoid missing label problems
                default:
                    break;
            }
        }
    }
    
    /**
     * Called when a tower is destroyed. Updates quest progress for tower-related quests.
     * Updates MatchState for tracking.
     */
    public void onTowerDestroyed(boolean isCrownTower, boolean isPlayerTower) {
        // Update match state
        if (isPlayerTower && isCrownTower) {
            currentMatchState.setLostCrownTower(true);
        }
        
        // Update quest progress
        for (Quest quest : dailyQuests) {
            if (quest.getClaimed() || quest.getCompleted()) {
                continue;
            }
            
            Quest.QuestType type = quest.getQuestType();
            
            if (!isPlayerTower) { //if the tower destroyed belongs to enemy
                switch (type) {
                    case DESTROY_5_CROWN_TOWERS:
                        if (isCrownTower) {
                            updateQuestProgress(quest, quest.getCurrentProgress() + 1);
                        }
                        break;
                        
                    case DESTROY_KING_TOWER:
                        if (!isCrownTower) { // if it's a king tower
                            updateQuestProgress(quest, quest.getCurrentProgress() + 1);
                        }
                        break;

                    default:
                        break;
                }
            }
        }
    }
    
    /**
     * Called when spell damage is dealt. Updates quest progress for spell damage quests.
     * "damage": the amount of damage dealt
     */
    public void onSpellDamageDealt(int damage) {
        for (Quest quest : dailyQuests) {
            if (quest.getClaimed() || quest.getCompleted()) {
                continue;
            }
            
            if (quest.getQuestType() == Quest.QuestType.DEAL_3000_SPELL_DAMAGE) {
                updateQuestProgress(quest, quest.getCurrentProgress() + damage);
            }
        }
    }
    
    /**
     * Updates quest progress from lifetime statistics.
     * Call this periodically or after matches to sync with PlayerStatistics.
     * parameter "stats": the player's lifetime statistics
     */
    public void updateFromStatistics(PlayerStatistics stats) {
        for (Quest quest : dailyQuests) {
            if (quest.getClaimed() || quest.getCompleted()) {
                continue;
            }
            
            Quest.QuestType type = quest.getQuestType();
            
            switch (type) {
                case SPEND_100_ELIXIR:
                    int elixirSpent = stats.getTotalElixirSpent();
                    updateQuestProgress(quest, elixirSpent);
                    break;
                default:
                    break;
            }
        }
    }
    
    /**
     * Called when a challenge is completed.
     * "stars": the number of stars earned (0-3)
     */
    public void onChallengeCompleted(int stars) {
        for (Quest quest : dailyQuests) {
            if (quest.getClaimed() || quest.getCompleted()) {
                continue;
            }
            
            if (quest.getQuestType() == Quest.QuestType.COMPLETE_2_CHALLENGES) {
                updateQuestProgress(quest, quest.getCurrentProgress() + 1);
            }
        }
    }
    
    /**
     * Updates a quest's progress and checks for completion.
     * "quest": the quest to update
     * "newProgress": the new progress value
     */
    private void updateQuestProgress(Quest quest, int newProgress) {
        quest.setCurrentProgress(newProgress);
        
        // Check if quest is completed
        if (newProgress >= quest.getQuestType().getTargetValue() && !quest.getCompleted()) {
            quest.setCompleted(true);
            System.out.println("Quest completed: " + quest.getQuestType().getDescription() + 
                             " (+" + quest.getQuestType().getGoldReward() + " gold)");
        }
    }
    
    /**
     * Claims a quest reward. Returns the gold reward amount.
     * "quest": the quest to claim
     * returns the gold reward amount, or 0 if a quest can't be claimed
     */
    public int claimQuestReward(Quest quest) {
        if (!quest.getCompleted() || quest.getClaimed()) {
            return 0; // Cannot claim incomplete or already claimed quest
        }
        
        quest.setClaimed(true);
        return quest.getQuestType().getGoldReward();
    }
    
    /**
     * Gets the time remaining until the next quest reset in milliseconds.
     * returns milliseconds until reset, or 0 if reset is needed
     */
    public long getTimeUntilReset() {
        if (lastResetTimestamp == 0) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceReset = currentTime - lastResetTimestamp;
        long timeUntilReset = RESET_INTERVAL_MILISECONDS - timeSinceReset;
        
        return Math.max(0, timeUntilReset);
    }
    
    /**
    * Checks if any quests are completed but not yet claimed.
    * returns true if there are unclaimed completed quests
    */
    public boolean hasUnclaimedQuests() {
        for (int i = 0; i < dailyQuests.size(); i++) {
            Quest quest = dailyQuests.get(i);
            if (quest.getCompleted() && !quest.getClaimed()) {
                return true;
            }
        }
        return false;
    }
}