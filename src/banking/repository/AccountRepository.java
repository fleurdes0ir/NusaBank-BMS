/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.repository;

import banking.model.Account;
import banking.model.CurrentAccount;
import banking.model.DepositAccount;
import banking.model.SavingsAccount;
import banking.model.enums.AccountType;
import banking.util.AppConfig;
import banking.util.CsvUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * AccountRepository menangani persistensi semua jenis rekening.
 *
 * Tantangan utama di sini: kita menyimpan tiga jenis objek berbeda
 * (SavingsAccount, CurrentAccount, DepositAccount) ke SATU file CSV.
 *
 * Solusinya: kolom accountType di CSV dipakai sebagai discriminator —
 * saat membaca, kita cek tipenya dulu, baru instantiate subclass yang tepat.
 *
 * Ini adalah aplikasi nyata dari POLYMORPHISM — kita bekerja dengan
 * tipe Account (parent), tapi objek aktualnya adalah subclass spesifik.
 *
 * Format CSV Tabungan/Giro : accountId,customerId,balance,type,openDate
 * Format CSV Deposito      : accountId,customerId,balance,type,openDate,tenor,maturityDate,isMature
 */
public class AccountRepository {

    /**
     * Mengambil semua rekening dari CSV.
     *
     * Proses parsing berbeda per tipe rekening:
     * - SAVINGS dan CURRENT : 5 kolom
     * - DEPOSIT             : 8 kolom (ada tenor, maturityDate, isMature tambahan)
     *
     * @return List semua Account (campuran tiga subclass)
     */
    public List<Account> findAll() {
        List<Account> accounts = new ArrayList<>();
        List<String[]> rows = CsvUtil.readAll(AppConfig.ACCOUNTS_FILE);

        for (String[] row : rows) {
            // Minimal 5 kolom untuk semua tipe rekening
            if (row.length < 5) continue;

            try {
                String accountId  = row[0].trim();
                String customerId = row[1].trim();
                // parseDouble untuk konversi String ke angka desimal
                double balance    = Double.parseDouble(row[2].trim());
                // valueOf() konversi String ke enum AccountType
                AccountType type  = AccountType.valueOf(row[3].trim());
                String openDate   = row[4].trim();

                // Switch expression berdasarkan tipe — instantiate subclass yang tepat
                // Ini adalah polymorphism in action: variabel bertipe Account,
                // tapi objek aktual adalah SavingsAccount/CurrentAccount/DepositAccount
                switch (type) {
                    case SAVINGS:
                        accounts.add(new SavingsAccount(
                            accountId, customerId, balance, openDate));
                        break;

                    case CURRENT:
                        accounts.add(new CurrentAccount(
                            accountId, customerId, balance, openDate));
                        break;

                    case DEPOSIT:
                        // DepositAccount butuh kolom tambahan — validasi dulu
                        if (row.length < 8) continue;
                        int tenorMonths      = Integer.parseInt(row[5].trim());
                        String maturityDate  = row[6].trim();
                        // parseBoolean: "true" → true, apapun selain itu → false
                        boolean isMature     = Boolean.parseBoolean(row[7].trim());
                        accounts.add(new DepositAccount(
                            accountId, customerId, balance, openDate,
                            tenorMonths, maturityDate, isMature));
                        break;

                    default:
                        System.err.println("Unknown account type: " + type);
                }
            } catch (IllegalArgumentException e) {
                // Skip baris corrupt daripada crash seluruh aplikasi
                System.err.println("Error parsing account row: " + e.getMessage());
            }
        }
        return accounts;
    }

    /**
     * Mencari rekening berdasarkan accountId.
     *
     * @param accountId ID rekening yang dicari
     * @return Account jika ditemukan, null jika tidak
     */
    public Account findById(String accountId) {
        return findAll().stream()
                .filter(a -> a.getAccountId().equals(accountId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Mengambil semua rekening milik satu Customer.
     *
     * Stream filter() menyaring hanya rekening dengan customerId yang cocok.
     * Hasilnya dikumpulkan ke List via collect(Collectors.toList()).
     *
     * @param customerId ID customer pemilik rekening
     * @return List rekening milik customer tersebut
     */
    public List<Account> findByCustomerId(String customerId) {
        // Menggunakan enhanced for loop untuk kejelasan
        List<Account> result = new ArrayList<>();
        for (Account a : findAll()) {
            if (a.getCustomerId().equals(customerId)) {
                result.add(a);
            }
        }
        return result;
    }

    /**
     * Menghitung total saldo semua rekening milik satu Customer.
     *
     * @param customerId ID customer
     * @return total saldo dalam double
     */
    public double getTotalBalanceByCustomerId(String customerId) {
        double total = 0;
        for (Account a : findByCustomerId(customerId)) {
            total += a.getBalance();
        }
        return total;
    }

    /**
     * Menyimpan rekening baru ke CSV.
     *
     * @param account objek Account yang akan disimpan
     */
    public void save(Account account) {
        List<Account> accounts = findAll();
        accounts.add(account);
        writeToFile(accounts);
    }

    /**
     * Memperbarui data rekening — dipanggil setelah transaksi
     * mengubah saldo rekening.
     *
     * @param updated objek Account dengan data terbaru
     */
    public void update(Account updated) {
        List<Account> accounts = findAll();
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).getAccountId().equals(updated.getAccountId())) {
                accounts.set(i, updated);
                break;
            }
        }
        writeToFile(accounts);
    }

    /**
     * Menghapus rekening berdasarkan ID.
     *
     * @param accountId ID rekening yang dihapus
     */
    public void delete(String accountId) {
        List<Account> accounts = findAll();
        accounts.removeIf(a -> a.getAccountId().equals(accountId));
        writeToFile(accounts);
    }

    /**
     * Menghitung total rekening yang tersimpan.
     *
     * @return jumlah rekening
     */
    public int count() {
        return findAll().size();
    }

    /**
     * Helper private — konversi List<Account> ke List<String> CSV.
     *
     * Memanggil toString() pada tiap Account — karena toString() di
     * DepositAccount di-override untuk menyertakan kolom tambahan,
     * polymorphism memastikan method yang tepat dipanggil otomatis.
     *
     * @param accounts List Account yang akan ditulis
     */
    private void writeToFile(List<Account> accounts) {
        List<String> lines = new ArrayList<>();
        for (Account a : accounts) {
            // Polymorphism: toString() yang dipanggil adalah milik subclass
            // DepositAccount.toString() → 8 kolom
            // SavingsAccount.toString() → 5 kolom (inherit dari Account)
            lines.add(a.toString());
        }
        CsvUtil.writeAll(AppConfig.ACCOUNTS_FILE, lines);
    }
}