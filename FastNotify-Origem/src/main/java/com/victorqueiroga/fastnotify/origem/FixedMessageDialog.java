package com.victorqueiroga.fastnotify.origem;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

public class FixedMessageDialog extends Dialog<Boolean> {
    private final ComboBox<MsgType> typeCombo = new ComboBox<>();
    private final TextField titleField = new TextField();
    private final TextArea bodyArea = new TextArea();
    private ConfigStore.FixedMessage result;
    private boolean confirmed;

    public FixedMessageDialog(Window owner, ConfigStore.FixedMessage initial) {
        initOwner(owner);
        initModality(javafx.stage.Modality.APPLICATION_MODAL);
        setTitle(initial == null ? "Nova mensagem fixa" : "Editar mensagem fixa");
        setHeaderText(null);

        typeCombo.getItems().setAll(MsgType.values());
        typeCombo.setValue(initial == null ? MsgType.NOTIFICATION : initial.type());
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        titleField.setPromptText("Título (opcional)");
        if (initial != null) {
            titleField.setText(initial.title());
        }

        bodyArea.setPromptText("Texto da mensagem fixa");
        bodyArea.setWrapText(true);
        bodyArea.setPrefRowCount(6);
        if (initial != null) {
            bodyArea.setText(initial.body());
        }

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));
        grid.add(new Label("Tipo:"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Título:"), 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(new Label("Mensagem:"), 0, 2);
        grid.add(bodyArea, 1, 2);

        getDialogPane().setContent(grid);
        getDialogPane().setPrefWidth(460);
        getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.CANCEL,
                javafx.scene.control.ButtonType.OK);

        Button ok = (Button) getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        ok.setText("Salvar");
        Button cancel = (Button) getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancel.setText("Cancelar");

        ok.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (bodyArea.getText().isBlank()) {
                e.consume();
                bodyArea.requestFocus();
                return;
            }
            confirmed = true;
            result = new ConfigStore.FixedMessage(
                    typeCombo.getValue(),
                    titleField.getText().trim(),
                    bodyArea.getText().trim());
        });

        setResultConverter(button -> button == javafx.scene.control.ButtonType.OK);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public ConfigStore.FixedMessage toFixedMessage() {
        return result;
    }
}
