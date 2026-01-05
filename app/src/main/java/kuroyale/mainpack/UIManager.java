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
import kuroyale.mainpack.models.GameMode;

public class UIManager extends Application {
    private Stage stage;
    private Scene scene;
    private Parent root;
    private static String selectedDifficulty = null;

    @FXML
    private ImageView arenaPreviewImage;

    @FXML
    private javafx.scene.control.Label chestCountLabel;

    @FXML
    private void initialize() {
        refreshArenaPreviewIfPresent();
        refreshChestCountLabel();
    }

    private void refreshChestCountLabel() {
        if (chestCountLabel == null)
            return;

        kuroyale.mainpack.managers.PersistenceManager pm = new kuroyale.mainpack.managers.PersistenceManager();
        kuroyale.mainpack.models.PlayerProfile profile = pm.loadPlayerProfile();
        chestCountLabel.setText("x" + profile.getChestCount());
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
            Parent root = FXMLLoader.load(UIManager.class.getResource("/kuroyale/scenes/StartScene.fxml"));
            Scene scene = new Scene(root, 1280, 720);
            root.setStyle("-fx-background-color: BD7FFF;");
            stage.setResizable(false);
            stage.setTitle("KURoyale");
            stage.getIcons().add(new Image("/kuroyale/images/icon.png"));

            // Add global key listener for debug console
            addDebugConsoleKeyListener(scene);

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
        }
    }

    private static void addDebugConsoleKeyListener(Scene scene) {
        scene.setOnKeyPressed(event -> {
            // Check for " key (QUOTE character)
            if (event.getText().equals("\"") || event.getText().equals("'")) {
                showDebugConsole(scene);
            }
        });
    }

    private static void showDebugConsole(Scene scene) {
        javafx.scene.layout.VBox consoleBox = new javafx.scene.layout.VBox(10);
        consoleBox.setAlignment(javafx.geometry.Pos.CENTER);
        consoleBox.setMaxWidth(400);
        consoleBox.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.95); " +
                        "-fx-padding: 30; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: #00FF00; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 15;");

        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("DEBUG CONSOLE");
        titleLabel.setStyle("-fx-text-fill: #00FF00; -fx-font-weight: bold; -fx-font-size: 18;");

        javafx.scene.control.Label instructionLabel = new javafx.scene.control.Label(
                "Enter command (e.g., 'addchests 10'):");
        instructionLabel.setStyle("-fx-text-fill: #CCCCCC; -fx-font-size: 12;");

        javafx.scene.control.TextField commandField = new javafx.scene.control.TextField();
        commandField.setPromptText("Command...");
        commandField.setStyle(
                "-fx-background-color: #333333; " +
                        "-fx-text-fill: #00FF00; " +
                        "-fx-prompt-text-fill: #666666; " +
                        "-fx-font-family: 'Consolas'; " +
                        "-fx-font-size: 14;");

        javafx.scene.control.Label resultLabel = new javafx.scene.control.Label("");
        resultLabel.setStyle("-fx-text-fill: #FFFF00; -fx-font-size: 12;");

        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.control.Button executeBtn = new javafx.scene.control.Button("Execute");
        executeBtn.setStyle("-fx-background-color: #00AA00; -fx-text-fill: white; -fx-font-weight: bold;");

        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("Close");
        closeBtn.setStyle("-fx-background-color: #AA0000; -fx-text-fill: white; -fx-font-weight: bold;");

        buttonBox.getChildren().addAll(executeBtn, closeBtn);
        consoleBox.getChildren().addAll(titleLabel, instructionLabel, commandField, resultLabel, buttonBox);

        javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.7);");
        overlay.getChildren().add(consoleBox);

        // Execute command
        executeBtn.setOnAction(e -> {
            String result = executeCommand(commandField.getText());
            resultLabel.setText(result);
            refreshChestCountLabelInScene(scene);
        });

        commandField.setOnAction(e -> {
            String result = executeCommand(commandField.getText());
            resultLabel.setText(result);
            refreshChestCountLabelInScene(scene);
        });

        // Close console
        closeBtn.setOnAction(e -> {
            Parent sceneRoot = scene.getRoot();
            if (sceneRoot instanceof javafx.scene.layout.Pane) {
                ((javafx.scene.layout.Pane) sceneRoot).getChildren().remove(overlay);
            }
        });

        // Add to scene
        Parent sceneRoot = scene.getRoot();
        if (sceneRoot instanceof javafx.scene.layout.AnchorPane) {
            javafx.scene.layout.AnchorPane anchorRoot = (javafx.scene.layout.AnchorPane) sceneRoot;
            anchorRoot.getChildren().add(overlay);
            javafx.scene.layout.AnchorPane.setTopAnchor(overlay, 0.0);
            javafx.scene.layout.AnchorPane.setBottomAnchor(overlay, 0.0);
            javafx.scene.layout.AnchorPane.setLeftAnchor(overlay, 0.0);
            javafx.scene.layout.AnchorPane.setRightAnchor(overlay, 0.0);
        } else if (sceneRoot instanceof javafx.scene.layout.Pane) {
            ((javafx.scene.layout.Pane) sceneRoot).getChildren().add(overlay);
        }

        commandField.requestFocus();
    }

    private static String executeCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "No command entered";
        }

        String[] parts = command.trim().toLowerCase().split("\\s+");
        String cmd = parts[0];

        try {
            kuroyale.mainpack.managers.PersistenceManager pm = new kuroyale.mainpack.managers.PersistenceManager();
            kuroyale.mainpack.models.PlayerProfile profile = pm.loadPlayerProfile();

            switch (cmd) {
                case "addchests":
                case "addchest":
                    if (parts.length < 2)
                        return "Usage: addchests <amount>";
                    int chestAmount = Integer.parseInt(parts[1]);
                    for (int i = 0; i < chestAmount; i++) {
                        profile.addChest();
                    }
                    pm.savePlayerProfile(profile);
                    return "Added " + chestAmount + " chests. Total: " + profile.getChestCount();

                case "setchests":
                case "setchest":
                    if (parts.length < 2)
                        return "Usage: setchests <amount>";
                    int setAmount = Integer.parseInt(parts[1]);
                    profile.setChestCount(setAmount);
                    pm.savePlayerProfile(profile);
                    return "Set chests to " + setAmount;

                case "addgold":
                    if (parts.length < 2)
                        return "Usage: addgold <amount>";
                    int goldAmount = Integer.parseInt(parts[1]);
                    profile.setTotalGold(profile.getTotalGold() + goldAmount);
                    pm.savePlayerProfile(profile);
                    return "Added " + goldAmount + " gold. Total: " + profile.getTotalGold();

                case "unlockall":
                    for (int i = 1; i <= 28; i++) {
                        profile.unlockCard(i);
                    }
                    pm.savePlayerProfile(profile);
                    return "All 28 cards unlocked!";

                case "resetcards":
                case "lockcards":
                    // Reset to only starter cards (1-8)
                    java.util.Set<Integer> starterCards = new java.util.HashSet<>();
                    for (int i = 1; i <= 8; i++) {
                        starterCards.add(i);
                    }
                    profile.setUnlockedCards(starterCards);
                    pm.savePlayerProfile(profile);
                    return "Cards reset to starter 8 only!";

                case "status":
                case "info":
                    return "Gold: " + profile.getTotalGold() + " | Chests: " + profile.getChestCount() +
                            " | Unlocked: " + profile.getUnlockedCards().size() + "/28";

                case "help":
                    return "addchests, setchests, addgold, unlockall, resetcards, status";

                default:
                    return "Unknown command. Type 'help' for commands.";
            }
        } catch (NumberFormatException e) {
            return "Invalid number format";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private static void refreshChestCountLabelInScene(Scene scene) {
        if (scene == null)
            return;
        Parent root = scene.getRoot();
        if (root == null)
            return;

        // Find chestCountLabel by fx:id
        javafx.scene.Node labelNode = root.lookup("#chestCountLabel");
        if (labelNode instanceof javafx.scene.control.Label) {
            kuroyale.mainpack.managers.PersistenceManager pm = new kuroyale.mainpack.managers.PersistenceManager();
            kuroyale.mainpack.models.PlayerProfile profile = pm.loadPlayerProfile();
            ((javafx.scene.control.Label) labelNode).setText("x" + profile.getChestCount());
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
        int selectedDeckNumber = kuroyale.deckpack.DeckManager.getSelectedDeckNumber();
        int cardCount = kuroyale.deckpack.DeckManager.getDeckCardCount(selectedDeckNumber);

        if (cardCount != 8) {
            // Redirect to deck builder with flash animation
            switchToDeckBuilderSceneWithFlash(event);
            return;
        }

        switchToDifficultySelectionScene(event);
    }

    @FXML
    void btnNoAIClicked(ActionEvent event) throws IOException {
        selectedDifficulty = "NoAI";
        GameEngine.setGameMode(GameMode.SINGLE_PLAYER_AI);
        switchToBattleScene(event);
    }

    @FXML
    void btnSimpleClicked(ActionEvent event) throws IOException {
        selectedDifficulty = "Simple";
        GameEngine.setGameMode(GameMode.SINGLE_PLAYER_AI);
        switchToBattleScene(event);
    }

    @FXML
    void btnMediumClicked(ActionEvent event) throws IOException {
        selectedDifficulty = "Medium";
        GameEngine.setGameMode(GameMode.SINGLE_PLAYER_AI);
        switchToBattleScene(event);
    }

    @FXML
    void btnAdvancedClicked(ActionEvent event) throws IOException {
        selectedDifficulty = "Advanced";
        GameEngine.setGameMode(GameMode.SINGLE_PLAYER_AI);
        switchToBattleScene(event);
    }

    @FXML
    void btnLocalPvPClicked(ActionEvent event) throws IOException {
        switchToPvPDeckSelectionScene(event);
    }
    
    @FXML
    void btnNetworkMultiplayerClicked(ActionEvent event) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/kuroyale/scenes/NetworkConnectionDialog.fxml"));
            Parent root = loader.load();
            kuroyale.mainpack.network.NetworkConnectionDialogController controller = loader.getController();
            
            Stage dialog = new Stage();
            controller.setStage(dialog);
            dialog.setTitle("Network Multiplayer");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.initOwner((Stage) ((Node) event.getSource()).getScene().getWindow());
            dialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        addDebugConsoleKeyListener(scene);
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

    private void switchToDeckBuilderSceneWithFlash(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/kuroyale/scenes/DeckBuilderScene.fxml"));
        root = loader.load();

        // Get controller and trigger flash animation
        Object controller = loader.getController();
        if (controller instanceof DeckBuilder deckBuilder) {
            deckBuilder.triggerDeckSlotsFlash();
        }

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

    private void switchToPvPDeckSelectionScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/PvPDeckSelectionScene.fxml"));
        root.setStyle("-fx-background-color: BD7FFF;");
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root, 1280, 720, Color.web("0xBD7FFF"));
        stage.setScene(scene);
        stage.show();
    }

    public static String getSelectedDifficulty() {
        return selectedDifficulty;
    }

    // ===== Chest Opening =====

    @FXML
    void btnOpenChestClicked(ActionEvent event) {
        // Load player profile
        kuroyale.mainpack.managers.PersistenceManager persistenceManager = new kuroyale.mainpack.managers.PersistenceManager();
        kuroyale.mainpack.models.PlayerProfile playerProfile = persistenceManager.loadPlayerProfile();

        // Check if player has chests
        if (playerProfile.getChestCount() <= 0) {
            showNoChestPopup(event);
            return;
        }

        // Consume one chest
        playerProfile.consumeChest();

        // Open chest and get rewards
        kuroyale.mainpack.models.ChestReward reward = kuroyale.mainpack.managers.ChestManager
                .openBasicChest(playerProfile);

        // Save profile after chest opening
        persistenceManager.savePlayerProfile(playerProfile);

        // Refresh chest count label
        Node source = (Node) event.getSource();
        if (source != null && source.getScene() != null) {
            refreshChestCountLabelInScene(source.getScene());
        }

        // Show reward popup
        showChestRewardPopup(event, reward);
    }

    private void showNoChestPopup(ActionEvent event) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("No Chests");
        alert.setHeaderText("No Chests Available");
        alert.setContentText("Win battles to earn chests!");
        alert.showAndWait();
    }

    private void showChestRewardPopup(ActionEvent event, kuroyale.mainpack.models.ChestReward reward) {
        javafx.scene.layout.StackPane modalOverlay = new javafx.scene.layout.StackPane();
        modalOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);");

        javafx.scene.layout.VBox rewardPanel = new javafx.scene.layout.VBox(20);
        rewardPanel.setAlignment(javafx.geometry.Pos.CENTER);
        rewardPanel.setMaxWidth(400);
        rewardPanel.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #9C27B0, #673AB7); " +
                        "-fx-background-radius: 20; " +
                        "-fx-padding: 40; " +
                        "-fx-border-color: #FFD700; " +
                        "-fx-border-width: 4; " +
                        "-fx-border-radius: 20;");

        // Title
        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("*** Chest Opened! ***");
        titleLabel.setFont(javafx.scene.text.Font.font("Trebuchet MS", javafx.scene.text.FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.WHITE);

        // Gold reward
        javafx.scene.control.Label goldRewardLabel = new javafx.scene.control.Label(
                "+" + reward.getGoldAmount() + " Gold");
        goldRewardLabel.setFont(javafx.scene.text.Font.font("Trebuchet MS", javafx.scene.text.FontWeight.BOLD, 24));
        goldRewardLabel.setTextFill(Color.GOLD);

        rewardPanel.getChildren().addAll(titleLabel, goldRewardLabel);

        // Button for first stage
        javafx.scene.control.Button actionButton = new javafx.scene.control.Button();
        actionButton.setFont(javafx.scene.text.Font.font("Trebuchet MS", javafx.scene.text.FontWeight.BOLD, 16));
        actionButton.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 15 50; " +
                        "-fx-background-radius: 10; " +
                        "-fx-cursor: hand;");

        if (reward.hasCard()) {
            // Stage 1: Show gold, then continue to card
            actionButton.setText("Continue");
            actionButton.setOnAction(e -> {
                // Clear panel and show card stage
                rewardPanel.getChildren().clear();

                javafx.scene.control.Label cardTitleLabel = new javafx.scene.control.Label("Card Reward!");
                cardTitleLabel
                        .setFont(javafx.scene.text.Font.font("Trebuchet MS", javafx.scene.text.FontWeight.BOLD, 28));
                cardTitleLabel.setTextFill(Color.WHITE);

                javafx.scene.control.Label cardLabel;
                if (reward.isNewCard()) {
                    cardLabel = new javafx.scene.control.Label("*** NEW CARD UNLOCKED! ***");
                    cardLabel.setTextFill(Color.LIME);
                } else {
                    cardLabel = new javafx.scene.control.Label("Card Found:");
                    cardLabel.setTextFill(Color.WHITE);
                }
                cardLabel.setFont(javafx.scene.text.Font.font("Trebuchet MS", javafx.scene.text.FontWeight.BOLD, 18));

                // Card name with rarity color
                kuroyale.cardpack.Card card = reward.getUnlockedCard();
                kuroyale.cardpack.CardRarity rarity = kuroyale.cardpack.CardRarityMapper.getRarity(card.getId());
                String rarityColor = kuroyale.mainpack.managers.CardVisualManager.getRarityBorderColor(rarity);

                javafx.scene.control.Label cardNameLabel = new javafx.scene.control.Label(card.getName());
                cardNameLabel
                        .setFont(javafx.scene.text.Font.font("Trebuchet MS", javafx.scene.text.FontWeight.BOLD, 22));
                cardNameLabel.setStyle("-fx-text-fill: " + rarityColor + ";");

                javafx.scene.control.Label rarityLabel = new javafx.scene.control.Label(rarity.name());
                rarityLabel
                        .setFont(javafx.scene.text.Font.font("Trebuchet MS", javafx.scene.text.FontWeight.NORMAL, 14));
                rarityLabel.setStyle("-fx-text-fill: " + rarityColor
                        + "; -fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 5; -fx-padding: 5 15;");

                // Collect button for final stage
                javafx.scene.control.Button collectButton = new javafx.scene.control.Button("Collect!");
                collectButton
                        .setFont(javafx.scene.text.Font.font("Trebuchet MS", javafx.scene.text.FontWeight.BOLD, 16));
                collectButton.setStyle(
                        "-fx-background-color: #4CAF50; " +
                                "-fx-text-fill: white; " +
                                "-fx-padding: 15 50; " +
                                "-fx-background-radius: 10; " +
                                "-fx-cursor: hand;");
                collectButton.setOnAction(ev -> {
                    Node source = (Node) event.getSource();
                    Parent sceneRoot = source.getScene().getRoot();
                    if (sceneRoot instanceof javafx.scene.layout.Pane) {
                        ((javafx.scene.layout.Pane) sceneRoot).getChildren().remove(modalOverlay);
                    }
                });

                rewardPanel.getChildren().addAll(cardTitleLabel, cardLabel, cardNameLabel, rarityLabel, collectButton);
            });
        } else {
            // No card, just gold - go straight to collect
            actionButton.setText("Collect!");
            actionButton.setOnAction(e -> {
                Node source = (Node) event.getSource();
                Parent sceneRoot = source.getScene().getRoot();
                if (sceneRoot instanceof javafx.scene.layout.Pane) {
                    ((javafx.scene.layout.Pane) sceneRoot).getChildren().remove(modalOverlay);
                }
            });
        }

        rewardPanel.getChildren().add(actionButton);
        modalOverlay.getChildren().add(rewardPanel);

        // Add to scene
        Node source = (Node) event.getSource();
        Parent sceneRoot = source.getScene().getRoot();
        if (sceneRoot instanceof javafx.scene.layout.StackPane) {
            ((javafx.scene.layout.StackPane) sceneRoot).getChildren().add(modalOverlay);
        } else if (sceneRoot instanceof javafx.scene.layout.AnchorPane) {
            javafx.scene.layout.AnchorPane anchorRoot = (javafx.scene.layout.AnchorPane) sceneRoot;
            anchorRoot.getChildren().add(modalOverlay);
            javafx.scene.layout.AnchorPane.setTopAnchor(modalOverlay, 0.0);
            javafx.scene.layout.AnchorPane.setBottomAnchor(modalOverlay, 0.0);
            javafx.scene.layout.AnchorPane.setLeftAnchor(modalOverlay, 0.0);
            javafx.scene.layout.AnchorPane.setRightAnchor(modalOverlay, 0.0);
        } else if (sceneRoot instanceof javafx.scene.layout.Pane) {
            ((javafx.scene.layout.Pane) sceneRoot).getChildren().add(modalOverlay);
        }
    }
}