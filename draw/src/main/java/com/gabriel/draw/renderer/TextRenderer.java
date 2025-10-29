// tommerst/finalsss/FINALSSS-de875fc7483c0551319e977be48e282c3289c2a8/draw/src/main/java/com/gabriel/draw/renderer/TextRenderer.java
// Corrected typo getAscent()
package com.gabriel.draw.renderer;

import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.renderer.ShapeRenderer;

import java.awt.*;
import java.awt.image.BufferedImage; // Import for temp graphics if needed

public class TextRenderer extends ShapeRenderer {

    // Helper to get FontMetrics safely
    private FontMetrics getFontMetrics(Graphics g, Font font) {
        if (g == null || font == null) {
            // Fallback: create temporary graphics if g is null
            if (g == null) {
                Graphics tempG = null;
                try {
                    tempG = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
                    if (tempG != null) {
                        return tempG.getFontMetrics(font);
                    }
                } catch (Exception e) {
                    System.err.println("Error creating temp graphics for FontMetrics: " + e.getMessage());
                } finally {
                    if (tempG != null) tempG.dispose();
                }
            }
            System.err.println("Warning: Could not get FontMetrics.");
            return null; // Indicate failure
        }
        // Use provided graphics context
        try {
            return g.getFontMetrics(font);
        } catch (Exception e) {
            System.err.println("Error getting FontMetrics from provided Graphics: " + e.getMessage());
            return null;
        }
    }

    // Helper to calculate bounds based on current text and font
    private Dimension calculateBounds(Graphics g, Shape shape) {
        Font font = shape.getFont();
        String textContent = shape.getText();

        // Use defaults if missing
        if (font == null) font = new Font("SansSerif", Font.PLAIN, 12);
        if (textContent == null || textContent.isEmpty()) textContent = "Text"; // Ensure non-empty for calculation

        FontMetrics fm = getFontMetrics(g, font);
        if (fm == null) {
            // Fallback if font metrics failed
            return new Dimension(Math.max(10, textContent.length() * 8), 16); // Ensure non-zero
        }

        int textWidth = fm.stringWidth(textContent);
        // *** CORRECTED TYPO HERE ***
        int textHeight = fm.getAscent() + fm.getDescent(); // Use ascent+descent

        // Add padding and ensure minimum size
        return new Dimension(Math.max(10, textWidth + 10), Math.max(10, textHeight + 5));
    }


    @Override
    public void render(Graphics g, Shape shape, boolean xor) {
        Point location = shape.getLocation();
        if (location == null) return;
        int x = location.x; // Text baseline X
        int y = location.y; // Text baseline Y

        Font font = shape.getFont();
        String textContent = shape.getText();

        // Use defaults if font or text is missing for rendering
        if (font == null) font = new Font("SansSerif", Font.PLAIN, 12);
        if (textContent == null || textContent.isEmpty()) textContent = "Text"; // Render placeholder

        Graphics2D g2 = (Graphics2D) g;

        // --- Ensure Bounds are Calculated and Set ---
        Dimension calculatedBounds = calculateBounds(g2, shape);
        shape.setWidth(calculatedBounds.width);
        shape.setHeight(calculatedBounds.height);
        int width = shape.getWidth();
        int height = shape.getHeight();
        // --- End Bounds Calculation ---

        FontMetrics fm = getFontMetrics(g2, font);
        // *** CORRECTED TYPO HERE ***
        int ascent = (fm != null) ? fm.getAscent() : (int)(height * 0.8); // Estimate ascent
        int visualTopY = y - ascent; // Calculate the visual top-left Y coordinate

        // --- Save state ---
        Font originalFont = g2.getFont();
        Stroke originalStroke = g2.getStroke();
        Composite originalComposite = g2.getComposite();
        Paint originalPaint = g2.getPaint();

        // --- Apply shape properties ---
        g2.setFont(font);
        if (!xor && shape.getOpacity() < 1.0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shape.getOpacity()));
        }

        // --- Render ---
        if (xor) {
            g2.setXORMode(shape.getColor() != null ? shape.getColor() : Color.BLACK);
            g2.drawRect(x, visualTopY, width, height);
        } else {
            if (shape.getFill() != null) {
                g2.setColor(shape.getFill());
                g2.fillRect(x, visualTopY, width, height);
            }
            g2.setColor(shape.getColor() != null ? shape.getColor() : Color.BLACK);
            g2.drawString(textContent, x + 5, y);
        }

        // --- Restore state ---
        if (xor) g2.setPaintMode();
        g2.setFont(originalFont);
        g2.setStroke(originalStroke);
        g2.setComposite(originalComposite);
        g2.setPaint(originalPaint);

        // --- Render Handles ---
        if (shape.isSelected() && !xor) {
            g2.translate(0, -ascent);
            super.render(g, shape, false);
            g2.translate(0, ascent);
        }
    }
}