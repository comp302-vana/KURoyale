package kuroyale.mainpack.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import kuroyale.mainpack.UIManager;

public class NetworkConnectionDialogController {
    @FXML
    private TextField txtPlayerName;
    @FXML
    private TextField txtHostPort;
    @FXML
    private TextField txtHostAddress;
    @FXML
    private TextField txtClientPort;
    @FXML
    private Button btnHost;
    @FXML
    private Button btnJoinLobby;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblPublicIP;
    @FXML
    private Label lblInstructions;
    @FXML
    private RadioButton radioLocal;
    @FXML
    private RadioButton radioInternet;
    @FXML
    private Label lblConnectionInfo;
    
    private Stage dialogStage;
    private ToggleGroup connectionTypeGroup;
    private String detectedLocalIP = null; // Store detected local IP for auto-fill
    
    public void setStage(Stage stage) {
        this.dialogStage = stage;
        
        // Setup toggle group for connection type
        connectionTypeGroup = new ToggleGroup();
        radioLocal.setToggleGroup(connectionTypeGroup);
        radioInternet.setToggleGroup(connectionTypeGroup);
        radioLocal.setSelected(true); // Default to local
        
        // Update info when selection changes
        radioLocal.setOnAction(e -> updateConnectionInfo());
        radioInternet.setOnAction(e -> updateConnectionInfo());
        
        // Initial info update
        updateConnectionInfo();
    }
    
    private void updateConnectionInfo() {
        if (radioLocal.isSelected()) {
            // Local network mode - show local IP info
            lblConnectionInfo.setText("Direct connection - same WiFi/router");
            lblConnectionInfo.setStyle("-fx-text-fill: #87CEEB; -fx-font-size: 12px;");
            loadPublicIP(); // Still show IPs for reference
            // Enable host IP field for joining
            if (txtHostAddress != null) {
                txtHostAddress.setDisable(false);
                // Auto-fill localhost as default (same computer) or show local IP hint
                if (txtHostAddress.getText().trim().isEmpty()) {
                    txtHostAddress.setText("127.0.0.1"); // Default to localhost (same computer)
                    txtHostAddress.setPromptText("Host IP (default: 127.0.0.1 for same computer, or use IP shown above)");
                } else {
                    txtHostAddress.setPromptText("Host IP (127.0.0.1 for same computer, or IP shown above)");
                }
            }
        } else {
            // Internet mode - show relay info
            String relayIP = NetworkManager.getRelayServerIP();
            int relayPort = NetworkManager.getRelayServerPort();
            lblConnectionInfo.setText("Using relay server: " + relayIP + ":" + relayPort + "\nNo port forwarding needed!");
            lblConnectionInfo.setStyle("-fx-text-fill: #90EE90; -fx-font-size: 12px;");
            showRelayInfo();
            // Disable host IP field for joining (not needed in relay mode)
            if (txtHostAddress != null) {
                txtHostAddress.setDisable(true);
                txtHostAddress.setPromptText("Not needed (using relay server)");
                txtHostAddress.clear();
            }
        }
    }
    
    private void showRelayInfo() {
        Platform.runLater(() -> {
            if (lblPublicIP != null) {
                String relayIP = NetworkManager.getRelayServerIP();
                int relayPort = NetworkManager.getRelayServerPort();
                
                StringBuilder info = new StringBuilder();
                info.append("Relay Server Mode\n");
                info.append("Relay: ").append(relayIP).append(":").append(relayPort).append("\n");
                info.append("Connecting via Oracle Cloud server");
                
                lblPublicIP.setText(info.toString());
                lblPublicIP.setStyle("-fx-text-fill: #90EE90; -fx-font-size: 12px; -fx-font-weight: bold;");
                lblPublicIP.setVisible(true);
            }
            
            if (lblInstructions != null) {
                lblInstructions.setText("No port forwarding needed! Works across any network.");
                lblInstructions.setStyle("-fx-text-fill: #87CEEB; -fx-font-size: 11px;");
            }
        });
    }
    
    private void loadPublicIP() {
        new Thread(() -> {
            String localIP = getLocalIPAddress();
            String publicIP = null;
            
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
                            System.out.println("Successfully retrieved public IP: " + publicIP);
                            break; // Success, stop trying other services
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to get public IP from " + serviceUrl + ": " + e.getMessage());
                    // Try next service
                }
            }
            
            final String finalLocalIP = localIP;
            final String finalPublicIP = publicIP;
            
            // Store local IP for potential auto-fill
            detectedLocalIP = finalLocalIP;
            
            Platform.runLater(() -> {
                if (lblPublicIP != null) {
                    StringBuilder ipText = new StringBuilder();
                    
                    if (finalLocalIP != null && !finalLocalIP.isEmpty()) {
                        ipText.append("Local IP (same network): ").append(finalLocalIP).append("\n");
                        ipText.append("  → Use this if you're on the same WiFi/router\n");
                        ipText.append("  → Or use 127.0.0.1 if on the same computer\n\n");
                    } else {
                        ipText.append("Local IP: Not detected\n");
                        ipText.append("  → Use 127.0.0.1 for same computer\n\n");
                    }
                    
                    if (finalPublicIP != null && !finalPublicIP.isEmpty()) {
                        ipText.append("Public IP (internet): ").append(finalPublicIP).append("\n");
                        ipText.append("  → Use this for internet play (requires port forwarding)");
                        lblPublicIP.setStyle("-fx-text-fill: #90EE90; -fx-font-size: 12px; -fx-font-weight: bold;");
                    } else {
                        ipText.append("Public IP: Unable to detect automatically\n");
                        ipText.append("  → Visit whatismyip.com to find your public IP");
                        lblPublicIP.setStyle("-fx-text-fill: #FFA500; -fx-font-size: 11px;");
                    }
                    
                    lblPublicIP.setText(ipText.toString());
                    lblPublicIP.setVisible(true);
                    System.out.println("Updated IP label with: Local=" + finalLocalIP + ", Public=" + finalPublicIP);
                } else {
                    System.err.println("ERROR: lblPublicIP is null!");
                }
                
                if (lblInstructions != null) {
                    if (finalPublicIP != null && !finalPublicIP.isEmpty()) {
                        lblInstructions.setText("⚠ For internet play: Forward port 8080 on your router to this computer");
                        lblInstructions.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 11px;");
                    } else {
                        lblInstructions.setText("💡 Tip: Use Local IP for same network, Public IP for internet");
                        lblInstructions.setStyle("-fx-text-fill: #87CEEB; -fx-font-size: 11px;");
                    }
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
    
    @FXML
    private void btnHostClicked() {
        String playerName = txtPlayerName.getText().trim();
        String portText = txtHostPort.getText().trim();
        
        if (playerName.isEmpty()) {
            lblStatus.setText("Please enter your name");
            return;
        }
        
        int port;
        try {
            port = portText.isEmpty() ? 8080 : Integer.parseInt(portText);
            if (port < 1024 || port > 65535) {
                lblStatus.setText("Port must be between 1024 and 65535");
                return;
            }
        } catch (NumberFormatException e) {
            lblStatus.setText("Invalid port number");
            return;
        }
        
        btnHost.setDisable(true);
        btnJoinLobby.setDisable(true);
        lblStatus.setText("Creating lobby...");
        
        // Use selection from radio buttons
        boolean useInternet = radioInternet.isSelected();
        
        // Start host in background thread
        new Thread(() -> {
            try {
                NetworkManager.getInstance().startHost(port, playerName, null, useInternet);
                
                Platform.runLater(() -> {
                    lblStatus.setText("Lobby created! Waiting for player...");
                    // Navigate to lobby scene
                    try {
                        switchToLobbyScene();
                    } catch (IOException e) {
                        lblStatus.setText("Error: " + e.getMessage());
                        btnHost.setDisable(false);
                        btnJoinLobby.setDisable(false);
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error creating lobby: " + e.getMessage());
                    btnHost.setDisable(false);
                    btnJoinLobby.setDisable(false);
                });
            }
        }).start();
    }
    
    @FXML
    private void btnJoinLobbyClicked() {
        String playerName = txtPlayerName.getText().trim();
        String hostIP = txtHostAddress.getText().trim();
        String portText = txtClientPort.getText().trim();
        
        // Use selection from radio buttons
        boolean useInternet = radioInternet.isSelected();
        
        if (playerName.isEmpty()) {
            lblStatus.setText("Please enter your name");
            return;
        }
        
        // For local mode, host IP is required
        if (!useInternet && (hostIP == null || hostIP.isEmpty())) {
            lblStatus.setText("Please enter host IP address for local connection");
            return;
        }
        
        int port;
        try {
            port = portText.isEmpty() ? 8080 : Integer.parseInt(portText);
            if (port < 1024 || port > 65535) {
                lblStatus.setText("Port must be between 1024 and 65535");
                return;
            }
        } catch (NumberFormatException e) {
            lblStatus.setText("Invalid port number");
            return;
        }
        
        btnHost.setDisable(true);
        btnJoinLobby.setDisable(true);
        lblStatus.setText("Connecting...");
        
        // Connect to host in background thread
        new Thread(() -> {
            try {
                NetworkManager.getInstance().startClient(hostIP, port, playerName, null, useInternet);
                
                Platform.runLater(() -> {
                    lblStatus.setText("Connected!");
                    // Navigate to lobby scene
                    try {
                        switchToLobbyScene();
                    } catch (IOException e) {
                        lblStatus.setText("Error: " + e.getMessage());
                        btnHost.setDisable(false);
                        btnJoinLobby.setDisable(false);
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Connection failed: " + e.getMessage());
                    btnHost.setDisable(false);
                    btnJoinLobby.setDisable(false);
                });
            }
        }).start();
    }
    
    private void switchToLobbyScene() throws IOException {
        // Get the main application stage BEFORE closing the dialog
        Stage mainStage = null;
        if (dialogStage != null && dialogStage.getOwner() != null) {
            mainStage = (Stage) dialogStage.getOwner();
        }
        
        // Close the connection dialog
        if (dialogStage != null) {
            dialogStage.close();
        }
        
        // Load and show lobby in main stage
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/kuroyale/scenes/NetworkLobbyScene.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 1280, 720);
        scene.getRoot().setStyle("-fx-background-color: BD7FFF;");
        
        if (mainStage != null) {
            mainStage.setScene(scene);
            mainStage.setTitle("Network Lobby");
            mainStage.setWidth(1280);
            mainStage.setHeight(720);
            mainStage.show();
        } else {
            // Fallback: create new stage if we can't find main stage
            Stage newStage = new Stage();
            newStage.setScene(scene);
            newStage.setTitle("Network Lobby");
            newStage.setWidth(1280);
            newStage.setHeight(720);
            newStage.show();
        }
    }
}

