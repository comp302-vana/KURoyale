package kuroyale.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;
import kuroyale.cardpack.Card;

public class SimpleAI implements AIStrategy {
    private final Random random = new Random();

    @Override
    public AIAction decideMove(List<Card> hand, double currentElixir, ArenaMap map, int rows, int cols) {
        // 1. Filter Affordable Cards
        List<Card> affordableCards = new ArrayList<>();
        for (Card card : hand) {
            if (card != null && currentElixir >= card.getCost()) {
                affordableCards.add(card);
            }
        }

        if (affordableCards.isEmpty()) {
            return null; // No move possible
        }

        // 2. Pick a Random Card
        Card chosenCard = affordableCards.get(random.nextInt(affordableCards.size()));
        int cardID = chosenCard.getId();
        boolean isSpell = (cardID >= 25 && cardID <= 28);

        // 3. Determine Position
        if (isSpell) {
            // Spells can be placed anywhere
            int r = random.nextInt(rows);
            int c = random.nextInt(cols / 2 - 1); // Player's side
            return new AIAction(chosenCard, r, c);
        } else {
            // Units need valid placement (Try 10 times, as per original logic)
            int enemySideStartCol = cols / 2 + 1;
            
            for (int attempt = 0; attempt < 10; attempt++) {
                int r = random.nextInt(rows);
                int c = enemySideStartCol + random.nextInt(cols - enemySideStartCol);

                // Collision Logic from original placeCard method
                int cc = c;
                boolean placementOK;
                do {
                    // Check if placement is valid (assuming placeObject checks validity)
                    placementOK = map.placeObject(r, cc, ArenaObjectType.ENTITY);
                    if (!placementOK) cc--; 
                } while (!placementOK && cc >= enemySideStartCol);
                cc++; // Adjust back to valid spot

                // If a valid spot was found
                if (map.placeObject(r, cc, ArenaObjectType.ENTITY)) {
                     // Note: Ideally, checkObject should be separate from placeObject 
                     // to avoid side effects in Strategy, but following original logic:
                    return new AIAction(chosenCard, r, cc);
                }
            }
        }
        return null; // No valid move found after attempts
    }
}