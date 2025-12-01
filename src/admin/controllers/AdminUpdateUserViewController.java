package admin.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class AdminUpdateUserViewController {

    @FXML private Button btnCancel;

    @FXML
    private void closeWindow() {

        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}