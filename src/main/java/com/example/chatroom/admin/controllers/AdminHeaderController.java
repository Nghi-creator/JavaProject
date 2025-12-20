package com.example.chatroom.admin.controllers;

import java.util.List;

import com.example.chatroom.core.shared.controllers.SceneSwitcher;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;

public class AdminHeaderController {

    @FXML private Button usersButton;
    @FXML private Button friendCountButton;
    @FXML private Button userActivityButton;
    @FXML private Button loginHistoryButton;
    @FXML private Button groupsButton;
    @FXML private Button spamReportsButton;
    @FXML private Button userRegistrationChartButton;
    @FXML private Button activeUsersChartButton;

    private List<Button> navButtons;

    @FXML
    public void initialize() {
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
            case "userRegistrationChart", "regChart" -> userRegistrationChartButton;
            case "activeUsersChart", "activeChart" -> activeUsersChartButton;
            default -> null;
        };

        if (buttonToFocus != null) {
            buttonToFocus.getStyleClass().remove("nav-button");
            buttonToFocus.getStyleClass().add("nav-button-selected");
        }
    }

    // --- NAVIGATION HANDLERS ---

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
    public void handleUserRegistrationChartClick(ActionEvent event) {
        SceneSwitcher.switchScene((Node) event.getSource(), "/admin/ui/fxml/AdminUserRegistrationChartView.fxml");
    }

    @FXML
    public void handleActiveUsersChartClick(ActionEvent event) {
        SceneSwitcher.switchScene((Node) event.getSource(), "/admin/ui/fxml/AdminActiveUsersChartView.fxml");
    }
}