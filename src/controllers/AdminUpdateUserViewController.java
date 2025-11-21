package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class AdminUpdateUserViewController {

    @FXML private Button btnCancel;

    @FXML
    private void closeWindow() {
        // Get the stage from the button's scene and close it
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}