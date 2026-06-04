/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.ui;

import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.List;

/**
 * ThemeManager mengelola state tema (Dark/Light) secara global.
 *
 * Implementasi SINGLETON — satu instance untuk seluruh aplikasi.
 * Semua Scene yang terdaftar akan di-update serentak saat tema berubah.
 *
 * Cara kerja tema di JavaFX:
 * - Setiap Scene punya list stylesheet (.css)
 * - Kita swap stylesheet dark/light saat toggle dipanggil
 * - CSS variables di theme.css menentukan warna seluruh komponen
 */
public class ThemeManager {

    private static volatile ThemeManager instance;

    // Path ke file CSS — menggunakan getClass().getResource() di JavaFX
    // format: /package/path/file.css
    public static final String DARK_THEME  =
            "/banking/ui/styles/theme-dark.css";
    public static final String LIGHT_THEME =
            "/banking/ui/styles/theme-light.css";

    // State tema aktif — default Dark
    private boolean isDarkMode = true;

    // Daftar semua Scene yang perlu di-update saat tema berubah
    // Setiap screen mendaftarkan dirinya saat dibuat
    private final List<Scene> registeredScenes = new ArrayList<>();

    private ThemeManager() {}

    public static ThemeManager getInstance() {
        if (instance == null) {
            synchronized (ThemeManager.class) {
                if (instance == null) {
                    instance = new ThemeManager();
                }
            }
        }
        return instance;
    }

    /**
     * Mendaftarkan Scene agar ikut ter-update saat tema berubah.
     * Dipanggil oleh setiap Screen saat pertama kali dibuat.
     *
     * @param scene Scene yang akan didaftarkan
     */
    public void registerScene(Scene scene) {
        if (!registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
        }
        // Apply tema aktif saat ini ke scene yang baru didaftarkan
        applyTheme(scene);
    }

    /**
     * Toggle antara Dark dan Light mode.
     * Semua Scene yang terdaftar akan diperbarui serentak.
     */
    public void toggleTheme() {
        isDarkMode = !isDarkMode;
        for (Scene scene : registeredScenes) {
            applyTheme(scene);
        }
    }

    /**
     * Apply tema ke satu Scene — swap stylesheet.
     *
     * getStylesheets() mengembalikan ObservableList<String> —
     * kita clear dulu lalu add stylesheet yang sesuai.
     *
     * @param scene Scene yang akan di-apply tema
     */
    private void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        String css = isDarkMode ? DARK_THEME : LIGHT_THEME;

        // getResource() mencari file di classpath
        // toExternalForm() konversi URL ke String yang bisa dipakai JavaFX
        var resource = getClass().getResource(css);
        if (resource != null) {
            scene.getStylesheets().add(resource.toExternalForm());
        } else {
            System.err.println("CSS not found: " + css);
        }
    }

    /**
     * Apply tema ke Scene tanpa mendaftarkannya.
     * Dipakai untuk Scene sementara atau dialog.
     *
     * @param scene Scene tujuan
     */
    public void applyCurrentTheme(Scene scene) {
        applyTheme(scene);
    }

    public boolean isDarkMode() { return isDarkMode; }

    /**
     * Hapus Scene dari daftar — dipanggil saat Screen di-destroy.
     *
     * @param scene Scene yang dihapus dari daftar
     */
    public void unregisterScene(Scene scene) {
        registeredScenes.remove(scene);
    }
}