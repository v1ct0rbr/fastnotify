package com.victorqueiroga.fastnotify.origem;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URL;

public final class TrayIcons {
    private static final int TRAY_PX = 32;

    private TrayIcons() {
    }

    public static BufferedImage loadAwt(String classpathResource) {
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
            int w = TRAY_PX;
            int h = Math.max(1, Math.round(TRAY_PX * (float) size.height / (float) size.width));
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
}
