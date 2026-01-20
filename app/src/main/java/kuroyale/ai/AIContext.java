package kuroyale.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import kuroyale.arenapack.ArenaMap;
import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardFactory;
import kuroyale.cardpack.subclasses.BuildingCard;
import kuroyale.cardpack.subclasses.UnitCard;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.BuildingEntity;
import kuroyale.entitiypack.subclasses.UnitEntity;
import kuroyale.mainpack.GameEngine;

public class AIContext {
    // OVERVIEW: AIContext represents a mutable, automated player entity 
    // in the game. It uses the Strategy Pattern to decide moves.
    // A typical AIContext has a specific amount of current elixir, 
    // a managed deck of cards, and a hand of available cards.

    // The Abstraction Function:
    // AF(c) = An AI player where:
    //   Elixir Resources = c.currentElixir
    //   Deck Sequence = [c.aiDeckCards.get(0), ..., c.aiDeckCards.get(c.aiDeckCards.size()-1)]
    //   Current Hand = {c.aiHand.get(0), ..., c.aiHand.get(c.aiHand.size()-1)}
    //   Next Drawn Card's Index = c.nextCardIndex (pointing to the Deck Sequence)
    //   Strategy = c.strategy (The current behavior logic)

    // The Representation Invariant:
    //   c.currentElixir >= 0 && c.currentElixir <= c.MAX_ELIXIR &&
    //   c.aiDeckCards != null && c.aiDeckCards contains no null elements &&
    //   c.aiDeckCards.size() == 8 &&
    //   c.aiHand != null && c.aiHand.size() <= c.CARD_SLOT_COUNT (4) &&
    //   c.nextCardIndex >= 0 && c.nextCardIndex < c.aiDeckCards.size() &&
    //   c.arenaMap != null &&
    //   c.gameEngine != null &&
    //   c.strategy != null


    // --- Context State ---
    private double currentElixir;
    private final double MAX_ELIXIR = 10;
    private final double ELIXIR_REGEN_RATE = 1.0 / 2.8;
    private final double DOUBLE_ELIXIR_REGEN_RATE = 1.0 / 1.4;

    private List<Card> aiDeckCards = new ArrayList<>();
    private List<Card> aiHand = new ArrayList<>();
    private int nextCardIndex = 0;
    private final int CARD_SLOT_COUNT = 4;

    private ArenaMap arenaMap;
    private GameEngine gameEngine;
    private CardFactory cardFactory;
    
    private int rows;
    private int cols;

    // --- Strategy Reference ---
    private AIStrategy strategy; 
    private final double PLAY_CARD_PROBABILITY = 0.67;
    private Random random = new Random();

    // --- Timing ---
    private double timeSinceLastAICheck = 0.0;
    private double AI_CHECK_INTERVAL = 2.8;

    public AIContext(ArenaMap arenaMap, GameEngine gameEngine, AIStrategy aiStrategy) {
        this.arenaMap = arenaMap;
        this.gameEngine = gameEngine;
        this.cardFactory = CardFactory.getInstance();
        this.rows = ArenaMap.getRows();
        this.cols = ArenaMap.getCols();
        
        // Initialize Default Strategy
        this.strategy = aiStrategy;
        
        this.currentElixir = 5.0;
        createAIDeck();
        loadDecktoHand();
    }
    
    // Allows changing strategy dynamically
    public void setStrategy(AIStrategy strategy) {
        this.strategy = strategy;
    }

    private void createAIDeck(){
        List<Integer> selectedCards = new ArrayList<>();
        while(aiDeckCards.size() < 8){
            int a = random.nextInt(1, 28);
            while(selectedCards.contains(a)){
                a = random.nextInt(1, 28);
            }
            selectedCards.add(a);
            aiDeckCards.add(cardFactory.createCard(a));
        }
        nextCardIndex = 0;
    }

    private void loadDecktoHand() {
        aiHand.clear();
        for (int i = 0; i < CARD_SLOT_COUNT; i++) {
            if (nextCardIndex < aiDeckCards.size()) {
                Card card = aiDeckCards.get(nextCardIndex);
                aiHand.add(card);
                nextCardIndex++;
            }
        }
    }

    public void update(double deltaTime, int totalSeconds) {
        // 1. Elixir Logic (Model update)
        updateElixir(deltaTime, totalSeconds);
        
        // 2. AI Decision Tick
        timeSinceLastAICheck += deltaTime;
        if (timeSinceLastAICheck >= AI_CHECK_INTERVAL) {
            
            // Probability check logic remains part of the "Player Personality" (Context)
            // or could be moved to Strategy if preferred.
            if (random.nextDouble() < PLAY_CARD_PROBABILITY){
                executeTurn();
            }
            
            timeSinceLastAICheck = 0.0;
        }
    }

    private void updateElixir(double deltaTime, int totalSeconds) {
        if (currentElixir < MAX_ELIXIR) {
            double rate = (totalSeconds >= 60) ? ELIXIR_REGEN_RATE : DOUBLE_ELIXIR_REGEN_RATE;
            currentElixir += (rate * deltaTime);
            if (currentElixir > MAX_ELIXIR) currentElixir = MAX_ELIXIR;
        }
    }

    private void executeTurn() {
        // Delegate to Strategy 
        AIAction action = strategy.decideMove(aiHand, currentElixir, arenaMap, rows, cols);

        // Execute the Strategy's decision
        if (action != null) {
            applyAction(action);
        }
    }

    private void applyAction(AIAction action) {
        Card card = action.getCard();
        int row = action.getRow();
        int col = action.getCol();
        int cost = card.getCost();

        boolean isSpell = (card.getId() >= 25 && card.getId() <= 28);

        if (isSpell) {
            gameEngine.executeSpell(card.getId(), row, col, false);
            finalizeCardPlay(card, cost, row, col);
        } 
        else {
            // Instantiate Entity based on Card
            AliveEntity playedEntity = createEntityFromCard(card);
            
            // Place and Add to Map
            playedEntity.setPosition(row, col);
            arenaMap.setEntity(row, col, playedEntity);
            arenaMap.addEntity(playedEntity);
            
            finalizeCardPlay(card, cost, row, col);
        }
    }

    private AliveEntity createEntityFromCard(Card card) {
        int id = card.getId();
        if (id <= 15) {
            return new UnitEntity((UnitCard) card, false);
        } else if (id <= 24) {
            return new BuildingEntity((BuildingCard) card, false);
        }
        return null;
    }

    private void finalizeCardPlay(Card card, int cost, int r, int c) {
        currentElixir -= cost;
        System.out.println("AI played: " + card.getName() + " at (" + r + ", " + c + ")");
        
        // Cycle Cards
        aiHand.remove(card);
        if (nextCardIndex >= aiDeckCards.size()) {
            nextCardIndex = 0;
        }
        if (nextCardIndex < aiDeckCards.size() && !aiDeckCards.isEmpty()) {
            Card nextCard = aiDeckCards.get(nextCardIndex);
            aiHand.add(nextCard); 
            nextCardIndex++;
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
}
