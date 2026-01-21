package kuroyale.mainpack;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import kuroyale.mainpack.models.Challenge;

public class ChallengeItemController {

    @FXML
    private VBox cardRoot;

    @FXML
    private Label titleLabel;

    @FXML
    private Label descLabel;

    @FXML
    private Label rewardLabel;

    @FXML
    private Label starsLabel;

    @FXML
    private Button actionButton;

    private Challenge challenge;
    private Runnable onStartAction;

    public void setChallenge(Challenge challenge, boolean unlocked, Runnable onStartAction) {
        this.challenge = challenge;
        this.onStartAction = onStartAction;

        // Set Texts
        titleLabel.setText(formatTitle(challenge.getType().toString()));
        descLabel.setText(getChallengeDescription(challenge.getType()));

        // Rewards are hardcoded in the original controller, ideally should be in
        // Challenge model.
        // For now, I will imply them or switch logic.
        // Original logic had hardcoded rewards labels. I'll map them here for now.
        int reward = getRewardForType(challenge.getType());
        rewardLabel.setText(reward + " Gold");

        // Stars
        int stars = challenge.getStarsEarned();
        StringBuilder starText = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            starText.append(i < stars ? "★" : "☆");
        }
        starsLabel.setText(starText.toString());

        // Button State
        if (!unlocked) {
            actionButton.setText("Locked");
            actionButton.setDisable(true);
            cardRoot.getStyleClass().add("locked-card");
        } else {
            actionButton.setText(challenge.isCompleted() ? "Replay" : "Start");
            actionButton.setDisable(false);
            cardRoot.getStyleClass().removeAll("locked-card");
        }
    }

    @FXML
    void onActionClicked() {
        if (onStartAction != null) {
            onStartAction.run();
        }
    }

    // Helper methods to map data (since it was hardcoded in FXML before)
    private String formatTitle(String enumName) {
        String[] parts = enumName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part.charAt(0)).append(part.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    private String getChallengeDescription(Challenge.ChallengeType type) {
        switch (type) {
            case SWARM_MASTER:
                return "Use only swarm troops";
            case SPELL_BARRAGE:
                return "All 4 spells, spell cost -1";
            case NO_BUILDINGS_ALLOWED:
                return "Only troops or spells, no buildings";
            case BUDGET_BATTLE:
                return "Cards cost ≤3 elixir";
            case TANK_RUSH:
                return "High-HP units only";
            default:
                return "";
        }
    }

    private int getRewardForType(Challenge.ChallengeType type) {
        switch (type) {
            case SWARM_MASTER:
                return 250;
            case SPELL_BARRAGE:
                return 300;
            case NO_BUILDINGS_ALLOWED:
                return 200;
            case BUDGET_BATTLE:
                return 250;
            case TANK_RUSH:
                return 300;
            default:
                return 0;
        }
    }
}
