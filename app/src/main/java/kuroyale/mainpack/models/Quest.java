package kuroyale.mainpack.models;

public class Quest {

    private QuestType type;
    private int currentProgress;
    private boolean completed;
    private boolean claimed;


    public Quest(QuestType type, int currentProgress, boolean completed, boolean claimed) {
        this.type = type;
        this.currentProgress = currentProgress;
        this.completed = completed;
        this.claimed = claimed;
    }

    public enum QuestType {
        WIN_3_MATCHES(1, "Win 3 matches", 250, 3),
        DESTROY_5_CROWN_TOWERS(2, "Destroy 5 Crown Towers", 200, 5),
        PLAY_10_SPELL_CARDS(3, "Play 10 spell cards", 150, 10),
        DEPLOY_15_TROOP_CARDS(4, "Deploy 15 troop cards", 175, 15),
        SPEND_100_ELIXIR(5, "Spend 100 total Elixir", 100, 100),
        WIN_WITHOUT_LOSING_TOWER(6, "Win a match without losing a Crown Tower", 300, 1),
        PLAY_5_BUILDING_CARDS(7, "Play 5 building cards", 150, 5),
        DEAL_3000_SPELL_DAMAGE(8, "Deal 3000 damage with spells", 200, 3000),
        WIN_WITH_COMMON_CARDS_ONLY(9, "Win using only common cards", 250, 1),
        COMPLETE_2_CHALLENGES(10, "Complete 2 challenges", 300, 2),
        WIN_NETWORK_MULTIPLAYER(11, "Win a network multiplayer match", 200, 1),
        PLAY_20_CARDS_SINGLE_MATCH(12, "Play 20 cards in a single match", 150, 20),
        WIN_2_MATCHES_IN_ROW(13, "Win 2 matches in a row", 300, 2),
        DESTROY_KING_TOWER(14, "Destroy an enemy King Tower", 350, 1),
        WIN_PVP_MATCH(15, "Win a PvP match", 200, 1);
        
        private final int id;
        private final String description;
        private final int goldReward;
        private final int targetValue;
        
        QuestType(int id, String description, int goldReward, int targetValue) {
            this.id = id;
            this.description = description;
            this.goldReward = goldReward;
            this.targetValue = targetValue;
        }
        
        public int getId() {
            return id;
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
    
    //getters
    
    public QuestType getQuestType() {
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

    //setters
    
    public void setQuestType(QuestType type) {
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

