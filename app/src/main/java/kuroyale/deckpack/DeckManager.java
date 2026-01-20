package kuroyale.deckpack;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardFactory;

public class DeckManager {
    private static DeckManager INSTANCE;
    private static final String DECKS_DIR = "decks";
    
    private Deck currentDeck;
    private List<Deck> savedDecks;
    private int selectedDeckNumber;

    public static DeckManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DeckManager();
        }
        return INSTANCE;
    }

    public DeckManager() {
        savedDecks = new ArrayList<>();
        selectedDeckNumber = 1;
        loadAllDecks();
    }

    public void saveDeck(Deck deck) {
        File decksDir = new File(DECKS_DIR);
        if (!decksDir.exists()) {
            decksDir.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(
                new File(decksDir, deck.getName() + ".deck")))) {
            for (Card card : deck.getCards()) {
                writer.println(card.getName());
            }
            savedDecks.add(deck);
        } catch (IOException e) {
            System.err.println("Error saving deck: " + e.getMessage());
        }
    }

    public Deck loadDeck(String name) {
        File deckFile = new File(DECKS_DIR, name + ".deck");
        if (!deckFile.exists()) {
            return null;
        }

        List<Card> cards = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(deckFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String cardName = line.trim();
                // Find card by name from all available cards
                Card card = findCardByName(cardName);
                if (card != null) {
                    cards.add(card);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading deck: " + e.getMessage());
            return null;
        }
        return new Deck(name, cards);
    }

    // Helper method to find a card by its name using getName()
    private Card findCardByName(String name) {
        CardFactory cf = CardFactory.getInstance();
        for (Card card : cf.getAllCards()) {
            if (card.getName().equals(name)) {
                return card;
            }
        }
        return null;
    }

    public void loadAllDecks() {
        File decksDir = new File(DECKS_DIR);
        if (!decksDir.exists()) {
            return;
        }

        savedDecks.clear();
        File[] files = decksDir.listFiles((dir, name) -> name.endsWith(".deck"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName().replace(".deck", "");
                Deck deck = loadDeck(name);
                if (deck != null) {
                    savedDecks.add(deck);
                }
            }
        }
    }

    public void deleteDeck(String name) {
        File deckFile = new File(DECKS_DIR, name + ".deck");
        if (deckFile.exists()) {
            deckFile.delete();
        }
        savedDecks.removeIf(deck -> deck.getName().equals(name));
        if (currentDeck != null && currentDeck.getName().equals(name)) {
            currentDeck = null;
        }
    }

    public List<Deck> getAllDecks() {
        return new ArrayList<>(savedDecks);
    }

    public Deck getDeckByName(String name) {
        for (Deck deck : savedDecks) {
            if (deck.getName().equals(name)) {
                return deck;
            }
        }
        return null;
    }

    public Deck getCurrentDeck() {
        return currentDeck;
    }

    public void setCurrentDeck(Deck deck) {
        currentDeck = deck;
    }

    // ===== Numbered Deck System Methods =====

    public int getSelectedDeckNumber() {
        return selectedDeckNumber;
    }

    public void setSelectedDeckNumber(int deckNumber) {
        if (deckNumber >= 1 && deckNumber <= 8) {
            selectedDeckNumber = deckNumber;
        }
    }

    public int getDeckCardCount(int deckNumber) {
        Deck deck = loadDeckByNumber(deckNumber);
        return deck != null ? deck.getCards().size() : 0;
    }

    public Deck loadDeckByNumber(int deckNumber) {
        return loadDeck("Deck" + deckNumber);
    }

    public void saveDeckByNumber(int deckNumber, Deck deck) {
        if (deck == null) {
            // Delete the deck file if deck is null or empty
            deleteDeck("Deck" + deckNumber);
            return;
        }
        // Create a new deck with the numbered name
        Deck numberedDeck = new Deck("Deck" + deckNumber, deck.getCards());
        saveDeck(numberedDeck);
    }
}
