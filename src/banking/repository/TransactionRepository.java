/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.repository;

import banking.model.Transaction;
import banking.model.enums.TransactionType;
import banking.util.AppConfig;
import banking.util.CsvUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * TransactionRepository menangani persistensi data transaksi.
 *
 * Berbeda dari repository lain — Transaction bersifat IMMUTABLE
 * (semua field final, tidak ada setter). Artinya:
 * - Tidak ada method update() — transaksi yang sudah terjadi tidak bisa diubah
 * - Tidak ada method delete() — audit trail harus dijaga integritas-nya
 * - Hanya ada save() dan berbagai macam find()
 *
 * Ini mencerminkan aturan bisnis perbankan nyata: riwayat transaksi
 * adalah rekaman permanen yang tidak boleh dimanipulasi.
 *
 * Format CSV: transactionId,accountId,type,amount,date,description,targetAccountId
 */
public class TransactionRepository {

    /**
     * Mengambil semua transaksi dari CSV.
     *
     * targetAccountId bisa kosong (untuk DEPOSIT dan WITHDRAWAL),
     * hanya terisi untuk TRANSFER_IN dan TRANSFER_OUT.
     *
     * @return List semua Transaction
     */
    public List<Transaction> findAll() {
        List<Transaction> transactions = new ArrayList<>();
        List<String[]> rows = CsvUtil.readAll(AppConfig.TRANSACTIONS_FILE);

        for (String[] row : rows) {
            // Minimal 7 kolom — termasuk targetAccountId yang bisa kosong
            if (row.length < 7) continue;

            try {
                String transactionId    = row[0].trim();
                String accountId        = row[1].trim();
                // valueOf() konversi String ke enum TransactionType
                TransactionType type    = TransactionType.valueOf(row[2].trim());
                double amount           = Double.parseDouble(row[3].trim());
                String date             = row[4].trim();
                String description      = row[5].trim();
                // targetAccountId boleh kosong — pakai null jika string kosong
                String targetAccountId  = row[6].trim().isEmpty()
                                          ? null : row[6].trim();

                transactions.add(new Transaction(
                    transactionId, accountId, type, amount,
                    date, description, targetAccountId
                ));
            } catch (IllegalArgumentException e) {
                System.err.println("Error parsing transaction row: "
                        + e.getMessage());
            }
        }
        return transactions;
    }

    /**
     * Mengambil semua transaksi milik satu rekening.
     *
     * Dipakai untuk menampilkan riwayat transaksi di panel nasabah
     * dan panel transaksi admin.
     *
     * @param accountId ID rekening
     * @return List transaksi milik rekening tersebut
     */
    public List<Transaction> findByAccountId(String accountId) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : findAll()) {
            // Transaksi terkait rekening ini bisa sebagai:
            // 1. accountId langsung (deposit, tarik, transfer out)
            // 2. targetAccountId (transfer in)
            if (t.getAccountId().equals(accountId) ||
                (t.getTargetAccountId() != null &&
                 t.getTargetAccountId().equals(accountId))) {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * Mengambil N transaksi terakhir dari seluruh sistem.
     *
     * Dipakai untuk widget "Transaksi Terbaru" di dashboard Admin.
     * List dibalik dulu (reverse) karena CSV menyimpan urutan kronologis —
     * kita ingin yang terbaru di atas.
     *
     * @param limit jumlah maksimal transaksi yang dikembalikan
     * @return List transaksi terbaru sejumlah limit
     */
    public List<Transaction> findRecent(int limit) {
        List<Transaction> all = findAll();

        // Balik urutan list — elemen terakhir (terbaru) jadi pertama
        java.util.Collections.reverse(all);

        // Ambil sejumlah limit atau semua jika total < limit
        // subList(fromIndex, toIndex) — toIndex exclusive
        return all.subList(0, Math.min(limit, all.size()));
    }

    /**
     * Mengambil N transaksi terakhir milik satu rekening.
     *
     * Dipakai untuk widget "Transaksi Terbaru" di panel Nasabah.
     *
     * @param accountId ID rekening
     * @param limit     jumlah maksimal transaksi
     * @return List transaksi terbaru milik rekening
     */
    public List<Transaction> findRecentByAccountId(String accountId, int limit) {
        List<Transaction> byAccount = findByAccountId(accountId);
        java.util.Collections.reverse(byAccount);
        return byAccount.subList(0, Math.min(limit, byAccount.size()));
    }

    /**
     * Menyimpan transaksi baru ke CSV.
     *
     * Tidak ada update() dan delete() — transaksi bersifat immutable.
     * Sekali disimpan, tidak bisa diubah atau dihapus.
     *
     * @param transaction objek Transaction yang akan disimpan
     */
    public void save(Transaction transaction) {
        List<Transaction> transactions = findAll();
        transactions.add(transaction);
        writeToFile(transactions);
    }

    /**
     * Menghitung total transaksi yang tersimpan.
     *
     * @return jumlah transaksi
     */
    public int count() {
        return findAll().size();
    }

    /**
     * Menghitung total transaksi hari ini.
     *
     * Dipakai untuk stat card "Total Transaksi Hari Ini" di dashboard Admin.
     *
     * @param today tanggal hari ini dalam format String (contoh: "2026-06-01")
     * @return jumlah transaksi hari ini
     */
    public int countByDate(String today) {
        int count = 0;
        for (Transaction t : findAll()) {
            if (t.getDate().startsWith(today)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Helper private — tulis List<Transaction> ke file CSV.
     *
     * @param transactions List Transaction yang akan ditulis
     */
    private void writeToFile(List<Transaction> transactions) {
        List<String> lines = new ArrayList<>();
        for (Transaction t : transactions) {
            lines.add(t.toString());
        }
        CsvUtil.writeAll(AppConfig.TRANSACTIONS_FILE, lines);
    }
}