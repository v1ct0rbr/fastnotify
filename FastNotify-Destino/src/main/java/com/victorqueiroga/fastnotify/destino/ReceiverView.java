package com.victorqueiroga.fastnotify.destino;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ReceiverView extends BorderPane {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ConfigStore config;
    private final TextField origemHostField = new TextField();
    private final Spinner<Integer> origemPortSpinner;
    private final TextField origemTokenField = new TextField();
    private final TextField aliasField = new TextField();
    private final TextField departmentField = new TextField();
    private final Button registerBtn = new Button("Cadastrar nesta Origem");
    private final Label registerStatus = new Label("");
    private final TextArea logArea = new TextArea();
    private final Label statusLabel = new Label("Parado");
    private final ListenerService listener;
    private final LogService logService;
    private Runnable openSettingsAction = () -> {
    };

    public ReceiverView(ConfigStore config, LogService logService) {
        this.config = config;
        this.logService = logService;

        origemHostField.setText(config.getOrigemHost());
        origemHostField.setPromptText("ip ou nome da Origem");
        origemHostField.setPrefColumnCount(16);
        origemPortSpinner = new Spinner<>(1, 65535, config.getOrigemRegisterPort());
        origemPortSpinner.setEditable(true);
        origemPortSpinner.setPrefWidth(100);
        origemTokenField.setText(config.getOrigemRegisterToken());
        origemTokenField.setPromptText("token da Origem");
        origemTokenField.setPrefColumnCount(14);
        aliasField.setText(config.getAlias());
        aliasField.setPromptText("nome deste destino");
        aliasField.setPrefColumnCount(14);
        departmentField.setText(config.getDepartment());
        departmentField.setPromptText("departamento (ex.: Financeiro)");
        departmentField.setPrefColumnCount(14);
        registerStatus.setStyle("-fx-font-size: 11px;");

        if (aliasField.getText() == null || aliasField.getText().isBlank()) {
            aliasField.setText(LocalHostInfo.hostname());
        }

        logArea.setEditable(false);
        logArea.setWrapText(false);
        LocalHostInfo.warmUp();

        listener = new ListenerService(
                config::getPort,
                () -> config.getToken(),
                () -> config.effectivePsk(config.getToken()),
                config::isSoundEnabled,
                this::onEvent,
                logService);

        setTop(buildTop());
        setCenter(buildLogPane());
        setBottom(buildStatusBar());
    }

    public void setOpenSettingsAction(Runnable openSettingsAction) {
        this.openSettingsAction = openSettingsAction == null ? () -> {
        } : openSettingsAction;
    }

    public void startListener() {
        listener.start();
        log("Destino iniciado. Monitores: " + describeScreens());
    }

    private VBox buildTop() {
        Button settings = new Button("Configurações");
        settings.setTooltip(new javafx.scene.control.Tooltip(
                "Porta/token de escuta, PSK, som, retenção de logs e firewall"));
        settings.setOnAction(e -> openSettingsAction.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(8, spacer, settings);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 8, 0, 8));

        return new VBox(bar, buildRegisterPane());
    }

    private TitledPane buildRegisterPane() {
        Label localLabel = new Label("Este equipamento: " + LocalHostInfo.display());
        localLabel.setWrapText(true);
        localLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(130);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        grid.add(new Label("Origem:"), 0, 0);
        grid.add(origemHostField, 1, 0);
        grid.add(new Label("Porta cadastro:"), 0, 1);
        grid.add(origemPortSpinner, 1, 1);
        grid.add(new Label("Token origem:"), 0, 2);
        grid.add(origemTokenField, 1, 2);
        grid.add(new Label("Nome (padrão: host):"), 0, 3);
        grid.add(aliasField, 1, 3);
        grid.add(new Label("Departamento:"), 0, 4);
        grid.add(departmentField, 1, 4);

        Hint hint = new Hint(
                "Hostname e IP deste equipamento são lidos do SO e enviados"
                        + " separados; a Origem prefere o hostname (o IP é dinâmico)."
                        + " Porta/token/som ficam em Configurações.");
        hint.setWrapText(true);

        registerBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #2563EB;"
                + " -fx-text-fill: white;");
        registerBtn.setOnAction(e -> runRegister());

        HBox actions = new HBox(8, registerBtn, registerStatus);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, localLabel, grid, hint, actions);
        box.setPadding(new Insets(8));
        TitledPane tp = new TitledPane("Cadastrar nesta Origem", box);
        tp.setExpanded(true);
        tp.setCollapsible(false);
        return tp;
    }

    private static final class Hint extends Label {
        private Hint(String text) {
            super(text);
            setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        }
    }

    private void runRegister() {
        String host = origemHostField.getText().trim();
        int registerPort = origemPortSpinner.getValue();
        String origemToken = origemTokenField.getText().trim();
        String alias = aliasField.getText().trim();
        String department = departmentField.getText() == null
                ? "" : departmentField.getText().trim();
        int listenPort = config.getPort();
        String destinoToken = config.getToken();

        if (host.isEmpty()) {
            setRegisterStatus("Informe o host da Origem.", true);
            return;
        }
        if (origemToken.isEmpty()) {
            setRegisterStatus("Informe o token de cadastro da Origem.", true);
            return;
        }

        if (alias == null || alias.isBlank()) {
            alias = LocalHostInfo.hostname();
            aliasField.setText(alias);
        }

        config.setOrigemHost(host);
        config.setOrigemRegisterPort(registerPort);
        config.setOrigemRegisterToken(origemToken);
        config.setAlias(alias);
        config.setDepartment(department);
        config.save();

        Protocol.Registration reg = new Protocol.Registration(
                origemToken, alias, listenPort, destinoToken, 0, 5000, department,
                LocalHostInfo.hostname(), LocalHostInfo.primaryIp());

        registerBtn.setDisable(true);
        setRegisterStatus("Cadastrando em " + host + ":" + registerPort + " ...", false);
        log("Enviando cadastro para " + host + ":" + registerPort
                + " (porta de escuta " + listenPort + ") ...");

        Thread t = new Thread(() -> {
            try {
                Protocol.Ack ack = RegistrationClient.register(
                        host, registerPort, reg, config.effectivePsk(origemToken));
                Platform.runLater(() -> {
                    registerBtn.setDisable(false);
                    if (ack.ok()) {
                        setRegisterStatus("OK — " + ack.detail(), false);
                        log("CADASTRO OK — " + host + ":" + registerPort + " — " + ack.detail());
                        logService.append(LogType.REGISTER, host,
                                "Cadastro aceito pela Origem: " + ack.detail()
                                        + " (escuta " + listenPort + ")");
                    } else {
                        setRegisterStatus("Recusado — " + ack.detail(), true);
                        log("CADASTRO RECUSADO — " + ack.detail());
                        logService.append(LogType.ERROR, host,
                                "Cadastro recusado: " + ack.detail());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    registerBtn.setDisable(false);
                    setRegisterStatus("Falha — " + ex.getMessage(), true);
                    log("CADASTRO FALHOU — " + ex.getMessage());
                    logService.append(LogType.ERROR, host,
                            "Falha no cadastro: " + ex.getMessage());
                });
            }
        }, "fastnotify-register");
        t.setDaemon(true);
        t.start();
    }

    private void setRegisterStatus(String text, boolean error) {
        registerStatus.setText(text);
        registerStatus.setStyle("-fx-font-size: 11px;"
                + (error ? " -fx-text-fill: #DC2626;" : " -fx-text-fill: #16A34A;"));
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

    private void onEvent(NotificationMessage msg, String text) {
        Platform.runLater(() -> {
            if (text.startsWith("Escutando")) {
                statusLabel.setText("Ativo na porta " + config.getPort());
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

    public void logAs(LogType type, String ip, String message) {
        log(message);
        if (logService != null) {
            logService.append(type, ip, message);
        }
    }

    public void applyConfigFromDisk(boolean portChanged) {
        origemHostField.setText(config.getOrigemHost());
        origemPortSpinner.getValueFactory().setValue(config.getOrigemRegisterPort());
        origemTokenField.setText(config.getOrigemRegisterToken());
        aliasField.setText(config.getAlias());
        departmentField.setText(config.getDepartment());
        if (portChanged) {
            restartListener();
        } else if (listener.isRunning()) {
            statusLabel.setText("Ativo na porta " + config.getPort());
        }
    }

    public void restartListener() {
        listener.stop();
        listener.start();
        statusLabel.setText("Reiniciando na porta " + config.getPort() + "...");
    }
}
