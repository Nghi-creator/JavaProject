package admin.controllers;

import core.shared.controllers.SceneSwitcher;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
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
    @FXML private HBox firstRow;
    @FXML private HBox secondRow;
    @FXML private ImageView arrowImage;
    @FXML private HBox accountMenu;

    private List<Button> navButtons;
    private boolean menuVisible = false;

    @FXML
    public void initialize() {
        accountMenu.setOpacity(0);
        accountMenu.setVisible(false);
        accountMenu.setManaged(false);

        navButtons = List.of(
                usersButton,
                friendCountButton,
                userActivityButton,
                loginHistoryButton,
                groupsButton,
                spamReportsButton,
                userRegistrationChartButton,
                activeUsersChartButton
        );
    }

    public void focusButton(String buttonStr) {
        navButtons.forEach(btn -> {
            btn.getStyleClass().remove("nav-button-selected");
            if (!btn.getStyleClass().contains("nav-button")) {
                btn.getStyleClass().add("nav-button");
            }
        });

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
        accountMenu.setTranslateX(10);

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
        SceneSwitcher.switchScene((Node) event.getSource(), "/admin/ui/fxml/AdminUserView.fxml");
    }

    @FXML
    private void handleFriendCountClick(ActionEvent event) {
        SceneSwitcher.switchScene((Node) event.getSource(), "/admin/ui/fxml/AdminFriendCountView.fxml");
    }

    @FXML
    void handleUserActivityClick(ActionEvent event) {
        SceneSwitcher.switchScene((Node) event.getSource(), "/admin/ui/fxml/AdminUserActivityView.fxml");
    }

    @FXML
    private void handleLoginHistoryClick(ActionEvent event) {
        SceneSwitcher.switchScene((Node) event.getSource(), "/admin/ui/fxml/AdminLoginHistoryView.fxml");
    }

    @FXML
    private void handleGroupsClick(ActionEvent event) {
        SceneSwitcher.switchScene((Node) event.getSource(), "/admin/ui/fxml/AdminGroupView.fxml");
    }

    @FXML
    private void handleSpamReportClick(ActionEvent event) {
        SceneSwitcher.switchScene((Node) event.getSource(), "/admin/ui/fxml/AdminReportView.fxml");
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
        SceneSwitcher.switchScene((Node) event.getSource(), "/user/ui/fxml/SettingsView.fxml");
    }

    @FXML
    private void handleLogoutClick(ActionEvent event) {
        SceneSwitcher.switchScene((Node) event.getSource(), "/user/ui/fxml/LoginView.fxml");
    }

    public void handleUserRegistrationChartClick(ActionEvent event) {
        SceneSwitcher.switchScene((Node) event.getSource(), "/admin/ui/fxml/AdminUserRegistrationChartView.fxml");
    }

    public void handleActiveUsersChartClick(ActionEvent event) {
        SceneSwitcher.switchScene((Node) event.getSource(), "/admin/ui/fxml/AdminActiveUsersChartView.fxml");
    }

    @FXML
    private void handleArrowClick() {
        boolean showingFirst = firstRow.isVisible();
        firstRow.setVisible(!showingFirst);
        firstRow.setManaged(!showingFirst);
        secondRow.setVisible(showingFirst);
        secondRow.setManaged(showingFirst);
        arrowImage.setRotate(180 - arrowImage.getRotate());
    }
}
