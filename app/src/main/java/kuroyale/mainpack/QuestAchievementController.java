package kuroyale.mainpack;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import kuroyale.mainpack.managers.PersistenceManager;
import kuroyale.mainpack.managers.QuestManager;
import kuroyale.mainpack.managers.AchievementManager;
import kuroyale.mainpack.models.Quest;
import kuroyale.mainpack.models.Achievement;
import kuroyale.mainpack.models.PlayerProfile;
import kuroyale.mainpack.models.PlayerStatistics;

public class QuestAchievementController implements Initializable {
    
    @FXML
    private TabPane mainTabPane;
    
    @FXML
    private Tab dailyQuestsTab;
    
    @FXML
    private Tab achievementsTab;
    
    @FXML
    private VBox questsContainer;
    
    @FXML
    private VBox achievementsContainer;
    
    @FXML
    private Label resetTimerLabel;
    
    @FXML
    private Button backButton;
    
    private QuestManager questManager;
    private AchievementManager achievementManager;
    private PersistenceManager persistenceManager;
    private Timeline timerUpdateTimeline;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize managers
        persistenceManager = new PersistenceManager();
        questManager = new QuestManager();
        achievementManager = new AchievementManager();
        
        // Load data from profile
        PlayerProfile profile = persistenceManager.loadPlayerProfile();
        questManager.setDailyQuests(profile.getDailyQuests());
        questManager.setLastResetTimestamp(profile.getLastQuestResetTimestamp());
        
        // Load achievements from profile only if they exist
        List<Achievement> profileAchievements = profile.getAchievements();
        if (profileAchievements != null && !profileAchievements.isEmpty()) {
            achievementManager.setAchievements(profileAchievements);
        }
        // Otherwise, keep the initialized achievements from AchievementManager constructor
        
        // Update achievements with current statistics
        PlayerStatistics stats = profile.getStatistics();
        if (stats != null) {
            achievementManager.updateFromStatistics(stats);
        }
        
        // Initialize quests (check for daily reset)
        questManager.initializeDailyQuests();
        
        // Update UI
        refreshQuestsDisplay();
        refreshAchievementsDisplay();
        updateResetTimer();
        
        // Start timer update (every second)
        timerUpdateTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateResetTimer()));
        timerUpdateTimeline.setCycleCount(Animation.INDEFINITE);
        timerUpdateTimeline.play();
    }
    
    private void refreshQuestsDisplay() {
        questsContainer.getChildren().clear();
        
        List<Quest> quests = questManager.getDailyQuests();
        
        if (quests.isEmpty()) {
            Label noQuestsLabel = new Label("No daily quests available.");
            noQuestsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            questsContainer.getChildren().add(noQuestsLabel);
            return;
        }
        
        for (Quest quest : quests) {
            VBox questBox = createQuestBox(quest);
            questsContainer.getChildren().add(questBox);
        }
    }
    
    private VBox createQuestBox(Quest quest) {
        VBox questBox = new VBox(8);
        questBox.setStyle("-fx-padding: 15px; -fx-background-color: #f5f5f5; -fx-background-radius: 8px; -fx-spacing: 8px;");
        
        // Quest description
        Label descriptionLabel = new Label(quest.getQuestType().getDescription());
        descriptionLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Progress bar
        ProgressBar progressBar = new ProgressBar();
        double progress = quest.getQuestType().getTargetValue() > 0 
            ? (double) quest.getCurrentProgress() / quest.getQuestType().getTargetValue() 
            : 0.0;
        progressBar.setProgress(Math.min(1.0, progress));
        progressBar.setPrefWidth(600);
        progressBar.setPrefHeight(25);
        
        // Progress text
        String progressText = quest.getCurrentProgress() + " / " + quest.getQuestType().getTargetValue();
        Label progressLabel = new Label(progressText);
        progressLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        
        // Reward and status
        HBox infoBox = new HBox(10);
        Label rewardLabel = new Label("Reward: " + quest.getQuestType().getGoldReward() + " gold");
        rewardLabel.setStyle("-fx-font-size: 14px;");
        
        Label statusLabel = new Label();
        if (quest.getClaimed()) {
            statusLabel.setText("✓ Claimed");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4CAF50;");
        } else if (quest.getCompleted()) {
            statusLabel.setText("✓ Completed");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2196F3;");
        } else {
            statusLabel.setText("In Progress");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FF9800;");
        }
        
        infoBox.getChildren().addAll(rewardLabel, statusLabel);
        
        // Claim button
        Button claimButton = new Button("Claim Reward");
        claimButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 16px;");
        claimButton.setDisable(!quest.getCompleted() || quest.getClaimed());
        
        claimButton.setOnAction(e -> claimQuestReward(quest, claimButton, statusLabel));
        
        questBox.getChildren().addAll(descriptionLabel, progressBar, progressLabel, infoBox, claimButton);
        
        return questBox;
    }
    
    private void claimQuestReward(Quest quest, Button claimButton, Label statusLabel) {
        int goldReward = questManager.claimQuestReward(quest);
        
        if (goldReward > 0) {
            // Update profile with gold
            PlayerProfile profile = persistenceManager.loadPlayerProfile();
            profile.setTotalGold(profile.getTotalGold() + goldReward);
            profile.addGoldTransaction(goldReward, "Quest Reward: " + quest.getQuestType().getDescription());
            profile.setDailyQuests(questManager.getDailyQuests());
            persistenceManager.savePlayerProfile(profile);
            
            // Update UI
            claimButton.setDisable(true);
            statusLabel.setText("✓ Claimed");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4CAF50;");
            
            // Show notification
            showNotification("Quest Reward Claimed!", "+" + goldReward + " Gold", 3);
        }
    }
    
    private void refreshAchievementsDisplay() {
        achievementsContainer.getChildren().clear();
        
        List<Achievement> achievements = achievementManager.getAchievements();
        
        for (Achievement achievement : achievements) {
            VBox achievementBox = createAchievementBox(achievement);
            achievementsContainer.getChildren().add(achievementBox);
        }
    }
    
    private VBox createAchievementBox(Achievement achievement) {
        VBox achievementBox = new VBox(8);
        
        // Style based on completion status
        if (achievement.getClaimed()) {
            achievementBox.setStyle("-fx-padding: 15px; -fx-background-color: #e8f5e9; -fx-background-radius: 8px; -fx-spacing: 8px;");
        } else if (achievement.getCompleted()) {
            achievementBox.setStyle("-fx-padding: 15px; -fx-background-color: #e3f2fd; -fx-background-radius: 8px; -fx-spacing: 8px;");
        } else {
            achievementBox.setStyle("-fx-padding: 15px; -fx-background-color: #f5f5f5; -fx-background-radius: 8px; -fx-spacing: 8px; -fx-opacity: 0.6;");
        }
        
        // Achievement name
        Label nameLabel = new Label(achievement.getAchievementType().getName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Achievement description
        Label descriptionLabel = new Label(achievement.getAchievementType().getDescription());
        descriptionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        
        // Progress bar
        ProgressBar progressBar = new ProgressBar();
        double progress = achievement.getAchievementType().getTargetValue() > 0 
            ? (double) achievement.getCurrentProgress() / achievement.getAchievementType().getTargetValue() 
            : 0.0;
        progressBar.setProgress(Math.min(1.0, progress));
        progressBar.setPrefWidth(600);
        progressBar.setPrefHeight(25);
        
        // Progress text
        String progressText;
        if (achievement.getCompleted()) {
            progressText = achievement.getCurrentProgress() + " / " + achievement.getAchievementType().getTargetValue() + " (Completed)";
        } else {
            progressText = achievement.getCurrentProgress() + " / " + achievement.getAchievementType().getTargetValue();
        }
        Label progressLabel = new Label(progressText);
        progressLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        
        // Reward and status
        HBox infoBox = new HBox(10);
        Label rewardLabel = new Label("Reward: " + achievement.getAchievementType().getGoldReward() + " gold");
        rewardLabel.setStyle("-fx-font-size: 14px;");
        
        Label statusLabel = new Label();
        if (achievement.getClaimed()) {
            statusLabel.setText("✓ Claimed");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4CAF50;");
        } else if (achievement.getCompleted()) {
            statusLabel.setText("✓ Completed");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2196F3;");
        } else {
            statusLabel.setText("Locked");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #999;");
        }
        
        infoBox.getChildren().addAll(rewardLabel, statusLabel);
        
        // Claim button
        Button claimButton = new Button("Claim Reward");
        claimButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 16px;");
        claimButton.setDisable(!achievement.getCompleted() || achievement.getClaimed());
        claimButton.setVisible(achievement.getCompleted() && !achievement.getClaimed());
        
        claimButton.setOnAction(e -> claimAchievementReward(achievement, claimButton, statusLabel));
        
        achievementBox.getChildren().addAll(nameLabel, descriptionLabel, progressBar, progressLabel, infoBox, claimButton);
        
        return achievementBox;
    }
    
    private void claimAchievementReward(Achievement achievement, Button claimButton, Label statusLabel) {
        int goldReward = achievementManager.claimAchievementReward(achievement);
        
        if (goldReward > 0) {
            // Update profile with gold
            PlayerProfile profile = persistenceManager.loadPlayerProfile();
            profile.setTotalGold(profile.getTotalGold() + goldReward);
            profile.addGoldTransaction(goldReward, "Achievement Reward: " + achievement.getAchievementType().getName());
            profile.setAchievements(achievementManager.getAchievements());
            persistenceManager.savePlayerProfile(profile);
            
            // Update UI
            claimButton.setDisable(true);
            claimButton.setVisible(false);
            statusLabel.setText("✓ Claimed");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4CAF50;");
            
            // Show notification
            showNotification("Achievement Unlocked!", "+" + goldReward + " Gold", 3);
        }
    }
    
    private void updateResetTimer() {
        long timeUntilReset = questManager.getTimeUntilReset();
        
        if (timeUntilReset <= 0) {
            resetTimerLabel.setText("Reset: Available now");
            return;
        }
        
        long hours = timeUntilReset / (60 * 60 * 1000);
        long minutes = (timeUntilReset % (60 * 60 * 1000)) / (60 * 1000);
        long seconds = (timeUntilReset % (60 * 1000)) / 1000;
        
        resetTimerLabel.setText(String.format("Reset in: %02d:%02d:%02d", hours, minutes, seconds));
    }
    
    private void showNotification(String title, String message, int durationSeconds) {
        // Simple notification - you can enhance this with animations
        System.out.println(title + " - " + message);
    }
    
    @FXML
    private void handleBackButton(ActionEvent event) throws IOException {
        // Save current state before leaving
        PlayerProfile profile = persistenceManager.loadPlayerProfile();
        profile.setDailyQuests(questManager.getDailyQuests());
        profile.setLastQuestResetTimestamp(questManager.getLastResetTimestamp());
        profile.setAchievements(achievementManager.getAchievements());
        persistenceManager.savePlayerProfile(profile);
    
        // Stop timer
        if (timerUpdateTimeline != null) {
            timerUpdateTimeline.stop();
        }
    
        // Navigate back to previous scene
        Parent root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/StartBattleScene.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 1280, 720, Color.web("0xBD7FFF"));
        root.setStyle("-fx-background-color: BD7FFF;");
        stage.setScene(scene);
        stage.show();
    }
}