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
import banking.util.LoanCalculator;
import banking.util.ValidationUtil;
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

        // Validation layer
        ValidationUtil.validateFullName(fullName);
        fullName = fullName.trim();
        ValidationUtil.validateFullName(fullName);
        ValidationUtil.validateEmail(email);
        ValidationUtil.validatePhone(phone);
        ValidationUtil.validateUsername(username);
        ValidationUtil.validatePassword(password);
        address   = ValidationUtil.sanitizeDescription(
                    address, "Alamat", 200);

        // Cek duplikasi username
        if (userRepository.findByUsername(username) != null)
            throw new IllegalArgumentException(
                "Username '" + username + "' sudah digunakan.");

        String customerId = CsvUtil.generateId("C",
                customerRepository.count() + 1);
        String today = LocalDate.now().format(DATE_FORMAT);

        Customer customer = new Customer(
                customerId, fullName.trim(), email.trim(),
                phone.trim(), address, today);
        customerRepository.save(customer);

        String userId       = CsvUtil.generateId("U",
                userRepository.count() + 1);
        String passwordHash = CsvUtil.hashPassword(password);

        User user = new User(userId, username.trim(),
                passwordHash, UserRole.CUSTOMER, customerId);
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
     // Validation
     ValidationUtil.validateDepositAmount(amount);

     Account account = getAccountOrThrow(accountId);
     account.deposit(amount);
     accountRepository.update(account);

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
    // Validation
        ValidationUtil.validateWithdrawalAmount(amount);

        Account account = getAccountOrThrow(accountId);
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

        // Validation
        ValidationUtil.validateTransferAmount(amount);
        ValidationUtil.validateDifferentAccounts(
                sourceAccountId, targetAccountId);

        Account source = getAccountOrThrow(sourceAccountId);
        Account target = getAccountOrThrow(targetAccountId);

        source.transfer(amount, target);
        accountRepository.update(source);
        accountRepository.update(target);

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
        int tenorMonths, String description,
        double annualRate, LoanType loanType) {

    // Validation
    Customer customer = customerRepository.findById(customerId);
    if (customer == null)
        throw new IllegalArgumentException(
            "Customer tidak ditemukan: " + customerId);

    ValidationUtil.validateLoanPrincipal(principal);
    ValidationUtil.validateLoanTenor(tenorMonths);
    ValidationUtil.validateInterestRate(annualRate);
    description = ValidationUtil.sanitizeDescription(
            description, "Keterangan pinjaman", 100);

    String loanId = CsvUtil.generateId("L",
            loanRepository.count() + 1);
    String today = LocalDate.now().format(DATE_FORMAT);

    // Hitung cicilan berdasarkan metode yang dipilih
    double monthlyPayment = LoanCalculator.calculateMonthly(
            loanType, principal, annualRate, tenorMonths);
    double totalPayment = LoanCalculator.calculateTotalPayment(
            loanType, principal, annualRate, tenorMonths);

    Loan loan = new Loan(
        loanId, customerId, principal,
        monthlyPayment, tenorMonths, 0,
        LoanStatus.PENDING,
        today, description,
        annualRate, loanType, totalPayment,
        "", "", ""
    );

    loanRepository.save(loan);
    return loan;
}
    
        /**
     * Approve pinjaman yang sedang PENDING.
     * Hanya Admin yang boleh memanggil method ini.
     *
     * @param loanId      ID pinjaman yang di-approve
     * @param approvedBy  username admin yang menyetujui
     * @throws IllegalArgumentException jika pinjaman tidak ditemukan
     *         atau statusnya bukan PENDING
     */
    public void approveLoan(String loanId, String approvedBy) {
        Loan loan = loanRepository.findById(loanId);
        if (loan == null)
            throw new IllegalArgumentException(
                "Pinjaman tidak ditemukan: " + loanId);
        if (loan.getStatus() != LoanStatus.PENDING)
            throw new IllegalArgumentException(
                "Hanya pinjaman berstatus PENDING yang bisa disetujui. "
                + "Status saat ini: " + loan.getStatus().getDisplayName());

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setApprovedBy(approvedBy);
        loan.setApprovedDate(LocalDate.now().format(DATE_FORMAT));
        loan.setRejectionReason("");

        loanRepository.update(loan);
    }

    /**
     * Reject pinjaman yang sedang PENDING.
     *
     * @param loanId          ID pinjaman yang ditolak
     * @param rejectedBy      username admin yang menolak
     * @param rejectionReason alasan penolakan (wajib diisi)
     */
    public void rejectLoan(String loanId, String rejectedBy,
            String rejectionReason) {

        ValidationUtil.validateRejectionReason(rejectionReason);

        Loan loan = loanRepository.findById(loanId);
        if (loan == null)
            throw new IllegalArgumentException(
                "Pinjaman tidak ditemukan: " + loanId);
        if (loan.getStatus() != LoanStatus.PENDING)
            throw new IllegalArgumentException(
                "Hanya pinjaman berstatus PENDING yang bisa ditolak. "
                + "Status saat ini: " + loan.getStatus().getDisplayName());

        loan.setStatus(LoanStatus.REJECTED);
        
        // Pinjaman ditolak, maka data Approval harus dikosongkan/diisi penanda ditolak
        loan.setApprovedBy(rejectedBy); 
        loan.setApprovedDate("-"); // Jangan diisi tanggal aktif agar tidak dikira pinjaman sukses oleh AlertService!
        loan.setRejectionReason(rejectionReason);

        loanRepository.update(loan);
    }

    /**
     * Override calculateMonthlyPayment lama — sekarang support
     * dua metode bunga.
     *
     * @param loanType    FLAT atau ANNUITY
     * @param principal   pokok pinjaman
     * @param annualRate  suku bunga tahunan dalam persen
     * @param tenorMonths tenor dalam bulan
     * @return cicilan bulanan
     */


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

    /**
     * Overload calculateMonthlyPayment baru — Mendukung metode bunga
     * FLAT atau ANNUITY dengan mendelegasikan ke LoanCalculator utility.
     * * Tambahkan method ini untuk memperbaiki error di LoanPanel dan NasabahPanel!
     */
    public double calculateMonthlyPayment(LoanType loanType, double principal, 
                                          double annualRate, int tenorMonths) {
        // Mendelegasikan kalkulasi secara aman ke kelas utility LoanCalculator
        return LoanCalculator.calculateMonthly(loanType, principal, annualRate, tenorMonths);
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
        
        // ── [FIX SOLUSI STRUK GANDA] ──
        // Hanya cetak struk otomatis jika tipe transaksi BUKAN dana masuk (TRANSFER_IN)
        if (type != TransactionType.TRANSFER_IN) {
            banking.util.ReceiptService.generateAndOpenReceipt(tx);
        }
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
