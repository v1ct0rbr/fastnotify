package com.victorqueiroga.fastnotify.origem;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;

public class SenderApp extends Application {
    private Stage stage;
    private SenderView view;
    private ConfigStore config;
    private MessageStore messageStore;
    private DestinosStore destinosStore;
    private TrayIcon trayIcon;

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);
        this.stage = stage;
        java.nio.file.Path configPath = resolveConfigPath("origem.properties");
        this.config = new ConfigStore(configPath);
        this.messageStore = new MessageStore(configPath.resolveSibling("messages.properties"), config);
        this.destinosStore = new DestinosStore(configPath.resolveSibling("destinos.properties"));
        this.view = new SenderView(messageStore, destinosStore);

        stage.setTitle("FastNotify - Origem");
        stage.setScene(new Scene(view, 860, 620));
        stage.setMinWidth(720);
        stage.setMinHeight(480);
        stage.setOnCloseRequest(this::onCloseRequest);

        view.log("Iniciando no tray. Clique no ícone para abrir a janela.");
        view.log("Mensagens: " + messageStore.getFile().toAbsolutePath()
                + " (" + view.getMessageCount() + " carregada(s))");
        view.log("Destinos: " + destinosStore.getFile().toAbsolutePath()
                + " (" + view.getDestinoCount() + " carregado(s))");
        setupTray();
    }

    private void onCloseRequest(WindowEvent e) {
        e.consume();
        CloseDialog.Choice choice = CloseDialog.show(stage, "FastNotify Origem");
        switch (choice) {
            case HIDE -> {
                stage.hide();
                view.log("Janela oculta — reabra pelo tray.");
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
                    TrayIcons.loadAwt("/img/mensagem-enviada.svg"),
                    "FastNotify - Origem");
            trayIcon.setImageAutoSize(true);

            PopupMenu menu = new PopupMenu();

            MenuItem show = new MenuItem("Abrir janela");
            show.addActionListener(e -> showStage());

            MenuItem settings = new MenuItem("Configurações...");
            settings.addActionListener(e -> openConfigDialog());

            MenuItem bell = new MenuItem("🔔 Campainha");
            bell.addActionListener(e -> {
                Platform.runLater(() -> view.sendCampainha());
            });

            MenuItem exit = new MenuItem("Sair");
            exit.addActionListener(e -> exitApp());

            menu.add(show);
            menu.add(settings);
            menu.add(bell);
            menu.addSeparator();
            menu.add(exit);

            trayIcon.setPopupMenu(menu);
            trayIcon.addActionListener(e -> showStage());
            SystemTray.getSystemTray().add(trayIcon);
            trayIcon.displayMessage(
                    "FastNotify - Origem",
                    "Rodando no tray. Duplo clique para abrir.",
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
            showStage();
            ConfigDialog dialog = new ConfigDialog(
                    stage.isShowing() ? stage : null, destinosStore);
            dialog.showAndWait();
            if (dialog.isConfirmed()) {
                view.applyConfigFromDisk();
            }
        });
    }

    private void exitApp() {
        Platform.runLater(() -> {
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

    @Override
    public void stop() {
        if (SystemTray.isSupported() && trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
