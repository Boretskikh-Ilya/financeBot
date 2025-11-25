package org.example.bot;

import java.util.List;

public interface ExpenseDao {
    void addExpense(Long chatId, double amount, String category);
    List<Expense> getExpenses(Long chatId);
    double getBalance(Long chatId);
}
