// tommerst/finalsss/FINALSSS-6bcea5b67907ccf7b6ff1ee8d4aed3787d183a76/draw/src/main/java/com/gabriel/draw/renderer/TextRenderer.java
// Corrected imports and bounds calculation, handle rendering, and rendering logic
package com.gabriel.draw.renderer;

import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.renderer.ShapeRenderer;

import java.awt.*;
import java.awt.geom.AffineTransform; // <-- ADDED: Import AffineTransform
import java.awt.image.BufferedImage; // Import for temp graphics if needed

public class TextRenderer extends ShapeRenderer {

    // Helper to get FontMetrics safely
    private FontMetrics getFontMetrics(Graphics g, Font font) {
        if (font == null) {
            // Use a default font if null
            font = new Font("SansSerif", Font.PLAIN, 12);
        }
        if (g == null) {
            // Fallback: create temporary graphics if g is null
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
        int textHeight = fm.getAscent() + fm.getDescent(); // Use ascent+descent

        // Add padding and ensure minimum size
        // Padding helps with selection hit area and visual spacing for handles
        return new Dimension(Math.max(10, textWidth + 10), Math.max(10, textHeight + 5));
    }


    @Override
    public void render(Graphics g, Shape shape, boolean xor) {
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
        // Recalculate bounds every time in case font/text changed
        Dimension calculatedBounds = calculateBounds(g2, shape);
        // *** Update the shape's actual width/height ***
        shape.setWidth(calculatedBounds.width);
        shape.setHeight(calculatedBounds.height);
        int width = shape.getWidth();
        int height = shape.getHeight();
        // --- End Bounds Calculation ---

        // Get FontMetrics again with the potentially updated font
        FontMetrics fm = getFontMetrics(g2, font);
        int ascent = (fm != null) ? fm.getAscent() : (int)(height * 0.8); // Estimate ascent if fm failed
        // Calculate the visual top-left coordinates based on baseline and ascent
        int visualTopLeftX = x; // Baseline starts at the left edge + padding included in width
        int visualTopLeftY = y - ascent; // Visual top is baseline minus ascent

        // --- Save state ---
        Font originalFont = g2.getFont();
        Stroke originalStroke = g2.getStroke();
        Composite originalComposite = g2.getComposite();
        Paint originalPaint = g2.getPaint();
        AffineTransform originalTransform = g2.getTransform(); // <-- CORRECTED: Save transform

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
        } else {
            // Normal Rendering
            // Draw optional fill background rectangle
            if (shape.getFill() != null) {
                g2.setColor(shape.getFill());
                // Fill based on visual top-left and calculated bounds
                g2.fillRect(visualTopLeftX, visualTopLeftY, width, height);
            }
            // Draw the text string itself
            g2.setColor(shape.getColor() != null ? shape.getColor() : Color.BLACK);
            // Draw string relative to baseline (x, y), add padding offset (e.g., 5px)
            g2.drawString(textContent, x + 5, y); // x,y is the baseline start
        }

        // --- Restore state before drawing handles ---
        if (xor) g2.setPaintMode();
        g2.setFont(originalFont);
        g2.setStroke(originalStroke);
        g2.setComposite(originalComposite);
        g2.setPaint(originalPaint);
        // DO NOT restore transform yet if handles need to be drawn

        // --- Render Handles (if selected and not XOR) ---
        if (shape.isSelected() && !xor) {
            // Handles should be drawn relative to the VISUAL bounds.
            // ShapeRenderer expects location to be top-left.
            // Temporarily adjust the shape's location for the handle renderer.
            Point originalLocation = shape.getLocation();
            shape.setLocation(new Point(visualTopLeftX, visualTopLeftY)); // Set to visual top-left

            // Now call super.render, which will draw handles around the new temporary location and width/height
            super.render(g, shape, false); // Pass 'false' for XOR mode

            // Restore the shape's original baseline location
            shape.setLocation(originalLocation);
        }

        // Restore transform if it wasn't restored earlier
        g2.setTransform(originalTransform); // <-- CORRECTED: Restore transform
    }
}