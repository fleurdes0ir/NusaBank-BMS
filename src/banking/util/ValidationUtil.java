/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.util;

import java.util.regex.Pattern;

/**
 * ValidationUtil — centralized validation layer.
 *
 * Mengimplementasikan 5 kategori validasi:
 * 1. Data Type    — pastikan input bisa dikonversi ke tipe yang benar
 * 2. Length/Range — batas panjang string dan range angka
 * 3. Pattern/Regex — format email, phone, username, tanggal
 * 4. Sanitization — strip karakter berbahaya (XSS, CSV injection)
 * 5. Business Logic — aturan bisnis perbankan (saldo min, limit transfer)
 *
 * Semua method static — tidak perlu instantiate.
 * Throw IllegalArgumentException dengan pesan yang actionable.
 */
public class ValidationUtil {

    private ValidationUtil() {}

    // =========================================================
    // COMPILED REGEX PATTERNS
    // Pre-compile untuk performa — tidak dicompile ulang setiap call
    // =========================================================

    /**
     * Email RFC 5322 simplified — cukup untuk validasi form perbankan.
     * Contoh valid  : budi.santoso@gmail.com, user@bank.co.id
     * Contoh invalid: budi@, @gmail.com, budi@@gmail.com
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    /**
     * Nomor telepon Indonesia:
     * - Diawali 08 atau +628 atau 628
     * - Panjang 10-15 digit
     * Contoh valid  : 081234567890, +6281234567890, 6281234567890
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(\\+62|62|0)8[1-9][0-9]{7,11}$"
    );

    /**
     * Username:
     * - Hanya huruf, angka, titik, underscore, strip
     * - Minimal 3, maksimal 30 karakter
     * - Tidak boleh diawali/diakhiri karakter spesial
     * Contoh valid  : budi.santoso, user_123, john-doe
     * Contoh invalid: .budi, budi., bu di, budi@
     */
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9][a-zA-Z0-9._\\-]{1,28}[a-zA-Z0-9]$"
    );

    /**
     * Nama lengkap:
     * - Hanya huruf (termasuk aksen), spasi, titik, strip
     * - Minimal 2, maksimal 100 karakter
     * Contoh valid  : Budi Santoso, Muhammad Al-Farisi, Dr. Sari
     * Contoh invalid: Budi123, <script>, Budi@Santoso
     */
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile(
        "^[a-zA-Z\\u00C0-\\u024F][a-zA-Z\\u00C0-\\u024F .\\-']{1,99}$"
    );

    /**
     * Tanggal format yyyy-MM-dd:
     * Contoh valid  : 2024-01-15, 2026-12-31
     * Contoh invalid: 15-01-2024, 2024/01/15, 2024-13-01
     */
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$"
    );

    /**
     * Karakter berbahaya untuk CSV injection.
     * Formula injection diawali: =, +, -, @
     * Digunakan spreadsheet untuk execute formula dari CSV.
     */
    private static final Pattern CSV_INJECTION_PATTERN = Pattern.compile(
        "^[=+\\-@].*"
    );

    /**
     * Karakter HTML/script berbahaya (XSS prevention).
     */
    private static final Pattern XSS_PATTERN = Pattern.compile(
        ".*[<>\"';&].*"
    );

    // =========================================================
    // 1. DATA TYPE VALIDATION
    // =========================================================

    /**
     * Validasi bahwa string bisa diparse sebagai double positif.
     *
     * @param value     string yang akan divalidasi
     * @param fieldName nama field untuk pesan error
     * @return nilai double yang sudah diparsed
     * @throws IllegalArgumentException jika tidak valid
     */
    public static double validatePositiveDouble(String value,
            String fieldName) {
        if (value == null || value.trim().isEmpty())
            throw new IllegalArgumentException(
                fieldName + " tidak boleh kosong.");
        try {
            // Ganti koma dengan titik untuk handle input Indonesia
            // (user mungkin ketik "1.000.000" atau "1,000,000")
            String cleaned = value.trim()
                .replace(".", "")
                .replace(",", ".");
            double result = Double.parseDouble(cleaned);
            if (result <= 0)
                throw new IllegalArgumentException(
                    fieldName + " harus bernilai positif.");
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                fieldName + " harus berupa angka. "
                + "Contoh: 1000000 atau 1,000,000");
        }
    }

    /**
     * Validasi integer positif.
     *
     * @param value     string yang akan divalidasi
     * @param fieldName nama field untuk pesan error
     * @return nilai int yang sudah diparsed
     */
    public static int validatePositiveInt(String value,
            String fieldName) {
        if (value == null || value.trim().isEmpty())
            throw new IllegalArgumentException(
                fieldName + " tidak boleh kosong.");
        try {
            int result = Integer.parseInt(value.trim());
            if (result <= 0)
                throw new IllegalArgumentException(
                    fieldName + " harus bernilai positif.");
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                fieldName + " harus berupa bilangan bulat. "
                + "Contoh: 12");
        }
    }

    // =========================================================
    // 2. LENGTH & RANGE VALIDATION
    // =========================================================

    /**
     * Validasi panjang string dalam range [min, max].
     *
     * @param value     string yang divalidasi
     * @param fieldName nama field
     * @param min       panjang minimum
     * @param max       panjang maksimum
     * @throws IllegalArgumentException jika di luar range
     */
    public static void validateLength(String value, String fieldName,
            int min, int max) {
        if (value == null || value.trim().length() < min)
            throw new IllegalArgumentException(
                fieldName + " minimal " + min + " karakter.");
        if (value.trim().length() > max)
            throw new IllegalArgumentException(
                fieldName + " maksimal " + max + " karakter.");
    }

    /**
     * Validasi range nilai double dalam [min, max].
     *
     * @param value     nilai yang divalidasi
     * @param fieldName nama field
     * @param min       nilai minimum
     * @param max       nilai maksimum
     */
    public static void validateRange(double value, String fieldName,
            double min, double max) {
        if (value < min || value > max)
            throw new IllegalArgumentException(
                fieldName + " harus antara "
                + formatRupiah(min) + " dan "
                + formatRupiah(max) + ".");
    }

    /**
     * Validasi range integer dalam [min, max].
     */
    public static void validateIntRange(int value, String fieldName,
            int min, int max) {
        if (value < min || value > max)
            throw new IllegalArgumentException(
                fieldName + " harus antara " + min
                + " dan " + max + ".");
    }

    // =========================================================
    // 3. PATTERN MATCHING / REGEX VALIDATION
    // =========================================================

    /**
     * Validasi format email.
     *
     * @param email string email
     * @throws IllegalArgumentException jika format salah
     */
    public static void validateEmail(String email) {
        validateLength(email, "Email", 5, 100);
        if (!EMAIL_PATTERN.matcher(email.trim()).matches())
            throw new IllegalArgumentException(
                "Format email tidak valid. "
                + "Contoh: nama@domain.com");
    }

    /**
     * Validasi nomor telepon Indonesia.
     *
     * @param phone nomor telepon
     * @throws IllegalArgumentException jika format salah
     */
    public static void validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty())
            throw new IllegalArgumentException(
                "Nomor telepon tidak boleh kosong.");
        // Hapus spasi dan strip sebelum validasi
        String cleaned = phone.trim().replace(" ", "").replace("-", "");
        if (!PHONE_PATTERN.matcher(cleaned).matches())
            throw new IllegalArgumentException(
                "Format nomor telepon tidak valid. "
                + "Contoh: 081234567890 atau +6281234567890");
    }

    /**
     * Validasi username.
     *
     * @param username string username
     * @throws IllegalArgumentException jika format salah
     */
    public static void validateUsername(String username) {
        validateLength(username, "Username", 3, 30);
        if (!USERNAME_PATTERN.matcher(username.trim()).matches())
            throw new IllegalArgumentException(
                "Username hanya boleh berisi huruf, angka, titik, "
                + "underscore, atau strip. Minimal 3 karakter.");
    }

    /**
     * Validasi nama lengkap.
     *
     * @param fullName string nama lengkap
     * @throws IllegalArgumentException jika format salah
     */
    public static void validateFullName(String fullName) {
        validateLength(fullName, "Nama lengkap", 2, 100);
        if (!FULL_NAME_PATTERN.matcher(fullName.trim()).matches())
            throw new IllegalArgumentException(
                "Nama lengkap hanya boleh berisi huruf dan spasi. "
                + "Karakter spesial tidak diizinkan.");
    }

    /**
     * Validasi password strength.
     * Minimum requirement perbankan digital Indonesia (OJK):
     * - Minimal 8 karakter
     * - Mengandung huruf besar, huruf kecil, dan angka
     *
     * @param password string password plaintext
     * @throws IllegalArgumentException jika terlalu lemah
     */
    public static void validatePassword(String password) {
        if (password == null || password.length() < 8)
            throw new IllegalArgumentException(
                "Password minimal 8 karakter.");
        if (password.length() > 64)
            throw new IllegalArgumentException(
                "Password maksimal 64 karakter.");

        boolean hasUpper  = password.chars()
            .anyMatch(Character::isUpperCase);
        boolean hasLower  = password.chars()
            .anyMatch(Character::isLowerCase);
        boolean hasDigit  = password.chars()
            .anyMatch(Character::isDigit);

        if (!hasUpper || !hasLower || !hasDigit)
            throw new IllegalArgumentException(
                "Password harus mengandung huruf besar, "
                + "huruf kecil, dan angka. "
                + "Contoh: Nasabah123");
    }

    // =========================================================
    // 4. SANITIZATION & SECURITY
    // =========================================================

    /**
     * Sanitasi input teks umum.
     * - Trim whitespace
     * - Deteksi CSV injection (formula injection)
     * - Deteksi XSS pattern
     *
     * @param value     input dari user
     * @param fieldName nama field untuk pesan error
     * @return string yang sudah disanitasi
     * @throws IllegalArgumentException jika mengandung karakter berbahaya
     */
    public static String sanitize(String value, String fieldName) {
        if (value == null) return "";
        String trimmed = value.trim();

        // Deteksi CSV injection — karakter formula di awal
        if (CSV_INJECTION_PATTERN.matcher(trimmed).matches())
            throw new IllegalArgumentException(
                fieldName + " mengandung karakter yang tidak diizinkan "
                + "di awal input (=, +, -, @).");

        // Deteksi XSS pattern
        if (XSS_PATTERN.matcher(trimmed).matches())
            throw new IllegalArgumentException(
                fieldName + " mengandung karakter HTML yang tidak "
                + "diizinkan (<, >, \", ', ;, &).");

        return trimmed;
    }

    /**
     * Sanitasi dan validasi field deskripsi/keterangan.
     * Lebih permissive dari sanitize() — allow titik, koma, tanda seru.
     * Tidak allow karakter HTML dan formula injection.
     *
     * @param value     input deskripsi
     * @param fieldName nama field
     * @param maxLength panjang maksimum
     * @return string yang sudah disanitasi
     */
    public static String sanitizeDescription(String value,
            String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty())
            return "";
        String sanitized = sanitize(value, fieldName);
        if (sanitized.length() > maxLength)
            throw new IllegalArgumentException(
                fieldName + " maksimal " + maxLength + " karakter.");
        return sanitized;
    }

    /**
     * Validasi bahwa string tidak kosong setelah trim.
     *
     * @param value     input
     * @param fieldName nama field
     * @return string trimmed
     */
    public static String requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty())
            throw new IllegalArgumentException(
                fieldName + " tidak boleh kosong.");
        return value.trim();
    }

    // =========================================================
    // 5. BUSINESS LOGIC VALIDATION
    // =========================================================

    /**
     * Validasi jumlah deposit.
     * Minimum deposit Rp 10.000 sesuai standar bank umum Indonesia.
     *
     * @param amount jumlah deposit
     */
    public static void validateDepositAmount(double amount) {
        validateRange(amount, "Jumlah deposit",
            10_000, 500_000_000);
    }

    /**
     * Validasi jumlah penarikan.
     * Minimum tarik Rp 50.000, maksimum Rp 100.000.000 per transaksi.
     *
     * @param amount jumlah penarikan
     */
    public static void validateWithdrawalAmount(double amount) {
        validateRange(amount, "Jumlah penarikan",
            50_000, 100_000_000);
    }

    /**
     * Validasi jumlah transfer.
     * Minimum transfer Rp 10.000, maksimum Rp 100.000.000 per transaksi.
     * Sesuai limit transfer online banking umum di Indonesia.
     *
     * @param amount jumlah transfer
     */
    public static void validateTransferAmount(double amount) {
        validateRange(amount, "Jumlah transfer",
            10_000, 100_000_000);
    }

    /**
     * Validasi pokok pinjaman.
     * Minimum Rp 1.000.000, maksimum Rp 500.000.000.
     * Sesuai range KTA bank umum Indonesia.
     *
     * @param principal pokok pinjaman
     */
    public static void validateLoanPrincipal(double principal) {
        validateRange(principal, "Jumlah pinjaman",
            1_000_000, 500_000_000);
    }

    /**
     * Validasi tenor pinjaman.
     * Minimum 3 bulan, maksimum 360 bulan (30 tahun).
     *
     * @param tenorMonths tenor dalam bulan
     */
    public static void validateLoanTenor(int tenorMonths) {
        validateIntRange(tenorMonths, "Tenor pinjaman", 3, 360);
    }

    /**
     * Validasi suku bunga pinjaman.
     * Range: MIN_ANNUAL_RATE s/d MAX_ANNUAL_RATE sesuai LoanCalculator.
     *
     * @param annualRate suku bunga per tahun dalam persen
     */
    public static void validateInterestRate(double annualRate) {
        validateRange(annualRate, "Suku bunga",
            LoanCalculator.MIN_ANNUAL_RATE,
            LoanCalculator.MAX_ANNUAL_RATE);
    }

    /**
     * Validasi setoran awal rekening tabungan.
     * Minimum Rp 50.000 sesuai SavingsAccount.getMinimumBalance().
     *
     * @param amount    setoran awal
     * @param minBalance saldo minimum rekening
     */
    public static void validateInitialDeposit(double amount,
            double minBalance) {
        if (amount < minBalance)
            throw new IllegalArgumentException(
                "Setoran awal minimal " + formatRupiah(minBalance)
                + " untuk jenis rekening ini.");
    }

    /**
     * Validasi rekening sumber dan tujuan tidak sama saat transfer.
     *
     * @param sourceId ID rekening sumber
     * @param targetId ID rekening tujuan
     */
    public static void validateDifferentAccounts(String sourceId,
            String targetId) {
        if (sourceId != null && sourceId.equals(targetId))
            throw new IllegalArgumentException(
                "Rekening sumber dan tujuan tidak boleh sama.");
    }

    /**
     * Validasi alasan penolakan pinjaman tidak kosong.
     *
     * @param reason alasan penolakan
     */
    public static void validateRejectionReason(String reason) {
        requireNonEmpty(reason, "Alasan penolakan");
        validateLength(reason, "Alasan penolakan", 5, 200);
    }

    // =========================================================
    // HELPER
    // =========================================================

    /**
     * Format angka ke format Rupiah singkat untuk pesan error.
     * Contoh: 50000 → "Rp 50.000"
     */
    private static String formatRupiah(double amount) {
        return "Rp " + String.format("%,.0f", amount)
            .replace(",", ".");
    }
}