/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.util;

import banking.model.Customer;
import banking.model.Transaction;
import banking.model.enums.TransactionType;
import banking.service.BankService;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * ReceiptService — generate struk transaksi dalam format .txt
 * dan buka file otomatis setelah transaksi berhasil.
 *
 * Format struk mengikuti standar receipt perbankan Indonesia:
 * header bank, detail nasabah, detail transaksi, footer.
 *
 * Output disimpan di folder receipts/ dengan nama:
 * {transactionId}_{transactionType}.txt
 */
public class ReceiptService {

    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    private static final String RECEIPTS_DIR = "receipts/";
    private static final String LINE_FULL =
            "==========================================";
    private static final String LINE_DASH =
            "------------------------------------------";

    private ReceiptService() {}

    /**
     * Generate struk dan buka file otomatis.
     * Dipanggil dari BankService setelah transaksi berhasil disimpan.
     *
     * @param tx transaksi yang akan dicetak struk-nya
     */
    public static void generateAndOpenReceipt(Transaction tx) {
        try {
            // Pastikan folder receipts/ ada
            new File(RECEIPTS_DIR).mkdirs();

            String fileName = RECEIPTS_DIR
                    + tx.getTransactionId()
                    + "_" + tx.getType().name()
                    + ".txt";

            String content = buildReceipt(tx);

            // Tulis ke file
            try (FileWriter fw = new FileWriter(fileName)) {
                fw.write(content);
            }

            // Buka file otomatis dengan default text editor OS
            openFile(fileName);

        } catch (IOException e) {
            System.err.println("ReceiptService: gagal generate struk — "
                    + e.getMessage());
        }
    }

    /**
     * Build konten struk berdasarkan tipe transaksi.
     *
     * @param tx transaksi
     * @return String konten struk
     */
    private static String buildReceipt(Transaction tx) {
        BankService bank = BankService.getInstance();
        StringBuilder sb = new StringBuilder();

        // ── Header ──
        sb.append(LINE_FULL).append("\n");
        sb.append(center("NUSABANK", 42)).append("\n");
        sb.append(center("Mobile Banking Desktop", 42)).append("\n");
        sb.append(LINE_FULL).append("\n");
        sb.append(field("NOMOR STRUK", tx.getTransactionId())).append("\n");
        sb.append(field("TANGGAL", tx.getDate())).append("\n");
        sb.append(field("JENIS AKSES", "ONLINE BANKING ACTIVITY")).append("\n");
        sb.append(field("STATUS", "BERHASIL / SUCCESS")).append("\n");
        sb.append(LINE_FULL).append("\n");

        // ── Data Nasabah ──
        banking.model.Account acc = bank.getAccountById(tx.getAccountId());
        String customerName = "-";
        if (acc != null) {
            Customer c = bank.getCustomerById(acc.getCustomerId());
            if (c != null) customerName = c.getFullName();
        }

        sb.append(field("NASABAH", customerName)).append("\n");
        sb.append(field("NO. REKENING", tx.getAccountId())).append("\n");
        sb.append(field("AKTIVITAS", tx.getType().name())).append("\n");
        sb.append(field("DESKRIPSI", tx.getDescription())).append("\n");

        // ── Detail per tipe transaksi ──
        if (tx.getType() == TransactionType.TRANSFER_OUT
                || tx.getType() == TransactionType.TRANSFER_IN) {
            sb.append(LINE_DASH).append("\n");
            sb.append(center("DATA TRANSFER", 42)).append("\n");
            sb.append(LINE_DASH).append("\n");

            String targetId = tx.getTargetAccountId() != null
                    ? tx.getTargetAccountId() : "-";
            String targetName = "-";
            banking.model.Account targetAcc = bank.getAccountById(targetId);
            if (targetAcc != null) {
                Customer tc = bank.getCustomerById(
                        targetAcc.getCustomerId());
                if (tc != null) targetName = tc.getFullName();
            }

            if (tx.getType() == TransactionType.TRANSFER_OUT) {
                sb.append(field("REK. TUJUAN", targetId)).append("\n");
                sb.append(field("NAMA PENERIMA", targetName)).append("\n");
            } else {
                sb.append(field("REK. PENGIRIM", targetId)).append("\n");
                sb.append(field("NAMA PENGIRIM", targetName)).append("\n");
            }

            sb.append(LINE_FULL).append("\n");
            sb.append(field("NOMINAL TRF",
                    CURRENCY.format(tx.getAmount()))).append("\n");
            sb.append(field("BIAYA ADMIN",
                    "Rp 0 (PROMO 2026)")).append("\n");

        } else if (tx.getType() == TransactionType.DEPOSIT) {
            sb.append(LINE_FULL).append("\n");
            sb.append(field("NOMINAL SETOR",
                    CURRENCY.format(tx.getAmount()))).append("\n");

        } else if (tx.getType() == TransactionType.WITHDRAWAL) {
            sb.append(LINE_FULL).append("\n");
            sb.append(field("NOMINAL TARIK",
                    CURRENCY.format(tx.getAmount()))).append("\n");
            sb.append(field("BIAYA ADMIN",
                    "Rp 0 (PROMO 2026)")).append("\n");
        }

        // ── Sisa saldo ──
        sb.append(LINE_DASH).append("\n");
        double balance = acc != null ? acc.getBalance() : 0;
        sb.append(field("SISA SALDO",
                CURRENCY.format(balance))).append("\n");
        sb.append(LINE_FULL).append("\n");

        // ── Footer ──
        sb.append(center("TERIMA KASIH ATAS KEPERCAYAAN", 42)).append("\n");
        sb.append(center("ANDA PADA BANK", 42)).append("\n");
        sb.append(center("NUSABANK", 42)).append("\n");
        sb.append(LINE_FULL).append("\n");

        return sb.toString();
    }

    /**
     * Buka file dengan default application di OS.
     * Menggunakan Desktop API Java — cross-platform.
     *
     * @param fileName path ke file yang akan dibuka
     */
    private static void openFile(String fileName) {
        try {
            File file = new File(fileName);
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file);
            } else {
                // Fallback untuk Linux tanpa Desktop support
                new ProcessBuilder("xdg-open", file.getAbsolutePath())
                        .start();
            }
        } catch (IOException e) {
            System.err.println("ReceiptService: tidak bisa buka file — "
                    + e.getMessage());
        }
    }

    /**
     * Helper — format field dengan padding rata kiri-kanan.
     * Contoh: field("NASABAH", "Budi Santoso")
     * → " NASABAH      : Budi Santoso"
     */
    private static String field(String key, String value) {
        return String.format(" %-13s: %s", key, value != null ? value : "-");
    }

    /**
     * Helper — center text dalam lebar tertentu.
     *
     * @param text  teks yang akan di-center
     * @param width lebar total
     * @return string dengan padding kiri-kanan
     */
    private static String center(String text, int width) {
        if (text.length() >= width) return text;
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }
}