package kuroyale.mainpack;

import java.io.IOException;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;

import java.util.EnumMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import java.io.File;

import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javafx.scene.input.TransferMode;
import javafx.scene.input.ClipboardContent;

import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;
import javafx.scene.input.MouseButton;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import kuroyale.arenapack.SpriteLoader;

public class ArenaController {

    @FXML
    private GridPane arenaGrid;

    @FXML
    private Pane ourtower;
    @FXML
    private Pane enemytower;
    @FXML
    private Pane ourking;
    @FXML
    private Pane enemyking;
    @FXML
    private Pane bridge;

    @FXML
    private TextField saveNameField;

    // MODEL
    private ArenaMap arenaMap = new ArenaMap();

    private EnumMap<ArenaObjectType, Integer> remaining = new EnumMap<>(ArenaObjectType.class);

    private EnumMap<ArenaObjectType, Label> countLabels = new EnumMap<>(ArenaObjectType.class);

    private final int rows = ArenaMap.getRows();
    private final int cols = ArenaMap.getCols();

    private final int tileSize = 24;


    @FXML
    private void initialize() {
        // Make draggable palette items

        remaining.put(ArenaObjectType.OUR_TOWER, 2);
        remaining.put(ArenaObjectType.OUR_KING, 1);
        remaining.put(ArenaObjectType.ENEMY_TOWER, 2);
        remaining.put(ArenaObjectType.ENEMY_KING, 1);
        remaining.put(ArenaObjectType.BRIDGE, 3);

        makeDraggable(ourtower, "ourtower");
        makeDraggable(ourking, "ourking");
        makeDraggable(enemytower, "enemytower");
        makeDraggable(enemyking, "enemyking");
        makeDraggable(bridge, "bridge");

        fillArenaGrid();
        addPaletteSprites();
        loadDefaultArenaIfExists();
    }

    private ArenaObjectType convert(String s) {
        return switch (s.toLowerCase()) {
            case "ourtower" -> ArenaObjectType.OUR_TOWER;
            case "ourking" -> ArenaObjectType.OUR_KING;
            case "enemytower" -> ArenaObjectType.ENEMY_TOWER;
            case "enemyking" -> ArenaObjectType.ENEMY_KING;
            case "bridge" -> ArenaObjectType.BRIDGE;
            case "entity" -> ArenaObjectType.ENTITY;
            default -> null;
        };
    }

    private void addPaletteSprites() {
        addSpriteToPane(ourtower, ArenaObjectType.OUR_TOWER, "Our Tower", 2);
        addSpriteToPane(ourking, ArenaObjectType.OUR_KING, "Our King", 1);
        addSpriteToPane(enemytower, ArenaObjectType.ENEMY_TOWER, "Enemy Tower", 2);
        addSpriteToPane(enemyking, ArenaObjectType.ENEMY_KING, "Enemy King", 1);
        addSpriteToPane(bridge, ArenaObjectType.BRIDGE, "Bridge", 3);
    }

    private void addSpriteToPane(Pane pane, ArenaObjectType type, String displayName, int count) {

        ImageView img = loadFullImage(type); // FULL PNG, not sprite
        if (img == null)
            return;

        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        Label countLabel = new Label(" x" + remaining.get(type));
        countLabel.setStyle("-fx-text-fill: yellow; -fx-font-size: 12px;");
        countLabels.put(type, countLabel); // store label reference

        HBox labelBox = new HBox(nameLabel, countLabel);
        labelBox.setAlignment(Pos.CENTER);

        VBox box = new VBox(2, img, labelBox);
        box.setAlignment(Pos.CENTER);

        pane.getChildren().add(box);

        Platform.runLater(() -> {
            double pw = pane.getWidth();
            double ph = pane.getHeight();
            double bw = box.getBoundsInLocal().getWidth();
            double bh = box.getBoundsInLocal().getHeight();

            box.setLayoutX((pw - bw) / 2);
            box.setLayoutY((ph - bh) / 2);
        });
    }

    private ImageView loadFullImage(ArenaObjectType type) {
        ImageView img = SpriteLoader.getImage(type, tileSize);
        img.setFitWidth(30);
        img.setPreserveRatio(true);

        return img;
    }

    private void fillArenaGrid() {

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {

                Pane tile = new Pane();
                tile.setPrefWidth(tileSize);
                tile.setPrefHeight(tileSize);

                if (col >= cols / 2 - 1 && col <= cols / 2) {
                    tile.setStyle(
                            "-fx-background-image: url('/kuroyale/images/water.png');" +
                                    "-fx-background-size: cover;");
                } else {
                    if ((col + row) % 2 == 0) {
                        tile.setStyle(
                                "-fx-background-image: url('/kuroyale/images/darkGrass.png');" +
                                        "-fx-background-size: cover;");
                    } else {
                        tile.setStyle(
                                "-fx-background-image: url('/kuroyale/images/lightGrass.png');" +
                                        "-fx-background-size: cover;");
                    }
                }

                int r = row;
                int c = col;

                tile.setOnMouseClicked(e -> System.out.println("Clicked tile: " + r + ", " + c));

                tile.setOnDragOver(event -> {
                    if (event.getDragboard().hasString())
                        event.acceptTransferModes(TransferMode.COPY);

                    event.consume();
                });

                tile.setOnDragDropped(event -> {
                    var db = event.getDragboard();
                    boolean success = false;

                    if (db.hasString()) {

                        String typeStr = db.getString();
                        ArenaObjectType objType = convert(typeStr);

                        System.out.println("Trying to place " + objType + " at " + r + "," + c);

                        if (remaining.containsKey(objType) && remaining.get(objType) == 0) {
                            System.out.println("NO REMAINING: " + objType);
                            return;
                        }

                        boolean placementOK = true;
                        if (objType == ArenaObjectType.BRIDGE) {
                            if (c == 15) {
                                placementOK = false;
                            }
                        }
                        if (placementOK) {
                            placementOK = arenaMap.placeObject(r, c, objType);
                        }
                        if (placementOK) {
                            decrement(objType);
                            // Try to get sprite
                            ImageView sprite;
                            if (objType == ArenaObjectType.BRIDGE) {
                                sprite = SpriteLoader.getBuilderBridgeSprite(tileSize);
                                arenaMap.setObject(r, c + (2 * (c % 2) - 1), ArenaObjectType.BRIDGE);
                                System.out.printf("placed first bridge on %d, %d\n", r, c);
                                System.out.printf("placed second bridge on %d, %d\n", r, c + (2 * (c % 2) - 1));
                            } else {
                                sprite = SpriteLoader.getSprite(objType, tileSize);
                            }
                            if (sprite != null) {
                                tile.getChildren().add(sprite);

                                javafx.application.Platform.runLater(() -> {
                                    sprite.applyCss();
                                    sprite.autosize();

                                    double tileW = tile.getWidth();
                                    double spriteW = sprite.getBoundsInParent().getWidth();
                                    
                                    // ALIGN right (otherwise we draw tile on top of sprites)
                                    sprite.setTranslateX(tileW - spriteW);
                                    
                                    // ALIGN bottom
                                    double tileH = tile.getHeight();
                                    double spriteH = sprite.getBoundsInParent().getHeight();
                                    
                                    sprite.setTranslateY(tileH - spriteH);
                                });

                                sprite.setOnMouseClicked(ev -> {
                                    if (ev.getButton() == MouseButton.SECONDARY) {
                                        arenaMap.clearObject(r, c);
                                        tile.getChildren().remove(sprite);
                                        increment(objType);
                                        ev.consume();
                                    }
                                });
                            }

                            else {
                                Pane fallback = new Pane();
                                fallback.setPrefSize(tileSize, tileSize);
                                fallback.setStyle("-fx-background-color: gray;");

                                fallback.setOnMouseClicked(ev -> {
                                    if (ev.getButton() == MouseButton.SECONDARY) {
                                        arenaMap.clearObject(r, c);
                                        tile.getChildren().remove(fallback);
                                        ev.consume();
                                    }
                                });

                                tile.getChildren().add(fallback);
                            }

                            success = true;

                        } else {
                            tile.setStyle(
                                    "-fx-background-color: #FFCDD2; -fx-border-color: #E57373; -fx-border-width: 1;");
                            System.out.println("INVALID PLACEMENT at " + r + "," + c);
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

    @FXML
    void btnSelectDeckClicked(ActionEvent event) throws IOException {
        switchToDeckBuilderScene(event);
    }

    @FXML
    void btnSelectBattleClicked(ActionEvent event) throws IOException {
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

    private void switchToDeckBuilderScene(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/DeckBuilderScene.fxml"));
        root.setStyle("-fx-background-color: BD7FFF;");
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, Color.web("0xBD7FFF"));
        stage.setScene(scene);
        stage.show();
    }

    private void decrement(ArenaObjectType t) {
        if (remaining.containsKey(t)) {
            remaining.put(t, remaining.get(t) - 1);
            countLabels.get(t).setText(" x" + remaining.get(t));
        }
    }

    private void increment(ArenaObjectType t) {
        if (remaining.containsKey(t)) {
            remaining.put(t, remaining.get(t) + 1);
            countLabels.get(t).setText(" x" + remaining.get(t));
        }
    }

    /*
    private String showLoadDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/kuroyale/scenes/LoadArenaDialog.fxml"));
        Parent root = loader.load();

        Stage stage = new Stage();
        stage.setTitle("Load Arena");
        stage.setScene(new Scene(root));
        stage.showAndWait();

        return (String) stage.getUserData(); // selected file or null
    }
    */

    @FXML
    private void onSaveClicked() {
        // 1. Check name
        String name = saveNameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Error", "Please enter a save name.");
            return;
        }

        File file = new File("saves/" + name + ".csv");
        if (file.exists()) {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setHeaderText(null);
            a.setContentText("File \"" + name + "\" already exists.\nOverwrite?");
            var result = a.showAndWait();
            if (result.isEmpty() || result.get().getButtonData().isCancelButton()) {
                return; // user cancelled
            }
        }

        // 2. Check remaining counts (must ALL be 0 except bridges)
        for (var entry : remaining.entrySet()) {
            if (entry.getValue() >= 3 || (entry.getKey() != ArenaObjectType.BRIDGE && entry.getValue() > 0)) {
                showAlert("Error", "You must place all objects before saving.");
                return;
            }
        }

        // 3. Create saves directory
        File dir = new File("saves/");
        dir.mkdirs();

        // 4. Save file
        String filePath = "saves/" + name + ".csv";
        arenaMap.saveToFile(filePath);

        // 5. Show success message
        showAlert("Saved!", "Arena saved as \"" + name + "\".");
    }

    private void saveDefaultArenaPreviewPng() {
        try {
            // Ensure directory exists
            Files.createDirectories(Path.of("saves"));

            File out = new File("saves/default_preview.png");

            // Make sure CSS/layout are applied before snapshot
            arenaGrid.applyCss();
            arenaGrid.layout();

            SnapshotParameters params = new SnapshotParameters();
            // If you want transparent background, use TRANSPARENT
            // If you want a solid background, set a Color instead.
            params.setFill(Color.TRANSPARENT);

            WritableImage fxImage = arenaGrid.snapshot(params, null);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(fxImage, null);

            ImageIO.write(bufferedImage, "png", out);

            System.out.println("Saved default preview: " + out.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void onLoadClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/kuroyale/scenes/LoadArenaDialog.fxml"));
            Parent root = loader.load();

            Stage dialog = new Stage();
            dialog.setScene(new Scene(root));
            dialog.setTitle("Load Saved Arena");
            dialog.showAndWait();

            Object result = dialog.getUserData();
            if (result == null)
                return;

            // CASE 1 → User set a default
            if (result instanceof String s && s.startsWith("DEFAULT_SET:")) {
                String fileName = s.substring("DEFAULT_SET:".length());

                // load into builder so the preview matches new default
                File file = new File("saves/" + fileName);
                arenaMap.loadFromFile(file.getAbsolutePath());
                redrawArena();
                saveNameField.setText(fileName.replace(".csv", ""));

                // snapshot it
                Platform.runLater(() -> {
                    Platform.runLater(this::saveDefaultArenaPreviewPng);
                });     // safer than immediate

                showAlert("Default Arena Set", "Default arena set to: " + fileName);
                return;
            }

            // CASE 2 → User clicked Load
            String selectedFile = (String) result;
            File file = new File("saves/" + selectedFile);

            arenaMap.loadFromFile(file.getAbsolutePath());
            redrawArena();

            String justName = selectedFile.replace(".csv", "");
            saveNameField.setText(justName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void recalcRemainingFromArena() {
        // reset full
        remaining.put(ArenaObjectType.OUR_TOWER, 2);
        remaining.put(ArenaObjectType.ENEMY_TOWER, 2);
        remaining.put(ArenaObjectType.OUR_KING, 1);
        remaining.put(ArenaObjectType.ENEMY_KING, 1);
        remaining.put(ArenaObjectType.BRIDGE, 3);

        // scan the map and subtract
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                var obj = arenaMap.getObject(r, c);
                if (obj == null)
                    continue;

                if (remaining.containsKey(obj.getType())) {
                    remaining.put(obj.getType(), remaining.get(obj.getType()) - 1);
                }
            }
        }

        // update right panel UI
        for (var t : remaining.keySet()) {
            countLabels.get(t).setText(" x" + remaining.get(t));
        }
    }

    private void redrawArena() {
        // clear grid UI
        for (Node n : arenaGrid.getChildren()) {
            if (n instanceof Pane tile) {
                tile.getChildren().clear(); // remove sprites
            }
        }

        recalcRemainingFromArena();

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

                sprite.setOnMouseClicked(ev -> {
                    if (ev.getButton() == MouseButton.SECONDARY) {
                        arenaMap.clearObject(rr, cc); // ✅ FIXED
                        tile.getChildren().remove(sprite);

                        if (remaining.containsKey(obj.getType())) {
                            remaining.put(obj.getType(), remaining.get(obj.getType()) + 1);
                            countLabels.get(obj.getType()).setText(" x" + remaining.get(obj.getType()));
                        }
                        ev.consume();
                    }
                });
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

    private void showAlert(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    private void loadDefaultArenaIfExists() {
        try {
            File f = new File("saves/default.txt");
            if (!f.exists())
                return; // no default set

            String fileName = new String(java.nio.file.Files.readAllBytes(f.toPath())).trim();
            if (fileName.isEmpty())
                return;

            File arenaFile = new File("saves/" + fileName);
            if (!arenaFile.exists()) {
                System.out.println("Default file missing: " + fileName);
                return;
            }

            // Load arena contents
            arenaMap.loadFromFile(arenaFile.getAbsolutePath());

            // Redraw sprites
            redrawArena();
            saveNameField.setText(fileName.replace(".csv", ""));
            System.out.println("Loaded default arena: " + fileName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNewArenaClicked() {
        // reset model
        arenaMap = new ArenaMap();

        // reset remaining counters
        remaining.put(ArenaObjectType.OUR_TOWER, 2);
        remaining.put(ArenaObjectType.ENEMY_TOWER, 2);
        remaining.put(ArenaObjectType.OUR_KING, 1);
        remaining.put(ArenaObjectType.ENEMY_KING, 1);
        remaining.put(ArenaObjectType.BRIDGE, 3);

        // update labels
        for (ArenaObjectType t : countLabels.keySet()) {
            countLabels.get(t).setText(" x" + remaining.get(t));
        }

        // clear grid UI
        for (Node n : arenaGrid.getChildren()) {
            if (n instanceof Pane tile) {
                tile.getChildren().clear(); // remove sprites

                // Integer r = GridPane.getRowIndex(n);
                Integer c = GridPane.getColumnIndex(n);
                Integer r = GridPane.getRowIndex(n);
                int col = (c == null ? 0 : c);
                int row = (r == null ? 0 : r);

                // restore tile color
                if (col >= cols / 2 - 1 && col <= cols / 2) {
                    tile.setStyle(
                            "-fx-background-image: url('/kuroyale/images/water.png');" +
                                    "-fx-background-size: cover;");
                } else {
                    if ((col + row) % 2 == 0) {
                        tile.setStyle(
                                "-fx-background-image: url('/kuroyale/images/darkGrass.png');" +
                                        "-fx-background-size: cover;");
                    } else {
                        tile.setStyle(
                                "-fx-background-image: url('/kuroyale/images/lightGrass.png');" +
                                        "-fx-background-size: cover;");
                    }
                }
            }
        }

        // clear textfield
        saveNameField.setText("");

        System.out.println("New arena created.");
    }
}
