package kuroyale.deckpack;

import java.util.ArrayList;
import java.util.List;
import kuroyale.cardpack.Card;

public class Deck {
    private String name;
    private List<Card> cards;
    private static final int DECK_SIZE = 8;

    public Deck(String name) {
        this.name = name;
        this.cards = new ArrayList<>();
    }

    public Deck(String name, List<Card> cards) {
        this.name = name;
        this.cards = new ArrayList<>(cards);
    }

    public String getName() {
        return name;
    }

    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    public boolean addCard(Card card) {
        if (isFull()) {
            throw new IllegalStateException("Deck is already full"); // ensures the deck does not have more than 8 cards
        }
        for (Card selectedCard : cards) {
            if (selectedCard.getId() == card.getId()) {
                throw new IllegalArgumentException("Card is already in the deck"); // ensures the card is not already
                                                                                   // added to the deck
            }
        }
        cards.add(card);
        return true;
    }

    public boolean removeCard(Card card) {
        return cards.remove(card);
    }

    public boolean removeCard(int index) {
        if (index >= 0 && index < cards.size()) {
            cards.remove(index);
            return true;
        }
        return false;
    }

    public boolean isFull() {
        return cards.size() >= DECK_SIZE;
    }

    public int getSize() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public void clear() {
        cards.clear();
    }

    public void setName(String name) {
        this.name = name;
    }

}
