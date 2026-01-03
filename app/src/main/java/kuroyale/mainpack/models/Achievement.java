package kuroyale.mainpack.models;

public class Achievement {

    private AchievementType type;
    private int currentProgress;
    private boolean completed;
    private boolean claimed;

    Achievement(AchievementType type, int currentProgress, boolean completed, boolean claimed) {
        this.type = type;
        this.currentProgress = currentProgress;
        this.completed = completed;
        this.claimed = claimed;
    }

    public enum AchievementType {
        FIRST_BLOOD(1, "First Blood", "Win your first match", 500, 1),
        TOWER_HUNTER(2, "Tower Hunter", "Destroy 50 Crown Towers total", 750, 50),
        CHALLENGE_MASTER(3, "Challenge Master", "Complete all 5 challenges", 1500, 5),
        THREE_STAR_HERO(4, "Three-Star Hero", "Get 3 stars on any challenge", 600, 1),
        LEGENDARY_COLLECTOR(5, "Legendary Collector", "Upgrade a Legendary card to Level 3", 1000, 1),
        NETWORK_WARRIOR(6, "Network Warrior", "Win 10 network multiplayer matches", 800, 10),
        ARMY_BUILDER(7, "Army Builder", "Deploy 100 swarm troops total", 700, 100),
        SPELL_MASTER(8, "Spell Master", "Deal 10,000 damage with spells total", 800, 10000),
        GOLD_HOARDER(9, "Gold Hoarder", "Accumulate 5,000 total gold earned", 500, 5000),
        VETERAN_PLAYER(10, "Veteran Player", "Play 50 matches", 600, 50),
        COMBO_EXPERT(11, "Combo Expert", "Trigger 25 card combos", 750, 25),
        UNDEFEATED(12, "Undefeated", "Win 5 matches in a row", 1000, 5);
        
        private final int id;
        private final String name;
        private final String description;
        private final int goldReward;
        private final int targetValue;
        
        AchievementType(int id, String name, String description, int goldReward, int targetValue) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.goldReward = goldReward;
            this.targetValue = targetValue;
        }
        
        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public int getGoldReward() {
            return goldReward;
        }

        public int getTargetValue() {
            return targetValue;
        }
    }
    
    
    public AchievementType getAchievementType() {
        return type;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public boolean getCompleted() {
        return completed;
    }

    public boolean getClaimed() {
        return claimed;
    }

    public void setAchievementType(AchievementType type) {
        this.type = type;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }
}
