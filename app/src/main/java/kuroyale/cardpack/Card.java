package kuroyale.cardpack;

public abstract class Card {

    private final int id;
    private final String name;
    private final String description;
    private final int cost;
    private final double damage;
    private final double radius;
    private final CardType type;
    private final CardCategory category;
    private final CardTarget target;
    
    public Card(
            int id, String name, String description, int cost, double damage,
            double radius, CardType type, CardCategory category, CardTarget target) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.damage = damage;
        this.radius = radius;
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

    public double getDamage() {
        return damage;
    }

    public double getRadius() {
        return radius;
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
        return name + " (Elixir Cost: " + cost + ")" + "\n" + "Description: " + description;
    }
}
