package com.gabriel.draw.renderer;

import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.renderer.ShapeRenderer;

import java.awt.*;

public class RectangleRenderer extends ShapeRenderer {

    @Override
    public void render(Graphics g, Shape shape, boolean xor) {

        // --- NEW: Check visibility first ---
        if (!shape.isVisible()) {
            return; // Don't draw anything if not visible
        }
        // --- END NEW ---

        // Basic validation
        if (shape.getLocation() == null) return;

        int x = shape.getLocation().x;
        int y = shape.getLocation().y;
        int width = shape.getWidth();
        int height = shape.getHeight();

        // Ensure width/height are non-negative for drawing calculations
        if (width < 0) {
            x += width;
            width = -width;
        }
        if (height < 0) {
            y += height;
            height = -height;
        }


        Graphics2D g2 = (Graphics2D) g;

        // Save original settings
        Stroke originalStroke = g2.getStroke();
        Composite originalComposite = g2.getComposite();
        Paint originalPaint = g2.getPaint(); // Save original paint/color
        Color originalColor = g2.getColor();

        // Apply common settings
        g2.setStroke(new BasicStroke(shape.getThickness()));
        if (!xor && shape.getOpacity() < 1.0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shape.getOpacity()));
        }

        if (xor) {
            // XOR usually just draws outline
            g2.setXORMode(shape.getColor() != null ? shape.getColor() : Color.BLACK);
            g2.drawRect(x, y, width, height);
            g2.setPaintMode(); // Reset XOR mode
        } else {
            // --- NEW: Gradient Fill Logic ---
            if (shape.isUseGradient() && shape.getStartColor() != null && shape.getEndColor() != null) {
                // Create and apply gradient paint
                GradientPaint gradient = new GradientPaint(
                        x, y, shape.getStartColor(),
                        x + width, y + height, shape.getEndColor() // Simple diagonal gradient
                        // You could adjust gradient direction (e.g., x, y+height for vertical)
                );
                g2.setPaint(gradient);
                g2.fillRect(x, y, width, height);
            }
            // --- Original Solid Fill Logic ---
            else if (shape.getFill() != null) {
                g2.setColor(shape.getFill());
                g2.fillRect(x, y, width, height);
            }
            // --- END Fill Logic ---


            // Draw outline (Fore Color)
            g2.setColor(shape.getColor() != null ? shape.getColor() : Color.BLACK); // Use shape's fore color
            g2.drawRect(x, y, width, height);
        }

        // Restore original settings before drawing handles
        g2.setStroke(originalStroke);
        g2.setComposite(originalComposite);
        g2.setPaint(originalPaint); // Restore paint/color
        g2.setColor(originalColor);


        // Call super.render to draw handles AFTER restoring graphics settings
        // Super.render uses its own colors/strokes for handles
        super.render(g, shape, xor); // Handles are drawn if selected and not XOR

    }
}
