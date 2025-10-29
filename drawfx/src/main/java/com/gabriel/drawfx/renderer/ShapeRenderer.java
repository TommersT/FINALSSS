package com.gabriel.drawfx.renderer;

import com.gabriel.drawfx.model.Shape;

import java.awt.*;

public class ShapeRenderer implements Renderer {

    @Override
    public void render(Graphics g, Shape shape, boolean xor) {
        // Draw handles ONLY if the shape is selected and not in XOR mode
        if (shape.isSelected() && !xor) {
            Point loc = shape.getLocation();
            if (loc == null) return; // Need location to draw handles

            int width = shape.getWidth();
            int height = shape.getHeight();
            int r = shape.getR(); // Use handle size from shape

            Graphics2D g2 = (Graphics2D) g;
            Color handleColor = Color.BLACK;
            Stroke handleStroke = new BasicStroke(1); // Thin stroke for handles

            // --- Adjust location for Text visual bounds before drawing handles ---
            // This logic needs to be aware of Text shapes, ideally via a method on Shape
            // or handled entirely within TextRenderer's override.
            // For now, we assume TextRenderer adjusts coordinates *before* calling super.render
            // OR we add a generic way to get the visual top-left.
            // Let's assume TextRenderer handles the coordinate adjustment for now.

            // Normalize dimensions and location for handle drawing if width/height might be negative
            int x1 = Math.min(loc.x, loc.x + width);
            int y1 = Math.min(loc.y, loc.y + height);
            int widthAbs = Math.abs(width);
            int heightAbs = Math.abs(height);


            // --- Draw the standard 8 handles ---
            // Removed the check for Line type. The base renderer always draws 8 handles.
            // Specific renderers like LineRenderer can override this if needed,
            // or clear the handles drawn by super and draw their own.
            g2.setColor(handleColor);
            g2.setStroke(handleStroke);

            // Use fillRect for solid handles which are easier to see/click
            g2.fillRect(x1 - r, y1 - r, 2*r, 2*r); // UpperLeft
            g2.fillRect(x1 - r, y1 + heightAbs - r, 2*r, 2*r); // LowerLeft
            g2.fillRect(x1 + widthAbs - r, y1 - r, 2*r, 2*r); // UpperRight
            g2.fillRect(x1 + widthAbs - r, y1 + heightAbs - r, 2*r, 2*r); // LowerRight

            g2.fillRect(x1 + widthAbs/2 - r, y1 - r, 2*r, 2*r); // MiddleTop
            g2.fillRect(x1 - r, y1 + heightAbs/2 - r, 2*r, 2*r); // MiddleLeft
            g2.fillRect(x1 + widthAbs - r, y1 + heightAbs/2 - r, 2*r, 2*r); // MiddleRight
            g2.fillRect(x1 + widthAbs/2 - r, y1 + heightAbs - r, 2*r, 2*r); // MiddleBottom

            // No need for XOR mode or composite handling here, as we only draw when !xor
        }
    }
}