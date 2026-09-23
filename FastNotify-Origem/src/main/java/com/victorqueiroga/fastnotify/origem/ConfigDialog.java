package com.victorqueiroga.fastnotify.origem;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class ConfigDialog extends Dialog<Boolean> {
    private boolean confirmed;

    public ConfigDialog(Window owner, DestinosStore destinosStore) {
        initOwner(owner);
        initModality(javafx.stage.Modality.WINDOW_MODAL);
        setTitle("Configurações - FastNotify Origem");
        setHeaderText("Não há config global de destino.");

        Label body = new Label(
                "Host, porta, token, tela e tempo são definidos por destino, "
                        + "em cada item da lista da janela principal.\n\n"
                        + "Arquivo de destinos:\n"
                        + destinosStore.getFile().toAbsolutePath());
        body.setWrapText(true);

        VBox box = new VBox(8, body);
        box.setPadding(new Insets(8));
        getDialogPane().setContent(box);
        getDialogPane().setPrefWidth(460);
        getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.OK);

        Button ok = (Button) getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        ok.setText("Fechar");
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, e -> confirmed = true);
        setResultConverter(b -> confirmed);
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
