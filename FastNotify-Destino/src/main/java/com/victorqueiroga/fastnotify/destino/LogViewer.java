package com.victorqueiroga.fastnotify.destino;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDate;
import java.util.List;

public class LogViewer {
    private final LogService logService;
    private Stage stage;

    public LogViewer(LogService logService) {
        this.logService = logService;
    }

    public void show(Window owner) {
        if (stage != null && stage.isShowing()) {
            stage.toFront();
            stage.requestFocus();
            return;
        }
        stage = new Stage();
        stage.setTitle("FastNotify Destino - Logs");
        if (owner != null) {
            stage.initOwner(owner);
        }
        TrayIcons.applyWindowIcons(stage, "/img/mensagem-recebida.svg");

        DatePicker datePicker = new DatePicker(LocalDate.now());
        ComboBox<String> filter = new ComboBox<>();
        filter.getItems().add("Todos os tipos");
        for (LogType t : LogType.values()) {
            filter.getItems().add(t.label() + " (" + t.code() + ")");
        }
        filter.getSelectionModel().selectFirst();

        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(false);
        area.setFont(Font.font("Consolas", 13));
        area.setStyle("-fx-text-fill: #0F172A; -fx-control-inner-background: #F8FAFC;");

        Label status = new Label();
        status.setTextFill(Color.web("#64748B"));

        Button refresh = new Button("Atualizar");
        refresh.setDefaultButton(true);

        Runnable reload = () -> {
            LocalDate date = datePicker.getValue() == null ? LocalDate.now() : datePicker.getValue();
            LogType type = selectedType(filter.getValue());
            List<String> lines = logService.readLines(date, type);
            area.setText(String.join("\n", lines));
            if (area.getText().isBlank()) {
                area.setText("(sem registros para " + date
                        + (type == null ? "" : " / " + type.label()) + ")");
            }
            area.positionCaret(0);
            status.setText("Arquivo: " + logService.logFile(date).getFileName()
                    + " | " + lines.size() + " linha(s)"
                    + " | cache: " + logService.cacheSize()
                    + " | flush a cada " + LogService.FLUSH_INTERVAL_MINUTES + " min");
        };

        Button save = new Button("Salvar cache no log");
        save.setOnAction(e -> {
            int flushed = logService.flush();
            reload.run();
            if (flushed > 0) {
                status.setText(flushed + " linha(s) gravada(s) em "
                        + logService.logFile(LocalDate.now()).getFileName()
                        + " | cache: " + logService.cacheSize());
            } else {
                status.setText("Nada para gravar — cache vazio.");
            }
        });

        refresh.setOnAction(e -> reload.run());
        datePicker.valueProperty().addListener((o, a, b) -> reload.run());
        filter.valueProperty().addListener((o, a, b) -> reload.run());

        HBox toolbar = new HBox(10,
                new Label("Dia:"), datePicker,
                new Label("Tipo:"), filter,
                refresh,
                save);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox center = new VBox(area);
        VBox.setVgrow(area, Priority.ALWAYS);
        center.setPadding(new Insets(0, 10, 0, 10));

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(center);
        root.setBottom(status);
        BorderPane.setMargin(status, new Insets(6, 10, 8, 10));

        stage.setScene(new Scene(root, 900, 520));
        stage.show();
        reload.run();
    }

    private static LogType selectedType(String comboValue) {
        if (comboValue == null || comboValue.startsWith("Todos")) {
            return null;
        }
        int open = comboValue.lastIndexOf('(');
        int close = comboValue.lastIndexOf(')');
        if (open > 0 && close > open) {
            return LogType.fromCode(comboValue.substring(open + 1, close));
        }
        return null;
    }
}
