/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.service;

import banking.model.Account;
import banking.model.DepositAccount;
import banking.model.Loan;
import banking.model.enums.LoanStatus;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * AlertService — engine deteksi kondisi kritis yang membutuhkan
 * notifikasi ke nasabah.
 *
 * Dipanggil saat login nasabah berhasil. Cek:
 * 1. Deposito yang akan jatuh tempo dalam 30 hari
 * 2. Pinjaman yang mendekati akhir tenor (sisa <= 3 bulan)
 * 3. Pinjaman PENDING yang sudah disetujui atau ditolak
 *
 * Design decision: alert ditampilkan sekali saat login — tidak ada
 * background thread. Ini intentional untuk menghindari kompleksitas
 * threading JavaFX dan cukup untuk scope project ini.
 */
public class AlertService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Threshold hari sebelum jatuh tempo untuk trigger alert
    private static final int DEPOSIT_ALERT_DAYS    = 30;
    private static final int LOAN_ALERT_REMAINING  = 3; // bulan

    private AlertService() {}

    // =========================================================
    // PUBLIC API
    // =========================================================

    /**
     * Generate semua alert untuk nasabah yang sedang login.
     *
     * @param customerId ID nasabah
     * @return List<Alert> — kosong jika tidak ada kondisi kritis
     */
    public static List<Alert> checkAlerts(String customerId) {
        List<Alert> alerts = new ArrayList<>();
        BankService bank = BankService.getInstance();

        // Cek deposito jatuh tempo
        List<Account> accounts = bank
                .getAccountsByCustomerId(customerId);
        for (Account acc : accounts) {
            if (acc instanceof DepositAccount) {
                Alert alert = checkDepositMaturity(
                        (DepositAccount) acc);
                if (alert != null) alerts.add(alert);
            }
        }

        // Cek status pinjaman
        List<Loan> loans = bank.getLoansByCustomerId(customerId);
        for (Loan loan : loans) {
            Alert alert = checkLoanStatus(loan);
            if (alert != null) alerts.add(alert);
        }

        return alerts;
    }

    // =========================================================
    // PRIVATE CHECKERS
    // =========================================================

    /**
     * Cek apakah deposito akan jatuh tempo dalam DEPOSIT_ALERT_DAYS hari.
     *
     * @param deposit DepositAccount yang dicek
     * @return Alert jika jatuh tempo dekat, null jika aman
     */
    private static Alert checkDepositMaturity(DepositAccount deposit) {
        String maturityDateStr = deposit.getMaturityDate();
        if (maturityDateStr == null || maturityDateStr.isEmpty())
            return null;

        try {
            LocalDate maturityDate = LocalDate.parse(
                    maturityDateStr, DATE_FORMAT);
            LocalDate today = LocalDate.now();

            // Hitung selisih hari antara hari ini dan jatuh tempo
            long daysUntilMaturity = ChronoUnit.DAYS.between(
                    today, maturityDate);

            if (daysUntilMaturity < 0) {
                // Sudah jatuh tempo — high priority
                return new Alert(
                    Alert.Type.DEPOSIT_OVERDUE,
                    Alert.Priority.HIGH,
                    "Deposito Jatuh Tempo",
                    "Rekening deposito " + deposit.getAccountId()
                    + " sudah jatuh tempo pada "
                    + maturityDateStr + ". "
                    + "Segera hubungi bank untuk perpanjangan "
                    + "atau pencairan dana."
                );
            } else if (daysUntilMaturity <= DEPOSIT_ALERT_DAYS) {
                // Akan jatuh tempo dalam 30 hari — medium priority
                return new Alert(
                    Alert.Type.DEPOSIT_NEAR_MATURITY,
                    Alert.Priority.MEDIUM,
                    "Deposito Akan Jatuh Tempo",
                    "Rekening deposito " + deposit.getAccountId()
                    + " akan jatuh tempo dalam "
                    + daysUntilMaturity + " hari "
                    + "(" + maturityDateStr + "). "
                    + "Pertimbangkan untuk memperpanjang deposito Anda."
                );
            }
        } catch (Exception e) {
            // Skip jika format tanggal tidak valid
            System.err.println("AlertService: invalid maturity date — "
                    + maturityDateStr);
        }
        return null;
    }

    /**
     * Cek status pinjaman dan kondisi kritis.
     *
     * Kondisi yang dicek:
     * 1. Pinjaman PENDING — disetujui atau ditolak
     * 2. Pinjaman ACTIVE dengan sisa tenor <= 3 bulan
     *
     * @param loan Loan yang dicek
     * @return Alert jika ada kondisi kritis, null jika aman
     */
    private static Alert checkLoanStatus(Loan loan) {
        // Pinjaman baru disetujui
        if (loan.getStatus() == LoanStatus.ACTIVE
                && loan.getApprovedDate() != null
                && !loan.getApprovedDate().isEmpty()
                && loan.getPaidMonths() == 0) {
            return new Alert(
                Alert.Type.LOAN_APPROVED,
                Alert.Priority.HIGH,
                "Pinjaman Disetujui",
                "Pinjaman " + loan.getLoanId()
                + " (" + loan.getDescription() + ") "
                + "sebesar Rp " + String.format("%,.0f",
                    loan.getPrincipal())
                + " telah disetujui oleh admin pada "
                + loan.getApprovedDate() + ". "
                + "Cicilan pertama Rp " + String.format("%,.0f",
                    loan.getMonthlyPayment()) + "/bulan."
            );
        }

        // Pinjaman ditolak — belum pernah ditampilkan
        if (loan.getStatus() == LoanStatus.REJECTED
                && loan.getRejectionReason() != null
                && !loan.getRejectionReason().isEmpty()) {
            return new Alert(
                Alert.Type.LOAN_REJECTED,
                Alert.Priority.HIGH,
                "Pinjaman Ditolak",
                "Pengajuan pinjaman " + loan.getLoanId()
                + " (" + loan.getDescription() + ") "
                + "ditolak. Alasan: "
                + loan.getRejectionReason()
            );
        }

        // Pinjaman aktif mendekati lunas
        if (loan.getStatus() == LoanStatus.ACTIVE) {
            int remaining = loan.getRemainingMonths();
            if (remaining > 0 && remaining <= LOAN_ALERT_REMAINING) {
                return new Alert(
                    Alert.Type.LOAN_NEAR_COMPLETE,
                    Alert.Priority.MEDIUM,
                    "Pinjaman Hampir Lunas",
                    "Pinjaman " + loan.getLoanId()
                    + " (" + loan.getDescription() + ") "
                    + "tersisa " + remaining + " bulan lagi. "
                    + "Total sisa: Rp " + String.format("%,.0f",
                        loan.getMonthlyPayment() * remaining) + "."
                );
            }
        }

        return null;
    }

    // =========================================================
    // ALERT DATA CLASS
    // =========================================================

    /**
     * Immutable data class untuk satu alert.
     *
     * Menggunakan inner static class agar AlertService dan Alert
     * selalu dalam satu unit — tidak perlu file terpisah untuk
     * class yang hanya dipakai oleh AlertService.
     */
    public static class Alert {

        /**
         * Tipe alert — menentukan icon dan warna di UI.
         */
        public enum Type {
            DEPOSIT_NEAR_MATURITY,
            DEPOSIT_OVERDUE,
            LOAN_APPROVED,
            LOAN_REJECTED,
            LOAN_NEAR_COMPLETE
        }

        /**
         * Priority — menentukan urutan tampil di UI.
         * HIGH ditampilkan lebih dulu dari MEDIUM.
         */
        public enum Priority {
            HIGH,
            MEDIUM,
            LOW
        }

        public final Type type;
        public final Priority priority;
        public final String title;
        public final String message;

        public Alert(Type type, Priority priority,
                String title, String message) {
            this.type     = type;
            this.priority = priority;
            this.title    = title;
            this.message  = message;
        }

        /**
         * Emoji icon berdasarkan tipe alert — dipakai di UI.
         */
        public String getIcon() {
            switch (type) {
                case DEPOSIT_OVERDUE:      return "🔴";
                case DEPOSIT_NEAR_MATURITY: return "🟡";
                case LOAN_APPROVED:        return "✅";
                case LOAN_REJECTED:        return "❌";
                case LOAN_NEAR_COMPLETE:   return "🟡";
                default:                   return "ℹ️";
            }
        }

        /**
         * Warna border card berdasarkan priority — untuk CSS styling.
         */
        public String getBorderColor() {
            switch (priority) {
                case HIGH:   return "#f87171"; // merah
                case MEDIUM: return "#fbbf24"; // kuning
                default:     return "#4f8ef7"; // biru
            }
        }
    }
}