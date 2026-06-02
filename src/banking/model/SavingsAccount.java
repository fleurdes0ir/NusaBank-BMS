/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.model;

import banking.model.enums.AccountType;

public class SavingsAccount extends Account {
    private static final double MINIMUM_BALANCE = 50_000;
    private static final double INTEREST_RATE = 0.035;

    public SavingsAccount(String accountId, String customerId,
                          double balance, String openDate) {
        super(accountId, customerId, balance, AccountType.SAVINGS, openDate);
    }

    @Override
    public void withdraw(double amount) throws IllegalArgumentException {
        if (amount <= 0)
            throw new IllegalArgumentException("Jumlah penarikan harus positif.");
        if (balance - amount < MINIMUM_BALANCE)
            throw new IllegalArgumentException(
                "Saldo tidak mencukupi. Minimum saldo Rp " + MINIMUM_BALANCE);
        balance -= amount;
    }

    @Override
    public double calculateInterest() {
        return balance * INTEREST_RATE;
    }

    public static double getMinimumBalance() { return MINIMUM_BALANCE; }
}