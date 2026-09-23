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

        origemHostField.setText(config.getOrigemHost());
        origemHostField.setPromptText("ip ou nome da Origem");
        origemHostField.setPrefColumnCount(14);
        origemPortSpinner = new Spinner<>(1, 65535, config.getOrigemRegisterPort());
        origemPortSpinner.setEditable(true);
        origemPortSpinner.setPrefWidth(90);
        origemTokenField.setText(config.getOrigemRegisterToken());
        origemTokenField.setPromptText("token da Origem");
        origemTokenField.setPrefColumnCount(12);
        aliasField.setText(config.getAlias());
        aliasField.setPromptText("nome deste destino");
        aliasField.setPrefColumnCount(10);
        departmentField.setText(config.getDepartment());
        departmentField.setPromptText("departamento (ex.: Financeiro)");
        departmentField.setPrefColumnCount(12);
        registerStatus.setStyle("-fx-font-size: 11px;");

        logArea.setEditable(false);
        logArea.setWrapText(false);
        LocalHostInfo.warmUp();

        listener = new ListenerService(
                () -> portSpinner.getValue(),
                () -> tokenField.getText().trim(),
                () -> config.effectivePsk(tokenField.getText().trim()),
                () -> soundCheck.isSelected(),
                this::onEvent,
                logService);

        VBox top = new VBox(buildConfigPane(), buildRegisterPane());
        setTop(top);
        setCenter(buildLogPane());
        setBottom(buildStatusBar());
    }

    public void startListener() {
        listener.start();
        log("Destino iniciado. Monitores: " + describeScreens());
        checkFirewall();
    }

    private void checkFirewall() {
        int port = portSpinner.getValue();
        log("Consultando firewall (entrada porta " + port + ", regra "
                + "FastNotify-Destino-Notificacao) ...");
        Thread t = new Thread(() -> {
            String status = FirewallChecker.checkInboundRule(
                    "FastNotify-Destino-Notificacao", port);
            Platform.runLater(() -> log(status));
        }, "fastnotify-firewall-check");
        t.setDaemon(true);
        t.start();
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
        Button firewall = new Button("Firewall");
        firewall.setTooltip(new javafx.scene.control.Tooltip(
                "Consulta se a porta de notificação tem regra de entrada no Windows"));
        firewall.setOnAction(e -> checkFirewall());

        HBox actions = new HBox(8, save, restart, firewall);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(0, 8, 8, 8));

        VBox box = new VBox(row, actions);
        TitledPane tp = new TitledPane("Configuração", box);
        tp.setExpanded(true);
        tp.setCollapsible(false);
        return tp;
    }

    private TitledPane buildRegisterPane() {
        HBox localRow = new HBox(8,
                new Label("Este equipamento:"),
                new Label(LocalHostInfo.display()));
        localRow.setAlignment(Pos.CENTER_LEFT);
        localRow.setPadding(new Insets(8, 8, 0, 8));
        localRow.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        HBox row = new HBox(8,
                new Label("Origem:"), origemHostField,
                new Label("Porta cadastro:"), origemPortSpinner,
                new Label("Token origem:"), origemTokenField);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));

        HBox row2 = new HBox(8,
                new Label("Nome (padrão: host):"), aliasField,
                new Label("Departamento:"), departmentField);
        row2.setAlignment(Pos.CENTER_LEFT);
        row2.setPadding(new Insets(0, 8, 0, 8));

        if (aliasField.getText() == null || aliasField.getText().isBlank()) {
            aliasField.setText(LocalHostInfo.hostname());
        }

        registerBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #2563EB;"
                + " -fx-text-fill: white;");
        registerBtn.setOnAction(e -> runRegister());

        Hint hint = new Hint(
                "Hostname e IP deste equipamento são lidos do SO (acima) e enviados"
                        + " separados; a Origem prefere o hostname (o IP é dinâmico)."
                        + " Informe só a Origem (host/porta/token), nome e departamento.");
        hint.setWrapText(true);

        HBox actions = new HBox(8, registerBtn, registerStatus);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(0, 8, 4, 8));

        VBox box = new VBox(4, localRow, row, row2, hint, actions);
        TitledPane tp = new TitledPane("Cadastrar nesta Origem", box);
        tp.setExpanded(true);
        tp.setCollapsible(false);
        return tp;
    }

    private static final class Hint extends Label {
        private Hint(String text) {
            super(text);
            setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
            setPadding(new Insets(0, 8, 0, 8));
        }
    }

    private void runRegister() {
        String host = origemHostField.getText().trim();
        int registerPort = origemPortSpinner.getValue();
        String origemToken = origemTokenField.getText().trim();
        String alias = aliasField.getText().trim();
        String department = departmentField.getText() == null
                ? "" : departmentField.getText().trim();
        int listenPort = portSpinner.getValue();
        String destinoToken = tokenField.getText().trim();

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

    public void logAs(LogType type, String ip, String message) {
        log(message);
        if (logService != null) {
            logService.append(type, ip, message);
        }
    }

    public void applyConfigFromDisk(boolean portChanged) {
        tokenField.setText(config.getToken());
        portSpinner.getValueFactory().setValue(config.getPort());
        soundCheck.setSelected(config.isSoundEnabled());
        origemHostField.setText(config.getOrigemHost());
        origemPortSpinner.getValueFactory().setValue(config.getOrigemRegisterPort());
        origemTokenField.setText(config.getOrigemRegisterToken());
        aliasField.setText(config.getAlias());
        departmentField.setText(config.getDepartment());
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
