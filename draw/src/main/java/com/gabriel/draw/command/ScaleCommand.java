package com.gabriel.draw.command;

import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.awt.Point;

public class ScaleCommand implements Command {
    private AppService appService;
    private Shape shape;
    private Point originalLocation;
    private int originalWidth;
    private int originalHeight;
    private Point start;
    private Point end;
    
    public ScaleCommand(AppService appService, Shape shape, Point start, Point end) {
        this.appService = appService;
        this.shape = shape;
        this.start = start;
        this.end = end;
        this.originalLocation = new Point(shape.getLocation());
        this.originalWidth = shape.getWidth();
        this.originalHeight = shape.getHeight();
    }
    
    @Override
    public void execute() {
        appService.scale(shape, start, end);
    }
    
    @Override
    public void undo() {
        shape.setLocation(new Point(originalLocation));
        shape.setWidth(originalWidth);
        shape.setHeight(originalHeight);
    }
    
    @Override
    public void redo() {
        execute();
    }
}
