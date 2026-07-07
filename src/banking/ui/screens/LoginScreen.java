/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.ui.screens;

import banking.service.AuthService;
import banking.ui.SceneManager;
import banking.ui.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

/**
 * LoginScreen adalah screen pertama yang ditampilkan saat aplikasi dibuka.
 *
 * Struktur layout:
 * StackPane (root, fullscreen)
 *   └── VBox (center card)
 *         ├── Label "NusaBank" (logo/title)
 *         ├── Label subtitle
 *         ├── TextField username
 *         ├── PasswordField password
 *         ├── Label error (hidden by default)
 *         └── Button login
 *
 * Pattern yang dipakai:
 * - Scene dibuat di constructor, diakses via getScene()
 * - AuthService.getInstance() untuk validasi login
 * - SceneManager.getInstance().showMain() untuk navigasi setelah login
 */
public class LoginScreen {

    // Scene yang akan di-set ke Stage oleh SceneManager
    private final Scene scene;

    public LoginScreen() {
        scene = buildScene();
        // Daftarkan scene ke ThemeManager agar tema aktif ter-apply
        ThemeManager.getInstance().registerScene(scene);
    }

    /**
     * Membangun seluruh layout LoginScreen.
     *
     * @return Scene yang siap ditampilkan
     */
    private Scene buildScene() {
        // ── Root Container ──
        // StackPane sebagai root agar card bisa di-center secara absolut
        StackPane root = new StackPane();
        root.getStyleClass().add("login-container");
        root.setPrefSize(1100, 700);

        // ── Login Card ──
        // VBox sebagai container form — layout vertikal
        VBox card = new VBox(16);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(400);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.CENTER_LEFT);

        // ── Logo / Title ──
        Label logoLabel = new Label("NusaBank");
        logoLabel.setStyle(
            "-fx-font-size: 28px; -fx-font-weight: bold; " +
            "-fx-text-fill: #4f8ef7;"
        );

        Label subtitleLabel = new Label(
            "Selamat datang. Silakan login untuk melanjutkan.");
        subtitleLabel.getStyleClass().add("label-subtitle");
        subtitleLabel.setWrapText(true);

        // ── Separator visual ──
        Region spacer = new Region();
        spacer.setPrefHeight(8);

        // ── Username Field ──
        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("label-subtitle");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Masukkan username");
        usernameField.setPrefHeight(40);
        usernameField.setMaxWidth(Double.MAX_VALUE);

        // ── Password Field ──
        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("label-subtitle");

        // PasswordField otomatis menyembunyikan karakter input
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Masukkan password");
        passwordField.setPrefHeight(40);
        passwordField.setMaxWidth(Double.MAX_VALUE);

        // ── Error Label — hidden by default ──
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        // ── Login Button ──
        Button loginBtn = new Button("Login");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setPrefHeight(42);
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        // ── Demo credentials hint ──
        Label hintLabel = new Label(
            "Demo: admin / admin123  |  budi.santoso / nasabah123");
        hintLabel.setStyle(
            "-fx-text-fill: #555d74; -fx-font-size: 11px;");
        hintLabel.setAlignment(Pos.CENTER);
        hintLabel.setMaxWidth(Double.MAX_VALUE);

        // ── Theme toggle button ──
        Button themeBtn = new Button("🌙 Dark Mode");
        themeBtn.getStyleClass().add("btn-secondary");
        themeBtn.setStyle("-fx-font-size: 11px;");

        // ── Event Handlers ──

        // Login button action
        loginBtn.setOnAction(e -> handleLogin(
            usernameField, passwordField, errorLabel));

        // Enter key di password field = klik login
        // Ini UX improvement — user tidak perlu klik tombol
        passwordField.setOnAction(e -> handleLogin(
            usernameField, passwordField, errorLabel));

        // Enter key di username field = pindah fokus ke password
        usernameField.setOnAction(e -> passwordField.requestFocus());

        // Theme toggle
        themeBtn.setOnAction(e -> {
            ThemeManager.getInstance().toggleTheme();
            // Update label tombol sesuai tema aktif
            themeBtn.setText(ThemeManager.getInstance().isDarkMode()
                ? "🌙 Dark Mode" : "☀️ Light Mode");
        });

        // ── Assemble Card ──
        card.getChildren().addAll(
            logoLabel,
            subtitleLabel,
            spacer,
            usernameLabel,
            usernameField,
            passwordLabel,
            passwordField,
            errorLabel,
            loginBtn,
            hintLabel,
            themeBtn
        );

        // Center card di dalam root StackPane
        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().add(card);

        return new Scene(root, 1100, 700);
    }

    /**
     * Menangani logika login saat tombol ditekan.
     *
     * Alur:
     * 1. Ambil input dari field
     * 2. Validasi tidak kosong
     * 3. Panggil AuthService.login()
     * 4. Jika berhasil → navigasi ke MainScreen
     * 5. Jika gagal → tampilkan pesan error
     *
     * @param usernameField field username
     * @param passwordField field password
     * @param errorLabel    label untuk menampilkan error
     */
    /**
     * Menangani logika login saat tombol ditekan.
     */
    private void handleLogin(TextField usernameField,
            PasswordField passwordField, Label errorLabel) {

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validasi input tidak kosong
        if (username.isEmpty() || password.isEmpty()) {
            showError(errorLabel, "Username dan password tidak boleh kosong.");
            return;
        }

        // Panggil AuthService untuk validasi kredensial
        boolean success = AuthService.getInstance().login(username, password);

        if (success) {
            // ── HUBUNGKAN PEMICU POP-UP ALERT DI SINI ──
            try {
                // Pastikan yang login saat ini adalah Nasabah (bukan Admin)
                if (AuthService.getInstance().getCurrentCustomer() != null) {
                    String customerId = AuthService.getInstance().getCurrentCustomer().getCustomerId();
                    
                    // Ambil Stage utama JavaFX secara dinamis dari root window scene aktif
                    javafx.stage.Stage mainStage = (javafx.stage.Stage) scene.getWindow();
                    
                    // Panggil paksa pengecekan alert agar pop-up notifikasi melompat keluar
                    banking.service.AlertService.showAlertsIfAny(mainStage, customerId);
                }
            } catch (Exception ex) {
                System.err.println("[DEBUG ERROR] Gagal memuat pop-up notifikasi saat login: " + ex.getMessage());
            }
            // ──────────────────────────────────────────

            // Login berhasil — navigasi ke MainScreen
            SceneManager.getInstance().showMain();
        } else {
            // Login gagal — tampilkan error, kosongkan password
            showError(errorLabel,
                "Username atau password salah. Silakan coba lagi.");
            passwordField.clear();
            passwordField.requestFocus();
        }
    }

    /**
     * Helper — tampilkan pesan error di errorLabel.
     *
     * @param errorLabel label target
     * @param message    pesan error
     */
    private void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /**
     * Getter untuk Scene — dipanggil oleh SceneManager.
     *
     * @return Scene LoginScreen
     */
    public Scene getScene() { return scene; }
}