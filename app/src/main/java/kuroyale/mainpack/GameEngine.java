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
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;
import kuroyale.arenapack.SpriteLoader;

import kuroyale.deckpack.Deck;
import kuroyale.cardpack.CardFactory;
import kuroyale.cardpack.CardType;
import kuroyale.cardpack.Card;
import kuroyale.cardpack.subclasses.UnitCard;
import kuroyale.cardpack.subclasses.BuildingCard;
import kuroyale.cardpack.subclasses.SpellCard;

import kuroyale.entitiypack.Entity;
import kuroyale.entitiypack.subclasses.UnitEntity;
import kuroyale.entitiypack.subclasses.BuildingEntity;
import kuroyale.entitiypack.subclasses.SpellEntity;

public class GameEngine {
    @FXML
    private GridPane arenaGrid;
    @FXML
    private AnchorPane cardSlot0;
    @FXML
    private AnchorPane cardSlot1;
    @FXML
    private AnchorPane cardSlot2;
    @FXML
    private AnchorPane cardSlot3;

    private ArenaMap arenaMap = new ArenaMap();

    private final int rows = arenaMap.getRows();
    private final int cols = arenaMap.getCols();
    private final int tileSize = 32;


    public static void main(String[] args) {
        UIManager.launch(UIManager.class, args);
    }
    
    @FXML
    private void initialize() {
        clipImage(getImageFromPane(cardSlot0), 6);
        clipImage(getImageFromPane(cardSlot1), 6);
        clipImage(getImageFromPane(cardSlot2), 6);
        clipImage(getImageFromPane(cardSlot3), 6);
        // Make draggable palette items

        makeDraggable(cardSlot0, "1");

        fillArenaGrid();
        loadDefaultArenaIfExists();
    }

    /** ARENA LOGIC **/
    private void fillArenaGrid() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {

                Pane tile = new Pane();
                tile.setPrefWidth(tileSize);
                tile.setPrefHeight(tileSize);

                if (col >= cols/2 - 1 && col <= cols/2) {
                    tile.setStyle(
                        "-fx-background-image: url('/kuroyale/images/water.jpg');" +
                        "-fx-background-size: cover;"
                    );
                } else {
                    if ((col + row) % 2 == 0) {
                        tile.setStyle(
                            "-fx-background-image: url('/kuroyale/images/darkGrass.jpg');" +
                            "-fx-background-size: cover;"
                        );
                    } else {
                        tile.setStyle(
                            "-fx-background-image: url('/kuroyale/images/lightGrass.jpg');" +
                            "-fx-background-size: cover;"
                        );
                    }
                }

                int r = row;
                int c = col;

                tile.setOnDragOver(event -> {   // what to do when the draggable is hovering over the tile. required for tile to accept droppage of draggable.
                    if (event.getDragboard().hasString())
                        event.acceptTransferModes(TransferMode.COPY);

                    event.consume();
                });

                tile.setOnDragDropped(event -> {    // what to do when draggable is dropped on tile.
                    var db = event.getDragboard();
                    boolean success = false;

                    if (db.hasString()) {
                        String typeStr = db.getString();
                        int cardID = Integer.parseInt(typeStr);
                        
                        Entity playedEntity;
                        if (cardID <= 15) {
                            playedEntity = new UnitEntity(((UnitCard) CardFactory.createCard(cardID)), true);
                        } else if (cardID <= 24) {
                            playedEntity = new BuildingEntity(((BuildingCard) CardFactory.createCard(cardID)), true);
                        } else if (cardID <= 28) {
                            playedEntity = new SpellEntity(((SpellCard) CardFactory.createCard(cardID)), true);
                        } else {
                            playedEntity = new UnitEntity((UnitCard) CardFactory.createCard(1), true);
                            ((UnitEntity) playedEntity).reduceHP(9999.9);
                        }
                        System.out.println(playedEntity.getCard().getName());

                        boolean placementOK;
                        int cc = c;
                        do {
                            placementOK = arenaMap.placeObject(r, cc, ArenaObjectType.ENTITY);
                            cc--;
                        } while(!placementOK && cc >= 0);
                        cc++;
                        if (placementOK) {
                            System.out.println("yey");
                            System.out.printf("(%d, %d)\n", r, cc);
                        } else {
                            System.out.println("no");
                        }
                    }
                    
                    event.setDropCompleted(success);
                    event.consume();
                });

                arenaGrid.add(tile, col, row);
            }
        }
    }

    private void makeDraggable(Pane source, String type) {
        source.setOnDragDetected(event -> {
            var db = source.startDragAndDrop(TransferMode.COPY);

            ClipboardContent content = new ClipboardContent();
            content.putString(type);

            db.setContent(content);
            event.consume();
        });
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

    private ImageView getImageFromPane(AnchorPane ap) {
        for (Node n : ap.getChildren()) {
            if (n instanceof Pane p) {
                return (ImageView) p.getChildren().get(0);
            }
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
    private void clipImage(ImageView img, int arch) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(2 * arch);
        clip.setArcHeight(2 * arch);
        clip.widthProperty().bind(img.fitWidthProperty());
        clip.heightProperty().bind(img.fitHeightProperty());
        img.setClip(clip);
    }

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
