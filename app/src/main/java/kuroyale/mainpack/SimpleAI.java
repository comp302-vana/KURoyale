package kuroyale.mainpack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;
import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardFactory;
import kuroyale.cardpack.subclasses.BuildingCard;
import kuroyale.cardpack.subclasses.UnitCard;
import kuroyale.deckpack.DeckManager;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.BuildingEntity;
import kuroyale.entitiypack.subclasses.UnitEntity;




public class SimpleAI {
    // OVERVIEW: SimpleAI represents a mutable, automated player entity 
    // in the game.
    // A typical SimpleAI has a specific amount of current elixir, 
    // a managed deck of cards, and a hand of available cards.
    // It interacts with the ArenaMap to deploy entities based on 
    // resource availability.

    // The Abstraction Function:
    // AF(c) = An AI player where:
    //   Elixir Resources = c.currentElixir
    //   Deck Sequence = [c.aiDeckCards.get(0), ..., c.aiDeckCards.get(c.aiDeckCards.size()-1)]
    //   Current Hand = {c.aiHand.get(0), ..., c.aiHand.get(c.aiHand.size()-1)}
    //   Next Drawn Card's Index = c.nextCardIndex (pointing to the Deck Sequence)

    // The Representation Invariant:
    //   c.currentElixir >= 0 && c.currentElixir <= c.MAX_ELIXIR &&
    //   c.aiDeckCards != null && c.aiDeckCards contains no null elements &&
    //   c.aiDeckCards.size() == 8 (Based on createAIDeck logic) &&
    //   c.aiHand != null && c.aiHand contains no null elements &&
    //   c.aiHand.size() <= c.CARD_SLOT_COUNT (4) &&
    //   c.nextCardIndex >= 0 && c.nextCardIndex < c.aiDeckCards.size() &&
    //   c.arenaMap != null &&
    //   c.gameEngine != null

    private double currentElixir =5.0;
    private final double MAX_ELIXIR = 10;
    private final double ELIXIR_REGEN_RATE = 1.0 / 2.8;
    private final double DOUBLE_ELIXIR_REGEN_RATE = 1.0 / 1.4;

    private List<Card> aiDeckCards = new ArrayList<>();
    private List<Card> aiHand = new ArrayList<>();
    private int nextCardIndex = 0;
    private final int CARD_SLOT_COUNT = 4;

    private ArenaMap arenaMap;
    private int rows;
    private int cols;

    private Random random = new Random();
    private final double PLAY_CARD_PROBABILITY = 0.67;

    private GameEngine gameEngine;
    private CardFactory cardFactory;
    private double timeSinceLastAICheck = 0.0;
    private double AI_CHECK_INTERVAL = 2.8;

    public SimpleAI(ArenaMap arenaMap, GameEngine gameEngine) {
        this.arenaMap = arenaMap;
        this.gameEngine = gameEngine;
        this.cardFactory = CardFactory.getInstance();
        this.rows = ArenaMap.getRows();
        this.cols = ArenaMap.getCols();
        
        this.currentElixir = 5.0;
        createAIDeck();
        loadDecktoHand();
    }
    
    private void createAIDeck(){ // AI Deck Randomized
        List<Integer> selectedCards = new ArrayList<>();
        while(aiDeckCards.size() < 8){
            int a = random.nextInt(1, 28);
            while(selectedCards.contains(a)){
                a = random.nextInt(1,28);
            }
            selectedCards.add(a);
            aiDeckCards.add(cardFactory.createCard(a));
        }
        nextCardIndex = 0;
    }

    private void loadDecktoHand() {
        aiHand.clear();
        
        // Draw initial 4 cards
        for (int i = 0; i < CARD_SLOT_COUNT; i++) {
            if (nextCardIndex < aiDeckCards.size()) {
                Card card = aiDeckCards.get(nextCardIndex);
                aiHand.add(card);
                nextCardIndex++;
            }
        }
    }

    //I could also use this repOk method inside other functions for assertion
    //but as we wrote a whole test class I found that unnecessary
    private boolean repOk() {
        if(currentElixir < 0 || currentElixir > MAX_ELIXIR){
            return false;
        }

        if (aiDeckCards == null || aiDeckCards.size() != 8) {
            return false;
        }
        for (Card c : aiDeckCards) {
            if (c == null) return false;
        }

        if (aiHand == null || aiHand.size() > CARD_SLOT_COUNT) {
            return false;
        }
        for (Card c : aiHand) {
            if (c == null) return false;
        }

        if (!aiDeckCards.isEmpty()) {
            if (nextCardIndex < 0 || nextCardIndex >= aiDeckCards.size()) {
                return false;
            }
        }

        return gameEngine != null && arenaMap != null;
    }

    public void update(double deltaTime, int totalSeconds) {
        // Regenerating elixir (same as the player)
        if (currentElixir < MAX_ELIXIR) {
            if (totalSeconds >= 60) {
                currentElixir += (ELIXIR_REGEN_RATE * deltaTime);
                if (currentElixir > MAX_ELIXIR) currentElixir = MAX_ELIXIR;
            } else {
                currentElixir += (DOUBLE_ELIXIR_REGEN_RATE * deltaTime);
                if (currentElixir > MAX_ELIXIR) currentElixir = MAX_ELIXIR;
            }
        }
        
        timeSinceLastAICheck += deltaTime;
        if (totalSeconds >= 60 && timeSinceLastAICheck >= AI_CHECK_INTERVAL) {
            if (random.nextDouble() < PLAY_CARD_PROBABILITY){
                tryPlayCard();
            }
            
            timeSinceLastAICheck = 0.0;
        }
        else if (totalSeconds < 60 && timeSinceLastAICheck >= AI_CHECK_INTERVAL) {
            if (random.nextDouble() < PLAY_CARD_PROBABILITY){
                tryPlayCard();
            }
            timeSinceLastAICheck = 0.0;
        }
    }

    private void tryPlayCard(){
        if(aiHand.isEmpty()){return;}

        List<Card> cardsAffordable = new ArrayList<>();
        for (Card card : aiHand) {
            if (card != null && currentElixir >= card.getCost()) {
                cardsAffordable.add(card);
            }
        }

        if(cardsAffordable.isEmpty()){return;}
        Card aiCard = cardsAffordable.get(random.nextInt(cardsAffordable.size()));
        int cardID = aiCard.getId();
        int cost = aiCard.getCost();
        
        if (placeCard(cardID, cost)) {
            aiHand.remove(aiCard);
            
            if (nextCardIndex >= aiDeckCards.size()) {
                nextCardIndex = 0;
            }
            
            if (nextCardIndex < aiDeckCards.size() && !aiDeckCards.isEmpty()) {
                Card nextCard = aiDeckCards.get(nextCardIndex);
                aiHand.add(nextCard); 
                nextCardIndex++;
            }
        }
    }

    private boolean placeCard(int cardID, int cost){
        int enemySideStartCol = cols / 2 + 1;

        boolean isSpell = (cardID >= 25 && cardID <= 28);

        if(isSpell){
            int spellRow = random.nextInt(rows);
            int spellCol = random.nextInt(cols / 2 - 1); // Player's side
    
            // Need access to GameEngine - see step 4
            gameEngine.executeSpell(cardID, spellRow, spellCol, false); // false = AI spell
            currentElixir -= cost;
            System.out.println("AI cast spell " + cardID + " at (" + spellRow + ", " + spellCol + ")");
            return true;
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            int row = random.nextInt(rows);
            int col = enemySideStartCol + random.nextInt(cols - enemySideStartCol);
            
            // Entity creation
            AliveEntity playedEntity;
            if (cardID <= 15) {
                playedEntity = new UnitEntity(((UnitCard) cardFactory.createCard(cardID)), false); // false = enemy
            } else if (cardID <= 24) {
                playedEntity = new BuildingEntity(((BuildingCard) cardFactory.createCard(cardID)), false); // false = enemy
            } else {
                // Spell cards will be implemented in the future update
                continue;
            }
            
            // Entity Placement Check
            boolean placementOK;
            int cc = col;
            do {
                placementOK = arenaMap.placeObject(row, cc, ArenaObjectType.ENTITY);
                cc--; 
            } while (!placementOK && cc >= enemySideStartCol);
            cc++;
            
            if (placementOK) {
                playedEntity.setPosition(row, cc);
                arenaMap.setEntity(row, cc, playedEntity);
                arenaMap.addEntity(playedEntity);
                currentElixir -= cost;
                System.out.println("AI played: " + playedEntity.getCard().getName() + " at (" + row + ", " + cc + ")");
                return true;
            }
        }
        
        return false;

    }
}
