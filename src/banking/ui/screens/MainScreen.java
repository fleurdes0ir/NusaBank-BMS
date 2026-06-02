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

/**
 * MainScreen adalah screen utama setelah login berhasil.
 *
 * Struktur layout (Admin):
 * BorderPane (root)
 *   ├── TOP    : TopBar (nama user, role badge, theme toggle, logout)
 *   ├── LEFT   : Sidebar (navigasi menu)
 *   └── CENTER : ContentArea (panel yang aktif)
 *
 * Struktur layout (Nasabah):
 * BorderPane (root)
 *   └── CENTER : NasabahPanel (single panel, tidak ada sidebar)
 *
 * TODO: Uncomment panel references setelah semua panel selesai dibuat.
 */
public class MainScreen {

    private final Scene scene;
    private final StackPane contentArea = new StackPane();
    private Button activeButton = null;

    public MainScreen() {
        scene = buildScene();
        ThemeManager.getInstance().registerScene(scene);
    }

    private Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setPrefSize(1100, 700);

        boolean isAdmin = AuthService.getInstance().isAdmin();

        if (isAdmin) {
            root.setTop(buildTopBar());
            root.setLeft(buildSidebar());
            root.setCenter(contentArea);
            BorderPane.setMargin(contentArea, new Insets(16, 16, 16, 16));

            // TODO: Uncomment setelah DashboardPanel selesai
            // showPanel(new banking.ui.panels.DashboardPanel());

            // Placeholder sementara
            Label placeholder = new Label("Panels sedang dibangun...");
            placeholder.getStyleClass().add("label-subtitle");
            contentArea.getChildren().add(placeholder);

        } else {
            // TODO: Uncomment setelah NasabahPanel selesai
            // root.setCenter(
            //     new banking.ui.panels.NasabahPanel().getRoot());

            Label placeholder = new Label("Nasabah Panel sedang dibangun...");
            placeholder.getStyleClass().add("label-subtitle");
            placeholder.setAlignment(Pos.CENTER);
            root.setCenter(placeholder);
        }

        return new Scene(root, 1100, 700);
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox();
        topBar.getStyleClass().add("topbar");
        topBar.setPrefHeight(56);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 16, 0, 16));

        Label logo = new Label("NusaBank");
        logo.setStyle(
            "-fx-font-size: 18px; -fx-font-weight: bold; " +
            "-fx-text-fill: #4f8ef7;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String username = AuthService.getInstance()
                .getCurrentUser().getUsername();
        Label userLabel = new Label(username);
        userLabel.getStyleClass().add("label-subtitle");

        Label roleLabel = new Label("Administrator");
        roleLabel.getStyleClass().add("badge-active");
        roleLabel.setPadding(new Insets(2, 10, 2, 10));

        Button themeBtn = new Button(
            ThemeManager.getInstance().isDarkMode() ? "🌙" : "☀️");
        themeBtn.getStyleClass().add("btn-secondary");
        themeBtn.setStyle("-fx-font-size: 14px; -fx-padding: 4 10 4 10;");
        themeBtn.setOnAction(e -> {
            ThemeManager.getInstance().toggleTheme();
            themeBtn.setText(ThemeManager.getInstance().isDarkMode()
                ? "🌙" : "☀️");
        });

        Button logoutBtn = new Button("⬡ Keluar");
        logoutBtn.getStyleClass().add("btn-secondary");
        logoutBtn.setOnAction(e -> {
            AuthService.getInstance().logout();
            SceneManager.getInstance().showLogin();
        });

        HBox rightBox = new HBox(12, userLabel, roleLabel,
                themeBtn, logoutBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        topBar.getChildren().addAll(logo, spacer, rightBox);
        return topBar;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(4);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(200);
        sidebar.setPadding(new Insets(16, 12, 16, 12));

        Label menuLabel = new Label("MENU");
        menuLabel.getStyleClass().add("label-section");
        menuLabel.setPadding(new Insets(8, 8, 8, 8));

        Button dashboardBtn   = createSidebarButton("⊞  Dashboard");
        Button customerBtn    = createSidebarButton("⚇  Nasabah");
        Button accountBtn     = createSidebarButton("▣  Rekening");
        Button transactionBtn = createSidebarButton("⇄  Transaksi");
        Button loanBtn        = createSidebarButton("▤  Pinjaman");

        setActiveButton(dashboardBtn);

        // TODO: Uncomment setelah panel selesai dibuat
        dashboardBtn.setOnAction(e -> {
            // showPanel(new banking.ui.panels.DashboardPanel());
            setActiveButton(dashboardBtn);
        });
        customerBtn.setOnAction(e -> {
            // showPanel(new banking.ui.panels.CustomerPanel());
            setActiveButton(customerBtn);
        });
        accountBtn.setOnAction(e -> {
            // showPanel(new banking.ui.panels.AccountPanel());
            setActiveButton(accountBtn);
        });
        transactionBtn.setOnAction(e -> {
            // showPanel(new banking.ui.panels.TransactionPanel());
            setActiveButton(transactionBtn);
        });
        loanBtn.setOnAction(e -> {
            // showPanel(new banking.ui.panels.LoanPanel());
            setActiveButton(loanBtn);
        });

        sidebar.getChildren().addAll(
            menuLabel, dashboardBtn, customerBtn,
            accountBtn, transactionBtn, loanBtn
        );
        return sidebar;
    }

    private Button createSidebarButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPrefHeight(40);
        return btn;
    }

    private void showPanel(javafx.scene.Node panel) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(panel);
    }

    private void setActiveButton(Button btn) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("sidebar-item-active");
            if (!activeButton.getStyleClass().contains("sidebar-item")) {
                activeButton.getStyleClass().add("sidebar-item");
            }
        }
        btn.getStyleClass().add("sidebar-item-active");
        activeButton = btn;
    }

    public Scene getScene() { return scene; }
}