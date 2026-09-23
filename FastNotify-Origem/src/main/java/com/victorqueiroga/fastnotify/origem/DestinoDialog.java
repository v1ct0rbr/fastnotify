package com.victorqueiroga.fastnotify.origem;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

public class DestinoDialog extends Dialog<Boolean> {
    private final TextField aliasField = new TextField();
    private final TextField hostField = new TextField();
    private final Spinner<Integer> portSpinner;
    private final TextField tokenField = new TextField();
    private final Spinner<Integer> screenSpinner;
    private final Spinner<Integer> durationSpinner;
    private final TextField departmentField = new TextField();
    private final Label testResult = new Label();
    private final Button testBtn = new Button("Testar destino");
    private final ConfigStore config;
    private DestinosStore.Destino result;
    private boolean confirmed;

    public DestinoDialog(Window owner, DestinosStore.Destino initial, ConfigStore config) {
        this.config = config;
        initOwner(owner);
        initModality(javafx.stage.Modality.APPLICATION_MODAL);
        setTitle(initial == null ? "Novo destino" : "Editar destino");
        setHeaderText(null);

        aliasField.setPromptText("Alias (ex.: Recepção)");
        if (initial != null && initial.alias() != null) {
            aliasField.setText(initial.alias());
        }

        hostField.setPromptText("Hostname preferencial ou IP (ex.: recepcao.local)");
        hostField.setPrefColumnCount(24);
        if (initial != null) {
            hostField.setText(initial.connectHost());
        }

        DestinosStore.Destino base = initial == null
                ? new DestinosStore.Destino(null, "", DestinosStore.DEFAULT_PORT,
                        "", DestinosStore.DEFAULT_SCREEN, DestinosStore.DEFAULT_DURATION_MS, "")
                : initial.normalized();

        portSpinner = new Spinner<>(1, 65535, base.effectivePortInt());
        portSpinner.setEditable(true);
        portSpinner.setPrefWidth(100);

        tokenField.setText(base.effectiveToken());
        tokenField.setPromptText("token deste destino");
        tokenField.setPrefColumnCount(16);

        screenSpinner = new Spinner<>(0, 15, base.effectiveScreen());
        screenSpinner.setEditable(true);
        screenSpinner.setPrefWidth(80);

        durationSpinner = new Spinner<>(500, 300000, (int) Math.min(base.effectiveDurationMs(), 300000), 500);
        durationSpinner.setEditable(true);
        durationSpinner.setPrefWidth(120);

        departmentField.setPromptText("Departamento (ex.: Financeiro)");
        departmentField.setPrefColumnCount(20);
        departmentField.setText(base.effectiveDepartment());

        testResult.setStyle("-fx-text-fill: #64748B;");
        testBtn.setOnAction(e -> runFormTest());

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));

        grid.add(new Label("Alias:"), 0, 0);
        grid.add(aliasField, 1, 0);
        grid.add(new Label("Host:"), 0, 1);
        grid.add(hostField, 1, 1);
        grid.add(new Label("Porta:"), 0, 2);
        grid.add(portSpinner, 1, 2);
        grid.add(new Label("Token:"), 0, 3);
        grid.add(tokenField, 1, 3);
        grid.add(new Label("Tela:"), 0, 4);
        grid.add(screenSpinner, 1, 4);
        grid.add(new Label("Tempo (ms):"), 0, 5);
        grid.add(durationSpinner, 1, 5);
        grid.add(new Label("Departamento:"), 0, 6);
        grid.add(departmentField, 1, 6);

        Label hint = new Label(
                "Cada destino é isolado: porta, token, tela e tempo valem só para ele.");
        hint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        hint.setWrapText(true);
        grid.add(hint, 1, 7);

        HBox testRow = new HBox(8, testBtn, testResult);
        testRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(testRow, 1, 8);

        getDialogPane().setContent(grid);
        getDialogPane().setPrefWidth(520);
        getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.CANCEL,
                javafx.scene.control.ButtonType.OK);

        Button ok = (Button) getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        ok.setText("Salvar");
        Button cancel = (Button) getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancel.setText("Cancelar");

        ok.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String host = hostField.getText().trim();
            if (host.isEmpty()) {
                e.consume();
                hostField.requestFocus();
                return;
            }
            String alias = aliasField.getText().trim();
            confirmed = true;
            String priorIp = initial == null ? "" : initial.effectiveHostIp();
            result = new DestinosStore.Destino(
                    alias.isEmpty() ? host : alias,
                    host,
                    portSpinner.getValue(),
                    tokenField.getText().trim(),
                    screenSpinner.getValue(),
                    durationSpinner.getValue().longValue(),
                    departmentField.getText() == null ? "" : departmentField.getText().trim(),
                    priorIp)
                    .normalized();
        });

        setResultConverter(button -> button == javafx.scene.control.ButtonType.OK);
    }

    private void runFormTest() {
        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            testResult.setText("Informe o host.");
            testResult.setStyle("-fx-text-fill: #DC2626;");
            return;
        }
        int port = portSpinner.getValue();
        String token = tokenField.getText().trim();
        testResult.setText("Testando " + host + ":" + port + " ...");
        testResult.setStyle("-fx-text-fill: #64748B;");
        testBtn.setDisable(true);

        Thread t = new Thread(() -> {
            try {
                Protocol.Ack ack = NotificationClient.test(host, port, token,
                        config == null ? "" : config.effectivePsk(token));
                javafx.application.Platform.runLater(() -> {
                    testBtn.setDisable(false);
                    if (ack.ok()) {
                        testResult.setText("OK — " + ack.detail());
                        testResult.setStyle("-fx-text-fill: #16A34A;");
                    } else {
                        testResult.setText("FALHOU — " + ack.detail());
                        testResult.setStyle("-fx-text-fill: #DC2626;");
                    }
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    testBtn.setDisable(false);
                    testResult.setText("FALHOU — " + ex.getMessage());
                    testResult.setStyle("-fx-text-fill: #DC2626;");
                });
            }
        }, "fastnotify-destino-dialog-test");
        t.setDaemon(true);
        t.start();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public DestinosStore.Destino toDestino() {
        return result;
    }
}
