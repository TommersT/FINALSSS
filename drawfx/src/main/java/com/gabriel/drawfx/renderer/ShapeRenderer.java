package com.gabriel.drawfx.renderer;

import com.gabriel.drawfx.model.Shape;

import java.awt.*;

public class ShapeRenderer implements Renderer {

    @Override
    public void render(Graphics g, Shape shape, boolean xor) {
        if (shape.isSelected() && !xor) {
            Point loc = shape.getLocation();
            if (loc == null) return;

            int width = shape.getWidth();
            int height = shape.getHeight();
            int r = shape.getR();

            Graphics2D g2 = (Graphics2D) g;
            Color handleColor = Color.BLACK;
            Stroke handleStroke = new BasicStroke(1);

            int x1 = Math.min(loc.x, loc.x + width);
            int y1 = Math.min(loc.y, loc.y + height);
            int widthAbs = Math.abs(width);
            int heightAbs = Math.abs(height);

            g2.setColor(handleColor);
            g2.setStroke(handleStroke);

            g2.fillRect(x1 - r, y1 - r, 2*r, 2*r);
            g2.fillRect(x1 - r, y1 + heightAbs - r, 2*r, 2*r);
            g2.fillRect(x1 + widthAbs - r, y1 - r, 2*r, 2*r);
            g2.fillRect(x1 + widthAbs - r, y1 + heightAbs - r, 2*r, 2*r);

            g2.fillRect(x1 + widthAbs/2 - r, y1 - r, 2*r, 2*r);
            g2.fillRect(x1 - r, y1 + heightAbs/2 - r, 2*r, 2*r);
            g2.fillRect(x1 + widthAbs - r, y1 + heightAbs/2 - r, 2*r, 2*r);
            g2.fillRect(x1 + widthAbs/2 - r, y1 + heightAbs - r, 2*r, 2*r);
        }
    }
}
