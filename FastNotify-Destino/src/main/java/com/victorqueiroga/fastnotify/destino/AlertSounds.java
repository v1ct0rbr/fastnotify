package com.victorqueiroga.fastnotify.destino;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

public final class AlertSounds {
    private static final String RESOURCE = "/sounds/alert_sound.mp3";
    private static MediaPlayer last;

    private AlertSounds() {
    }

    public static void play() {
        try {
            URL url = AlertSounds.class.getResource(RESOURCE);
            if (url == null) {
                return;
            }
            if (last != null) {
                last.stop();
                last.dispose();
            }
            Media media = new Media(url.toExternalForm());
            MediaPlayer player = new MediaPlayer(media);
            last = player;
            player.setOnEndOfMedia(() -> {
                player.dispose();
                if (last == player) {
                    last = null;
                }
            });
            player.setOnError(() -> {
                player.dispose();
                if (last == player) {
                    last = null;
                }
            });
            player.play();
        } catch (Exception ignored) {
        }
    }
}
