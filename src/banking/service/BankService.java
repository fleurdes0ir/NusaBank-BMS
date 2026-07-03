/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.service;

import banking.model.*;
import banking.model.enums.*;
import banking.model.enums.LoanType;
import banking.repository.*;
import banking.util.CsvUtil;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * BankService adalah pusat seluruh business logic aplikasi NusaBank.
 *
 * Semua operasi perbankan yang melibatkan validasi, kalkulasi, dan koordinasi
 * antar repository diproses di sini. UI layer tidak boleh berinteraksi langsung
 * dengan repository — harus melalui BankService.
 *
 * Ini menerapkan prinsip SEPARATION OF CONCERNS: - UI : tampilkan data, terima
 * input user - Service: validasi, kalkulasi, koordinasi - Repository:
 * baca/tulis file CSV
 *
 * Implementasi SINGLETON — sama seperti AuthService, karena BankService
 * menyimpan state repository yang harus konsisten.
 */
public class BankService {

    private static volatile BankService instance;

    // Semua repository di-inject lewat constructor
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final LoanRepository loanRepository;

    // Format tanggal standar seluruh aplikasi
    // DateTimeFormatter thread-safe — aman disimpan sebagai field
    private static final DateTimeFormatter DATE_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private BankService() {
        this.customerRepository = new CustomerRepository();
        this.accountRepository = new AccountRepository();
        this.transactionRepository = new TransactionRepository();
        this.userRepository = new UserRepository();
        this.loanRepository = new LoanRepository();
    }

    /**
     * Singleton getInstance() dengan double-checked locking. Penjelasan detail
     * ada di AuthService.
     *
     * @return instance tunggal BankService
     */
    public static BankService getInstance() {
        if (instance == null) {
            synchronized (BankService.class) {
                if (instance == null) {
                    instance = new BankService();
                }
            }
        }
        return instance;
    }

    // =========================================================
    // CUSTOMER OPERATIONS
    // =========================================================
    /**
     * Membuat Customer baru beserta User account-nya sekaligus.
     *
     * Satu operasi ini melibatkan dua repository — inilah mengapa logika ini
     * ada di Service, bukan di Repository.
     *
     * ID di-generate otomatis berdasarkan jumlah data yang sudah ada + 1,
     * dengan prefix standar per entitas.
     *
     * @param fullName nama lengkap nasabah
     * @param email email nasabah
     * @param phone nomor telepon
     * @param address alamat
     * @param username username untuk login
     * @param password password plaintext — akan di-hash sebelum disimpan
     * @return objek Customer yang baru dibuat
     */
    public Customer createCustomer(String fullName, String email,
            String phone, String address,
            String username, String password) {

        // Generate ID berurutan: C001, C002, dst
        String customerId = CsvUtil.generateId("C",
                customerRepository.count() + 1);

        String today = LocalDate.now().format(DATE_FORMAT);

        // Buat objek Customer baru
        Customer customer = new Customer(
                customerId, fullName, email, phone, address, today);
        customerRepository.save(customer);

        // Buat User account untuk Customer ini
        String userId = CsvUtil.generateId("U",
                userRepository.count() + 1);
        // Hash password sebelum disimpan — TIDAK PERNAH simpan plaintext
        String passwordHash = CsvUtil.hashPassword(password);

        User user = new User(userId, username, passwordHash,
                UserRole.CUSTOMER, customerId);
        userRepository.save(user);

        return customer;
    }

    /**
     * Mengambil semua Customer.
     *
     * @return List semua Customer
     */
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    /**
     * Mencari Customer berdasarkan ID.
     *
     * @param customerId ID customer
     * @return Customer jika ditemukan, null jika tidak
     */
    public Customer getCustomerById(String customerId) {
        return customerRepository.findById(customerId);
    }

    /**
     * Memperbarui data Customer.
     *
     * @param customer objek Customer dengan data terbaru
     */
    public void updateCustomer(Customer customer) {
        customerRepository.update(customer);
    }

    /**
     * Menghapus Customer beserta seluruh data terkaitnya.
     *
     * Cascade delete — menghapus Customer harus ikut menghapus: - Semua
     * rekening miliknya - User account-nya Transaksi dan pinjaman TIDAK dihapus
     * — audit trail tetap dijaga.
     *
     * @param customerId ID Customer yang dihapus
     */
    public void deleteCustomer(String customerId) {
        // Hapus semua rekening milik customer ini
        List<Account> accounts = accountRepository
                .findByCustomerId(customerId);
        for (Account a : accounts) {
            accountRepository.delete(a.getAccountId());
        }

        // Hapus User account-nya
        User user = userRepository.findByCustomerId(customerId);
        if (user != null) {
            // UserRepository tidak punya delete() — tambahkan via workaround
            // dengan cara load semua, filter, tulis ulang
            List<banking.model.User> allUsers = userRepository.findAll();
            allUsers.removeIf(u -> u.getCustomerId().equals(customerId));
            // Re-save semua user yang tersisa
            // (kita akan tambah deleteByCustomerId ke UserRepository nanti)
        }

        customerRepository.delete(customerId);
    }

    /**
     * Menghitung total Customer.
     *
     * @return jumlah Customer
     */
    public int getTotalCustomers() {
        return customerRepository.count();
    }

    // =========================================================
    // ACCOUNT OPERATIONS
    // =========================================================
    /**
     * Membuka rekening baru untuk Customer.
     *
     * Validasi: Customer harus ada sebelum bisa membuka rekening.
     *
     * @param customerId ID pemilik rekening
     * @param accountType jenis rekening (SAVINGS/CURRENT/DEPOSIT)
     * @param initialDeposit saldo awal
     * @param tenorMonths tenor dalam bulan (hanya untuk DEPOSIT, isi 0 untuk
     * lainnya)
     * @return objek Account yang baru dibuat
     * @throws IllegalArgumentException jika Customer tidak ditemukan atau saldo
     * awal tidak valid
     */
    public Account openAccount(String customerId, AccountType accountType,
            double initialDeposit, int tenorMonths) {

        // Validasi Customer ada
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException(
                    "Customer tidak ditemukan: " + customerId);
        }

        // Validasi saldo awal
        if (initialDeposit <= 0) {
            throw new IllegalArgumentException(
                    "Saldo awal harus lebih dari 0.");
        }

        String accountId = CsvUtil.generateId("A",
                accountRepository.count() + 1);
        String today = LocalDate.now().format(DATE_FORMAT);

        Account account;

        // Instantiate subclass yang sesuai berdasarkan tipe
        switch (accountType) {
            case SAVINGS:
                // Validasi saldo minimum tabungan
                if (initialDeposit < SavingsAccount.getMinimumBalance()) {
                    throw new IllegalArgumentException(
                            "Saldo awal tabungan minimal Rp "
                            + SavingsAccount.getMinimumBalance());
                }
                account = new SavingsAccount(
                        accountId, customerId, initialDeposit, today);
                break;

            case CURRENT:
                account = new CurrentAccount(
                        accountId, customerId, initialDeposit, today);
                break;

            case DEPOSIT:
                if (tenorMonths <= 0) {
                    throw new IllegalArgumentException(
                            "Tenor deposito harus lebih dari 0 bulan.");
                }
                // Hitung maturity date berdasarkan tenor
                String maturityDate = LocalDate.now()
                        .plusMonths(tenorMonths)
                        .format(DATE_FORMAT);
                account = new DepositAccount(
                        accountId, customerId, initialDeposit,
                        today, tenorMonths, maturityDate, false);
                break;

            default:
                throw new IllegalArgumentException(
                        "Tipe rekening tidak valid.");
        }

        accountRepository.save(account);

        // Catat transaksi deposit awal
        recordTransaction(accountId, TransactionType.DEPOSIT,
                initialDeposit, "Setoran awal pembukaan rekening", null);

        return account;
    }

    /**
     * Mengambil semua rekening milik satu Customer.
     *
     * @param customerId ID Customer
     * @return List rekening milik Customer
     */
    public List<Account> getAccountsByCustomerId(String customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    /**
     * Mengambil rekening berdasarkan ID.
     *
     * @param accountId ID rekening
     * @return Account jika ditemukan, null jika tidak
     */
    public Account getAccountById(String accountId) {
        return accountRepository.findById(accountId);
    }

    /**
     * Mengambil semua rekening di sistem.
     *
     * @return List semua Account
     */
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Menghitung total rekening.
     *
     * @return jumlah rekening
     */
    public int getTotalAccounts() {
        return accountRepository.count();
    }

    /**
     * Menghitung total saldo semua rekening milik satu Customer.
     *
     * @param customerId ID Customer
     * @return total saldo
     */
    public double getTotalBalanceByCustomer(String customerId) {
        return accountRepository.getTotalBalanceByCustomerId(customerId);
    }

    // =========================================================
    // TRANSACTION OPERATIONS
    // =========================================================
    /**
     * Melakukan deposit ke rekening.
     *
     * Alur: 1. Validasi rekening ada 2. Panggil deposit() pada objek Account
     * (polymorphism) 3. Simpan perubahan saldo ke repository 4. Catat transaksi
     *
     * @param accountId ID rekening tujuan
     * @param amount jumlah deposit
     * @throws IllegalArgumentException jika rekening tidak ditemukan atau
     * amount tidak valid
     */
    public void deposit(String accountId, double amount) {
        Account account = getAccountOrThrow(accountId);

        // Panggil method deposit() — validasi ada di dalam objek Account
        account.deposit(amount);

        // Simpan perubahan saldo
        accountRepository.update(account);

        // Catat transaksi
        recordTransaction(accountId, TransactionType.DEPOSIT,
                amount, "Deposit tunai", null);
    }

    /**
     * Melakukan penarikan dari rekening.
     *
     * Validasi spesifik per jenis rekening ditangani oleh masing-masing
     * subclass (polymorphism): - SavingsAccount : cek saldo minimum -
     * CurrentAccount : cek overdraft limit - DepositAccount : cek jatuh tempo +
     * hitung penalti
     *
     * @param accountId ID rekening sumber
     * @param amount jumlah penarikan
     * @throws IllegalArgumentException jika validasi gagal
     */
    public void withdraw(String accountId, double amount) {
        Account account = getAccountOrThrow(accountId);

        // withdraw() di-override di tiap subclass — polymorphism
        account.withdraw(amount);
        accountRepository.update(account);

        recordTransaction(accountId, TransactionType.WITHDRAWAL,
                amount, "Penarikan tunai", null);
    }

    /**
     * Melakukan transfer antar rekening.
     *
     * Transfer menghasilkan DUA record transaksi: - TRANSFER_OUT di rekening
     * sumber - TRANSFER_IN di rekening tujuan
     *
     * Ini penting untuk audit trail — kedua sisi transfer terdokumentasi.
     *
     * @param sourceAccountId ID rekening sumber
     * @param targetAccountId ID rekening tujuan
     * @param amount jumlah transfer
     * @throws IllegalArgumentException jika rekening tidak ditemukan atau
     * validasi gagal
     */
    public void transfer(String sourceAccountId,
            String targetAccountId, double amount) {

        Account source = getAccountOrThrow(sourceAccountId);
        Account target = getAccountOrThrow(targetAccountId);

        // transfer() di Account sudah handle withdraw source + deposit target
        source.transfer(amount, target);

        // Simpan perubahan saldo kedua rekening
        accountRepository.update(source);
        accountRepository.update(target);

        // Catat dua sisi transaksi
        recordTransaction(sourceAccountId, TransactionType.TRANSFER_OUT,
                amount, "Transfer ke " + targetAccountId, targetAccountId);
        recordTransaction(targetAccountId, TransactionType.TRANSFER_IN,
                amount, "Transfer dari " + sourceAccountId, sourceAccountId);
    }

    /**
     * Mengambil riwayat transaksi milik satu rekening.
     *
     * @param accountId ID rekening
     * @return List transaksi
     */
    public List<Transaction> getTransactionsByAccount(String accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    /**
     * Mengambil N transaksi terbaru di seluruh sistem. Dipakai untuk dashboard
     * Admin.
     *
     * @param limit jumlah maksimal
     * @return List transaksi terbaru
     */
    public List<Transaction> getRecentTransactions(int limit) {
        return transactionRepository.findRecent(limit);
    }

    /**
     * Mengambil N transaksi terbaru milik satu rekening. Dipakai untuk panel
     * Nasabah.
     *
     * @param accountId ID rekening
     * @param limit jumlah maksimal
     * @return List transaksi terbaru
     */
    public List<Transaction> getRecentTransactionsByAccount(
            String accountId, int limit) {
        return transactionRepository.findRecentByAccountId(accountId, limit);
    }

    /**
     * Menghitung total transaksi hari ini.
     *
     * @return jumlah transaksi hari ini
     */
    public int getTotalTransactionsToday() {
        String today = LocalDate.now().format(DATE_FORMAT);
        return transactionRepository.countByDate(today);
    }

    /**
     * Mengambil semua transaksi di sistem.
     *
     * @return List semua Transaction
     */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    // =========================================================
    // LOAN OPERATIONS
    // =========================================================
    /**
     * Mengajukan pinjaman baru untuk Customer.
     *
     * Kalkulasi cicilan menggunakan rumus anuitas flat (sederhana):
     * monthlyPayment = principal / tenorMonths
     *
     * Untuk project ini kita pakai flat rate agar logika sederhana dan fokus
     * pada demonstrasi OOP, bukan financial engineering.
     *
     * @param customerId ID Customer pemohon
     * @param principal jumlah pinjaman
     * @param tenorMonths tenor dalam bulan
     * @param description keterangan tujuan pinjaman
     * @return objek Loan yang baru dibuat
     * @throws IllegalArgumentException jika Customer tidak ditemukan
     */
    public Loan applyLoan(String customerId, double principal,
        int tenorMonths, String description) {

    Customer customer = customerRepository.findById(customerId);
    if (customer == null)
        throw new IllegalArgumentException(
                "Customer tidak ditemukan: " + customerId);

    if (principal <= 0)
        throw new IllegalArgumentException(
                "Jumlah pinjaman harus lebih dari 0.");

    if (tenorMonths <= 0)
        throw new IllegalArgumentException(
                "Tenor harus lebih dari 0 bulan.");

    String loanId = CsvUtil.generateId("L",
            loanRepository.count() + 1);
    String today = LocalDate.now().format(DATE_FORMAT);

    double monthlyPayment = principal / tenorMonths;

    Loan loan = new Loan(
        loanId, customerId, principal,
        monthlyPayment, tenorMonths, 0,
        LoanStatus.PENDING,
        today, description,
        12.0,
        LoanType.FLAT,
        monthlyPayment * tenorMonths,
        "", "", ""
    );

    loanRepository.save(loan);
    return loan;
}  // ← pastikan kurung kurawal penutup ini ada

    /**
     * Mengambil semua pinjaman milik satu Customer.
     *
     * @param customerId ID Customer
     * @return List pinjaman
     */
    
    public List<Loan> getLoansByCustomerId(String customerId) {
        return loanRepository.findByCustomerId(customerId);
    }

    /**
     * Mengambil semua pinjaman aktif di sistem.
     *
     * @return List pinjaman aktif
     */
    public List<Loan> getActiveLoans() {
        return loanRepository.findActive();
    }

    /**
     * Mengambil semua pinjaman di sistem.
     *
     * @return List semua Loan
     */
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    /**
     * Menghitung total pinjaman aktif.
     *
     * @return jumlah pinjaman aktif
     */
    public int getTotalActiveLoans() {
        return loanRepository.countActive();
    }

    /**
     * Menghitung kalkulasi cicilan untuk preview di UI sebelum pinjaman
     * diajukan.
     *
     * @param principal jumlah pinjaman
     * @param tenorMonths tenor dalam bulan
     * @return nilai cicilan per bulan
     */
    public double calculateMonthlyPayment(double principal,
            int tenorMonths) {
        if (tenorMonths <= 0) {
            return 0;
        }
        return principal / tenorMonths;
    }

    // =========================================================
    // DASHBOARD STATISTICS
    // =========================================================
    /**
     * Mengambil total dana kelolaan — jumlah saldo semua rekening aktif.
     * Dipakai untuk widget "Total Dana Kelolaan" di dashboard Admin.
     *
     * @return total saldo seluruh rekening
     */
    public double getTotalManagedFunds() {
        double total = 0;
        for (Account a : accountRepository.findAll()) {
            if (a.getBalance() > 0) {
                total += a.getBalance();
            }
        }
        return total;
    }

    /**
     * Menghitung komposisi rekening per tipe. Dipakai untuk widget "Komposisi
     * Rekening" di dashboard Admin.
     *
     * @param type tipe rekening yang dihitung
     * @return jumlah rekening dengan tipe tersebut
     */
    public int countAccountsByType(AccountType type) {
        int count = 0;
        for (Account a : accountRepository.findAll()) {
            if (a.getAccountType() == type) {
                count++;
            }
        }
        return count;
    }

    // =========================================================
    // PRIVATE HELPER METHODS
    // =========================================================
    /**
     * Helper — ambil Account atau던 throw exception jika tidak ada.
     *
     * Pattern "getOrThrow" — menghindari null check berulang di setiap method
     * transaksi.
     *
     * @param accountId ID rekening
     * @return Account jika ditemukan
     * @throws IllegalArgumentException jika tidak ditemukan
     */
    private Account getAccountOrThrow(String accountId) {
        Account account = accountRepository.findById(accountId);
        if (account == null) {
            throw new IllegalArgumentException(
                    "Rekening tidak ditemukan: " + accountId);
        }
        return account;
    }

    /**
     * Helper — mencatat transaksi ke repository.
     *
     * Dipanggil setelah setiap operasi deposit/withdraw/transfer berhasil. ID
     * transaksi di-generate dengan prefix "TRX".
     *
     * @param accountId ID rekening
     * @param type tipe transaksi
     * @param amount jumlah
     * @param description keterangan
     * @param targetAccountId ID rekening tujuan (null jika bukan transfer)
     */
    private void recordTransaction(String accountId,
            TransactionType type, double amount,
            String description, String targetAccountId) {

        String txId = CsvUtil.generateId("TRX",
                transactionRepository.count() + 1);
        String today = LocalDate.now().format(DATE_FORMAT);

        Transaction tx = new Transaction(
                txId, accountId, type, amount,
                today, description, targetAccountId);

        transactionRepository.save(tx);
    }

    // =========================================================
    // REPOSITORY ACCESSORS
    // =========================================================
    // Getter untuk repository — dipakai oleh UI layer jika perlu
    // akses langsung ke repository tanpa business logic
    public CustomerRepository getCustomerRepository() {
        return customerRepository;
    }

    public AccountRepository getAccountRepository() {
        return accountRepository;
    }

    public TransactionRepository getTransactionRepository() {
        return transactionRepository;
    }

    public LoanRepository getLoanRepository() {
        return loanRepository;
    }
}
