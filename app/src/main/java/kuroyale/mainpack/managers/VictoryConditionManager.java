package kuroyale.mainpack.managers;

import kuroyale.arenapack.ArenaMap;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.TowerEntity;
import kuroyale.mainpack.PointsCounter;
import kuroyale.mainpack.models.Achievement;
import kuroyale.mainpack.models.Challenge;
import kuroyale.mainpack.models.GameMode;
import kuroyale.mainpack.models.Quest;

/**
 * Handles victory conditions, tie-breaker logic, and game end determination.
 * High cohesion: All victory/end game logic in one place.
 */
public class VictoryConditionManager {
    private final ArenaMap arenaMap;
    private final PointsCounter pointsCounter;
    private final int rows;
    private final int cols;
    private final SceneNavigationManager sceneNavigationManager;
    private EconomyManager economyManager;
    private GameMode gameMode = GameMode.SINGLE_PLAYER_AI;
    private QuestManager questManager;
    private AchievementManager achievementManager;
    private PersistenceManager persistenceManager;
    private NotificationManager notificationManager;
    private ChallengeManager challengeManager;
    private GameStateManager gameStateManager;
    private long matchStartTime;
    private double playerKingTowerInitialHP;

    public VictoryConditionManager(ArenaMap arenaMap, PointsCounter pointsCounter, int rows, int cols,
            SceneNavigationManager sceneNavigationManager) {
        this.arenaMap = arenaMap;
        this.pointsCounter = pointsCounter;
        this.rows = rows;
        this.cols = cols;
        this.sceneNavigationManager = sceneNavigationManager;
    }

    public void setEconomyManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public void setQuestManager(QuestManager questManager) {
        this.questManager = questManager;
    }
    
    public void setAchievementManager(AchievementManager achievementManager) {
        this.achievementManager = achievementManager;
    }
    
    public void setPersistenceManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    public void setNotificationManager(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    public void setChallengeManager(ChallengeManager challengeManager) {
        this.challengeManager = challengeManager;
    }
    
    public void setGameStateManager(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }
    
    /**
     * Initialize match tracking for challenges (time and damage).
     * Call this when the match starts.
     */
    public void initializeMatchTracking() {
        matchStartTime = System.currentTimeMillis();
        
        playerKingTowerInitialHP = 2400.0; 
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                AliveEntity entity = arenaMap.getEntity(r, c);
                if (entity instanceof TowerEntity tower && tower.isKing() && tower.isPlayer()) {
                    playerKingTowerInitialHP = tower.getHP();
                    break;
                }
            }
        }
    }

    public void tieBreaker(javafx.animation.Timeline gameLoop, javafx.scene.control.Label gameTimerLabel) {
        if (pointsCounter.getEnemyPoints() > pointsCounter.getOurPoints()) {
            endGame(false, false, gameLoop);
            return;
        } else if (pointsCounter.getEnemyPoints() < pointsCounter.getOurPoints()) {
            endGame(true, false, gameLoop);
            return;
        }
        double minPlayer = Double.MAX_VALUE;
        double minEnemy = Double.MAX_VALUE;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                AliveEntity entity = arenaMap.getEntity(r, c);
                if (entity instanceof TowerEntity && entity.getHP() > 0) {
                    if (entity.isPlayer()) {
                        if (entity.getHP() < minPlayer) {
                            minPlayer = entity.getHP();
                        }
                    } else {
                        if (entity.getHP() < minEnemy) {
                            minEnemy = entity.getHP();
                        }
                    }
                }
            }
        }
        if (minPlayer > minEnemy) {
            endGame(true, false, gameLoop);
            return;
        } else if (minPlayer < minEnemy) {
            endGame(false, false, gameLoop);
            return;
        }
        // Stop the game loop
        if (gameLoop != null) {
            gameLoop.stop();
        }

        // Draw case
        if (gameTimerLabel != null) {
            gameTimerLabel.setText("00:00");
        }
        endGame(false, true, gameLoop); // isDraw = true, playerWon value is ignored in draw case
    }

    public void endGame(boolean playerWon, boolean isDraw, javafx.animation.Timeline gameLoop) {
        // Award gold based on match result (only in single-player mode, not PvP)
        if (economyManager != null && gameMode == GameMode.SINGLE_PLAYER_AI) {
            if (isDraw) {
                economyManager.awardGold("DRAW");
            } else if (playerWon) {
                economyManager.awardGold("VICTORY");
                // Award a chest on victory!
                awardChest();
            } else {
                economyManager.awardGold("DEFEAT");
            }
        }

        // Update quest and achievement system
        if (questManager != null || achievementManager != null) {
            // Load player profile to get statistics
            if (persistenceManager != null) {
                kuroyale.mainpack.models.PlayerProfile profile = persistenceManager.loadPlayerProfile();
                kuroyale.mainpack.models.PlayerStatistics stats = profile.getStatistics();
            
                // Record match result in statistics
                if (stats != null) {
                    stats.recordMatchResult(playerWon, gameMode);
                }
            
                //Update quest manager
                if (questManager != null && stats != null) {
                    // Track which quests were completed before update
                    java.util.Set<Integer> questIdsBefore = new java.util.HashSet<>();
                    for (Quest q : questManager.getDailyQuests()) {
                        if (q.getCompleted()) {
                            questIdsBefore.add(q.getQuestType().getId());
                        }
                    }
                    
                    questManager.onMatchEnded(playerWon, gameMode, stats);
                    questManager.updateFromStatistics(stats);
                    
                    // Find newly completed quests
                    java.util.List<Quest> newlyCompletedQuests = new java.util.ArrayList<>();
                    for (Quest quest : questManager.getDailyQuests()) {
                        if (quest.getCompleted() && !questIdsBefore.contains(quest.getQuestType().getId())) {
                            newlyCompletedQuests.add(quest);
                        }
                    }
                    
                    // Show notifications immediately
                    if (notificationManager != null && !newlyCompletedQuests.isEmpty()) {
                        for (Quest quest : newlyCompletedQuests) {
                            notificationManager.showNotification(
                                "Quest Complete!",
                                "+" + quest.getQuestType().getGoldReward() + " Gold"
                            );
                        }
                    }
                }
                
                // Update achievement manager
                if (achievementManager != null && stats != null) {
                    // Track which achievements were completed before update
                    java.util.Set<Integer> achievementIdsBefore = new java.util.HashSet<>();
                    for (Achievement a : achievementManager.getAchievements()) {
                        if (a.getCompleted()) {
                            achievementIdsBefore.add(a.getAchievementType().getId());
                        }
                    }
                    
                    achievementManager.onMatchEnded(playerWon, gameMode, stats);
                    achievementManager.updateFromStatistics(stats);
                    
                    // Find newly completed achievements
                    java.util.List<Achievement> newlyCompletedAchievements = new java.util.ArrayList<>();
                    for (Achievement achievement : achievementManager.getAchievements()) {
                        if (achievement.getCompleted() && !achievementIdsBefore.contains(achievement.getAchievementType().getId())) {
                            newlyCompletedAchievements.add(achievement);
                        }
                    }
                    
                    // Show notifications immediately
                    if (notificationManager != null && !newlyCompletedAchievements.isEmpty()) {
                        for (Achievement achievement : newlyCompletedAchievements) {
                            notificationManager.showNotification(
                                "Achievement Unlocked!",
                                "+" + achievement.getAchievementType().getGoldReward() + " Gold"
                            );
                        }
                    }

                    if (challengeManager != null && challengeManager.getCurrentChallenge() != null) {
                        Challenge currentChallenge = challengeManager.getCurrentChallenge();
                        
                        if (playerWon) {
                            // Calculate actual match time
                            long matchEndTime = System.currentTimeMillis();
                            int timeSeconds = (int) ((matchEndTime - matchStartTime) / 1000);
                            
                            // Check if player king tower took damage
                            boolean tookDamage = false;
                            for (int r = 0; r < rows; r++) {
                                for (int c = 0; c < cols; c++) {
                                    AliveEntity entity = arenaMap.getEntity(r, c);
                                    if (entity instanceof TowerEntity tower && tower.isKing() && tower.isPlayer()) {
                                        if (tower.getHP() < playerKingTowerInitialHP) {
                                            tookDamage = true;
                                        }
                                        break;
                                    }
                                }
                            }
                            
                            // Calculate stars using Template Method Pattern
                            int stars = challengeManager.calculateStars(playerWon, timeSeconds, tookDamage);
                            
                            // Complete challenge
                            currentChallenge.setCompleted(true);
                            if (stars > currentChallenge.getStarsEarned()) {
                                currentChallenge.setStarsEarned(stars);
                            }
                            currentChallenge.incrementCompletion();
                            
                            // Award gold
                            int goldReward = currentChallenge.getType().getGoldReward();
                            if (persistenceManager != null && stats != null) {
                                profile.setTotalGold(profile.getTotalGold() + goldReward);
                                stats.incrementTotalGoldEarned(goldReward);
                                profile.setStatistics(stats);
                                profile.addGoldTransaction(goldReward, "Challenge: " + currentChallenge.getType().getName());
                                
                                // Update challenge progress in profile
                                profile.setChallenges(challengeManager.getChallenges());
                            }
                            
                            // Notify quest manager about challenge completion
                            if (questManager != null) {
                                questManager.onChallengeCompleted(stars);
                            }
                        }
                        
                        challengeManager.clearCurrentChallenge();
                    }
                }
            
                // Save updated profile
                profile.setDailyQuests(questManager != null ? questManager.getDailyQuests() : profile.getDailyQuests());
                profile.setLastQuestResetTimestamp(questManager != null ? questManager.getLastResetTimestamp() : profile.getLastQuestResetTimestamp());
                profile.setAchievements(achievementManager != null ? achievementManager.getAchievements() : profile.getAchievements());
                profile.setStatistics(stats);
                persistenceManager.savePlayerProfile(profile);
            }
        }

        sceneNavigationManager.showGameEndScreen(playerWon, isDraw, gameLoop, gameMode);
    }

    private void awardChest() {
        // Load profile, add chest, save
        PersistenceManager pm = new PersistenceManager();
        kuroyale.mainpack.models.PlayerProfile profile = pm.loadPlayerProfile();
        profile.addChest();
        pm.savePlayerProfile(profile);
        System.out.println("Chest awarded! Total chests: " + profile.getChestCount());
    }
}
