package kuroyale.mainpack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.Interpolator;

import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;
import kuroyale.arenapack.PlacedObject;
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
    private Label card1CostLabel;
    @FXML
    private Label card2CostLabel;
    @FXML
    private Label card3CostLabel;
    @FXML
    private Label card4CostLabel;
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
    private double timePassedSinceLastEntityUpdate = 0;
    private final double ENTITY_UPDATE_INTERVAL = 0.1; // Update entities every 0.1 seconds

    private double currentElixir = 5.0;
    private final double MAX_ELIXIR = 10;
    private final double ELIXIR_REGEN_RATE = 1.0 / 2.8;
    private final double DOUBLE_ELIXIR_REGEN_RATE = 1.0 / 1.4;

    private List<Card> currentDeckCards = new ArrayList<>();
    private List<Card> currentHand = new ArrayList<>(); // The 4 cards currently in hand
    private int nextCardIndex = 0; // Index of next card to draw from deck
    private final int CARD_SLOT_COUNT = 4;

    // Track attack cooldowns for each entity (time remaining until next attack)
    private Map<AliveEntity, Double> attackCooldowns = new HashMap<>();

    private SimpleAI aiOpponent;

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
            currentDeckCards = new ArrayList<>(currentDeck.getCards());
            nextCardIndex = 0;
            currentHand.clear();
            loadDeckToSlots();
        } else {
            System.err.println("No active deck.");
        }

        fillArenaGrid();
        loadDefaultArenaIfExists();

        startTimer();

        // Verify all cards are draggable after initialization
        verifyAllCardsDraggable();

        // Initialize the AI opponent
        String difficulty = UIManager.getSelectedDifficulty();
        if ("Simple".equals(difficulty)) {
            aiOpponent = new SimpleAI(arenaMap);
        } else {
            aiOpponent = null; // at least for now
        }
    }

    private void verifyAllCardsDraggable() {
        AnchorPane[] cardSlots = { cardSlot0, cardSlot1, cardSlot2, cardSlot3 };
        System.out.println("=== Verifying all cards are draggable ===");
        for (int i = 0; i < cardSlots.length; i++) {
            Pane innerPane = getInnerPaneFromSlot(cardSlots[i]);
            if (innerPane != null) {
                boolean hasHandler = innerPane.getOnDragDetected() != null;
                System.out.println("Slot " + i + " has drag handler: " + hasHandler);
                if (i < currentHand.size()) {
                    System.out.println(
                            "  Card: " + currentHand.get(i).getName() + " (ID: " + currentHand.get(i).getId() + ")");
                }
            } else {
                System.out.println("Slot " + i + ": Inner pane not found!");
            }
        }
        System.out.println("=== End verification ===");
    }

    /** ARENA LOGIC **/
    private void fillArenaGrid() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {

                Pane tile = new Pane();
                tile.setPrefWidth(tileSize);
                tile.setPrefHeight(tileSize);

                if (col >= cols / 2 - 1 && col <= cols / 2) {
                    tile.setStyle(
                            "-fx-background-image: url('/kuroyale/images/water.jpg');" +
                                    "-fx-background-size: cover;");
                } else {
                    if ((col + row) % 2 == 0) {
                        tile.setStyle(
                                "-fx-background-image: url('/kuroyale/images/darkGrass.jpg');" +
                                        "-fx-background-size: cover;");
                    } else {
                        tile.setStyle(
                                "-fx-background-image: url('/kuroyale/images/lightGrass.jpg');" +
                                        "-fx-background-size: cover;");
                    }
                }

                int r = row;
                int c = col;

                tile.setOnDragOver(event -> { // what to do when the draggable is hovering over the tile. required for
                                              // tile to accept droppage of draggable.
                    if (event.getDragboard().hasString())
                        event.acceptTransferModes(TransferMode.COPY);

                    event.consume();
                });

                tile.setOnDragDropped(event -> { // what to do when draggable is dropped on tile.
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

                        // Spawn restriction: don't allow initial placement on enemy side or bridge
                        if (c >= arenaMap.getCols() / 2 - 1) {
                            System.out.println("Cannot place troops on enemy side or bridge.");
                            event.setDropCompleted(false);
                            event.consume();
                            return;
                        }

                        AliveEntity playedEntity;
                        if (cardID <= 15) {
                            playedEntity = new UnitEntity(((UnitCard) CardFactory.createCard(cardID)), true);
                        } else if (cardID <= 24) {
                            playedEntity = new BuildingEntity(((BuildingCard) CardFactory.createCard(cardID)), true);
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
                        } while (!placementOK && cc >= 0);
                        cc++;
                        if (placementOK) {
                            currentElixir -= cost;
                            updateElixirUI();

                            // Set entity position
                            playedEntity.setPosition(r, cc);
                            arenaMap.setEntity(r, cc, playedEntity);

                            // Redraw arena to show the new entity
                            redrawArena();

                            // Find which slot the card was in and cycle it
                            int slotIndex = findCardSlotIndex(cardID);
                            System.out.println("Card played: " + cardID + " at slot: " + slotIndex);
                            if (slotIndex >= 0) {
                                cycleCardInSlot(slotIndex);
                            }

                            System.out.println("yey");
                            System.out.printf("(%d, %d)\n", r, cc);
                            success = true;
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
        if (source == null) {
            System.err.println("Cannot make null pane draggable with type: " + type);
            return;
        }
        source.setOnDragDetected(event -> {
            var db = source.startDragAndDrop(TransferMode.COPY);

            ClipboardContent content = new ClipboardContent();
            content.putString(type);

            db.setContent(content);
            System.out.println("Drag started for card ID: " + type);
            event.consume();
        });
        System.out.println("Made pane draggable with card ID: " + type);
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

    private Pane getInnerPaneFromSlot(AnchorPane ap) {
        for (Node n : ap.getChildren()) {
            if (n instanceof Pane p) {
                return p;
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

    private javafx.scene.layout.Pane createHealthBar(double currentHP, double maxHP, boolean isTower) {
        // Create a simple health bar using rectangles
        int barHeight = isTower ? 8 : 4; // Bigger for towers
        int barWidth = isTower ? (int) (tileSize * 1.8) : tileSize; // Much wider for towers

        javafx.scene.layout.Pane healthBarContainer = new javafx.scene.layout.Pane();
        healthBarContainer.setPrefWidth(barWidth);
        healthBarContainer.setPrefHeight(barHeight);

        // Background (red/dark)
        Rectangle bg = new Rectangle(barWidth, barHeight);
        bg.setFill(Color.DARKRED);
        bg.setStroke(Color.BLACK);
        bg.setStrokeWidth(0.5);

        // Health (green)
        double healthPercent = Math.max(0, Math.min(1, currentHP / maxHP));
        Rectangle health = new Rectangle(barWidth * healthPercent, barHeight);
        health.setFill(healthPercent > 0.5 ? Color.LIMEGREEN : (healthPercent > 0.25 ? Color.YELLOW : Color.RED));

        healthBarContainer.getChildren().addAll(bg, health);
        return healthBarContainer;
    }

    private ImageView getEntitySpriteFromCard(Card card) {
        if (card == null)
            return null;

        String cardName = card.getName().toLowerCase().replaceAll(" ", "");
        // System.out.println("Loading sprite for card: " + card.getName() + "
        // (normalized: " + cardName + ")");

        // Try arena sprite first
        String arenaImagePath = "/kuroyale/images/cards/arena/" + cardName + ".png";
        String regularImagePath = "/kuroyale/images/cards/" + cardName + ".png";

        // Don't create ImageView with any default image - start empty
        ImageView img = new ImageView();
        // Make units bigger - use 24px (75% of tile size)
        int spriteSize = 24;
        img.setFitWidth(spriteSize);
        img.setFitHeight(spriteSize);
        img.setPreserveRatio(true);
        img.setSmooth(true);
        img.setImage(null);

        // Check if resource exists using getResourceAsStream
        java.io.InputStream arenaStream = getClass().getResourceAsStream(arenaImagePath);
        final boolean arenaExists = (arenaStream != null);
        if (arenaStream != null) {
            try {
                arenaStream.close();
            } catch (Exception e) {
            }
        }

        java.io.InputStream regularStream = getClass().getResourceAsStream(regularImagePath);
        final boolean regularExists = (regularStream != null);
        if (regularStream != null) {
            try {
                regularStream.close();
            } catch (Exception e) {
            }
        }

        String imagePath;
        final boolean useArena;

        if (arenaExists) {
            imagePath = arenaImagePath;
            useArena = true;
        } else if (regularExists) {
            imagePath = regularImagePath;
            useArena = false;
        } else {
            System.err.println("No image found for card: " + cardName);
            return null;
        }

        final String finalImagePath = imagePath;
        final String finalRegularPath = regularImagePath;

        // Load image using URL
        java.net.URL imageURL = getClass().getResource(finalImagePath);
        final String actualImagePath;

        if (imageURL == null && useArena && regularExists) {
            imageURL = getClass().getResource(finalRegularPath);
            actualImagePath = finalRegularPath;
        } else {
            actualImagePath = finalImagePath;
        }

        if (imageURL == null)
            return null;

        javafx.scene.image.Image image = new javafx.scene.image.Image(imageURL.toExternalForm());

        // Async checks for image loading
        if (image.getWidth() > 0 && !image.isError()) {
            img.setImage(image);
            return img;
        }

        Platform.runLater(() -> {
            if (image.getWidth() > 0 && !image.isError()) {
                img.setImage(image);
            }
        });

        image.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 1.0 && !image.isError()) {
                Platform.runLater(() -> {
                    if (!img.getImage().isError())
                        img.setImage(image);
                });
            }
        });

        return img;
    }

    private void redrawArena() {
        // Iterate through all tiles in the grid
        for (Node n : arenaGrid.getChildren()) {
            if (!(n instanceof Pane))
                continue;
            Pane tile = (Pane) n;

            // Get row and col efficiently
            Integer r = GridPane.getRowIndex(n);
            Integer c = GridPane.getColumnIndex(n);
            if (r == null || c == null)
                continue;
            int row = r;
            int col = c;

            // 1. STATIC OBJECTS CHECK (Towers, Kings, Bridges)
            PlacedObject obj = arenaMap.getObject(row, col);
            boolean isStatic = (obj != null && obj.getType() != ArenaObjectType.ENTITY);

            if (isStatic) {
                // Check if sprite exists
                boolean hasSprite = false;
                for (Node child : tile.getChildren()) {
                    if (child instanceof ImageView) {
                        // Static sprites don't have user data usually.
                        // But dynamic sprites have Entity user data.
                        if (child.getUserData() == null) {
                            hasSprite = true;
                            break;
                        }
                    }
                }

                if (!hasSprite) {
                    // Load static sprite
                    ImageView staticSprite = SpriteLoader.getSprite(obj.getType(), tileSize);
                    if (staticSprite != null) {
                        tile.getChildren().add(0, staticSprite); // Add at bottom

                        // Center/Resize logic
                        Platform.runLater(() -> {
                            staticSprite.applyCss();
                            double tileW = tile.getWidth() > 0 ? tile.getWidth() : tileSize;
                            double spriteW = staticSprite.getBoundsInParent().getWidth();
                            // Try to center or align based on original logic logic
                            double tileH = tile.getHeight() > 0 ? tile.getHeight() : tileSize;
                            double spriteH = staticSprite.getBoundsInParent().getHeight();

                            if (spriteW > 0)
                                staticSprite.setTranslateX(tileW - spriteW);
                            if (spriteH > 0)
                                staticSprite.setTranslateY(tileH - spriteH);
                        });
                    }
                }

                // Check if we need to update health bar for King/Tower
                Entity entity = arenaMap.getEntity(row, col);
                if (entity instanceof AliveEntity aliveEntity) {
                    updateHealthBar(tile, aliveEntity, true);
                }
            }

            // 2. DYNAMIC ENTITY RECONCILIATION
            AliveEntity expectedEntity = arenaMap.getEntity(row, col);

            // Skip towers here (already handled above if they are map objects)
            // Logic: if it's a tower entity, it should have been caught by isStatic check
            // if it corresponds to a PlacedObject.
            // But sometimes Entity grid has it?
            if (expectedEntity instanceof kuroyale.entitiypack.subclasses.TowerEntity) {
                // Double check if it was handled. If not static obj, maybe dynamic? (Not in
                // this game design usually)
                updateHealthBar(tile, expectedEntity, true);
                continue;
            }

            // Find current dynamic sprite on this tile
            ImageView currentSprite = null;
            javafx.scene.layout.Pane currentHealthBar = null;

            // Scan children to identify current state
            // We assume dynamic sprites have userData set to the Entity object.
            java.util.List<Node> toRemove = new java.util.ArrayList<>();

            for (Node child : tile.getChildren()) {
                if (child instanceof ImageView iv) {
                    Object userData = iv.getUserData();
                    if (userData instanceof Entity) {
                        // This is a dynamic sprite
                        if (userData == expectedEntity) {
                            currentSprite = iv;
                        } else {
                            // Wrong entity (dead or moved), remove it
                            toRemove.add(iv);
                        }
                    }
                } else if (child instanceof javafx.scene.layout.Pane p && "healthBar".equals(p.getUserData())) {
                    // Check if this health bar belongs to the current sprite?
                    // Actually we'll just remove health bars if we remove the sprite, or find a way
                    // to link them.
                    // Simplified: Re-create health bar or update it.
                    // To be safe: if we found a match Sprite, let's look for its health bar.
                    // But easier to just store health bar reference in the sprite's userData? No.
                    // Let's rely on updateHealthBar to manage the health bar presence.
                    // For now, if expectedEntity is null, remove all health bars
                    if (expectedEntity == null) {
                        toRemove.add(p);
                    }
                }
            }

            // Execute removals
            tile.getChildren().removeAll(toRemove);

            // Reconciliation
            if (expectedEntity != null) {
                // CASE 1: New Entity (Insert)
                if (currentSprite == null) {
                    ImageView newSprite = getEntitySpriteFromCard(expectedEntity.getCard());
                    if (newSprite != null) {
                        newSprite.setUserData(expectedEntity); // CRITICAL: Tag for identity

                        // Center sprite
                        Platform.runLater(() -> {
                            // Initial positioning (might need adjust after image load)
                            double tileW = tile.getWidth() > 0 ? tile.getWidth() : tileSize;
                            double tileH = tile.getHeight() > 0 ? tile.getHeight() : tileSize;
                            newSprite.setFitWidth(24);
                            newSprite.setFitHeight(24);
                            newSprite.setTranslateX((tileW - 24) / 2);
                            newSprite.setTranslateY((tileH - 24) / 2);
                        });

                        tile.getChildren().add(newSprite);
                    }
                } else {
                    // CASE 2: Existing Entity (Update)
                    // Nothing to do for sprite (image is same)
                }

                // Update Health Bar (Always update to animate changes)
                updateHealthBar(tile, expectedEntity, false);
            }
        }

    }

    private void updateHealthBar(Pane tile, AliveEntity entity, boolean isTower) {
        if (entity == null || entity.getHP() <= 0) {
            // Remove health bar if exists
            tile.getChildren().removeIf(n -> "healthBar".equals(n.getUserData()));
            return;
        }

        Card card = entity.getCard();
        double maxHP = (card instanceof kuroyale.cardpack.subclasses.AliveCard ac) ? ac.getHp() : entity.getHP();
        double currentHP = entity.getHP();
        double pct = Math.max(0, Math.min(1, currentHP / maxHP));

        // Find existing health bar
        javafx.scene.layout.Pane bar = null;
        for (Node n : tile.getChildren()) {
            if ("healthBar".equals(n.getUserData())) {
                bar = (javafx.scene.layout.Pane) n;
                break;
            }
        }

        if (bar == null) {
            // Create new bar
            bar = createHealthBar(currentHP, maxHP, isTower);
            bar.setUserData("healthBar");
            bar.setVisible(true);

            // Position it
            if (isTower) {
                double barH = bar.getPrefHeight();
                double barW = bar.getPrefWidth(); // 1.8 * tileSize
                bar.setTranslateY((tileSize - barH) / 2);
                bar.setTranslateX((tileSize - barW) / 2);
            } else {
                bar.setTranslateY(2);
                bar.setTranslateX(0);
            }
            tile.getChildren().add(bar);
        }

        // Update Width with Animation (Smoothness)
        if (bar.getChildren().size() >= 2 && bar.getChildren().get(1) instanceof Rectangle healthRect) {
            double totalWidth = bar.getPrefWidth();
            double targetWidth = totalWidth * pct; // Target width

            // Color update
            healthRect.setFill(pct > 0.5 ? Color.LIMEGREEN : (pct > 0.25 ? Color.YELLOW : Color.RED));

            // Animate width change (0.2s)
            // Using ScaleTransition is tricky on Rectangle width directly? No, changing
            // width property is better.
            // Timeline for 'width' property.
            // But simpler: just use width if small change? User asked for smooth.

            // Stop previous animation if any? JavaFX doesn't easily expose running
            // animations on a property without reference.
            // But Timeline on a property replaces? No.
            // Simple approach: create a new Timeline.

            // Check if we need to animate (optimization)
            if (Math.abs(healthRect.getWidth() - targetWidth) > 0.1) {
                // Only animate if difference is significant
                javafx.animation.Timeline timeline = new javafx.animation.Timeline();
                javafx.animation.KeyValue kv = new javafx.animation.KeyValue(healthRect.widthProperty(), targetWidth,
                        Interpolator.EASE_OUT);
                javafx.animation.KeyFrame kf = new javafx.animation.KeyFrame(Duration.millis(200), kv);
                timeline.getKeyFrames().add(kf);
                timeline.play();
            }
        }
    }

    private void drawArena() {
        // Use redrawArena instead - this method is deprecated
        redrawArena();
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

    private void startTimer() {
        final double TICK_DURATION = 0.1;

        gameLoop = new Timeline(new KeyFrame(Duration.seconds(TICK_DURATION), e -> {
            if (currentElixir < MAX_ELIXIR) {
                if (totalSeconds >= 60) {
                    currentElixir += (ELIXIR_REGEN_RATE * TICK_DURATION);
                    if (currentElixir > MAX_ELIXIR)
                        currentElixir = MAX_ELIXIR;
                } else {
                    currentElixir += (DOUBLE_ELIXIR_REGEN_RATE * TICK_DURATION);
                    if (currentElixir > MAX_ELIXIR)
                        currentElixir = MAX_ELIXIR;
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

            // Update entities (movement and combat)
            timePassedSinceLastEntityUpdate += TICK_DURATION;
            if (timePassedSinceLastEntityUpdate >= ENTITY_UPDATE_INTERVAL) {
                updateEntities();
                // Update AI opponent
                if (aiOpponent != null) {
                    aiOpponent.update(TICK_DURATION, totalSeconds);
                }
                timePassedSinceLastEntityUpdate = 0;
            }
        }));

        gameLoop.setCycleCount(Timeline.INDEFINITE);
        gameLoop.play();
    }

    private void updateEntities() {
        List<AliveEntity> entitiesToUpdate = new ArrayList<>();
        List<AliveEntity> deadEntities = new ArrayList<>();

        // Collect all entities
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                AliveEntity entity = arenaMap.getEntity(r, c);
                if (entity != null && entity.getHP() > 0) {
                    entitiesToUpdate.add(entity);
                } else if (entity != null && entity.getHP() <= 0) {
                    // Mark for removal
                    deadEntities.add(entity);
                }
            }
        }

        // Remove dead entities first
        for (AliveEntity deadEntity : deadEntities) {
            removeDeadEntity(deadEntity);
        }

        // Update attack cooldowns
        for (AliveEntity entity : new ArrayList<>(attackCooldowns.keySet())) {
            if (entity.getHP() <= 0 || !entitiesToUpdate.contains(entity)) {
                // Remove cooldown for dead or missing entities
                attackCooldowns.remove(entity);
            } else {
                // Decrease cooldown
                double currentCooldown = attackCooldowns.get(entity);
                double newCooldown = Math.max(0, currentCooldown - ENTITY_UPDATE_INTERVAL);
                if (newCooldown > 0) {
                    attackCooldowns.put(entity, newCooldown);
                } else {
                    attackCooldowns.remove(entity);
                }
            }
        }

        // Update each entity
        for (AliveEntity entity : entitiesToUpdate) {
            // Find entity's actual position in map (don't use getRow/getCol from card)
            int entityRow = -1, entityCol = -1;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (arenaMap.getEntity(r, c) == entity) {
                        entityRow = r;
                        entityCol = c;
                        break;
                    }
                }
                if (entityRow >= 0)
                    break;
            }

            if (entityRow < 0 || entityCol < 0 || entity.getHP() <= 0) {
                continue; // Entity not found or dead
            }

            // Update entity's internal position tracking
            entity.setPosition(entityRow, entityCol);

            // Only units can move (not buildings)
            if (entity instanceof UnitEntity) {
                updateUnitEntity((UnitEntity) entity);
            } else if (entity instanceof BuildingEntity
                    && !(entity instanceof kuroyale.entitiypack.subclasses.TowerEntity)) {
                updateBuildingEntity((BuildingEntity) entity);
            } else if (entity instanceof kuroyale.entitiypack.subclasses.TowerEntity) {
                // Towers can also attack
                updateTowerEntity((kuroyale.entitiypack.subclasses.TowerEntity) entity);
            }
        }

        // Redraw arena after updates
        Platform.runLater(() -> redrawArena());
    }

    private void removeDeadEntity(AliveEntity entity) {
        // Check if this is a king tower - if so, end the game
        if (entity instanceof kuroyale.entitiypack.subclasses.TowerEntity tower) {
            if (tower.isKing()) {
                // King died - end the game
                boolean playerWon = !tower.isPlayer(); // If enemy king died, player won
                endGame(playerWon);
                return;
            }
        }

        // Find entity position
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (arenaMap.getEntity(r, c) == entity) {
                    // Remove from entities grid
                    arenaMap.setEntity(r, c, null);
                    // Clear object (but preserve bridges)
                    arenaMap.clearObject(r, c);
                    // Remove from cooldown tracking
                    attackCooldowns.remove(entity);
                    return;
                }
            }
        }
    }

    private void endGame(boolean playerWon) {
        // Stop the game loop
        if (gameLoop != null) {
            gameLoop.stop();
        }

        // Show game end screen
        Platform.runLater(() -> {
            try {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        playerWon ? javafx.scene.control.Alert.AlertType.INFORMATION
                                : javafx.scene.control.Alert.AlertType.WARNING);
                alert.setTitle("Game Over");
                alert.setHeaderText(playerWon ? "Victory!" : "Defeat!");
                alert.setContentText(playerWon ? "You destroyed the enemy king!" : "Your king has been destroyed!");

                // Show and wait
                alert.showAndWait();

                // Switch back to start battle scene
                Parent root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/StartBattleScene.fxml"));
                root.setStyle("-fx-background-color: BD7FFF;");
                Stage stage = (Stage) arenaGrid.getScene().getWindow();
                Scene scene = new Scene(root, 1280, 720, javafx.scene.paint.Color.web("0xBD7FFF"));
                stage.setScene(scene);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateUnitEntity(UnitEntity unit) {
        // Get current position from arena map
        int currentRow = -1, currentCol = -1;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (arenaMap.getEntity(r, c) == unit) {
                    currentRow = r;
                    currentCol = c;
                    break;
                }
            }
            if (currentRow >= 0)
                break;
        }

        if (currentRow < 0 || currentCol < 0) {
            return; // Entity not found in map
        }

        // Update entity's internal position tracking
        unit.setPosition(currentRow, currentCol);

        // Find closest target
        AliveEntity target = unit.findClosestTarget(arenaMap);

        if (target == null) {
            return; // No target found
        }

        // --- find target position on the entity grid ---
        int targetRow = -1, targetCol = -1;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (arenaMap.getEntity(r, c) == target) {
                    targetRow = r;
                    targetCol = c;
                    break;
                }
            }
            if (targetRow >= 0)
                break;
        }

        if (targetRow < 0 || targetCol < 0) {
            return; // target vanished from map
        }

        int dRow = Math.abs(targetRow - currentRow);
        int dCol = Math.abs(targetCol - currentCol);

        // default: Manhattan distance
        double distance = dRow + dCol;

        // Make melee detection robust - treat ≤1 tile as melee
        double unitRange = unit.getRange();
        boolean isMelee = unitRange <= 1.0;
        if (isMelee) {
            unitRange = 1.0; // Normalize melee range to 1 tile
        }

        kuroyale.cardpack.subclasses.AliveCard aliveCard = (kuroyale.cardpack.subclasses.AliveCard) unit.getCard();
        double actSpeed = aliveCard.getActSpeed();
        double attackCooldownTime = actSpeed > 0 ? 1.0 / actSpeed : 1.0;

        // --- special case: melee vs tower ---
        // treat "hugging the tower sprite" as in-range if ANY orthogonal neighbour tile
        // contains the target tower entity (towers occupy multiple cells)
        boolean adjacentEnemyTowerObject = false;
        if (isMelee && target instanceof kuroyale.entitiypack.subclasses.TowerEntity) {
            int[][] orthoDirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
            for (int[] d : orthoDirs) {
                int rr = currentRow + d[0];
                int cc = currentCol + d[1];
                if (rr < 0 || rr >= rows || cc < 0 || cc >= cols)
                    continue;

                // Check if the target tower entity is in this neighbor cell
                // (towers occupy multiple cells, so check entity grid directly)
                AliveEntity neighborEntity = arenaMap.getEntity(rr, cc);
                if (neighborEntity == target) {
                    adjacentEnemyTowerObject = true;
                    System.out.println("  DEBUG: Found target tower entity at neighbor (" + rr + "," + cc
                            + ") → adjacentEnemyTowerObject = true");
                    break;
                }

                // Also check collision layer for tower objects (backup check)
                var obj = arenaMap.getObject(rr, cc);
                if (obj != null && obj.getType() != null) {
                    switch (obj.getType()) {
                        case ENEMY_TOWER, ENEMY_KING -> {
                            if (unit.isPlayer()) {
                                adjacentEnemyTowerObject = true;
                                System.out.println("  DEBUG: Found ENEMY_TOWER/KING object at (" + rr + "," + cc
                                        + ") → adjacentEnemyTowerObject = true");
                                break;
                            }
                        }
                        case OUR_TOWER, OUR_KING -> {
                            if (!unit.isPlayer()) {
                                adjacentEnemyTowerObject = true;
                                System.out.println("  DEBUG: Found OUR_TOWER/KING object at (" + rr + "," + cc
                                        + ") → adjacentEnemyTowerObject = true");
                                break;
                            }
                        }
                        default -> {
                        }
                    }
                }

                if (adjacentEnemyTowerObject)
                    break;
            }
        }

        // Melee: strict 1-tile range, Ranged: allow small fudge factor
        // If melee unit is hugging tower sprite, force inRange = true
        boolean inRange = adjacentEnemyTowerObject || // melee hugging tower sprite
                (isMelee ? (distance <= unitRange) // melee: strict 1-tile range
                        : (distance <= unitRange + 0.5)); // ranged: allow a small fudge

        // Debug output for melee units attacking towers
        if (isMelee && target instanceof kuroyale.entitiypack.subclasses.TowerEntity) {
            System.out.println("Melee unit at (" + currentRow + "," + currentCol + ") - Target: " +
                    (((kuroyale.entitiypack.subclasses.TowerEntity) target).isKing() ? "KING_TOWER" : "TOWER") +
                    " - Distance: " + distance + " - Range: " + unitRange +
                    " - AdjacentTowerObject: " + adjacentEnemyTowerObject + " - InRange: " + inRange);
        }

        if (inRange) {
            double currentCooldown = attackCooldowns.getOrDefault(unit, 0.0);
            if (currentCooldown <= 0) {
                double originalDamage = unit.getDamage();
                double multipliedDamage = originalDamage * 5.0;
                target.reduceHP(multipliedDamage);

                attackCooldowns.put(unit, attackCooldownTime);

                Platform.runLater(() -> redrawArena());

                if (target.getHP() <= 0) {
                    removeDeadEntity(target);
                }
            }
        } else {
            String speedStr = getUnitSpeed(unit);
            double speedMultiplier = getSpeedMultiplier(speedStr);

            if (Math.random() < speedMultiplier * 0.1) {
                int oldRow = currentRow;
                int oldCol = currentCol;

                unit.move(arenaMap);

                int newRow = unit.getRow();
                int newCol = unit.getCol();

                if ((newRow != oldRow || newCol != oldCol) && newRow >= 0 && newRow < rows && newCol >= 0
                        && newCol < cols) {
                    PlacedObject oldObj = arenaMap.getObject(oldRow, oldCol);
                    if (oldObj == null || oldObj.getType() == ArenaObjectType.ENTITY) {
                        arenaMap.clearObject(oldRow, oldCol);
                    }
                    boolean placementSuccess = arenaMap.placeObject(newRow, newCol, ArenaObjectType.ENTITY);

                    if (!placementSuccess) {
                        unit.setPosition(oldRow, oldCol);
                        arenaMap.moveEntitiy(newRow, newCol, oldRow, oldCol);
                        if (oldObj == null || oldObj.getType() == ArenaObjectType.ENTITY) {
                            arenaMap.placeObject(oldRow, oldCol, ArenaObjectType.ENTITY);
                        }
                    }
                } else {
                    AliveEntity entityAtNewPos = arenaMap.getEntity(newRow, newCol);
                    if (entityAtNewPos != unit) {
                        unit.setPosition(oldRow, oldCol);
                        if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                            arenaMap.moveEntitiy(newRow, newCol, oldRow, oldCol);
                        }
                    }
                }
            }
        }
    }

    private void updateBuildingEntity(BuildingEntity building) {
        int currentRow = -1, currentCol = -1;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (arenaMap.getEntity(r, c) == building) {
                    currentRow = r;
                    currentCol = c;
                    break;
                }
            }
            if (currentRow >= 0)
                break;
        }

        if (currentRow < 0 || currentCol < 0) {
            return; // Building not found in map
        }

        building.setPosition(currentRow, currentCol);

        AliveEntity target = building.findClosestTarget(arenaMap);

        if (target == null) {
            // Reduce lifetime for buildings
            building.reduceLifetime(ENTITY_UPDATE_INTERVAL);
            if (building.getHP() <= 0) {
                arenaMap.setEntity(currentRow, currentCol, null);
                arenaMap.clearObject(currentRow, currentCol);
            }
            return;
        }

        // Get target position - for towers, find the minimum distance to any cell they
        // occupy
        double distance;
        if (target instanceof kuroyale.entitiypack.subclasses.TowerEntity towerTarget) {
            // Towers occupy multiple cells - find minimum distance to any occupied cell
            int towerSize = towerTarget.isKing() ? 3 : 2;

            // Find the bottom-right corner where the tower is stored
            int bottomRightRow = -1, bottomRightCol = -1;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (arenaMap.getEntity(r, c) == target) {
                        bottomRightRow = r;
                        bottomRightCol = c;
                        break;
                    }
                }
                if (bottomRightRow >= 0)
                    break;
            }

            if (bottomRightRow < 0 || bottomRightCol < 0) {
                return; // Target not found
            }

            // Calculate minimum distance to any cell the tower occupies
            double minDistance = Double.MAX_VALUE;
            int startRow = bottomRightRow - towerSize + 1;
            int startCol = bottomRightCol - towerSize + 1;

            for (int tr = startRow; tr <= bottomRightRow; tr++) {
                for (int tc = startCol; tc <= bottomRightCol; tc++) {
                    int rowDiff = Math.abs(tr - currentRow);
                    int colDiff = Math.abs(tc - currentCol);
                    double cellDistance = rowDiff + colDiff;
                    if (cellDistance < minDistance) {
                        minDistance = cellDistance;
                    }
                }
            }

            distance = minDistance;
        } else {
            // For non-tower entities, use single cell position
            int targetRow = -1, targetCol = -1;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (arenaMap.getEntity(r, c) == target) {
                        targetRow = r;
                        targetCol = c;
                        break;
                    }
                }
                if (targetRow >= 0)
                    break;
            }

            if (targetRow < 0 || targetCol < 0) {
                return; // Target not found
            }

            int rowDiff = Math.abs(targetRow - currentRow);
            int colDiff = Math.abs(targetCol - currentCol);
            distance = rowDiff + colDiff;
        }

        boolean canAttackTower = false;
        if (target instanceof kuroyale.entitiypack.subclasses.TowerEntity && distance == 1) {
            canAttackTower = true;
        }

        kuroyale.cardpack.subclasses.AliveCard aliveCard = (kuroyale.cardpack.subclasses.AliveCard) building.getCard();
        double actSpeed = aliveCard.getActSpeed();
        double attackCooldownTime = actSpeed > 0 ? 1.0 / actSpeed : 1.0;

        if (distance <= building.getRange() + 0.5 || canAttackTower) {
            double currentCooldown = attackCooldowns.getOrDefault(building, 0.0);
            if (currentCooldown <= 0) {
                double originalDamage = building.getDamage();
                double multipliedDamage = originalDamage * 5.0;
                target.reduceHP(multipliedDamage);

                attackCooldowns.put(building, attackCooldownTime);

                Platform.runLater(() -> redrawArena());

                if (target.getHP() <= 0) {
                    removeDeadEntity(target);
                }
            }
        }

        building.reduceLifetime(ENTITY_UPDATE_INTERVAL);
        if (building.getHP() <= 0) {
            removeDeadEntity(building);
        }
    }

    private void updateTowerEntity(kuroyale.entitiypack.subclasses.TowerEntity tower) {
        // Get current position from arena map
        int currentRow = -1, currentCol = -1;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (arenaMap.getEntity(r, c) == tower) {
                    currentRow = r;
                    currentCol = c;
                    break;
                }
            }
            if (currentRow >= 0)
                break;
        }

        if (currentRow < 0 || currentCol < 0) {
            return; // Tower not found in map
        }

        // Update tower's internal position tracking
        tower.setPosition(currentRow, currentCol);

        // Find closest target
        AliveEntity target = tower.findClosestTarget(arenaMap);

        if (target == null) {
            return; // No target found
        }

        // Get target position - for towers, find the minimum distance to any cell they
        // occupy
        double distance;
        if (target instanceof kuroyale.entitiypack.subclasses.TowerEntity towerTarget) {
            // Towers occupy multiple cells - find minimum distance to any occupied cell
            int targetTowerSize = towerTarget.isKing() ? 3 : 2;

            // Find the bottom-right corner where the target tower is stored
            int bottomRightRow = -1, bottomRightCol = -1;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (arenaMap.getEntity(r, c) == target) {
                        bottomRightRow = r;
                        bottomRightCol = c;
                        break;
                    }
                }
                if (bottomRightRow >= 0)
                    break;
            }

            if (bottomRightRow < 0 || bottomRightCol < 0) {
                return; // Target not found
            }

            // Calculate minimum distance to any cell the target tower occupies
            // But also consider that the attacking tower occupies multiple cells
            int attackerTowerSize = tower.isKing() ? 3 : 2;
            int attackerStartRow = currentRow - attackerTowerSize + 1;
            int attackerStartCol = currentCol - attackerTowerSize + 1;

            double minDistance = Double.MAX_VALUE;
            int targetStartRow = bottomRightRow - targetTowerSize + 1;
            int targetStartCol = bottomRightCol - targetTowerSize + 1;

            // Check distance from any cell of attacker tower to any cell of target tower
            for (int ar = attackerStartRow; ar <= currentRow; ar++) {
                for (int ac = attackerStartCol; ac <= currentCol; ac++) {
                    for (int tr = targetStartRow; tr <= bottomRightRow; tr++) {
                        for (int tc = targetStartCol; tc <= bottomRightCol; tc++) {
                            int rowDiff = Math.abs(tr - ar);
                            int colDiff = Math.abs(tc - ac);
                            double cellDistance = rowDiff + colDiff;
                            if (cellDistance < minDistance) {
                                minDistance = cellDistance;
                            }
                        }
                    }
                }
            }

            distance = minDistance;
        } else {
            // For non-tower entities, use single cell position
            int targetRow = -1, targetCol = -1;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (arenaMap.getEntity(r, c) == target) {
                        targetRow = r;
                        targetCol = c;
                        break;
                    }
                }
                if (targetRow >= 0)
                    break;
            }

            if (targetRow < 0 || targetCol < 0) {
                return; // Target not found
            }

            // Calculate distance from any cell of the attacking tower to the target
            int attackerTowerSize = tower.isKing() ? 3 : 2;
            int attackerStartRow = currentRow - attackerTowerSize + 1;
            int attackerStartCol = currentCol - attackerTowerSize + 1;

            double minDistance = Double.MAX_VALUE;
            for (int ar = attackerStartRow; ar <= currentRow; ar++) {
                for (int ac = attackerStartCol; ac <= currentCol; ac++) {
                    int rowDiff = Math.abs(targetRow - ar);
                    int colDiff = Math.abs(targetCol - ac);
                    double cellDistance = rowDiff + colDiff;
                    if (cellDistance < minDistance) {
                        minDistance = cellDistance;
                    }
                }
            }

            distance = minDistance;
        }

        // Get attack speed from card
        kuroyale.cardpack.subclasses.AliveCard aliveCard = (kuroyale.cardpack.subclasses.AliveCard) tower.getCard();
        double actSpeed = aliveCard.getActSpeed();
        double attackCooldownTime = actSpeed > 0 ? 1.0 / actSpeed : 1.0; // Time between attacks in seconds

        // Check if in attack range
        if (distance <= tower.getRange() + 0.5) {
            // Check attack cooldown
            double currentCooldown = attackCooldowns.getOrDefault(tower, 0.0);
            if (currentCooldown <= 0) {
                // Can attack - perform attack
                // TEMPORARY: Multiply damage by 5x to see damage clearly
                double originalDamage = tower.getDamage();
                double multipliedDamage = originalDamage * 5.0;
                target.reduceHP(multipliedDamage);

                // Set cooldown for next attack
                attackCooldowns.put(tower, attackCooldownTime);

                // Immediately update health bars after damage
                Platform.runLater(() -> redrawArena());

                // Check if target died
                if (target.getHP() <= 0) {
                    removeDeadEntity(target);
                }
            }
        }
    }

    private String getUnitSpeed(UnitEntity unit) {
        // Access the protected getSpeed() method through the card
        try {
            java.lang.reflect.Method method = UnitEntity.class.getDeclaredMethod("getSpeed");
            method.setAccessible(true);
            return (String) method.invoke(unit);
        } catch (Exception e) {
            // Fallback: try to get from card directly if possible
            return "Medium"; // Default speed
        }
    }

    private double getSpeedMultiplier(String speed) {
        if (speed == null)
            return 1.0;
        switch (speed.toLowerCase()) {
            case "very fast":
                return 3.0;
            case "fast":
                return 2.0;
            case "medium":
                return 1.0;
            case "slow":
                return 0.5;
            case "very slow":
                return 0.25;
            default:
                return 1.0;
        }
    }

    private void loadDeckToSlots() {
        AnchorPane[] cardSlots = { cardSlot0, cardSlot1, cardSlot2, cardSlot3 };
        Label[] costLabels = { card1CostLabel, card2CostLabel, card3CostLabel, card4CostLabel };
        currentHand.clear();

        // Draw initial 4 cards
        for (int i = 0; i < CARD_SLOT_COUNT; i++) {
            if (nextCardIndex < currentDeckCards.size()) {
                Card card = currentDeckCards.get(nextCardIndex);
                currentHand.add(card);
                updateCardSlot(cardSlots[i], costLabels[i], card, i);
                nextCardIndex++;
            } else {
                // Deck exhausted, keep slot empty or show placeholder
                clearCardSlot(cardSlots[i], costLabels[i], i);
            }
        }
    }

    private void updateCardSlot(AnchorPane slotPane, Label costLabel, Card card, int slotIndex) {
        if (slotPane == null) {
            System.err.println("Slot pane is null for slot " + slotIndex);
            return;
        }

        if (card == null) {
            System.err.println("Card is null for slot " + slotIndex);
            return;
        }

        ImageView cardImage = getImageFromPane(slotPane);
        if (cardImage != null) {
            String cardName = card.getName().toLowerCase().replaceAll(" ", "");
            String imagePath = "/kuroyale/images/cards/" + cardName + ".png";

            System.out.println("Updating image for slot " + slotIndex + " with path: " + imagePath);

            // Use the class resource to load the image
            try {
                java.io.InputStream imageStream = getClass().getResourceAsStream(imagePath);
                if (imageStream != null) {
                    javafx.scene.image.Image newImage = new javafx.scene.image.Image(imageStream);
                    // Clear old image and set new one
                    cardImage.setImage(null);
                    cardImage.setImage(newImage);
                    System.out.println("Successfully updated card image in slot " + slotIndex);
                } else {
                    // Fallback to URL string
                    System.out.println("Trying fallback URL method for: " + imagePath);
                    javafx.scene.image.Image newImage = new javafx.scene.image.Image(imagePath);
                    cardImage.setImage(null);
                    cardImage.setImage(newImage);
                    System.out.println("Updated card image using URL in slot " + slotIndex);
                }
            } catch (Exception e) {
                System.err.println("Failed to load image: " + imagePath + " - " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Card image view is null for slot " + slotIndex);
        }

        if (costLabel != null) {
            costLabel.setText(String.valueOf(card.getCost()));
            System.out.println("Updated cost label in slot " + slotIndex + " to: " + card.getCost());
        } else {
            System.err.println("Cost label is null for slot " + slotIndex);
        }

        // Remove old drag handler from both AnchorPane and inner Pane, then add new one
        // to inner Pane
        slotPane.setOnDragDetected(null);
        Pane innerPane = getInnerPaneFromSlot(slotPane);
        if (innerPane != null) {
            // Clear any existing drag handlers
            innerPane.setOnDragDetected(null);
            // Also clear from any child nodes that might have handlers
            for (javafx.scene.Node child : innerPane.getChildren()) {
                if (child instanceof ImageView) {
                    child.setOnDragDetected(null);
                }
            }
            // Make the inner pane draggable
            makeDraggable(innerPane, String.valueOf(card.getId()));
            System.out.println("Made card draggable in slot " + slotIndex + " with ID: " + card.getId()
                    + ", Card name: " + card.getName());
        } else {
            System.err.println("ERROR: Inner pane not found for slot " + slotIndex);
        }
    }

    private void clearCardSlot(AnchorPane slotPane, Label costLabel, int slotIndex) {
        ImageView cardImage = getImageFromPane(slotPane);
        if (cardImage != null) {
            cardImage.setImage(null);
        }

        if (costLabel != null) {
            costLabel.setText("");
        }

        // Remove drag functionality from both AnchorPane and inner Pane
        slotPane.setOnDragDetected(null);
        Pane innerPane = getInnerPaneFromSlot(slotPane);
        if (innerPane != null) {
            innerPane.setOnDragDetected(null);
            for (javafx.scene.Node child : innerPane.getChildren()) {
                if (child instanceof ImageView) {
                    child.setOnDragDetected(null);
                }
            }
        }
    }

    private int findCardSlotIndex(int cardID) {
        System.out.println("Looking for card ID: " + cardID);
        System.out.println("Current hand size: " + currentHand.size());
        for (int i = 0; i < currentHand.size(); i++) {
            Card handCard = currentHand.get(i);
            System.out.println("  Slot " + i + ": " + handCard.getName() + " (ID: " + handCard.getId() + ")");
            if (handCard.getId() == cardID) {
                System.out.println("Found card at slot: " + i);
                return i;
            }
        }
        System.out.println("Card not found in hand!");
        return -1;
    }

    private void cycleCardInSlot(int slotIndex) {
        System.out.println("=== CYCLE CARD IN SLOT " + slotIndex + " ===");
        AnchorPane[] cardSlots = { cardSlot0, cardSlot1, cardSlot2, cardSlot3 };
        Label[] costLabels = { card1CostLabel, card2CostLabel, card3CostLabel, card4CostLabel };

        if (slotIndex < 0 || slotIndex >= cardSlots.length) {
            System.out.println("ERROR: Invalid slot index: " + slotIndex);
            return;
        }

        System.out.println("Current nextCardIndex: " + nextCardIndex + ", Deck size: " + currentDeckCards.size());

        // If deck is exhausted, cycle back to the beginning
        if (nextCardIndex >= currentDeckCards.size()) {
            nextCardIndex = 0;
            System.out.println("Deck cycled back to beginning");
        }

        // Draw next card from deck
        if (nextCardIndex < currentDeckCards.size() && !currentDeckCards.isEmpty()) {
            Card nextCard = currentDeckCards.get(nextCardIndex);
            System.out.println("Drawing next card: " + nextCard.getName() + " (ID: " + nextCard.getId() + ") to slot "
                    + slotIndex);

            // Replace the card at this slot index
            if (slotIndex < currentHand.size()) {
                Card oldCard = currentHand.get(slotIndex);
                System.out.println("Replacing card: " + oldCard.getName() + " (ID: " + oldCard.getId() + ") with "
                        + nextCard.getName() + " (ID: " + nextCard.getId() + ")");
                currentHand.set(slotIndex, nextCard);
            } else {
                System.out.println("Adding new card to hand at slot " + slotIndex);
                currentHand.add(slotIndex, nextCard);
            }

            System.out.println("Calling updateCardSlot for slot " + slotIndex);
            updateCardSlot(cardSlots[slotIndex], costLabels[slotIndex], nextCard, slotIndex);
            nextCardIndex++;
            System.out.println("Next card index is now: " + nextCardIndex);
        } else {
            // Deck is empty, clear slot
            System.out.println("Deck exhausted, clearing slot " + slotIndex);
            if (slotIndex < currentHand.size()) {
                currentHand.remove(slotIndex);
            }
            clearCardSlot(cardSlots[slotIndex], costLabels[slotIndex], slotIndex);
        }
        System.out.println("=== END CYCLE CARD ===");
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
