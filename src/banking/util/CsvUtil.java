/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.util;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class untuk operasi file CSV dan keamanan password.
 *
 * Seluruh method bersifat STATIC — artinya tidak perlu membuat objek CsvUtil
 * untuk memakainya. Cukup panggil langsung: CsvUtil.readAll(path)
 *
 * Constructor dibuat private untuk mencegah instantiasi — ini adalah
 * pattern "Utility Class" yang umum dipakai di Java.
 */
public class CsvUtil {

    // Private constructor — class ini tidak boleh di-instantiate
    private CsvUtil() {}

    /**
     * Membaca semua baris dari file CSV dan mengembalikannya sebagai
     * List of String array. Tiap elemen array = satu kolom CSV.
     *
     * Contoh baris CSV: "C001,Budi,budi@mail.com"
     * Hasil split:      ["C001", "Budi", "budi@mail.com"]
     *
     * @param filePath path ke file CSV
     * @return List berisi array String per baris, kosong jika file tidak ada
     */
    public static List<String[]> readAll(String filePath) {
        // ArrayList sebagai container hasil — ukuran dinamis
        List<String[]> result = new ArrayList<>();
        File file = new File(filePath);

        // Guard clause — kalau file belum ada, kembalikan list kosong
        // Ini terjadi saat aplikasi pertama kali dijalankan
        if (!file.exists()) return result;

        // try-with-resources: BufferedReader otomatis ditutup setelah selesai
        // BufferedReader lebih efisien dari FileReader langsung karena ada buffer
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;

            // Baca baris satu per satu sampai habis (readLine() return null di EOF)
            while ((line = br.readLine()) != null) {

                // Skip baris kosong agar tidak ada array kosong di hasil
                if (!line.trim().isEmpty()) {
                    // split dengan limit -1 agar trailing empty string ikut masuk
                    // Contoh: "A001,C001,,data" → ["A001","C001","","data"]
                    // Tanpa -1, field kosong di akhir akan terpotong
                    result.add(parseCsvLine(line));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath
                    + " - " + e.getMessage());
        }
        return result;
    }

    /**
     * Menulis List of String ke file CSV, mengganti seluruh isi file (overwrite).
     * Folder data/ akan dibuat otomatis jika belum ada.
     *
     * @param filePath path tujuan penulisan
     * @param lines    List berisi String yang akan ditulis (satu String = satu baris)
     */
    public static void writeAll(String filePath, List<String> lines) {
        try {
            // Pastikan folder data/ ada — mkdirs() membuat seluruh path jika perlu
            File dir = new File(AppConfig.DATA_DIR);
            if (!dir.exists()) dir.mkdirs();

            // FileWriter dengan parameter false = OVERWRITE (bukan append)
            // Ini intentional — kita selalu tulis ulang seluruh file
            try (BufferedWriter bw = new BufferedWriter(
                    new FileWriter(filePath, false))) {
                for (String line : lines) {
                    bw.write(line);
                    // Platform-safe newline (\n di Linux, \r\n di Windows)
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing file: " + filePath
                    + " - " + e.getMessage());
        }
    }

    /**
     * Menghasilkan hash SHA-256 dari password plaintext.
     *
     * SHA-256 adalah one-way hash — tidak bisa di-decrypt kembali.
     * Kita tidak menyimpan password asli, hanya hashnya.
     * Saat login, password input di-hash lagi lalu dibandingkan
     * dengan hash tersimpan.
     *
     * @param password password plaintext
     * @return String hex 64 karakter hasil SHA-256
     */
    public static String hashPassword(String password) {
        try {
            // MessageDigest adalah class Java untuk operasi hashing kriptografi
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // digest() mengubah String menjadi array byte hasil hash
            byte[] hash = md.digest(password.getBytes());

            // Konversi byte array ke String hex yang readable
            // %02x = format hex 2 digit lowercase (contoh: 0a, ff, 3b)
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 dijamin ada di semua JVM — practically unreachable
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Menghasilkan ID unik dengan format prefix + nomor urut.
     * Contoh: generateId("C", 3)  → "C003"
     *         generateId("TRX", 12) → "TRX012"
     *
     * @param prefix awalan ID (contoh: "C", "A", "TRX")
     * @param number nomor urut
     * @return String ID terformat
     */
    public static String generateId(String prefix, int number) {
        // %03d = integer minimum 3 digit, dipadding nol di depan
        // Contoh: 1 → "001", 12 → "012", 123 → "123"
        return prefix + String.format("%03d", number);
    }
    
        /**
     * Parse satu baris CSV yang field-nya bisa mengandung koma.
     * Field yang mengandung koma dibungkus tanda kutip ganda.
     *
     * Contoh input : C001,Budi,budi@mail.com,081234,"Jl. Sudirman, Jakarta",2024-01-15
     * Contoh output: ["C001","Budi","budi@mail.com","081234","Jl. Sudirman, Jakarta","2024-01-15"]
     *
     * Algoritma: iterasi karakter satu per satu, track apakah
     * sedang di dalam quoted field atau tidak.
     *
     * @param line satu baris CSV
     * @return array String per field
     */
    public static String[] parseCsvLine(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // Toggle status inQuotes
                // Double quote "" di dalam quoted field = escape
                if (inQuotes && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {
                    // Escaped quote — tambah satu kutip ke current
                    current.append('"');
                    i++; // skip karakter berikutnya
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // Koma di luar quotes = pemisah field
                fields.add(current.toString().trim());
                current.setLength(0); // reset buffer
            } else {
                current.append(c);
            }
        }
        // Tambah field terakhir
        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }

    /**
     * Bungkus nilai field dengan tanda kutip jika mengandung koma.
     * Dipakai saat menulis CSV agar field dengan koma tidak merusak format.
     *
     * Contoh: "Jl. Sudirman, Jakarta" → "\"Jl. Sudirman, Jakarta\""
     *
     * @param value nilai field yang akan ditulis
     * @return nilai yang sudah di-escape jika perlu
     */
    public static String escapeCsvField(String value) {
        if (value == null) return "";
        // Jika mengandung koma atau kutip, bungkus dengan kutip ganda
        if (value.contains(",") || value.contains("\"")
                || value.contains("\n")) {
            // Escape kutip ganda di dalam value dengan double quote
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}