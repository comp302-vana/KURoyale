package kuroyale.mainpack.models;

public class Combo {
    private ComboType type;
    private boolean triggered;

    public Combo(ComboType type, boolean triggered) {
        this.type = type;
        this.triggered = triggered;
    }

    public enum ComboType {
        TANK_SUPPORT(1, "Tank + Support", 
            "Play Giant or Knight, then play any ranged troop within 5 seconds. Ranged troop gains +15% damage."),
        SPELL_SYNERGY(2, "Spell Synergy", 
            "Play any spell, then play a different spell within 5 seconds. Second spell costs 1 less Elixir."),
        SWARM_ATTACK(3, "Swarm Attack", 
            "Play any two swarm cards within 5 seconds. All swarm units gain +10% movement speed."),
        BUILDING_DEFENSE(4, "Building Defense", 
            "Play any two building cards within 5 seconds. Both buildings gain +20% HP."),
        AIR_ASSAULT(5, "Air Assault", 
            "Play Minions and Minion Horde within 5 seconds. All air units gain +15% damage."),
        ROYAL_COMBO(6, "Royal Combo", 
            "Play Knight and Archers within 5 seconds. Knight gains +100 HP."),
        SIEGE_MODE(7, "Siege Mode", 
            "Play Mortar and any defensive building within 5 seconds. Mortar range increased by +2 tiles."),
        RUSH_ATTACK(8, "Rush Attack", 
            "Play Hog Rider and any low-cost card (1-2 Elixir) within 5 seconds. Hog Rider speed increased by +20%.");

        private final int id;
        private final String name;
        private final String description;
        
        ComboType(int id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        //getters
        public int getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
    }


        public ComboType getType() {
            return type;
        }

        //setters
        public void setType(ComboType type) {
            this.type = type;
        }
        
        
        public void setTriggered(boolean triggered) {
            this.triggered = triggered;
        }

        //triggered check
        public boolean isTriggered() {
            return triggered;
        }
    }
