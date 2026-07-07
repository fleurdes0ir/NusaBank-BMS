/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.ui.panels;

import banking.model.Account;
import banking.model.Customer;
import banking.model.DepositAccount;
import banking.model.Loan;
import banking.model.Transaction;
import banking.model.enums.LoanStatus;
import banking.model.enums.TransactionType;
import banking.service.AuthService;
import banking.service.BankService;
import banking.ui.SceneManager;
import banking.ui.ThemeManager;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
/**
 * NasabahPanel adalah panel utama untuk role CUSTOMER.
 *
 * Sesuai design reference "Mobile Banking Feel di Desktop":
 * ┌─────────────────────────────────────────┐
 * │ [BS] Budi Santoso    Nasabah-C001 Keluar│ ← Header
 * ├─────────────────────────────────────────┤
 * │ Total Saldo                             │
 * │ Rp 65.000.000                           │ ← Hero saldo
 * │ 2 rekening aktif                        │
 * ├────────┬────────┬──────────┬────────────┤
 * │Deposit │ Tarik  │ Transfer │  Pinjaman  │ ← Quick actions
 * ├─────────────────────────────────────────┤
 * │ REKENING SAYA                           │ ← Daftar rekening
 * ├─────────────────────────────────────────┤
 * │ TRANSAKSI TERBARU                       │ ← 5 transaksi terakhir
 * ├─────────────────────────────────────────┤
 * │ PINJAMAN AKTIF                          │ ← Pinjaman dengan progress
 * └─────────────────────────────────────────┘
 */
public class NasabahPanel {

    private final BankService bankService = BankService.getInstance();
    private final AuthService authService = AuthService.getInstance();
    private final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    // Data nasabah yang sedang login
    private final Customer customer;
    private final List<Account> accounts;

    public NasabahPanel() {
        this.customer = authService.getCurrentCustomer();
        this.accounts = customer != null
            ? bankService.getAccountsByCustomerId(
                customer.getCustomerId())
            : List.of();
    }

    /**
     * Build root scene — dipanggil dari MainScreen untuk Nasabah.
     * Mengembalikan Scene lengkap dengan tema terdaftar.
     *
     * @return Scene NasabahPanel
     */
    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setPrefSize(1100, 700);
        root.setCenter(buildContent());

        Scene scene = new Scene(root, 1100, 700);
        ThemeManager.getInstance().registerScene(scene);
        return scene;
    }

    /**
     * Mengembalikan root Node — dipakai saat NasabahPanel
     * di-embed di dalam MainScreen (fallback).
     *
     * @return ScrollPane berisi seluruh konten
     */
    public ScrollPane getRoot() {
        return buildContent();
    }

    /**
     * Membangun seluruh konten panel.
     */
    private ScrollPane buildContent() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");

        // Container utama — max width 700px agar terasa mobile-like
        VBox content = new VBox(20);
        content.setMaxWidth(700);
        content.setPadding(new Insets(24, 24, 40, 24));

        if (customer == null) {
            Label error = new Label("Data nasabah tidak ditemukan.");
            error.getStyleClass().add("label-subtitle");
            content.getChildren().add(error);
            scroll.setContent(content);
            return scroll;
        }

        content.getChildren().addAll(
            buildHeader(),
            buildHeroSaldo(),
            buildQuickActions(),
            buildRekeningSection(),
            buildTransaksiSection(),
            buildPinjamanSection()
        );

        // Center content di dalam scroll
        StackPane wrapper = new StackPane(content);
        StackPane.setAlignment(content, Pos.TOP_CENTER);
        scroll.setContent(wrapper);
        return scroll;
    }

    /**
     * Header — avatar inisial, nama, ID, theme toggle, logout.
     */
    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 8, 0));

        // Avatar lingkaran dengan inisial
        Label avatar = new Label(customer.getInitials());
        avatar.getStyleClass().add("avatar");
        avatar.setMinSize(44, 44);
        avatar.setMaxSize(44, 44);
        avatar.setAlignment(Pos.CENTER);

        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(customer.getFullName());
        nameLabel.setStyle(
            "-fx-font-size: 15px; -fx-font-weight: bold;");
        Label idLabel = new Label(
            "Nasabah · " + customer.getCustomerId());
        idLabel.getStyleClass().add("label-subtitle");
        nameBox.getChildren().addAll(nameLabel, idLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Theme toggle
        Button themeBtn = new Button(
            ThemeManager.getInstance().isDarkMode() ? "🌙" : "☀️");
        themeBtn.getStyleClass().add("btn-secondary");
        themeBtn.setStyle("-fx-font-size: 14px; -fx-padding: 6 12 6 12;");
        themeBtn.setOnAction(e -> {
            ThemeManager.getInstance().toggleTheme();
            themeBtn.setText(ThemeManager.getInstance().isDarkMode()
                ? "🌙" : "☀️");
        });

        // Logout button
        Button logoutBtn = new Button("⬡ Keluar");
        logoutBtn.getStyleClass().add("btn-secondary");
        logoutBtn.setOnAction(e -> {
            authService.logout();
            SceneManager.getInstance().showLogin();
        });

        header.getChildren().addAll(
            avatar, nameBox, spacer, themeBtn, logoutBtn);
        return header;
    }

    /**
     * Hero section — total saldo besar di tengah.
     * Ini adalah elemen paling menonjol di panel nasabah.
     */
    private VBox buildHeroSaldo() {
        VBox hero = new VBox(6);
        hero.getStyleClass().add("card");
        hero.setAlignment(Pos.CENTER_LEFT);

        Label totalLabel = new Label("Total Saldo");
        totalLabel.getStyleClass().add("label-subtitle");

        // Hitung total saldo semua rekening
        double totalSaldo = bankService
                .getTotalBalanceByCustomer(customer.getCustomerId());

        Label saldoLabel = new Label(CURRENCY.format(totalSaldo));
        saldoLabel.getStyleClass().add("label-amount-large");
        saldoLabel.setStyle(
            "-fx-font-size: 32px; -fx-font-weight: bold;");

        Label subLabel = new Label(accounts.size()
            + " rekening aktif · diperbarui baru saja");
        subLabel.getStyleClass().add("label-subtitle");

        hero.getChildren().addAll(totalLabel, saldoLabel, subLabel);
        return hero;
    }

    /**
     * Quick action buttons — Deposit, Tarik, Transfer, Pinjaman.
     * Layout 4 kolom sejajar.
     */
    private HBox buildQuickActions() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);

        Button depositBtn  = createActionBtn("⊕", "Deposit");
        Button tarikBtn    = createActionBtn("⊖", "Tarik");
        Button transferBtn = createActionBtn("⇄", "Transfer");
        Button pinjamanBtn = createActionBtn("▤", "Pinjaman");

        // Action handlers — tampilkan dialog transaksi
        depositBtn.setOnAction(e  -> showDepositDialog());
        tarikBtn.setOnAction(e    -> showWithdrawDialog());
        transferBtn.setOnAction(e -> showTransferDialog());
        pinjamanBtn.setOnAction(e -> showLoanDialog());

        // Semua tombol lebar sama
        for (Button btn : new Button[]{
                depositBtn, tarikBtn, transferBtn, pinjamanBtn}) {
            HBox.setHgrow(btn, Priority.ALWAYS);
            btn.setMaxWidth(Double.MAX_VALUE);
        }

        row.getChildren().addAll(
            depositBtn, tarikBtn, transferBtn, pinjamanBtn);
        return row;
    }

    /**
     * Helper — buat action button dengan icon di atas dan label di bawah.
     */
    private Button createActionBtn(String icon, String label) {
        Button btn = new Button(icon + "\n" + label);
        btn.getStyleClass().add("btn-action");
        btn.setPrefHeight(70);
        btn.setStyle(
            "-fx-font-size: 13px; -fx-alignment: center; " +
            "-fx-text-alignment: center;");
        return btn;
    }

    /**
     * Section daftar rekening milik nasabah.
     */
    private VBox buildRekeningSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card");

        Label title = new Label("REKENING SAYA");
        title.getStyleClass().add("label-section");
        section.getChildren().add(title);

        if (accounts.isEmpty()) {
            Label empty = new Label("Belum ada rekening.");
            empty.getStyleClass().add("label-subtitle");
            section.getChildren().add(empty);
            return section;
        }

        for (Account acc : accounts) {
            section.getChildren().add(createRekeningItem(acc));
            if (accounts.indexOf(acc) < accounts.size() - 1) {
                section.getChildren().add(new Separator());
            }
        }
        return section;
    }

    /**
     * Helper — satu baris item rekening.
     *
     * Layout:
     * [icon] Tabungan          Rp 15.000.000
     *        A001
     */
    private HBox createRekeningItem(Account acc) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(6, 0, 6, 0));

        // Icon per tipe rekening
        String icon;
        switch (acc.getAccountType()) {
            case SAVINGS: icon = "💳"; break;
            case CURRENT: icon = "🏦"; break;
            case DEPOSIT: icon = "🔒"; break;
            default:      icon = "▣";
        }

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");

        VBox info = new VBox(2);
        Label typeLabel = new Label(
            acc.getAccountType().getDisplayName());
        typeLabel.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold;");

        // Info tambahan untuk deposito
        String subText = acc.getAccountId();
        if (acc instanceof DepositAccount) {
            DepositAccount dep = (DepositAccount) acc;
            subText += " · " + dep.getTenorMonths()
                + " bln · Jatuh: " + dep.getMaturityDate();
        }
        Label subLabel = new Label(subText);
        subLabel.getStyleClass().add("label-subtitle");
        info.getChildren().addAll(typeLabel, subLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label saldoLabel = new Label(CURRENCY.format(acc.getBalance()));
        saldoLabel.setStyle(
            "-fx-font-size: 14px; -fx-font-weight: bold;");

        item.getChildren().addAll(iconLabel, info, saldoLabel);
        return item;
    }

    /**
     * Section transaksi terbaru — 5 transaksi terakhir
     * dari semua rekening nasabah ini.
     */
    private VBox buildTransaksiSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card");

        Label title = new Label("TRANSAKSI TERBARU");
        title.getStyleClass().add("label-section");
        section.getChildren().add(title);

        // Kumpulkan transaksi dari semua rekening
        // lalu ambil 5 terbaru
        List<Transaction> allTx = accounts.stream()
            .flatMap(a -> bankService
                .getTransactionsByAccount(a.getAccountId()).stream())
            .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate()))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());

        if (allTx.isEmpty()) {
            Label empty = new Label("Belum ada transaksi.");
            empty.getStyleClass().add("label-subtitle");
            section.getChildren().add(empty);
            return section;
        }

        for (Transaction tx : allTx) {
            section.getChildren().add(createTransaksiItem(tx));
        }
        return section;
    }

    /**
     * Helper — satu baris item transaksi.
     */
    private HBox createTransaksiItem(Transaction tx) {
        HBox item = new HBox(12);
        item.getStyleClass().add("transaction-item");
        item.setAlignment(Pos.CENTER_LEFT);

        // Icon dan warna berdasarkan tipe
        boolean isPositive = tx.getType() == TransactionType.DEPOSIT
            || tx.getType() == TransactionType.TRANSFER_IN;

        Label iconLabel = new Label(isPositive ? "↙" : "↗");
        iconLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: "
            + (isPositive ? "#34d399" : "#f87171") + ";");

        VBox info = new VBox(2);
        Label descLabel = new Label(tx.getDescription());
        descLabel.setStyle("-fx-font-size: 13px;");

        Label subLabel = new Label(
            tx.getAccountId() + " · " + tx.getDate());
        subLabel.getStyleClass().add("label-subtitle");
        info.getChildren().addAll(descLabel, subLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        String sign = isPositive ? "+" : "-";
        Label amountLabel = new Label(
            sign + CURRENCY.format(tx.getAmount()));
        amountLabel.setStyle(
            "-fx-font-weight: bold; -fx-text-fill: "
            + (isPositive ? "#34d399" : "#f87171") + ";");

        item.getChildren().addAll(iconLabel, info, amountLabel);
        return item;
    }

    /**
     * Section pinjaman aktif nasabah.
     */
    private VBox buildPinjamanSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card");

        Label title = new Label("PINJAMAN AKTIF");
        title.getStyleClass().add("label-section");
        section.getChildren().add(title);

        List<Loan> loans = bankService
                .getLoansByCustomerId(customer.getCustomerId())
                .stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                .collect(java.util.stream.Collectors.toList());

        if (loans.isEmpty()) {
            Label empty = new Label("Tidak ada pinjaman aktif.");
            empty.getStyleClass().add("label-subtitle");
            section.getChildren().add(empty);
            return section;
        }

        for (Loan loan : loans) {
            section.getChildren().add(createPinjamanItem(loan));
        }
        return section;
    }

    /**
     * Helper — satu card item pinjaman dengan progress bar.
     */
    private VBox createPinjamanItem(Loan loan) {
        VBox item = new VBox(8);
        item.getStyleClass().add("card-stat");

        // Baris atas: ID + keterangan + status
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label(
            loan.getLoanId() + " — " + loan.getDescription());
        idLabel.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold;");
        HBox.setHgrow(idLabel, Priority.ALWAYS);

        Label statusLabel = new Label(
            loan.getStatus().getDisplayName());
        statusLabel.getStyleClass().add("badge-active");

        topRow.getChildren().addAll(idLabel, statusLabel);

        // Baris detail: Pokok, Cicilan, Sisa
        HBox detailRow = new HBox(24);
        detailRow.setAlignment(Pos.CENTER_LEFT);

        detailRow.getChildren().addAll(
            createDetailItem("Pokok",
                CURRENCY.format(loan.getPrincipal())),
            createDetailItem("Cicilan/bln",
                CURRENCY.format(loan.getMonthlyPayment())),
            createDetailItem("Sisa",
                loan.getRemainingMonths() + " bulan")
        );

        // Progress bar
        ProgressBar progressBar = new ProgressBar();
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
            topRow, detailRow, progressBar, progressLabel);
        return item;
    }

    /**
     * Helper — label detail kecil (judul + nilai).
     */
    private VBox createDetailItem(String label, String value) {
        VBox box = new VBox(2);
        Label lblLabel = new Label(label);
        lblLabel.setStyle(
            "-fx-font-size: 11px; -fx-text-fill: #555d74;");
        Label valLabel = new Label(value);
        valLabel.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold;");
        box.getChildren().addAll(lblLabel, valLabel);
        return box;
    }

    // ── Dialog Transaksi untuk Nasabah ──
    // Sama seperti TransactionPanel tapi hanya rekening milik
    // nasabah yang sedang login yang ditampilkan di ComboBox

    /**
     * Helper — ComboBox hanya rekening milik nasabah ini.
     */
    private ComboBox<String> buildMyAccountCombo() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setPrefHeight(36);
        combo.setMaxWidth(Double.MAX_VALUE);
        accounts.forEach(a ->
            combo.getItems().add(
                a.getAccountId() + " - "
                + a.getAccountType().getDisplayName()
                + " (" + CURRENCY.format(a.getBalance()) + ")"));
        if (!combo.getItems().isEmpty())
            combo.getSelectionModel().selectFirst();
        return combo;
    }

    private String extractAccountId(String selected) {
        if (selected == null) return null;
        return selected.split(" - ")[0].trim();
    }

    private void showDepositDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Deposit");
        dialog.setHeaderText("Masukkan dana ke rekening Anda");

        ComboBox<String> combo = buildMyAccountCombo();
        TextField amountField = new TextField();
        amountField.setPromptText("Jumlah deposit (Rp)");
        amountField.setPrefHeight(36);

        VBox form = buildSimpleForm(
            "Rekening Tujuan", combo,
            "Jumlah (Rp)", amountField);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(
            ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    String accountId = extractAccountId(
                        combo.getSelectionModel().getSelectedItem());
                    double amount = Double.parseDouble(
                        amountField.getText().trim());
                    bankService.deposit(accountId, amount);
                    showAlert(Alert.AlertType.INFORMATION,
                        "Berhasil",
                        "Deposit berhasil: Rp "
                        + String.format("%,.0f", amount));
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR,
                        "Error", ex.getMessage());
                }
            }
        });
    }

    private void showWithdrawDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Penarikan");
        dialog.setHeaderText("Tarik dana dari rekening Anda");

        ComboBox<String> combo = buildMyAccountCombo();
        TextField amountField = new TextField();
        amountField.setPromptText("Jumlah penarikan (Rp)");
        amountField.setPrefHeight(36);

        VBox form = buildSimpleForm(
            "Rekening Sumber", combo,
            "Jumlah (Rp)", amountField);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(
            ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    String accountId = extractAccountId(
                        combo.getSelectionModel().getSelectedItem());
                    double amount = Double.parseDouble(
                        amountField.getText().trim());
                    bankService.withdraw(accountId, amount);
                    showAlert(Alert.AlertType.INFORMATION,
                        "Berhasil",
                        "Penarikan berhasil: Rp "
                        + String.format("%,.0f", amount));
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR,
                        "Error", ex.getMessage());
                }
            }
        });
}
    
    private void showTransferDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Transfer");
        dialog.setHeaderText("Transfer dari rekening Anda");

        // DEKLARASI VARIABEL DI DALAM METODE (Scope lokal yang benar)
        ComboBox<String> sourceCombo = buildMyAccountCombo();
        ComboBox<String> targetCombo = new ComboBox<>();
        targetCombo.setPrefHeight(36);
        targetCombo.setMaxWidth(Double.MAX_VALUE);
        
        bankService.getAllAccounts().forEach(a -> {
            Customer c = bankService.getCustomerById(a.getCustomerId());
            String name = c != null ? c.getFullName() : a.getCustomerId();
            targetCombo.getItems().add(a.getAccountId() + " - " + name
                + " (" + a.getAccountType().getDisplayName() + ")");
        });
        if (!targetCombo.getItems().isEmpty())
            targetCombo.getSelectionModel().selectFirst();

        TextField amountField = new TextField();
        amountField.setPromptText("Jumlah transfer (Rp)");
        amountField.setPrefHeight(36);

        VBox form = buildSimpleForm(
            "Rekening Sumber", sourceCombo,
            "Rekening Tujuan", targetCombo,
            "Jumlah (Rp)", amountField);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    // PANGGIL VARIABEL YANG SUDAH DIDEKLARASIKAN DI ATAS
                    String sourceId = extractAccountId(sourceCombo.getSelectionModel().getSelectedItem());
                    String targetId = extractAccountId(targetCombo.getSelectionModel().getSelectedItem());
                    double amount = Double.parseDouble(amountField.getText().trim());

                    if (sourceId == null || targetId == null || sourceId.equals(targetId)) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Rekening sumber/tujuan tidak valid atau sama.");
                        return;
                    }

                    bankService.transfer(sourceId, targetId, amount);
                    showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Transfer berhasil: " + CURRENCY.format(amount));
                    
                    // Refresh tampilan agar saldo terupdate
                    banking.ui.SceneManager.getInstance().showMain();
                    
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
                }
            }
        });
    }



    private void showLoanDialog() {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Ajukan Pinjaman");
            dialog.setHeaderText("Pengajuan pinjaman atas nama: "
                    + customer.getFullName());

            // ── Form fields ──
            TextField principalField = new TextField();
            principalField.setPromptText("Contoh: 10000000");
            principalField.setPrefHeight(36);

            TextField tenorField = new TextField();
            tenorField.setPromptText("Contoh: 12");
            tenorField.setPrefHeight(36);

            TextField rateField = new TextField("12");
            rateField.setPrefHeight(36);

            ComboBox<String> loanTypeCombo = new ComboBox<>();
            loanTypeCombo.getItems().addAll("Flat Rate", "Anuitas");
            loanTypeCombo.getSelectionModel().selectFirst();
            loanTypeCombo.setPrefHeight(36);
            loanTypeCombo.setMaxWidth(Double.MAX_VALUE);

            TextField descField = new TextField("Pinjaman Personal");
            descField.setPrefHeight(36);

            // ── Preview label ──
            Label previewLabel = new Label("Cicilan/bulan: -");
            previewLabel.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold; " +
                "-fx-text-fill: #4f8ef7;");

            // ── Preview updater ──
            javafx.beans.value.ChangeListener<String> updater =
                    (obs, o, n) -> {
                try {
                    double p = Double.parseDouble(
                        principalField.getText().trim());
                    int t = Integer.parseInt(
                        tenorField.getText().trim());
                    double r = Double.parseDouble(
                        rateField.getText().trim());
                    banking.model.enums.LoanType lt =
                        loanTypeCombo.getValue().equals("Anuitas")
                        ? banking.model.enums.LoanType.ANNUITY
                        : banking.model.enums.LoanType.FLAT;

                    double monthly  = bankService.calculateMonthlyPayment(
                            lt, p, r, t);
                    double total    = banking.util.LoanCalculator
                            .calculateTotalPayment(lt, p, r, t);
                    double interest = total - p;

                    previewLabel.setText(
                        "Cicilan/bulan : Rp " + String.format("%,.0f", monthly)
                        + "\nTotal bayar  : Rp " + String.format("%,.0f", total)
                        + "\nTotal bunga  : Rp " + String.format("%,.0f", interest)
                    );
                } catch (NumberFormatException ex) {
                    previewLabel.setText("Cicilan/bulan: -");
                }
            };

            principalField.textProperty().addListener(updater);
            tenorField.textProperty().addListener(updater);
            rateField.textProperty().addListener(updater);
            loanTypeCombo.valueProperty().addListener(
                    (obs, o, n) -> updater.changed(null, null, null));

            // ── Build form ──
            VBox form = buildSimpleForm(
                "Jumlah Pinjaman (Rp)", principalField,
                "Tenor (bulan)",        tenorField,
                "Suku Bunga (% / thn)", rateField,
                "Metode Bunga",         loanTypeCombo,
                "Keterangan",           descField);
            form.getChildren().addAll(new Separator(), previewLabel);

            dialog.getDialogPane().setContent(form);
            dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

            // ── Submit handler ──
            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    try {
                        double principal = Double.parseDouble(
                            principalField.getText().trim());
                        int tenor = Integer.parseInt(
                            tenorField.getText().trim());
                        double rateValue = Double.parseDouble(
                            rateField.getText().trim());
                        banking.model.enums.LoanType selectedLoanType =
                            loanTypeCombo.getValue().equals("Anuitas")
                            ? banking.model.enums.LoanType.ANNUITY
                            : banking.model.enums.LoanType.FLAT;

                        bankService.applyLoan(
                            customer.getCustomerId(),
                            principal, tenor,
                            descField.getText().trim(),
                            rateValue, selectedLoanType);

                        showAlert(Alert.AlertType.INFORMATION,
                            "Berhasil",
                            "Pinjaman berhasil diajukan dan "
                            + "menunggu persetujuan admin.");
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR,
                            "Error", ex.getMessage());
                    }
                }
            });
        }
    /**
     * Helper — buat form VBox dari pasangan label-control.
     */
    private VBox buildSimpleForm(Object... pairs) {
        VBox form = new VBox(8);
        form.setPadding(new Insets(16));
        form.setPrefWidth(380);
        for (int i = 0; i < pairs.length; i += 2) {
            Label lbl = new Label((String) pairs[i]);
            lbl.getStyleClass().add("label-subtitle");
            javafx.scene.control.Control ctrl =
                (javafx.scene.control.Control) pairs[i + 1];
            ctrl.setMaxWidth(Double.MAX_VALUE);
            form.getChildren().addAll(lbl, ctrl);
        }
        return form;
    }

    private void showAlert(Alert.AlertType type,
            String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}