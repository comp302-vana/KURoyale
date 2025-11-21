package kuroyale.mainpack.cards;

public abstract class Card {

    private int id;
    private String name;
    private String description;
    private int cost;
    private CardType type;
    private CardCategory category;
    private CardTarget target;
    

   public Card(int id, String name, String description, int cost, CardType type, CardCategory category, CardTarget target) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.type = type;
        this.category = category;
        this.target = target;
    }

       public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCost() {
        return cost;
    }

    public CardType getType() {
        return type;
    }

    public CardCategory getCategory() {
        return category;
    }

    public CardTarget getTarget() {
        return target;
    }

        public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name + " (Cost: " + cost + ")" + "\n" + "Description: " + description;
    }
}
