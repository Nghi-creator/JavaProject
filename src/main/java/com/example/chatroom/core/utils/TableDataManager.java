package com.example.chatroom.core.utils;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class TableDataManager<T> {

    private final FilteredList<T> filteredData;
    private final SortedList<T> sortedData;
    private final Map<String, Comparator<T>> sortOptions = new HashMap<>();

    public TableDataManager(TableView<T> table, ObservableList<T> masterData) {
        // 1. Wrap Master Data in FilteredList
        this.filteredData = new FilteredList<>(masterData, p -> true);

        // 2. Wrap in SortedList
        this.sortedData = new SortedList<>(this.filteredData);

        // 3. CRITICAL FIX: Do NOT bind comparator to table if using external ComboBox sorting
        // This prevents "java.lang.RuntimeException: SortedList.comparator : A bound value cannot be set"
        // this.sortedData.comparatorProperty().bind(table.comparatorProperty()); <--- DELETED

        // 4. Set Items
        table.setItems(this.sortedData);
    }

    // --- FILTERING (Search + Date + Status) ---
    public void setFilterPredicate(Predicate<T> predicate) {
        filteredData.setPredicate(predicate);
    }

    // --- SORTING ---
    public void addSortOption(String label, Comparator<T> comparator) {
        sortOptions.put(label, comparator);
    }

    public void setupSortController(ComboBox<String> sortCombo) {
        sortCombo.getItems().setAll(sortOptions.keySet());
        sortCombo.setOnAction(e -> {
            String selected = sortCombo.getValue();
            if (selected != null && sortOptions.containsKey(selected)) {
                sortedData.setComparator(sortOptions.get(selected));
            }
        });
    }
}