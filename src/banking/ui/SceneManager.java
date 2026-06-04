/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.ui;

import javafx.stage.Stage;

/**
 * SceneManager mengelola navigasi antar screen di aplikasi.
 *
 * Implementasi SINGLETON — satu instance untuk seluruh aplikasi.
 * Menyimpan referensi ke primaryStage dan mengganti Scene
 * saat navigasi dipanggil.
 *
 * Analogi: SceneManager adalah "sutradara" yang menentukan
 * scene mana yang tampil di "panggung" (Stage) saat ini.
 *
 * Flow navigasi:
 * Login berhasil → showMain()
 * Logout         → showLogin()
 */
public class SceneManager {

    private static volatile SceneManager instance;

    // Referensi ke jendela utama aplikasi
    // Disimpan saat initialize() dipanggil dari Main.java
    private Stage primaryStage;

    private SceneManager() {}

    public static SceneManager getInstance() {
        if (instance == null) {
            synchronized (SceneManager.class) {
                if (instance == null) {
                    instance = new SceneManager();
                }
            }
        }
        return instance;
    }

    /**
     * Inisialisasi SceneManager dengan primaryStage.
     * WAJIB dipanggil sekali dari Main.start() sebelum
     * method navigasi lainnya bisa dipakai.
     *
     * @param stage primaryStage dari JavaFX Application
     */
    public void initialize(Stage stage) {
        this.primaryStage = stage;
    }

    /**
     * Tampilkan LoginScreen.
     * TODO: Uncomment setelah LoginScreen selesai dibuat.
     */
    public void showLogin() {
        if (primaryStage == null) {
            throw new IllegalStateException(
                "SceneManager belum diinisialisasi.");
        }
    banking.ui.screens.LoginScreen loginScreen =
        new banking.ui.screens.LoginScreen();
    primaryStage.setScene(loginScreen.getScene());
    primaryStage.setTitle("NusaBank — Login");
    primaryStage.centerOnScreen();
}

    public void showMain() {
        if (primaryStage == null) {
            throw new IllegalStateException(
                "SceneManager belum diinisialisasi.");
        }
    banking.ui.screens.MainScreen mainScreen =
        new banking.ui.screens.MainScreen();
    primaryStage.setScene(mainScreen.getScene());
    primaryStage.setTitle("NusaBank — Banking Management System");
    primaryStage.centerOnScreen();
}

    public Stage getPrimaryStage() { return primaryStage; }
}