package com.example.chatroom.core.services;

import com.example.chatroom.core.dto.LoginRequest;
import com.example.chatroom.core.dto.SignupRequest;
import com.example.chatroom.core.shared.controllers.ConfigController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class UserService {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()) .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);;

    public static boolean checkEmailAvailability(String email) {
        try {
            String serverIp = ConfigController.getServerIp();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://" + serverIp + ":8080/api/users/check-email?email=" + email))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false; // Fail closed
        }
    }

    public static boolean signup(SignupRequest requestObj) {
        try {
            String json = mapper.writeValueAsString(requestObj);
            String serverIp = ConfigController.getServerIp();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://" + serverIp + ":8080/api/users/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean login(String username, String password) {
        try {
            LoginRequest payload = new LoginRequest(username, password);

            String serverIp = ConfigController.getServerIp();

            String json = mapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://" + serverIp + ":8080/api/users/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
