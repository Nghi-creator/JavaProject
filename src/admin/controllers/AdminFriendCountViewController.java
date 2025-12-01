package admin.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.beans.property.SimpleStringProperty;

import java.util.Comparator;

public class AdminFriendCountViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colUsername, colFullname, colAddress, colDob, colFriends, colFriendsOfFriends, colAction;
    @FXML private ComboBox<String> sortCombo, filterCombo;

    private ObservableList<User> masterData;

    @FXML
    public void initialize() {
        headerController.focusButton("friendCount");


        sortCombo.setItems(FXCollections.observableArrayList("Name (A-Z)", "Most Friends"));
        filterCombo.setItems(FXCollections.observableArrayList("Name", "Username", "Status"));


        setupColumn(colUsername, data -> data.username);
        setupColumn(colFullname, data -> data.fullname);
        setupColumn(colAddress, data -> data.address);
        setupColumn(colDob, data -> data.dob);
        setupColumn(colFriends, data -> String.valueOf(data.friends));
        setupColumn(colFriendsOfFriends, data -> String.valueOf(data.friendsOfFriends));


        colAction.setSortable(false);
        colAction.setReorderable(false);
        colAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<User, String> call(TableColumn<User, String> param) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox buttons = new HBox(10);
                            buttons.setAlignment(Pos.CENTER_LEFT);

                            Button btnUpdate = new Button("Update");
                            btnUpdate.getStyleClass().add("admin-action-button");
                            btnUpdate.setOnAction(e -> AdminSceneSwitcher.openPopup("/admin/ui/fxml/AdminUpdateUserView.fxml", "Update User"));

                            Button btnDelete = new Button("Delete");
                            btnDelete.getStyleClass().add("admin-danger-button");
                            btnDelete.setOnAction(e -> showDeleteConfirmation());

                            buttons.getChildren().addAll(btnUpdate, btnDelete);
                            setGraphic(buttons);
                        }
                    }
                };
            }
        });


        masterData = FXCollections.observableArrayList(
                new User("john_doe", "John Doe", "123 Street, NY", "1990-01-01", 5, 12),
                new User("jane_smith", "Jane Smith", "456 Avenue, CA", "1992-05-10", 8, 20),
                new User("bobby_g", "Bob Gamer", "789 Road, TX", "1995-11-20", 2, 4)
        );
        userTable.setItems(masterData);


        sortCombo.setOnAction(event -> {
            String selected = sortCombo.getValue();
            if (selected == null) return;

            if (selected.equals("Name (A-Z)")) {
                FXCollections.sort(masterData, Comparator.comparing(u -> u.fullname.toLowerCase()));
            } else if (selected.equals("Most Friends")) {
                FXCollections.sort(masterData, Comparator.comparingInt((User u) -> u.friends).reversed());
            }
        });
    }

    private void setupColumn(TableColumn<User, String> column, Callback<User, String> valueExtractor) {
        column.setCellValueFactory(data -> new SimpleStringProperty(valueExtractor.call(data.getValue())));
        column.setReorderable(false);
        column.setSortable(false);
    }

    private void showDeleteConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Confirm Deletion");
        alert.setContentText("Are you sure you want to delete this user?");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/shared/ui/css/DiscordTheme.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }

    @FXML private void goToHistory(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/admin/ui/fxml/AdminLoginHistoryView.fxml"); }
    @FXML private void goToGroups(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/admin/ui/fxml/AdminGroupView.fxml"); }
    @FXML private void goToReports(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/admin/ui/fxml/AdminReportView.fxml"); }

    public static class User {
        String username, fullname, address, dob;
        int friends, friendsOfFriends;

        public User(String u, String f, String a, String d, int friends, int fof) {
            this.username = u; this.fullname = f;
            this.address = a; this.dob = d;
            this.friends = friends; this.friendsOfFriends = fof;
        }
    }
}
