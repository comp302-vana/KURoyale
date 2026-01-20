package kuroyale.ai;

import kuroyale.cardpack.Card;

public class AIAction {
    private final Card card;
    private final int targetRow;
    private final int targetCol;

    public AIAction(Card card, int row, int col) {
        this.card = card;
        this.targetRow = row;
        this.targetCol = col;
    }

    public Card getCard() {
        return card;
    } public int getRow() {
        return targetRow;
    } public int getCol() {
        return targetCol;
    }
}
