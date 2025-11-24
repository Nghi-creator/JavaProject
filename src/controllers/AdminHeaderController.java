package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.List;

public class AdminHeaderController {

    @FXML private Button usersButton;
    @FXML private Button friendCountButton;
    @FXML private Button userActivityButton;
    @FXML private Button loginHistoryButton;
    @FXML private Button groupsButton;
    @FXML private Button spamReportsButton;
    @FXML private Button userRegistrationChartButton;
    @FXML private Button activeUsersChartButton;
    @FXML private Button accountButton;
    @FXML private HBox accountMenu;

    private List<Button> navButtons;

    private boolean menuVisible = false;

    @FXML
    public void initialize() {
        accountMenu.setOpacity(0);
        accountMenu.setVisible(false);
        accountMenu.setManaged(false);

        // collect all nav buttons into a list for easy focus management
        navButtons = List.of(
                usersButton, friendCountButton, userActivityButton, loginHistoryButton,
                groupsButton, spamReportsButton, userRegistrationChartButton, activeUsersChartButton
        );
    }

    public void focusButton(String buttonStr) {
        // first, remove focus style from all
        navButtons.forEach(btn -> {
            btn.getStyleClass().remove("nav-button-selected");
            if (!btn.getStyleClass().contains("nav-button")) {
                btn.getStyleClass().add("nav-button");
            }
        });

        // then, find the one to focus
        Button buttonToFocus = switch (buttonStr) {
            case "users" -> usersButton;
            case "friendCount" -> friendCountButton;
            case "userActivity" -> userActivityButton;
            case "loginHistory" -> loginHistoryButton;
            case "groups" -> groupsButton;
            case "spamReports" -> spamReportsButton;
            case "userRegistrationChart" -> userRegistrationChartButton;
            case "activeUsersChart" -> activeUsersChartButton;
            default -> null;
        };

        if (buttonToFocus != null) {
            buttonToFocus.getStyleClass().remove("nav-button");
            buttonToFocus.getStyleClass().add("nav-button-selected");
        }
    }

    private void showMenu() {
        accountMenu.setVisible(true);
        accountMenu.setManaged(true);
        accountMenu.setTranslateX(10); // slightly shifted for effect

        FadeTransition fade = new FadeTransition(Duration.millis(200), accountMenu);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(200), accountMenu);
        slide.setFromX(10);
        slide.setToX(0);

        ParallelTransition anim = new ParallelTransition(fade, slide);
        anim.play();
    }

    private void hideMenu() {
        FadeTransition fade = new FadeTransition(Duration.millis(150), accountMenu);
        fade.setFromValue(1);
        fade.setToValue(0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(150), accountMenu);
        slide.setFromX(0);
        slide.setToX(10);

        ParallelTransition anim = new ParallelTransition(fade, slide);
        anim.setOnFinished(e -> {
            accountMenu.setVisible(false);
            accountMenu.setManaged(false);
        });
        anim.play();
    }

    @FXML
    private void handleUsersClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/AdminUserView.fxml");
    }

    @FXML
    private void handleFriendCountClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/AdminFriendCountView.fxml");
    }

    @FXML void handleUserActivityClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/AdminUserActivityView.fxml");
    }

    @FXML
    private void handleLoginHistoryClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/AdminLoginHistoryView.fxml");
    }

    @FXML
    private void handleGroupsClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/AdminGroupView.fxml");
    }

    @FXML
    private void handleSpamReportClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/AdminReportView.fxml");
    }

    @FXML
    private void handleAccountClick() {
        if (menuVisible) {
            hideMenu();
        } else {
            showMenu();
        }
        menuVisible = !menuVisible;
    }

    @FXML
    private void handleSettingsClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/SettingsView.fxml");
    }

    @FXML
    private void handleLogoutClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/LoginView.fxml");
    }

    public void handleUserRegistrationChartClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/AdminUserRegistrationChartView.fxml");
    }

    public void handleActiveUsersChartClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/AdminActiveUsersChartView.fxml");
    }
}
