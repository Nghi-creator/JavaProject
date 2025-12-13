package com.example.chatroom.core.shared.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import java.util.function.Consumer;

public class SearchBarController {
    @FXML private TextField searchField;
    private Consumer<String> onSearchListener;

    @FXML
    public void initialize() {
        searchField.setOnKeyReleased(event -> {
            if (onSearchListener != null) onSearchListener.accept(searchField.getText());
        });
    }

    public void setOnSearchListener(Consumer<String> listener) {
        this.onSearchListener = listener;
    }

    // --- FIXED: Added Getter ---
    public TextField getSearchField() { return searchField; }
}