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
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardFactory;
import kuroyale.deckpack.Deck;
import kuroyale.deckpack.DeckManager;
import kuroyale.cardpack.subclasses.*;

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
    // @FXML
    // private Label deckStatusLabel;

    private Deck currentDeck;
    private ObservableList<Card> deckCards;

    @FXML
    public void initialize() {
        if (cardScrollPane == null || cardContainer == null || deckSlots == null) {
            System.err.println("ERROR: FXML fields not injected! Check FXML file.");
            return;
        }

        deckCards = FXCollections.observableArrayList();
        currentDeck = new Deck("New Deck");

        setupCardDisplay();
        setupDeckSlots();
        setupDeckSelector();
        updateUI();
    }

    private void setupCardDisplay() {
        if (cardContainer == null) {
            return;
        }

        cardContainer.setHgap(8);
        cardContainer.setVgap(10);
        cardContainer.setPrefWrapLength(760);

        // create buttons for all cards
        for (Card card : CardFactory.getAllCards()) {
            AnchorPane cardButton = createCardNode(card);
            cardContainer.getChildren().add(cardButton);
        }

        if (cardScrollPane != null) {
            cardScrollPane.setContent(cardContainer);
            cardScrollPane.setFitToWidth(true);
        }
    }

    private AnchorPane createCardNode(Card card) {
        // --- BUTTON (the card) ---
        Button btn = new Button();
        btn.setStyle(
            "-fx-background-color: #E8E8E8; " +
            "-fx-border-color: #333; " +
            "-fx-border-width: 3; " +
            "-fx-background-radius: 6;" +
            "-fx-border-radius: 5;"
        );
        btn.setOnAction(e -> addCardToDeck(card));

        VBox vbox = new VBox(4);
        vbox.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(card.getName());
        nameLabel.setFont(new Font("Trebuchet MS", 11));
        nameLabel.setStyle("-fx-text-fill: black;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(105);
        nameLabel.setAlignment(Pos.CENTER);

        Label descLabel = new Label(card.getDescription());
        descLabel.setFont(new Font("Trebuchet MS", 11));
        descLabel.setStyle("-fx-text-fill: black;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(105);
        descLabel.setTextAlignment(TextAlignment.CENTER);

        vbox.getChildren().addAll(nameLabel, descLabel);
        btn.setGraphic(vbox);

        HBox hoverButtons = new HBox(5);
        hoverButtons.setAlignment(Pos.CENTER);
        hoverButtons.setStyle("-fx-background-color: rgba(0,0,0,1); -fx-padding: 5;");
        hoverButtons.setVisible(false); 
        hoverButtons.setAlignment(Pos.CENTER);
        
        Button btnAdd = new Button("Add");
        btnAdd.setStyle(
            "-fx-background-color: #4CAF50; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 10px; " +
            "-fx-padding: 5 10; " +
            "-fx-cursor: hand;"
        );
        btnAdd.setOnAction(e -> {
            addCardToDeck(card);
            e.consume(); 
        });
        
        Button btnView = new Button("View");
        btnView.setStyle(
            "-fx-background-color: #2196F3; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 10px; " +
            "-fx-padding: 5 10; " +
            "-fx-cursor: hand;"
        );
        btnView.setOnAction(e -> {
            viewCardDetails(card);
            e.consume();
        });
    
    hoverButtons.getChildren().addAll(btnAdd, btnView);

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
        AnchorPane.setTopAnchor(btn, radius);
        AnchorPane.setLeftAnchor(btn, radius);
        AnchorPane.setRightAnchor(btn, 0.0);
        AnchorPane.setBottomAnchor(btn, 0.0);

        AnchorPane.setBottomAnchor(hoverButtons, 5.0);
        AnchorPane.setLeftAnchor(hoverButtons, 15.0);
        AnchorPane.setRightAnchor(hoverButtons, 15.0);

        // optional: set a fixed card size if you want
        ap.setPrefSize(126, 168);

        ap.setOnMouseEntered(e -> {
            hoverButtons.setVisible(true);
            btn.setStyle(
                "-fx-background-color: #D0D0D0; " + // Biraz daha koyu
                "-fx-border-color: #333; " +
                "-fx-border-width: 3; " +
                "-fx-background-radius: 6;" +
                "-fx-border-radius: 5;"
            );
        });

        ap.setOnMouseExited(e -> {
            hoverButtons.setVisible(false);
            btn.setStyle(
                "-fx-background-color: #E8E8E8; " +
                "-fx-border-color: #333; " +
                "-fx-border-width: 3; " +
                "-fx-background-radius: 6;" +
                "-fx-border-radius: 5;"
            );
        });

        return ap;
    }

    private void viewCardDetails(Card card) {
        StackPane modalOverlay = new StackPane();
        modalOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        
        VBox detailPanel = new VBox(15);
        detailPanel.setAlignment(Pos.CENTER);
        detailPanel.setMaxWidth(400);
        detailPanel.setMaxHeight(500);
        detailPanel.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 10; " +
            "-fx-padding: 30; " +
            "-fx-border-color: #333; " +
            "-fx-border-width: 3; " +
            "-fx-border-radius: 10;"
        );
        
        Label titleLabel = new Label(card.getName());
        titleLabel.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #333;");
        
        javafx.scene.shape.Line separator = new javafx.scene.shape.Line(0, 0, 340, 0);
        separator.setStroke(Color.GRAY);
        
        VBox statsBox = new VBox(10);
        statsBox.setAlignment(Pos.CENTER_LEFT);
        
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
            hp = String.valueOf(unitCard.getHp());
            damage = String.valueOf(unitCard.getDamage());
            hitSpeed = String.valueOf(unitCard.getHitSpeed());
            range = String.valueOf(unitCard.getRange());
            speed = unitCard.getSpeed();

            Label hpStat = new Label("HP: " + hp);
            hpStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label damageStat = new Label("Damage: " + damage);
            damageStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label hitSpeedStat = new Label("Hit Speed: " + hitSpeed);
            hitSpeedStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label rangeStat = new Label("Range: " + range);
            rangeStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label speedStat = new Label("Speed: " + speed);
            speedStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            statsBox.getChildren().addAll(costStat, typeStat, hpStat, damageStat, hitSpeedStat, rangeStat, speedStat, descStat, descText);
        }
    
        if (card instanceof BuildingCard) {
            BuildingCard buildingCard = (BuildingCard) card;
            hp = String.valueOf(buildingCard.getHp());
            damage = String.valueOf(buildingCard.getDamage());
            range = String.valueOf(buildingCard.getRange());
            lifetime = String.valueOf(buildingCard.getLifetime());

            Label hpStat = new Label("HP: " + hp);
            hpStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label damageStat = new Label("Damage: " + damage);
            damageStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label rangeStat = new Label("Range: " + range);
            rangeStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label lifetimeStat = new Label("Lifetime: " + lifetime);
            lifetimeStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            statsBox.getChildren().addAll(costStat, typeStat, hpStat, damageStat, rangeStat, lifetimeStat, descStat, descText);
        }

        if (card instanceof SpellCard) {
            SpellCard spellCard = (SpellCard) card;
            areaDamage = String.valueOf(spellCard.getAreaDamage());
            radius = String.valueOf(spellCard.getRadius());

            Label areaDamageStat = new Label("Area Damage: " + areaDamage);
            areaDamageStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            Label radiusStat = new Label("Radius: " + radius);
            radiusStat.setFont(Font.font("Trebuchet MS", FontWeight.NORMAL, 16));

            statsBox.getChildren().addAll(costStat, typeStat, areaDamageStat, radiusStat, descStat, descText);
        }
        
        
        Button closeButton = new Button("Close");
        closeButton.setStyle(
            "-fx-background-color: #FF5252; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-padding: 10 30; " +
            "-fx-cursor: hand; " +
            "-fx-background-radius: 5;"
        );
        closeButton.setOnAction(e -> {
            // Modal'ı kapat - root tipine göre
            Parent sceneRoot = cardScrollPane.getScene().getRoot();
            if (sceneRoot instanceof Pane) {
                ((Pane) sceneRoot).getChildren().remove(modalOverlay);
            }
        });
        
        closeButton.setOnMouseEntered(e -> 
            closeButton.setStyle(
                "-fx-background-color: #D32F2F; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-padding: 10 30; " +
                "-fx-cursor: hand; " +
                "-fx-background-radius: 5;"
            )
        );
        closeButton.setOnMouseExited(e -> 
            closeButton.setStyle(
                "-fx-background-color: #FF5252; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-padding: 10 30; " +
                "-fx-cursor: hand; " +
                "-fx-background-radius: 5;"
            )
        );
        
        detailPanel.getChildren().addAll(titleLabel, separator, statsBox, closeButton);
        modalOverlay.getChildren().add(detailPanel);
        
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
            "-fx-border-radius: 5;"
        );
        btn.setText("Slot " + (index + 1));
        btn.setFont(new Font("Trebuchet MS", 9));

        btn.setOnAction(e -> removeCardFromDeck(index));

        // --- CONTAINER ---
        AnchorPane ap = new AnchorPane();

        // add in the order you described
        ap.getChildren().addAll(btn);

        // button fills rest, offset by radius
        AnchorPane.setTopAnchor(btn, 15.0);
        AnchorPane.setLeftAnchor(btn, 15.0);
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
        Button btn = new Button();
        btn.setPrefSize(111, 113);
        btn.setStyle(
            "-fx-background-color: #B8E6B8; " +
            "-fx-border-color: #333; " +
            "-fx-border-width: 3; " +
            "-fx-background-radius: 6;" +
            "-fx-border-radius: 5;"
        );

        VBox vbox = new VBox(4);
        vbox.setAlignment(javafx.geometry.Pos.CENTER);

        Label nameLabel = new Label(card.getName());
        nameLabel.setFont(new Font("Trebuchet MS", 11));
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(80);
        nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        nameLabel.setAlignment(javafx.geometry.Pos.CENTER);

        vbox.getChildren().addAll(nameLabel);
        btn.setGraphic(vbox);

        btn.setOnAction(e -> removeCardFromDeck(index));

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
        ap.getChildren().addAll(btn, circle, costLabel);

        // circle at (0,0)
        AnchorPane.setTopAnchor(circle, 0.0);
        AnchorPane.setLeftAnchor(circle, 0.0);

        // label centered in the circle:
        // top = r - h/2 = 15 - 16/2 = 7
        // left = r - w/2 = 15 - 10/2 = 10
        AnchorPane.setTopAnchor(costLabel, 8.5);
        AnchorPane.setLeftAnchor(costLabel, 11.5);

        // button fills rest, offset by radius
        AnchorPane.setTopAnchor(btn, radius);
        AnchorPane.setLeftAnchor(btn, radius);
        AnchorPane.setRightAnchor(btn, 0.0);
        AnchorPane.setBottomAnchor(btn, 0.0);

        // optional: set a fixed card size if you want
        ap.setPrefSize(126, 168);

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
        //     deckStatusLabel.setStyle("-fx-text-fill: green;");
        // } else {
        //     deckStatusLabel.setStyle("-fx-text-fill: orange;");
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
