/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.repository;

import banking.model.Loan;
import banking.model.enums.LoanType;
import banking.model.enums.LoanStatus;
import banking.util.AppConfig;
import banking.util.CsvUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * LoanRepository menangani persistensi data pinjaman.
 *
 * Format CSV:
 * loanId,customerId,principal,monthlyPayment,tenorMonths,
 * paidMonths,status,startDate,description
 */
public class LoanRepository {

    /**
     * Mengambil semua data pinjaman dari CSV.
     *
     * Perhatikan parsing LoanStatus — sama seperti UserRole dan AccountType,
     * disimpan sebagai String di CSV, dikonversi ke enum saat dibaca.
     *
     * @return List semua objek Loan
     */
    public List<Loan> findAll() {
        List<Loan> loans = new ArrayList<>();
        List<String[]> rows = CsvUtil.readAll(AppConfig.LOANS_FILE);

        for (String[] row : rows) {
            if (row.length < 9) continue;

            try {
                String loanId          = row[0].trim();
                String customerId      = row[1].trim();
                // Parsing angka — Double untuk nilai uang, Integer untuk bulan
                double principal       = Double.parseDouble(row[2].trim());
                double monthlyPayment  = Double.parseDouble(row[3].trim());
                int tenorMonths        = Integer.parseInt(row[4].trim());
                int paidMonths         = Integer.parseInt(row[5].trim());
                // valueOf() konversi String ke enum LoanStatus
                LoanStatus status      = LoanStatus.valueOf(row[6].trim());
                String startDate       = row[7].trim();
                String description     = row[8].trim();

                loans.add(new Loan(
                    row[0].trim(),  // loanId
                    row[1].trim(),  // customerId
                    Double.parseDouble(row[2].trim()),  // principal
                    Double.parseDouble(row[3].trim()),  // monthlyPayment
                    Integer.parseInt(row[4].trim()),    // tenorMonths
                    Integer.parseInt(row[5].trim()),    // paidMonths
                    LoanStatus.valueOf(row[6].trim()),  // status
                    row[7].trim(),  // startDate
                    row[8].trim(),  // description
                    // Field baru — pakai default jika CSV lama (kurang dari 15 kolom)
                    row.length > 9  ? Double.parseDouble(row[9].trim())  : 12.0,
                    row.length > 10 ? LoanType.valueOf(row[10].trim())   : LoanType.FLAT,
                    row.length > 11 ? Double.parseDouble(row[11].trim()) : 0.0,
                    row.length > 12 ? row[12].trim() : "",
                    row.length > 13 ? row[13].trim() : "",
                    row.length > 14 ? row[14].trim() : ""
                ));
                
            } catch (IllegalArgumentException e) {
                System.err.println("Error parsing loan row: " + e.getMessage());
            }
        }
        return loans;
    }

    /**
     * Mencari pinjaman berdasarkan ID.
     *
     * @param loanId ID pinjaman
     * @return Loan jika ditemukan, null jika tidak
     */
    public Loan findById(String loanId) {
        return findAll().stream()
                .filter(l -> l.getLoanId().equals(loanId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Mengambil semua pinjaman milik satu Customer.
     *
     * @param customerId ID customer
     * @return List pinjaman milik customer tersebut
     */
    public List<Loan> findByCustomerId(String customerId) {
        List<Loan> result = new ArrayList<>();
        for (Loan l : findAll()) {
            if (l.getCustomerId().equals(customerId)) {
                result.add(l);
            }
        }
        return result;
    }

    /**
     * Mengambil semua pinjaman dengan status ACTIVE.
     *
     * Dipakai untuk widget "Status Pinjaman" di dashboard Admin
     * dan panel Nasabah.
     *
     * @return List pinjaman aktif
     */
    public List<Loan> findActive() {
        List<Loan> result = new ArrayList<>();
        for (Loan l : findAll()) {
            if (l.getStatus() == LoanStatus.ACTIVE) {
                result.add(l);
            }
        }
        return result;
    }

    /**
     * Menyimpan pinjaman baru ke CSV.
     *
     * @param loan objek Loan yang akan disimpan
     */
    public void save(Loan loan) {
        List<Loan> loans = findAll();
        loans.add(loan);
        writeToFile(loans);
    }

    /**
     * Memperbarui data pinjaman — dipanggil saat cicilan dibayar
     * atau status berubah menjadi PAID.
     *
     * @param updated objek Loan dengan data terbaru
     */
    public void update(Loan updated) {
        List<Loan> loans = findAll();
        for (int i = 0; i < loans.size(); i++) {
            if (loans.get(i).getLoanId().equals(updated.getLoanId())) {
                loans.set(i, updated);
                break;
            }
        }
        writeToFile(loans);
    }

    /**
     * Menghapus pinjaman berdasarkan ID.
     *
     * @param loanId ID pinjaman yang dihapus
     */
    public void delete(String loanId) {
        List<Loan> loans = findAll();
        loans.removeIf(l -> l.getLoanId().equals(loanId));
        writeToFile(loans);
    }

    /**
     * Menghitung total pinjaman aktif.
     *
     * Dipakai untuk stat card "Pinjaman Aktif" di dashboard Admin.
     *
     * @return jumlah pinjaman aktif
     */
    public int countActive() {
        return findActive().size();
    }

    /**
     * Menghitung total dana pinjaman aktif yang beredar.
     *
     * Dipakai untuk menampilkan total nilai pinjaman di dashboard.
     *
     * @return total nilai pinjaman aktif
     */
    public double getTotalActiveLoanAmount() {
        double total = 0;
        for (Loan l : findActive()) {
            total += l.getPrincipal();
        }
        return total;
    }

    /**
 * Menghitung total pinjaman yang tersimpan.
 *
 * @return jumlah pinjaman
 */
    public int count() {
        return findAll().size();
    }
    
    /**
     * Helper private — tulis List<Loan> ke file CSV.
     *
     * @param loans List Loan yang akan ditulis
     */
    private void writeToFile(List<Loan> loans) {
        List<String> lines = new ArrayList<>();
        for (Loan l : loans) {
            lines.add(l.toString());
        }
        CsvUtil.writeAll(AppConfig.LOANS_FILE, lines);
    }
}
