package kuroyale.mainpack;

import  javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

public class PointsCounter extends HBox {
    private double ourPoints=0;
    private double enemyPoints=0;
    private final Label ourLabel;
    private final Label enemyLabel;

    public PointsCounter() {
        ourLabel=createCounterLabel();
        enemyLabel=createCounterLabel();

        StackPane ourBox = createBox("US", ourLabel);
        StackPane enemyBox = createBox("ENEMY", enemyLabel);

        setSpacing(10);

        setAlignment(Pos.CENTER);

        getChildren().addAll(ourBox, enemyBox);

    }
    private StackPane createBox(String title, Label counterLabel){
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: black;");

        VBox content = new VBox(2, titleLabel, counterLabel);
        content.setAlignment(Pos.CENTER);

        StackPane box = new StackPane(content);
        box.setPrefSize(50, 50);
        box.setStyle("-fx-background-color: white;" + "-fx-border-color: black;" + "-fx-border-width: 2;");
        return box;
    }
    private Label createCounterLabel(){
        Label label= new Label("0");
        label.setStyle("-fx-font-size: 12px;" + "-fx-text-fill: black;" + "-fx-font-weight: bold;");
        return label;
    }
    //REQUIRES: PointsCounter to be initialized
    //MODIFIES: this.ourPoints:
    //EFFECTS: Increments outPoints by 0.111111111111 and updates label by rounding.
    public void addToUs(){
        ourPoints+=0.111111111111;
        ourLabel.setText(String.valueOf((int) Math.round(ourPoints)));
    }
    //REQUIRES: PointsCounter to be initialized
    //MODIFIES: this.enemyPoints:
    //EFFECTS: Increments enemyPoints by 0.111111111111 and updates label by rounding.
    public void addToEnemy(){
        enemyPoints+=0.111111111111;
        enemyLabel.setText(String.valueOf((int) Math.round(enemyPoints)));
    }
    //REQUIRES: PointsCounter to be initialized
    //MODIFIES: this.ourPoints:
    //EFFECTS: Sets outPoints to point and updates label by rounding.
    public void setOurPoints(int point){
        ourPoints=point;
        ourLabel.setText(String.valueOf((int) Math.round(ourPoints)));
    }
    //REQUIRES: PointsCounter to be initialized
    //MODIFIES: this.enemyPoints:
    //EFFECTS: Sets enemyPoints to point and updates label by rounding.
    public void setEnemyPoints(int point){
        enemyPoints=point;
        enemyLabel.setText(String.valueOf((int) Math.round(enemyPoints)));
    }

    public int getOurPoints(){
        return (int) Math.round(ourPoints);
    }
    public int getEnemyPoints() {return (int) Math.round(enemyPoints); }


}
