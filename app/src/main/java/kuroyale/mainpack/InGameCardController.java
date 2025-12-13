package kuroyale.mainpack;

import java.util.Collections;
import java.util.List;

public class InGameCardController {
    public static <T> void shuffle(List<T> deck){
        Collections.shuffle(deck);
    }
    public static <T> void moveCardToBottom(List<T> deck,int index) {
        T card = deck.remove(index);
        deck.add(card);
    }

}
