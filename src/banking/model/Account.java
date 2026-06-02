/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.model;

import banking.model.enums.AccountType;

public abstract class Account implements Transactable {
    protected String accountId;
    protected String customerId;
    protected double balance;
    protected AccountType accountType;
    protected String openDate;

    public Account(String accountId, String customerId, double balance,
                   AccountType accountType, String openDate) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.balance = balance;
        this.accountType = accountType;
        this.openDate = openDate;
    }

    public String getAccountId() { return accountId; }
    public String getCustomerId() { return customerId; }
    public double getBalance() { return balance; }
    public AccountType getAccountType() { return accountType; }
    public String getOpenDate() { return openDate; }

    public void setBalance(double balance) { this.balance = balance; }

    public abstract double calculateInterest();

    @Override
    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Jumlah deposit harus positif.");
        this.balance += amount;
    }

    @Override
    public void transfer(double amount, Account target) throws IllegalArgumentException {
        this.withdraw(amount);
        target.deposit(amount);
    }

    @Override
    public String toString() {
        return accountId + "," + customerId + "," + balance + ","
             + accountType.name() + "," + openDate;
    }
}