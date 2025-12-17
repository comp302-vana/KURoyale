package kuroyale.mainpack;

import  javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

public class PointsCounter extends HBox {
    private int ourPoints=0;
    private int enemyPoints=0;
    private final Label ourLabel;
    private final Label enemyLabel;

    public PointsCounter(int size) {
        ourLabel=createCounterLabel(size);
        enemyLabel=createCounterLabel(size);

        StackPane ourBox = createBox("US", ourLabel);
        StackPane enemyBox = createBox("ENEMY", enemyLabel);

        setSpacing(size /2);

        setAlignment(Pos.CENTER);

        getChildren().addAll(ourBox, enemyBox);

    }
    private StackPane createBox(String title, Label counterLabel){
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");

        VBox content = new VBox(5, titleLabel, counterLabel);
        content.setAlignment(Pos.CENTER);

        StackPane box = new StackPane(content);
        box.setPrefSize(80, 80);
        box.setStyle("-fx-background-color: white;" + "-fx-border-color: black;" + "-fx-border-width: 2;");
        return box;
    }
    private Label createCounterLabel(int size){
        Label label= new Label("0");
        label.setStyle("-fx-font-size:"+ size+ "px;" + "-fx-text-fill: black;" + "-fx-font-weight: bold;");
        return label;
    }

    public void addToUs(){
        ourPoints++;
        ourLabel.setText(String.valueOf(ourPoints));
    }

    public void addToEnemy(){
        enemyPoints++;
        enemyLabel.setText(String.valueOf(enemyPoints));
    }
    public void setOurPoints(int point){
        ourPoints=point;
        ourLabel.setText(String.valueOf(ourPoints));
    }
    public void setEnemyPoints(int point){
        enemyPoints=point;
        enemyLabel.setText(String.valueOf(enemyPoints));
    }

    public int getOurPoints(){
        return ourPoints;
    }
    public int getEnemyPoints(){
        return enemyPoints;
    }


}
