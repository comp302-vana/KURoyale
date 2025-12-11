package kuroyale.mainpack;

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
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class UIManager extends Application {
    private Stage stage;
    private Scene scene;
    private Parent root;
    private static String selectedDifficulty = null;

    @Override
    public void start(Stage stage) {
        try {
            Parent root = FXMLLoader.load(UIManager.class.getResource("/kuroyale/scenes/StartScene.fxml"));
            Scene scene = new Scene(root, 1280, 720);
            root.setStyle("-fx-background-color: BD7FFF;");
            stage.setResizable(false);
            stage.setTitle("KURoyale");
            stage.getIcons().add(new Image("/kuroyale/images/icon.png"));
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {}
    }

    @FXML
    void btnQuitCliked(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    void btnStartClicked(ActionEvent event) throws IOException {
        switchToStartBattleScene(event);;
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
        switchToDifficultySelectionScene(event);
    }

    @FXML
    void btnSimpleClicked(ActionEvent event) throws IOException {
        selectedDifficulty = "Simple";
        switchToBattleScene(event);
    }

    @FXML
    void btnMediumClicked(ActionEvent event) throws IOException {
        selectedDifficulty = "Medium";
        switchToBattleScene(event);
    }

    @FXML
    void btnAdvancedClicked(ActionEvent event) throws IOException {
        selectedDifficulty = "Advanced";
        switchToBattleScene(event);
    }


    private void switchToStartBattleScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/kuroyale/scenes/StartBattleScene.fxml"));
        root.setStyle("-fx-background-color: BD7FFF;");
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root, 1280, 720, Color.web("0xBD7FFF"));
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

    public static String getSelectedDifficulty() {
        return selectedDifficulty;
    }
}