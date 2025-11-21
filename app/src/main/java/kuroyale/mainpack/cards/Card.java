package kuroyale.mainpack.cards;

public class Card {

    private int id;
    private String name;
    private String description;
    private int cost;
    private CardType type;
    private CardCategory category;
    private CardTarget target;
    

   public Card(String name, int cost, CardType type, CardCategory category, CardTarget target) {
        this.name = name;
        this.cost = cost;
        this.type = type;
        this.category = category;
        this.target = target;
    }
}
