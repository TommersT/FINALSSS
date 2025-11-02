package com.gabriel.draw.renderer;

import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.renderer.ShapeRenderer;

import java.awt.*;

public class LineRenderer extends ShapeRenderer {

    @Override
    public void render(Graphics g, Shape shape, boolean xor) {

        if (!shape.isVisible()) {
            return;
        }

        if (shape.getLocation() == null) return;

        int x1 = shape.getLocation().x;
        int y1 = shape.getLocation().y;
        int x2 = x1 + shape.getWidth();
        int y2 = y1 + shape.getHeight();

        Graphics2D g2 = (Graphics2D) g;

        Stroke originalStroke = g2.getStroke();
        Composite originalComposite = g2.getComposite();
        Paint originalPaint = g2.getPaint();
        Color originalColor = g2.getColor();

        g2.setStroke(new BasicStroke(shape.getThickness(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (!xor && shape.getOpacity() < 1.0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shape.getOpacity()));
        }

        if (xor) {
            g2.setXORMode(shape.getColor() != null ? shape.getColor() : Color.BLACK);
            g2.drawLine(x1, y1, x2, y2);
            g2.setPaintMode();
        } else {
            if (shape.isUseGradient() && shape.getStartColor() != null && shape.getEndColor() != null) {
                GradientPaint gradient = new GradientPaint(
                        x1, y1, shape.getStartColor(),
                        x2, y2, shape.getEndColor()
                );
                g2.setPaint(gradient);
            } else {
                g2.setColor(shape.getColor() != null ? shape.getColor() : Color.BLACK);
            }

            g2.drawLine(x1, y1, x2, y2);
        }

        g2.setStroke(originalStroke);
        g2.setComposite(originalComposite);
        g2.setPaint(originalPaint);
        g2.setColor(originalColor);

        super.render(g, shape, xor);
    }
}
