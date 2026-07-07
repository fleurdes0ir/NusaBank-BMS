/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.ui.panels;

import banking.model.Customer;
import banking.model.Loan;
import banking.model.enums.LoanStatus;
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
 * LoanPanel menampilkan manajemen pinjaman.
 *
 * Fitur:
 * - Tabel semua pinjaman dengan filter pencarian
 * - Ajukan pinjaman baru via dialog dengan preview cicilan realtime
 * - Tandai pinjaman sebagai lunas
 *
 * Layout:
 * ┌──────────────────────────────────────────┐
 * │ PINJAMAN  [Search...]  [+ Ajukan]        │
 * ├──────────────────────────────────────────┤
 * │ ID │ Nasabah │ Pokok │ Cicilan │ Status  │
 * └──────────────────────────────────────────┘
 */
public class LoanPanel {

    private final BankService bankService = BankService.getInstance();
    private final ObservableList<Loan> loanData =
            FXCollections.observableArrayList();
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    private TableView<Loan> table;
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

        Label title = new Label("Manajemen Pinjaman");
        title.getStyleClass().add("label-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Cari ID atau nasabah...");
        searchField.setPrefWidth(220);
        searchField.setPrefHeight(36);
        searchField.textProperty().addListener(
            (obs, oldVal, newVal) -> filterData(newVal));

        Button addBtn = new Button("+ Ajukan Pinjaman");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setPrefHeight(36);
        addBtn.setOnAction(e -> showApplyLoanDialog());

        header.getChildren().addAll(title, searchField, addBtn);
        return header;
    }

    private TableView<Loan> buildTable() {
        table = new TableView<>();
        table.setItems(loanData);
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(
            TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPrefHeight(500);

        // Kolom ID
        TableColumn<Loan, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getLoanId()));
        idCol.setPrefWidth(70);

        // Kolom Nasabah
        TableColumn<Loan, String> customerCol =
                new TableColumn<>("Nasabah");
        customerCol.setCellValueFactory(data -> {
            Customer c = bankService.getCustomerById(
                data.getValue().getCustomerId());
            return new SimpleStringProperty(
                c != null ? c.getFullName()
                          : data.getValue().getCustomerId());
        });

        // Kolom Pokok Pinjaman
        TableColumn<Loan, String> principalCol =
                new TableColumn<>("Pokok");
        principalCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                CURRENCY.format(data.getValue().getPrincipal())));
        principalCol.setPrefWidth(140);

        // Kolom Cicilan per Bulan
        TableColumn<Loan, String> monthlyCol =
                new TableColumn<>("Cicilan/bln");
        monthlyCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                CURRENCY.format(
                    data.getValue().getMonthlyPayment())));
        monthlyCol.setPrefWidth(140);

        // Kolom Tenor
        TableColumn<Loan, String> tenorCol =
                new TableColumn<>("Tenor");
        tenorCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getPaidMonths()
                + "/" + data.getValue().getTenorMonths()
                + " bln"));
        tenorCol.setPrefWidth(80);

        // Kolom Status — dengan color coding
        TableColumn<Loan, String> statusCol =
                new TableColumn<>("Status");
        statusCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getStatus().getDisplayName()));
        statusCol.setPrefWidth(80);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                // Warna berdasarkan status
                if ("Aktif".equals(item)) {
                    setStyle("-fx-text-fill: #34d399; " +
                             "-fx-font-weight: bold;");
                } else if ("Lunas".equals(item)) {
                    setStyle("-fx-text-fill: #8b92a5;");
                } else {
                    setStyle("-fx-text-fill: #fbbf24;");
                }
            }
        });

        // Kolom Keterangan
        TableColumn<Loan, String> descCol =
                new TableColumn<>("Keterangan");
        descCol.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getDescription()));

        // Kolom Aksi — tombol Lunas
        // Kolom Aksi — Tombol Manajemen Admin (Approve / Reject / Lunas)
        TableColumn<Loan, Void> actionCol = new TableColumn<>("Aksi");
        actionCol.setPrefWidth(180); // Diperlebar agar muat 2 tombol sejajar
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button approveBtn = new Button("Setujui");
            private final Button rejectBtn = new Button("Tolak");
            private final Button paidBtn = new Button("Tandai Lunas");
            private final HBox container = new HBox(6);

            {
                // Styling tombol-tombol Admin
                approveBtn.getStyleClass().add("btn-primary");
                approveBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8 4 8; -fx-background-color: #10b981;");
                approveBtn.setOnAction(e -> {
                    Loan loan = getTableView().getItems().get(getIndex());
                    processLoanApproval(loan, true);
                });

                rejectBtn.getStyleClass().add("btn-secondary");
                rejectBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8 4 8; -fx-background-color: #ef4444; -fx-text-fill: white;");
                rejectBtn.setOnAction(e -> {
                    Loan loan = getTableView().getItems().get(getIndex());
                    processLoanApproval(loan, false);
                });

                paidBtn.getStyleClass().add("btn-secondary");
                paidBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8 4 8;");
                paidBtn.setOnAction(e -> {
                    Loan loan = getTableView().getItems().get(getIndex());
                    markAsPaid(loan);
                });
                
                container.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                Loan loan = getTableView().getItems().get(getIndex());
                container.getChildren().clear();

                // Kondisi Percabangan Tombol Berdasarkan Status Pinjaman
                if (loan.getStatus() == LoanStatus.PENDING) {
                    // Jika PENDING, Admin bisa Approve atau Reject
                    container.getChildren().addAll(approveBtn, rejectBtn);
                    setGraphic(container);
                } else if (loan.getStatus() == LoanStatus.ACTIVE) {
                    // Jika ACTIVE, tampilkan tombol Tandai Lunas
                    container.getChildren().add(paidBtn);
                    setGraphic(container);
                } else {
                    // Jika REJECTED atau PAID, tidak butuh aksi lagi
                    setGraphic(null);
                }
            }
        });

        table.getColumns().addAll(
            idCol, customerCol, principalCol,
            monthlyCol, tenorCol, statusCol,
            descCol, actionCol);
        table.setPlaceholder(
            new Label("Belum ada data pinjaman."));

        return table;
    }

    private void refreshData() {
        loanData.setAll(bankService.getAllLoans());
    }

    private void filterData(String query) {
        if (query == null || query.trim().isEmpty()) {
            loanData.setAll(bankService.getAllLoans());
            return;
        }
        String lower = query.toLowerCase().trim();
        List<Loan> filtered = bankService.getAllLoans()
                .stream()
                .filter(l -> {
                    Customer c = bankService.getCustomerById(
                            l.getCustomerId());
                    String name = c != null
                            ? c.getFullName().toLowerCase() : "";
                    return l.getLoanId().toLowerCase().contains(lower)
                        || l.getCustomerId().toLowerCase().contains(lower)
                        || name.contains(lower);
                })
                .collect(Collectors.toList());
        loanData.setAll(filtered);
    }

    /**
     * Dialog ajukan pinjaman baru dengan preview cicilan realtime.
     *
     * Preview cicilan diupdate otomatis setiap kali nominal
     * atau tenor berubah — tanpa perlu klik tombol apapun.
     * Ini menggunakan ChangeListener pada TextField.
     */
    private void showApplyLoanDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ajukan Pinjaman");
        dialog.setHeaderText("Pengajuan pinjaman baru");

        // ComboBox nasabah
        ComboBox<String> customerCombo = new ComboBox<>();
        customerCombo.setPrefHeight(36);
        customerCombo.setMaxWidth(Double.MAX_VALUE);
        bankService.getAllCustomers().forEach(c ->
            customerCombo.getItems().add(
                c.getCustomerId() + " - " + c.getFullName()));
        if (!customerCombo.getItems().isEmpty())
            customerCombo.getSelectionModel().selectFirst();

        TextField principalField = new TextField();
        principalField.setPromptText("Contoh: 10000000");
        principalField.setPrefHeight(36);

        TextField tenorField = new TextField();
        tenorField.setPromptText("Contoh: 12");
        tenorField.setPrefHeight(36);

        TextField descField = new TextField("Pinjaman Personal");
        descField.setPrefHeight(36);

        // Rate field
        TextField rateField = new TextField("12");
        rateField.setPrefHeight(36);

        // LoanType combo
        ComboBox<String> loanTypeCombo = new ComboBox<>();
        loanTypeCombo.getItems().addAll("Flat Rate", "Anuitas");
        loanTypeCombo.getSelectionModel().selectFirst();
        loanTypeCombo.setPrefHeight(36);
        loanTypeCombo.setMaxWidth(Double.MAX_VALUE);
        
        // Preview cicilan — update realtime
        // Label ini di-update oleh ChangeListener
        Label previewLabel = new Label("Cicilan/bulan: -");
        previewLabel.setStyle(
            "-fx-font-size: 14px; -fx-font-weight: bold; " +
            "-fx-text-fill: #4f8ef7;");

        // ChangeListener untuk update preview cicilan
        // Dipanggil setiap kali nilai principal atau tenor berubah
        javafx.beans.value.ChangeListener<String> previewUpdater =
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

                double monthly = bankService.calculateMonthlyPayment(
                    lt, p, r, t);
                double total   = banking.util.LoanCalculator
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

        principalField.textProperty().addListener(previewUpdater);
        tenorField.textProperty().addListener(previewUpdater);
        rateField.textProperty().addListener(previewUpdater);
        loanTypeCombo.valueProperty().addListener(
                (obs, o, n) -> previewUpdater.changed(null, null, null));

        // Attach listener ke kedua field
        principalField.textProperty().addListener(previewUpdater);
        tenorField.textProperty().addListener(previewUpdater);

        VBox form = new VBox(8);
        form.setPadding(new Insets(16));
        form.setPrefWidth(380);

        Label custLabel  = new Label("Nasabah");
        Label prcpLabel  = new Label("Jumlah Pinjaman (Rp)");
        Label tenorLabel = new Label("Tenor (bulan)");
        Label descLabel  = new Label("Keterangan");
        Label rateLabel     = new Label("Suku Bunga (% per tahun)");
        Label loanTypeLabel = new Label("Metode Bunga");
        custLabel.getStyleClass().add("label-subtitle");
        prcpLabel.getStyleClass().add("label-subtitle");
        tenorLabel.getStyleClass().add("label-subtitle");
        descLabel.getStyleClass().add("label-subtitle");
        rateLabel.getStyleClass().add("label-subtitle");
        loanTypeLabel.getStyleClass().add("label-subtitle");

        // Separator visual sebelum preview
        Separator sep = new Separator();

        form.getChildren().addAll(
        custLabel,      customerCombo,
        prcpLabel,      principalField,
        tenorLabel,     tenorField,
        rateLabel,      rateField,
        loanTypeLabel,  loanTypeCombo,
        descLabel,      descField,
        sep,            previewLabel
    );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(
            ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().addAll(
            table.getScene().getStylesheets());

        dialog.showAndWait().ifPresent(result -> {
        if (result == ButtonType.OK) {
            try {
                String selected = customerCombo
                        .getSelectionModel().getSelectedItem();
                if (selected == null) {
                    showAlert(Alert.AlertType.ERROR,
                        "Error", "Pilih nasabah terlebih dahulu.");
                    return;
                }
                String customerId = selected.split(" - ")[0];
                double principal  = Double.parseDouble(
                    principalField.getText().trim());
                int tenor         = Integer.parseInt(
                    tenorField.getText().trim());
                double rateValue  = Double.parseDouble(
                    rateField.getText().trim());
                banking.model.enums.LoanType selectedLoanType =
                    loanTypeCombo.getValue().equals("Anuitas")
                    ? banking.model.enums.LoanType.ANNUITY
                    : banking.model.enums.LoanType.FLAT;
                String desc = descField.getText().trim();

                bankService.applyLoan(customerId, principal,
                    tenor, desc, rateValue, selectedLoanType);
                refreshData();
                showAlert(Alert.AlertType.INFORMATION,
                    "Berhasil", "Pinjaman berhasil diajukan.");

            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR,
                    "Error", "Nominal atau tenor tidak valid.");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR,
                    "Error", ex.getMessage());
            }
        }
    });
}
    
    /**
     * Tandai pinjaman sebagai lunas.
     *
     * Update status Loan menjadi PAID dan simpan ke repository.
     *
     * @param loan pinjaman yang akan ditandai lunas
     */
    private void markAsPaid(Loan loan) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi");
        confirm.setHeaderText("Tandai pinjaman sebagai lunas?");
        confirm.setContentText(
            "Pinjaman " + loan.getLoanId()
            + " akan ditandai LUNAS.\nTindakan tidak dapat dibatalkan.");
        confirm.getDialogPane().getStylesheets().addAll(
            table.getScene().getStylesheets());

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // Update status dan paid months ke tenor penuh
                loan.setStatus(LoanStatus.PAID);
                loan.setPaidMonths(loan.getTenorMonths());
                bankService.getLoanRepository().update(loan);
                refreshData();
                showAlert(Alert.AlertType.INFORMATION,
                    "Berhasil", "Pinjaman berhasil ditandai lunas.");
            }
        });
    }
    
    /**
     * Memproses persetujuan atau penolakan pinjaman oleh Admin.
     */
    private void processLoanApproval(Loan loan, boolean isApprove) {
        String adminUsername = "Admin"; // Fallback default username admin
        
        if (isApprove) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Konfirmasi Persetujuan");
            confirm.setHeaderText("Setujui Pengajuan Pinjaman?");
            confirm.setContentText("Pinjaman " + loan.getLoanId() + " sebesar Rp " 
                    + String.format("%,.0f", loan.getPrincipal()) + " akan diaktifkan.");
            
            confirm.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    try {
                        bankService.approveLoan(loan.getLoanId(), adminUsername);
                        refreshData();
                        showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Pinjaman berhasil disetujui dan berstatus AKTIF.");
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
                    }
                }
            });
        } else {
            // Jika REJECT, wajib memunculkan TextInputDialog untuk mengisi alasan penolakan
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Penolakan Pinjaman");
            dialog.setHeaderText("Alasan Penolakan Pinjaman " + loan.getLoanId());
            dialog.setContentText("Masukkan alasan penolakan:");

            dialog.showAndWait().ifPresent(reason -> {
                if (reason.trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Alasan penolakan wajib diisi!");
                    return;
                }
                try {
                    bankService.rejectLoan(loan.getLoanId(), adminUsername, reason.trim());
                    refreshData();
                    showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Pengajuan pinjaman resmi ditolak.");
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
                }
            });
        }
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