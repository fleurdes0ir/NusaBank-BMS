/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.service;

import banking.model.User;
import banking.model.Customer;
import banking.repository.UserRepository;
import banking.repository.CustomerRepository;
import banking.util.CsvUtil;

/**
 * AuthService menangani autentikasi dan sesi login aktif.
 *
 * Implementasi pattern SINGLETON — hanya boleh ada SATU instance
 * AuthService di seluruh aplikasi. Alasannya: sesi login adalah
 * state global yang harus konsisten di semua panel UI.
 *
 * Cara kerja Singleton di Java:
 * 1. Constructor dibuat private — tidak bisa di-instantiate dari luar
 * 2. Instance disimpan sebagai static field
 * 3. Akses via getInstance() — buat baru jika belum ada, return yang
 *    sudah ada jika sudah pernah dibuat
 */
public class AuthService {

    // Satu-satunya instance AuthService di seluruh aplikasi
    // 'static' — milik class, bukan objek
    // 'volatile' — memastikan visibility di multi-thread environment
    private static volatile AuthService instance;

    // Repository untuk akses data User dan Customer
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    // State sesi login aktif — null jika belum login
    private User currentUser;
    private Customer currentCustomer;

    /**
     * Private constructor — hanya bisa dipanggil dari dalam class ini.
     * Inisialisasi repository di sini.
     */
    private AuthService() {
        this.userRepository = new UserRepository();
        this.customerRepository = new CustomerRepository();
    }

    /**
     * Satu-satunya cara mendapatkan instance AuthService.
     *
     * Double-checked locking pattern — thread-safe dan efisien:
     * - Check pertama (tanpa synchronized): hindari overhead locking
     *   jika instance sudah ada
     * - Check kedua (dalam synchronized): pastikan tidak ada race
     *   condition saat instance belum ada
     *
     * @return instance tunggal AuthService
     */
    public static AuthService getInstance() {
        // Check pertama — tanpa lock untuk performa
        if (instance == null) {
            // Synchronized block — hanya satu thread yang bisa masuk
            synchronized (AuthService.class) {
                // Check kedua — pastikan belum dibuat oleh thread lain
                if (instance == null) {
                    instance = new AuthService();
                }
            }
        }
        return instance;
    }

    /**
     * Memproses login dengan username dan password.
     *
     * Alur validasi:
     * 1. Cari User berdasarkan username
     * 2. Hash password input → bandingkan dengan hash tersimpan
     * 3. Jika cocok, set sesi aktif
     * 4. Jika User adalah CUSTOMER, load data Customer-nya juga
     *
     * Kita tidak pernah membandingkan password plaintext —
     * selalu hash dulu baru bandingkan. Ini standar keamanan dasar.
     *
     * @param username username input dari form login
     * @param password password plaintext input dari form login
     * @return true jika login berhasil, false jika gagal
     */
    public boolean login(String username, String password) {
        // Cari user berdasarkan username
        User user = userRepository.findByUsername(username);

        // User tidak ditemukan — return false tanpa info detail
        // (jangan beri tahu apakah username atau password yang salah
        //  — ini security best practice)
        if (user == null) return false;

        // Hash password input lalu bandingkan dengan hash tersimpan
        String inputHash = CsvUtil.hashPassword(password);
        if (!inputHash.equals(user.getPasswordHash())) return false;

        // Login berhasil — set sesi aktif
        this.currentUser = user;

        // Jika role CUSTOMER, load data Customer untuk ditampilkan di UI
        if (user.getCustomerId() != null && !user.getCustomerId().isEmpty()) {
            this.currentCustomer = customerRepository.findById(user.getCustomerId());
        }

        return true;
    }

    /**
     * Mengakhiri sesi login aktif.
     * Reset semua state ke null.
     */
    public void logout() {
        this.currentUser = null;
        this.currentCustomer = null;
    }

    /**
     * Mengecek apakah ada sesi login aktif.
     *
     * @return true jika user sedang login
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Mengecek apakah user yang sedang login adalah Admin.
     *
     * @return true jika role ADMIN
     */
    public boolean isAdmin() {
        if (currentUser == null) return false;
        return currentUser.getRole() ==
               banking.model.enums.UserRole.ADMIN;
    }

    // Getter untuk data sesi aktif
    // Tidak ada setter — sesi hanya bisa diubah via login() dan logout()

    /**
     * @return User yang sedang login, null jika belum login
     */
    public User getCurrentUser() { return currentUser; }

    /**
     * @return Customer yang sedang login, null jika Admin atau belum login
     */
    public Customer getCurrentCustomer() { return currentCustomer; }
}