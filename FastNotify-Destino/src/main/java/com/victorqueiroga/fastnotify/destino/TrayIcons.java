package com.victorqueiroga.fastnotify.destino;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URL;

public final class TrayIcons {
    private static final int TRAY_PX = 32;
    private static final int[] WINDOW_PX = {16, 24, 32, 48, 64, 128, 256};

    private TrayIcons() {
    }

    public static BufferedImage loadAwt(String classpathResource) {
        return renderAwt(classpathResource, TRAY_PX);
    }

    public static void applyWindowIcons(Stage stage, String classpathResource) {
        if (stage == null) {
            return;
        }
        for (int px : WINDOW_PX) {
            try {
                stage.getIcons().add(toFxImage(renderAwt(classpathResource, px)));
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static BufferedImage renderAwt(String classpathResource, int targetPx) {
        URL url = TrayIcons.class.getResource(classpathResource);
        if (url == null) {
            throw new IllegalStateException("Recurso não encontrado: " + classpathResource);
        }
        try {
            SVGDocument document = new SVGLoader().load(url);
            if (document == null) {
                throw new IllegalStateException("SVG inválido: " + classpathResource);
            }
            FloatSize size = document.size();
            int w = targetPx;
            int h = Math.max(1, Math.round(targetPx * (float) size.height / (float) size.width));
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                double scale = Math.min((double) w / size.width, (double) h / size.height);
                g.scale(scale, scale);
                document.render(null, g);
            } finally {
                g.dispose();
            }
            return bi;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao carregar SVG: " + classpathResource, e);
        }
    }

    private static Image toFxImage(BufferedImage bi) {
        int w = bi.getWidth();
        int h = bi.getHeight();
        int[] pixels = bi.getRGB(0, 0, w, h, null, 0, w);
        WritableImage wr = new WritableImage(w, h);
        wr.getPixelWriter().setPixels(0, 0, w, h,
                PixelFormat.getIntArgbInstance(), pixels, 0, w);
        return wr;
    }
}
