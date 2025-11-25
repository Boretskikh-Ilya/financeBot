package org.example.bot;

import java.util.*;

public class InMemoryExpenseDao implements ExpenseDao {

    // Хранилище расходов для каждого пользователя
    private final Map<Long, List<Expense>> userExpenses = new HashMap<>();
    // Хранилище баланса для каждого пользователя
    private final Map<Long, Double> userBalances = new HashMap<>();

    @Override
    public void addExpense(Long chatId, double amount, String category) {
        // Инициализация если пользователь новый
        userExpenses.putIfAbsent(chatId, new ArrayList<>());
        userBalances.putIfAbsent(chatId, 0.0);

        Expense expense = new Expense(amount, category);
        userExpenses.get(chatId).add(expense);

        // Обновление баланса
        userBalances.put(chatId, userBalances.get(chatId) - amount);
    }

    @Override
    public List<Expense> getExpenses(Long chatId) {
        // Возвращаем копию списка, чтобы не сломать внутреннее хранилище
        return new ArrayList<>(userExpenses.getOrDefault(chatId, Collections.emptyList()));
    }

    @Override
    public double getBalance(Long chatId) {
        return userBalances.getOrDefault(chatId, 0.0);
    }
}
