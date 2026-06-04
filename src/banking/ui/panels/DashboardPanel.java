/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.ui.panels;

import banking.model.*;
import banking.model.enums.AccountType;
import banking.service.AuthService;
import banking.service.BankService;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * DashboardPanel adalah panel utama Admin — ditampilkan pertama kali
 * setelah login sebagai Administrator.
 *
 * Sesuai design reference, terdiri dari:
 * ┌─────────────────────────────────────────────┐
 * │ Selamat pagi, Admin 👋                       │
 * │ Tanggal hari ini                             │
 * ├──────────┬──────────┬──────────┬────────────┤
 * │ Nasabah  │ Rekening │Transaksi │  Pinjaman  │ ← Stat Cards
 * ├──────────┴──────────┼──────────┴────────────┤
 * │ Transaksi Terbaru   │ Komposisi Rekening     │
 * ├─────────────────────┼────────────────────────┤
 * │ Nasabah Terdaftar   │ Aksi Cepat             │
 * ├─────────────────────┴────────────────────────┤
 * │ Status Pinjaman                              │
 * └─────────────────────────────────────────────┘
 */
public class DashboardPanel {

    private final BankService bankService = BankService.getInstance();
    private final AuthService authService = AuthService.getInstance();

    // Format mata uang Rupiah
    // NumberFormat.getCurrencyInstance() otomatis format angka ke mata uang
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    /**
     * Membangun dan mengembalikan root node panel.
     * Dipanggil oleh MainScreen saat menu Dashboard diklik.
     *
     * @return ScrollPane berisi seluruh konten dashboard
     */
    public ScrollPane getRoot() {
        // ScrollPane sebagai root agar konten bisa di-scroll
        // jika window terlalu kecil
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.getStyleClass().add("scroll-pane");

        VBox content = new VBox(20);
        content.setPadding(new Insets(8, 8, 24, 8));

        // Bangun tiap section
        content.getChildren().addAll(
            buildHeader(),
            buildStatCards(),
            buildMiddleSection(),
            buildBottomSection(),
            buildLoanStatus()
        );

        scroll.setContent(content);
        return scroll;
    }

    /**
     * Header section — greeting dan tanggal.
     *
     * Greeting dinamis berdasarkan jam:
     * 00-11 → Selamat pagi
     * 12-14 → Selamat siang
     * 15-17 → Selamat sore
     * 18-23 → Selamat malam
     */
    private VBox buildHeader() {
        VBox header = new VBox(4);

        String username = authService.getCurrentUser().getUsername();
        // Capitalize huruf pertama username
        String displayName = username.substring(0, 1).toUpperCase()
                + username.substring(1);

        // Greeting dinamis berdasarkan jam
        int hour = java.time.LocalTime.now().getHour();
        String greeting;
        if (hour < 12)       greeting = "Selamat pagi";
        else if (hour < 15)  greeting = "Selamat siang";
        else if (hour < 18)  greeting = "Selamat sore";
        else                 greeting = "Selamat malam";

        Label titleLabel = new Label(greeting + ", " + displayName + " 👋");
        titleLabel.getStyleClass().add("label-title");

        // Format tanggal Indonesia
        String today = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy",
                new Locale("id", "ID")));
        Label dateLabel = new Label(today
                + " · Berikut ringkasan sistem hari ini");
        dateLabel.getStyleClass().add("label-subtitle");

        header.getChildren().addAll(titleLabel, dateLabel);
        return header;
    }

    /**
     * Stat Cards — 4 kartu statistik di baris atas.
     * Sesuai design reference: Nasabah, Rekening, Transaksi, Pinjaman.
     */
    private HBox buildStatCards() {
        HBox row = new HBox(12);

        int totalCustomers  = bankService.getTotalCustomers();
        int totalAccounts   = bankService.getTotalAccounts();
        int totalTxToday    = bankService.getTotalTransactionsToday();
        int totalLoans      = bankService.getTotalActiveLoans();

        // Setiap kartu: icon, angka besar, label, subtitle
        row.getChildren().addAll(
            createStatCard("⚇", String.valueOf(totalCustomers),
                "Total Nasabah", "+1 bulan ini"),
            createStatCard("▣", String.valueOf(totalAccounts),
                "Total Rekening", "+2 bulan ini"),
            createStatCard("⇄", String.valueOf(totalTxToday),
                "Total Transaksi", "hari ini " + totalTxToday),
            createStatCard("▤", String.valueOf(totalLoans),
                "Pinjaman Aktif",
                "Rp " + formatShort(bankService.getTotalManagedFunds())
                + " total")
        );

        // Semua kartu punya lebar yang sama — HGrow ALWAYS
        for (javafx.scene.Node node : row.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        return row;
    }

    /**
     * Helper — buat satu stat card.
     *
     * Layout kartu:
     * ┌─────────────────┐
     * │ [icon]          │
     * │ 42              │ ← angka besar
     * │ Total Nasabah   │ ← label
     * │ +1 bulan ini    │ ← subtitle kecil
     * └─────────────────┘
     */
    private VBox createStatCard(String icon, String value,
            String label, String subtitle) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card-stat");
        card.setPadding(new Insets(16));

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #4f8ef7;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle(
            "-fx-font-size: 28px; -fx-font-weight: bold;");

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("label-subtitle");

        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555d74;");

        card.getChildren().addAll(
            iconLabel, valueLabel, nameLabel, subLabel);
        return card;
    }

    /**
     * Middle section — dua kolom:
     * Kiri  : Transaksi Terbaru
     * Kanan : Komposisi Rekening
     */
    private HBox buildMiddleSection() {
        HBox row = new HBox(12);

        VBox leftCard  = buildRecentTransactions();
        VBox rightCard = buildAccountComposition();

        HBox.setHgrow(leftCard, Priority.ALWAYS);
        HBox.setHgrow(rightCard, Priority.ALWAYS);

        row.getChildren().addAll(leftCard, rightCard);
        return row;
    }

    /**
     * Widget Transaksi Terbaru — 4 transaksi terakhir di sistem.
     */
    private VBox buildRecentTransactions() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label title = new Label("TRANSAKSI TERBARU");
        title.getStyleClass().add("label-section");

        card.getChildren().add(title);

        List<Transaction> recent =
                bankService.getRecentTransactions(4);

        if (recent.isEmpty()) {
            Label empty = new Label("Belum ada transaksi.");
            empty.getStyleClass().add("label-subtitle");
            card.getChildren().add(empty);
        } else {
            for (Transaction tx : recent) {
                card.getChildren().add(createTransactionItem(tx));
            }
        }

        return card;
    }

    /**
     * Helper — buat satu baris item transaksi.
     *
     * Layout:
     * [●] Deposit — A003        +Rp 8jt
     *     20 Feb 2024
     */
    private HBox createTransactionItem(Transaction tx) {
        HBox item = new HBox(10);
        item.getStyleClass().add("transaction-item");
        item.setAlignment(Pos.CENTER_LEFT);

        // Warna dot berdasarkan tipe transaksi
        String dotColor;
        String sign;
        switch (tx.getType()) {
            case DEPOSIT:
            case TRANSFER_IN:
                dotColor = "#34d399"; // hijau
                sign = "+";
                break;
            default:
                dotColor = "#f87171"; // merah
                sign = "-";
        }

        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: " + dotColor
                + "; -fx-font-size: 10px;");

        VBox info = new VBox(2);
        Label descLabel = new Label(tx.getDescription()
                + " — " + tx.getAccountId());
        descLabel.setStyle("-fx-font-size: 12px;");

        Label dateLabel = new Label(tx.getDate());
        dateLabel.setStyle(
            "-fx-font-size: 11px; -fx-text-fill: #555d74;");

        info.getChildren().addAll(descLabel, dateLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Format amount singkat: 8000000 → "Rp 8jt"
        Label amountLabel = new Label(
            sign + "Rp " + formatShort(tx.getAmount()));
        amountLabel.setStyle(
            "-fx-font-weight: bold; -fx-text-fill: "
            + (sign.equals("+") ? "#34d399" : "#f87171") + ";");

        item.getChildren().addAll(dot, info, amountLabel);
        return item;
    }

    /**
     * Widget Komposisi Rekening — breakdown per tipe + total dana.
     */
    private VBox buildAccountComposition() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label title = new Label("KOMPOSISI REKENING");
        title.getStyleClass().add("label-section");

        int savings = bankService.countAccountsByType(AccountType.SAVINGS);
        int current = bankService.countAccountsByType(AccountType.CURRENT);
        int deposit = bankService.countAccountsByType(AccountType.DEPOSIT);
        double totalFunds = bankService.getTotalManagedFunds();

        card.getChildren().addAll(
            title,
            createCompositionRow("Tabungan", savings, "#4f8ef7"),
            createCompositionRow("Giro",     current, "#f59e0b"),
            createCompositionRow("Deposito", deposit, "#34d399"),
            new Separator(),
            createTotalFundsWidget(totalFunds)
        );

        return card;
    }

    /**
     * Helper — satu baris komposisi rekening dengan progress bar mini.
     */
    private HBox createCompositionRow(String label,
            int count, String color) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        // Dot warna sebagai legend
        Label dot = new Label("─");
        dot.setStyle("-fx-text-fill: " + color
                + "; -fx-font-weight: bold;");

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 13px;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Label countLabel = new Label(count + " rekening");
        countLabel.getStyleClass().add("label-subtitle");

        row.getChildren().addAll(dot, nameLabel, countLabel);
        return row;
    }

    /**
     * Widget total dana kelolaan.
     */
    private VBox createTotalFundsWidget(double total) {
        VBox box = new VBox(4);

        Label titleLabel = new Label("Total Dana Kelolaan");
        titleLabel.getStyleClass().add("label-subtitle");

        Label amountLabel = new Label(CURRENCY.format(total));
        amountLabel.setStyle(
            "-fx-font-size: 20px; -fx-font-weight: bold;");

        box.getChildren().addAll(titleLabel, amountLabel);
        return box;
    }

    /**
     * Bottom section — dua kolom:
     * Kiri  : Nasabah Terdaftar (3 nasabah terbaru)
     * Kanan : Aksi Cepat
     */
    private HBox buildBottomSection() {
        HBox row = new HBox(12);

        VBox leftCard  = buildRecentCustomers();
        VBox rightCard = buildQuickActions();

        HBox.setHgrow(leftCard, Priority.ALWAYS);
        HBox.setHgrow(rightCard, Priority.ALWAYS);

        row.getChildren().addAll(leftCard, rightCard);
        return row;
    }

    /**
     * Widget Nasabah Terdaftar — 3 nasabah terbaru.
     */
    private VBox buildRecentCustomers() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label title = new Label("NASABAH TERDAFTAR");
        title.getStyleClass().add("label-section");
        card.getChildren().add(title);

        List<Customer> customers = bankService.getAllCustomers();

        // Ambil 3 terakhir — subList dari belakang
        int size  = customers.size();
        int start = Math.max(0, size - 3);
        List<Customer> recent = customers.subList(start, size);

        for (Customer c : recent) {
            card.getChildren().add(createCustomerItem(c));
        }

        return card;
    }

    /**
     * Helper — satu baris item nasabah dengan avatar inisial.
     */
    private HBox createCustomerItem(Customer customer) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(4, 0, 4, 0));

        // Avatar — lingkaran dengan inisial nama
        Label avatar = new Label(customer.getInitials());
        avatar.getStyleClass().add("avatar");
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);
        avatar.setAlignment(Pos.CENTER);

        VBox info = new VBox(2);
        Label nameLabel = new Label(customer.getFullName());
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        // Hitung jumlah rekening milik nasabah ini
        int accountCount = bankService
                .getAccountsByCustomerId(customer.getCustomerId()).size();
        Label subLabel = new Label(customer.getCustomerId()
                + " · " + accountCount + " rekening");
        subLabel.getStyleClass().add("label-subtitle");

        info.getChildren().addAll(nameLabel, subLabel);
        item.getChildren().addAll(avatar, info);
        return item;
    }

    /**
     * Widget Aksi Cepat — 4 tombol shortcut untuk operasi umum.
     * Sesuai design reference: Tambah Nasabah, Buka Rekening,
     * Buat Transaksi, Ajukan Pinjaman.
     *
     * Untuk sekarang tombol hanya placeholder — aksi akan
     * dihubungkan ke panel masing-masing setelah semua panel selesai.
     */
    private VBox buildQuickActions() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label title = new Label("AKSI CEPAT");
        title.getStyleClass().add("label-section");

        // Grid 2x2 untuk 4 tombol aksi
        // GridPane lebih tepat untuk layout grid daripada HBox bertingkat
        javafx.scene.layout.GridPane grid =
                new javafx.scene.layout.GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        Button addCustomerBtn = createActionButton("⚇", "Tambah\nNasabah");
        Button openAccountBtn = createActionButton("▣", "Buka\nRekening");
        Button transactionBtn = createActionButton("⇄", "Buat\nTransaksi");
        Button loanBtn        = createActionButton("▤", "Ajukan\nPinjaman");

        // Kolom 0 = kiri, Kolom 1 = kanan
        // Baris 0 = atas, Baris 1 = bawah
        grid.add(addCustomerBtn, 0, 0);
        grid.add(openAccountBtn, 1, 0);
        grid.add(transactionBtn, 0, 1);
        grid.add(loanBtn,        1, 1);

        // Semua kolom lebar sama
        javafx.scene.layout.ColumnConstraints col =
                new javafx.scene.layout.ColumnConstraints();
        col.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col,
                new javafx.scene.layout.ColumnConstraints());
        grid.getColumnConstraints().get(1).setPercentWidth(50);

        card.getChildren().addAll(title, grid);
        return card;
    }

    /**
     * Helper — buat tombol aksi cepat dengan icon dan label.
     */
    private Button createActionButton(String icon, String label) {
        Button btn = new Button(icon + "\n" + label);
        btn.getStyleClass().add("btn-action");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(64);
        btn.setStyle("-fx-font-size: 12px; -fx-alignment: center;");
        return btn;
    }

    /**
     * Widget Status Pinjaman — daftar pinjaman aktif.
     */
    private VBox buildLoanStatus() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label title = new Label("STATUS PINJAMAN");
        title.getStyleClass().add("label-section");
        card.getChildren().add(title);

        List<Loan> activeLoans = bankService.getActiveLoans();

        if (activeLoans.isEmpty()) {
            Label empty = new Label("Tidak ada pinjaman aktif.");
            empty.getStyleClass().add("label-subtitle");
            card.getChildren().add(empty);
        } else {
            for (Loan loan : activeLoans) {
                card.getChildren().add(createLoanItem(loan));
            }
        }

        return card;
    }

    /**
     * Helper — satu baris item pinjaman.
     *
     * Layout:
     * Budi Santoso                    Aktif    Rp 2,35jt/bln
     * Rp 50jt · 24 bln
     * [████████░░░░] 8 dari 24 bulan terbayar
     */
    private VBox createLoanItem(Loan loan) {
        VBox item = new VBox(6);
        item.getStyleClass().add("card-stat");

        // Ambil nama customer
        Customer customer = bankService
                .getCustomerById(loan.getCustomerId());
        String customerName = customer != null
                ? customer.getFullName() : loan.getCustomerId();

        // Baris 1: nama + status + cicilan
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(customerName);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Label statusLabel = new Label(
                loan.getStatus().getDisplayName());
        statusLabel.getStyleClass().add("badge-active");

        Label cicilan = new Label(
            "Rp " + formatShort(loan.getMonthlyPayment()) + "/bln");
        cicilan.getStyleClass().add("label-subtitle");

        topRow.getChildren().addAll(nameLabel, statusLabel, cicilan);

        // Baris 2: detail pinjaman
        Label detailLabel = new Label(
            loan.getLoanId() + " · Rp "
            + formatShort(loan.getPrincipal())
            + " · " + loan.getTenorMonths() + " bln"
            + " · " + loan.getDescription());
        detailLabel.getStyleClass().add("label-subtitle");

        // Progress bar cicilan
        javafx.scene.control.ProgressBar progressBar =
                new javafx.scene.control.ProgressBar();
        double progress = loan.getTenorMonths() > 0
            ? (double) loan.getPaidMonths() / loan.getTenorMonths()
            : 0;
        progressBar.setProgress(progress);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(6);

        Label progressLabel = new Label(
            loan.getPaidMonths() + " dari "
            + loan.getTenorMonths() + " bulan terbayar");
        progressLabel.setStyle(
            "-fx-font-size: 11px; -fx-text-fill: #555d74;");

        item.getChildren().addAll(
            topRow, detailLabel, progressBar, progressLabel);
        return item;
    }

    /**
     * Helper — format angka besar menjadi format singkat.
     * Contoh: 8000000 → "8jt", 2350000 → "2,35jt", 500000 → "500rb"
     *
     * @param amount angka yang akan diformat
     * @return String format singkat
     */
    private String formatShort(double amount) {
        if (amount >= 1_000_000_000) {
            return String.format("%.1fM", amount / 1_000_000_000);
        } else if (amount >= 1_000_000) {
            // Hilangkan desimal jika bulat
            double val = amount / 1_000_000;
            if (val == Math.floor(val)) {
                return String.format("%.0fjt", val);
            }
            return String.format("%.2fjt", val);
        } else if (amount >= 1_000) {
            double val = amount / 1_000;
            if (val == Math.floor(val)) {
                return String.format("%.0frb", val);
            }
            return String.format("%.1frb", val);
        }
        return String.format("%.0f", amount);
    }
}