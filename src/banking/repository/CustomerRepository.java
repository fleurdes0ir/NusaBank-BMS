/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.repository;

import banking.model.Customer;
import banking.util.AppConfig;
import banking.util.CsvUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * CustomerRepository bertanggung jawab HANYA untuk operasi baca/tulis
 * data Customer ke file CSV.
 *
 * Ini adalah implementasi pattern REPOSITORY — memisahkan logika
 * akses data dari logika bisnis. Service layer tidak perlu tahu
 * bagaimana data disimpan, cukup panggil method di sini.
 *
 * Single Responsibility Principle (SRP): class ini hanya urusan
 * persistensi Customer, tidak lebih.
 */
public class CustomerRepository {

    /**
     * Mengambil semua data Customer dari file CSV.
     *
     * Proses: baca raw CSV → parsing tiap baris → konversi ke objek Customer
     * Format CSV: customerId,fullName,email,phone,address,joinDate
     *
     * @return List berisi semua objek Customer
     */
    public List<Customer> findAll() {
        List<Customer> customers = new ArrayList<>();

        // readAll() mengembalikan List<String[]> — tiap array = satu baris CSV
        List<String[]> rows = CsvUtil.readAll(AppConfig.CUSTOMERS_FILE);

        for (String[] row : rows) {
            // Validasi jumlah kolom — hindari ArrayIndexOutOfBoundsException
            // jika ada baris CSV yang corrupt atau tidak lengkap
            if (row.length < 6) continue;

            // Mapping kolom CSV ke parameter constructor Customer
            // Index 0 = customerId, 1 = fullName, dst (sesuai urutan toString())
            customers.add(new Customer(
                row[0].trim(), // customerId
                row[1].trim(), // fullName
                row[2].trim(), // email
                row[3].trim(), // phone
                row[4].trim(), // address
                row[5].trim()  // joinDate
            ));
        }
        return customers;
    }

    /**
     * Mencari Customer berdasarkan ID.
     *
     * Menggunakan Java Stream API untuk filtering — lebih ekspresif
     * dari loop manual. filter() menyaring, findFirst() mengambil
     * hasil pertama, orElse(null) mengembalikan null jika tidak ditemukan.
     *
     * @param customerId ID yang dicari
     * @return objek Customer jika ditemukan, null jika tidak
     */
    public Customer findById(String customerId) {
        return findAll().stream()
                .filter(c -> c.getCustomerId().equals(customerId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Menyimpan Customer baru ke file CSV.
     *
     * Strategi: load semua data → tambah data baru → tulis ulang seluruh file.
     * Ini lebih aman dari append langsung karena menghindari duplikasi.
     *
     * @param customer objek Customer yang akan disimpan
     */
    public void save(Customer customer) {
        // Load existing data terlebih dahulu
        List<Customer> customers = findAll();

        // Tambahkan customer baru ke list
        customers.add(customer);

        // Konversi list Customer ke list String untuk ditulis ke CSV
        // customer.toString() sudah menghasilkan format CSV yang benar
        writeToFile(customers);
    }

    /**
     * Memperbarui data Customer yang sudah ada.
     *
     * Strategi: loop semua data → temukan yang ID-nya cocok → replace.
     * Jika tidak ditemukan, data tidak berubah (silent fail — bisa
     * ditingkatkan dengan exception jika diperlukan).
     *
     * @param updated objek Customer dengan data baru
     */
    public void update(Customer updated) {
        List<Customer> customers = findAll();

        // Iterasi dengan index untuk keperluan replacement
        for (int i = 0; i < customers.size(); i++) {
            if (customers.get(i).getCustomerId().equals(updated.getCustomerId())) {
                // Temukan — replace objek lama dengan yang baru
                customers.set(i, updated);
                break; // ID unik, tidak perlu lanjut iterasi
            }
        }
        writeToFile(customers);
    }

    /**
     * Menghapus Customer berdasarkan ID.
     *
     * Menggunakan removeIf() — cara idiomatik Java untuk menghapus
     * elemen dari collection berdasarkan kondisi (predicate).
     *
     * @param customerId ID Customer yang akan dihapus
     */
    public void delete(String customerId) {
        List<Customer> customers = findAll();

        // removeIf() menghapus semua elemen yang memenuhi kondisi lambda
        customers.removeIf(c -> c.getCustomerId().equals(customerId));
        writeToFile(customers);
    }

    /**
     * Menghitung total jumlah Customer.
     *
     * @return jumlah Customer yang tersimpan
     */
    public int count() {
        return findAll().size();
    }

    /**
     * Helper method private — konversi List<Customer> ke List<String>
     * lalu tulis ke file CSV.
     *
     * Dibuat private karena hanya dipakai internal class ini.
     * Ini menerapkan prinsip encapsulation — detail implementasi
     * penulisan file disembunyikan dari luar.
     *
     * @param customers List Customer yang akan ditulis
     */
    private void writeToFile(List<Customer> customers) {
        List<String> lines = new ArrayList<>();
        for (Customer c : customers) {
            // toString() Customer sudah format CSV: "id,nama,email,..."
            lines.add(c.toString());
        }
        CsvUtil.writeAll(AppConfig.CUSTOMERS_FILE, lines);
    }
}