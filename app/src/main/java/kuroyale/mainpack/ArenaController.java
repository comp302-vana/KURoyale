package kuroyale.mainpack;

import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

public class ArenaController {

    @FXML
    private GridPane arenaGrid;

    @FXML
    private void initialize() {
        fillArenaGrid();
    }

    private void fillArenaGrid() {
        for (int row = 0; row < 18; row++) {
            for (int col = 0; col < 10; col++) {

                Pane tile = new Pane();
                tile.setStyle("-fx-background-color: yellow; -fx-border-color: black;");

                int r = row;
                int c = col;

                tile.setOnMouseClicked(e ->
                    System.out.println("Clicked tile: " + r + ", " + c)
                );

                arenaGrid.add(tile, col, row);
            }
        }
    }
}
