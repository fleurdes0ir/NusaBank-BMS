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

        // 1. Cek deposito jatuh tempo (Tetap seperti biasa)
        List<Account> accounts = bank.getAccountsByCustomerId(customerId);
        for (Account acc : accounts) {
            if (acc instanceof DepositAccount) {
                Alert alert = checkDepositMaturity((DepositAccount) acc);
                if (alert != null) alerts.add(alert);
            }
        }

        // 2. BYPASS AMAN: Ambil seluruh pinjaman tanpa filter, lalu saring manual di sini
        // Gunakan method bawaan bankService kamu untuk ambil semua data pinjaman (misal: getAllLoans)
        List<Loan> allLoans = bank.getAllLoans(); 
        if (allLoans != null) {
            for (Loan loan : allLoans) {
                // Pastikan pinjaman ini benar-benar milik nasabah yang sedang login
                if (loan.getCustomerId() != null && loan.getCustomerId().equals(customerId)) {
                    Alert alert = checkLoanStatus(loan);
                    if (alert != null) alerts.add(alert);
                }
            }
        }

        return alerts;
    }

    // =========================================================
    // VISUAL ALERT TRIGGER (JavaFX Integration)
    // =========================================================

    /**
     * Memeriksa kondisi kritis nasabah dan menampilkan dialog ringkasan
     * notifikasi jika ada alert yang aktif.
     * * @param ownerStage stage utama aplikasi sebagai owner modal pop-up
     * @param customerId ID nasabah yang login
     */
    public static void showAlertsIfAny(javafx.stage.Stage ownerStage, String customerId) {
        List<Alert> activeAlerts = checkAlerts(customerId);
        
        if (activeAlerts.isEmpty()) {
            return; // Damai, tidak ada alert kritis. Langsung skip.
        }

        // Membuat Dialog Pop-up Modal JavaFX
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.initOwner(ownerStage);
        dialog.setTitle("Pusat Notifikasi NusaBank");
        dialog.setHeaderText("Halo! Kami menemukan beberapa catatan penting terkait akun Anda:");

        // Layout kontainer utama untuk menampung baris-baris alert
        javafx.scene.layout.VBox container = new javafx.scene.layout.VBox(10);
        container.setPadding(new javafx.geometry.Insets(15));
        container.setPrefWidth(450);

        // Iterasi dan styling setiap alert yang aktif
        for (Alert alert : activeAlerts) {
            javafx.scene.layout.HBox alertCard = new javafx.scene.layout.HBox(12);
            alertCard.setAlignment(javafx.geometry.Pos.TOP_LEFT);
            alertCard.setPadding(new javafx.geometry.Insets(10));
            
            // Memberi style border tipis sesuai dengan tingkat prioritas alert
            alertCard.setStyle(
                "-fx-background-color: " + (ownerStage.getScene().getStylesheets().toString().contains("dark") ? "#1e293b" : "#f8fafc") + "; " +
                "-fx-border-color: " + alert.getBorderColor() + "; " +
                "-fx-border-width: 0 0 0 4; " + // Border tebal hanya di sebelah kiri (Accent Line)
                "-fx-background-radius: 4; " +
                "-fx-border-radius: 4 0 0 4;"
            );

            // Icon Emoji
            javafx.scene.control.Label iconLabel = new javafx.scene.control.Label(alert.getIcon());
            iconLabel.setStyle("-fx-font-size: 16px;");

            // Teks Pesan (Judul + Deskripsi Detail)
            javafx.scene.layout.VBox textContent = new javafx.scene.layout.VBox(2);
            javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(alert.title);
            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            
            javafx.scene.control.Label msgLabel = new javafx.scene.control.Label(alert.message);
            msgLabel.setWrapText(true);
            msgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            textContent.getChildren().addAll(titleLabel, msgLabel);
            javafx.scene.layout.HBox.setHgrow(textContent, javafx.scene.layout.Priority.ALWAYS);

            alertCard.getChildren().addAll(iconLabel, textContent);
            container.getChildren().add(alertCard);
        }

        // Membungkus container dalam ScrollPane jika alert sangat banyak
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(Math.min( activeAlerts.size() * 100 + 40, 400)); // Dynamic height max 400px
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        
        // Inherit style CSS dari scene utama agar Dark/Light theme tetap sinkron
        if (ownerStage.getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(ownerStage.getScene().getStylesheets());
        }

        // Tampilkan jendela alert secara memblokir (modal) sampai nasabah menekan tombol Close
        dialog.showAndWait();
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
        // [DEBUG LOG] Cetak ke terminal Fedora untuk melihat data asli yang sedang diperiksa
        System.out.println("[DEBUG ALERT] Memeriksa Loan ID: " + loan.getLoanId() + " | Status: " + loan.getStatus() + " | Alasan: " + loan.getRejectionReason());

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
                + "sebesar Rp " + String.format("%,.0f", loan.getPrincipal())
                + " telah disetujui oleh admin."
            );
        }

        // Pinjaman ditolak 
        // DIUBAH: Dibuat lebih longgar agar jika rejectionReason null tetap memunculkan notifikasi
        if (loan.getStatus() == LoanStatus.REJECTED) {
            String alasan = (loan.getRejectionReason() != null && !loan.getRejectionReason().isEmpty()) 
                            ? loan.getRejectionReason() 
                            : "Alasan tidak ditentukan oleh admin.";
                            
            return new Alert(
                Alert.Type.LOAN_REJECTED,
                Alert.Priority.HIGH,
                "Pinjaman Ditolak",
                "Pengajuan pinjaman " + loan.getLoanId()
                + " (" + loan.getDescription() + ") "
                + "ditolak. Alasan: " + alasan
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
                    + "tersisa " + remaining + " bulan lagi."
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