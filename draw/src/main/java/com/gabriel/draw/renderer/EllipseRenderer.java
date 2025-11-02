package com.gabriel.draw.renderer;

import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.renderer.ShapeRenderer;

import java.awt.*;

public class EllipseRenderer extends ShapeRenderer {

    @Override
    public void render(Graphics g, Shape shape, boolean xor) {
        if (!shape.isVisible()) {
            return;
        }

        if (shape.getLocation() == null) return;

        int x = shape.getLocation().x;
        int y = shape.getLocation().y;
        int width = shape.getWidth();
        int height = shape.getHeight();

        if (width < 0) {
            x += width;
            width = -width;
        }
        if (height < 0) {
            y += height;
            height = -height;
        }

        Graphics2D g2 = (Graphics2D) g;

        Stroke originalStroke = g2.getStroke();
        Composite originalComposite = g2.getComposite();
        Paint originalPaint = g2.getPaint();
        Color originalColor = g2.getColor();

        g2.setStroke(new BasicStroke(shape.getThickness()));
        if (!xor && shape.getOpacity() < 1.0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shape.getOpacity()));
        }

        if (xor) {
            g2.setXORMode(shape.getColor() != null ? shape.getColor() : Color.BLACK);
            g2.drawOval(x, y, width, height);
            g2.setPaintMode();
        } else {
            if (shape.isUseGradient() && shape.getStartColor() != null && shape.getEndColor() != null) {
                GradientPaint gradient = new GradientPaint(
                        x, y, shape.getStartColor(),
                        x + width, y + height, shape.getEndColor()
                );
                g2.setPaint(gradient);
                g2.fillOval(x, y, width, height);
            } else if (shape.getFill() != null) {
                g2.setColor(shape.getFill());
                g2.fillOval(x, y, width, height);
            }

            g2.setPaint(originalPaint);
            g2.setColor(shape.getColor() != null ? shape.getColor() : Color.BLACK);
            g2.drawOval(x, y, width, height);
        }

        g2.setStroke(originalStroke);
        g2.setComposite(originalComposite);
        g2.setPaint(originalPaint);
        g2.setColor(originalColor);

        super.render(g, shape, xor);
    }
}
