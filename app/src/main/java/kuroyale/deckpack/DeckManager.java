package kuroyale.deckpack;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardFactory;

public class DeckManager {
    private static final String DECKS_DIR = "decks";
    private static Deck currentDeck;
    private static List<Deck> savedDecks;

    static {
        savedDecks = new ArrayList<>();
        loadAllDecks();
    }

    public static void saveDeck(Deck deck) {
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

    public static Deck loadDeck(String name) {
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
    private static Card findCardByName(String name) {
        for (Card card : CardFactory.getAllCards()) {
            if (card.getName().equals(name)) {
                return card;
            }
        }
        return null;
    }

    public static void loadAllDecks() {
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

    public static void deleteDeck(String name) {
        File deckFile = new File(DECKS_DIR, name + ".deck");
        if (deckFile.exists()) {
            deckFile.delete();
        }
        savedDecks.removeIf(deck -> deck.getName().equals(name));
        if (currentDeck != null && currentDeck.getName().equals(name)) {
            currentDeck = null;
        }
    }

    public static List<Deck> getAllDecks() {
        return new ArrayList<>(savedDecks);
    }

    public static Deck getDeckByName(String name) {
        for (Deck deck : savedDecks) {
            if (deck.getName().equals(name)) {
                return deck;
            }
        }
        return null;
    }

    public static Deck getCurrentDeck() {
        return currentDeck;
    }

    public static void setCurrentDeck(Deck deck) {
        currentDeck = deck;
    }

    // Save the selected deck index to file
    public static void saveSelectedDeckIndex(int index) {
        File decksDir = new File(DECKS_DIR);
        if (!decksDir.exists()) {
            decksDir.mkdirs();
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(new File(decksDir, "selected_deck.txt")))) {
            writer.println(index);
        } catch (IOException e) {
            System.err.println("Error saving deck index: " + e.getMessage());
        }
    }

    // Load the selected deck index from file
    public static int loadSelectedDeckIndex() {
        File indexFile = new File(DECKS_DIR, "selected_deck.txt");
        if (!indexFile.exists()) {
            return 0; // Default to first deck
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(indexFile))) {
            String line = reader.readLine();
            if (line != null) {
                return Integer.parseInt(line.trim());
            }
        } catch (Exception e) {
            System.err.println("Error loading deck index: " + e.getMessage());
        }
        return 0;
    }
}
