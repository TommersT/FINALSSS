package com.gabriel.draw.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public class ModernIconFactory {
    private static final int ICON_SIZE = 24;
    private static final Color ICON_COLOR = new Color(60, 60, 60);
    private static final int STROKE_WIDTH = 2;

    public static ImageIcon createIcon(String type) {
        BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setColor(ICON_COLOR);
        g2.setStroke(new BasicStroke(STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (type.toLowerCase()) {
            case "undo":
                drawUndo(g2);
                break;
            case "redo":
                drawRedo(g2);
                break;
            case "select":
                drawSelect(g2);
                break;
            case "move":
                drawMove(g2);
                break;
            case "scale":
                drawScale(g2);
                break;
            case "line":
                drawLine(g2);
                break;
            case "rect":
            case "rectangle":
                drawRectangle(g2);
                break;
            case "ellipse":
                drawEllipse(g2);
                break;
            case "text":
                drawText(g2);
                break;
            case "image":
                drawImage(g2);
                break;
            case "color":
                drawColor(g2);
                break;
            case "fill":
                drawFill(g2);
                break;
        }

        g2.dispose();
        return new ImageIcon(image);
    }

    private static void drawUndo(Graphics2D g2) {
        Path2D path = new Path2D.Float();
        path.moveTo(16, 12);
        path.curveTo(16, 8, 13, 5, 9, 5);
        path.lineTo(5, 5);
        g2.draw(path);
        g2.fillPolygon(new int[]{5, 9, 9}, new int[]{5, 2, 8}, 3);
    }

    private static void drawRedo(Graphics2D g2) {
        Path2D path = new Path2D.Float();
        path.moveTo(8, 12);
        path.curveTo(8, 8, 11, 5, 15, 5);
        path.lineTo(19, 5);
        g2.draw(path);
        g2.fillPolygon(new int[]{19, 15, 15}, new int[]{5, 2, 8}, 3);
    }

    private static void drawSelect(Graphics2D g2) {
        int[] xPoints = {4, 4, 9, 12, 14, 10, 10};
        int[] yPoints = {4, 18, 14, 16, 14, 10, 7};
        g2.fillPolygon(xPoints, yPoints, 7);
    }

    private static void drawMove(Graphics2D g2) {
        g2.drawLine(12, 5, 12, 19);
        g2.drawLine(5, 12, 19, 12);
        g2.fillPolygon(new int[]{12, 9, 15}, new int[]{5, 8, 8}, 3);
        g2.fillPolygon(new int[]{12, 9, 15}, new int[]{19, 16, 16}, 3);
        g2.fillPolygon(new int[]{5, 8, 8}, new int[]{12, 9, 15}, 3);
        g2.fillPolygon(new int[]{19, 16, 16}, new int[]{12, 9, 15}, 3);
    }

    private static void drawScale(Graphics2D g2) {
        g2.drawRect(6, 6, 12, 12);
        g2.fillPolygon(new int[]{5, 7, 7}, new int[]{5, 5, 7}, 3);
        g2.fillPolygon(new int[]{19, 17, 17}, new int[]{5, 5, 7}, 3);
        g2.fillPolygon(new int[]{5, 7, 7}, new int[]{19, 17, 19}, 3);
        g2.fillPolygon(new int[]{19, 17, 17}, new int[]{19, 17, 19}, 3);
    }

    private static void drawLine(Graphics2D g2) {
        g2.drawLine(5, 19, 19, 5);
    }

    private static void drawRectangle(Graphics2D g2) {
        g2.drawRect(5, 5, 14, 14);
    }

    private static void drawEllipse(Graphics2D g2) {
        g2.drawOval(5, 5, 14, 14);
    }

    private static void drawText(Graphics2D g2) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString("A", 7, 18);
    }

    private static void drawImage(Graphics2D g2) {
        g2.drawRect(5, 5, 14, 14);
        g2.fillOval(8, 8, 3, 3);
        int[] xPoints = {6, 11, 16, 18};
        int[] yPoints = {18, 13, 15, 18};
        g2.drawPolyline(xPoints, yPoints, 4);
    }

    private static void drawColor(Graphics2D g2) {
        g2.setColor(new Color(255, 100, 100));
        g2.fillOval(4, 4, 16, 16);
        g2.setColor(ICON_COLOR);
        g2.drawOval(4, 4, 16, 16);
    }

    private static void drawFill(Graphics2D g2) {
        int[] xPoints = {12, 7, 7, 17, 17};
        int[] yPoints = {6, 11, 18, 18, 11};
        g2.fillPolygon(xPoints, yPoints, 5);
        g2.drawRect(8, 12, 8, 6);
    }
}
