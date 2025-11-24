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

public class HeaderController {

    @FXML private Button chatButton;
    @FXML private Button friendsButton;
    @FXML private Button createGroupButton;
    @FXML private Button notificationButton;
    @FXML private Button accountButton;
    @FXML private HBox accountMenu;

    private boolean menuVisible = false;

    @FXML
    public void initialize() {
        accountMenu.setOpacity(0);
        accountMenu.setVisible(false);
        accountMenu.setManaged(false); // so layout doesn’t reserve space
    }

    public void focusButton(String buttonStr) {
        Button button = switch (buttonStr) {
            case "chat" -> chatButton;
            case "friends" -> friendsButton;
            case "createGroup" -> createGroupButton;
            case "notification" -> notificationButton;
            default -> null;
        };

        button.getStyleClass().remove("nav-button");
        button.getStyleClass().add("nav-button-selected");
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
    private void handleChatClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/ChatroomView.fxml");
    }

    @FXML void handleFriendsClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/FriendsView.fxml");
    }

    @FXML
    private void handleCreateGroupClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/CreateGroupView.fxml");
    }

    @FXML
    private void handleNotificationClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/NotificationView.fxml");
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
    private void handleProfileClick(ActionEvent event) {}

    @FXML
    private void handleSettingsClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/SettingsView.fxml");
    }

    @FXML
    private void handleLogoutClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/LoginView.fxml");
    }

}
