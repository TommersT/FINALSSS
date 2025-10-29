// tommerst/finalsss/FINALSSS-6bcea5b67907ccf7b6ff1ee8d4aed3787d183a76/draw/src/main/java/com/gabriel/draw/renderer/TextRenderer.java
// Corrected imports and bounds calculation, handle rendering, gradient, and rendering logic
// FIXED: originalColor scope issue
package com.gabriel.draw.renderer;

import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.renderer.ShapeRenderer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage; // Import for temp graphics if needed

public class TextRenderer extends ShapeRenderer {

    // Helper to get FontMetrics safely (Unchanged)
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

    // Helper to calculate bounds based on current text and font (Unchanged)
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

        // --- Check visibility first ---
        if (!shape.isVisible()) {
            return;
        }
        // --- END Check ---

        Point location = shape.getLocation();
        if (location == null) return;
        // Text location is the BASELINE start point
        int x = location.x;
        int y = location.y;

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

        // Get FontMetrics again with the potentially updated font
        FontMetrics fm = getFontMetrics(g2, font);
        int ascent = (fm != null) ? fm.getAscent() : (int)(height * 0.8); // Estimate ascent if fm failed
        // Calculate the visual top-left coordinates based on baseline and ascent
        int visualTopLeftX = x; // Baseline starts near the left edge (padding included in width)
        int visualTopLeftY = y - ascent; // Visual top is baseline minus ascent

        // --- Save state ---
        Font originalFont = g2.getFont();
        Stroke originalStroke = g2.getStroke();
        Composite originalComposite = g2.getComposite();
        Paint originalPaint = g2.getPaint();
        AffineTransform originalTransform = g2.getTransform();
        Color originalColor = g2.getColor(); // <<<--- MOVED DECLARATION HERE

        // --- Apply shape properties ---
        g2.setFont(font);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); // Enable smooth text
        if (!xor && shape.getOpacity() < 1.0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shape.getOpacity()));
        }

        // --- Render ---
        if (xor) {
            // XOR mode usually draws a simple rectangle outline for previews
            g2.setXORMode(shape.getColor() != null ? shape.getColor() : Color.BLACK);
            // Draw the XOR rectangle based on the *visual* top-left and calculated bounds
            g2.drawRect(visualTopLeftX, visualTopLeftY, width, height);
            g2.setPaintMode(); // Reset XOR
        } else {
            // Normal Rendering
            // Draw optional fill background rectangle (Can also be gradient)
            if (shape.isUseGradient() && shape.getStartColor() != null && shape.getEndColor() != null) {
                // Background Gradient
                GradientPaint bgGradient = new GradientPaint(
                        visualTopLeftX, visualTopLeftY, shape.getStartColor(),
                        visualTopLeftX + width, visualTopLeftY + height, shape.getEndColor()
                );
                g2.setPaint(bgGradient);
                g2.fillRect(visualTopLeftX, visualTopLeftY, width, height);
            } else if (shape.getFill() != null) {
                // Solid Background Fill
                g2.setColor(shape.getFill());
                g2.fillRect(visualTopLeftX, visualTopLeftY, width, height);
            }

            // --- Draw the text string itself (Apply Gradient or Solid Fore Color) ---
            if (shape.isUseGradient() && shape.getStartColor() != null && shape.getEndColor() != null) {
                // Apply gradient TO THE TEXT ITSELF
                GradientPaint textGradient = new GradientPaint(
                        x, y - ascent, shape.getStartColor(), // Start color at top of text
                        x, y + fm.getDescent(), shape.getEndColor() // End color at bottom of text (descent)
                        // Adjust coordinates as needed for desired gradient direction
                );
                g2.setPaint(textGradient); // Set gradient paint for text
            } else {
                // Use solid fore color for text
                g2.setPaint(shape.getColor() != null ? shape.getColor() : Color.BLACK); // Use setPaint for consistency
            }
            // Draw string relative to baseline (x, y), add horizontal padding offset (e.g., 5px)
            g2.drawString(textContent, x + 5, y); // x,y is the baseline start
            // --- End Text Drawing ---
        }

        // --- Restore state before drawing handles ---
        // if (xor) g2.setPaintMode(); // Already done above
        g2.setFont(originalFont);
        g2.setStroke(originalStroke);
        g2.setComposite(originalComposite);
        g2.setPaint(originalPaint); // Restore original paint
        g2.setColor(originalColor); // <<<--- RESTORE using the variable declared earlier
        // DO NOT restore transform yet if handles need to be drawn

        // --- Render Handles (if selected and not XOR) ---
        if (shape.isSelected() && !xor) {
            // Handles should be drawn relative to the VISUAL bounds.
            // Temporarily adjust the shape's location for the handle renderer.
            Point originalLocation = shape.getLocation();
            shape.setLocation(new Point(visualTopLeftX, visualTopLeftY)); // Set to visual top-left

            // Now call super.render, which will draw handles around the new temporary location and width/height
            super.render(g, shape, false); // Pass 'false' for XOR mode

            // Restore the shape's original baseline location
            shape.setLocation(originalLocation);
        }

        // Restore transform AFTER handles are drawn
        g2.setTransform(originalTransform);
    }
}