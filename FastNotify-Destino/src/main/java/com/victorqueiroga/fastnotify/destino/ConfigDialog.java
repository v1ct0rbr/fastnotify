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
    private final CheckBox soundCheck = new CheckBox("Som de alerta ao abrir notificação");
    private final ConfigStore config;
    private boolean confirmed;
    private boolean portChanged;

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
        soundCheck.setSelected(config.isSoundEnabled());

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));
        grid.add(new Label("Porta:"), 0, 0);
        grid.add(portSpinner, 1, 0);
        grid.add(new Label("Token:"), 0, 1);
        grid.add(tokenField, 1, 1);
        grid.add(soundCheck, 0, 2);
        grid.setColumnSpan(soundCheck, 2);

        getDialogPane().setContent(grid);
        getDialogPane().setPrefWidth(380);
        getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.CANCEL,
                javafx.scene.control.ButtonType.OK);

        Button ok = (Button) getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        ok.setText("Salvar");
        Button cancel = (Button) getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancel.setText("Cancelar");

        ok.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            portChanged = portSpinner.getValue() != originalPort;
            config.setPort(portSpinner.getValue());
            config.setToken(tokenField.getText().trim());
            config.setSoundEnabled(soundCheck.isSelected());
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
}
