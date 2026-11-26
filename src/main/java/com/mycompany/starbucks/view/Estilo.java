package com.mycompany.starbucks.view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.ui.FlatLineBorder;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;

public final class Estilo {
    static final Color FONDO = Color.decode("#101413");
    static final Color SUPERFICIE = Color.decode("#202826");
    static final Color VERDE = Color.decode("#006241");
    static final Color MENU = Color.decode("#12372A");
    static final Color TEXTO = Color.decode("#F5F5F5");
    static final Color SECUNDARIO = Color.decode("#BDBDBD");
    static final Color MENTA = Color.decode("#D4E9E2");
    static final Color BORDE = Color.decode("#35403C");

    private Estilo() {}

    public static void instalar() {
        FlatDarkLaf.setup();
        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        UIManager.put("Panel.background", FONDO);
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Table.background", SUPERFICIE);
        UIManager.put("Table.foreground", TEXTO);
        UIManager.put("Table.alternateRowColor", Color.decode("#252F2B"));
        UIManager.put("Table.selectionBackground", VERDE);
        UIManager.put("TableHeader.background", Color.decode("#283732"));
        UIManager.put("TableHeader.foreground", MENTA);
        UIManager.put("Table.showHorizontalLines", false);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("ScrollPane.background", SUPERFICIE);
    }

    static Border tarjeta(int espacio) {
        return new FlatLineBorder(new Insets(espacio, espacio, espacio, espacio), BORDE, 1, 16);
    }

    static Icon icono(String tipo, Color color) {
        return new Icon() {
            @Override public int getIconWidth() { return 20; }
            @Override public int getIconHeight() { return 20; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D p = (Graphics2D) g.create(x, y, 20, 20);
                try {
                    p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    p.setColor(color);
                    p.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    switch (tipo) {
                        case "cafe" -> {
                            p.drawRoundRect(3, 8, 11, 9, 4, 4);
                            p.drawArc(12, 9, 6, 5, -90, 180);
                            for (int i : new int[]{5, 8, 11}) p.drawLine(i, 3, i, 5);
                        }
                        case "inicio" -> {
                            for (int i : new int[]{3, 11})
                                for (int j : new int[]{3, 11}) p.drawRoundRect(i, j, 6, 6, 2, 2);
                        }
                        case "personal", "usuarios" -> {
                            p.drawOval(7, 3, 6, 6);
                            p.drawArc(3, 11, 14, 10, 0, 180);
                        }
                        case "periodos" -> {
                            p.drawRoundRect(3, 5, 14, 12, 3, 3);
                            p.drawLine(3, 9, 17, 9);
                            p.drawLine(7, 3, 7, 6);
                            p.drawLine(13, 3, 13, 6);
                        }
                        default -> {
                            p.drawRoundRect(4, 3, 12, 14, 3, 3);
                            p.drawLine(7, 7, 13, 7);
                            p.drawLine(7, 10, 13, 10);
                            p.drawLine(7, 13, 11, 13);
                        }
                    }
                } finally {
                    p.dispose();
                }
            }
        };
    }

    static JLabel marca() {
        JLabel marca = new JLabel(icono("cafe", Color.WHITE), SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D p = (Graphics2D) g.create();
                p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                p.setColor(VERDE);
                p.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                p.dispose();
                super.paintComponent(g);
            }
        };
        marca.setPreferredSize(new Dimension(45, 45));
        return marca;
    }
}
