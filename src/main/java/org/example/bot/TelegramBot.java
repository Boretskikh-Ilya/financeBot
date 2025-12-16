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
    private final String botToken;
    private final String botUsername;

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

    public TelegramBot(String botToken, String botUsername) {
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
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
            return handleState(chatId, text, state);
        }

        return switch (text.toLowerCase()) {
            case "/start" -> handleStart(chatId);
            case "/add" -> handleAdd(chatId);
            case "/balance" -> handleBalance(chatId);
            case "/expenses" -> handleExpenses(chatId);
            case "/help" -> handleHelp(chatId);
            default -> handleUnknown(chatId, text);
        };
    }

    private String handleStart(Long chatId) {
        return "💰 Финансовый бот\n\n" +
                "Команды:\n" +
                "/add - Добавить расход\n" +
                "/balance - Баланс\n" +
                "/expenses - Последние расходы\n" +
                "/help - Помощь";
    }

    private String handleAdd(Long chatId) {
        userStates.put(chatId, "WAITING_AMOUNT");
        return "💸 Введите сумму расхода:";
    }

    private String handleBalance(Long chatId) {
        return "💰 Баланс: " + userBalances.get(chatId) + " руб.";
    }

    private String handleExpenses(Long chatId) {
        return getLastExpenses(chatId);
    }

    private String handleHelp(Long chatId) {
        return "📋 Команды:\n" +
                "/add - Добавить расход\n" +
                "/balance - Баланс\n" +
                "/expenses - Последние расходы\n" +
                "/help - Помощь";
    }

    private String handleUnknown(Long chatId, String text) {
        return "Используйте /help для списка команд";
    }

    private String handleState(Long chatId, String text, String state) {
        return switch (state) {
            case "WAITING_AMOUNT" -> handleWaitingAmount(chatId, text);
            case "WAITING_CATEGORY" -> handleWaitingCategory(chatId, text);
            default -> {
                userStates.remove(chatId);
                temporaryAmounts.remove(chatId);
                yield "Ошибка состояния. Используйте /help для списка команд";
            }
        };
    }

    private String handleWaitingAmount(Long chatId, String text) {
        try {
            double amount = Double.parseDouble(text);
            if (amount <= 0) {
                return "❌ Сумма должна быть больше 0!";
            }
            temporaryAmounts.put(chatId, amount); // сохраняем сумму
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
    }

    private String handleWaitingCategory(Long chatId, String text) {
        String category = getCategoryByNumber(text);
        Double amount = temporaryAmounts.get(chatId);

        if (amount == null) {
            userStates.remove(chatId);
            return "❌ Ошибка данных. Начните заново с /add";
        }

        Expense expense = new Expense(amount, category);
        userExpenses.get(chatId).add(expense);

        userBalances.compute(chatId, (k, currentBalance) -> currentBalance - amount);

        userStates.remove(chatId);
        temporaryAmounts.remove(chatId);

        return "✅ Добавлен расход:\n" +
                "💸 Сумма: " + amount + " руб.\n" +
                "📁 Категория: " + category + "\n" +
                "💰 Новый баланс: " + userBalances.get(chatId) + " руб.";
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
