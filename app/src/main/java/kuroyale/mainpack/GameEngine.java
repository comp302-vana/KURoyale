package kuroyale.mainpack;

import java.io.File;
import java.io.IOException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;
import kuroyale.arenapack.SpriteLoader;

public class GameEngine {
    @FXML
    private GridPane arenaGrid;

    private ArenaMap arenaMap = new ArenaMap();

    private final int rows = arenaMap.getRows();
    private final int cols = arenaMap.getCols();
    private final int tileSize = 32;

    public static void main(String[] args) {
        UIManager.launch(UIManager.class, args);
    }
    
    @FXML
    private void initialize() {
        // Make draggable palette items

        //makeDraggable(ourtower, "ourtower");

        fillArenaGrid();
        loadDefaultArenaIfExists();
    }

    /** ARENA LOGIC **/
    private void fillArenaGrid() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {

                Pane tile = new Pane();
                tile.setPrefWidth(32);
                tile.setPrefHeight(32);

                if (col >= 0 && col < cols/2 - 1) {
                    tile.setStyle("-fx-background-color: #4CAF50; -fx-border-color: #9CCC65; -fx-border-width: 0.5;");
                } else if (col >= cols/2 - 1 && col <= cols/2) {
                    tile.setStyle("-fx-background-color: #42A5F5; -fx-border-color: #64B5F6; -fx-border-width: 0.5;");
                } else {
                    tile.setStyle("-fx-background-color: #4CAF50; -fx-border-color: #9CCC65; -fx-border-width: 0.5;");
                }

                int r = row;
                int c = col;

                tile.setOnDragOver(event -> {   // what to do when the draggable is hovering over the tile. required for tile to accept droppage of draggable.
                    if (event.getDragboard().hasString())
                        event.acceptTransferModes(TransferMode.COPY);

                    event.consume();
                });

                tile.setOnDragDropped(event -> {    // waht to do when draggable is dropped on tile.
                    var db = event.getDragboard();
                    boolean success = false;

                    if (db.hasString()) {
                        String typeStr = db.getString();
                    }
                    
                    event.setDropCompleted(success);
                    event.consume();
                });

                arenaGrid.add(tile, col, row);
            }
        }
    }

    private Pane getTile(int row, int col) {
        for (Node n : arenaGrid.getChildren()) {

            Integer r = GridPane.getRowIndex(n);
            Integer c = GridPane.getColumnIndex(n);

            // default null → 0
            int rr = (r == null ? 0 : r);
            int cc = (c == null ? 0 : c);

            if (rr == row && cc == col && n instanceof Pane)
                return (Pane) n;
        }
        return null;
    }

    private void redrawArena() {
        // clear grid UI
        for (Node n : arenaGrid.getChildren()) {
            if (n instanceof Pane tile) {
                tile.getChildren().clear(); // remove sprites
            }
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                var obj = arenaMap.getObject(r, c);
                if (obj == null)
                    continue;

                final int rr = r;
                final int cc = c;

                Pane tile = getTile(rr, cc);
                ImageView sprite = SpriteLoader.getSprite(obj.getType(), tileSize);
                if (sprite == null)
                    continue;

                tile.getChildren().add(sprite);

                Platform.runLater(() -> {
                    sprite.applyCss();
                    sprite.autosize();

                    double tileW = tile.getWidth();
                    double spriteW = sprite.getBoundsInParent().getWidth();
                    sprite.setTranslateX(tileW - spriteW);

                    double tileH = tile.getHeight();
                    double spriteH = sprite.getBoundsInParent().getHeight();
                    sprite.setTranslateY(tileH - spriteH);
                });
            }
        }

    }
    
    private void loadDefaultArenaIfExists() {
        try {
            File f = new File("saves/default.txt");
            if (!f.exists()) {
                switchToStartBattleScene(null);
                return; // no default set
            }

            String fileName = new String(java.nio.file.Files.readAllBytes(f.toPath())).trim();
            if (fileName.isEmpty()) {
                switchToStartBattleScene(null);
                return;
            }

            File arenaFile = new File("saves/" + fileName);
            if (!arenaFile.exists()) {
                System.out.println("Default file missing: " + fileName);
                switchToStartBattleScene(null);
                return;
            }

            // Load arena contents
            arenaMap.loadFromFile(arenaFile.getAbsolutePath());

            // Redraw sprites
            redrawArena();
            System.out.println("Loaded default arena: " + fileName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** SCENE LOGIC **/
    @FXML
    private void btnBackClicked(ActionEvent event) throws IOException {
        switchToStartBattleScene(event);
    }

    private void switchToStartBattleScene(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/StartBattleScene.fxml"));
        root.setStyle("-fx-background-color: BD7FFF;");
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, Color.web("0xBD7FFF"));
        stage.setScene(scene);
        stage.show();
    }
}
