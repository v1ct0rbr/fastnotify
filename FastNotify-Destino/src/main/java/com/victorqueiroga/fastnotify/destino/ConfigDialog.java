package com.victorqueiroga.fastnotify.destino;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

public class ConfigDialog extends Dialog<Boolean> {
    private final Spinner<Integer> portSpinner;
    private final TextField tokenField = new TextField();
    private final TextField pskField = new TextField();
    private final CheckBox soundCheck = new CheckBox("Som de alerta ao abrir notificação");
    private final Spinner<Integer> retentionSpinner;
    private final ConfigStore config;
    private final int originalRetentionDays;
    private boolean confirmed;
    private boolean portChanged;
    private boolean retentionChanged;

    public ConfigDialog(Window owner, ConfigStore config) {
        this.config = config;
        initOwner(owner);
        initModality(javafx.stage.Modality.WINDOW_MODAL);
        setTitle("Configurações - FastNotify Destino");
        setHeaderText("Estas opções são gravadas em:\n" + config.getFile().toAbsolutePath());

        int originalPort = config.getPort();
        portSpinner = new Spinner<>(1, 65535, originalPort);
        portSpinner.setEditable(true);
        tokenField.setText(config.getToken());
        tokenField.setPromptText("token compartilhado com a origem");
        pskField.setText(config.getPsk());
        pskField.setPromptText("vazia = usa o token como PSK");
        soundCheck.setSelected(config.isSoundEnabled());
        this.originalRetentionDays = config.getLogRetentionDays();
        retentionSpinner = new Spinner<>(0, 3650, originalRetentionDays);
        retentionSpinner.setEditable(true);
        retentionSpinner.setPrefWidth(140);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));
        grid.add(new Label("Porta:"), 0, 0);
        grid.add(portSpinner, 1, 0);
        grid.add(new Label("Token:"), 0, 1);
        grid.add(tokenField, 1, 1);
        grid.add(new Label("PSK (AES-GCM):"), 0, 2);
        grid.add(pskField, 1, 2);
        Label pskInfo = new Label(
                "PSK opcional (mesma da Origem). Vazia = o token de notificação/cadastro "
                        + "já usado vira a chave de cifra automaticamente.");
        pskInfo.setWrapText(true);
        pskInfo.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        grid.add(pskInfo, 1, 3);
        grid.add(soundCheck, 0, 4);
        grid.setColumnSpan(soundCheck, 2);
        grid.add(new Label("Retenção de logs (dias):"), 0, 5);
        grid.add(retentionSpinner, 1, 5);
        Label retentionInfo = new Label(
                "Exclui logs-AAAA-MM-DD.log com data anterior a hoje − N dias. 0 = manter todos.");
        retentionInfo.setWrapText(true);
        retentionInfo.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        grid.add(retentionInfo, 1, 6);

        Label firewallStatus = new Label();
        firewallStatus.setWrapText(true);
        firewallStatus.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        Button firewallBtn = new Button("Verificar firewall");
        firewallBtn.setTooltip(new javafx.scene.control.Tooltip(
                "Consulta se a regra de entrada da porta de notificação existe/está ativa"));
        firewallBtn.setOnAction(e -> {
            int p = portSpinner.getValue();
            firewallStatus.setText("Consultando porta " + p + " ...");
            Thread t = new Thread(() -> {
                String status = FirewallChecker.checkInboundRule(
                        "FastNotify-Destino-Notificacao", p);
                javafx.application.Platform.runLater(() -> firewallStatus.setText(status));
            }, "fastnotify-firewall-check");
            t.setDaemon(true);
            t.start();
        });
        Button liberarBtn = new Button("Liberar porta");
        liberarBtn.setTooltip(new javafx.scene.control.Tooltip(
                "Executa liberar-porta-destino.bat (admin) — lê port de destino.properties"));
        liberarBtn.setOnAction(e -> {
            config.setPort(portSpinner.getValue());
            config.setToken(tokenField.getText().trim());
            config.setPsk(pskField.getText() == null ? "" : pskField.getText().trim());
            config.setSoundEnabled(soundCheck.isSelected());
            config.save();
            firewallStatus.setText("Abrindo elevação do Windows para liberar a porta...");
            Thread t = new Thread(() -> {
                try {
                    FirewallScripts.liberarPorta(config.getFile().getParent());
                    javafx.application.Platform.runLater(() -> firewallStatus.setText(
                            "Elevação aberta — porta lida de "
                                    + config.getFile().getFileName() + "."));
                } catch (RuntimeException ex) {
                    javafx.application.Platform.runLater(() ->
                            firewallStatus.setText(ex.getMessage()));
                }
            }, "fastnotify-firewall-libera");
            t.setDaemon(true);
            t.start();
        });
        Button removerRegraBtn = new Button("Remover regra");
        removerRegraBtn.setTooltip(new javafx.scene.control.Tooltip(
                "Executa remover-porta-destino.bat (admin) — remove a regra se existir"));
        removerRegraBtn.setOnAction(e -> {
            firewallStatus.setText("Abrindo elevação do Windows para remover a regra...");
            Thread t = new Thread(() -> {
                try {
                    FirewallScripts.removerPorta(config.getFile().getParent());
                    javafx.application.Platform.runLater(() -> firewallStatus.setText(
                            "Elevação aberta — remoção da regra FastNotify-Destino-Notificacao."));
                } catch (RuntimeException ex) {
                    javafx.application.Platform.runLater(() ->
                            firewallStatus.setText(ex.getMessage()));
                }
            }, "fastnotify-firewall-remove");
            t.setDaemon(true);
            t.start();
        });
        javafx.scene.layout.HBox fwRow = new javafx.scene.layout.HBox(8,
                firewallBtn, liberarBtn, removerRegraBtn, firewallStatus);
        fwRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        grid.add(fwRow, 0, 7);
        grid.setColumnSpan(fwRow, 2);

        Label startupStatus = new Label();
        startupStatus.setWrapText(true);
        startupStatus.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        Button installStartupBtn = new Button("Inicializar com Windows");
        installStartupBtn.setTooltip(new javafx.scene.control.Tooltip(
                "Executa instalar-inicializacao-destino.ps1 (HKCU Run, sem admin)"));
        installStartupBtn.setOnAction(e -> runStartup(
                FirewallScripts::instalarInicializacao, startupStatus,
                "Executando instalar-inicializacao-destino..."));
        Button removeStartupBtn = new Button("Remover inicialização");
        removeStartupBtn.setTooltip(new javafx.scene.control.Tooltip(
                "Executa remover-inicializacao-destino.ps1 (HKCU Run, sem admin)"));
        removeStartupBtn.setOnAction(e -> runStartup(
                FirewallScripts::removerInicializacao, startupStatus,
                "Executando remover-inicializacao-destino..."));
        javafx.scene.layout.HBox startRow = new javafx.scene.layout.HBox(8,
                installStartupBtn, removeStartupBtn, startupStatus);
        startRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        grid.add(startRow, 0, 8);
        grid.setColumnSpan(startRow, 2);

        getDialogPane().setContent(grid);
        getDialogPane().setPrefWidth(560);
        getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.CANCEL,
                javafx.scene.control.ButtonType.OK);

        Button ok = (Button) getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        ok.setText("Salvar");
        Button cancel = (Button) getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancel.setText("Cancelar");

        ok.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            portChanged = portSpinner.getValue() != originalPort;
            int retention = retentionSpinner.getValue();
            if (retention < 0) {
                retention = originalRetentionDays;
            }
            retentionChanged = retention != originalRetentionDays;
            config.setPort(portSpinner.getValue());
            config.setToken(tokenField.getText().trim());
            config.setPsk(pskField.getText() == null ? "" : pskField.getText().trim());
            config.setSoundEnabled(soundCheck.isSelected());
            config.setLogRetentionDays(retention);
            config.save();
            confirmed = true;
        });

        setResultConverter(b -> confirmed);
    }

    private void runStartup(Runnable action, Label status, String runningText) {
        status.setText(runningText);
        Thread t = new Thread(() -> {
            try {
                action.run();
                javafx.application.Platform.runLater(() ->
                        status.setText("Script de inicialização iniciado."));
            } catch (RuntimeException ex) {
                javafx.application.Platform.runLater(() -> status.setText(ex.getMessage()));
            }
        }, "fastnotify-startup-script");
        t.setDaemon(true);
        t.start();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isPortChanged() {
        return portChanged;
    }

    public boolean isRetentionChanged() {
        return retentionChanged;
    }
}
