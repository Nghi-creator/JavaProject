package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import javafx.beans.property.SimpleStringProperty;
import java.util.Comparator;

public class AdminGroupViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private TableView<Group> groupTable;
    @FXML private TableColumn<Group, String> colGroupName, colCreated, colAction;
    @FXML private ComboBox<String> sortCombo;

    private ObservableList<Group> masterData;

    @FXML
    public void initialize() {
        headerController.focusButton("groups");

        sortCombo.setItems(FXCollections.observableArrayList("Name (A-Z)", "Created Date (Newest)"));

        setupColumn(colGroupName, d -> d.name);
        setupColumn(colCreated, d -> d.created);
        
        colAction.setReorderable(false);
        colAction.setSortable(false);
        
        colAction.setCellFactory(new Callback<>() {
            @Override public TableCell<Group, String> call(TableColumn<Group, String> param) {
                return new TableCell<>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty) {
                            Button btn = new Button("Detail");
                            btn.getStyleClass().add("admin-action-button");
                            setGraphic(btn);
                        } else { setGraphic(null); }
                    }
                };
            }
        });

        masterData = FXCollections.observableArrayList(
            new Group("Dev Team", "2023-05-01"),
            new Group("Marketing", "2023-06-12")
        );
        groupTable.setItems(masterData);

        // Sorting Logic
        sortCombo.setOnAction(event -> {
            String selected = sortCombo.getValue();
            if (selected == null) return;

            if (selected.equals("Name (A-Z)")) {
                FXCollections.sort(masterData, Comparator.comparing(g -> g.name.toLowerCase()));
            } else if (selected.equals("Created Date (Newest)")) {
                 FXCollections.sort(masterData, (g1, g2) -> g2.created.compareTo(g1.created));
            }
        });
    }
    
    private void setupColumn(TableColumn<Group, String> column, Callback<Group, String> valueExtractor) {
        column.setCellValueFactory(data -> new SimpleStringProperty(valueExtractor.call(data.getValue())));
        column.setReorderable(false); 
        column.setSortable(false); // Disable click-to-sort
    }

    @FXML private void goToUsers(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/fxml/AdminUserView.fxml"); }
    @FXML private void goToHistory(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/fxml/AdminLoginHistoryView.fxml"); }
    @FXML private void goToReports(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/fxml/AdminReportView.fxml"); }

    public static class Group {
        String name, created;
        public Group(String n, String c) { this.name = n; this.created = c; }
    }
}