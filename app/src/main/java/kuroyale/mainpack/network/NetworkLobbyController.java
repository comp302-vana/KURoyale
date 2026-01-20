package kuroyale.mainpack.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
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
import kuroyale.mainpack.network.NetworkMessage;

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
    @FXML
    private Label lblIPAddress;
    
    private NetworkManager networkManager;
    private Timer updateTimer;
    private String selectedDeckName;
    private boolean battleHandlerRegistered = false; // Track if handler is already registered
    
    @FXML
    private void initialize() {
        networkManager = NetworkManager.getInstance();
        
        // Reset lobby state when entering lobby (in case returning from a game)
        resetLobbyState();
        
        // Register lobby message handler to listen for START_GAME message (only once)
        if (!battleHandlerRegistered) {
            networkManager.registerLobbyMessageHandler(msg -> {
                if (msg.getType() == NetworkMessage.MessageType.START_GAME) {
                    Platform.runLater(() -> {
                        // Client receives START_GAME, navigate to battle
                        if (!networkManager.isHost()) {
                            navigateToBattleScene();
                        }
                    });
                }
            });
            battleHandlerRegistered = true;
        }
        
        // Load and set default deck automatically
        loadAndSetDefaultDeck();
        
        // Start periodic lobby updates
        startLobbyUpdateTimer();
        
        // Update UI
        updateLobbyUI();
        
        // Set start button visibility (only host can start)
        btnStartGame.setVisible(networkManager.isHost());
        
        // Display connection info
        updateConnectionInfo();
    }
    
    private void updateConnectionInfo() {
        Platform.runLater(() -> {
            try {
                if (NetworkManager.isRelayModeEnabled()) {
                    // Relay mode: Show relay server info
                    String relayIP = NetworkManager.getRelayServerIP();
                    int relayPort = NetworkManager.getRelayServerPort();
                    
                    StringBuilder info = new StringBuilder();
                    info.append("Connected via Relay Server\n");
                    info.append("Relay: ").append(relayIP).append(":").append(relayPort).append("\n");
                    
                    if (networkManager.isHost()) {
                        info.append("Status: Waiting for player...");
                        lblIPAddress.setStyle("-fx-font-size: 13px; -fx-text-fill: #90EE90; -fx-font-weight: bold;");
                    } else {
                        info.append("Status: Waiting for host...");
                        lblIPAddress.setStyle("-fx-font-size: 13px; -fx-text-fill: #90EE90; -fx-font-weight: bold;");
                    }
                    
                    lblIPAddress.setText(info.toString());
                } else {
                    // Direct mode: Show IP addresses (old behavior)
                    updateIPAddressDirect();
                }
            } catch (Exception e) {
                lblIPAddress.setText("Connection: Error");
                lblIPAddress.setStyle("-fx-font-size: 13px; -fx-text-fill: white;");
                System.err.println("Error updating connection info: " + e.getMessage());
            }
        });
    }
    
    private void updateIPAddressDirect() {
        // Load IPs in background thread (for direct mode only)
        new Thread(() -> {
            String localIP = getLocalIPAddress();
            String publicIP = null;
            
            // Only fetch public IP if host (they need to share it)
            if (networkManager.isHost()) {
                // Try multiple services in case one fails
                String[] services = {
                    "https://api.ipify.org",
                    "https://checkip.amazonaws.com",
                    "https://icanhazip.com"
                };
                
                for (String serviceUrl : services) {
                    try {
                        URL url = new URL(serviceUrl);
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
                            publicIP = in.readLine();
                            if (publicIP != null && !publicIP.trim().isEmpty()) {
                                publicIP = publicIP.trim();
                                break; // Success, stop trying other services
                            }
                        }
                    } catch (Exception e) {
                        // Try next service
                    }
                }
            }
            
            final String finalLocalIP = localIP;
            final String finalPublicIP = publicIP;
            
            Platform.runLater(() -> {
                try {
                    StringBuilder ipText = new StringBuilder();
                    
                    if (networkManager.isHost()) {
                        // Host: Show both IPs
                        if (finalLocalIP != null && !finalLocalIP.isEmpty()) {
                            ipText.append("Local IP (same network): ").append(finalLocalIP).append("\n");
                        }
                        
                        if (finalPublicIP != null && !finalPublicIP.isEmpty()) {
                            ipText.append("Public IP (internet): ").append(finalPublicIP);
                            ipText.append(" ← Share this with your friend!");
                            lblIPAddress.setStyle("-fx-font-size: 14px; -fx-text-fill: #90EE90; -fx-font-weight: bold;");
                        } else {
                            if (finalLocalIP != null && !finalLocalIP.isEmpty()) {
                                ipText.append("\nPublic IP: Unable to detect (check whatismyip.com)");
                            }
                            lblIPAddress.setStyle("-fx-font-size: 13px; -fx-text-fill: #FFA500;");
                        }
                    } else {
                        // Client: Just show local IP
                        if (finalLocalIP != null && !finalLocalIP.isEmpty()) {
                            ipText.append("Your IP Address: ").append(finalLocalIP);
                        } else {
                            ipText.append("IP Address: Unable to determine");
                        }
                        lblIPAddress.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold;");
                    }
                    
                    if (ipText.length() > 0) {
                        lblIPAddress.setText(ipText.toString());
                    } else {
                        lblIPAddress.setText("IP Address: Unable to determine");
                    }
                } catch (Exception e) {
                    lblIPAddress.setText("IP Address: Error");
                    System.err.println("Error updating IP address: " + e.getMessage());
                }
            });
        }).start();
    }
    
    private String getLocalIPAddress() {
        try {
            // Try to get a non-loopback, non-link-local IPv4 address
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    
                    // Skip loopback and link-local addresses
                    if (address.isLoopbackAddress() || address.isLinkLocalAddress()) {
                        continue;
                    }
                    
                    // Prefer IPv4 addresses
                    String hostAddress = address.getHostAddress();
                    if (hostAddress != null && !hostAddress.contains(":")) {
                        return hostAddress;
                    }
                }
            }
            
            // Fallback: try getLocalHost()
            try {
                InetAddress localhost = InetAddress.getLocalHost();
                return localhost.getHostAddress();
            } catch (Exception e) {
                // Ignore
            }
            
            return null;
        } catch (SocketException e) {
            System.err.println("Error getting network interfaces: " + e.getMessage());
            return null;
        }
    }
    
    private void loadAndSetDefaultDeck() {
        DeckManager dm = DeckManager.getInstance();
        // Get the currently selected deck number
        int selectedDeckNumber = dm.getSelectedDeckNumber();
        Deck defaultDeck = dm.loadDeckByNumber(selectedDeckNumber);
        
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
            List<Deck> allDecks = dm.getAllDecks();
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
        
        // Navigate to battle scene
        navigateToBattleScene();
    }
    
    private void navigateToBattleScene() {
        try {
            // Cancel timer
            if (updateTimer != null) {
                updateTimer.cancel();
            }
            
            // Load battle scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/kuroyale/scenes/BattleScene.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 720);
            scene.getRoot().setStyle("-fx-background-color: BD7FFF;");
            
            // Get stage
            Stage stage = null;
            if (btnStartGame != null && btnStartGame.getScene() != null) {
                stage = (Stage) btnStartGame.getScene().getWindow();
            } else if (btnLeave != null && btnLeave.getScene() != null) {
                stage = (Stage) btnLeave.getScene().getWindow();
            } else if (listPlayers != null && listPlayers.getScene() != null) {
                stage = (Stage) listPlayers.getScene().getWindow();
            }
            
            if (stage != null) {
                stage.setScene(scene);
                stage.setTitle("KURoyale - Network Battle");
            } else {
                System.err.println("Error: Could not get stage to navigate to battle scene");
            }
        } catch (IOException e) {
            System.err.println("Error navigating to battle scene: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                lblStatus.setText("Error: Failed to start game");
            });
        }
    }
    
    private void resetLobbyState() {
        // Reset network state when entering lobby
        if (networkManager != null) {
            // Reset game started flag in host/client
            networkManager.resetLobbyState();
        }
    }
    
    @FXML
    private void btnLeaveClicked() {
        if (updateTimer != null) {
            updateTimer.cancel();
        }
        
        // Clear lobby handler when leaving lobby
        if (networkManager != null) {
            networkManager.clearLobbyMessageHandler();
        }
        
        // Reset handler registration flag
        battleHandlerRegistered = false;
        
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

