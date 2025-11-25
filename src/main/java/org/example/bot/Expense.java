package org.example.bot;

import java.util.Date;

public class Expense {
    public double amount;
    public String category;
    public Date date;

    public Expense(double amount, String category) {
        this.amount = amount;
        this.category = category;
        this.date = new Date();
    }
}
