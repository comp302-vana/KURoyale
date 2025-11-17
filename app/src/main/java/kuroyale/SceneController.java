package kuroyale;

import java.io.IOException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneController {
    private Stage stage;
    private Scene scene;
    private Parent root;

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
    void btnStartBattleClicked(ActionEvent event) {
        
    }

    private void switchToStartBattleScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/kuroyale/StartBattleScene.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private void switchToDeckBuilderScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/kuroyale/DeckBuilderScene.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private void switchToArenaBuilderScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/kuroyale/ArenaBuilderScene.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
