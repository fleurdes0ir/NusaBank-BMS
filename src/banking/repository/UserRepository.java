/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.repository;

import banking.model.User;
import banking.model.enums.UserRole;
import banking.util.AppConfig;
import banking.util.CsvUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * UserRepository menangani persistensi data User (akun login).
 *
 * Berbeda dari CustomerRepository — User menyimpan kredensial login
 * (username + password hash) dan role akses, bukan data personal nasabah.
 * Satu Customer bisa punya tepat satu User account.
 *
 * Format CSV: userId,username,passwordHash,role,customerId
 */
public class UserRepository {

    /**
     * Mengambil semua User dari file CSV.
     *
     * Perhatikan parsing UserRole — data di CSV disimpan sebagai String
     * ("ADMIN" / "CUSTOMER"), perlu dikonversi ke enum via UserRole.valueOf().
     *
     * @return List semua objek User
     */
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        List<String[]> rows = CsvUtil.readAll(AppConfig.USERS_FILE);

        for (String[] row : rows) {
            if (row.length < 5) continue;
            try {
                // valueOf() mengkonversi String ke enum
                // Akan throw IllegalArgumentException jika string tidak valid
                UserRole role = UserRole.valueOf(row[3].trim());
                users.add(new User(
                    row[0].trim(), // userId
                    row[1].trim(), // username
                    row[2].trim(), // passwordHash
                    role,          // UserRole enum
                    row[4].trim()  // customerId (kosong jika Admin)
                ));
            } catch (IllegalArgumentException e) {
                // Skip baris dengan role tidak valid daripada crash
                System.err.println("Invalid role in users.csv: " + row[3]);
            }
        }
        return users;
    }

    /**
     * Mencari User berdasarkan username — dipakai saat proses login.
     *
     * @param username username yang dicari
     * @return objek User jika ditemukan, null jika tidak
     */
    public User findByUsername(String username) {
        return findAll().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    /**
     * Mencari User berdasarkan customerId — untuk mapping Customer ke User.
     *
     * @param customerId ID customer
     * @return objek User yang terhubung, null jika tidak ada
     */
    public User findByCustomerId(String customerId) {
        return findAll().stream()
                .filter(u -> u.getCustomerId().equals(customerId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Menyimpan User baru ke CSV.
     *
     * @param user objek User yang akan disimpan
     */
    public void save(User user) {
        List<User> users = findAll();
        users.add(user);
        writeToFile(users);
    }

    /**
     * Memperbarui data User — misalnya saat ganti password.
     *
     * @param updated objek User dengan data terbaru
     */
    public void update(User updated) {
        List<User> users = findAll();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserId().equals(updated.getUserId())) {
                users.set(i, updated);
                break;
            }
        }
        writeToFile(users);
    }

    /**
     * Menghitung total User yang terdaftar.
     *
     * @return jumlah User
     */
    public int count() {
        return findAll().size();
    }

    /**
     * Helper private — tulis List<User> ke file CSV.
     *
     * @param users List User yang akan ditulis
     */
    private void writeToFile(List<User> users) {
        List<String> lines = new ArrayList<>();
        for (User u : users) {
            lines.add(u.toString());
        }
        CsvUtil.writeAll(AppConfig.USERS_FILE, lines);
    }
}