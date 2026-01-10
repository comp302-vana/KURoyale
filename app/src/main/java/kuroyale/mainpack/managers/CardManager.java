package kuroyale.mainpack.managers;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.Node;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import kuroyale.cardpack.Card;
import kuroyale.deckpack.Deck;
import kuroyale.deckpack.DeckManager;

/**
 * Manages deck, hand, and card slot UI updates.
 * High cohesion: All card/deck management in one place.
 */
public class CardManager {
    private final int CARD_SLOT_COUNT = 4;
    private List<Card> currentDeckCards = new ArrayList<>();
    private List<Card> currentHand = new ArrayList<>();
    private int nextCardIndex = 0;

    private AnchorPane[] cardSlots;
    private Label[] costLabels;
    private ChallengeManager challengeManager;

    public CardManager(AnchorPane cardSlot0, AnchorPane cardSlot1, AnchorPane cardSlot2, AnchorPane cardSlot3,
                      Label card1CostLabel, Label card2CostLabel, Label card3CostLabel, Label card4CostLabel) {
        this.cardSlots = new AnchorPane[]{cardSlot0, cardSlot1, cardSlot2, cardSlot3};
        this.costLabels = new Label[]{card1CostLabel, card2CostLabel, card3CostLabel, card4CostLabel};
    }

    public List<Card> getCurrentHand() { return currentHand; }
    
    public void setChallengeManager(ChallengeManager challengeManager) {
        this.challengeManager = challengeManager;
        // Refresh costs for cards currently in hand
        refreshCurrentHandCosts();
    }
    
    private void refreshCurrentHandCosts() {
        for (int i = 0; i < currentHand.size() && i < CARD_SLOT_COUNT; i++) {
            Card card = currentHand.get(i);
            if (card != null && costLabels[i] != null) {
                int displayCost = card.getCost();
                if (challengeManager != null) {
                    displayCost = challengeManager.getModifiedCost(card.getCost(), card.getId());
                }
                costLabels[i].setText(String.valueOf(displayCost));
            }
        }
    }
    public int findCardSlotIndex(int cardID) {
        for (int i = 0; i < currentHand.size(); i++) {
            Card handCard = currentHand.get(i);
            if (handCard.getId() == cardID) {
                return i;
            }
        }
        return -1;
    }

    public void loadDeck() {
        Deck currentDeck = DeckManager.getCurrentDeck();
        if (currentDeck != null) {
            currentDeckCards = new ArrayList<>(currentDeck.getCards());
            nextCardIndex = 0;
            currentHand.clear();
            loadDeckToSlots();
        } else {
            System.err.println("No active deck.");
        }
    }
    
    /**
     * Load a specific deck for PvP mode (backward compatible with loadDeck()).
     * @param deck The deck to load for this player
     */
    public void loadDeckForPlayer(Deck deck) {
        if (deck != null) {
            currentDeckCards = new ArrayList<>(deck.getCards());
            nextCardIndex = 0;
            currentHand.clear();
            loadDeckToSlots();
        } else {
            System.err.println("No deck provided for player.");
        }
    }

    public void loadDeckToSlots() {
        currentHand.clear();

        for (int i = 0; i < CARD_SLOT_COUNT; i++) {
            if (nextCardIndex < currentDeckCards.size()) {
                Card card = currentDeckCards.get(nextCardIndex);
                currentHand.add(card);
                updateCardSlot(cardSlots[i], costLabels[i], card, i);
                nextCardIndex++;
            } else {
                clearCardSlot(cardSlots[i], costLabels[i], i);
            }
        }
    }

    public void cycleCardInSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= cardSlots.length) {
            return;
        }

        if (nextCardIndex >= currentDeckCards.size()) {
            nextCardIndex = 0;
        }

        Card nextCard = null;
        
        while (nextCardIndex < currentDeckCards.size() && !currentDeckCards.isEmpty()) {
            Card candidateCard = currentDeckCards.get(nextCardIndex);
            
            boolean cardInHand = false;
            for (Card handCard : currentHand) {
                if (handCard.getId() == candidateCard.getId()) {
                    cardInHand = true;
                    break;
                }
            }
            if (!cardInHand) {
                nextCard = candidateCard;
                break;
            }
            nextCardIndex++;
            
            if (nextCardIndex >= currentDeckCards.size()) {
                nextCardIndex = 0;
            }
        }
        
        if (nextCard != null) {
            if (slotIndex < currentHand.size()) {
                currentHand.set(slotIndex, nextCard);
            } else {
                currentHand.add(slotIndex, nextCard);
            }

            updateCardSlot(cardSlots[slotIndex], costLabels[slotIndex], nextCard, slotIndex);
            nextCardIndex++;
            if (nextCardIndex >= currentDeckCards.size()) {
                nextCardIndex = 0;
            }
        } else {
            if (slotIndex < currentHand.size()) {
                currentHand.remove(slotIndex);
            }
            clearCardSlot(cardSlots[slotIndex], costLabels[slotIndex], slotIndex);
        }
    }

    private void updateCardSlot(AnchorPane slotPane, Label costLabel, Card card, int slotIndex) {
        if (slotPane == null || card == null) return;

        ImageView cardImage = getImageFromPane(slotPane);
        if (cardImage != null) {
            String cardName = card.getName().toLowerCase().replaceAll(" ", "");
            String imagePath = "/kuroyale/images/cards/" + cardName + ".png";

            try {
                java.io.InputStream imageStream = getClass().getResourceAsStream(imagePath);
                if (imageStream != null) {
                    javafx.scene.image.Image newImage = new javafx.scene.image.Image(imageStream);
                    cardImage.setImage(null);
                    cardImage.setImage(newImage);
                } else {
                    javafx.scene.image.Image newImage = new javafx.scene.image.Image(imagePath);
                    cardImage.setImage(null);
                    cardImage.setImage(newImage);
                }
            } catch (Exception e) {
                System.err.println("Failed to load image: " + imagePath + " - " + e.getMessage());
            }
        }

        if (costLabel != null) {
            int displayCost = card.getCost();
            // Decorator Pattern: Apply challenge-specific cost modification for display
            if (challengeManager != null) {
                displayCost = challengeManager.getModifiedCost(card.getCost(), card.getId());
            }
            costLabel.setText(String.valueOf(displayCost));
        }

        slotPane.setOnDragDetected(null);
        Pane innerPane = getInnerPaneFromSlot(slotPane);
        if (innerPane != null) {
            innerPane.setOnDragDetected(null);
            for (Node child : innerPane.getChildren()) {
                if (child instanceof ImageView) {
                    child.setOnDragDetected(null);
                }
            }
            makeDraggable(innerPane, String.valueOf(card.getId()));
        }
    }

    private void clearCardSlot(AnchorPane slotPane, Label costLabel, int slotIndex) {
        ImageView cardImage = getImageFromPane(slotPane);
        if (cardImage != null) {
            cardImage.setImage(null);
        }

        if (costLabel != null) {
            costLabel.setText("");
        }

        slotPane.setOnDragDetected(null);
        Pane innerPane = getInnerPaneFromSlot(slotPane);
        if (innerPane != null) {
            innerPane.setOnDragDetected(null);
            for (Node child : innerPane.getChildren()) {
                if (child instanceof ImageView) {
                    child.setOnDragDetected(null);
                }
            }
        }
    }

    private void makeDraggable(Pane source, String type) {
        if (source == null) return;
        source.setOnDragDetected(event -> {
            var db = source.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(type);
            db.setContent(content);
            event.consume();
        });
    }

    private ImageView getImageFromPane(AnchorPane ap) {
        for (Node n : ap.getChildren()) {
            if (n instanceof Pane p) {
                return (ImageView) p.getChildren().get(1);
            }
        }
        return null;
    }

    private Pane getInnerPaneFromSlot(AnchorPane ap) {
        for (Node n : ap.getChildren()) {
            if (n instanceof Pane p) {
                return p;
            }
        }
        return null;
    }
}
