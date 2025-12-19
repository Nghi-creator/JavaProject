package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import org.json.JSONArray;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AdminActiveUsersChartViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private ComboBox<Integer> yearCombo;
    @FXML private BarChart<String, Number> activeChart;

    private final String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    @FXML
    public void initialize() {
        headerController.focusButton("activeUsersChart");

        int currentYear = java.time.Year.now().getValue();
        ObservableList<Integer> years = FXCollections.observableArrayList(
                currentYear, currentYear - 1, currentYear - 2, currentYear - 3, currentYear - 4
        );
        yearCombo.setItems(years);
        yearCombo.setValue(currentYear);

        yearCombo.setOnAction(e -> updateChart());

        // Set X-axis categories
        CategoryAxis xAxis = (CategoryAxis) activeChart.getXAxis();
        xAxis.setCategories(FXCollections.observableArrayList(months));

        updateChart();
    }

    private void updateChart() {
        Integer year = yearCombo.getValue();
        if (year == null) return;

        activeChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Active Users " + year);

        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/active-users?year=" + year))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            Platform.runLater(() -> {
                                try {
                                    JSONArray dataArray = new JSONArray(response.body());

                                    int maxCount = 0;
                                    for (int i = 0; i < 12; i++) {
                                        int count = dataArray.optInt(i, 0);
                                        maxCount = Math.max(maxCount, count);
                                        series.getData().add(new XYChart.Data<>(months[i], count));
                                    }

                                    activeChart.getData().add(series);

                                    NumberAxis yAxis = (NumberAxis) activeChart.getYAxis();
                                    yAxis.setAutoRanging(false);
                                    yAxis.setLowerBound(0);
                                    yAxis.setUpperBound(maxCount + 5);
                                    yAxis.setTickUnit(5);
                                    yAxis.setMinorTickCount(0);
                                    yAxis.setForceZeroInRange(true);
                                    yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
                                        @Override
                                        public String toString(Number object) {
                                            return String.valueOf(object.intValue());
                                        }
                                    });

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            });
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
