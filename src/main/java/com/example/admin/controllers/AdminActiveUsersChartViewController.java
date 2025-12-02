package com.example.admin.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;

public class AdminActiveUsersChartViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private ComboBox<Integer> yearCombo;
    @FXML private BarChart<String, Number> activeChart;

    @FXML
    public void initialize() {
        headerController.focusButton("aciveUsersChart");
        int currentYear = java.time.Year.now().getValue();
        yearCombo.setItems(FXCollections.observableArrayList(
                currentYear, currentYear - 1, currentYear - 2, currentYear - 3, currentYear - 4
        ));

        yearCombo.setOnAction(e -> updateChart());
        yearCombo.setValue(currentYear);
        updateChart();
    }

    private void updateChart() {
        Integer year = yearCombo.getValue();
        if (year == null) return;

        activeChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Active Users " + year);


        for (int month = 1; month <= 12; month++) {
            int dummyCount = (int) (Math.random() * 200);
            series.getData().add(new XYChart.Data<>(String.valueOf(month), dummyCount));
        }

        activeChart.getData().add(series);
    }
}
