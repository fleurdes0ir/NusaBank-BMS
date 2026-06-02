/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.model;

import banking.model.enums.LoanStatus;

public class Loan {
    private String loanId;
    private String customerId;
    private double principal;
    private double monthlyPayment;
    private int tenorMonths;
    private int paidMonths;
    private LoanStatus status;
    private String startDate;
    private String description;

    public Loan(String loanId, String customerId, double principal,
                double monthlyPayment, int tenorMonths, int paidMonths,
                LoanStatus status, String startDate, String description) {
        this.loanId = loanId;
        this.customerId = customerId;
        this.principal = principal;
        this.monthlyPayment = monthlyPayment;
        this.tenorMonths = tenorMonths;
        this.paidMonths = paidMonths;
        this.status = status;
        this.startDate = startDate;
        this.description = description;
    }

    public String getLoanId() { return loanId; }
    public String getCustomerId() { return customerId; }
    public double getPrincipal() { return principal; }
    public double getMonthlyPayment() { return monthlyPayment; }
    public int getTenorMonths() { return tenorMonths; }
    public int getPaidMonths() { return paidMonths; }
    public LoanStatus getStatus() { return status; }
    public String getStartDate() { return startDate; }
    public String getDescription() { return description; }

    public void setPaidMonths(int paidMonths) { this.paidMonths = paidMonths; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public int getRemainingMonths() { return tenorMonths - paidMonths; }

    public double getTotalAmount() { return monthlyPayment * tenorMonths; }

    @Override
    public String toString() {
        return loanId + "," + customerId + "," + principal + ","
             + monthlyPayment + "," + tenorMonths + "," + paidMonths + ","
             + status.name() + "," + startDate + "," + description;
    }
}