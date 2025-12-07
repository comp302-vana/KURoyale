package kuroyale.mainpack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;
import kuroyale.arenapack.SpriteLoader;

import kuroyale.deckpack.Deck;
import kuroyale.deckpack.DeckManager;
import kuroyale.cardpack.CardFactory;
import kuroyale.cardpack.CardType;
import kuroyale.cardpack.Card;
import kuroyale.cardpack.subclasses.UnitCard;
import kuroyale.cardpack.subclasses.BuildingCard;
import kuroyale.cardpack.subclasses.SpellCard;

import kuroyale.entitiypack.Entity;
import kuroyale.entitiypack.subclasses.AliveEntity;
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
    @FXML
    private Label gameTimerLabel;
    @FXML
    private Label elixirCountLabel;
    @FXML
    private ProgressBar elixirProgressBar;
    
    private ArenaMap arenaMap = new ArenaMap();

    private final int rows = arenaMap.getRows();
    private final int cols = arenaMap.getCols();
    private final int tileSize = 32;

    private Timeline gameLoop;
    private int totalSeconds = 180;
    private double timePassedSinceLastSecond = 0;

    private double currentElixir = 5.0;
    private final double MAX_ELIXIR = 10;
    private final double ELIXIR_REGEN_RATE = 1.0 / 2.8;
    private final double DOUBLE_ELIXIR_REGEN_RATE = 1.0 / 1.4;

    private List<Card> currentDeckCards = new ArrayList<>();
    private final int CARD_SLOT_COUNT = 4;

    public static void main(String[] args) {
        UIManager.launch(UIManager.class, args);
    }
    
    @FXML
    private void initialize() {
        clipImage(getImageFromPane(cardSlot0), 6);
        clipImage(getImageFromPane(cardSlot1), 6);
        clipImage(getImageFromPane(cardSlot2), 6);
        clipImage(getImageFromPane(cardSlot3), 6);

        Deck currentDeck = DeckManager.getCurrentDeck();
        if (currentDeck != null) {
            currentDeckCards = currentDeck.getCards();
            loadDeckToSlots(); 
        } else {
            System.err.println("No active deck.");
        }

        fillArenaGrid();
        loadDefaultArenaIfExists();

        startTimer();
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

                        Card cardToCheck = CardFactory.createCard(cardID);
                        int cost = cardToCheck.getCost();

                        if (currentElixir < cost) {
                            System.out.println("Not enough:" + cost);
                            event.setDropCompleted(false);
                            event.consume();
                            return; 
                        }
                        
                        AliveEntity playedEntity;
                        if (cardID <= 15) {
                            playedEntity = new UnitEntity(((UnitCard) CardFactory.createCard(cardID)), true);
                        } else if (cardID <= 24) {
                            playedEntity = new BuildingEntity(((BuildingCard) CardFactory.createCard(cardID)), true);
                        }  else {
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
                            currentElixir -= cost;
                            updateElixirUI();

                            arenaMap.setEntity(r, cc, playedEntity); 
                                                        
                            drawArena();
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

    private Label getLabelFromPane(AnchorPane ap) {
        for (Node n : ap.getChildren()) {
            if (n instanceof Pane p) {
                return (Label) p.getChildren().get(2);
            }
        }
        return null;
    }

    private ImageView getEntitySpriteFromCard(Card card) {
        if (card == null) return null;
        
        String cardName = card.getName().toLowerCase().replaceAll(" ", "");
        
        String imagePath = "/kuroyale/images/cards/arena/" + cardName + ".png"; 
        
        ImageView img = new ImageView(imagePath);
        img.setFitWidth(tileSize);
        img.setFitHeight(tileSize);
        
        return img;
    }

    private void redrawArena() {
        // clear grid UI
        for (Node n : arenaGrid.getChildren()) {
            if (n instanceof Pane tile) {
                tile.getChildren().clear(); // remove sprites
            }
        }

        ImageView sprite = new ImageView("/kuroyale/images/icon.png");
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                var obj = arenaMap.getObject(r, c);
                if (obj == null)
                    continue;

                final int rr = r;
                final int cc = c;

                Pane tile = getTile(rr, cc);

                if (obj.getType() == ArenaObjectType.ENTITY) {
                    Entity entity = arenaMap.getEntity(rr, cc);
                    if (entity != null) {
                        sprite = getEntitySpriteFromCard(entity.getCard()); 
                    }
                } else {
                    sprite = SpriteLoader.getSprite(obj.getType(), tileSize);
                }
                
                final ImageView finalSprite = sprite;

                if (finalSprite == null)
                    continue;

                tile.getChildren().add(sprite);

                Platform.runLater(() -> {
                    finalSprite.applyCss();
                    finalSprite.autosize();

                    double tileW = tile.getWidth();
                    double spriteW = finalSprite.getBoundsInParent().getWidth();
                    finalSprite.setTranslateX(tileW - spriteW);

                    double tileH = tile.getHeight();
                    double spriteH = finalSprite.getBoundsInParent().getHeight();
                    finalSprite.setTranslateY(tileH - spriteH);
                });
            }
        }

    }
    
    private void drawArena() {
        ImageView sprite = new ImageView("/kuroyale/images/icon.png");
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                var obj = arenaMap.getObject(r, c);
                if (obj == null)
                    continue;

                final int rr = r;
                final int cc = c;

                Pane tile = getTile(rr, cc);

                if (obj.getType() == ArenaObjectType.ENTITY) {
                    Entity entity = arenaMap.getEntity(rr, cc);
                    if (entity != null) {
                        sprite = getEntitySpriteFromCard(entity.getCard()); 
                    }
                } else {
                    sprite = SpriteLoader.getSprite(obj.getType(), tileSize);
                }
                

                final ImageView finalSprite = sprite;

                if (finalSprite == null)
                    continue;

                tile.getChildren().add(sprite);

                Platform.runLater(() -> {
                    finalSprite.applyCss();
                    finalSprite.autosize();

                    double tileW = tile.getWidth();
                    double spriteW = finalSprite.getBoundsInParent().getWidth();
                    finalSprite.setTranslateX(tileW - spriteW);

                    double tileH = tile.getHeight();
                    double spriteH = finalSprite.getBoundsInParent().getHeight();
                    finalSprite.setTranslateY(tileH - spriteH);
                });
            }
        }
    }
    private void loadDefaultArenaIfExists() {
        try {
            File f = new File("saves/default.txt");
            if (!f.exists()) {

                return; // no default set
            }

            String fileName = new String(java.nio.file.Files.readAllBytes(f.toPath())).trim();
            if (fileName.isEmpty()) {

                return;
            }

            File arenaFile = new File("saves/" + fileName);
            if (!arenaFile.exists()) {
                System.out.println("Default file missing: " + fileName);
 
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

    /** GAMEPLAY **/
    
    private void startTimer()  {
        final double TICK_DURATION = 0.1; 
        
        gameLoop = new Timeline(new KeyFrame(Duration.seconds(TICK_DURATION), e -> {
            if (currentElixir < MAX_ELIXIR) {
                if (totalSeconds >= 60) {
                    currentElixir += (ELIXIR_REGEN_RATE * TICK_DURATION);
                    if (currentElixir > MAX_ELIXIR) currentElixir = MAX_ELIXIR;
                } else {
                    currentElixir += (DOUBLE_ELIXIR_REGEN_RATE * TICK_DURATION);
                    if (currentElixir > MAX_ELIXIR) currentElixir = MAX_ELIXIR;
                }
                
            }
            updateElixirUI();
            
            timePassedSinceLastSecond += TICK_DURATION;
            if (timePassedSinceLastSecond >= 1.0) {
                if (totalSeconds > 0) {
                    totalSeconds--; 
                    updateTimerLabel();
                } else {
                    gameLoop.stop();
                    // endGame();
                    gameTimerLabel.setText("00:00");
                }
                timePassedSinceLastSecond = 0;
            }
        }));

        gameLoop.setCycleCount(Timeline.INDEFINITE);
        gameLoop.play();
    }
    
    private void loadDeckToSlots() {
    AnchorPane[] cardSlots = {cardSlot0, cardSlot1, cardSlot2, cardSlot3};
    
    for (int i = 0; i < CARD_SLOT_COUNT; i++) {
        if (i < currentDeckCards.size()) {
            Card card = currentDeckCards.get(i);
            AnchorPane slotPane = cardSlots[i];
            
            ImageView cardImage = getImageFromPane(slotPane);
            if (cardImage != null) {
                String cardName = card.getName().toLowerCase().replaceAll(" ", "");
                String imagePath = "/kuroyale/images/cards/" + cardName + ".png";
                cardImage.setImage(new javafx.scene.image.Image(imagePath));
            }
            
            Label costLabel = getLabelFromPane(slotPane);
            if (costLabel != null) {
                costLabel.setText(String.valueOf(card.getCost()));
            }

            makeDraggable(slotPane, String.valueOf(card.getId()));
            
        } else {

        }
    }
}
    @FXML
    private void updateElixirUI() {
        if (elixirProgressBar != null) {
            elixirProgressBar.setProgress(currentElixir / MAX_ELIXIR);
        }
        if (elixirCountLabel != null) {
            elixirCountLabel.setText(String.format("%d", (int) Math.floor(currentElixir)));
        }
    }

    @FXML
    private void updateTimerLabel() {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60; 
    
        String timeText = String.format("%02d:%02d", minutes, seconds);
    
        gameTimerLabel.setText(timeText);
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
