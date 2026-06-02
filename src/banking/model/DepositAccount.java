/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.model;

import banking.model.enums.AccountType;

public class DepositAccount extends Account {
    private static final double EARLY_WITHDRAWAL_PENALTY = 0.15;
    private static final double INTEREST_RATE = 0.06;

    private int tenorMonths;
    private String maturityDate;
    private boolean isMature;

    public DepositAccount(String accountId, String customerId, double balance,
                          String openDate, int tenorMonths,
                          String maturityDate, boolean isMature) {
        super(accountId, customerId, balance, AccountType.DEPOSIT, openDate);
        this.tenorMonths = tenorMonths;
        this.maturityDate = maturityDate;
        this.isMature = isMature;
    }

    @Override
    public void withdraw(double amount) throws IllegalArgumentException {
        if (amount <= 0)
            throw new IllegalArgumentException("Jumlah penarikan harus positif.");
        if (!isMature) {
            double penalty = amount * EARLY_WITHDRAWAL_PENALTY;
            double total = amount + penalty;
            if (total > balance)
                throw new IllegalArgumentException(
                    "Saldo tidak mencukupi termasuk penalti penarikan dini.");
            balance -= total;
        } else {
            if (amount > balance)
                throw new IllegalArgumentException("Saldo tidak mencukupi.");
            balance -= amount;
        }
    }

    @Override
    public double calculateInterest() {
        return balance * INTEREST_RATE * tenorMonths / 12.0;
    }

    public int getTenorMonths() { return tenorMonths; }
    public String getMaturityDate() { return maturityDate; }
    public boolean isMature() { return isMature; }
    public void setMature(boolean mature) { isMature = mature; }

    @Override
    public String toString() {
        return accountId + "," + customerId + "," + balance + ","
             + accountType.name() + "," + openDate + ","
             + tenorMonths + "," + maturityDate + "," + isMature;
    }
}