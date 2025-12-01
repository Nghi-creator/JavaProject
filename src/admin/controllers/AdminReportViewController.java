package admin.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import javafx.beans.property.SimpleStringProperty;

public class AdminReportViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private TableView<Report> reportTable;
    @FXML private TableColumn<Report, String> colTime, colReporter, colReported, colReason, colAction;
    @FXML private ComboBox<String> timeFilter, userFilter;

    @FXML
    public void initialize() {
        headerController.focusButton("spamReports");

        timeFilter.setItems(FXCollections.observableArrayList("Newest", "Oldest"));
        userFilter.setItems(FXCollections.observableArrayList("User A", "User B"));

        setupColumn(colTime, d -> d.time);
        setupColumn(colReporter, d -> d.reporter);
        setupColumn(colReported, d -> d.reported);
        setupColumn(colReason, d -> d.reason);

        colAction.setSortable(false);
        colAction.setReorderable(false);
        colAction.setCellFactory(new Callback<>() {
            @Override public TableCell<Report, String> call(TableColumn<Report, String> param) {
                return new TableCell<>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty) {
                            Button btn = new Button("Lock Account");
                            btn.getStyleClass().add("admin-danger-button");
                            setGraphic(btn);
                        } else { setGraphic(null); }
                    }
                };
            }
        });

        reportTable.setItems(FXCollections.observableArrayList(
            new Report("10:00 AM", "user1", "spammer99", "Spamming links")
        ));
    }

    private void setupColumn(TableColumn<Report, String> column, Callback<Report, String> valueExtractor) {
        column.setCellValueFactory(data -> new SimpleStringProperty(valueExtractor.call(data.getValue())));
        column.setReorderable(false);
        column.setSortable(false);
    }

    @FXML private void goToUsers(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/admin/ui/fxml/AdminUserView.fxml"); }
    @FXML private void goToHistory(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/admin/ui/fxml/AdminLoginHistoryView.fxml"); }
    @FXML private void goToGroups(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/admin/ui/fxml/AdminGroupView.fxml"); }

    public static class Report {
        String time, reporter, reported, reason;
        public Report(String t, String r1, String r2, String re) {
            this.time = t; this.reporter = r1; this.reported = r2; this.reason = re;
        }
    }
}