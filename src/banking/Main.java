/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package banking;

import banking.service.AuthService;
import banking.service.BankService;
import banking.util.DataSeeder;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main adalah entry point aplikasi NusaBank.
 *
 * Extends Application — wajib untuk semua aplikasi JavaFX.
 * JavaFX memiliki lifecycle tersendiri:
 *
 * 1. main()  → memanggil launch() yang menginisialisasi JavaFX runtime
 * 2. init()  → dipanggil sebelum UI dibuat (non-UI thread)
 *              cocok untuk inisialisasi data, seeding, dsb
 * 3. start() → dipanggil setelah init(), di JavaFX Application Thread
 *              di sinilah Stage (window) pertama dibuat dan ditampilkan
 * 4. stop()  → dipanggil saat aplikasi ditutup
 *              cocok untuk cleanup, save state, dsb
 */
public class Main extends Application {

    /**
     * init() dipanggil otomatis oleh JavaFX sebelum start().
     * Berjalan di background thread — JANGAN buat UI di sini.
     *
     * Kita pakai untuk:
     * 1. Inisialisasi Singleton services
     * 2. Menjalankan DataSeeder untuk data awal
     *
     * @throws Exception jika inisialisasi gagal
     */
    @Override
    public void init() throws Exception {
        System.out.println("NusaBank starting...");

        // Inisialisasi Singleton — memastikan instance sudah siap
        // sebelum UI pertama kali dirender
        BankService.getInstance();
        AuthService.getInstance();

        // Jalankan seeder — akan skip jika data sudah ada
        new DataSeeder().seed();

        System.out.println("Initialization complete.");
    }

    /**
     * start() adalah method utama JavaFX — dipanggil setelah init().
     * Berjalan di JavaFX Application Thread.
     *
     * Stage = jendela utama aplikasi (analoginya: panggung teater)
     * Scene = konten yang ditampilkan di dalam Stage
     *
     * Untuk sekarang kita setup Stage dasar dulu —
     * SceneManager dan LoginScreen akan dibuat di UI layer.
     *
     * @param primaryStage jendela utama yang disediakan JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
    primaryStage.setTitle("NusaBank — Banking Management System");
    primaryStage.setWidth(1100);
    primaryStage.setHeight(700);
    primaryStage.setMinWidth(900);
    primaryStage.setMinHeight(600);

    // Inisialisasi SceneManager
    banking.ui.SceneManager.getInstance().initialize(primaryStage);

    // Tampilkan LoginScreen
    banking.ui.SceneManager.getInstance().showLogin();

    primaryStage.show();
    System.out.println("NusaBank started successfully.");
}

    /**
     * stop() dipanggil otomatis saat aplikasi ditutup.
     * Cocok untuk cleanup resources — koneksi database, file handles, dsb.
     * Untuk project ini tidak ada cleanup khusus yang dibutuhkan.
     */
    @Override
    public void stop() {
        System.out.println("NusaBank shutting down...");
    }

    /**
     * main() adalah entry point Java standar.
     *
     * launch() adalah static method dari Application yang:
     * 1. Menginisialisasi JavaFX runtime
     * 2. Membuat instance Main
     * 3. Memanggil init() → start() secara berurutan
     *
     * @param args command line arguments (tidak dipakai)
     */
    public static void main(String[] args) {
        launch(args);
    }
}