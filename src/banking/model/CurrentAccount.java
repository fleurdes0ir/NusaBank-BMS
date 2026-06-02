/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.model;

import banking.model.enums.AccountType;

public class CurrentAccount extends Account {
    private static final double OVERDRAFT_LIMIT = 5_000_000;
    private static final double INTEREST_RATE = 0.02;

    public CurrentAccount(String accountId, String customerId,
                          double balance, String openDate) {
        super(accountId, customerId, balance, AccountType.CURRENT, openDate);
    }

    @Override
    public void withdraw(double amount) throws IllegalArgumentException {
        if (amount <= 0)
            throw new IllegalArgumentException("Jumlah penarikan harus positif.");
        if (balance - amount < -OVERDRAFT_LIMIT)
            throw new IllegalArgumentException(
                "Melebihi batas overdraft Rp " + OVERDRAFT_LIMIT);
        balance -= amount;
    }

    @Override
    public double calculateInterest() {
        return balance > 0 ? balance * INTEREST_RATE : 0;
    }

    public static double getOverdraftLimit() { return OVERDRAFT_LIMIT; }
}