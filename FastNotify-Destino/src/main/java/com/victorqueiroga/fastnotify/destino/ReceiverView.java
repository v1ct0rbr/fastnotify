package com.victorqueiroga.fastnotify.destino;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ReceiverView extends BorderPane {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ConfigStore config;
    private final Spinner<Integer> portSpinner;
    private final TextField tokenField = new TextField();
    private final CheckBox soundCheck = new CheckBox("Som de alerta ao abrir notificação");
    private final TextArea logArea = new TextArea();
    private final Label statusLabel = new Label("Parado");
    private final ListenerService listener;
    private final LogService logService;

    public ReceiverView(ConfigStore config, LogService logService) {
        this.config = config;
        this.logService = logService;

        portSpinner = new Spinner<>(1, 65535, config.getPort());
        portSpinner.setEditable(true);
        portSpinner.setPrefWidth(100);
        tokenField.setText(config.getToken());
        tokenField.setPromptText("token compartilhado");
        tokenField.setPrefColumnCount(18);
        soundCheck.setSelected(config.isSoundEnabled());

        logArea.setEditable(false);
        logArea.setWrapText(false);

        listener = new ListenerService(
                () -> portSpinner.getValue(),
                () -> tokenField.getText().trim(),
                () -> soundCheck.isSelected(),
                this::onEvent,
                logService);

        setTop(buildConfigPane());
        setCenter(buildLogPane());
        setBottom(buildStatusBar());
    }

    public void startListener() {
        listener.start();
        log("Destino iniciado. Monitores: " + describeScreens());
    }

    private TitledPane buildConfigPane() {
        soundCheck.setOnAction(e -> saveConfig());

        HBox row = new HBox(8,
                new Label("Porta:"), portSpinner,
                new Label("Token:"), tokenField,
                soundCheck);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));

        Button save = new Button("Salvar");
        save.setOnAction(e -> saveConfig());
        Button restart = new Button("Reiniciar escuta");
        restart.setOnAction(e -> {
            saveConfig();
            listener.stop();
            listener.start();
            statusLabel.setText("Reiniciando...");
        });

        HBox actions = new HBox(8, save, restart);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(0, 8, 8, 8));

        VBox box = new VBox(row, actions);
        TitledPane tp = new TitledPane("Configuração", box);
        tp.setExpanded(true);
        tp.setCollapsible(false);
        return tp;
    }

    private TitledPane buildLogPane() {
        VBox box = new VBox(logArea);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        box.setPadding(new Insets(4));
        TitledPane tp = new TitledPane("Log de recepção", box);
        tp.setExpanded(true);
        tp.setCollapsible(false);
        BorderPane.setMargin(tp, new Insets(0));
        return tp;
    }

    private HBox buildStatusBar() {
        Label caption = new Label("Status:");
        statusLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 12));
        HBox box = new HBox(6, caption, statusLabel);
        box.setPadding(new Insets(6, 8, 6, 8));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void saveConfig() {
        String token = tokenField.getText().trim();
        if (token.isEmpty()) {
            log("Aviso: sem token, notificações serão rejeitadas.");
        }
        config.setPort(portSpinner.getValue());
        config.setToken(token);
        config.setSoundEnabled(soundCheck.isSelected());
        config.save();
        log("Configuração salva"
                + (soundCheck.isSelected() ? " (som de alerta ATIVO)." : " (som de alerta DESATIVADO)."));
    }

    private void onEvent(NotificationMessage msg, String text) {
        Platform.runLater(() -> {
            if (text.startsWith("Escutando")) {
                statusLabel.setText("Ativo na porta " + portSpinner.getValue());
            }
            log(text);
        });
    }

    private String describeScreens() {
        int n = javafx.stage.Screen.getScreens().size();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(i);
        }
        return sb.isEmpty() ? "0" : sb.toString();
    }

    public void log(String message) {
        logArea.appendText(LocalTime.now().format(TIME) + "  " + message + "\n");
        logArea.positionCaret(logArea.getLength());
    }

    public void applyConfigFromDisk(boolean portChanged) {
        tokenField.setText(config.getToken());
        portSpinner.getValueFactory().setValue(config.getPort());
        soundCheck.setSelected(config.isSoundEnabled());
        if (portChanged) {
            restartListener();
        }
    }

    public void restartListener() {
        listener.stop();
        listener.start();
        statusLabel.setText("Reiniciando na porta " + config.getPort() + "...");
    }
}
