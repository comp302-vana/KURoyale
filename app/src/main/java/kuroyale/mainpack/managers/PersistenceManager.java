package kuroyale.mainpack.managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kuroyale.mainpack.models.GoldTransaction;
import kuroyale.mainpack.models.PlayerProfile;
import kuroyale.mainpack.models.Quest;
import kuroyale.mainpack.models.Achievement;
import kuroyale.mainpack.models.Challenge;
import kuroyale.mainpack.models.PlayerStatistics;

/**
 * Handles persistence of player profile data (gold, card levels, gold history).
 * Uses CSV format following the ArenaMap save/load pattern.
 * High cohesion: All persistence logic in one place.
 */
public class PersistenceManager {
    private static final String PROFILE_FILE = "saves/player_profile.csv";
    private static final String GOLD_SECTION = "GOLD";
    private static final String LEVELS_SECTION = "LEVELS";
    private static final String HISTORY_SECTION = "HISTORY";
    private static final String UNLOCKED_SECTION = "UNLOCKED";
    private static final String CHESTS_SECTION = "CHESTS";
    private static final String QUESTS_SECTION = "QUESTS";
    private static final String ACHIEVEMENTS_SECTION = "ACHIEVEMENTS";
    private static final String STATISTICS_SECTION = "STATISTICS";
    private static final String QUEST_RESET_SECTION = "QUEST_RESET";
    private static final String CHALLENGES_SECTION = "CHALLENGES";

    /**
     * Saves the player profile to file.
     */
    public void savePlayerProfile(PlayerProfile profile) {
        try {
            File savesDir = new File("saves");
            if (!savesDir.exists()) {
                savesDir.mkdirs();
            }

            try (PrintWriter pw = new PrintWriter(new FileWriter(PROFILE_FILE))) {
                // Gold section
                pw.println(GOLD_SECTION);
                pw.println(profile.getTotalGold());
                pw.println();

                // Card levels section
                pw.println(LEVELS_SECTION);
                Map<Integer, Integer> levels = profile.getCardLevels();
                for (int cardId = 1; cardId <= 28; cardId++) {
                    int level = profile.getCardLevel(cardId);
                    pw.println(cardId + "," + level);
                }
                pw.println();

                // Gold history section
                pw.println(HISTORY_SECTION);
                List<GoldTransaction> history = profile.getGoldHistory();
                for (GoldTransaction transaction : history) {
                    pw.println(transaction.getTimestamp() + "," +
                            transaction.getAmount() + "," +
                            transaction.getReason());
                }
                pw.println();

                // Unlocked cards section
                pw.println(UNLOCKED_SECTION);
                java.util.Set<Integer> unlockedCards = profile.getUnlockedCards();
                for (Integer cardId : unlockedCards) {
                    pw.println(cardId);
                }
                pw.println();

                // Chests section
                pw.println(CHESTS_SECTION);
                pw.println(profile.getChestCount());
                pw.println();

                //Quests section
                pw.println(QUESTS_SECTION);
                List<Quest> quests = profile.getDailyQuests();
                for (Quest quest : quests) {
                    pw.println(quest.getQuestType().getId() + "," +
                            quest.getCurrentProgress() + "," +
                            quest.getCompleted() + "," +
                            quest.getClaimed());
                }
                pw.println();

                // Achievements section
                pw.println(ACHIEVEMENTS_SECTION);
                List<Achievement> achievements = profile.getAchievements();
                for (Achievement achievement : achievements) {
                    pw.println(achievement.getAchievementType().getId() + "," +
                            achievement.getCurrentProgress() + "," +
                            achievement.getCompleted() + "," +
                            achievement.getClaimed());
                }
                pw.println();

                // Statistics section
                pw.println(STATISTICS_SECTION);
                PlayerStatistics stats = profile.getStatistics();
                if (stats != null) {
                    pw.println(stats.getTotalMatchesPlayed() + "," +
                            stats.getTotalMatchesWon() + "," +
                            stats.getTotalCrownTowersDestroyed() + "," +
                            stats.getTotalKingTowersDestroyed() + "," +
                            stats.getTotalSpellCardsPlayed() + "," +
                            stats.getTotalTroopCardsDeployed() + "," +
                            stats.getTotalBuildingCardsPlayed() + "," +
                            stats.getTotalElixirSpent() + "," +
                            stats.getTotalSpellDamageDealt() + "," +
                            stats.getTotalSwarmTroopsDeployed() + "," +
                            stats.getTotalGoldEarned() + "," +
                            stats.getTotalCardCombos() + "," +
                            stats.getCurrentWinStreak() + "," +
                            stats.getMaxWinStreak() + "," +
                            stats.getChallengesCompleted() + "," +
                            stats.getChallengesWithThreeStars() + "," +
                            stats.getNetworkMultiplayerWins() + "," +
                            stats.getPvPMatchWins());
                } else {
                    // Write zeros if statistics is null
                    pw.println("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0");
                }
                pw.println();

                // Quest reset timestamp section
                pw.println(QUEST_RESET_SECTION);
                pw.println(profile.getLastQuestResetTimestamp());

                pw.println();
                pw.println(CHALLENGES_SECTION);
                java.util.List<Challenge> challenges = profile.getChallenges();
                if (challenges != null && !challenges.isEmpty()) {
                    for (Challenge challenge : challenges) {
                        pw.println(challenge.getType().getId() + "," +
                            challenge.isCompleted() + "," +
                            challenge.getStarsEarned() + "," +
                            challenge.getAttempts() + "," +
                            challenge.getNumOfCompletion());
                    }
                }
                pw.println();

            System.out.println("Player profile saved to: " + PROFILE_FILE);
            }
        } catch (Exception e) {
            System.err.println("Failed to save player profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads the player profile from file, or returns default profile if file
     * doesn't exist.
     */
    public PlayerProfile loadPlayerProfile() {
        File profileFile = new File(PROFILE_FILE);
        if (!profileFile.exists()) {
            System.out.println("No existing player profile found, creating default profile");
            return new PlayerProfile();
        }

        try (BufferedReader br = new BufferedReader(new FileReader(PROFILE_FILE))) {
            String line;
            String currentSection = null;

            int gold = 0;
            Map<Integer, Integer> cardLevels = new HashMap<>();
            List<GoldTransaction> goldHistory = new ArrayList<>();
            java.util.Set<Integer> unlockedCards = new java.util.HashSet<>();
            int chestCount = 0;
            List<Quest> dailyQuests = new ArrayList<>();
            List<Achievement> achievements = new ArrayList<>();
            List<Challenge> challenges = new ArrayList<>();
            PlayerStatistics statistics = new PlayerStatistics();
            long lastQuestResetTimestamp = 0;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                // Check for section headers
                if (line.equals(GOLD_SECTION)) {
                    currentSection = GOLD_SECTION;
                    continue;
                } else if (line.equals(LEVELS_SECTION)) {
                    currentSection = LEVELS_SECTION;
                    continue;
                } else if (line.equals(HISTORY_SECTION)) {
                    currentSection = HISTORY_SECTION;
                    continue;
                } else if (line.equals(UNLOCKED_SECTION)) {
                    currentSection = UNLOCKED_SECTION;
                    continue;
                } else if (line.equals(CHESTS_SECTION)) {
                    currentSection = CHESTS_SECTION;
                    continue;
                } else if (line.equals(QUESTS_SECTION)) {
                    currentSection = QUESTS_SECTION;
                    continue;
                } else if (line.equals(ACHIEVEMENTS_SECTION)) {
                    currentSection = ACHIEVEMENTS_SECTION;
                    continue;
                } else if (line.equals(STATISTICS_SECTION)) {
                    currentSection = STATISTICS_SECTION;
                    continue;
                } else if (line.equals(QUEST_RESET_SECTION)) {
                    currentSection = QUEST_RESET_SECTION;
                    continue;
                } else if (line.equals(CHALLENGES_SECTION)) {
                    currentSection = CHALLENGES_SECTION;
                    continue;
                }

                // Parse based on current section
                if (GOLD_SECTION.equals(currentSection)) {
                    gold = Integer.parseInt(line);
                } else if (LEVELS_SECTION.equals(currentSection)) {
                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        int cardId = Integer.parseInt(parts[0].trim());
                        int level = Integer.parseInt(parts[1].trim());
                        cardLevels.put(cardId, level);
                    }
                } else if (HISTORY_SECTION.equals(currentSection)) {
                    String[] parts = line.split(",");
                    if (parts.length == 3) {
                        long timestamp = Long.parseLong(parts[0].trim());
                        int amount = Integer.parseInt(parts[1].trim());
                        String reason = parts[2].trim();
                        goldHistory.add(new GoldTransaction(timestamp, amount, reason));
                    }
                } else if (UNLOCKED_SECTION.equals(currentSection)) {
                    int cardId = Integer.parseInt(line.trim());
                    unlockedCards.add(cardId);
                } else if (CHESTS_SECTION.equals(currentSection)) {
                    chestCount = Integer.parseInt(line.trim());
                } else if (QUESTS_SECTION.equals(currentSection)) {
                    String[] parts = line.split(",");
                    if (parts.length == 4) {
                        int questTypeId = Integer.parseInt(parts[0].trim());
                        int currentProgress = Integer.parseInt(parts[1].trim());
                        boolean completed = Boolean.parseBoolean(parts[2].trim());
                        boolean claimed = Boolean.parseBoolean(parts[3].trim());
                        
                        // Find quest type by ID
                        Quest.QuestType questType = findQuestTypeById(questTypeId);
                        if (questType != null) {
                            dailyQuests.add(new Quest(questType, currentProgress, completed, claimed));
                        }
                    }
                } else if (ACHIEVEMENTS_SECTION.equals(currentSection)) {
                    String[] parts = line.split(",");
                    if (parts.length == 4) {
                        int achievementTypeId = Integer.parseInt(parts[0].trim());
                        int currentProgress = Integer.parseInt(parts[1].trim());
                        boolean completed = Boolean.parseBoolean(parts[2].trim());
                        boolean claimed = Boolean.parseBoolean(parts[3].trim());
                        
                        // Find achievement type by ID
                        Achievement.AchievementType achievementType = findAchievementTypeById(achievementTypeId);
                        if (achievementType != null) {
                            achievements.add(new Achievement(achievementType, currentProgress, completed, claimed));
                        }
                    }
                } else if (STATISTICS_SECTION.equals(currentSection)) {
                    String[] parts = line.split(",");
                    if (parts.length == 18) {
                        statistics.setTotalMatchesPlayed(Integer.parseInt(parts[0].trim()));
                        statistics.setTotalMatchesWon(Integer.parseInt(parts[1].trim()));
                        statistics.setTotalCrownTowersDestroyed(Integer.parseInt(parts[2].trim()));
                        statistics.setTotalKingTowersDestroyed(Integer.parseInt(parts[3].trim()));
                        statistics.setTotalSpellCardsPlayed(Integer.parseInt(parts[4].trim()));
                        statistics.setTotalTroopCardsDeployed(Integer.parseInt(parts[5].trim()));
                        statistics.setTotalBuildingCardsPlayed(Integer.parseInt(parts[6].trim()));
                        statistics.setTotalElixirSpent(Integer.parseInt(parts[7].trim()));
                        statistics.setTotalSpellDamageDealt(Integer.parseInt(parts[8].trim()));
                        statistics.setTotalSwarmTroopsDeployed(Integer.parseInt(parts[9].trim()));
                        statistics.setTotalGoldEarned(Integer.parseInt(parts[10].trim()));
                        statistics.setTotalCardCombos(Integer.parseInt(parts[11].trim()));
                        statistics.setCurrentWinStreak(Integer.parseInt(parts[12].trim()));
                        statistics.setMaxWinStreak(Integer.parseInt(parts[13].trim()));
                        statistics.setChallengesCompleted(Integer.parseInt(parts[14].trim()));
                        statistics.setChallengesWithThreeStars(Integer.parseInt(parts[15].trim()));
                        statistics.setNetworkMultiplayerWins(Integer.parseInt(parts[16].trim()));
                        statistics.setPvPMatchWins(Integer.parseInt(parts[17].trim()));
                    }
                } else if (QUEST_RESET_SECTION.equals(currentSection)) {
                    lastQuestResetTimestamp = Long.parseLong(line.trim());
                } else if (CHALLENGES_SECTION.equals(currentSection)) {
                    String[] parts = line.split(",");
                    if (parts.length >= 5) {
                        int typeId = Integer.parseInt(parts[0].trim());
                        boolean completed = Boolean.parseBoolean(parts[1].trim());
                        int stars = Integer.parseInt(parts[2].trim());
                        int attempts = Integer.parseInt(parts[3].trim());
                        int completions = Integer.parseInt(parts[4].trim());
                        
                        Challenge.ChallengeType type = findChallengeTypeById(typeId);
                        if (type != null) {
                            Challenge challenge = new Challenge(type, completed, stars, attempts, completions);
                            challenges.add(challenge);
                        }
                    }
                }
            }

            // Ensure all 28 cards have levels (fill missing ones with Level 1)
            for (int cardId = 1; cardId <= 28; cardId++) {
                cardLevels.putIfAbsent(cardId, 1);
            }

            
            // Ensure all challenge types exist (initialize if empty or incomplete)
            java.util.Set<Challenge.ChallengeType> existingTypes = new java.util.HashSet<>();
            for (Challenge challenge : challenges) {
                existingTypes.add(challenge.getType());
            }
            
            // Add any missing challenge types
            for (Challenge.ChallengeType type : Challenge.ChallengeType.values()) {
                if (!existingTypes.contains(type)) {
                    challenges.add(new Challenge(type, false, 0, 0, 0));
                }
            }
            
            System.out.println("Player profile loaded from: " + PROFILE_FILE);
            PlayerProfile profile = new PlayerProfile(gold, cardLevels, goldHistory, unlockedCards);
            profile.setChestCount(chestCount);
            profile.setDailyQuests(dailyQuests);
            profile.setAchievements(achievements);
            profile.setStatistics(statistics);
            profile.setChallenges(challenges);
            profile.setLastQuestResetTimestamp(lastQuestResetTimestamp);
            return profile;

        } catch (Exception e) {
            System.err.println("Failed to load player profile: " + e.getMessage());
            e.printStackTrace();
            // Return default profile on error
            return new PlayerProfile();
        }
    }

    /**
     * Helper method to find QuestType by ID.
     * "id": the quest type ID
     * returns the QuestType, or null if not found
     */
    private Quest.QuestType findQuestTypeById(int id) {
        for (Quest.QuestType type : Quest.QuestType.values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Helper method to find AchievementType by ID.
     * "id": the achievement type ID
     * returns the AchievementType, or null if not found
     */
    private Achievement.AchievementType findAchievementTypeById(int id) {
        for (Achievement.AchievementType type : Achievement.AchievementType.values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return null;
    }

    /**
     * Helper method to find ChallengeType by ID.
     * "id": the achievement type ID
     * returns the type, or null if not found
     */
    private Challenge.ChallengeType findChallengeTypeById(int id) {
        for (Challenge.ChallengeType type : Challenge.ChallengeType.values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return null;
    }
}
