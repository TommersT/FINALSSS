package com.gabriel.draw.renderer;

import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.renderer.ShapeRenderer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class TextRenderer extends ShapeRenderer {

    private FontMetrics getFontMetrics(Graphics g, Font font) {
        if (font == null) {
            font = new Font("SansSerif", Font.PLAIN, 12);
        }
        if (g == null) {
            Graphics tempG = null;
            try {
                tempG = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
                if (tempG != null) return tempG.getFontMetrics(font);
            } catch (Exception e) { System.err.println("Error creating temp graphics for FontMetrics: " + e.getMessage());
            } finally { if (tempG != null) tempG.dispose(); }
            System.err.println("Warning: Could not get FontMetrics.");
            return null;
        }
        try { return g.getFontMetrics(font); }
        catch (Exception e) { System.err.println("Error getting FontMetrics from provided Graphics: " + e.getMessage()); return null; }
    }

    private Dimension calculateBounds(Graphics g, Shape shape) {
        Font font = shape.getFont(); String textContent = shape.getText();
        if (font == null) font = new Font("SansSerif", Font.PLAIN, 12);
        if (textContent == null || textContent.isEmpty()) textContent = "Text";
        FontMetrics fm = getFontMetrics(g, font);
        if (fm == null) return new Dimension(Math.max(10, textContent.length() * 8), 16);
        int textWidth = fm.stringWidth(textContent); int textHeight = fm.getAscent() + fm.getDescent();
        return new Dimension(Math.max(10, textWidth + 10), Math.max(10, textHeight + 5));
    }

    @Override
    public void render(Graphics g, Shape shape, boolean xor) {

        if (!shape.isVisible()) {
            return;
        }

        Point location = shape.getLocation();
        if (location == null) return;
        int x = location.x;
        int y = location.y;

        Font font = shape.getFont();
        String textContent = shape.getText();

        if (font == null) font = new Font("SansSerif", Font.PLAIN, 12);
        if (textContent == null || textContent.isEmpty()) textContent = "Text";

        Graphics2D g2 = (Graphics2D) g;

        Dimension calculatedBounds = calculateBounds(g2, shape);
        shape.setWidth(calculatedBounds.width);
        shape.setHeight(calculatedBounds.height);
        int width = shape.getWidth();
        int height = shape.getHeight();

        FontMetrics fm = getFontMetrics(g2, font);
        int ascent = (fm != null) ? fm.getAscent() : (int)(height * 0.8);
        int visualTopLeftX = x;
        int visualTopLeftY = y - ascent;

        Font originalFont = g2.getFont();
        Stroke originalStroke = g2.getStroke();
        Composite originalComposite = g2.getComposite();
        Paint originalPaint = g2.getPaint();
        AffineTransform originalTransform = g2.getTransform();
        Color originalColor = g2.getColor();

        g2.setFont(font);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (!xor && shape.getOpacity() < 1.0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shape.getOpacity()));
        }

        if (xor) {
            g2.setXORMode(shape.getColor() != null ? shape.getColor() : Color.BLACK);
            g2.drawRect(visualTopLeftX, visualTopLeftY, width, height);
            g2.setPaintMode();
        } else {
            if (shape.isUseGradient() && shape.getStartColor() != null && shape.getEndColor() != null) {
                GradientPaint bgGradient = new GradientPaint(
                        visualTopLeftX, visualTopLeftY, shape.getStartColor(),
                        visualTopLeftX + width, visualTopLeftY + height, shape.getEndColor()
                );
                g2.setPaint(bgGradient);
                g2.fillRect(visualTopLeftX, visualTopLeftY, width, height);
            } else if (shape.getFill() != null) {
                g2.setColor(shape.getFill());
                g2.fillRect(visualTopLeftX, visualTopLeftY, width, height);
            }

            if (shape.isUseGradient() && shape.getStartColor() != null && shape.getEndColor() != null) {
                GradientPaint textGradient = new GradientPaint(
                        x, y - ascent, shape.getStartColor(),
                        x, y + fm.getDescent(), shape.getEndColor()
                );
                g2.setPaint(textGradient);
            } else {
                g2.setPaint(shape.getColor() != null ? shape.getColor() : Color.BLACK);
            }
            g2.drawString(textContent, x + 5, y);
        }

        g2.setFont(originalFont);
        g2.setStroke(originalStroke);
        g2.setComposite(originalComposite);
        g2.setPaint(originalPaint);
        g2.setColor(originalColor);

        if (shape.isSelected() && !xor) {
            Point originalLocation = shape.getLocation();
            shape.setLocation(new Point(visualTopLeftX, visualTopLeftY));
            super.render(g, shape, false);
            shape.setLocation(originalLocation);
        }

        g2.setTransform(originalTransform);
    }
}
