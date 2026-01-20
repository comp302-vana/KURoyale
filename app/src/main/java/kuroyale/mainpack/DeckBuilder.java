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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.effect.DropShadow;

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
    private HBox deckNumberSelector;
    @FXML
    private Label goldLabel;
    @FXML
    private Button chestButton;

    private Deck currentDeck;
    private ObservableList<Card> deckCards;
    private int selectedDeckNumber = 1;
    private Button[] deckNumberButtons = new Button[8];

    // Managers for economy and upgrades
    private DeckManager deckManager;
    private CardStats cardStats = CardStats.getInstance();
    private PersistenceManager persistenceManager;
    private EconomyManager economyManager;
    private CardDataRepository cardDataRepository;
    private CardUpgradeManager cardUpgradeManager;
    private AchievementManager achievementManager;
    private PlayerProfile playerProfile;

    @FXML
    public void initialize() {
        if (cardScrollPane == null || cardContainer == null || deckSlots == null) {
            System.err.println("ERROR: FXML fields not injected! Check FXML file.");
            return;
        }
        deckManager = DeckManager.getInstance();

        deckCards = FXCollections.observableArrayList();

        // Initialize persistence and economy managers
        initializeManagers();

        // Setup deck number selector (1-8 buttons)
        setupDeckNumberSelector();

        setupCardDisplay();
        setupDeckSlots();

        // Load the selected deck (default: 1)
        selectedDeckNumber = deckManager.getSelectedDeckNumber();
        loadDeck(selectedDeckNumber);

        updateUI();
    }

    private void initializeManagers() {
        // Load player profile
        persistenceManager = new PersistenceManager();
        playerProfile = persistenceManager.loadPlayerProfile();

        // Initialize managers
        economyManager = new EconomyManager(playerProfile.getTotalGold(), persistenceManager);
        cardDataRepository = new CardDataRepository(playerProfile.getCardLevels());
        cardUpgradeManager = new CardUpgradeManager(economyManager, cardDataRepository, persistenceManager);

        achievementManager = new AchievementManager();
        achievementManager.setAchievements(playerProfile.getAchievements());
        cardUpgradeManager.setAchievementManager(achievementManager);

        // Setup gold display
        if (goldLabel != null) {
            goldLabel.setText("Gold: " + economyManager.getCurrentGold());
            // Listen for gold changes
            economyManager.currentGoldProperty().addListener((obs, oldVal, newVal) -> {
                goldLabel.setText("Gold: " + newVal.intValue());
            });
        }
    }

    private void setupDeckNumberSelector() {
        if (deckNumberSelector == null) {
            return;
        }

        for (int i = 1; i <= 8; i++) {
            final int deckNum = i;
            Button btn = new Button(String.valueOf(i));
            btn.setPrefSize(40, 40);
            btn.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 16));

            // Check if deck has cards
            btn.setOnAction(e -> selectDeck(deckNum));

            deckNumberButtons[i - 1] = btn;
            deckNumberSelector.getChildren().add(btn);
        }

        // Highlight the selected deck
        updateDeckNumberButtonStyles();
    }

    private void updateDeckNumberButtonStyles() {
        for (int i = 0; i < 8; i++) {
            Button btn = deckNumberButtons[i];
            if (btn == null)
                continue;

            int deckNum = i + 1;
            int cardCount = deckManager.getDeckCardCount(deckNum);
            boolean isSelected = (deckNum == selectedDeckNumber);
            boolean isComplete = cardCount == 8;
            boolean isIncomplete = cardCount > 0 && cardCount < 8;

            if (isSelected) {
                // Selected deck - use appropriate color based on state with highlighted border
                if (isComplete) {
                    btn.setStyle(
                            "-fx-background-color: #4CAF50; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-background-radius: 20; " +
                                    "-fx-border-color: #2E7D32; " +
                                    "-fx-border-width: 3; " +
                                    "-fx-border-radius: 20; " +
                                    "-fx-cursor: hand;");
                } else if (isIncomplete) {
                    btn.setStyle(
                            "-fx-background-color: #F44336; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-background-radius: 20; " +
                                    "-fx-border-color: #C62828; " +
                                    "-fx-border-width: 3; " +
                                    "-fx-border-radius: 20; " +
                                    "-fx-cursor: hand;");
                } else {
                    btn.setStyle(
                            "-fx-background-color: #9E9E9E; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-background-radius: 20; " +
                                    "-fx-border-color: #616161; " +
                                    "-fx-border-width: 3; " +
                                    "-fx-border-radius: 20; " +
                                    "-fx-cursor: hand;");
                }
            } else if (isComplete) {
                // Complete deck (8 cards) - green
                btn.setStyle(
                        "-fx-background-color: #66BB6A; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 20; " +
                                "-fx-border-color: #43A047; " +
                                "-fx-border-width: 2; " +
                                "-fx-border-radius: 20; " +
                                "-fx-cursor: hand;");
            } else if (isIncomplete) {
                // Incomplete deck (1-7 cards) - red
                btn.setStyle(
                        "-fx-background-color: #EF5350; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 20; " +
                                "-fx-border-color: #E53935; " +
                                "-fx-border-width: 2; " +
                                "-fx-border-radius: 20; " +
                                "-fx-cursor: hand;");
            } else {
                // Empty slot (0 cards) - gray
                btn.setStyle(
                        "-fx-background-color: #BDBDBD; " +
                                "-fx-text-fill: #616161; " +
                                "-fx-background-radius: 20; " +
                                "-fx-border-color: #9E9E9E; " +
                                "-fx-border-width: 2; " +
                                "-fx-border-radius: 20; " +
                                "-fx-cursor: hand;");
            }
        }
    }

    private void selectDeck(int deckNumber) {
        if (deckNumber == selectedDeckNumber) {
            return; // Already on this deck
        }

        // Auto-save current deck before switching (even if empty)
        if (currentDeck != null) {
            deckManager.saveDeckByNumber(selectedDeckNumber, currentDeck);
        }

        // Switch to the new deck
        selectedDeckNumber = deckNumber;
        deckManager.setSelectedDeckNumber(deckNumber);

        // Load the new deck
        loadDeck(deckNumber);

        // Update button styles
        updateDeckNumberButtonStyles();
    }

    private void loadDeck(int deckNumber) {
        currentDeck = deckManager.loadDeckByNumber(deckNumber);
        deckManager.setCurrentDeck(currentDeck);
        updateDeckDisplay();
        updateUI();
    }

    private void setupCardDisplay() {
        if (cardContainer == null) {
            return;
        }

        cardContainer.setHgap(8);
        cardContainer.setVgap(10);
        cardContainer.setPrefWrapLength(760);

        // create buttons for all cards (using level 1 for display, actual level from
        // repository)
        for (Card card : CardFactory.getInstance().getAllCards()) {
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

        // Check if card is unlocked
        boolean isUnlocked = playerProfile != null && playerProfile.isCardUnlocked(card.getId());

        // --- BUTTON (the card) ---
        Button btn = new Button();
        String borderColor = CardVisualManager.getRarityBorderColor(rarity);
        btn.setStyle(
                "-fx-background-image: url(\"/kuroyale/images/cards/" + card.getName().toLowerCase().replaceAll(" ", "")
                        + ".png\");" +
                        "-fx-background-size: cover;" +
                        "-fx-background-color: #5D3F7F;" +
                        "-fx-background-radius: 5;" +
                        "-fx-padding: 0;" +
                        "-fx-border-color: " + borderColor + "; " +
                        "-fx-border-width: 3; " +
                        "-fx-border-radius: 5;");
        if (isUnlocked) {
            btn.setOnAction(e -> addCardToDeck(card));
        }

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

        Button btnAdd = new Button(isUnlocked ? "Add" : "🔒 Locked");
        if (isUnlocked) {
            btnAdd.setStyle(
                    "-fx-background-color: #4CAF50; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 10px; " +
                            "-fx-cursor: hand;");
            btnAdd.setOnAction(e -> {
                addCardToDeck(card);
                e.consume();
            });
        } else {
            btnAdd.setStyle(
                    "-fx-background-color: #666666; " +
                            "-fx-text-fill: #999999; " +
                            "-fx-font-size: 10px; " +
                            "-fx-cursor: default;");
            btnAdd.setDisable(true);
        }

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
        AnchorPane.setTopAnchor(btn, radius / 3);
        AnchorPane.setLeftAnchor(btn, radius / 3);
        AnchorPane.setRightAnchor(btn, 0.0);
        AnchorPane.setBottomAnchor(btn, 0.0);

        AnchorPane.setBottomAnchor(hoverButtons, 5.0);
        AnchorPane.setLeftAnchor(hoverButtons, 20.0);
        AnchorPane.setRightAnchor(hoverButtons, 15.0);

        // optional: set a fixed card size if you want
        ap.setPrefSize(126, 168);

        // --- LOCKED OVERLAY ---
        if (!isUnlocked) {
            // Dark overlay - added after btn but before circle so elixir badge stays on top
            javafx.scene.shape.Rectangle lockOverlay = new javafx.scene.shape.Rectangle(121, 163);
            lockOverlay.setFill(Color.rgb(0, 0, 0, 0.6));
            lockOverlay.setArcWidth(10);
            lockOverlay.setArcHeight(10);
            lockOverlay.setMouseTransparent(true);

            // Insert overlay at index 1 (after btn at 0, before circle)
            ap.getChildren().add(1, lockOverlay);
            AnchorPane.setTopAnchor(lockOverlay, radius / 3);
            AnchorPane.setLeftAnchor(lockOverlay, radius / 3);
        }

        // Add level indicator (after overlay so stars appear on top)
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
            playerProfile.setAchievements(achievementManager.getAchievements());
            persistenceManager.savePlayerProfile(playerProfile);
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
        // Get card level and rarity (refresh from repository to ensure we have latest
        // level)
        int cardLevel = cardDataRepository != null ? cardDataRepository.getCardLevel(card.getId()) : 1;
        CardRarity rarity = CardRarityMapper.getRarity(card.getId());

        // Check if card can be upgraded (not at max level and has enough gold)
        boolean isMaxLevel = (cardLevel >= 3);
        boolean canUpgrade = !isMaxLevel && cardUpgradeManager != null && cardUpgradeManager.canUpgrade(card.getId());
        int upgradeCost = (!isMaxLevel && cardUpgradeManager != null) ? cardUpgradeManager.getUpgradeCost(card.getId())
                : -1;
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
        rarityLabel.setStyle("-fx-text-fill: " + rarityColor + "; -fx-background-color: " + rarityColor
                + "22; -fx-background-radius: 3; -fx-padding: 3 8;");
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
            int currentHP = cardStats.getHPRounded(card.getId(), cardLevel);
            int currentDamage = cardStats.getDamageRounded(card.getId(), cardLevel);
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

            // Add upgrade preview if not at max level (show even if can't afford, button
            // will be disabled)
            if (!isMaxLevel) {
                int nextLevel = cardLevel + 1;
                int nextHP = cardStats.getHPRounded(card.getId(), nextLevel);
                int nextDamage = cardStats.getDamageRounded(card.getId(), nextLevel);

                javafx.scene.shape.Line upgradeSeparator = new javafx.scene.shape.Line(0, 0, 340, 0);
                upgradeSeparator.setStroke(Color.GRAY);

                Label upgradeTitle = new Label("Upgrade to Level " + nextLevel);
                upgradeTitle.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 16));
                upgradeTitle.setStyle("-fx-text-fill: #4CAF50;");

                Label nextHPStat = new Label("HP: " + nextHP + " (+" + ((nextLevel == 2) ? "10%" : "20%") + ")");
                nextHPStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));
                nextHPStat.setStyle("-fx-text-fill: #4CAF50;");

                Label nextDamageStat = new Label(
                        "Damage: " + nextDamage + " (+" + ((nextLevel == 2) ? "10%" : "20%") + ")");
                nextDamageStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));
                nextDamageStat.setStyle("-fx-text-fill: #4CAF50;");

                Label costLabel = new Label("Upgrade Cost: " + upgradeCost + " Gold");
                costLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 14));

                Label goldLabelModal = new Label("Your Gold: " + currentGold);
                goldLabelModal.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));

                Button upgradeButton = new Button("Upgrade");
                boolean canAfford = economyManager != null && economyManager.canAfford(upgradeCost);
                upgradeButton.setDisable(!canAfford || !canUpgrade);
                upgradeButton.setStyle(
                        canAfford && canUpgrade
                                ? "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30; -fx-cursor: hand; -fx-background-radius: 5;"
                                : "-fx-background-color: #CCCCCC; -fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 5;");
                upgradeButton.setOnAction(e -> {
                    handleUpgrade(card);
                    // Close modal after upgrade
                    Parent sceneRoot = cardScrollPane.getScene().getRoot();
                    if (sceneRoot instanceof Pane) {
                        ((Pane) sceneRoot).getChildren().remove(modalOverlay);
                    }
                });

                statsBox.getChildren().addAll(upgradeSeparator, upgradeTitle, nextHPStat, nextDamageStat,
                        costLabel, goldLabelModal);
                // Store reference to upgrade button for later alignment with close button
                upgradeButtonRef = upgradeButton;
            }
        }

        if (card instanceof BuildingCard) {
            BuildingCard buildingCard = (BuildingCard) card;
            // Get stats at current level
            int currentHP = cardStats.getHPRounded(card.getId(), cardLevel);
            int currentDamage = cardStats.getDamageRounded(card.getId(), cardLevel);
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

            // Add upgrade preview if not at max level (show even if can't afford, button
            // will be disabled)
            if (!isMaxLevel) {
                int nextLevel = cardLevel + 1;
                int nextHP = cardStats.getHPRounded(card.getId(), nextLevel);
                int nextDamage = cardStats.getDamageRounded(card.getId(), nextLevel);

                javafx.scene.shape.Line upgradeSeparator = new javafx.scene.shape.Line(0, 0, 340, 0);
                upgradeSeparator.setStroke(Color.GRAY);

                Label upgradeTitle = new Label("Upgrade to Level " + nextLevel);
                upgradeTitle.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 16));
                upgradeTitle.setStyle("-fx-text-fill: #4CAF50;");

                Label nextHPStat = new Label("HP: " + nextHP + " (+" + ((nextLevel == 2) ? "10%" : "20%") + ")");
                nextHPStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));
                nextHPStat.setStyle("-fx-text-fill: #4CAF50;");

                Label nextDamageStat = new Label(
                        "Damage: " + nextDamage + " (+" + ((nextLevel == 2) ? "10%" : "20%") + ")");
                nextDamageStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));
                nextDamageStat.setStyle("-fx-text-fill: #4CAF50;");

                Label costLabel = new Label("Upgrade Cost: " + upgradeCost + " Gold");
                costLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 14));

                Label goldLabelModal = new Label("Your Gold: " + currentGold);
                goldLabelModal.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));

                Button upgradeButton = new Button("Upgrade");
                boolean canAfford = economyManager != null && economyManager.canAfford(upgradeCost);
                upgradeButton.setDisable(!canAfford || !canUpgrade);
                upgradeButton.setStyle(
                        canAfford && canUpgrade
                                ? "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30; -fx-cursor: hand; -fx-background-radius: 5;"
                                : "-fx-background-color: #CCCCCC; -fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 5;");
                upgradeButton.setOnAction(e -> {
                    handleUpgrade(card);
                    // Close modal after upgrade
                    Parent sceneRoot = cardScrollPane.getScene().getRoot();
                    if (sceneRoot instanceof Pane) {
                        ((Pane) sceneRoot).getChildren().remove(modalOverlay);
                    }
                });

                statsBox.getChildren().addAll(upgradeSeparator, upgradeTitle, nextHPStat, nextDamageStat,
                        costLabel, goldLabelModal);
                // Store reference to upgrade button for later alignment with close button
                upgradeButtonRef = upgradeButton;
            }
        }

        if (card instanceof SpellCard) {
            SpellCard spellCard = (SpellCard) card;
            // Get stats at current level
            int currentDamage = cardStats.getDamageRounded(card.getId(), cardLevel);
            radius = String.valueOf(spellCard.getRadius());

            Label areaDamageStat = new Label("Area Damage: " + currentDamage);
            areaDamageStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label radiusStat = new Label("Radius: " + radius);
            radiusStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            statsBox.getChildren().addAll(costStat, typeStat, areaDamageStat, radiusStat, descStat, descText);

            // Add upgrade preview if not at max level (show even if can't afford, button
            // will be disabled)
            if (!isMaxLevel) {
                int nextLevel = cardLevel + 1;
                int nextDamage = cardStats.getDamageRounded(card.getId(), nextLevel);

                javafx.scene.shape.Line upgradeSeparator = new javafx.scene.shape.Line(0, 0, 340, 0);
                upgradeSeparator.setStroke(Color.GRAY);

                Label upgradeTitle = new Label("Upgrade to Level " + nextLevel);
                upgradeTitle.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 16));
                upgradeTitle.setStyle("-fx-text-fill: #4CAF50;");

                Label nextDamageStat = new Label(
                        "Area Damage: " + nextDamage + " (+" + ((nextLevel == 2) ? "10%" : "20%") + ")");
                nextDamageStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));
                nextDamageStat.setStyle("-fx-text-fill: #4CAF50;");

                Label costLabel = new Label("Upgrade Cost: " + upgradeCost + " Gold");
                costLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 14));

                Label goldLabelModal = new Label("Your Gold: " + currentGold);
                goldLabelModal.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));

                Button upgradeButton = new Button("Upgrade");
                boolean canAfford = economyManager != null && economyManager.canAfford(upgradeCost);
                upgradeButton.setDisable(!canAfford || !canUpgrade);
                upgradeButton.setStyle(
                        canAfford && canUpgrade
                                ? "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30; -fx-cursor: hand; -fx-background-radius: 5;"
                                : "-fx-background-color: #CCCCCC; -fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 5;");
                upgradeButton.setOnAction(e -> {
                    handleUpgrade(card);
                    // Close modal after upgrade
                    Parent sceneRoot = cardScrollPane.getScene().getRoot();
                    if (sceneRoot instanceof Pane) {
                        ((Pane) sceneRoot).getChildren().remove(modalOverlay);
                    }
                });

                statsBox.getChildren().addAll(upgradeSeparator, upgradeTitle, nextDamageStat,
                        costLabel, goldLabelModal);
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
        // Create a new deck if none exists
        if (currentDeck == null) {
            currentDeck = new Deck("Deck" + selectedDeckNumber, new java.util.ArrayList<>());
            deckManager.setCurrentDeck(currentDeck);
        }

        try {
            currentDeck.addCard(card);
            updateDeckDisplay();
            updateUI();
            // Auto-save after adding card
            deckManager.saveDeckByNumber(selectedDeckNumber, currentDeck);
            updateDeckNumberButtonStyles();
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
        if (currentDeck != null) {
            deckCards.addAll(currentDeck.getCards());
        }

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
                "-fx-background-image: url(\"/kuroyale/images/cards/" + card.getName().toLowerCase().replaceAll(" ", "")
                        + ".png\");" +
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
        AnchorPane.setTopAnchor(btn, radius / 3);
        AnchorPane.setLeftAnchor(btn, radius / 3);
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

    private void updateUI() {
        // Update deck number button styles to reflect current state
        updateDeckNumberButtonStyles();
    }

    /**
     * Triggers a flashing animation on the deck slots to alert user that deck is
     * incomplete
     */
    public void triggerDeckSlotsFlash() {
        if (deckSlots == null)
            return;

        DropShadow redGlow = new DropShadow();
        redGlow.setColor(Color.RED);
        redGlow.setRadius(25);
        redGlow.setSpread(0.6);

        // Create flashing animation
        Timeline flashTimeline = new Timeline();
        flashTimeline.setCycleCount(8); // Flash 4 times (on/off = 2 keyframes per flash)

        KeyFrame glowOn = new KeyFrame(Duration.millis(0), e -> {
            deckSlots.setEffect(redGlow);
            deckSlots.setStyle(
                    "-fx-border-color: #FF0000; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-color: rgba(255,0,0,0.1); -fx-background-radius: 10;");
        });

        KeyFrame glowOff = new KeyFrame(Duration.millis(250), e -> {
            deckSlots.setEffect(null);
            deckSlots.setStyle("");
        });

        KeyFrame glowOn2 = new KeyFrame(Duration.millis(500), e -> {
            deckSlots.setEffect(redGlow);
            deckSlots.setStyle(
                    "-fx-border-color: #FF0000; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-color: rgba(255,0,0,0.1); -fx-background-radius: 10;");
        });

        KeyFrame glowOff2 = new KeyFrame(Duration.millis(750), e -> {
            deckSlots.setEffect(null);
            deckSlots.setStyle("");
        });

        KeyFrame glowOn3 = new KeyFrame(Duration.millis(1000), e -> {
            deckSlots.setEffect(redGlow);
            deckSlots.setStyle(
                    "-fx-border-color: #FF0000; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-color: rgba(255,0,0,0.1); -fx-background-radius: 10;");
        });

        KeyFrame glowOff3 = new KeyFrame(Duration.millis(1250), e -> {
            deckSlots.setEffect(null);
            deckSlots.setStyle("");
        });

        KeyFrame glowOn4 = new KeyFrame(Duration.millis(1500), e -> {
            deckSlots.setEffect(redGlow);
            deckSlots.setStyle(
                    "-fx-border-color: #FF0000; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-color: rgba(255,0,0,0.1); -fx-background-radius: 10;");
        });

        KeyFrame glowOff4 = new KeyFrame(Duration.millis(1750), e -> {
            deckSlots.setEffect(null);
            deckSlots.setStyle("");
        });

        flashTimeline.getKeyFrames().addAll(glowOn, glowOff, glowOn2, glowOff2, glowOn3, glowOff3, glowOn4, glowOff4);
        flashTimeline.setCycleCount(1);
        flashTimeline.play();
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
        // Auto-save current deck before leaving (even if empty)
        if (currentDeck != null) {
            deckManager.saveDeckByNumber(selectedDeckNumber, currentDeck);
        }
        switchToStartBattleScene(event);
    }

    @FXML
    void btnSelectBuildClicked(ActionEvent event) throws IOException {
        // Auto-save current deck before leaving (even if empty)
        if (currentDeck != null) {
            deckManager.saveDeckByNumber(selectedDeckNumber, currentDeck);
        }
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

    @FXML
    void btnOpenChestClicked(ActionEvent event) {
        // Open chest and get rewards
        kuroyale.mainpack.models.ChestReward reward = kuroyale.mainpack.managers.ChestManager
                .openBasicChest(playerProfile);
        
        // Update PlayerStatistics for gold earned from chest
        kuroyale.mainpack.models.PlayerStatistics stats = playerProfile.getStatistics();
        if (stats != null) {
            stats.incrementTotalGoldEarned(reward.getGoldAmount());
            playerProfile.setStatistics(stats);
        }

        // Save profile after chest opening
        persistenceManager.savePlayerProfile(playerProfile);

        // Update gold display (ChestManager already added gold to profile, just refresh
        // UI)
        economyManager.addGold(reward.getGoldAmount());

        // Update achievements
        kuroyale.mainpack.managers.AchievementManager achievementManager = 
        new kuroyale.mainpack.managers.AchievementManager();
        achievementManager.setAchievements(playerProfile.getAchievements());
        if (stats != null) {
            achievementManager.onGoldEarned(reward.getGoldAmount(), stats);
            achievementManager.updateFromStatistics(stats);
            playerProfile.setAchievements(achievementManager.getAchievements());
            persistenceManager.savePlayerProfile(playerProfile);
        }

        // Show reward popup
        showChestRewardPopup(reward);

        // Refresh card display if a new card was unlocked
        if (reward.isNewCard()) {
            refreshAllCardDisplay();
        }
    }

    private void showChestRewardPopup(kuroyale.mainpack.models.ChestReward reward) {
        StackPane modalOverlay = new StackPane();
        modalOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);");

        VBox rewardPanel = new VBox(20);
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
        Label titleLabel = new Label("*** Chest Opened! ***");
        titleLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.WHITE);

        // Gold reward
        Label goldRewardLabel = new Label("+" + reward.getGoldAmount() + " Gold");
        goldRewardLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 24));
        goldRewardLabel.setTextFill(Color.GOLD);

        rewardPanel.getChildren().addAll(titleLabel, goldRewardLabel);

        // Button for first stage
        Button actionButton = new Button();
        actionButton.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 16));
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

                Label cardTitleLabel = new Label("Card Reward!");
                cardTitleLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 28));
                cardTitleLabel.setTextFill(Color.WHITE);

                Label cardLabel;
                if (reward.isNewCard()) {
                    cardLabel = new Label("*** NEW CARD UNLOCKED! ***");
                    cardLabel.setTextFill(Color.LIME);
                } else {
                    cardLabel = new Label("Card Found:");
                    cardLabel.setTextFill(Color.WHITE);
                }
                cardLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 18));

                // Card name with rarity color
                kuroyale.cardpack.Card card = reward.getUnlockedCard();
                kuroyale.cardpack.CardRarity rarity = kuroyale.cardpack.CardRarityMapper.getRarity(card.getId());
                String rarityColor = CardVisualManager.getRarityBorderColor(rarity);

                Label cardNameLabel = new Label(card.getName());
                cardNameLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 22));
                cardNameLabel.setStyle("-fx-text-fill: " + rarityColor + ";");

                Label rarityLabel = new Label(rarity.name());
                rarityLabel.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 14));
                rarityLabel.setStyle("-fx-text-fill: " + rarityColor
                        + "; -fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 5; -fx-padding: 5 15;");

                // Collect button for final stage
                Button collectButton = new Button("Collect!");
                collectButton.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 16));
                collectButton.setStyle(
                        "-fx-background-color: #4CAF50; " +
                                "-fx-text-fill: white; " +
                                "-fx-padding: 15 50; " +
                                "-fx-background-radius: 10; " +
                                "-fx-cursor: hand;");
                collectButton.setOnAction(ev -> {
                    Parent sceneRoot = cardScrollPane.getScene().getRoot();
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
                Parent sceneRoot = cardScrollPane.getScene().getRoot();
                if (sceneRoot instanceof javafx.scene.layout.Pane) {
                    ((javafx.scene.layout.Pane) sceneRoot).getChildren().remove(modalOverlay);
                }
            });
        }

        rewardPanel.getChildren().add(actionButton);
        modalOverlay.getChildren().add(rewardPanel);

        // Add to scene
        Parent sceneRoot = cardScrollPane.getScene().getRoot();
        if (sceneRoot instanceof StackPane) {
            ((StackPane) sceneRoot).getChildren().add(modalOverlay);
        } else if (sceneRoot instanceof AnchorPane) {
            AnchorPane anchorRoot = (AnchorPane) sceneRoot;
            anchorRoot.getChildren().add(modalOverlay);
            AnchorPane.setTopAnchor(modalOverlay, 0.0);
            AnchorPane.setBottomAnchor(modalOverlay, 0.0);
            AnchorPane.setLeftAnchor(modalOverlay, 0.0);
            AnchorPane.setRightAnchor(modalOverlay, 0.0);
        } else if (sceneRoot instanceof javafx.scene.layout.Pane) {
            ((javafx.scene.layout.Pane) sceneRoot).getChildren().add(modalOverlay);
        }
    }

    private void refreshAllCardDisplay() {
        // Reload player profile to get updated unlock status
        playerProfile = persistenceManager.loadPlayerProfile();

        // Clear and rebuild card display
        cardContainer.getChildren().clear();
        for (kuroyale.cardpack.Card card : CardFactory.getInstance().getAllCards()) {
            AnchorPane cardButton = createCardNode(card);
            cardButton.setUserData(card.getId());
            cardContainer.getChildren().add(cardButton);
        }
    }
}
