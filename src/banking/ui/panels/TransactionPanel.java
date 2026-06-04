/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.ui.panels;

import banking.model.Account;
import banking.model.Customer;
import banking.model.Transaction;
import banking.model.enums.TransactionType;
import banking.service.BankService;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * TransactionPanel menampilkan manajemen transaksi.
 *
 * Fitur:
 * - Tabel semua transaksi dengan filter pencarian
 * - Deposit, Penarikan, Transfer via dialog
 *
 * Layout:
 * ┌──────────────────────────────────────────────────────┐
 * │ TRANSAKSI  [Search...]  [Deposit] [Tarik] [Transfer] │
 * ├──────────────────────────────────────────────────────┤
 * │ ID │ Rekening │ Tipe │ Jumlah │ Tanggal │ Keterangan │
 * └──────────────────────────────────────────────────────┘
 */
public class TransactionPanel {

    private final BankService bankService = BankService.getInstance();
    private final ObservableList<Transaction> transactionData =
            FXCollections.observableArrayList();
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    private TableView<Transaction> table;
    private TextField searchField;

    public VBox getRoot() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(8));

        root.getChildren().addAll(
            buildHeader(),
            buildTable()
        );

        refreshData();
        return root;
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Manajemen Transaksi");
        title.getStyleClass().add("label-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Cari ID atau rekening...");
        searchField.setPrefWidth(200);
        searchField.setPrefHeight(36);
        searchField.textProperty().addListener(
            (obs, oldVal, newVal) -> filterData(newVal));

        // Tiga tombol aksi transaksi
        Button depositBtn  = new Button("+ Deposit");
        Button withdrawBtn = new Button("- Tarik");
        Button transferBtn = new Button("⇄ Transfer");

        depositBtn.getStyleClass().add("btn-primary");
        withdrawBtn.getStyleClass().add("btn-secondary");
        transferBtn.getStyleClass().add("btn-secondary");

        depositBtn.setPrefHeight(36);
        withdrawBtn.setPrefHeight(36);
        transferBtn.setPrefHeight(36);

        depositBtn.setOnAction(e  -> showDepositDialog());
        withdrawBtn.setOnAction(e -> showWithdrawDialog());
        transferBtn.setOnAction(e -> showTransferDialog());

        header.getChildren().addAll(
            title, searchField,
            depositBtn, withdrawBtn, transferBtn);
        return header;
    }

    private TableView<Transaction> buildTable() {
        table = new TableView<>();
        table.setItems(transactionData);
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(
            TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPrefHeight(500);

        // Kolom ID Transaksi
        TableColumn<Transaction, String> idCol =
                new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getTransactionId()));
        idCol.setPrefWidth(80);

        // Kolom Rekening
        TableColumn<Transaction, String> accountCol =
                new TableColumn<>("Rekening");
        accountCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getAccountId()));
        accountCol.setPrefWidth(90);

        // Kolom Tipe — dengan warna berbeda per tipe
        TableColumn<Transaction, String> typeCol =
                new TableColumn<>("Tipe");
        typeCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getType().getDisplayName()));
        typeCol.setPrefWidth(110);

        // Cell factory untuk warnai tipe transaksi
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                // Warna teks berdasarkan tipe
                if (item.contains("Deposit") ||
                        item.contains("Masuk")) {
                    setStyle("-fx-text-fill: #34d399;");
                } else {
                    setStyle("-fx-text-fill: #f87171;");
                }
            }
        });

        // Kolom Jumlah — format currency
        TableColumn<Transaction, String> amountCol =
                new TableColumn<>("Jumlah");
        amountCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                CURRENCY.format(data.getValue().getAmount())));
        amountCol.setPrefWidth(140);

        // Kolom Tanggal
        TableColumn<Transaction, String> dateCol =
                new TableColumn<>("Tanggal");
        dateCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getDate()));
        dateCol.setPrefWidth(100);

        // Kolom Keterangan
        TableColumn<Transaction, String> descCol =
                new TableColumn<>("Keterangan");
        descCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getDescription()));

        // Kolom Rekening Tujuan (untuk transfer)
        TableColumn<Transaction, String> targetCol =
                new TableColumn<>("Tujuan");
        targetCol.setCellValueFactory(data -> {
            String target = data.getValue().getTargetAccountId();
            return new SimpleStringProperty(
                (target != null && !target.isEmpty()) ? target : "-");
        });
        targetCol.setPrefWidth(80);

        table.getColumns().addAll(
            idCol, accountCol, typeCol,
            amountCol, dateCol, descCol, targetCol);
        table.setPlaceholder(
            new Label("Belum ada data transaksi."));

        return table;
    }

    private void refreshData() {
        transactionData.setAll(bankService.getAllTransactions());
    }

    private void filterData(String query) {
        if (query == null || query.trim().isEmpty()) {
            transactionData.setAll(bankService.getAllTransactions());
            return;
        }
        String lower = query.toLowerCase().trim();
        List<Transaction> filtered = bankService.getAllTransactions()
                .stream()
                .filter(t ->
                    t.getTransactionId().toLowerCase().contains(lower) ||
                    t.getAccountId().toLowerCase().contains(lower) ||
                    t.getType().getDisplayName()
                        .toLowerCase().contains(lower))
                .collect(Collectors.toList());
        transactionData.setAll(filtered);
    }

    /**
     * Helper — ComboBox berisi semua rekening.
     * Format: "A001 - Budi Santoso (Tabungan)"
     */
    private ComboBox<String> buildAccountCombo() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setPrefHeight(36);
        combo.setMaxWidth(Double.MAX_VALUE);
        bankService.getAllAccounts().forEach(a -> {
            Customer c = bankService.getCustomerById(
                    a.getCustomerId());
            String name = c != null ? c.getFullName() : a.getCustomerId();
            combo.getItems().add(
                a.getAccountId() + " - " + name
                + " (" + a.getAccountType().getDisplayName() + ")");
        });
        if (!combo.getItems().isEmpty())
            combo.getSelectionModel().selectFirst();
        return combo;
    }

    /**
     * Ekstrak accountId dari pilihan combo.
     * Format input: "A001 - Nama (Tipe)"
     */
    private String extractAccountId(String selected) {
        if (selected == null) return null;
        return selected.split(" - ")[0].trim();
    }

    /**
     * Dialog Deposit.
     */
    private void showDepositDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Deposit");
        dialog.setHeaderText("Masukkan dana ke rekening");

        ComboBox<String> accountCombo = buildAccountCombo();
        TextField amountField = new TextField();
        amountField.setPromptText("Jumlah deposit (Rp)");
        amountField.setPrefHeight(36);
        TextField descField = new TextField("Deposit tunai");
        descField.setPrefHeight(36);

        VBox form = new VBox(8);
        form.setPadding(new Insets(16));
        form.setPrefWidth(380);

        Label accLabel  = new Label("Rekening Tujuan");
        Label amtLabel  = new Label("Jumlah (Rp)");
        Label descLabel = new Label("Keterangan");
        accLabel.getStyleClass().add("label-subtitle");
        amtLabel.getStyleClass().add("label-subtitle");
        descLabel.getStyleClass().add("label-subtitle");

        form.getChildren().addAll(
            accLabel, accountCombo,
            amtLabel, amountField,
            descLabel, descField
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(
            ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().addAll(
            table.getScene().getStylesheets());

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    String accountId = extractAccountId(
                        accountCombo.getSelectionModel()
                            .getSelectedItem());
                    double amount = Double.parseDouble(
                        amountField.getText().trim());

                    bankService.deposit(accountId, amount);
                    refreshData();
                    showAlert(Alert.AlertType.INFORMATION,
                        "Berhasil",
                        "Deposit berhasil: " + CURRENCY.format(amount));
                } catch (NumberFormatException ex) {
                    showAlert(Alert.AlertType.ERROR,
                        "Error", "Nominal tidak valid.");
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR,
                        "Error", ex.getMessage());
                }
            }
        });
    }

    /**
     * Dialog Penarikan.
     */
    private void showWithdrawDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Penarikan");
        dialog.setHeaderText("Tarik dana dari rekening");

        ComboBox<String> accountCombo = buildAccountCombo();
        TextField amountField = new TextField();
        amountField.setPromptText("Jumlah penarikan (Rp)");
        amountField.setPrefHeight(36);

        VBox form = new VBox(8);
        form.setPadding(new Insets(16));
        form.setPrefWidth(380);

        Label accLabel = new Label("Rekening Sumber");
        Label amtLabel = new Label("Jumlah (Rp)");
        accLabel.getStyleClass().add("label-subtitle");
        amtLabel.getStyleClass().add("label-subtitle");

        form.getChildren().addAll(
            accLabel, accountCombo,
            amtLabel, amountField
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(
            ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().addAll(
            table.getScene().getStylesheets());

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    String accountId = extractAccountId(
                        accountCombo.getSelectionModel()
                            .getSelectedItem());
                    double amount = Double.parseDouble(
                        amountField.getText().trim());

                    bankService.withdraw(accountId, amount);
                    refreshData();
                    showAlert(Alert.AlertType.INFORMATION,
                        "Berhasil",
                        "Penarikan berhasil: " + CURRENCY.format(amount));
                } catch (NumberFormatException ex) {
                    showAlert(Alert.AlertType.ERROR,
                        "Error", "Nominal tidak valid.");
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR,
                        "Error", ex.getMessage());
                }
            }
        });
    }

    /**
     * Dialog Transfer antar rekening.
     */
    private void showTransferDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Transfer");
        dialog.setHeaderText("Transfer antar rekening");

        ComboBox<String> sourceCombo = buildAccountCombo();
        ComboBox<String> targetCombo = buildAccountCombo();
        TextField amountField = new TextField();
        amountField.setPromptText("Jumlah transfer (Rp)");
        amountField.setPrefHeight(36);

        VBox form = new VBox(8);
        form.setPadding(new Insets(16));
        form.setPrefWidth(380);

        Label srcLabel = new Label("Rekening Sumber");
        Label tgtLabel = new Label("Rekening Tujuan");
        Label amtLabel = new Label("Jumlah (Rp)");
        srcLabel.getStyleClass().add("label-subtitle");
        tgtLabel.getStyleClass().add("label-subtitle");
        amtLabel.getStyleClass().add("label-subtitle");

        form.getChildren().addAll(
            srcLabel, sourceCombo,
            tgtLabel, targetCombo,
            amtLabel, amountField
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(
            ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().addAll(
            table.getScene().getStylesheets());

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    String sourceId = extractAccountId(
                        sourceCombo.getSelectionModel()
                            .getSelectedItem());
                    String targetId = extractAccountId(
                        targetCombo.getSelectionModel()
                            .getSelectedItem());

                    if (sourceId.equals(targetId)) {
                        showAlert(Alert.AlertType.ERROR, "Error",
                            "Rekening sumber dan tujuan tidak boleh sama.");
                        return;
                    }

                    double amount = Double.parseDouble(
                        amountField.getText().trim());

                    bankService.transfer(sourceId, targetId, amount);
                    refreshData();
                    showAlert(Alert.AlertType.INFORMATION,
                        "Berhasil",
                        "Transfer berhasil: " + CURRENCY.format(amount));
                } catch (NumberFormatException ex) {
                    showAlert(Alert.AlertType.ERROR,
                        "Error", "Nominal tidak valid.");
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR,
                        "Error", ex.getMessage());
                }
            }
        });
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