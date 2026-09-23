package com.victorqueiroga.fastnotify.origem;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SenderView extends BorderPane {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    static final String CAMPAINHA_TITLE = "🔔 Campainha";
    static final String CAMPAINHA_BODY = "Atenção! Chamada da campainha virtual.";
    static final long CAMPAINHA_MAX_MS = 8000;

    private final MessageStore messageStore;
    private final DestinosStore destinosStore;
    private final ConfigStore config;
    private final LogService logService;
    private final ListView<ConfigStore.FixedMessage> fixedList = new ListView<>();
    private final ListView<DestinosStore.Destino> destList = new ListView<>();
    private final TextArea logArea = new TextArea();
    private final List<ConfigStore.FixedMessage> fixedMessages;
    private final List<DestinosStore.Destino> destinos;
    private final Label registerStatus = new Label("Cadastro: —");

    public SenderView(MessageStore messageStore, DestinosStore destinosStore,
                      ConfigStore config, LogService logService) {
        this.messageStore = messageStore;
        this.destinosStore = destinosStore;
        this.config = config;
        this.logService = logService;
        this.fixedMessages = new ArrayList<>(messageStore.load());
        this.destinos = new ArrayList<>(destinosStore.load());

        destList.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        destList.setItems(javafx.collections.FXCollections.observableArrayList(destinos));
        destList.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(DestinosStore.Destino item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.display());
            }
        });
        destList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int idx = destList.getSelectionModel().getSelectedIndex();
                if (idx >= 0) {
                    testDestino(destinos.get(idx));
                }
            }
        });

        fixedList.setItems(javafx.collections.FXCollections.observableArrayList(fixedMessages));
        fixedList.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ConfigStore.FixedMessage item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : "[" + item.type().label() + "] " + item.title());
            }
        });

        logArea.setEditable(false);
        logArea.setWrapText(false);

        setTop(buildToolbar());
        setCenter(buildCenter());
        setBottom(buildLogPane());
    }

    private HBox buildToolbar() {
        Label info = new Label(
                "Configuração por destino (host, porta, token, tela, tempo) na lista abaixo.");
        info.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        info.setWrapText(true);
        HBox.setHgrow(info, Priority.ALWAYS);

        registerStatus.setStyle(
                "-fx-text-fill: #2563EB; -fx-font-size: 12px; -fx-font-weight: bold;");

        Button firewall = new Button("Firewall");
        firewall.setTooltip(new javafx.scene.control.Tooltip(
                "Consulta se a porta de cadastro tem regra de entrada no Windows"));
        firewall.setOnAction(e -> checkFirewall());

        Button bell = new Button("🔔 Campainha");
        bell.setStyle("-fx-font-weight: bold; -fx-background-color: #F59E0B; -fx-text-fill: white;");
        bell.setOnAction(e -> sendCampainha());

        HBox bar = new HBox(8, info, registerStatus, firewall, bell);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8));
        return bar;
    }

    private SplitPane buildCenter() {
        VBox left = new VBox(6);
        left.setPadding(new Insets(8));

        Label destHint = new Label(
                "Destinos (isolados). Clique duplo = testar. Ctrl para vários.");
        destHint.setWrapText(true);
        VBox destBox = new VBox(4, destHint, destList, buildDestinoButtons());
        VBox.setVgrow(destList, Priority.SOMETIMES);
        destBox.setMinHeight(160);

        VBox fixedBox = new VBox(4,
                new Label("Mensagens fixas (reenvio)"), fixedList, buildFixedButtons());
        VBox.setVgrow(fixedList, Priority.ALWAYS);

        left.getChildren().addAll(destBox, fixedBox);

        VBox right = new VBox(8);
        right.setPadding(new Insets(8));
        Button open = new Button("Abrir janela de mensagem...");
        open.setStyle("-fx-font-weight: bold;");
        open.setMaxWidth(Double.MAX_VALUE);
        open.setOnAction(e -> openCompose(null));
        right.getChildren().add(open);

        Label hint = new Label("Envio rápido por tipo:");
        right.getChildren().add(hint);
        for (MsgType type : MsgType.values()) {
            Button b = new Button(type.label());
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> openCompose(type));
            right.getChildren().add(b);
        }

        Label multiHint = new Label(
                "Sem seleção → envia/testa todos os destinos. "
                        + "Com seleção → só os selecionados. "
                        + "Cada destino usa a sua porta, token, tela e tempo.");
        multiHint.setWrapText(true);
        multiHint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        right.getChildren().add(multiHint);

        SplitPane split = new SplitPane(left, right);
        split.setDividerPositions(0.55);
        return split;
    }

    private HBox buildDestinoButtons() {
        Button add = new Button("Novo");
        Button edit = new Button("Editar");
        Button remove = new Button("Excluir");
        Button testOne = new Button("Testar");
        testOne.setOnAction(e -> {
            int idx = destList.getSelectionModel().getSelectedIndex();
            if (idx < 0) {
                log("Selecione um destino para testar (ou duplo clique na lista).");
                return;
            }
            testDestino(destinos.get(idx));
        });
        Button testAll = new Button("Testar todos");
        testAll.setOnAction(e -> testSelectedOrAll());

        add.setOnAction(e -> {
            DestinoDialog dialog = new DestinoDialog(
                    getScene() == null ? null : getScene().getWindow(), null, config);
            dialog.showAndWait();
            if (dialog.isConfirmed()) {
                destinos.add(dialog.toDestino());
                persistDestinos();
            }
        });
        edit.setOnAction(e -> {
            int idx = destList.getSelectionModel().getSelectedIndex();
            if (idx < 0) {
                return;
            }
            DestinoDialog dialog = new DestinoDialog(
                    getScene().getWindow(), destinos.get(idx), config);
            dialog.showAndWait();
            if (dialog.isConfirmed()) {
                destinos.set(idx, dialog.toDestino());
                persistDestinos();
            }
        });
        remove.setOnAction(e -> {
            List<Integer> indexes = new ArrayList<>(
                    destList.getSelectionModel().getSelectedIndices());
            if (indexes.isEmpty()) {
                return;
            }
            indexes.sort(java.util.Comparator.reverseOrder());
            for (int idx : indexes) {
                destinos.remove(idx);
            }
            persistDestinos();
        });

        HBox box = new HBox(6, add, edit, remove, testOne, testAll);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox buildFixedButtons() {
        Button add = new Button("Nova");
        Button edit = new Button("Editar");
        Button remove = new Button("Excluir");
        Button send = new Button("Reenviar");
        send.setStyle("-fx-font-weight: bold;");

        add.setOnAction(e -> {
            FixedMessageDialog dialog = new FixedMessageDialog(
                    getScene() == null ? null : getScene().getWindow(), null);
            dialog.showAndWait();
            if (dialog.isConfirmed()) {
                fixedMessages.add(dialog.toFixedMessage());
                persistFixed();
            }
        });
        edit.setOnAction(e -> {
            int idx = fixedList.getSelectionModel().getSelectedIndex();
            if (idx < 0) {
                return;
            }
            FixedMessageDialog dialog = new FixedMessageDialog(
                    getScene().getWindow(), fixedMessages.get(idx));
            dialog.showAndWait();
            if (dialog.isConfirmed()) {
                fixedMessages.set(idx, dialog.toFixedMessage());
                persistFixed();
            }
        });
        remove.setOnAction(e -> {
            int idx = fixedList.getSelectionModel().getSelectedIndex();
            if (idx < 0) {
                return;
            }
            fixedMessages.remove(idx);
            persistFixed();
        });
        send.setOnAction(e -> {
            int idx = fixedList.getSelectionModel().getSelectedIndex();
            if (idx < 0) {
                log("Selecione uma mensagem fixa.");
                return;
            }
            ConfigStore.FixedMessage m = fixedMessages.get(idx);
            sendToTargets(m.type(), m.title(), m.body(), true);
        });

        HBox box = new HBox(6, add, edit, remove, send);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private TitledPane buildLogPane() {
        VBox box = new VBox(logArea);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        box.setPadding(new Insets(4));
        box.setPrefHeight(160);
        TitledPane tp = new TitledPane("Log", box);
        tp.setExpanded(true);
        tp.setCollapsible(false);
        return tp;
    }

    private void openCompose(MsgType type) {
        ComposeDialog dialog = new ComposeDialog(getScene().getWindow(), type);
        dialog.showAndWait();
        if (dialog.isConfirmed()) {
            sendToTargets(dialog.getSelectedType(), dialog.getTitleText(), dialog.getBodyText(),
                    false);
        }
    }

    public void sendCampainha() {
        sendToTargets(MsgType.ALERT, CAMPAINHA_TITLE, CAMPAINHA_BODY, true);
    }

    private List<DestinosStore.Destino> resolveTargets() {
        List<DestinosStore.Destino> selected =
                new ArrayList<>(destList.getSelectionModel().getSelectedItems());
        if (!selected.isEmpty()) {
            return selected;
        }
        return new ArrayList<>(destinos);
    }

    private void sendToTargets(MsgType type, String title, String body, boolean campainha) {
        List<DestinosStore.Destino> targets = resolveTargets();
        if (targets.isEmpty()) {
            log("Nenhum destino cadastrado. Cadastre em Destinos → Novo.");
            return;
        }
        for (DestinosStore.Destino d : targets) {
            long duration = d.effectiveDurationMs();
            if (campainha) {
                duration = Math.min(duration, CAMPAINHA_MAX_MS);
            }
            sendOne(d, type, title, body, duration, d.effectiveScreen());
        }
    }

    private void sendOne(DestinosStore.Destino d, MsgType type, String title,
                         String body, long durationMs, int screen) {
        NotificationMessage msg = new NotificationMessage();
        msg.setToken(d.effectiveToken());
        msg.setType(type);
        msg.setTitle(title);
        msg.setBody(body);
        msg.setDurationMs(durationMs);
        msg.setScreenIndex(screen);
        msg.setSenderFullName(UserNames.realFullName());
        msg.setSenderDomain(UserNames.domainOrGroup());

        String host = d.connectHost();
        int port = d.effectivePortInt();
        String label = d.alias() == null || d.alias().isBlank() ? host : d.alias();
        String ip = d.effectiveHostIp();
        String psk = config.effectivePsk(d.effectiveToken());

        Thread t = new Thread(() -> {
            try {
                NotificationClient.send(host, port, msg, psk);
                String detail = "Enviado [" + type.label() + "] → " + label
                        + " (" + host + ":" + port + ")"
                        + (ip.isEmpty() || ip.equalsIgnoreCase(host) ? ""
                        : " ip=" + ip)
                        + " | tela " + screen + " | " + durationMs + "ms"
                        + (psk.isEmpty() ? "" : " | cifrado")
                        + (d.effectiveDepartment().isEmpty()
                        ? "" : " | dept=" + d.effectiveDepartment());
                javafx.application.Platform.runLater(() -> logAs(LogType.SEND, host, detail));
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> logAs(LogType.ERROR, host,
                        "ERRO " + label + " (" + host + ":" + port + ") — " + ex.getMessage()));
            }
        }, "fastnotify-send");
        t.setDaemon(true);
        t.start();
    }

    private void testSelectedOrAll() {
        List<DestinosStore.Destino> targets = resolveTargets();
        if (targets.isEmpty()) {
            log("Nenhum destino para testar.");
            return;
        }
        for (DestinosStore.Destino d : targets) {
            testDestino(d);
        }
    }

    private void testDestino(DestinosStore.Destino d) {
        String host = d.connectHost();
        int port = d.effectivePortInt();
        String token = d.effectiveToken();
        String label = d.alias() == null || d.alias().isBlank() ? host : d.alias();
        String ip = d.effectiveHostIp();
        String where = ip.isEmpty() || ip.equalsIgnoreCase(host)
                ? host + ":" + port
                : host + ":" + port + " ip=" + ip;
        log("Testando " + label + " (" + where + ") ...");
        Thread t = new Thread(() -> {
            try {
                Protocol.Ack ack = NotificationClient.test(host, port, token,
                        config.effectivePsk(token));
                javafx.application.Platform.runLater(() -> {
                    if (ack.ok()) {
                        logAs(LogType.TEST_OK, host, "TESTE OK — " + label + " — " + ack.detail());
                    } else {
                        logAs(LogType.TEST_FAIL, host,
                                "TESTE FALHOU — " + label + " — " + ack.detail());
                    }
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> logAs(LogType.TEST_FAIL, host,
                        "TESTE FALHOU — " + label + " — " + ex.getMessage()));
            }
        }, "fastnotify-test");
        t.setDaemon(true);
        t.start();
    }

    private void persistFixed() {
        messageStore.save(fixedMessages);
        log("Mensagens salvas em " + messageStore.getFile().toAbsolutePath());
        fixedList.getItems().setAll(fixedMessages);
    }

    private void persistDestinos() {
        destinosStore.save(destinos);
        log("Destinos salvos em " + destinosStore.getFile().toAbsolutePath());
        destList.getItems().setAll(destinos);
    }

    public int getMessageCount() {
        return fixedMessages.size();
    }

    public int getDestinoCount() {
        return destinos.size();
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

    public void setRegisterStatus(String text) {
        registerStatus.setText(text);
    }

    public void checkFirewall() {
        int port = config.getRegisterPort();
        log("Consultando firewall (entrada porta " + port + ", regra "
                + "FastNotify-Origem-Cadastro) ...");
        Thread t = new Thread(() -> {
            String status = FirewallChecker.checkInboundRule(
                    "FastNotify-Origem-Cadastro", port);
            javafx.application.Platform.runLater(() ->
                    logAs(LogType.FIREWALL, "-", status));
        }, "fastnotify-firewall-check");
        t.setDaemon(true);
        t.start();
    }

    public void upsertDestino(DestinosStore.Destino nuevo) {
        DestinosStore.Destino target = nuevo.normalized();
        String hostName = target.connectHost();
        String hostIp = target.effectiveHostIp();
        for (int i = 0; i < destinos.size(); i++) {
            DestinosStore.Destino actual = destinos.get(i);
            boolean match = hostName.equalsIgnoreCase(actual.connectHost())
                    || (!hostIp.isEmpty() && hostIp.equalsIgnoreCase(actual.connectHost()))
                    || (!hostIp.isEmpty() && hostIp.equalsIgnoreCase(actual.effectiveHostIp()))
                    || (!hostName.isEmpty() && !actual.effectiveHostIp().isEmpty()
                    && hostName.equalsIgnoreCase(actual.effectiveHostIp()));
            if (match) {
                destinos.set(i, target);
                persistDestinos();
                log("Destino atualizado por cadastro: " + destinos.get(i).display());
                return;
            }
        }
        destinos.add(target);
        persistDestinos();
        log("Novo destino cadastrado: " + destinos.get(destinos.size() - 1).display());
    }

    public void applyConfigFromDisk() {
        fixedMessages.clear();
        fixedMessages.addAll(messageStore.load());
        fixedList.getItems().setAll(fixedMessages);
        destinos.clear();
        destinos.addAll(destinosStore.load());
        destList.getItems().setAll(destinos);
    }
}
