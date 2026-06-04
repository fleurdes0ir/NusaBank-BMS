/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.ui.panels;

import banking.model.Account;
import banking.model.Customer;
import banking.model.DepositAccount;
import banking.model.enums.AccountType;
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
 * AccountPanel menampilkan manajemen rekening.
 *
 * Fitur:
 * - Tabel semua rekening dengan filter pencarian
 * - Buka rekening baru via dialog
 * - Hapus rekening dengan konfirmasi
 *
 * Layout:
 * ┌─────────────────────────────────────────┐
 * │ REKENING  [Search...]  [+ Buka Rekening]│
 * ├─────────────────────────────────────────┤
 * │ ID │ Nasabah │ Tipe │ Saldo │ Tgl │ Aksi│
 * └─────────────────────────────────────────┘
 */
public class AccountPanel {

    private final BankService bankService = BankService.getInstance();
    private final ObservableList<Account> accountData =
            FXCollections.observableArrayList();
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    private TableView<Account> table;
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

        Label title = new Label("Manajemen Rekening");
        title.getStyleClass().add("label-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Cari ID rekening atau nasabah...");
        searchField.setPrefWidth(250);
        searchField.setPrefHeight(36);
        searchField.textProperty().addListener(
            (obs, oldVal, newVal) -> filterData(newVal));

        Button addBtn = new Button("+ Buka Rekening");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setPrefHeight(36);
        addBtn.setOnAction(e -> showOpenAccountDialog());

        header.getChildren().addAll(title, searchField, addBtn);
        return header;
    }

    private TableView<Account> buildTable() {
        table = new TableView<>();
        table.setItems(accountData);
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(
            TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPrefHeight(500);

        // Kolom ID Rekening
        TableColumn<Account, String> idCol =
                new TableColumn<>("ID Rekening");
        idCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getAccountId()));
        idCol.setPrefWidth(100);

        // Kolom Nasabah — ambil nama dari customerId
        TableColumn<Account, String> customerCol =
                new TableColumn<>("Nasabah");
        customerCol.setCellValueFactory(data -> {
            Customer c = bankService.getCustomerById(
                data.getValue().getCustomerId());
            String name = c != null ? c.getFullName()
                    : data.getValue().getCustomerId();
            return new SimpleStringProperty(name);
        });

        // Kolom Tipe Rekening — pakai displayName enum
        TableColumn<Account, String> typeCol =
                new TableColumn<>("Tipe");
        typeCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getAccountType().getDisplayName()));
        typeCol.setPrefWidth(90);

        // Kolom Saldo — format currency
        TableColumn<Account, String> balanceCol =
                new TableColumn<>("Saldo");
        balanceCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                CURRENCY.format(data.getValue().getBalance())));
        balanceCol.setPrefWidth(150);

        // Kolom Tanggal Buka
        TableColumn<Account, String> dateCol =
                new TableColumn<>("Tgl Buka");
        dateCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getOpenDate()));
        dateCol.setPrefWidth(100);

        // Kolom Info Tambahan — khusus Deposito tampilkan tenor
        TableColumn<Account, String> infoCol =
                new TableColumn<>("Info");
        infoCol.setCellValueFactory(data -> {
            Account acc = data.getValue();
            if (acc instanceof DepositAccount) {
                DepositAccount dep = (DepositAccount) acc;
                // instanceof + cast — cara Java pre-16 untuk
                // pattern matching, valid di JDK 25
                return new SimpleStringProperty(
                    "Tenor: " + dep.getTenorMonths()
                    + " bln | Jatuh: " + dep.getMaturityDate());
            }
            return new SimpleStringProperty("-");
        });

        // Kolom Aksi — Hapus saja (tidak ada edit rekening)
        TableColumn<Account, Void> actionCol =
                new TableColumn<>("Aksi");
        actionCol.setPrefWidth(90);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Hapus");
            {
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setStyle(
                    "-fx-font-size: 11px; -fx-padding: 4 10 4 10;");
                deleteBtn.setOnAction(e -> {
                    Account acc = getTableView()
                            .getItems().get(getIndex());
                    showDeleteConfirmation(acc);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });

        table.getColumns().addAll(
            idCol, customerCol, typeCol,
            balanceCol, dateCol, infoCol, actionCol);
        table.setPlaceholder(
            new Label("Belum ada data rekening."));

        return table;
    }

    private void refreshData() {
        accountData.setAll(bankService.getAllAccounts());
    }

    private void filterData(String query) {
        if (query == null || query.trim().isEmpty()) {
            accountData.setAll(bankService.getAllAccounts());
            return;
        }
        String lower = query.toLowerCase().trim();
        List<Account> filtered = bankService.getAllAccounts()
                .stream()
                .filter(a ->
                    a.getAccountId().toLowerCase().contains(lower) ||
                    a.getCustomerId().toLowerCase().contains(lower) ||
                    a.getAccountType().getDisplayName()
                        .toLowerCase().contains(lower))
                .collect(Collectors.toList());
        accountData.setAll(filtered);
    }

    /**
     * Dialog buka rekening baru.
     * Field yang ditampilkan berubah secara dinamis
     * berdasarkan tipe rekening yang dipilih.
     */
    private void showOpenAccountDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Buka Rekening Baru");
        dialog.setHeaderText("Isi data pembukaan rekening");

        // ComboBox untuk pilih Customer
        ComboBox<String> customerCombo = new ComboBox<>();
        customerCombo.setPrefHeight(36);
        customerCombo.setMaxWidth(Double.MAX_VALUE);
        // Isi dengan format "C001 - Budi Santoso"
        bankService.getAllCustomers().forEach(c ->
            customerCombo.getItems().add(
                c.getCustomerId() + " - " + c.getFullName()));
        if (!customerCombo.getItems().isEmpty())
            customerCombo.getSelectionModel().selectFirst();

        // ComboBox tipe rekening
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.setPrefHeight(36);
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.getItems().addAll(
            "Tabungan", "Giro", "Deposito");
        typeCombo.getSelectionModel().selectFirst();

        TextField depositField = new TextField();
        depositField.setPromptText("Contoh: 1000000");
        depositField.setPrefHeight(36);

        // Tenor field — hanya muncul saat Deposito dipilih
        TextField tenorField = new TextField();
        tenorField.setPromptText("Tenor dalam bulan (contoh: 12)");
        tenorField.setPrefHeight(36);

        Label tenorLabel = new Label("Tenor (bulan)");
        tenorLabel.getStyleClass().add("label-subtitle");

        // VBox form — tenor ditambah/hapus secara dinamis
        VBox form = new VBox(8);
        form.setPadding(new Insets(16));
        form.setPrefWidth(360);

        Label custLabel    = new Label("Nasabah");
        Label typeLabel    = new Label("Tipe Rekening");
        Label depositLabel = new Label("Setoran Awal (Rp)");
        custLabel.getStyleClass().add("label-subtitle");
        typeLabel.getStyleClass().add("label-subtitle");
        depositLabel.getStyleClass().add("label-subtitle");

        form.getChildren().addAll(
            custLabel, customerCombo,
            typeLabel, typeCombo,
            depositLabel, depositField
        );

        // Listener tipe — tambah/hapus tenor field secara dinamis
        typeCombo.valueProperty().addListener((obs, old, newVal) -> {
            form.getChildren().removeAll(tenorLabel, tenorField);
            if ("Deposito".equals(newVal)) {
                form.getChildren().addAll(tenorLabel, tenorField);
            }
        });

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(
            ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().addAll(
            table.getScene().getStylesheets());

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    // Ekstrak customerId dari pilihan combo
                    String selected = customerCombo
                            .getSelectionModel().getSelectedItem();
                    if (selected == null) {
                        showAlert(Alert.AlertType.ERROR,
                            "Error", "Pilih nasabah terlebih dahulu.");
                        return;
                    }
                    String customerId = selected.split(" - ")[0];

                    // Parse tipe rekening
                    AccountType type;
                    switch (typeCombo.getValue()) {
                        case "Giro":     type = AccountType.CURRENT; break;
                        case "Deposito": type = AccountType.DEPOSIT; break;
                        default:         type = AccountType.SAVINGS; break;
                    }

                    double deposit = Double.parseDouble(
                        depositField.getText().trim());
                    int tenor = 0;
                    if (type == AccountType.DEPOSIT) {
                        tenor = Integer.parseInt(
                            tenorField.getText().trim());
                    }

                    bankService.openAccount(
                        customerId, type, deposit, tenor);
                    refreshData();
                    showAlert(Alert.AlertType.INFORMATION,
                        "Berhasil", "Rekening berhasil dibuka.");

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

    private void showDeleteConfirmation(Account account) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus");
        alert.setHeaderText("Hapus rekening: "
                + account.getAccountId());
        alert.setContentText(
            "Rekening dan seluruh data terkait akan dihapus.\n" +
            "Tindakan tidak dapat dibatalkan.\n\nLanjutkan?");
        alert.getDialogPane().getStylesheets().addAll(
            table.getScene().getStylesheets());

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                bankService.getAccountRepository()
                    .delete(account.getAccountId());
                refreshData();
                showAlert(Alert.AlertType.INFORMATION,
                    "Berhasil", "Rekening berhasil dihapus.");
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