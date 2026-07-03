/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.util;

import banking.model.enums.LoanType;
import java.util.ArrayList;
import java.util.List;

/**
 * LoanCalculator — engine kalkulasi bunga pinjaman.
 *
 * Mengimplementasikan dua metode bunga standar perbankan Indonesia
 * sesuai regulasi OJK (Otoritas Jasa Keuangan):
 *
 * 1. FLAT RATE
 *    Bunga dihitung dari pokok awal setiap bulan.
 *    Cicilan = (Pokok + Pokok × Rate × Tenor) / Tenor
 *    Karakteristik: cicilan tetap, total bunga lebih besar dari anuitas.
 *    Dipakai: KTA, kredit multiguna, cicilan dealer.
 *
 * 2. ANUITAS (ANNUITY)
 *    Cicilan tetap, tapi komposisi bunga/pokok berubah tiap bulan.
 *    Bunga bulan ini = Sisa Pokok × Rate Bulanan
 *    Pokok bulan ini = Cicilan - Bunga bulan ini
 *    Karakteristik: total bunga lebih kecil dari flat, sisa pokok
 *    turun lebih lambat di awal tenor.
 *    Dipakai: KPR, KKB, kredit investasi.
 *
 * Referensi: SE OJK No. 13/SEOJK.07/2014 tentang
 * Perjanjian Baku dalam Layanan Keuangan.
 */
public class LoanCalculator {

    // Range bunga yang diizinkan sesuai standar OJK
    // Bunga flat: 0.5% - 4% per bulan (6% - 48% per tahun)
    // Bunga efektif: 0.5% - 3% per bulan
    public static final double MIN_ANNUAL_RATE = 6.0;   // 6% per tahun
    public static final double MAX_ANNUAL_RATE = 48.0;  // 48% per tahun
    public static final double DEFAULT_ANNUAL_RATE = 12.0; // 12% per tahun

    private LoanCalculator() {}

    // =========================================================
    // FLAT RATE
    // =========================================================

    /**
     * Menghitung cicilan bulanan dengan metode flat rate.
     *
     * Rumus: cicilan = (P + P × r × t) / t
     * di mana:
     *   P = pokok pinjaman
     *   r = suku bunga per tahun (dalam desimal, contoh: 12% = 0.12)
     *   t = tenor dalam bulan
     *
     * Contoh: Pokok 10jt, rate 12%/tahun, tenor 12 bulan
     * Bunga total = 10.000.000 × 0.12 × (12/12) = 1.200.000
     * Cicilan = (10.000.000 + 1.200.000) / 12 = 933.333/bulan
     *
     * @param principal    pokok pinjaman dalam Rupiah
     * @param annualRate   suku bunga tahunan dalam persen (contoh: 12.0)
     * @param tenorMonths  tenor dalam bulan
     * @return cicilan bulanan
     */
    public static double calculateFlatMonthly(double principal,
            double annualRate, int tenorMonths) {
        // Konversi rate tahunan ke desimal per tahun
        double rateDecimal = annualRate / 100.0;
        // Hitung total bunga selama tenor
        double totalInterest = principal * rateDecimal * (tenorMonths / 12.0);
        // Cicilan = (pokok + total bunga) / tenor
        return (principal + totalInterest) / tenorMonths;
    }

    /**
     * Menghitung total pembayaran dengan metode flat rate.
     *
     * @param principal   pokok pinjaman
     * @param annualRate  suku bunga tahunan dalam persen
     * @param tenorMonths tenor dalam bulan
     * @return total yang harus dibayar (pokok + bunga)
     */
    public static double calculateFlatTotalPayment(double principal,
            double annualRate, int tenorMonths) {
        return calculateFlatMonthly(principal, annualRate, tenorMonths)
               * tenorMonths;
    }

    /**
     * Menghitung total bunga dengan metode flat rate.
     *
     * @param principal   pokok pinjaman
     * @param annualRate  suku bunga tahunan dalam persen
     * @param tenorMonths tenor dalam bulan
     * @return total bunga yang dibayar
     */
    public static double calculateFlatTotalInterest(double principal,
            double annualRate, int tenorMonths) {
        return calculateFlatTotalPayment(principal, annualRate, tenorMonths)
               - principal;
    }

    // =========================================================
    // ANUITAS
    // =========================================================

    /**
     * Menghitung cicilan bulanan dengan metode anuitas.
     *
     * Rumus anuitas:
     * cicilan = P × [r × (1+r)^t] / [(1+r)^t - 1]
     *
     * di mana:
     *   P = pokok pinjaman
     *   r = suku bunga per bulan (annualRate / 12 / 100)
     *   t = tenor dalam bulan
     *
     * Contoh: Pokok 10jt, rate 12%/tahun (1%/bulan), tenor 12 bulan
     * r = 0.01, t = 12
     * cicilan = 10.000.000 × [0.01 × (1.01)^12] / [(1.01)^12 - 1]
     *         = 10.000.000 × [0.01 × 1.12683] / [1.12683 - 1]
     *         = 10.000.000 × 0.011268 / 0.12683
     *         = 888.487/bulan
     *
     * @param principal   pokok pinjaman
     * @param annualRate  suku bunga tahunan dalam persen
     * @param tenorMonths tenor dalam bulan
     * @return cicilan bulanan anuitas
     */
    public static double calculateAnnuityMonthly(double principal,
            double annualRate, int tenorMonths) {
        // Konversi ke rate bulanan dalam desimal
        double monthlyRate = annualRate / 100.0 / 12.0;

        // Guard: jika rate 0%, tidak ada bunga — pakai flat sederhana
        if (monthlyRate == 0) return principal / tenorMonths;

        // Rumus anuitas
        double power = Math.pow(1 + monthlyRate, tenorMonths);
        return principal * (monthlyRate * power) / (power - 1);
    }

    /**
     * Menghitung total pembayaran dengan metode anuitas.
     *
     * @param principal   pokok pinjaman
     * @param annualRate  suku bunga tahunan dalam persen
     * @param tenorMonths tenor dalam bulan
     * @return total yang harus dibayar
     */
    public static double calculateAnnuityTotalPayment(double principal,
            double annualRate, int tenorMonths) {
        return calculateAnnuityMonthly(principal, annualRate, tenorMonths)
               * tenorMonths;
    }

    /**
     * Menghitung total bunga dengan metode anuitas.
     *
     * @param principal   pokok pinjaman
     * @param annualRate  suku bunga tahunan dalam persen
     * @param tenorMonths tenor dalam bulan
     * @return total bunga yang dibayar
     */
    public static double calculateAnnuityTotalInterest(double principal,
            double annualRate, int tenorMonths) {
        return calculateAnnuityTotalPayment(principal, annualRate, tenorMonths)
               - principal;
    }

    // =========================================================
    // JADWAL ANGSURAN (AMORTIZATION SCHEDULE)
    // =========================================================

    /**
     * Data satu baris jadwal angsuran.
     * Dipakai untuk menampilkan tabel simulasi di UI.
     */
    public static class InstallmentRow {
        public final int month;
        public final double payment;      // cicilan bulan ini
        public final double principal;    // porsi pokok
        public final double interest;     // porsi bunga
        public final double remaining;    // sisa pokok setelah bayar

        public InstallmentRow(int month, double payment,
                double principal, double interest, double remaining) {
            this.month     = month;
            this.payment   = payment;
            this.principal = principal;
            this.interest  = interest;
            this.remaining = remaining;
        }
    }

    /**
     * Generate jadwal angsuran lengkap untuk metode FLAT.
     *
     * Pada flat rate, porsi bunga sama setiap bulan:
     * bunga per bulan = total bunga / tenor
     * pokok per bulan = pokok / tenor
     *
     * @param principal   pokok pinjaman
     * @param annualRate  suku bunga tahunan dalam persen
     * @param tenorMonths tenor dalam bulan
     * @return List jadwal angsuran per bulan
     */
    public static List<InstallmentRow> generateFlatSchedule(
            double principal, double annualRate, int tenorMonths) {
        List<InstallmentRow> schedule = new ArrayList<>();
        double monthly      = calculateFlatMonthly(principal,
                annualRate, tenorMonths);
        double monthlyPrincipal = principal / tenorMonths;
        double monthlyInterest  = monthly - monthlyPrincipal;
        double remaining    = principal;

        for (int i = 1; i <= tenorMonths; i++) {
            remaining -= monthlyPrincipal;
            // Bulan terakhir — koreksi floating point
            if (i == tenorMonths) remaining = 0;
            schedule.add(new InstallmentRow(
                i, monthly, monthlyPrincipal,
                monthlyInterest, Math.max(0, remaining)));
        }
        return schedule;
    }

    /**
     * Generate jadwal angsuran lengkap untuk metode ANUITAS.
     *
     * Pada anuitas:
     * - Cicilan tetap setiap bulan
     * - Bunga = sisa pokok × rate bulanan (makin kecil tiap bulan)
     * - Pokok = cicilan - bunga (makin besar tiap bulan)
     *
     * @param principal   pokok pinjaman
     * @param annualRate  suku bunga tahunan dalam persen
     * @param tenorMonths tenor dalam bulan
     * @return List jadwal angsuran per bulan
     */
    public static List<InstallmentRow> generateAnnuitySchedule(
            double principal, double annualRate, int tenorMonths) {
        List<InstallmentRow> schedule = new ArrayList<>();
        double monthlyRate = annualRate / 100.0 / 12.0;
        double monthly     = calculateAnnuityMonthly(
                principal, annualRate, tenorMonths);
        double remaining   = principal;

        for (int i = 1; i <= tenorMonths; i++) {
            // Bunga bulan ini dari sisa pokok
            double interest  = remaining * monthlyRate;
            // Pokok yang dibayar bulan ini
            double principalPaid = monthly - interest;
            remaining -= principalPaid;
            // Koreksi floating point di bulan terakhir
            if (i == tenorMonths) remaining = 0;
            schedule.add(new InstallmentRow(
                i, monthly, principalPaid,
                interest, Math.max(0, remaining)));
        }
        return schedule;
    }

    /**
     * Dispatch ke metode yang sesuai berdasarkan LoanType.
     *
     * @param loanType    FLAT atau ANNUITY
     * @param principal   pokok pinjaman
     * @param annualRate  suku bunga tahunan dalam persen
     * @param tenorMonths tenor dalam bulan
     * @return cicilan bulanan
     */
    public static double calculateMonthly(LoanType loanType,
            double principal, double annualRate, int tenorMonths) {
        return loanType == LoanType.ANNUITY
            ? calculateAnnuityMonthly(principal, annualRate, tenorMonths)
            : calculateFlatMonthly(principal, annualRate, tenorMonths);
    }

    /**
     * Dispatch total payment berdasarkan LoanType.
     *
     * @param loanType    FLAT atau ANNUITY
     * @param principal   pokok pinjaman
     * @param annualRate  suku bunga tahunan dalam persen
     * @param tenorMonths tenor dalam bulan
     * @return total pembayaran
     */
    public static double calculateTotalPayment(LoanType loanType,
            double principal, double annualRate, int tenorMonths) {
        return loanType == LoanType.ANNUITY
            ? calculateAnnuityTotalPayment(principal, annualRate, tenorMonths)
            : calculateFlatTotalPayment(principal, annualRate, tenorMonths);
    }

    /**
     * Generate jadwal angsuran berdasarkan LoanType.
     *
     * @param loanType    FLAT atau ANNUITY
     * @param principal   pokok pinjaman
     * @param annualRate  suku bunga tahunan dalam persen
     * @param tenorMonths tenor dalam bulan
     * @return List jadwal angsuran
     */
    public static List<InstallmentRow> generateSchedule(LoanType loanType,
            double principal, double annualRate, int tenorMonths) {
        return loanType == LoanType.ANNUITY
            ? generateAnnuitySchedule(principal, annualRate, tenorMonths)
            : generateFlatSchedule(principal, annualRate, tenorMonths);
    }
}