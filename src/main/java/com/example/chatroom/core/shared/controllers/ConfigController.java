package com.example.chatroom.core.shared.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigController {
    private static String serverIp;

    public static void loadServerIp() throws IOException {
        Path path = Path.of("server.config");
        serverIp = Files.readString(path).trim();
    }

    public static String getServerIp() {
        return serverIp;
    }
}
