package kuroyale.mainpack;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import kuroyale.mainpack.managers.ChallengeManager;
import kuroyale.mainpack.managers.PersistenceManager;
import kuroyale.mainpack.models.Challenge;
import kuroyale.mainpack.models.Challenge.ChallengeType;
import kuroyale.mainpack.models.PlayerProfile;

public class ChallengeController implements Initializable {

    private static ChallengeType selectedChallengeType = null;

    @FXML
    private FlowPane challengeContainer;

    @FXML
    private VBox statsVBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeChallengeSelection();
    }

    /**
     * Initializes challenge selection screen with challenge status.
     * Factory Pattern: Uses ChallengeManager to get challenge instances.
     */
    private void initializeChallengeSelection() {
        PersistenceManager persistenceManager = new PersistenceManager();
        PlayerProfile profile = persistenceManager.loadPlayerProfile();
        ChallengeManager challengeManager = new ChallengeManager(profile);

        // Sync challenges
        profile.setChallenges(challengeManager.getChallenges());
        persistenceManager.savePlayerProfile(profile);

        // Clear container first
        if (challengeContainer != null) {
            challengeContainer.getChildren().clear();
        }

        // Define challenges to show
        ChallengeType[] types = {
                ChallengeType.SWARM_MASTER,
                ChallengeType.SPELL_BARRAGE,
                ChallengeType.NO_BUILDINGS_ALLOWED,
                ChallengeType.BUDGET_BATTLE,
                ChallengeType.TANK_RUSH
        };

        Challenge[] challenges = new Challenge[types.length];

        for (int i = 0; i < types.length; i++) {
            Challenge challenge = challengeManager.getChallenge(types[i]);
            challenges[i] = challenge;
            boolean unlocked = challengeManager.isChallengeUnlocked(challenge.getType());

            addChallengeCard(challenge, unlocked);
        }

        // Populate statistics VBox
        populateStatsVBox(challenges);
    }

    private void addChallengeCard(Challenge challenge, boolean unlocked) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/kuroyale/scenes/ChallengeItem.fxml"));
            Node cardNode = loader.load();
            ChallengeItemController controller = loader.getController();

            controller.setChallenge(challenge, unlocked, () -> {
                try {
                    handleChallengeStart(challenge.getType());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            challengeContainer.getChildren().add(cardNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleChallengeStart(ChallengeType type) throws IOException {
        selectedChallengeType = type;
        // Use the current stage. Since we are inside a lambda, we need a way to get the
        // stage.
        // We can get it from the container's scene.
        Stage stage = (Stage) challengeContainer.getScene().getWindow();
        switchToDifficultySelectionScene(stage);
    }

    private void populateStatsVBox(Challenge[] challenges) {
        if (statsVBox == null)
            return;

        // Clear existing content except title (but we clear everything in FXML anyway,
        // safe to clear here)
        statsVBox.getChildren().clear();

        Label titleLabel = new Label("Statistics");
        titleLabel.getStyleClass().add("stats-title");
        statsVBox.getChildren().add(titleLabel);

        for (Challenge challenge : challenges) {
            VBox challengeStat = new VBox();
            challengeStat.getStyleClass().add("stats-item-box");

            Label nameLabel = new Label(formatTitle(challenge.getType().toString()));
            nameLabel.getStyleClass().add("stats-item-title");

            Label attemptsLabel = new Label("Attempts: " + challenge.getAttempts());
            attemptsLabel.getStyleClass().add("stats-item-value");

            Label completionsLabel = new Label("Completions: " + challenge.getNumOfCompletion());
            completionsLabel.getStyleClass().add("stats-item-value");

            challengeStat.getChildren().addAll(nameLabel, attemptsLabel, completionsLabel);
            statsVBox.getChildren().add(challengeStat);
        }
    }

    // Helper to format enum names pretty
    private String formatTitle(String enumName) {
        String[] parts = enumName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part.charAt(0)).append(part.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    @FXML
    void btnBackFromChallengeClicked(ActionEvent event) throws IOException {
        switchToStartBattleScene(event);
    }

    private void switchToDifficultySelectionScene(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/DifficultySelectionScene.fxml"));
        root.setStyle("-fx-background-color: BD7FFF;");
        Scene scene = new Scene(root, 1280, 720, Color.web("0xBD7FFF"));
        stage.setScene(scene);
        stage.show();
    }

    // Kept for backward compatibility if called from explicit event, though not
    // used by cards
    @Deprecated
    private void switchToDifficultySelectionScene(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        switchToDifficultySelectionScene(stage);
    }

    private void switchToStartBattleScene(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/StartBattleScene.fxml"));
        root.setStyle("-fx-background-color: BD7FFF;");
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 1280, 720, Color.web("0xBD7FFF"));
        stage.setScene(scene);
        stage.show();
    }

    public static ChallengeType getSelectedChallengeType() {
        return selectedChallengeType;
    }
}