package kuroyale.mainpack;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;

public class LoadArenaDialogController {

    @FXML
    private ListView<String> savedListView;

    private File savesDir;

    @FXML
    private void initialize() {
        savesDir = new File("saves/");
        savesDir.mkdirs();

        refreshList();
    }

    private void refreshList() {
        savedListView.getItems().clear();

        File[] files = savesDir.listFiles((d, name) -> name.endsWith(".csv"));
        if (files == null)
            return;

        for (File f : files)
            savedListView.getItems().add(f.getName());
    }

    @FXML
    private void onLoadClicked() {
        String selected = savedListView.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        // return value via window user data
        Stage stage = (Stage) savedListView.getScene().getWindow();
        stage.setUserData(selected);
        stage.close();
    }

    @FXML
    private void onDeleteClicked() {
        String selected = savedListView.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        new File(savesDir, selected).delete();
        refreshList();
    }

    @FXML
    private void onCancelClicked() {
        Stage stage = (Stage) savedListView.getScene().getWindow();
        stage.setUserData(null);
        stage.close();
    }

    @FXML
    private void onSetDefaultClicked() {
        String selected = savedListView.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        try {
            File f = new File("saves/default.txt");
            try (var pw = new java.io.PrintWriter(f)) {
                pw.println(selected);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Stage stage = (Stage) savedListView.getScene().getWindow();
        stage.setUserData("DEFAULT_SET:" + selected);
        stage.close();
    }

}
