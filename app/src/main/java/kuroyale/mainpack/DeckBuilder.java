package kuroyale.mainpack;

import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardFactory;
import kuroyale.cardpack.CardRarity;
import kuroyale.cardpack.CardRarityMapper;
import kuroyale.deckpack.Deck;
import kuroyale.deckpack.DeckManager;
import kuroyale.cardpack.subclasses.*;
import kuroyale.mainpack.managers.*;
import kuroyale.mainpack.models.PlayerProfile;
import kuroyale.mainpack.util.CardDataRepository;
import kuroyale.mainpack.util.CardStats;

public class DeckBuilder {
    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    private ScrollPane cardScrollPane;
    @FXML
    private FlowPane cardContainer;
    @FXML
    private FlowPane deckSlots;
    @FXML
    private Button btnSaveDeck;
    @FXML
    private TextField deckNameField;
    @FXML
    private ComboBox<String> deckSelector;
    @FXML
    private Label goldLabel; // Gold display
    // @FXML
    // private Label deckStatusLabel;

    private Deck currentDeck;
    private ObservableList<Card> deckCards;
    
    // Managers for economy and upgrades
    private PersistenceManager persistenceManager;
    private EconomyManager economyManager;
    private CardDataRepository cardDataRepository;
    private CardUpgradeManager cardUpgradeManager;

    @FXML
    public void initialize() {
        if (cardScrollPane == null || cardContainer == null || deckSlots == null) {
            System.err.println("ERROR: FXML fields not injected! Check FXML file.");
            return;
        }

        deckCards = FXCollections.observableArrayList();
        currentDeck = new Deck("New Deck");

        // Initialize persistence and economy managers
        initializeManagers();

        setupCardDisplay();
        setupDeckSlots();
        setupDeckSelector();
        updateUI();
    }
    
    private void initializeManagers() {
        // Load player profile
        persistenceManager = new PersistenceManager();
        PlayerProfile profile = persistenceManager.loadPlayerProfile();
        
        // Initialize managers
        economyManager = new EconomyManager(profile.getTotalGold(), persistenceManager);
        cardDataRepository = new CardDataRepository(profile.getCardLevels());
        cardUpgradeManager = new CardUpgradeManager(economyManager, cardDataRepository, persistenceManager);
        
        // Setup gold display
        if (goldLabel != null) {
            goldLabel.setText("Gold: " + economyManager.getCurrentGold());
            // Listen for gold changes
            economyManager.currentGoldProperty().addListener((obs, oldVal, newVal) -> {
                goldLabel.setText("Gold: " + newVal.intValue());
            });
        }
    }

    private void setupCardDisplay() {
        if (cardContainer == null) {
            return;
        }

        cardContainer.setHgap(8);
        cardContainer.setVgap(10);
        cardContainer.setPrefWrapLength(760);

        // create buttons for all cards (using level 1 for display, actual level from repository)
        for (Card card : CardFactory.getAllCards()) {
            AnchorPane cardButton = createCardNode(card);
            // Store card ID in userData for later reference
            cardButton.setUserData(card.getId());
            cardContainer.getChildren().add(cardButton);
        }

        if (cardScrollPane != null) {
            cardScrollPane.setContent(cardContainer);
            cardScrollPane.setFitToWidth(true);
        }
    }

    private AnchorPane createCardNode(Card card) {
        // Get card level and rarity for visual enhancements
        int cardLevel = cardDataRepository != null ? cardDataRepository.getCardLevel(card.getId()) : 1;
        CardRarity rarity = CardRarityMapper.getRarity(card.getId());
        
        // --- BUTTON (the card) ---
        Button btn = new Button();
        String borderColor = CardVisualManager.getRarityBorderColor(rarity);
        btn.setStyle(
                "-fx-background-image: url(\"/kuroyale/images/cards/" + card.getName().toLowerCase().replaceAll(" ", "") + ".png\");" +
                "-fx-background-size: cover;" +
                "-fx-background-color: #5D3F7F;" +
                "-fx-background-radius: 5;" +
                "-fx-padding: 0;" +
                "-fx-border-color: " + borderColor + "; " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 5;");
        btn.setOnAction(e -> addCardToDeck(card));

        VBox hoverButtons = new VBox(5);
        hoverButtons.setAlignment(Pos.CENTER);
        hoverButtons.setStyle("-fx-background-color: transparent; -fx-padding: 5;");
        hoverButtons.setVisible(false);
        hoverButtons.setAlignment(Pos.CENTER);

        HBox hbox = new HBox(5);
        hbox.setAlignment(Pos.CENTER);
        hbox.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 5;");
        hbox.setAlignment(Pos.CENTER);

        Label lblName = new Label(card.getName());
        lblName.setFont(new Font("Trebuchet MS", 12));
        lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Button btnAdd = new Button("Add");
        btnAdd.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 10px; " +
                        "-fx-cursor: hand;");
        btnAdd.setOnAction(e -> {
            addCardToDeck(card);
            e.consume();
        });

        Button btnView = new Button("View");
        btnView.setStyle(
                "-fx-background-color: #2196F3; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 10px; " +
                        "-fx-cursor: hand;");
        btnView.setOnAction(e -> {
            viewCardDetails(card);
            e.consume();
        });

        hbox.getChildren().addAll(btnAdd, btnView);
        hoverButtons.getChildren().addAll(lblName, hbox);

        // --- BADGE: circle + cost number ---
        double radius = 15;

        Circle circle = new Circle(radius);
        circle.setStyle("-fx-fill: #ff1fff; -fx-stroke: #4d3d4d; -fx-stroke-width: 3;");

        Label costLabel = new Label(String.valueOf(card.getCost()));
        costLabel.setTextFill(Color.WHITE);
        costLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 14));
        costLabel.setAlignment(Pos.CENTER);
        costLabel.setPrefWidth(10);
        costLabel.setPrefHeight(16);

        // so clicks on badge still click the button
        circle.setMouseTransparent(true);
        costLabel.setMouseTransparent(true);

        // --- CONTAINER ---
        AnchorPane ap = new AnchorPane();

        // add in the order you described
        ap.getChildren().addAll(btn, hoverButtons, circle, costLabel);

        // circle at (0,0)
        AnchorPane.setTopAnchor(circle, 0.0);
        AnchorPane.setLeftAnchor(circle, 0.0);

        // label centered in the circle:
        // top = r - h/2 = 15 - 16/2 = 7
        // left = r - w/2 = 15 - 10/2 = 10
        AnchorPane.setTopAnchor(costLabel, 8.5);
        AnchorPane.setLeftAnchor(costLabel, 11.5);

        // button fills rest, offset by radius
        AnchorPane.setTopAnchor(btn, radius/3);
        AnchorPane.setLeftAnchor(btn, radius/3);
        AnchorPane.setRightAnchor(btn, 0.0);
        AnchorPane.setBottomAnchor(btn, 0.0);

        AnchorPane.setBottomAnchor(hoverButtons, 5.0);
        AnchorPane.setLeftAnchor(hoverButtons, 20.0);
        AnchorPane.setRightAnchor(hoverButtons, 15.0);

        // optional: set a fixed card size if you want
        ap.setPrefSize(126, 168);
        
        // Add level indicator
        if (cardDataRepository != null) {
            CardVisualManager.applyLevelIndicator(ap, cardLevel);
        }

        ap.setOnMouseEntered(e -> {
            hoverButtons.setVisible(true);
        });

        ap.setOnMouseExited(e -> {
            hoverButtons.setVisible(false);
        });

        return ap;
    }
    
    private void handleUpgrade(Card card) {
        if (cardUpgradeManager == null || economyManager == null) {
            System.err.println("Managers not initialized");
            return;
        }
        
        CardUpgradeManager.UpgradeResult result = cardUpgradeManager.upgradeCard(card.getId());
        
        if (result.success) {
            // Update UI: refresh card display, gold display
            // Refresh the card node to show new level
            refreshCardDisplay(card.getId());
            
            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Upgrade Success");
            alert.setHeaderText(null);
            alert.setContentText(result.message);
            alert.showAndWait();
        } else {
            // Show error message
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Upgrade Failed");
            alert.setHeaderText(null);
            alert.setContentText(result.message);
            alert.showAndWait();
        }
    }
    
    private void refreshCardDisplay(int cardId) {
        // Find the card node in cardContainer and update its level indicator
        for (Node node : cardContainer.getChildren()) {
            if (node instanceof AnchorPane) {
                AnchorPane cardPane = (AnchorPane) node;
                Object userData = cardPane.getUserData();
                if (userData != null && userData.equals(cardId)) {
                    // Update level indicator for this specific card
                    int newLevel = cardDataRepository.getCardLevel(cardId);
                    CardVisualManager.updateLevelIndicator(cardPane, newLevel);
                    break;
                }
            }
        }
    }

    private void viewCardDetails(Card card) {
        // Get card level and rarity (refresh from repository to ensure we have latest level)
        int cardLevel = cardDataRepository != null ? cardDataRepository.getCardLevel(card.getId()) : 1;
        CardRarity rarity = CardRarityMapper.getRarity(card.getId());
        
        // Check if card can be upgraded (not at max level and has enough gold)
        boolean isMaxLevel = (cardLevel >= 3);
        boolean canUpgrade = !isMaxLevel && cardUpgradeManager != null && cardUpgradeManager.canUpgrade(card.getId());
        int upgradeCost = (!isMaxLevel && cardUpgradeManager != null) ? cardUpgradeManager.getUpgradeCost(card.getId()) : -1;
        int currentGold = economyManager != null ? economyManager.getCurrentGold() : 0;
        
        StackPane modalOverlay = new StackPane();
        modalOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(600);
        scrollPane.setMaxWidth(450);
        
        VBox detailPanel = new VBox(15);
        detailPanel.setAlignment(Pos.CENTER);
        detailPanel.setMaxWidth(400);
        detailPanel.setStyle(
                "-fx-background-color: #FFFFFF; " +
                        "-fx-background-radius: 10; " +
                        "-fx-padding: 30; " +
                        "-fx-border-color: #333; " +
                        "-fx-border-width: 3; " +
                        "-fx-border-radius: 10;");
        
        scrollPane.setContent(detailPanel);

        // Title with rarity badge
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER);
        Label titleLabel = new Label(card.getName());
        titleLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #333;");
        
        Label rarityLabel = new Label(rarity.name());
        rarityLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 12));
        String rarityColor = CardVisualManager.getRarityBorderColor(rarity);
        rarityLabel.setStyle("-fx-text-fill: " + rarityColor + "; -fx-background-color: " + rarityColor + "22; -fx-background-radius: 3; -fx-padding: 3 8;");
        titleBox.getChildren().addAll(titleLabel, rarityLabel);

        javafx.scene.shape.Line separator = new javafx.scene.shape.Line(0, 0, 340, 0);
        separator.setStroke(Color.GRAY);
        
        // Level display
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < cardLevel; i++) {
            stars.append("★");
        }
        Label levelLabel = new Label("Level " + cardLevel + " " + stars.toString());
        levelLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 18));
        levelLabel.setStyle("-fx-text-fill: #FFD700;"); // Gold color for stars

        VBox statsBox = new VBox(10);
        statsBox.setAlignment(Pos.CENTER_LEFT);
        
        // Store upgrade button reference if it exists (will be set later)
        Button upgradeButtonRef = null;

        Label costStat = new Label("Cost: " + card.getCost());
        costStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

        Label descStat = new Label("Description:");
        descStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

        Label descText = new Label(card.getDescription());
        descText.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));
        descText.setWrapText(true);
        descText.setMaxWidth(340);
        descText.setStyle("-fx-text-fill: #555;");

        String type = card.getClass().getSimpleName();

        Label typeStat = new Label("Type: " + type);
        typeStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

        String hp = "";
        String damage = "";
        String hitSpeed = "";
        String range = "";
        String speed = "";
        String lifetime = "";
        String areaDamage = "";
        String radius = "";

        if (card instanceof UnitCard) {
            UnitCard unitCard = (UnitCard) card;
            // Get stats at current level
            int currentHP = CardStats.getHPRounded(card.getId(), cardLevel);
            int currentDamage = CardStats.getDamageRounded(card.getId(), cardLevel);
            hitSpeed = String.valueOf(unitCard.getActSpeed());
            range = String.valueOf(unitCard.getRange());
            speed = unitCard.getSpeed();

            Label hpStat = new Label("HP: " + currentHP);
            hpStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label damageStat = new Label("Damage: " + currentDamage);
            damageStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label hitSpeedStat = new Label("Hit Speed: " + hitSpeed);
            hitSpeedStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label rangeStat = new Label("Range: " + range);
            rangeStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label speedStat = new Label("Speed: " + speed);
            speedStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            statsBox.getChildren().addAll(costStat, typeStat, hpStat, damageStat, hitSpeedStat, rangeStat, speedStat,
                    descStat, descText);
            
            // Add upgrade preview if not at max level (show even if can't afford, button will be disabled)
            if (!isMaxLevel) {
                int nextLevel = cardLevel + 1;
                int nextHP = CardStats.getHPRounded(card.getId(), nextLevel);
                int nextDamage = CardStats.getDamageRounded(card.getId(), nextLevel);
                
                javafx.scene.shape.Line upgradeSeparator = new javafx.scene.shape.Line(0, 0, 340, 0);
                upgradeSeparator.setStroke(Color.GRAY);
                
                Label upgradeTitle = new Label("Upgrade to Level " + nextLevel);
                upgradeTitle.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 16));
                upgradeTitle.setStyle("-fx-text-fill: #4CAF50;");
                
                Label nextHPStat = new Label("HP: " + nextHP + " (+" + ((nextLevel == 2) ? "10%" : "20%") + ")");
                nextHPStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));
                nextHPStat.setStyle("-fx-text-fill: #4CAF50;");
                
                Label nextDamageStat = new Label("Damage: " + nextDamage + " (+" + ((nextLevel == 2) ? "10%" : "20%") + ")");
                nextDamageStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));
                nextDamageStat.setStyle("-fx-text-fill: #4CAF50;");
                
                Label costLabel = new Label("Upgrade Cost: " + upgradeCost + " Gold");
                costLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 14));
                
                Label goldLabel = new Label("Your Gold: " + currentGold);
                goldLabel.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));
                
                Button upgradeButton = new Button("Upgrade");
                boolean canAfford = economyManager != null && economyManager.canAfford(upgradeCost);
                upgradeButton.setDisable(!canAfford || !canUpgrade);
                upgradeButton.setStyle(
                    canAfford && canUpgrade ?
                        "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30; -fx-cursor: hand; -fx-background-radius: 5;" :
                        "-fx-background-color: #CCCCCC; -fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 5;");
                upgradeButton.setOnAction(e -> {
                    handleUpgrade(card);
                    // Close modal after upgrade
                    Parent sceneRoot = cardScrollPane.getScene().getRoot();
                    if (sceneRoot instanceof Pane) {
                        ((Pane) sceneRoot).getChildren().remove(modalOverlay);
                    }
                });
                
                statsBox.getChildren().addAll(upgradeSeparator, upgradeTitle, nextHPStat, nextDamageStat, 
                                             costLabel, goldLabel);
                // Store reference to upgrade button for later alignment with close button
                upgradeButtonRef = upgradeButton;
            }
        }

        if (card instanceof BuildingCard) {
            BuildingCard buildingCard = (BuildingCard) card;
            // Get stats at current level
            int currentHP = CardStats.getHPRounded(card.getId(), cardLevel);
            int currentDamage = CardStats.getDamageRounded(card.getId(), cardLevel);
            range = String.valueOf(buildingCard.getRange());
            lifetime = String.valueOf(buildingCard.getLifetime());

            Label hpStat = new Label("HP: " + currentHP);
            hpStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label damageStat = new Label("Damage: " + currentDamage);
            damageStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label rangeStat = new Label("Range: " + range);
            rangeStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label lifetimeStat = new Label("Lifetime: " + lifetime);
            lifetimeStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            statsBox.getChildren().addAll(costStat, typeStat, hpStat, damageStat, rangeStat, lifetimeStat, descStat,
                    descText);
            
            // Add upgrade preview if not at max level (show even if can't afford, button will be disabled)
            if (!isMaxLevel) {
                int nextLevel = cardLevel + 1;
                int nextHP = CardStats.getHPRounded(card.getId(), nextLevel);
                int nextDamage = CardStats.getDamageRounded(card.getId(), nextLevel);
                
                javafx.scene.shape.Line upgradeSeparator = new javafx.scene.shape.Line(0, 0, 340, 0);
                upgradeSeparator.setStroke(Color.GRAY);
                
                Label upgradeTitle = new Label("Upgrade to Level " + nextLevel);
                upgradeTitle.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 16));
                upgradeTitle.setStyle("-fx-text-fill: #4CAF50;");
                
                Label nextHPStat = new Label("HP: " + nextHP + " (+" + ((nextLevel == 2) ? "10%" : "20%") + ")");
                nextHPStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));
                nextHPStat.setStyle("-fx-text-fill: #4CAF50;");
                
                Label nextDamageStat = new Label("Damage: " + nextDamage + " (+" + ((nextLevel == 2) ? "10%" : "20%") + ")");
                nextDamageStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));
                nextDamageStat.setStyle("-fx-text-fill: #4CAF50;");
                
                Label costLabel = new Label("Upgrade Cost: " + upgradeCost + " Gold");
                costLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 14));
                
                Label goldLabel = new Label("Your Gold: " + currentGold);
                goldLabel.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));
                
                Button upgradeButton = new Button("Upgrade");
                boolean canAfford = economyManager != null && economyManager.canAfford(upgradeCost);
                upgradeButton.setDisable(!canAfford || !canUpgrade);
                upgradeButton.setStyle(
                    canAfford && canUpgrade ?
                        "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30; -fx-cursor: hand; -fx-background-radius: 5;" :
                        "-fx-background-color: #CCCCCC; -fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 5;");
                upgradeButton.setOnAction(e -> {
                    handleUpgrade(card);
                    // Close modal after upgrade
                    Parent sceneRoot = cardScrollPane.getScene().getRoot();
                    if (sceneRoot instanceof Pane) {
                        ((Pane) sceneRoot).getChildren().remove(modalOverlay);
                    }
                });
                
                statsBox.getChildren().addAll(upgradeSeparator, upgradeTitle, nextHPStat, nextDamageStat, 
                                             costLabel, goldLabel);
                // Store reference to upgrade button for later alignment with close button
                upgradeButtonRef = upgradeButton;
            }
        }

        if (card instanceof SpellCard) {
            SpellCard spellCard = (SpellCard) card;
            // Get stats at current level
            int currentDamage = CardStats.getDamageRounded(card.getId(), cardLevel);
            radius = String.valueOf(spellCard.getRadius());

            Label areaDamageStat = new Label("Area Damage: " + currentDamage);
            areaDamageStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label radiusStat = new Label("Radius: " + radius);
            radiusStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            statsBox.getChildren().addAll(costStat, typeStat, areaDamageStat, radiusStat, descStat, descText);
            
            // Add upgrade preview if not at max level (show even if can't afford, button will be disabled)
            if (!isMaxLevel) {
                int nextLevel = cardLevel + 1;
                int nextDamage = CardStats.getDamageRounded(card.getId(), nextLevel);
                
                javafx.scene.shape.Line upgradeSeparator = new javafx.scene.shape.Line(0, 0, 340, 0);
                upgradeSeparator.setStroke(Color.GRAY);
                
                Label upgradeTitle = new Label("Upgrade to Level " + nextLevel);
                upgradeTitle.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 16));
                upgradeTitle.setStyle("-fx-text-fill: #4CAF50;");
                
                Label nextDamageStat = new Label("Area Damage: " + nextDamage + " (+" + ((nextLevel == 2) ? "10%" : "20%") + ")");
                nextDamageStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));
                nextDamageStat.setStyle("-fx-text-fill: #4CAF50;");
                
                Label costLabel = new Label("Upgrade Cost: " + upgradeCost + " Gold");
                costLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 14));
                
                Label goldLabel = new Label("Your Gold: " + currentGold);
                goldLabel.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));
                
                Button upgradeButton = new Button("Upgrade");
                boolean canAfford = economyManager != null && economyManager.canAfford(upgradeCost);
                upgradeButton.setDisable(!canAfford || !canUpgrade);
                upgradeButton.setStyle(
                    canAfford && canUpgrade ?
                        "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30; -fx-cursor: hand; -fx-background-radius: 5;" :
                        "-fx-background-color: #CCCCCC; -fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 5;");
                upgradeButton.setOnAction(e -> {
                    handleUpgrade(card);
                    // Close modal after upgrade
                    Parent sceneRoot = cardScrollPane.getScene().getRoot();
                    if (sceneRoot instanceof Pane) {
                        ((Pane) sceneRoot).getChildren().remove(modalOverlay);
                    }
                });
                
                statsBox.getChildren().addAll(upgradeSeparator, upgradeTitle, nextDamageStat, 
                                             costLabel, goldLabel);
                // Store reference to upgrade button for later alignment with close button
                upgradeButtonRef = upgradeButton;
            }
        }

        Button closeButton = new Button("Close");
        closeButton.setStyle(
                "-fx-background-color: #FF5252; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 10 30; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 5;");
        closeButton.setOnAction(e -> {
            // Modal'ı kapat - root tipine göre
            Parent sceneRoot = cardScrollPane.getScene().getRoot();
            if (sceneRoot instanceof Pane) {
                ((Pane) sceneRoot).getChildren().remove(modalOverlay);
            }
        });

        closeButton.setOnMouseEntered(e -> closeButton.setStyle(
                "-fx-background-color: #D32F2F; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 10 30; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 5;"));
        closeButton.setOnMouseExited(e -> closeButton.setStyle(
                "-fx-background-color: #FF5252; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 10 30; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 5;"));

        // Create button container to align Upgrade and Close buttons horizontally
        HBox buttonContainer = new HBox(15);
        buttonContainer.setAlignment(Pos.CENTER);
        if (upgradeButtonRef != null) {
            buttonContainer.getChildren().addAll(upgradeButtonRef, closeButton);
        } else {
            buttonContainer.getChildren().add(closeButton);
        }
        
        detailPanel.getChildren().addAll(titleBox, separator, levelLabel, statsBox, buttonContainer);
        modalOverlay.getChildren().add(scrollPane);

        modalOverlay.setOnMouseClicked(e -> {
            if (e.getTarget() == modalOverlay) {
                Parent sceneRoot = cardScrollPane.getScene().getRoot();
                if (sceneRoot instanceof Pane) {
                    ((Pane) sceneRoot).getChildren().remove(modalOverlay);
                }
            }
        });

        Parent sceneRoot = cardScrollPane.getScene().getRoot();

        if (sceneRoot instanceof StackPane) {
            ((StackPane) sceneRoot).getChildren().add(modalOverlay);
        } else if (sceneRoot instanceof AnchorPane) {
            AnchorPane anchorRoot = (AnchorPane) sceneRoot;
            anchorRoot.getChildren().add(modalOverlay);
            // Modal'ı tam ekran yap
            AnchorPane.setTopAnchor(modalOverlay, 0.0);
            AnchorPane.setBottomAnchor(modalOverlay, 0.0);
            AnchorPane.setLeftAnchor(modalOverlay, 0.0);
            AnchorPane.setRightAnchor(modalOverlay, 0.0);
        } else if (sceneRoot instanceof Pane) {
            ((Pane) sceneRoot).getChildren().add(modalOverlay);
        }
    }

    private void setupDeckSlots() {
        if (deckSlots == null) {
            return;
        }

        deckSlots.setHgap(10);
        deckSlots.setVgap(10);
        deckSlots.setPrefWrapLength(580);

        for (int i = 0; i < 8; i++) {
            AnchorPane slot = createDeckSlot(i);
            deckSlots.getChildren().add(slot);
        }
    }

    private AnchorPane createDeckSlot(int index) {
        Button btn = new Button();
        btn.setPrefSize(111, 113);
        btn.setStyle(
                "-fx-background-color: #00000026; " +
                        "-fx-border-color: #333; " +
                        "-fx-border-width: 3; " +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 5;");
        btn.setText("Slot " + (index + 1));
        btn.setFont(new Font("Trebuchet MS", 9));

        btn.setOnAction(e -> removeCardFromDeck(index));

        // --- CONTAINER ---
        AnchorPane ap = new AnchorPane();

        // add in the order you described
        ap.getChildren().addAll(btn);

        // button fills rest, offset by radius
        AnchorPane.setTopAnchor(btn, 5.0);
        AnchorPane.setLeftAnchor(btn, 5.0);
        AnchorPane.setRightAnchor(btn, 0.0);
        AnchorPane.setBottomAnchor(btn, 0.0);

        // optional: set a fixed card size if you want
        ap.setPrefSize(126, 168);

        return ap;
    }

    private void addCardToDeck(Card card) {

        try {
            currentDeck.addCard(card);
            updateDeckDisplay();
            updateUI();
        } catch (IllegalArgumentException e) {
            // Trying to select a card that is already in the deck
            showAlert("Duplicate Card", "This card is already in your deck!");
        } catch (IllegalStateException e) {
            // Deck is full (No more than 8 cards)
            showAlert("Deck Full", "Your deck is full! Remove a card first.");
        }
    }

    private void removeCardFromDeck(int index) {
        if (currentDeck.removeCard(index)) {
            updateDeckDisplay();
            updateUI();
        }
    }

    private void updateDeckDisplay() {
        deckCards.clear();
        deckCards.addAll(currentDeck.getCards());

        deckSlots.getChildren().clear();
        for (int i = 0; i < 8; i++) {
            AnchorPane slot;
            if (i < deckCards.size()) {
                Card card = deckCards.get(i);
                slot = createCardInSlotButton(card, i);
            } else {
                slot = createDeckSlot(i);
            }
            deckSlots.getChildren().add(slot);
        }
    }

    private AnchorPane createCardInSlotButton(Card card, int index) {
        // --- BUTTON (the card) ---
        Button btn = new Button();
        btn.setStyle(
                "-fx-background-image: url(\"/kuroyale/images/cards/" + card.getName().toLowerCase().replaceAll(" ", "") + ".png\");" +
                "-fx-background-size: cover;" +
                "-fx-background-color: #5D3F7F;" +
                "-fx-background-radius: 5;" +
                "-fx-padding: 0;" +
                "-fx-border-color: #333; " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 5;");
        btn.setOnAction(e -> removeCardFromDeck(index));

        VBox hoverButtons = new VBox(5);
        hoverButtons.setAlignment(Pos.CENTER);
        hoverButtons.setStyle("-fx-background-color: transparent; -fx-padding: 5;");
        hoverButtons.setVisible(false);
        hoverButtons.setAlignment(Pos.CENTER);

        HBox hbox = new HBox(5);
        hbox.setAlignment(Pos.CENTER);
        hbox.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 5;");
        hbox.setAlignment(Pos.CENTER);

        Label lblName = new Label(card.getName());
        lblName.setFont(new Font("Trebuchet MS", 12));
        lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Button btnAdd = new Button("Remove");
        btnAdd.setStyle(
                "-fx-background-color: #af4c4cff; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 10px; " +
                        "-fx-cursor: hand;");
        btnAdd.setOnAction(e -> {
            removeCardFromDeck(index);
            e.consume();
        });

        Button btnView = new Button("View");
        btnView.setStyle(
                "-fx-background-color: #2196F3; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 10px; " +
                        "-fx-cursor: hand;");
        btnView.setOnAction(e -> {
            viewCardDetails(card);
            e.consume();
        });

        hbox.getChildren().addAll(btnAdd, btnView);
        hoverButtons.getChildren().addAll(lblName, hbox);

        // --- BADGE: circle + cost number ---
        double radius = 15;

        Circle circle = new Circle(radius);
        circle.setStyle("-fx-fill: #ff1fff; -fx-stroke: #4d3d4d; -fx-stroke-width: 3;");

        Label costLabel = new Label(String.valueOf(card.getCost()));
        costLabel.setTextFill(Color.WHITE);
        costLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 14));
        costLabel.setAlignment(Pos.CENTER);
        costLabel.setPrefWidth(10);
        costLabel.setPrefHeight(16);

        // so clicks on badge still click the button
        circle.setMouseTransparent(true);
        costLabel.setMouseTransparent(true);

        // --- CONTAINER ---
        AnchorPane ap = new AnchorPane();

        // add in the order you described
        ap.getChildren().addAll(btn, hoverButtons, circle, costLabel);

        // circle at (0,0)
        AnchorPane.setTopAnchor(circle, 0.0);
        AnchorPane.setLeftAnchor(circle, 0.0);

        // label centered in the circle:
        // top = r - h/2 = 15 - 16/2 = 7
        // left = r - w/2 = 15 - 10/2 = 10
        AnchorPane.setTopAnchor(costLabel, 8.5);
        AnchorPane.setLeftAnchor(costLabel, 11.5);

        // button fills rest, offset by radius
        AnchorPane.setTopAnchor(btn, radius/3);
        AnchorPane.setLeftAnchor(btn, radius/3);
        AnchorPane.setRightAnchor(btn, 0.0);
        AnchorPane.setBottomAnchor(btn, 0.0);

        AnchorPane.setBottomAnchor(hoverButtons, 5.0);
        AnchorPane.setLeftAnchor(hoverButtons, 10.0);
        AnchorPane.setRightAnchor(hoverButtons, 5.0);

        // optional: set a fixed card size if you want
        ap.setPrefSize(126, 168);

        ap.setOnMouseEntered(e -> {
            hoverButtons.setVisible(true);
        });

        ap.setOnMouseExited(e -> {
            hoverButtons.setVisible(false);
        });

        return ap;
    }

    private void setupDeckSelector() {
        deckSelector.getItems().clear();
        DeckManager.loadAllDecks();
        for (Deck deck : DeckManager.getAllDecks()) {
            deckSelector.getItems().add(deck.getName());
        }
    }

    @FXML
    private void handleSaveDeck() {
        String name = deckNameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Invalid Name", "Please enter a deck name.");
            return;
        }

        if (currentDeck.getSize() != 8) {
            showAlert("Incomplete Deck", "Your deck must have exactly 8 cards!");
            return;
        }

        String finalName = name;
        DeckManager.loadAllDecks();
        int deckNumber = 1;
        while (DeckManager.getDeckByName(finalName) != null) {
            finalName = name + " " + deckNumber;
            deckNumber++;
        }

        currentDeck.setName(finalName);
        DeckManager.saveDeck(currentDeck);
        DeckManager.setCurrentDeck(currentDeck);
        setupDeckSelector();
        deckNameField.setText(finalName);
        // deckStatusLabel.setText("Deck saved: " + finalName);
        showAlert("Success", "Deck saved successfully!");
    }

    @FXML
    private void handleClearDeck() {
        if (currentDeck.isEmpty()) {
            showAlert("Deck Empty", "The deck is already empty.");
            return;
        }

        currentDeck.clear();
        updateDeckDisplay();
        updateUI();
    }

    @FXML
    private void handleDeleteDeck() {
        String name = deckNameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Invalid Name", "Please enter a deck name to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Deck");
        confirm.setHeaderText("Are you sure you want to delete '" + name + "'?");
        confirm.setContentText("This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            DeckManager.deleteDeck(name);
            setupDeckSelector();
            // deckStatusLabel.setText("Deck deleted: " + name);

            // clear the currect deck if deleted
            if (currentDeck != null && currentDeck.getName().equals(name)) {
                currentDeck = new Deck("New Deck");
                deckNameField.clear();
                updateDeckDisplay();
                updateUI();
            }
        }
    }

    @FXML
    private void handleLoadDeck() {
        String selected = deckSelector.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a deck to load.");
            return;
        }

        Deck loaded = DeckManager.loadDeck(selected);
        if (loaded != null) {
            currentDeck = loaded;
            deckNameField.setText(loaded.getName());
            DeckManager.setCurrentDeck(loaded);
            updateDeckDisplay();
            updateUI();
            // deckStatusLabel.setText("Deck loaded: " + selected);
        }
    }

    private void updateUI() {
        int size = currentDeck.getSize();
        // deckStatusLabel.setText("Deck: " + size + "/8 cards");

        btnSaveDeck.setDisable(size != 8);

        // if (size == 8) {
        // deckStatusLabel.setStyle("-fx-text-fill: green;");
        // } else {
        // deckStatusLabel.setStyle("-fx-text-fill: orange;");
        // }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    void btnSelectBattleClicked(ActionEvent event) throws IOException {
        switchToStartBattleScene(event);
    }

    @FXML
    void btnSelectBuildClicked(ActionEvent event) throws IOException {
        switchToArenaBuilderScene(event);
    }

    private void switchToStartBattleScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/StartBattleScene.fxml"));
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
}
