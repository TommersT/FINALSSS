package com.gabriel.draw.renderer;

import com.gabriel.draw.model.Text;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.renderer.ShapeRenderer;

import java.awt.*;

public class TextRenderer extends ShapeRenderer {

    @Override
    public void render(Graphics g, Shape shape, boolean xor) {
        Text text = (Text) shape;

        int x = shape.getLocation().x;
        int y = shape.getLocation().y;

        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(shape.getFont());
        g2.setStroke(new BasicStroke(shape.getThickness()));

        String textContent = shape.getText();
        if (textContent == null || textContent.isEmpty()) {
            textContent = "Text";
        }

        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(textContent);
        int textHeight = fm.getHeight();

        if (shape.getWidth() == 0 || shape.getHeight() == 0) {
            shape.setWidth(textWidth + 10);
            shape.setHeight(textHeight + 5);
        }

        int width = shape.getWidth();
        int height = shape.getHeight();

        if (xor) {
            g2.setXORMode(shape.getColor());
            g2.drawRect(x, y - fm.getAscent(), width, height);
        } else {
            if (shape.getFill() != null) {
                g2.setColor(shape.getFill());
                g2.fillRect(x, y - fm.getAscent(), width, height);
            }
            g2.setColor(shape.getColor());
            g2.drawString(textContent, x + 5, y);
        }
        super.render(g, shape, xor);
    }
}
