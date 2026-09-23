package com.victorqueiroga.fastnotify.destino;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public final class CloseDialog {
    public enum Choice {
        HIDE, EXIT, CANCEL
    }

    private CloseDialog() {
    }

    public static Choice show(Window owner, String appName) {
        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        Label icon = new Label("!");
        icon.setMinSize(44, 44);
        icon.setMaxSize(44, 44);
        icon.setAlignment(Pos.CENTER);
        icon.setStyle("-fx-background-color: #F59E0B;"
                + " -fx-background-radius: 22;"
                + " -fx-text-fill: white;"
                + " -fx-font-size: 26px;"
                + " -fx-font-weight: bold;");

        Label title = new Label("Fechar " + appName + "?");
        title.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.EXTRA_BOLD, 18));
        title.setTextFill(Color.web("#0F172A"));

        Label subtitle = new Label("O aplicativo não encerra sozinho ao fechar a janela. Escolha o que fazer:");
        subtitle.setFont(Font.font(13));
        subtitle.setTextFill(Color.web("#64748B"));
        subtitle.setWrapText(true);

        VBox headerText = new VBox(4, title, subtitle);
        headerText.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(14, icon, headerText);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox hideCard = infoCard("#ECFDF5", "#A7F3D0",
                "Esconder no tray",
                "Escuta continua em segundo plano — reabre pelo ícone da bandeja.");
        HBox exitCard = infoCard("#FEF2F2", "#FECACA",
                "Sair completamente",
                "Encerra o processo, para a escuta e remove o tray.");

        Button hideBtn = primaryButton("Esconder no tray", "#059669");
        Button exitBtn = dangerButton("Sair completamente");
        Button cancelBtn = neutralButton("Cancelar");

        HBox actions = new HBox(10, cancelBtn, exitBtn, hideBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(16, header, hideCard, exitCard, actions);
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color: white;"
                + " -fx-border-color: #E2E8F0;"
                + " -fx-border-width: 1;"
                + " -fx-background-radius: 14;"
                + " -fx-border-radius: 14;"
                + " -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.28), 24, 0.45, 0, 10);");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.sizeToScene();

        Choice[] result = {Choice.CANCEL};
        hideBtn.setOnAction(e -> {
            result[0] = Choice.HIDE;
            dialog.close();
        });
        exitBtn.setOnAction(e -> {
            result[0] = Choice.EXIT;
            dialog.close();
        });
        cancelBtn.setOnAction(e -> {
            result[0] = Choice.CANCEL;
            dialog.close();
        });
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                result[0] = Choice.CANCEL;
                dialog.close();
            }
        });

        if (owner != null) {
            dialog.setX(owner.getX() + Math.max(0, (owner.getWidth() - dialog.getWidth()) / 2));
            dialog.setY(owner.getY() + Math.max(0, (owner.getHeight() - dialog.getHeight()) / 2));
        }
        dialog.showAndWait();
        return result[0];
    }

    private static HBox infoCard(String bg, String border, String titleLabel, String subtitleText) {
        Label t = new Label(titleLabel);
        t.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 13));
        t.setTextFill(Color.web("#0F172A"));

        Label s = new Label(subtitleText);
        s.setFont(Font.font(12));
        s.setTextFill(Color.web("#64748B"));
        s.setWrapText(true);

        VBox box = new VBox(2, t, s);
        HBox card = new HBox(box);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: " + bg + ";"
                + " -fx-background-radius: 10;"
                + " -fx-border-color: " + border + ";"
                + " -fx-border-radius: 10;"
                + " -fx-border-width: 1;");
        return card;
    }

    private static Button primaryButton(String text, String bg) {
        return styled(text, bg, "#047857", "white");
    }

    private static Button dangerButton(String text) {
        return styled(text, "#DC2626", "#B91C1C", "white");
    }

    private static Button neutralButton(String text) {
        return styled(text, "#F8FAFC", "#E2E8F0", "#334155");
    }

    private static Button styled(String text, String bg, String border, String textFill) {
        Button b = new Button(text);
        b.setPrefHeight(38);
        b.setMinWidth(130);
        b.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 13));
        b.setStyle("-fx-background-color: " + bg + ";"
                + " -fx-background-radius: 10;"
                + " -fx-border-color: " + border + ";"
                + " -fx-border-radius: 10;"
                + " -fx-border-width: 1;"
                + " -fx-text-fill: " + textFill + ";"
                + " -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: " + shade(bg, -14) + ";"
                + " -fx-background-radius: 10;"
                + " -fx-border-color: " + border + ";"
                + " -fx-border-radius: 10;"
                + " -fx-border-width: 1;"
                + " -fx-text-fill: " + textFill + ";"
                + " -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: " + bg + ";"
                + " -fx-background-radius: 10;"
                + " -fx-border-color: " + border + ";"
                + " -fx-border-radius: 10;"
                + " -fx-border-width: 1;"
                + " -fx-text-fill: " + textFill + ";"
                + " -fx-cursor: hand;"));
        return b;
    }

    private static String shade(String hex, int delta) {
        try {
            int r = clamp(Integer.parseInt(hex.substring(1, 3), 16) + delta);
            int g = clamp(Integer.parseInt(hex.substring(3, 5), 16) + delta);
            int b = clamp(Integer.parseInt(hex.substring(5, 7), 16) + delta);
            return String.format("#%02X%02X%02X", r, g, b);
        } catch (Exception e) {
            return hex;
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
