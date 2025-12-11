package com.example.chatroom.admin.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminPasswordRequestsViewController {

    @FXML private TableView<PasswordRequest> requestTable;
    @FXML private TableColumn<PasswordRequest, String> colUsername;
    @FXML private TableColumn<PasswordRequest, String> colDate;
    @FXML private TableColumn<PasswordRequest, Void> colAction;

    private ObservableList<PasswordRequest> requests = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Setup Columns
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));

        // Setup Action Button (Reset)
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnReset = new Button("Approve Reset");

            {
                btnReset.getStyleClass().add("admin-action-button");
                btnReset.setOnAction(event -> {
                    PasswordRequest req = getTableView().getItems().get(getIndex());
                    handleResetAction(req);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnReset);
                }
            }
        });

        requestTable.setItems(requests);
        loadDummyData(); // Placeholder until backend is ready
    }

    private void loadDummyData() {
        // Temporary data to test the UI
        requests.add(new PasswordRequest("john_doe", "2023-10-25"));
        requests.add(new PasswordRequest("jane_smith", "2023-10-26"));
    }

    private void handleResetAction(PasswordRequest req) {
        System.out.println("Approved reset for: " + req.getUsername());
        requests.remove(req); // Remove from list after approval
        // TODO: Call API to actually reset password
    }

    // Helper Class for the Table
    public static class PasswordRequest {
        private String username;
        private String requestDate;

        public PasswordRequest(String username, String requestDate) {
            this.username = username;
            this.requestDate = requestDate;
        }

        public String getUsername() { return username; }
        public String getRequestDate() { return requestDate; }
    }
}