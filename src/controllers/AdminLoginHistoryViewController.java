package controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleStringProperty;

public class AdminLoginHistoryViewController {
    
    @FXML private TableView<History> historyTable;
    @FXML private TableColumn<History, String> colTime, colUsername, colFullname;

    @FXML
    public void initialize() {
        setupColumn(colTime, d -> d.time);
        setupColumn(colUsername, d -> d.username);
        setupColumn(colFullname, d -> d.fullname);

        historyTable.setItems(FXCollections.observableArrayList(
            new History("2023-11-20 08:30:00", "john_doe", "John Doe"),
            new History("2023-11-20 09:15:22", "jane_smith", "Jane Smith")
        ));
    }

    private void setupColumn(TableColumn<History, String> column, javafx.util.Callback<History, String> valueExtractor) {
        column.setCellValueFactory(data -> new SimpleStringProperty(valueExtractor.call(data.getValue())));
        column.setReorderable(false);
        column.setSortable(false); // Disable click-to-sort
    }

    @FXML private void goToUsers(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/fxml/AdminUserView.fxml"); }
    @FXML private void goToGroups(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/fxml/AdminGroupView.fxml"); }
    @FXML private void goToReports(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/fxml/AdminReportView.fxml"); }

    public static class History {
        String time, username, fullname;
        public History(String t, String u, String f) { this.time = t; this.username = u; this.fullname = f; }
    }
}