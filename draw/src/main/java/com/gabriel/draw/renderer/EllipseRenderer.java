package com.gabriel.draw.renderer;

import com.gabriel.draw.model.Ellipse;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.renderer.ShapeRenderer;

import java.awt.*;

public class EllipseRenderer extends ShapeRenderer {

    @Override
    public void render(Graphics g,  Shape shape, boolean xor) {
        Ellipse ellipse = (Ellipse) shape;

        int x = shape.getLocation().x;
        int y = shape.getLocation().y;
        int width = shape.getWidth() ;
        int height = shape.getHeight();

        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(shape.getThickness()));

        Composite oldComposite = g2.getComposite();
        if (!xor && shape.getOpacity() < 1.0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shape.getOpacity()));
        }

        if (xor) {
            g2.setXORMode(shape.getColor());
        } else {
            g2.setColor(shape.getColor());
            if(shape.getFill() != null){
                g2.setColor(shape.getFill());
                g2.fillOval(x,y,width, height);
                g2.setColor(shape.getColor());
            }
        }
        g2.drawOval(x, y, width, height);
        
        if (!xor) {
            g2.setComposite(oldComposite);
        }
        
        super.render(g, shape, xor);
    }
}