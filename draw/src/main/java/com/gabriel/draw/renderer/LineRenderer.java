package com.gabriel.draw.renderer;

import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.renderer.ShapeRenderer;

import java.awt.*;


public class LineRenderer extends ShapeRenderer {

    @Override
    public void render(Graphics g, Shape shape, boolean xor) {

        // --- NEW: Check visibility first ---
        if (!shape.isVisible()) {
            return;
        }
        // --- END NEW ---

        // Basic validation
        if (shape.getLocation() == null) return;

        int x1 = shape.getLocation().x;
        int y1 = shape.getLocation().y;
        // End point is relative to start point via width/height
        int x2 = x1 + shape.getWidth();
        int y2 = y1 + shape.getHeight();

        Graphics2D g2 = (Graphics2D) g;

        // Save original settings
        Stroke originalStroke = g2.getStroke();
        Composite originalComposite = g2.getComposite();
        Paint originalPaint = g2.getPaint(); // Save paint (important for gradient)
        Color originalColor = g2.getColor();

        // Apply common settings
        g2.setStroke(new BasicStroke(shape.getThickness(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // Use round caps/joins for lines
        if (!xor && shape.getOpacity() < 1.0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shape.getOpacity()));
        }

        if(xor) {
            // XOR mode draws line outline
            g2.setXORMode(shape.getColor() != null ? shape.getColor() : Color.BLACK);
            g2.drawLine(x1, y1, x2, y2);
            g2.setPaintMode(); // Reset XOR
        }
        else {
            // --- Apply Gradient or Solid Color to Stroke ---
            if (shape.isUseGradient() && shape.getStartColor() != null && shape.getEndColor() != null) {
                // Create gradient along the line
                GradientPaint gradient = new GradientPaint(
                        x1, y1, shape.getStartColor(),
                        x2, y2, shape.getEndColor()
                );
                g2.setPaint(gradient); // Set paint for the stroke
            } else {
                // Use solid fore color
                g2.setColor(shape.getColor() != null ? shape.getColor() : Color.BLACK);
            }
            // --- END Color/Gradient Logic ---

            // Draw the line
            g2.drawLine(x1, y1, x2, y2);
        }

        // Restore original settings before drawing handles
        g2.setStroke(originalStroke);
        g2.setComposite(originalComposite);
        g2.setPaint(originalPaint); // Restore paint
        g2.setColor(originalColor);

        // Call super.render to draw handles (will draw 8 handles by default)
        // If you only want handles at the ends for lines, you'd need to override
        // the handle drawing logic here or in a specialized LineShapeRenderer.
        super.render(g, shape, xor); // Handles drawn if selected and not XOR
    }
}