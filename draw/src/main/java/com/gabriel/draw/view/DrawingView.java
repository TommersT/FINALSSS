package com.gabriel.draw.view;

import com.gabriel.draw.controller.DrawingController;
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
    private DrawingController drawingController;

    public DrawingView(AppService appService) {
        this.appService = appService;
        setBackground(Color.WHITE);
        setDoubleBuffered(true);
        setFocusable(true);
    }

    public void setDrawingController(DrawingController controller) {
        this.drawingController = controller;
        addMouseListener(controller);
        addMouseMotionListener(controller);
        addKeyListener(controller);
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
            if (bufferGraphics != null) {
                bufferGraphics.dispose();
            }
            bufferGraphics = buffer.createGraphics();
            bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            bufferGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            bufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            bufferGraphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }

        bufferGraphics.setColor(getBackground());
        bufferGraphics.fillRect(0, 0, width, height);

        Drawing drawing = appService.getDrawing();
        if (drawing != null) {
            List<Shape> shapes = drawing.getShapes();
            if (shapes != null) {
                for (int i = 0; i < shapes.size(); i++) {
                    Shape shape = shapes.get(i);
                    if (shape != null && shape.getRendererService() != null) {
                        try {
                            shape.getRendererService().render(bufferGraphics, shape, false);
                        } catch (Exception e) {
                            System.err.println("Error rendering shape " + i + ": " + e.getMessage());
                        }
                    }
                }
            }
        }

        if (drawingController != null) {
            Shape preview = drawingController.getPreviewShape();
            if (preview != null && preview.getRendererService() != null) {
                try {
                    preview.getRendererService().render(bufferGraphics, preview, false);
                } catch (Exception e) {
                    System.err.println("Error rendering preview shape: " + e.getMessage());
                }
            }
        }

        g.drawImage(buffer, 0, 0, null);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (bufferGraphics != null) {
            bufferGraphics.dispose();
            bufferGraphics = null;
        }
        if (buffer != null) {
            buffer.flush();
            buffer = null;
        }
    }
}
