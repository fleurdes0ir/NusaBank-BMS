/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.ui.panels;

import banking.model.Customer;
import banking.service.BankService;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * CustomerPanel menampilkan manajemen data nasabah.
 *
 * Fitur:
 * - Tabel nasabah dengan search realtime
 * - Tambah nasabah baru via dialog form
 * - Edit data nasabah via dialog form
 * - Hapus nasabah dengan konfirmasi
 *
 * Layout:
 * ┌─────────────────────────────────────┐
 * │ NASABAH  [Search...]  [+ Tambah]    │ ← Header + toolbar
 * ├─────────────────────────────────────┤
 * │ ID │ Nama │ Email │ Phone │ Aksi    │ ← Table
 * │ ...                                 │
 * └─────────────────────────────────────┘
 */
public class CustomerPanel {

    private final BankService bankService = BankService.getInstance();

    // ObservableList — JavaFX list yang otomatis update UI
    // saat data berubah. Ini adalah binding pattern di JavaFX.
    private final ObservableList<Customer> customerData =
            FXCollections.observableArrayList();

    private TableView<Customer> table;
    private TextField searchField;

    public VBox getRoot() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(8));

        root.getChildren().addAll(
            buildHeader(),
            buildTable()
        );

        // Load data awal
        refreshData();
        return root;
    }

    /**
     * Header: judul + search field + tombol tambah.
     */
    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Manajemen Nasabah");
        title.getStyleClass().add("label-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        // Search field — filter realtime saat user mengetik
        searchField = new TextField();
        searchField.setPromptText("Cari nama, email, atau ID...");
        searchField.setPrefWidth(250);
        searchField.setPrefHeight(36);

        // Listener — dipanggil setiap kali teks berubah
        // ChangeListener: (observable, oldValue, newValue)
        searchField.textProperty().addListener(
            (obs, oldVal, newVal) -> filterData(newVal));

        Button addBtn = new Button("+ Tambah Nasabah");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setPrefHeight(36);
        addBtn.setOnAction(e -> showAddDialog());

        header.getChildren().addAll(title, searchField, addBtn);
        return header;
    }

    /**
     * Tabel nasabah dengan kolom ID, Nama, Email, Telepon, Aksi.
     *
     * TableView adalah komponen JavaFX untuk menampilkan data tabular.
     * Setiap kolom (TableColumn) didefinisikan dengan CellValueFactory
     * yang menentukan data apa yang ditampilkan.
     */
    private TableView<Customer> buildTable() {
        table = new TableView<>();
        table.setItems(customerData);
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(
            TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPrefHeight(500);

        // ── Kolom ID ──
        // SimpleStringProperty wraps String menjadi Observable
        // yang dibutuhkan TableColumn
        TableColumn<Customer, String> idCol =
                new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getCustomerId()));
        idCol.setPrefWidth(70);

        // ── Kolom Nama ──
        TableColumn<Customer, String> nameCol =
                new TableColumn<>("Nama Lengkap");
        nameCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getFullName()));

        // ── Kolom Email ──
        TableColumn<Customer, String> emailCol =
                new TableColumn<>("Email");
        emailCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getEmail()));

        // ── Kolom Telepon ──
        TableColumn<Customer, String> phoneCol =
                new TableColumn<>("Telepon");
        phoneCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getPhone()));
        phoneCol.setPrefWidth(130);

        // ── Kolom Tanggal Daftar ──
        TableColumn<Customer, String> dateCol =
                new TableColumn<>("Tgl Daftar");
        dateCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getJoinDate()));
        dateCol.setPrefWidth(100);

        // ── Kolom Aksi ──
        // Kolom khusus berisi tombol Edit dan Hapus per baris
        TableColumn<Customer, Void> actionCol =
                new TableColumn<>("Aksi");
        actionCol.setPrefWidth(140);
        actionCol.setCellFactory(col -> new TableCell<>() {
            // Tombol dibuat sekali per cell, reused saat scroll
            private final Button editBtn  = new Button("Edit");
            private final Button deleteBtn = new Button("Hapus");
            private final HBox box = new HBox(6, editBtn, deleteBtn);

            // Inisialisasi style dan handler sekali
            {
                editBtn.getStyleClass().add("btn-secondary");
                editBtn.setStyle("-fx-font-size: 11px; " +
                        "-fx-padding: 4 10 4 10;");
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setStyle("-fx-font-size: 11px; " +
                        "-fx-padding: 4 10 4 10;");

                editBtn.setOnAction(e -> {
                    // getIndex() mengembalikan index baris saat ini
                    Customer c = getTableView()
                            .getItems().get(getIndex());
                    showEditDialog(c);
                });

                deleteBtn.setOnAction(e -> {
                    Customer c = getTableView()
                            .getItems().get(getIndex());
                    showDeleteConfirmation(c);
                });
            }

            /**
             * updateItem dipanggil JavaFX setiap kali cell di-render.
             * Wajib override untuk custom cell content.
             *
             * @param item  data item (Void karena kolom aksi tidak
             *              punya data model)
             * @param empty true jika baris kosong (padding row)
             */
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                // Jika baris kosong, jangan tampilkan apapun
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(
            idCol, nameCol, emailCol, phoneCol, dateCol, actionCol);

        // Placeholder saat tabel kosong
        table.setPlaceholder(
            new Label("Belum ada data nasabah."));

        return table;
    }

    /**
     * Memuat ulang data dari repository ke ObservableList.
     * Dipanggil setelah operasi tambah, edit, atau hapus.
     */
    private void refreshData() {
        customerData.setAll(bankService.getAllCustomers());
    }

    /**
     * Filter data tabel berdasarkan query pencarian.
     * Mencari di field: ID, nama, dan email.
     *
     * @param query string pencarian dari search field
     */
    private void filterData(String query) {
        if (query == null || query.trim().isEmpty()) {
            // Query kosong — tampilkan semua data
            customerData.setAll(bankService.getAllCustomers());
            return;
        }

        String lower = query.toLowerCase().trim();

        // Stream filter — cari di ID, nama, dan email
        List<Customer> filtered = bankService.getAllCustomers()
                .stream()
                .filter(c ->
                    c.getCustomerId().toLowerCase().contains(lower) ||
                    c.getFullName().toLowerCase().contains(lower)   ||
                    c.getEmail().toLowerCase().contains(lower))
                .collect(Collectors.toList());

        customerData.setAll(filtered);
    }

    /**
     * Dialog tambah nasabah baru.
     *
     * Dialog adalah window kecil yang muncul di atas window utama.
     * Kita pakai Dialog<ButtonType> dari JavaFX — lebih sederhana
     * dari membuat Stage baru.
     */
    private void showAddDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Tambah Nasabah Baru");
        dialog.setHeaderText("Isi data nasabah baru");

        // Form fields
        TextField nameField     = createField("Nama lengkap");
        TextField emailField    = createField("Alamat email");
        TextField phoneField    = createField("Nomor telepon");
        TextField addressField  = createField("Alamat lengkap");
        TextField usernameField = createField("Username login");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Password login");
        passField.setPrefHeight(36);

        VBox form = buildForm(
            "Nama Lengkap",    nameField,
            "Email",           emailField,
            "Telepon",         phoneField,
            "Alamat",          addressField,
            "Username",        usernameField,
            "Password",        passField
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(
            ButtonType.OK, ButtonType.CANCEL);

        // Style dialog pane agar mengikuti tema
        dialog.getDialogPane().getStylesheets().addAll(
            table.getScene().getStylesheets());

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    // Validasi field tidak kosong
                    if (nameField.getText().trim().isEmpty() ||
                        emailField.getText().trim().isEmpty() ||
                        usernameField.getText().trim().isEmpty() ||
                        passField.getText().isEmpty()) {
                        showAlert(Alert.AlertType.ERROR,
                            "Error", "Semua field wajib diisi.");
                        return;
                    }

                    // Panggil BankService untuk create customer
                    bankService.createCustomer(
                        nameField.getText().trim(),
                        emailField.getText().trim(),
                        phoneField.getText().trim(),
                        addressField.getText().trim(),
                        usernameField.getText().trim(),
                        passField.getText()
                    );

                    refreshData();
                    showAlert(Alert.AlertType.INFORMATION,
                        "Berhasil", "Nasabah berhasil ditambahkan.");

                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR,
                        "Error", ex.getMessage());
                }
            }
        });
    }

    /**
     * Dialog edit data nasabah yang sudah ada.
     *
     * @param customer Customer yang akan diedit
     */
    private void showEditDialog(Customer customer) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Nasabah");
        dialog.setHeaderText("Edit data: " + customer.getFullName());

        TextField nameField    = createField(customer.getFullName());
        TextField emailField   = createField(customer.getEmail());
        TextField phoneField   = createField(customer.getPhone());
        TextField addressField = createField(customer.getAddress());

        // Pre-fill dengan data existing
        nameField.setText(customer.getFullName());
        emailField.setText(customer.getEmail());
        phoneField.setText(customer.getPhone());
        addressField.setText(customer.getAddress());

        VBox form = buildForm(
            "Nama Lengkap", nameField,
            "Email",        emailField,
            "Telepon",      phoneField,
            "Alamat",       addressField
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(
            ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().addAll(
            table.getScene().getStylesheets());

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    // Update field customer
                    customer.setFullName(nameField.getText().trim());
                    customer.setEmail(emailField.getText().trim());
                    customer.setPhone(phoneField.getText().trim());
                    customer.setAddress(addressField.getText().trim());

                    bankService.updateCustomer(customer);
                    refreshData();
                    showAlert(Alert.AlertType.INFORMATION,
                        "Berhasil", "Data nasabah berhasil diperbarui.");

                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR,
                        "Error", ex.getMessage());
                }
            }
        });
    }

    /**
     * Dialog konfirmasi hapus nasabah.
     *
     * Best practice: selalu minta konfirmasi sebelum operasi
     * destruktif yang tidak bisa di-undo.
     *
     * @param customer Customer yang akan dihapus
     */
    private void showDeleteConfirmation(Customer customer) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus");
        alert.setHeaderText("Hapus nasabah: " + customer.getFullName());
        alert.setContentText(
            "Tindakan ini akan menghapus nasabah beserta semua " +
            "rekening terkait.\nData tidak dapat dikembalikan.\n\n" +
            "Lanjutkan?");

        alert.getDialogPane().getStylesheets().addAll(
            table.getScene().getStylesheets());

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                bankService.deleteCustomer(customer.getCustomerId());
                refreshData();
                showAlert(Alert.AlertType.INFORMATION,
                    "Berhasil", "Nasabah berhasil dihapus.");
            }
        });
    }

    // ── Helper Methods ──

    /**
     * Helper — buat TextField standar dengan prompt text.
     */
    private TextField createField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(36);
        field.setPrefWidth(320);
        return field;
    }

    /**
     * Helper — buat VBox form dari pasangan label-field.
     *
     * Parameter varargs: label1, field1, label2, field2, dst.
     * Varargs (Object...) memungkinkan jumlah parameter fleksibel.
     *
     * @param labelFieldPairs pasangan String label dan Control field
     * @return VBox form yang sudah tersusun
     */
    private VBox buildForm(Object... labelFieldPairs) {
        VBox form = new VBox(8);
        form.setPadding(new Insets(16));
        form.setPrefWidth(360);

        // Iterasi dua per dua: [label, field, label, field, ...]
        for (int i = 0; i < labelFieldPairs.length; i += 2) {
            Label label = new Label((String) labelFieldPairs[i]);
            label.getStyleClass().add("label-subtitle");

            javafx.scene.control.Control field =
                (javafx.scene.control.Control) labelFieldPairs[i + 1];
            field.setMaxWidth(Double.MAX_VALUE);

            form.getChildren().addAll(label, field);
        }
        return form;
    }

    /**
     * Helper — tampilkan Alert dialog sederhana.
     *
     * @param type    tipe alert (ERROR, INFORMATION, CONFIRMATION)
     * @param title   judul alert
     * @param message pesan yang ditampilkan
     */
    private void showAlert(Alert.AlertType type,
            String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}