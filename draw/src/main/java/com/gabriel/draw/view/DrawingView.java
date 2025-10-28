package com.gabriel.draw.view;

import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class DrawingView extends JPanel {
    AppService appService;
    private BufferedImage buffer;
    private Graphics2D bufferGraphics;

    public DrawingView(AppService appService){
        this.appService  = appService;
        setBackground(Color.WHITE);
        setDoubleBuffered(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        if (buffer == null || buffer.getWidth() != width || buffer.getHeight() != height) {
            buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            bufferGraphics = buffer.createGraphics();
            bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            bufferGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            bufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }

        bufferGraphics.setColor(Color.WHITE);
        bufferGraphics.fillRect(0, 0, width, height);

        Drawing drawing = appService.getDrawing();
        List<Shape> shapes  = drawing.getShapes();
        for(Shape shape : shapes){
            shape.getRendererService().render(bufferGraphics, shape, false);
        }

        g.drawImage(buffer, 0, 0, null);
    }

    @Override
    public Graphics getGraphics() {
        if (bufferGraphics != null) {
            return bufferGraphics;
        }
        return super.getGraphics();
    }
}
