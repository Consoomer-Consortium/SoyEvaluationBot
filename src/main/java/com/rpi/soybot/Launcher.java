package com.rpi.soybot;

import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Launcher {

    public static void main(String[] args) {
        try {
            JSONObject config = new JSONObject(Files.readString(Paths.get("./config.json")));

            String token = config.getString("token");

            Bot bot = new Bot(token);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
