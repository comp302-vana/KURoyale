package kuroyale;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        try {
            Parent root = FXMLLoader.load(App.class.getResource("/kuroyale/scenes/StartScene.fxml"));
            Scene scene = new Scene(root);
            root.setStyle("-fx-background-color: BD7FFF;");
            stage.setResizable(false);
            stage.setTitle("KURoyale");
            stage.getIcons().add(new Image("/kuroyale/images/icon.png"));
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {}
    }
}
