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

        getDialogPane().setContent(grid);
        getDialogPane().setPrefWidth(420);
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
