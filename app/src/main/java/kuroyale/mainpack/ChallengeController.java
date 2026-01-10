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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    private Label challenge1Label, challenge1Desc, challenge1Reward, challenge1Stars;
    @FXML
    private Button challenge1Button;
    @FXML
    private Label challenge2Label, challenge2Desc, challenge2Reward, challenge2Stars;
    @FXML
    private Button challenge2Button;
    @FXML
    private Label challenge3Label, challenge3Desc, challenge3Reward, challenge3Stars;
    @FXML
    private Button challenge3Button;
    @FXML
    private Label challenge4Label, challenge4Desc, challenge4Reward, challenge4Stars;
    @FXML
    private Button challenge4Button;
    @FXML
    private Label challenge5Label, challenge5Desc, challenge5Reward, challenge5Stars;
    @FXML
    private Button challenge5Button;

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
        
        Challenge[] challenges = new Challenge[5];
        challenges[0] = challengeManager.getChallenge(Challenge.ChallengeType.SWARM_MASTER);
        challenges[1] = challengeManager.getChallenge(Challenge.ChallengeType.SPELL_BARRAGE);
        challenges[2] = challengeManager.getChallenge(Challenge.ChallengeType.NO_BUILDINGS_ALLOWED);
        challenges[3] = challengeManager.getChallenge(Challenge.ChallengeType.BUDGET_BATTLE);
        challenges[4] = challengeManager.getChallenge(Challenge.ChallengeType.TANK_RUSH);
        
        Label[] starLabels = {challenge1Stars, challenge2Stars, challenge3Stars, challenge4Stars, challenge5Stars};
        Button[] buttons = {challenge1Button, challenge2Button, challenge3Button, challenge4Button, challenge5Button};
        
        for (int i = 0; i < 5; i++) {
            Challenge challenge = challenges[i];
            boolean unlocked = challengeManager.isChallengeUnlocked(challenge.getType());
            
            // Update stars display
            int stars = challenge.getStarsEarned();
            String starText = "";
            for (int j = 0; j < 3; j++) {
                starText += (j < stars) ? "★" : "☆";
            }
            starLabels[i].setText("Stars: " + starText);
            
            // Enable/disable button based on unlock status
            buttons[i].setDisable(!unlocked);
            if (!unlocked) {
                buttons[i].setText("Locked");
            } else {
                buttons[i].setText(challenge.isCompleted() ? "Replay" : "Start Challenge");
            }
        }
    }

    @FXML
    void challenge1Clicked(ActionEvent event) throws IOException {
        selectedChallengeType = Challenge.ChallengeType.SWARM_MASTER;
        switchToDifficultySelectionScene(event);
    }

    @FXML
    void challenge2Clicked(ActionEvent event) throws IOException {
        selectedChallengeType = Challenge.ChallengeType.SPELL_BARRAGE;
        switchToDifficultySelectionScene(event);
    }

    @FXML
    void challenge3Clicked(ActionEvent event) throws IOException {
        selectedChallengeType = Challenge.ChallengeType.NO_BUILDINGS_ALLOWED;
        switchToDifficultySelectionScene(event);
    }

    @FXML
    void challenge4Clicked(ActionEvent event) throws IOException {
        selectedChallengeType = Challenge.ChallengeType.BUDGET_BATTLE;
        switchToDifficultySelectionScene(event);
    }

    @FXML
    void challenge5Clicked(ActionEvent event) throws IOException {
        selectedChallengeType = Challenge.ChallengeType.TANK_RUSH;
        switchToDifficultySelectionScene(event);
    }

    @FXML
    void btnBackFromChallengeClicked(ActionEvent event) throws IOException {
        switchToStartBattleScene(event);
    }

    private void switchToDifficultySelectionScene(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/DifficultySelectionScene.fxml"));
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

    public static ChallengeType getSelectedChallengeType() {
        return selectedChallengeType;
    }
}