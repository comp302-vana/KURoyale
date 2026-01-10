package kuroyale.mainpack.managers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

/**
 * Manages in-game notifications for quest and achievement completions.
 */
public class NotificationManager {
    private final Pane rootPane;
    private final VBox notificationContainer;
    
    public NotificationManager(Pane rootPane) {
        this.rootPane = rootPane;
        this.notificationContainer = new VBox(10);
        this.notificationContainer.setSpacing(10);
        this.notificationContainer.setPickOnBounds(false);
        
        // Position in bottom-right corner using AnchorPane constraints
        if (rootPane instanceof javafx.scene.layout.AnchorPane) {
            javafx.scene.layout.AnchorPane.setRightAnchor(notificationContainer, 20.0);
            javafx.scene.layout.AnchorPane.setBottomAnchor(notificationContainer, 20.0);
        } else {
            // Fallback: use layout positioning
            notificationContainer.setLayoutX(rootPane.getWidth() - 320);
            notificationContainer.setLayoutY(rootPane.getHeight() - 200);
        }
        
        rootPane.getChildren().add(notificationContainer);
    }
    
    /**
     * Shows a notification popup that auto-closes after 3 seconds.
     */
    public void showNotification(String title, String message) {
        Platform.runLater(() -> {
            StackPane notificationBox = new StackPane();
            notificationBox.setStyle("-fx-background-color: rgb(202, 231, 232); " +
                                   "-fx-background-radius: 10; " +
                                   "-fx-padding: 15; " +
                                   "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 2);");
            notificationBox.setPrefWidth(300);
            
            Button closeButton = new Button("×");
            closeButton.setStyle("-fx-background-color: transparent; " +
                               "-fx-text-fill: #333333; " +
                               "-fx-font-size: 20; " +
                               "-fx-font-weight: bold; " +
                               "-fx-cursor: hand;");
            closeButton.setOnAction(e -> {
                notificationContainer.getChildren().remove(notificationBox);
            });
            
            VBox content = new VBox(5);
            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-text-fill:#dd00ff; " +
                              "-fx-font-size: 18; " +
                              "-fx-font-weight: bold;");
            
            Label messageLabel = new Label(message);
            messageLabel.setStyle("-fx-text-fill: #222222; " +
                                "-fx-font-size: 14;");
            
            content.getChildren().addAll(titleLabel, messageLabel);
            content.setAlignment(Pos.CENTER_LEFT);
            
            HBox layout = new HBox();
            HBox.setHgrow(content, Priority.ALWAYS);
            layout.getChildren().addAll(content, closeButton);
            layout.setAlignment(Pos.CENTER);
            
            notificationBox.getChildren().add(layout);
            notificationContainer.getChildren().add(notificationBox);
            
            // Auto-close after 3 seconds
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
                if (notificationContainer.getChildren().contains(notificationBox)) {
                    notificationContainer.getChildren().remove(notificationBox);
                }
            }));
            timeline.play();
        });
    }
}