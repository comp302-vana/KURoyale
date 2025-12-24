package kuroyale.mainpack;

import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import kuroyale.deckpack.Deck;
import kuroyale.deckpack.DeckManager;
import kuroyale.mainpack.models.GameMode;

/**
 * Controller for PvP deck selection scene.
 * Handles selection of decks for both players before starting a PvP match.
 */
public class PvPDeckSelectionController {
    @FXML
    private ComboBox<Deck> player1DeckCombo;
    @FXML
    private ComboBox<Deck> player2DeckCombo;
    @FXML
    private Label player1DeckNameLabel;
    @FXML
    private Label player2DeckNameLabel;
    @FXML
    private Button startBattleButton;
    
    @FXML
    private void initialize() {
        // Load all available decks into both combos
        ObservableList<Deck> decks = FXCollections.observableArrayList(DeckManager.getAllDecks());
        
        // Set StringConverter to display deck names instead of object references
        StringConverter<Deck> deckConverter = new StringConverter<Deck>() {
            @Override
            public String toString(Deck deck) {
                return deck == null ? "" : deck.getName();
            }

            @Override
            public Deck fromString(String string) {
                return decks.stream()
                    .filter(deck -> deck.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        };
        
        player1DeckCombo.setItems(decks);
        player1DeckCombo.setConverter(deckConverter);
        
        player2DeckCombo.setItems(decks);
        player2DeckCombo.setConverter(deckConverter);
        
        // Initially disable start button
        startBattleButton.setDisable(true);
        
        // Update deck name labels when selections change
        player1DeckCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                player1DeckNameLabel.setText("Selected: " + newVal.getName());
            } else {
                player1DeckNameLabel.setText("");
            }
            checkReady();
        });
        
        player2DeckCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                player2DeckNameLabel.setText("Selected: " + newVal.getName());
            } else {
                player2DeckNameLabel.setText("");
            }
            checkReady();
        });
    }
    
    private void checkReady() {
        boolean ready = player1DeckCombo.getValue() != null && 
                       player2DeckCombo.getValue() != null;
        startBattleButton.setDisable(!ready);
    }
    
    @FXML
    private void handleStartBattle(ActionEvent event) throws IOException {
        Deck deck1 = player1DeckCombo.getValue();
        Deck deck2 = player2DeckCombo.getValue();
        
        if (deck1 == null || deck2 == null) {
            return; // Should not happen if button is enabled
        }
        
        // Store decks and set game mode in GameEngine
        GameEngine.setGameMode(GameMode.LOCAL_PVP);
        GameEngine.setPlayer1Deck(deck1);
        GameEngine.setPlayer2Deck(deck2);
        
        // Navigate to battle scene
        switchToBattleScene(event);
    }
    
    @FXML
    private void handleBack(ActionEvent event) throws IOException {
        switchToStartBattleScene(event);
    }
    
    private void switchToBattleScene(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/BattleScene.fxml"));
        root.setStyle("-fx-background-color: BD7FFF;");
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 1280, 720, Color.web("0xBD7FFF"));
        stage.setScene(scene);
        stage.show();
    }
    
    private void switchToStartBattleScene(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/StartBattleScene.fxml"));
        root.setStyle("-fx-background-color: BD7FFF;");
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 1280, 720, Color.web("0xBD7FFF"));
        stage.setScene(scene);
        stage.show();
    }
}
