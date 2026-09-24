package com.victorqueiroga.fastnotify.destino;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class NotificationWindow {
    private static final double WIDTH = 620;
    private static final long MIN_DURATION_MS = 500;
    private static final long MAX_DURATION_MS = 300000;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Stage stage;
    private final PauseTransition autoClose;
    private final boolean playSound;

    public NotificationWindow(NotificationMessage msg) {
        this(msg, true);
    }

    public NotificationWindow(NotificationMessage msg, boolean playSound) {
        this.playSound = playSound;
        MsgType type = msg.getType();
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        long duration = Math.max(MIN_DURATION_MS, Math.min(msg.getDurationMs(), MAX_DURATION_MS));
        autoClose = new PauseTransition(Duration.millis(duration));

        VBox card = buildCard(type, msg);

        StackPane root = new StackPane(card);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setWidth(WIDTH);
        stage.sizeToScene();

        root.setOnMouseClicked(e -> close());
        card.setOnMouseClicked(e -> e.consume());

        playEntrance(root);
        positionCentered(msg.getScreenIndex());

        autoClose.setOnFinished(e -> close());
        autoClose.play();

        stage.show();
        stage.toFront();
        positionCentered(msg.getScreenIndex());
        if (playSound) {
            AlertSounds.play();
        }
    }

    private VBox buildCard(MsgType type, NotificationMessage msg) {
        Region accent = new Region();
        accent.setMinHeight(8);
        accent.setMaxHeight(8);
        accent.setPrefHeight(8);
        accent.setStyle("-fx-background-color: linear-gradient(to right, "
                + type.accentCss() + ", " + lighten(type, 0.35) + ");");

        Label typeLabel = new Label(type.label().toUpperCase());
        typeLabel.setTextFill(Color.web("#0F172A"));
        typeLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.EXTRA_BOLD, 16));

        Label time = new Label(LocalTime.now().format(CLOCK));
        time.setTextFill(Color.web("#94A3B8"));
        time.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.MEDIUM, 13));

        StackPane badge = buildBadge(type);

        HBox header = new HBox(14, badge);
        VBox headText = new VBox(2, typeLabel, time);
        HBox.setHgrow(headText, Priority.ALWAYS);
        header.getChildren().add(headText);
        header.setAlignment(Pos.CENTER_LEFT);

        Region divider = new Region();
        divider.setMinHeight(1);
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color: #E2E8F0;");

        VBox bodyBox = new VBox(14);
        if (msg.getTitle() != null && !msg.getTitle().isBlank()) {
            Label title = new Label(msg.getTitle());
            title.setWrapText(true);
            title.setMaxWidth(WIDTH - 72);
            title.setTextFill(Color.web("#0F172A"));
            title.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 22));
            title.setStyle("-fx-line-spacing: 2px;");
            bodyBox.getChildren().add(title);
        }

        if (msg.getBody() != null && !msg.getBody().isBlank()) {
            Label body = new Label(msg.getBody());
            body.setWrapText(true);
            body.setMaxWidth(WIDTH - 72);
            body.setTextFill(Color.web("#0F172A"));
            body.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.SEMI_BOLD, 22));
            body.setStyle("-fx-line-spacing: 5px;");
            bodyBox.getChildren().add(body);
        }
        if (bodyBox.getChildren().isEmpty()) {
            Label empty = new Label("—");
            empty.setTextFill(Color.web("#94A3B8"));
            empty.setFont(Font.font(24));
            bodyBox.getChildren().add(empty);
        }

        Label hint = new Label("Clique para fechar");
        hint.setTextFill(Color.web("#94A3B8"));
        hint.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.MEDIUM, 12));
        HBox.setHgrow(hint, Priority.ALWAYS);

        Label senderLabel = new Label();
        String sender = msg.getSenderFullName() == null ? "" : msg.getSenderFullName().trim();
        String domain = msg.getSenderDomain() == null ? "" : msg.getSenderDomain().trim();
        String who;
        if (!sender.isEmpty()) {
            who = sender;
        } else if (!domain.isEmpty()) {
            who = domain;
        } else {
            who = System.getProperty("user.name", "");
        }
        boolean hasSender = who != null && !who.isBlank();
        if (hasSender) {
            senderLabel.setText("De: " + who);
            senderLabel.setTextFill(Color.web("#334155"));
            senderLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.SEMI_BOLD, 15));
            senderLabel.setWrapText(true);
            senderLabel.setMaxWidth(WIDTH - 40);
        } else {
            senderLabel.setManaged(false);
            senderLabel.setVisible(false);
        }

        Label closeBtn = new Label("✕");
        closeBtn.setTextFill(Color.web("#64748B"));
        closeBtn.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 16));
        closeBtn.setStyle("-fx-padding: 4 8 4 8; -fx-background-color: #F1F5F9; -fx-background-radius: 6;");
        closeBtn.setOnMouseClicked(e -> close());

        ProgressBar bar = new ProgressBar(1);
        bar.setPrefWidth(140);
        bar.setMaxWidth(140);
        bar.setMinWidth(140);
        bar.setPrefHeight(8);
        bar.setStyle("-fx-accent: " + type.accentCss() + ";"
                + " -fx-control-inner-background: #E2E8F0;");
        wireProgress(bar);

        HBox controls = new HBox(12, hint, bar, closeBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox footer = new VBox(8);
        if (hasSender) {
            footer.getChildren().add(senderLabel);
        }
        footer.getChildren().add(controls);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(hasSender ? 10 : 14, 20, 16, 20));
        footer.setStyle("-fx-background-color: #F8FAFC;"
                + " -fx-background-radius: 0 0 16 16;");

        VBox headerBox = new VBox(0, accent, header);
        headerBox.setPadding(new Insets(0, 20, 14, 20));
        headerBox.setStyle("-fx-background-color: " + lighten(type, 0.92) + ";"
                + " -fx-background-radius: 16 16 0 0;");

        VBox content = new VBox(headerBox, divider, bodyBox, footer);
        content.setPrefWidth(WIDTH);
        content.setMaxWidth(WIDTH);
        content.setSpacing(0);
        VBox.setMargin(bodyBox, new Insets(22, 24, 14, 24));
        bodyBox.setStyle("-fx-background-color: #F1F5F9;"
                + " -fx-background-radius: 10;"
                + " -fx-padding: 14 16 14 16;");
        content.setStyle(
                " -fx-background-color: #FFFFFF;"
                        + " -fx-background-radius: 16;"
                        + " -fx-border-color: " + type.accentCss() + "40;"
                        + " -fx-border-radius: 16;"
                        + " -fx-border-width: 1;"
                        + " -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.45), 32, 0.55, 0, 14);");
        return content;
    }

    private void wireProgress(ProgressBar bar) {
        Duration total = autoClose.getDuration();
        autoClose.currentTimeProperty().addListener((obs, old, cur) -> {
            double left = Math.max(0, 1 - cur.toMillis() / total.toMillis());
            bar.setProgress(left);
        });
    }

    private StackPane buildBadge(MsgType type) {
        Label letter = new Label(type.badge());
        letter.setTextFill(Color.WHITE);
        letter.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.EXTRA_BOLD, 24));
        letter.setAlignment(Pos.CENTER);

        Circle circle = new Circle(26, type.accent());
        StackPane badge = new StackPane(circle, letter);
        badge.setPrefSize(52, 52);
        badge.setMaxSize(52, 52);
        badge.setStyle("-fx-effect: dropshadow(three-pass-box, " + type.accentCss() + "66, 14, 0.5, 0, 3);");
        return badge;
    }

    private void playEntrance(javafx.scene.Node node) {
        node.setOpacity(0);
        node.setScaleX(0.92);
        node.setScaleY(0.92);
        node.setTranslateY(12);
        FadeTransition fade = new FadeTransition(Duration.millis(260), node);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(260), node);
        scale.setToX(1);
        scale.setToY(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(260), node);
        slide.setToY(0);
        new ParallelTransition(fade, scale, slide).play();
    }

    private void positionCentered(int screenIndex) {
        java.util.List<Screen> screens = Screen.getScreens();
        int index = screenIndex;
        if (index < 0 || index >= screens.size()) {
            index = 0;
        }
        Rectangle2D bounds = screens.get(index).getVisualBounds();
        double w = stage.getWidth();
        double h = stage.getHeight();
        double x = bounds.getMinX() + (bounds.getWidth() - w) / 2.0;
        double y = bounds.getMinY() + (bounds.getHeight() - h) / 2.0;
        stage.setX(x);
        stage.setY(y);
    }

    private void close() {
        autoClose.stop();
        stage.close();
    }

    private static String lighten(MsgType type, double amount) {
        Color c = type.accent();
        double r = Math.min(1, c.getRed() + (1 - c.getRed()) * amount);
        double g = Math.min(1, c.getGreen() + (1 - c.getGreen()) * amount);
        double b = Math.min(1, c.getBlue() + (1 - c.getBlue()) * amount);
        return String.format("#%02X%02X%02X",
                (int) Math.round(r * 255),
                (int) Math.round(g * 255),
                (int) Math.round(b * 255));
    }
}
