package com.gabriel.draw.view;

import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.util.List;
import javax.swing.*;
import java.awt.*;

public class DrawingView extends JPanel {
    AppService appService;

    public DrawingView(AppService appService){
        this.appService  = appService;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Drawing drawing = appService.getDrawing();
        List<Shape> shapes  = drawing.getShapes();
        for(Shape shape : shapes){
            shape.getRendererService().render(g, shape, false);
        }
    }
}
