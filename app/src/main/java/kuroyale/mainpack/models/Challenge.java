package kuroyale.mainpack.models;


//Represents a challange with its type, completion and attempt stats.
//Uses strategy pattern -> In the strategy pattern we use each challenge type has different validation.

public class Challenge {
    private ChallengeType type;
    private boolean completed;
    private int starsEarned;
    private int timesAttempted;
    private int timesCompleted;

    public Challenge(ChallengeType type, boolean completed, int starsEarned, int timesAttempted, int timesCompleted){
        this.type = type;
        this.completed = completed;
        this.starsEarned = starsEarned;
        this.timesAttempted = timesAttempted;
        this.timesCompleted = timesCompleted;
    }

    public enum ChallengeType{
        SWARM_MASTER(1, "Swarm Master", 
            "Use only swarm troops. Must include at least 5 swarm cards.", 
            250),
        SPELL_BARRAGE(2, "Spell Barrage", 
            "Deck must contain all 4 spell cards. Spells cost 1 less Elixir (minimum 1).", 
            300),
        NO_BUILDINGS_ALLOWED(3, "No Buildings Allowed", 
            "Cannot use building cards. Deck must contain only troops and spells.", 
            200),
        BUDGET_BATTLE(4, "Budget Battle", 
            "Can only use cards costing 3 Elixir or less.", 
            250),
        TANK_RUSH(5, "Tank Rush", 
            "Can only use high-HP units: Giant, Knight, Valkyrie, Mini P.E.K.K.A, Barbarians. No spells or buildings.", 
            300);

        private final int id;
        private final String name;
        private final String description;
        private final int goldReward;

        ChallengeType(int id, String name, String description, int goldReward){
            this.id = id;
            this.name = name;
            this.description = description;
            this.goldReward = goldReward;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getGoldReward() { return goldReward; }
    }

    //getters and setters of the Challange class
    public ChallengeType getType() { return type; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public int getStarsEarned() { return starsEarned; }
    public void setStarsEarned(int starsEarned) { this.starsEarned = Math.max(0, Math.min(3, starsEarned)); }
    public int getAttempts() { return timesAttempted; }
    public void incrementAttempts() { this.timesAttempted++; }
    public int getNumOfCompletion() {return timesCompleted;}
    public void incrementCompletion() {this.timesCompleted++;}

}
