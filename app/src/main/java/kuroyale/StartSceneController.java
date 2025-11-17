package kuroyale;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class StartSceneController {

    @FXML
    void btnQuitCliked(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    void btnStartClicked(ActionEvent event) {
        System.out.println("STTAAARRRRTTTTTTTTTT!!!!!!!!!!!!!!!!");
    }

}
