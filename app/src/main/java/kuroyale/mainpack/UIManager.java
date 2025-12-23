package kuroyale.mainpack;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class UIManager extends Application {
    private Stage stage;
    private Scene scene;
    private Parent root;
    private static String selectedDifficulty = null;

    @FXML
    private ImageView arenaPreviewImage;

    @FXML
    private void initialize() {
        refreshArenaPreviewIfPresent();
    }

    private void refreshArenaPreviewIfPresent() {
        // System.out.println("Root: " + (arenaPreviewImage != null ?
        // arenaPreviewImage.getScene().getRoot() : "null"));

        if (arenaPreviewImage == null)
            return;

        File f = new File("saves/default_preview.png");
        if (!f.exists() || f.length() == 0) {
            arenaPreviewImage.setImage(null);
            return;
        }

        try (FileInputStream fis = new FileInputStream(f)) {
            Image img = new Image(fis);
            arenaPreviewImage.setImage(img);
        } catch (Exception e) {
            e.printStackTrace();
            arenaPreviewImage.setImage(null);
        }
    }

    @Override
    public void start(Stage stage) {
        try {
            // Load saved deck on startup
            loadSavedDeck();

            Parent root = FXMLLoader.load(UIManager.class.getResource("/kuroyale/scenes/StartScene.fxml"));
            Scene scene = new Scene(root, 1280, 720);
            root.setStyle("-fx-background-color: BD7FFF;");
            stage.setResizable(false);
            stage.setTitle("KURoyale");
            stage.getIcons().add(new Image("/kuroyale/images/icon.png"));
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
        }
    }

    private void loadSavedDeck() {
        int savedIndex = kuroyale.deckpack.DeckManager.loadSelectedDeckIndex();
        String deckName = "Deck_" + (savedIndex + 1);
        kuroyale.deckpack.Deck deck = kuroyale.deckpack.DeckManager.loadDeck(deckName);
        if (deck != null) {
            kuroyale.deckpack.DeckManager.setCurrentDeck(deck);
        }
    }

    @FXML
    void btnQuitCliked(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    void btnStartClicked(ActionEvent event) throws IOException {
        switchToStartBattleScene(event);
        ;
    }

    @FXML
    void btnSelectBattleClicked(ActionEvent event) throws IOException {
        switchToStartBattleScene(event);
    }

    @FXML
    void btnSelectBuildClicked(ActionEvent event) throws IOException {
        switchToArenaBuilderScene(event);
    }

    @FXML
    void btnSelectDeckClicked(ActionEvent event) throws IOException {
        switchToDeckBuilderScene(event);
    }

    @FXML
    void btnStartBattleClicked(ActionEvent event) throws IOException {
        // Check if current deck has 8 cards
        kuroyale.deckpack.Deck currentDeck = kuroyale.deckpack.DeckManager.getCurrentDeck();
        if (currentDeck == null || currentDeck.getSize() != 8) {
            // Show warning alert
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Incomplete Deck");
            alert.setHeaderText("Your deck is not complete!");
            alert.setContentText(
                    "You need exactly 8 cards in your deck to start a battle. Please go to the Deck Builder to complete your deck.");
            alert.showAndWait();
            return;
        }
        switchToDifficultySelectionScene(event);
    }

    @FXML
    void btnNoAIClicked(ActionEvent event) throws IOException {
        selectedDifficulty = "NoAI";
        switchToBattleScene(event);
    }

    @FXML
    void btnSimpleClicked(ActionEvent event) throws IOException {
        selectedDifficulty = "Simple";
        switchToBattleScene(event);
    }

    @FXML
    void btnMediumClicked(ActionEvent event) throws IOException {
        selectedDifficulty = "Medium";
        switchToBattleScene(event);
    }

    @FXML
    void btnAdvancedClicked(ActionEvent event) throws IOException {
        selectedDifficulty = "Advanced";
        switchToBattleScene(event);
    }

    private void switchToStartBattleScene(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/kuroyale/scenes/StartBattleScene.fxml"));
        root = loader.load();

        // Controller exists now (will be UIManager if fx:controller points to it)
        Object controller = loader.getController();
        if (controller instanceof UIManager ui) {
            ui.refreshArenaPreviewIfPresent();
        }

        root.setStyle("-fx-background-color: BD7FFF;");
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root, 1280, 720, Color.web("0xBD7FFF"));
        stage.setScene(scene);
        stage.show();
    }

    private void switchToDeckBuilderScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/DeckBuilderScene.fxml"));
        root.setStyle("-fx-background-color: BD7FFF;");
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root, 1280, 720, Color.web("0xBD7FFF"));
        stage.setScene(scene);
        stage.show();
    }

    private void switchToArenaBuilderScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/ArenaBuilderScene.fxml"));
        root.setStyle("-fx-background-color: BD7FFF;");
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root, 1280, 720, Color.web("0xBD7FFF"));
        stage.setScene(scene);
        stage.show();
    }

    private void switchToBattleScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/BattleScene.fxml"));
        root.setStyle("-fx-background-color: BD7FFF;");
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root, 1280, 720, Color.web("0xBD7FFF"));
        stage.setScene(scene);
        stage.show();
    }

    private void switchToDifficultySelectionScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/DifficultySelectionScene.fxml"));
        root.setStyle("-fx-background-color: BD7FFF;");
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root, 1280, 720, Color.web("0xBD7FFF"));
        stage.setScene(scene);
        stage.show();
    }

    public static String getSelectedDifficulty() {
        return selectedDifficulty;
    }
}