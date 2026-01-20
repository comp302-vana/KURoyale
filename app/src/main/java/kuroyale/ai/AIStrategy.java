package kuroyale.ai;

import java.util.List;
import kuroyale.arenapack.ArenaMap;
import kuroyale.cardpack.Card;

public interface AIStrategy {
    /**
     * Decides which card to play and where.
     * @param hand The current cards available to the AI.
     * @param currentElixir The AI's current resources.
     * @param map The arena map for collision checking.
     * @return An AIAction containing the move, or null if no move is possible/chosen.
     */
    AIAction decideMove(List<Card> hand, double currentElixir, ArenaMap map, int rows, int cols);
}
