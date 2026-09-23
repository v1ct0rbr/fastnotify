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

public class ComposeDialog extends Dialog<Boolean> {
    private final ComboBox<MsgType> typeCombo = new ComboBox<>();
    private final TextField titleField = new TextField();
    private final TextArea bodyArea = new TextArea();
    private boolean confirmed;

    public ComposeDialog(Window owner, MsgType initialType) {
        initOwner(owner);
        initModality(javafx.stage.Modality.APPLICATION_MODAL);
        setTitle("Nova mensagem");
        setHeaderText(null);

        typeCombo.getItems().setAll(MsgType.values());
        typeCombo.setValue(initialType == null ? MsgType.NOTIFICATION : initialType);
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        titleField.setPromptText("Título (opcional)");
        bodyArea.setPromptText("Texto da mensagem");
        bodyArea.setWrapText(true);
        bodyArea.setPrefRowCount(6);

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

        Label hint = new Label(
                "Tela e tempo em tela vêm de cada destino selecionado.");
        hint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        hint.setWrapText(true);
        grid.add(hint, 1, 3);

        getDialogPane().setContent(grid);
        getDialogPane().setPrefWidth(460);
        getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.CANCEL,
                javafx.scene.control.ButtonType.OK);

        Button ok = (Button) getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        ok.setText("Enviar");
        ok.setDefaultButton(true);
        Button cancel = (Button) getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancel.setText("Cancelar");

        ok.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (titleField.getText().isBlank() && bodyArea.getText().isBlank()) {
                e.consume();
                bodyArea.requestFocus();
                return;
            }
            confirmed = true;
        });

        setResultConverter(button -> button == javafx.scene.control.ButtonType.OK);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public MsgType getSelectedType() {
        return typeCombo.getValue();
    }

    public String getTitleText() {
        return titleField.getText().trim();
    }

    public String getBodyText() {
        return bodyArea.getText().trim();
    }
}
