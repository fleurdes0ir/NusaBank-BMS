/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.util;

import java.io.File;

/**
 * AppConfig adalah kelas konfigurasi terpusat untuk seluruh aplikasi.
 *
 * Tujuannya: semua path file CSV didefinisikan di SATU tempat.
 * Jika suatu saat path berubah, cukup ubah di sini — tidak perlu
 * hunting ke seluruh kodebase.
 *
 * Semua field bersifat:
 * - public  : bisa diakses dari mana saja
 * - static  : tidak perlu membuat objek AppConfig
 * - final   : nilai tidak bisa diubah setelah diinisialisasi (konstanta)
 *
 * Kombinasi ketiganya = CONSTANT, konvensi nama pakai UPPER_SNAKE_CASE.
 */
public class AppConfig {

    // File.separator otomatis menyesuaikan OS:
    // Linux/Mac → "/"   menjadi "data/"
    // Windows   → "\"  menjadi "data\"
    public static final String DATA_DIR = "data" + File.separator;

    // Path lengkap ke masing-masing file CSV
    public static final String CUSTOMERS_FILE    = DATA_DIR + "customers.csv";
    public static final String ACCOUNTS_FILE     = DATA_DIR + "accounts.csv";
    public static final String TRANSACTIONS_FILE = DATA_DIR + "transactions.csv";
    public static final String USERS_FILE        = DATA_DIR + "users.csv";
    public static final String LOANS_FILE        = DATA_DIR + "loans.csv";

    // Private constructor mencegah: AppConfig cfg = new AppConfig()
    // Kelas ini murni container konstanta, tidak perlu di-instantiate
    private AppConfig() {}
}