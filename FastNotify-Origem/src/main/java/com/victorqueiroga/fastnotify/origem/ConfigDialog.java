package com.victorqueiroga.fastnotify.origem;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class ConfigDialog extends Dialog<Boolean> {
    private boolean confirmed;
    private final int originalRegisterPort;
    private final int originalRetentionDays;
    private final ConfigStore config;
    private final Spinner<Integer> registerPortSpinner;
    private final TextField registerTokenField;
    private final TextField pskField;
    private final Spinner<Integer> retentionSpinner;

    public ConfigDialog(Window owner, DestinosStore destinosStore, ConfigStore config) {
        this.config = config;
        this.originalRegisterPort = config.getRegisterPort();
        this.originalRetentionDays = config.getLogRetentionDays();

        initOwner(owner);
        initModality(javafx.stage.Modality.WINDOW_MODAL);
        setTitle("Configurações - FastNotify Origem");
        setHeaderText("Configuração global, auto-cadastro e criptografia (PSK).");

        registerPortSpinner = new Spinner<>(1, 65535, config.getRegisterPort());
        registerPortSpinner.setEditable(true);
        registerPortSpinner.setPrefWidth(140);

        registerTokenField = new TextField(config.getRegisterToken());
        registerTokenField.setPromptText("Token exigido dos destinos ao cadastrar");
        registerTokenField.setPrefWidth(300);

        pskField = new TextField(config.getPsk());
        pskField.setPromptText("Chave AES-GCM (vazia = usa o token como PSK)");
        pskField.setPrefWidth(300);

        Label portInfo = new Label(
                "Porta em que a Origem escuta pedidos de cadastro dos Destinos. "
                        + "Libere no firewall (TCP) para a rede.");
        portInfo.setWrapText(true);
        portInfo.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        Label tokenInfo = new Label(
                "Token que o Destino deve informar para se cadastrar nesta Origem. "
                        + "Vazio = cadastro recusado.");
        tokenInfo.setWrapText(true);
        tokenInfo.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        Label pskInfo = new Label(
                "PSK opcional em AES-256-GCM (mesma chave no Destino). Se vazia, o "
                        + "token já usado (cadastro ou de cada destino) vira a chave — "
                        + "sem configurar nada extra.");
        pskInfo.setWrapText(true);
        pskInfo.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        retentionSpinner = new Spinner<>(0, 3650, config.getLogRetentionDays());
        retentionSpinner.setEditable(true);
        retentionSpinner.setPrefWidth(140);

        Label retentionInfo = new Label(
                "Dias de retenção de logs (logs-AAAA-MM-DD.log). Arquivos com data "
                        + "anterior a hoje − N dias são excluídos. 0 = manter todos.");
        retentionInfo.setWrapText(true);
        retentionInfo.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        Label destInfo = new Label(
                "Host, porta de notificação, token, tela e tempo de cada destino ficam "
                        + "em cada item da lista da janela principal.\n"
                        + "Arquivo de destinos: " + destinosStore.getFile().toAbsolutePath());
        destInfo.setWrapText(true);
        destInfo.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(160);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        grid.add(new Label("Porta de cadastro (registro):"), 0, 0);
        grid.add(registerPortSpinner, 1, 0);
        grid.add(portInfo, 1, 1);
        grid.add(new Label("Token de cadastro:"), 0, 2);
        grid.add(registerTokenField, 1, 2);
        grid.add(tokenInfo, 1, 3);
        grid.add(new Label("PSK (AES-GCM):"), 0, 4);
        grid.add(pskField, 1, 4);
        grid.add(pskInfo, 1, 5);
        grid.add(new Label("Retenção de logs (dias):"), 0, 6);
        grid.add(retentionSpinner, 1, 6);
        grid.add(retentionInfo, 1, 7);

        VBox box = new VBox(10, grid, destInfo);
        box.setPadding(new Insets(8));
        getDialogPane().setContent(box);
        getDialogPane().setPrefWidth(540);

        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType save = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(cancel, save);

        Button okButton = (Button) getDialogPane().lookupButton(save);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            saveConfig();
            confirmed = true;
        });
        setResultConverter(b -> confirmed);
    }

    private void saveConfig() {
        int port = registerPortSpinner.getValue();
        if (port < 1 || port > 65535) {
            port = 9877;
        }
        config.setRegisterPort(port);
        config.setRegisterToken(
                registerTokenField.getText() == null ? "" : registerTokenField.getText().trim());
        config.setPsk(pskField.getText() == null ? "" : pskField.getText().trim());
        int retention = retentionSpinner.getValue();
        if (retention < 0) {
            retention = 30;
        }
        config.setLogRetentionDays(retention);
        config.save();
    }

    public boolean isRetentionChanged() {
        return config.getLogRetentionDays() != originalRetentionDays;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isRegisterPortChanged() {
        return config.getRegisterPort() != originalRegisterPort;
    }
}
