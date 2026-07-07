/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.model;

import banking.util.CsvUtil;
import banking.model.enums.LoanStatus;
import banking.model.enums.LoanType;

/**
 * Loan merepresentasikan data pinjaman nasabah.
 *
 * Perubahan dari versi sebelumnya:
 * - Tambah field: interestRate, loanType, totalPayment (kalkulasi bunga)
 * - Tambah field: approvedBy, approvedDate, rejectionReason (approval workflow)
 *
 * Format CSV (14 kolom):
 * loanId, customerId, principal, monthlyPayment, tenorMonths, paidMonths,
 * status, startDate, description, interestRate, loanType,
 * totalPayment, approvedBy, approvedDate, rejectionReason
 */
public class Loan {

    // Core fields
    private String loanId;
    private String customerId;
    private double principal;
    private double monthlyPayment;
    private int tenorMonths;
    private int paidMonths;
    private LoanStatus status;
    private String startDate;
    private String description;

    // Interest fields — ditambahkan untuk simulasi bunga
    private double interestRate;   // dalam persen per tahun, contoh: 12.0 = 12%
    private LoanType loanType;     // FLAT atau ANNUITY
    private double totalPayment;   // total yang harus dibayar (pokok + bunga)

    // Approval workflow fields
    private String approvedBy;       // username admin yang approve/reject
    private String approvedDate;     // tanggal keputusan
    private String rejectionReason;  // alasan penolakan jika REJECTED

    public Loan(String loanId, String customerId, double principal,
                double monthlyPayment, int tenorMonths, int paidMonths,
                LoanStatus status, String startDate, String description,
                double interestRate, LoanType loanType, double totalPayment,
                String approvedBy, String approvedDate, String rejectionReason) {
        this.loanId          = loanId;
        this.customerId      = customerId;
        this.principal       = principal;
        this.monthlyPayment  = monthlyPayment;
        this.tenorMonths     = tenorMonths;
        this.paidMonths      = paidMonths;
        this.status          = status;
        this.startDate       = startDate;
        this.description     = description;
        this.interestRate    = interestRate;
        this.loanType        = loanType;
        this.totalPayment    = totalPayment;
        this.approvedBy      = approvedBy;
        this.approvedDate    = approvedDate;
        this.rejectionReason = rejectionReason;
    }

    // ── Getters ──
    public String getLoanId()          { return loanId; }
    public String getCustomerId()      { return customerId; }
    public double getPrincipal()       { return principal; }
    public double getMonthlyPayment()  { return monthlyPayment; }
    public int getTenorMonths()        { return tenorMonths; }
    public int getPaidMonths()         { return paidMonths; }
    public LoanStatus getStatus()      { return status; }
    public String getStartDate()       { return startDate; }
    public String getDescription()     { return description; }
    public double getInterestRate()    { return interestRate; }
    public LoanType getLoanType()      { return loanType; }
    public double getTotalPayment()    { return totalPayment; }
    public String getApprovedBy()      { return approvedBy; }
    public String getApprovedDate()    { return approvedDate; }
    public String getRejectionReason() { return rejectionReason; }

    // ── Setters ──
    public void setPaidMonths(int paidMonths)         { this.paidMonths = paidMonths; }
    public void setStatus(LoanStatus status)          { this.status = status; }
    public void setApprovedBy(String approvedBy)      { this.approvedBy = approvedBy; }
    public void setApprovedDate(String approvedDate)  { this.approvedDate = approvedDate; }
    public void setRejectionReason(String reason)     { this.rejectionReason = reason; }
    public void setMonthlyPayment(double mp)          { this.monthlyPayment = mp; }
    public void setTotalPayment(double tp)            { this.totalPayment = tp; }

    // ── Computed ──
    public int getRemainingMonths() {
        return tenorMonths - paidMonths;
    }

    /**
     * toString() menghasilkan format CSV 15 kolom.
     * Field yang bisa kosong (approvedBy, approvedDate, rejectionReason)
     * disimpan sebagai string kosong — tidak pernah null di CSV.
     */
    @Override
    public String toString() {
        return loanId + ","
             + customerId + ","
             + principal + ","
             + monthlyPayment + ","
             + tenorMonths + ","
             + paidMonths + ","
             + status.name() + ","
             + startDate + ","
             + CsvUtil.escapeCsvField(description) + ","
             + interestRate + ","
             + loanType.name() + ","
             + totalPayment + ","
             + (approvedBy      != null ? approvedBy      : "") + ","
             + (approvedDate    != null ? approvedDate    : "") + ","
             + CsvUtil.escapeCsvField(rejectionReason != null ? rejectionReason : "");
    }
}