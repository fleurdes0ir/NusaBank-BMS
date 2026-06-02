/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.model;

import banking.model.enums.TransactionType;

public class Transaction {
    private final String transactionId;
    private final String accountId;
    private final TransactionType type;
    private final double amount;
    private final String date;
    private final String description;
    private final String targetAccountId;

    public Transaction(String transactionId, String accountId,
                       TransactionType type, double amount,
                       String date, String description, String targetAccountId) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.targetAccountId = targetAccountId;
    }

    public String getTransactionId() { return transactionId; }
    public String getAccountId() { return accountId; }
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public String getDate() { return date; }
    public String getDescription() { return description; }
    public String getTargetAccountId() { return targetAccountId; }

    @Override
    public String toString() {
        return transactionId + "," + accountId + "," + type.name() + ","
             + amount + "," + date + "," + description + ","
             + (targetAccountId != null ? targetAccountId : "");
    }
}