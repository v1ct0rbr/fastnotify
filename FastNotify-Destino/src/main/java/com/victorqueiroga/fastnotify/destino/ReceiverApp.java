package com.victorqueiroga.fastnotify.destino;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;

public class ReceiverApp extends Application {
    private Stage stage;
    private ReceiverView view;
    private ConfigStore config;
    private LogService logService;
    private LogViewer logViewer;
    private TrayIcon trayIcon;

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);
        this.stage = stage;
        this.config = new ConfigStore(resolveConfigPath("destino.properties"));
        this.logService = new LogService(resolveLogsDir());
        this.logViewer = new LogViewer(logService);
        this.view = new ReceiverView(config, logService);

        stage.setTitle("FastNotify - Destino");
        stage.setScene(new Scene(view, 620, 440));
        stage.setMinWidth(520);
        stage.setMinHeight(360);
        stage.setOnCloseRequest(this::onCloseRequest);

        view.startListener();
        view.log("Iniciando no tray. Escuta ativa; clique no ícone para abrir a janela.");
        view.log("Config: " + config.getFile().toAbsolutePath());
        setupTray();
    }

    private void onCloseRequest(WindowEvent e) {
        e.consume();
        CloseDialog.Choice choice = CloseDialog.show(stage, "FastNotify Destino");
        switch (choice) {
            case HIDE -> {
                stage.hide();
                view.log("Janela oculta — escuta continua; reabra pelo tray.");
            }
            case EXIT -> exitApp();
            case CANCEL -> {
            }
        }
    }

    private void setupTray() {
        if (!SystemTray.isSupported()) {
            view.log("System tray não suportado — abrindo janela.");
            showStage();
            return;
        }
        try {
            trayIcon = new TrayIcon(
                    TrayIcons.loadAwt("/img/mensagem-recebida.svg"),
                    "FastNotify - Destino");
            trayIcon.setImageAutoSize(true);

            PopupMenu menu = new PopupMenu();

            MenuItem show = new MenuItem("Abrir janela");
            show.addActionListener(e -> showStage());

            MenuItem settings = new MenuItem("Configurações...");
            settings.addActionListener(e -> openConfigDialog());

            MenuItem logs = new MenuItem("Ver logs...");
            logs.addActionListener(e -> openLogs());

            MenuItem exit = new MenuItem("Sair");
            exit.addActionListener(e -> exitApp());

            menu.add(show);
            menu.add(settings);
            menu.add(logs);
            menu.addSeparator();
            menu.add(exit);

            trayIcon.setPopupMenu(menu);
            trayIcon.addActionListener(e -> showStage());
            SystemTray.getSystemTray().add(trayIcon);
            trayIcon.displayMessage(
                    "FastNotify - Destino",
                    "Escutando no tray. Duplo clique para abrir.",
                    TrayIcon.MessageType.INFO);
        } catch (Exception ex) {
            view.log("Falha ao criar tray: " + ex.getMessage());
            view.log("Abrindo janela (tray indisponível).");
            showStage();
        }
    }

    private void showStage() {
        Platform.runLater(() -> {
            stage.show();
            stage.toFront();
            stage.requestFocus();
        });
    }

    private void openConfigDialog() {
        Platform.runLater(() -> {
            ConfigDialog dialog = new ConfigDialog(
                    stage.isShowing() ? stage : null, config);
            dialog.showAndWait();
            if (dialog.isConfirmed()) {
                view.applyConfigFromDisk(dialog.isPortChanged());
                view.log("Configurações atualizadas via tray → "
                        + config.getFile().toAbsolutePath());
            }
        });
    }

    private void openLogs() {
        Platform.runLater(() -> logViewer.show(stage.isShowing() ? stage : null));
    }

    private void exitApp() {
        Platform.runLater(() -> {
            if (logService != null) {
                int flushed = logService.flush();
                if (flushed > 0) {
                    view.log("Logs gravados ao sair: " + flushed + " linha(s).");
                }
                logService.close();
            }
            if (SystemTray.isSupported() && trayIcon != null) {
                SystemTray.getSystemTray().remove(trayIcon);
                trayIcon = null;
            }
            Platform.setImplicitExit(true);
            Platform.exit();
        });
    }

    static java.nio.file.Path resolveConfigPath(String fileName) {
        String override = System.getProperty("fastnotify.config");
        if (override != null && !override.isBlank()) {
            return java.nio.file.Path.of(override);
        }
        return java.nio.file.Path.of("config", fileName);
    }

    static java.nio.file.Path resolveLogsDir() {
        String override = System.getProperty("fastnotify.logs");
        if (override != null && !override.isBlank()) {
            return java.nio.file.Path.of(override);
        }
        java.nio.file.Path configPath = resolveConfigPath("destino.properties");
        java.nio.file.Path parent = configPath.getParent();
        if (parent != null && parent.getParent() != null) {
            return parent.getParent().resolve("logs");
        }
        return java.nio.file.Path.of("logs");
    }

    @Override
    public void stop() {
        if (logService != null) {
            logService.close();
        }
        if (SystemTray.isSupported() && trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
