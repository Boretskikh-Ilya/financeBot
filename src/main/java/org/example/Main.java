package org.example;

import org.example.bot.TelegramBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        Properties props = new Properties();
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("bot.properties")) {
            if (input == null) {
                System.err.println("❌ Файл bot.properties не найден!");
                return;
            }
            props.load(input);
        } catch (IOException ex) {
            System.err.println("❌ Ошибка чтения bot.properties: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        String botToken = props.getProperty("BOT_TOKEN");
        String botUsername = props.getProperty("BOT_USERNAME");

        if (botToken == null || botToken.isEmpty()) {
            System.err.println("❌ BOT_TOKEN не задан!");
            return;
        }
        if (botUsername == null || botUsername.isEmpty()) {
            System.err.println("❌ BOT_USERNAME не задан!");
            return;
        }

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TelegramBot(botToken, botUsername));
            System.out.println("✅ Финансовый бот запущен!");
            System.out.println("Бот: " + botUsername);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.err.println("❌ Ошибка при запуске бота: " + e.getMessage());
        }
    }
}
