package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;

public class AdminUserRegistrationChartViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private ComboBox<Integer> yearCombo;
    @FXML private BarChart<String, Number> registrationChart;

    @FXML
    public void initialize() {
        headerController.focusButton("userRegistrationChart");


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


        registrationChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("New Users " + year);


        for (int month = 1; month <= 12; month++) {
            int dummyCount = (int) (Math.random() * 100);
            series.getData().add(new XYChart.Data<>(String.valueOf(month), dummyCount));
        }

        registrationChart.getData().add(series);
    }
}
