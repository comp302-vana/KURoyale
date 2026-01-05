package kuroyale.mainpack.network;

import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    
    private Stage dialogStage;
    
    public void setStage(Stage stage) {
        this.dialogStage = stage;
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
        
        // Start host in background thread
        new Thread(() -> {
            try {
                NetworkManager.getInstance().startHost(port, playerName, null);
                
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
        
        if (playerName.isEmpty()) {
            lblStatus.setText("Please enter your name");
            return;
        }
        
        if (hostIP.isEmpty()) {
            lblStatus.setText("Please enter host IP address");
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
                NetworkManager.getInstance().startClient(hostIP, port, playerName, null);
                
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

