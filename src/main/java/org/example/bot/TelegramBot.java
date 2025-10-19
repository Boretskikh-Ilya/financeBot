package org.example.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class TelegramBot extends TelegramLongPollingBot {

    private Map<Long, String> userStates = new HashMap<>();
    private Map<Long, Double> userBalances = new HashMap<>();
    private Map<Long, List<Expense>> userExpenses = new HashMap<>();
    private Map<Long, Double> temporaryAmounts = new HashMap<>(); // Новый Map для временных сумм

    private static class Expense {
        double amount;
        String category;
        Date date;

        Expense(double amount, String category) {
            this.amount = amount;
            this.category = category;
            this.date = new Date();
        }
    }

    @Override
    public String getBotToken() {
        return "7596704485:AAENl2PrL6D7Qxp4ilcQh9KLAR0VrDSXnsg";
    }

    @Override
    public String getBotUsername() {
        return "finance_matmech_bot";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String text = message.getText();

        // Инициализация данных пользователя
        userBalances.putIfAbsent(chatId, 0.0);
        userExpenses.putIfAbsent(chatId, new ArrayList<>());

        String response = processCommand(chatId, text);
        sendMessage(chatId, response);
    }

    private String processCommand(Long chatId, String text) {
        String state = userStates.get(chatId);

        if (state != null) {
            return processState(chatId, text, state);
        }

        switch (text.toLowerCase()) {
            case "/start":
                return "💰 Финансовый бот\n\n" +
                        "Команды:\n" +
                        "/add - Добавить расход\n" +
                        "/balance - Баланс\n" +
                        "/expenses - Последние расходы\n" +
                        "/help - Помощь";

            case "/add":
                userStates.put(chatId, "WAITING_AMOUNT");
                return "💸 Введите сумму расхода:";

            case "/balance":
                return "💰 Баланс: " + userBalances.get(chatId) + " руб.";

            case "/expenses":
                return getLastExpenses(chatId);

            case "/help":
                return "📋 Команды:\n" +
                        "/add - Добавить расход\n" +
                        "/balance - Баланс\n" +
                        "/expenses - Последние расходы\n" +
                        "/help - Помощь";

            default:
                return "Используйте /help для списка команд";
        }
    }

    private String processState(Long chatId, String text, String state) {
        switch (state) {
            case "WAITING_AMOUNT":
                try {
                    double amount = Double.parseDouble(text);
                    if (amount <= 0) {
                        return "❌ Сумма должна быть больше 0!";
                    }
                    temporaryAmounts.put(chatId, amount); // Сохраняем сумму во временный Map
                    userStates.put(chatId, "WAITING_CATEGORY");
                    return "📁 Выберите категорию:\n" +
                            "1 - Еда\n" +
                            "2 - Транспорт\n" +
                            "3 - Развлечения\n" +
                            "4 - Коммунальные\n" +
                            "5 - Другое";
                } catch (NumberFormatException e) {
                    userStates.remove(chatId);
                    return "❌ Ошибка! Введите корректную сумму (например: 1500 или 1500.50):";
                }

            case "WAITING_CATEGORY":
                String category = getCategoryByNumber(text);
                Double amount = temporaryAmounts.get(chatId); // Получаем сумму из временного Map

                if (amount == null) {
                    userStates.remove(chatId);
                    return "❌ Ошибка данных. Начните заново с /add";
                }

                // Сохраняем расход
                Expense expense = new Expense(amount, category);
                userExpenses.get(chatId).add(expense);

                // Обновляем баланс
                double currentBalance = userBalances.get(chatId);
                userBalances.put(chatId, currentBalance - amount);

                // Чистим состояния и временные данные
                userStates.remove(chatId);
                temporaryAmounts.remove(chatId);

                return "✅ Добавлен расход:\n" +
                        "💸 Сумма: " + amount + " руб.\n" +
                        "📁 Категория: " + category + "\n" +
                        "💰 Новый баланс: " + userBalances.get(chatId) + " руб.";

            default:
                userStates.remove(chatId);
                temporaryAmounts.remove(chatId);
                return "Ошибка состояния. Используйте /help для списка команд";
        }
    }

    private String getCategoryByNumber(String number) {
        switch (number) {
            case "1": return "Еда";
            case "2": return "Транспорт";
            case "3": return "Развлечения";
            case "4": return "Коммунальные";
            default: return "Другое";
        }
    }

    private String getLastExpenses(Long chatId) {
        List<Expense> expenses = userExpenses.get(chatId);
        if (expenses.isEmpty()) {
            return "📊 Расходы отсутствуют";
        }

        StringBuilder sb = new StringBuilder("📊 Последние расходы:\n");
        int count = Math.min(expenses.size(), 5);

        // Берем последние 5 расходов
        for (int i = expenses.size() - 1; i >= Math.max(0, expenses.size() - count); i--) {
            Expense exp = expenses.get(i);
            sb.append("• ").append(exp.amount).append(" руб. - ").append(exp.category).append("\n");
        }

        sb.append("\n💰 Общий баланс: ").append(userBalances.get(chatId)).append(" руб.");
        return sb.toString();
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}