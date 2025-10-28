package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.awt.Point;

public class ScaleCommand implements Command {
    private AppService underlyingAppService;
    private Shape shape;
    private Point originalLocation;
    private int originalWidth;
    private int originalHeight;
    private Point start;
    private Point end;
    
    public ScaleCommand(AppService appService, Shape shape, Point start, Point end) {
        if (appService instanceof DrawingCommandAppService) {
            this.underlyingAppService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
        } else {
            this.underlyingAppService = appService;
        }
        this.shape = shape;
        this.start = start;
        this.end = end;
        this.originalLocation = new Point(shape.getLocation());
        this.originalWidth = shape.getWidth();
        this.originalHeight = shape.getHeight();
    }
    
    @Override
    public void execute() {
        underlyingAppService.scale(shape, start, end);
    }
    
    @Override
    public void undo() {
        shape.setLocation(new Point(originalLocation));
        shape.setWidth(originalWidth);
        shape.setHeight(originalHeight);
    }
    
    @Override
    public void redo() {
        underlyingAppService.scale(shape, start, end);
    }
}
