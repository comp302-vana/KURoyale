package kuroyale.mainpack.network;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import kuroyale.deckpack.Deck;
import kuroyale.deckpack.DeckManager;

public class NetworkLobbyController {
    @FXML
    private ListView<String> listPlayers;
    @FXML
    private Button btnStartGame;
    @FXML
    private Button btnLeave;
    @FXML
    private CheckBox chkReady;
    @FXML
    private Label lblDeckInfo;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblConnectionInfo;
    
    private NetworkManager networkManager;
    private Timer updateTimer;
    private String selectedDeckName;
    
    @FXML
    private void initialize() {
        networkManager = NetworkManager.getInstance();
        
        // Load and set default deck automatically
        loadAndSetDefaultDeck();
        
        // Start periodic lobby updates
        startLobbyUpdateTimer();
        
        // Update UI
        updateLobbyUI();
        
        // Set start button visibility (only host can start)
        btnStartGame.setVisible(networkManager.isHost());
    }
    
    private void loadAndSetDefaultDeck() {
        // Get the currently selected deck number
        int selectedDeckNumber = DeckManager.getSelectedDeckNumber();
        Deck defaultDeck = DeckManager.loadDeckByNumber(selectedDeckNumber);
        
        if (defaultDeck != null && defaultDeck.getCards().size() == 8) {
            selectedDeckName = defaultDeck.getName();
            networkManager.setPlayerDeck(selectedDeckName);
            
            // Update deck info display
            StringBuilder deckInfo = new StringBuilder("Deck: " + selectedDeckName + "\nCards: ");
            for (int i = 0; i < defaultDeck.getCards().size(); i++) {
                if (i > 0) deckInfo.append(", ");
                deckInfo.append(defaultDeck.getCards().get(i).getName());
            }
            lblDeckInfo.setText(deckInfo.toString());
        } else {
            // Try to find any valid deck
            List<Deck> allDecks = DeckManager.getAllDecks();
            for (Deck deck : allDecks) {
                if (deck.getCards().size() == 8) {
                    selectedDeckName = deck.getName();
                    networkManager.setPlayerDeck(selectedDeckName);
                    
                    StringBuilder deckInfo = new StringBuilder("Deck: " + selectedDeckName + "\nCards: ");
                    for (int i = 0; i < deck.getCards().size(); i++) {
                        if (i > 0) deckInfo.append(", ");
                        deckInfo.append(deck.getCards().get(i).getName());
                    }
                    lblDeckInfo.setText(deckInfo.toString());
                    return;
                }
            }
            
            // No valid deck found
            lblDeckInfo.setText("No valid deck found!\nPlease create a deck with 8 cards.");
            lblStatus.setText("Warning: No valid deck available");
        }
    }
    
    private void startLobbyUpdateTimer() {
        updateTimer = new Timer(true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> updateLobbyUI());
            }
        }, 0, 500); // Update every 500ms
    }
    
    private void updateLobbyUI() {
        if (networkManager == null) return;
        
        // Check if connection was lost
        // Only auto-close if we're the client and host disconnected
        // If we're the host and client disconnected, just show message and wait
        if (!networkManager.isConnected() && (networkManager.getPlayer1Name() != null || networkManager.getPlayer2Name() != null)) {
            boolean isHost = networkManager.isHost();
            String otherPlayerName = isHost ? networkManager.getPlayer2Name() : networkManager.getPlayer1Name();
            
            // Only show message if we had a connection before
            if (otherPlayerName != null && !otherPlayerName.isEmpty()) {
                Platform.runLater(() -> {
                    lblStatus.setText("Other player has left the lobby");
                    lblConnectionInfo.setText("Status: Disconnected - Other player left");
                    
                    // Only auto-close if we're the client (host disconnected)
                    // Host should stay in lobby and wait for new connection
                    if (!isHost) {
                        // Client: host disconnected, auto-close after 2 seconds
                        new Timer(true).schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Platform.runLater(() -> {
                                    if (btnLeave != null && btnLeave.getScene() != null) {
                                        btnLeaveClicked();
                                    }
                                });
                            }
                        }, 2000);
                    }
                    // Host: client disconnected, just show message and wait
                });
                return;
            }
        }
        
        // Update player list
        ObservableList<String> players = FXCollections.observableArrayList();
        String player1Name = networkManager.getPlayer1Name();
        String player2Name = networkManager.getPlayer2Name();
        
        if (player1Name != null) {
            String player1Status = networkManager.isPlayer1Ready() ? "✓ Ready" : "Not Ready";
            String player1Deck = networkManager.getPlayer1Deck();
            String player1Info = "Player 1: " + player1Name + " - " + player1Status;
            if (player1Deck != null && !player1Deck.isEmpty()) {
                player1Info += "\n  Deck: " + player1Deck;
            }
            players.add(player1Info);
        }
        
        if (player2Name != null) {
            String player2Status = networkManager.isPlayer2Ready() ? "✓ Ready" : "Not Ready";
            String player2Deck = networkManager.getPlayer2Deck();
            String player2Info = "Player 2: " + player2Name + " - " + player2Status;
            if (player2Deck != null && !player2Deck.isEmpty()) {
                player2Info += "\n  Deck: " + player2Deck;
            }
            players.add(player2Info);
        }
        
        listPlayers.setItems(players);
        
        // Update connection info
        if (networkManager.isConnected()) {
            lblConnectionInfo.setText("Status: Connected");
            if (networkManager.isHost()) {
                lblConnectionInfo.setText("Status: Host - Waiting for player...");
                if (player2Name != null) {
                    lblConnectionInfo.setText("Status: Host - Player connected");
                }
            } else {
                lblConnectionInfo.setText("Status: Client - Connected to host");
            }
        } else {
            lblConnectionInfo.setText("Status: Disconnected");
        }
        
        // Update start button state
        if (networkManager.isHost()) {
            boolean clientConnected = player2Name != null && !player2Name.isEmpty();
            boolean bothReady = networkManager.isPlayer1Ready() && networkManager.isPlayer2Ready();
            boolean bothHaveDecks = networkManager.getPlayer1Deck() != null && 
                                   networkManager.getPlayer2Deck() != null &&
                                   !networkManager.getPlayer1Deck().isEmpty() &&
                                   !networkManager.getPlayer2Deck().isEmpty();
            
            // Button is disabled if: no client connected, not both ready, or missing decks
            btnStartGame.setDisable(!clientConnected || !bothReady || !bothHaveDecks || !networkManager.isConnected());
        }
        
        // Update ready checkbox to match current state
        boolean currentReady = networkManager.isHost() ? 
            networkManager.isPlayer1Ready() : networkManager.isPlayer2Ready();
        if (chkReady.isSelected() != currentReady) {
            chkReady.setSelected(currentReady);
        }
    }
    
    @FXML
    private void chkReadyChanged() {
        boolean ready = chkReady.isSelected();
        networkManager.setPlayerReady(ready);
        lblStatus.setText(ready ? "You are ready!" : "You are not ready");
    }
    
    @FXML
    private void btnStartGameClicked() {
        if (!networkManager.isHost()) {
            lblStatus.setText("Only host can start the game");
            return;
        }
        
        if (!networkManager.isPlayer1Ready() || !networkManager.isPlayer2Ready()) {
            lblStatus.setText("Both players must be ready");
            return;
        }
        
        String player2Name = networkManager.getPlayer2Name();
        if (player2Name == null || player2Name.isEmpty()) {
            lblStatus.setText("Waiting for another player to join");
            return;
        }
        
        if (networkManager.getPlayer1Deck() == null || networkManager.getPlayer2Deck() == null ||
            networkManager.getPlayer1Deck().isEmpty() || networkManager.getPlayer2Deck().isEmpty()) {
            lblStatus.setText("Both players must have valid decks");
            return;
        }
        
        networkManager.startGame();
        lblStatus.setText("Starting game...");
        
        // Navigate to battle scene (will be implemented later)
        // For now, just show a message
        Platform.runLater(() -> {
            lblStatus.setText("Game starting! (Battle screen not yet implemented)");
        });
    }
    
    @FXML
    private void btnLeaveClicked() {
        if (updateTimer != null) {
            updateTimer.cancel();
        }
        
        // Send disconnect message before closing
        if (networkManager != null && networkManager.isConnected()) {
            // The close() method will send the DISCONNECT message
            networkManager.close();
        }
        
        // Navigate back to start battle scene
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/kuroyale/scenes/StartBattleScene.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 720);
            scene.getRoot().setStyle("-fx-background-color: BD7FFF;");
            
            // Get stage safely
            Stage stage = null;
            if (btnLeave != null && btnLeave.getScene() != null) {
                stage = (Stage) btnLeave.getScene().getWindow();
            } else if (listPlayers != null && listPlayers.getScene() != null) {
                stage = (Stage) listPlayers.getScene().getWindow();
            }
            
            if (stage != null) {
                stage.setScene(scene);
            } else {
                System.err.println("Error: Could not get stage to navigate back");
            }
        } catch (IOException e) {
            System.err.println("Error navigating back: " + e.getMessage());
        }
    }
}

