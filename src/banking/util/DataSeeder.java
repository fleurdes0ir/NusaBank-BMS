/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.util;

import banking.model.*;
import banking.model.enums.*;
import banking.model.enums.LoanType;
import banking.repository.*;
import java.io.File;

/**
 * DataSeeder mengisi data awal (seed data) saat aplikasi pertama kali
 * dijalankan — ketika file CSV belum ada.
 *
 * Tujuannya: 1. Memastikan aplikasi langsung bisa dipakai tanpa input manual 2.
 * Menyediakan akun demo untuk testing (admin, nasabah) 3. Mengisi data contoh
 * yang realistis untuk demonstrasi
 *
 * Cara kerja: cek apakah file users.csv sudah ada. - Belum ada → jalankan
 * seeding - Sudah ada → skip (tidak menimpa data yang sudah ada)
 */
public class DataSeeder {

    // Semua repository dibutuhkan untuk menyimpan seed data
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;

    public DataSeeder() {
        this.customerRepository = new CustomerRepository();
        this.userRepository = new UserRepository();
        this.accountRepository = new AccountRepository();
        this.transactionRepository = new TransactionRepository();
        this.loanRepository = new LoanRepository();
    }

    /**
     * Entry point seeding — dipanggil dari Main.java saat startup.
     *
     * Guard clause: cek file users.csv sebagai indikator apakah seeding sudah
     * pernah dilakukan. Jika sudah ada, skip seluruhnya.
     */
    public void seed() {
        // Jika data sudah ada, tidak perlu seed ulang
        if (new File(AppConfig.USERS_FILE).exists()) {
            System.out.println("Data sudah ada, skip seeding.");
            return;
        }

        System.out.println("Menginisialisasi data awal...");

        // Buat folder data/ jika belum ada
        new File(AppConfig.DATA_DIR).mkdirs();

        seedUsers();
        seedCustomers();
        seedAccounts();
        seedTransactions();
        seedLoans();

        System.out.println("Seeding selesai.");
    }

    /**
     * Seed data User — akun login untuk semua role.
     *
     * Password di-hash menggunakan SHA-256 sebelum disimpan. Password plaintext
     * hanya ada di sini (kode sumber) — tidak pernah tersimpan di CSV.
     */
    private void seedUsers() {
        // Admin account — tidak terhubung ke Customer manapun
        userRepository.save(new User(
                "U001",
                "admin",
                CsvUtil.hashPassword("admin123"),
                UserRole.ADMIN,
                "" // Admin tidak punya customerId
        ));

        // Nasabah accounts
        userRepository.save(new User(
                "U002",
                "budi.santoso",
                CsvUtil.hashPassword("nasabah123"),
                UserRole.CUSTOMER,
                "C001"
        ));

        userRepository.save(new User(
                "U003",
                "sari.dewi",
                CsvUtil.hashPassword("nasabah123"),
                UserRole.CUSTOMER,
                "C002"
        ));

        userRepository.save(new User(
                "U004",
                "agus.setiawan",
                CsvUtil.hashPassword("nasabah123"),
                UserRole.CUSTOMER,
                "C003"
        ));

        System.out.println("✓ Users seeded");
    }

    /**
     * Seed data Customer — data personal nasabah.
     */
    private void seedCustomers() {
        customerRepository.save(new Customer(
                "C001",
                "Budi Santoso",
                "budi.santoso@email.com",
                "081234567890",
                "Jl. Sudirman No. 12, Jakarta",
                "2024-01-15"
        ));

        customerRepository.save(new Customer(
                "C002",
                "Sari Dewi",
                "sari.dewi@email.com",
                "081234567891",
                "Jl. Thamrin No. 5, Jakarta",
                "2024-02-20"
        ));

        customerRepository.save(new Customer(
                "C003",
                "Agus Setiawan",
                "agus.setiawan@email.com",
                "081234567892",
                "Jl. Gatot Subroto No. 8, Jakarta",
                "2024-03-10"
        ));

        System.out.println("✓ Customers seeded");
    }

    /**
     * Seed data Account — rekening dengan tipe bervariasi.
     *
     * Distribusi rekening sesuai design reference: - Tabungan : 3 rekening -
     * Giro : 1 rekening - Deposito : 1 rekening
     */
    private void seedAccounts() {
        // Budi — Tabungan + Deposito
        accountRepository.save(new SavingsAccount(
                "A001", "C001", 15_000_000, "2024-01-15"
        ));

        accountRepository.save(new DepositAccount(
                "A002", "C001", 50_000_000, "2024-01-15",
                12, "2026-01-15", false
        ));

        // Sari — Tabungan + Giro
        accountRepository.save(new SavingsAccount(
                "A003", "C002", 8_500_000, "2024-02-20"
        ));

        accountRepository.save(new CurrentAccount(
                "A004", "C002", 25_000_000, "2024-02-20"
        ));

        // Agus — Tabungan
        accountRepository.save(new SavingsAccount(
                "A005", "C003", 12_000_000, "2024-03-10"
        ));

        System.out.println("✓ Accounts seeded");
    }

    /**
     * Seed data Transaction — riwayat transaksi contoh.
     *
     * Data ini akan ditampilkan di widget "Transaksi Terbaru" di dashboard
     * Admin dan panel Nasabah.
     */
    private void seedTransactions() {
        // Setoran awal semua rekening
        transactionRepository.save(new Transaction(
                "TRX001", "A001", TransactionType.DEPOSIT,
                15_000_000, "2024-01-15", "Setoran awal pembukaan rekening", ""
        ));

        transactionRepository.save(new Transaction(
                "TRX002", "A002", TransactionType.DEPOSIT,
                50_000_000, "2024-01-15", "Buka deposito", ""
        ));

        transactionRepository.save(new Transaction(
                "TRX003", "A003", TransactionType.DEPOSIT,
                8_500_000, "2024-02-20", "Setoran awal pembukaan rekening", ""
        ));

        transactionRepository.save(new Transaction(
                "TRX004", "A004", TransactionType.DEPOSIT,
                25_000_000, "2024-02-20", "Setoran awal pembukaan rekening", ""
        ));

        transactionRepository.save(new Transaction(
                "TRX005", "A005", TransactionType.DEPOSIT,
                12_000_000, "2024-03-10", "Setoran awal pembukaan rekening", ""
        ));

        // Transaksi operasional
        transactionRepository.save(new Transaction(
                "TRX006", "A001", TransactionType.WITHDRAWAL,
                500_000, "2024-02-01", "Tarik tunai", ""
        ));

        transactionRepository.save(new Transaction(
                "TRX007", "A001", TransactionType.TRANSFER_OUT,
                2_000_000, "2024-02-15", "Transfer ke A003", "A003"
        ));

        transactionRepository.save(new Transaction(
                "TRX008", "A003", TransactionType.TRANSFER_IN,
                2_000_000, "2024-02-15", "Transfer dari A001", "A001"
        ));

        transactionRepository.save(new Transaction(
                "TRX009", "A003", TransactionType.DEPOSIT,
                8_000_000, "2024-02-20", "Setoran awal - Transfer dana", ""
        ));

        System.out.println("✓ Transactions seeded");
    }

    /**
     * Seed data Loan — pinjaman aktif contoh.
     *
     * Data ini ditampilkan di widget "Status Pinjaman" di dashboard Admin dan
     * panel Nasabah.
     */
    private void seedLoans() {
        // Budi — pinjaman personal 24 bulan
        // monthlyPayment = 50_000_000 / 24 = 2_083_333
        // Pinjaman pertama — Budi Santoso
        loanRepository.save(new Loan(
            "L001", "C001", 50_000_000,
            2_083_333, 24, 8,
            LoanStatus.ACTIVE, "2024-01-15",
            "Pinjaman Personal",
            12.0, LoanType.FLAT,
            50_000_000 + (50_000_000 * 0.12 * 2),
            "admin", "2024-01-15", ""
        ));

        // Pinjaman kedua — Agus Setiawan
        loanRepository.save(new Loan(
            "L002", "C003", 20_000_000,
            1_666_667, 12, 3,
            LoanStatus.ACTIVE, "2024-03-10",
            "Modal Usaha",
            12.0, LoanType.FLAT,
            20_000_000 + (20_000_000 * 0.12 * 1),
            "admin", "2024-03-10", ""
        ));

        System.out.println("✓ Loans seeded");
    }
}
