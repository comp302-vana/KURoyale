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
            }
            
            System.out.println("Player profile saved to: " + PROFILE_FILE);
            
        } catch (Exception e) {
            System.err.println("Failed to save player profile: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Loads the player profile from file, or returns default profile if file doesn't exist.
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
                }
            }
            
            // Ensure all 28 cards have levels (fill missing ones with Level 1)
            for (int cardId = 1; cardId <= 28; cardId++) {
                cardLevels.putIfAbsent(cardId, 1);
            }
            
            System.out.println("Player profile loaded from: " + PROFILE_FILE);
            return new PlayerProfile(gold, cardLevels, goldHistory);
            
        } catch (Exception e) {
            System.err.println("Failed to load player profile: " + e.getMessage());
            e.printStackTrace();
            // Return default profile on error
            return new PlayerProfile();
        }
    }
}
