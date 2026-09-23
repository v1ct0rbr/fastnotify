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
    private RegistrationListener registrationListener;
    private SingleInstanceLock instanceLock;
    private LogService logService;
    private LogViewer logViewer;

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);
        this.stage = stage;
        java.nio.file.Path configPath = resolveConfigPath("origem.properties");
        if (!acquireSingleInstanceLock(configPath)) {
            Platform.setImplicitExit(true);
            Platform.exit();
            return;
        }
        this.config = new ConfigStore(configPath);
        this.messageStore = new MessageStore(configPath.resolveSibling("messages.properties"), config);
        this.destinosStore = new DestinosStore(configPath.resolveSibling("destinos.properties"));
        this.logService = new LogService(
                resolveLogsDir(configPath), config.getLogRetentionDays());
        UserNames.warmUp();
        this.logViewer = new LogViewer(logService);
        this.view = new SenderView(messageStore, destinosStore, config, logService);
        this.view.setOpenSettingsAction(this::openConfigDialog);

        stage.setTitle("FastNotify - Origem");
        stage.setScene(new Scene(view, 860, 620));
        stage.setMinWidth(720);
        stage.setMinHeight(480);
        stage.setOnCloseRequest(this::onCloseRequest);
        TrayIcons.applyWindowIcons(stage, "/img/mensagem-enviada.svg");

        view.log("Iniciando no tray. Clique no ícone para abrir a janela.");
        view.log("Mensagens: " + messageStore.getFile().toAbsolutePath()
                + " (" + view.getMessageCount() + " carregada(s))");
        view.log("Destinos: " + destinosStore.getFile().toAbsolutePath()
                + " (" + view.getDestinoCount() + " carregado(s))");
        registrationListener = new RegistrationListener(
                config::getRegisterPort, config::getRegisterToken,
                () -> config.effectivePsk(config.getRegisterToken()),
                view, destinosStore);
        registrationListener.start();
        view.setRegisterStatus("Cadastro: porta " + config.getRegisterPort());
        setupTray();
    }

    private boolean acquireSingleInstanceLock(java.nio.file.Path configPath) {
        java.nio.file.Path lockFile = configPath.resolveSibling("origem.lock");
        try {
            instanceLock = SingleInstanceLock.tryLock(lockFile);
        } catch (Exception ex) {
            showAlreadyRunningDialog("Não foi possível criar a trava de instância:\n"
                    + ex.getMessage());
            return false;
        }
        if (instanceLock == null) {
            showAlreadyRunningDialog(
                    "FastNotify Origem já está em execução nesta máquina.\n\n"
                            + "Feche a instância atual (tray → Sair) e abra de novo.");
            return false;
        }
        return true;
    }

    private void showAlreadyRunningDialog(String message) {
        try {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("FastNotify - Origem");
            alert.setHeaderText("Instância já em execução");
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception ignored) {
            System.err.println(message);
        }
    }

    private void releaseSingleInstanceLock() {
        if (instanceLock != null) {
            instanceLock.close();
            instanceLock = null;
        }
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

            MenuItem logs = new MenuItem("Ver logs...");
            logs.addActionListener(e -> openLogs());

            MenuItem bell = new MenuItem("🔔 Campainha");
            bell.addActionListener(e -> {
                Platform.runLater(() -> view.sendCampainha());
            });

            MenuItem exit = new MenuItem("Sair");
            exit.addActionListener(e -> exitApp());

            menu.add(show);
            menu.add(settings);
            menu.add(logs);
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
                    stage.isShowing() ? stage : null, destinosStore, config);
            dialog.showAndWait();
            if (dialog.isConfirmed()) {
                view.applyConfigFromDisk();
                if (logService != null && dialog.isRetentionChanged()) {
                    int deleted = logService.setRetentionDays(config.getLogRetentionDays());
                    view.logAs(LogType.CONFIG, "-",
                            "Retenção de logs = " + config.getLogRetentionDays()
                                    + " dia(s); excluído(s): " + deleted);
                }
                if (dialog.isRegisterPortChanged() && registrationListener != null) {
                    registrationListener.restart();
                    view.setRegisterStatus("Cadastro: porta " + config.getRegisterPort());
                }
            }
        });
    }

    private void openLogs() {
        Platform.runLater(() -> logViewer.show(stage.isShowing() ? stage : null));
    }

    private void exitApp() {
        Platform.runLater(() -> {
            if (registrationListener != null) {
                registrationListener.stop();
            }
            if (logService != null) {
                int flushed = logService.flush();
                if (flushed > 0 && view != null) {
                    view.log("Logs gravados ao sair: " + flushed + " linha(s).");
                }
                logService.close();
            }
            if (SystemTray.isSupported() && trayIcon != null) {
                SystemTray.getSystemTray().remove(trayIcon);
                trayIcon = null;
            }
            releaseSingleInstanceLock();
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

    static java.nio.file.Path resolveLogsDir(java.nio.file.Path configPath) {
        String override = System.getProperty("fastnotify.logs");
        if (override != null && !override.isBlank()) {
            return java.nio.file.Path.of(override);
        }
        java.nio.file.Path parent = configPath.getParent();
        if (parent != null && parent.getParent() != null) {
            return parent.getParent().resolve("logs");
        }
        if (parent != null) {
            return parent.resolve("logs");
        }
        return java.nio.file.Path.of("logs");
    }

    @Override
    public void stop() {
        if (registrationListener != null) {
            registrationListener.stop();
            registrationListener = null;
        }
        if (logService != null) {
            logService.close();
            logService = null;
        }
        if (SystemTray.isSupported() && trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
        releaseSingleInstanceLock();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
