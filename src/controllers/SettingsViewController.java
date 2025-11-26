package controllers;

import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SettingsViewController {

    @FXML private ComboBox<String> themeSelector;
    @FXML private CheckBox notificationsCheck;


    @FXML private VBox accountEditPanel;
    @FXML private VBox changePasswordPanel;


    private boolean isAccountPanelVisible = false;
    private boolean isChangePasswordPanelVisible = false;

    private static final Duration ANIMATION_DURATION = Duration.millis(200);

    @FXML
    private void initialize() {
        themeSelector.getSelectionModel().select("Dark");

        
    }

    @FXML
    private void handleSave() {
        String theme = themeSelector.getValue();
        boolean notifications = notificationsCheck.isSelected();


        System.out.println("Theme: " + theme + ", Notifications: " + notifications);
    }

    @FXML
    private void handleBack() {
        SceneSwitcher.switchScene((javafx.scene.Node) themeSelector, "/fxml/ChatroomView.fxml");
    }



    @FXML
    public void handleUpdateAccountInfo(ActionEvent actionEvent) {
        togglePanel(accountEditPanel, isAccountPanelVisible);
        isAccountPanelVisible = !isAccountPanelVisible;
    }

    @FXML
    public void handleChangePassword(ActionEvent actionEvent) {
        togglePanel(changePasswordPanel, isChangePasswordPanelVisible);
        isChangePasswordPanelVisible = !isChangePasswordPanelVisible;
    }

    @FXML
    public void handleSaveAccount(ActionEvent actionEvent) {

        System.out.println("Account info saved");

        collapsePanel(accountEditPanel);
        isAccountPanelVisible = false;
    }



    /**
     * Toggle a panel: expand if hidden, collapse if visible.
     */
    private void togglePanel(VBox panel, boolean isVisible) {
        if (isVisible) {
            collapsePanel(panel);
        } else {
            expandPanel(panel);
        }
    }

    private void expandPanel(VBox panel) {
        panel.setVisible(true);
        panel.setManaged(true);


        panel.setPrefHeight(0);
        panel.applyCss();
        panel.layout();

        double targetHeight = panel.prefHeight(-1);

        Timeline timeline = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(panel.prefHeightProperty(), targetHeight, Interpolator.EASE_BOTH)
                )
        );


        panel.setOpacity(0);
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, panel);
        fade.setFromValue(0);
        fade.setToValue(1);

        ParallelTransition anim = new ParallelTransition(timeline, fade);
        anim.play();
    }

    private void collapsePanel(VBox panel) {
        double currentHeight = panel.getHeight();

        Timeline timeline = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(panel.prefHeightProperty(), 0, Interpolator.EASE_BOTH)
                )
        );


        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, panel);
        fade.setFromValue(panel.getOpacity());
        fade.setToValue(0);

        ParallelTransition anim = new ParallelTransition(timeline, fade);
        anim.setOnFinished(e -> {
            panel.setManaged(false);
            panel.setVisible(false);
        });
        anim.play();
    }

    public void handleSavePassword(ActionEvent actionEvent) {
    }

    public void handleGeneratePassword(ActionEvent actionEvent) {
    }
}
