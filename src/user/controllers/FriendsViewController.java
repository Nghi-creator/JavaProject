package user.controllers;

import core.shared.controllers.HeaderController;
import core.shared.controllers.SceneSwitcher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class FriendsViewController {

    @FXML private VBox friendListPane;
    @FXML private VBox requestsPane;
    @FXML private VBox findFriendsPane;
    @FXML private VBox blockedPane;

    @FXML private Button btnFriendList;
    @FXML private Button btnFriendRequests;
    @FXML private Button btnFindFriends;
    @FXML private Button btnBlocked;

    @FXML private HeaderController headerController;

    @FXML
    private void initialize() {
        headerController.focusButton("friends");
    }



    @FXML
    private void handleDashboardClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/ChatroomView.fxml");
    }

    @FXML
    private void handleCreateGroupClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/CreateGroupView.fxml");
    }

    @FXML
    private void handleNotificationClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/NotificationView.fxml");
    }

    @FXML
    private void handleAccountClick(ActionEvent actionEvent) {
    }



    @FXML
    private void showFriendList() {
        updateView(friendListPane, btnFriendList);
    }

    @FXML
    private void showRequests() {
        updateView(requestsPane, btnFriendRequests);
    }

    @FXML
    private void showFindFriends() {
        updateView(findFriendsPane, btnFindFriends);
    }

    @FXML
    private void showBlocked() {
        updateView(blockedPane, btnBlocked);
    }

    private void updateView(VBox paneToShow, Button activeBtn) {

        friendListPane.setVisible(false);
        requestsPane.setVisible(false);
        findFriendsPane.setVisible(false);
        blockedPane.setVisible(false);


        paneToShow.setVisible(true);


        String inactiveStyle = "friends-nav-button";
        String activeStyle = "friends-nav-button-selected";

        btnFriendList.getStyleClass().removeAll(activeStyle, inactiveStyle);
        btnFriendList.getStyleClass().add(inactiveStyle);

        btnFriendRequests.getStyleClass().removeAll(activeStyle, inactiveStyle);
        btnFriendRequests.getStyleClass().add(inactiveStyle);

        btnFindFriends.getStyleClass().removeAll(activeStyle, inactiveStyle);
        btnFindFriends.getStyleClass().add(inactiveStyle);

        btnBlocked.getStyleClass().removeAll(activeStyle, inactiveStyle);
        btnBlocked.getStyleClass().add(inactiveStyle);


        activeBtn.getStyleClass().remove(inactiveStyle);
        activeBtn.getStyleClass().add(activeStyle);
    }
}