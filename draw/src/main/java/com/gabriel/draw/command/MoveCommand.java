package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.awt.Point;
import java.util.List;
import java.util.ArrayList;

public class MoveCommand implements Command {
    private AppService underlyingAppService;
    private List<ShapeMoveData> moveData;
    private Point start;
    private Point end;
    
    private static class ShapeMoveData {
        Shape shape;
        Point originalLocation;
        
        ShapeMoveData(Shape shape) {
            this.shape = shape;
            this.originalLocation = new Point(shape.getLocation());
        }
    }
    
    public MoveCommand(AppService appService, Point start, Point end) {
        if (appService instanceof DrawingCommandAppService) {
            this.underlyingAppService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
        } else {
            this.underlyingAppService = appService;
        }
        this.start = start;
        this.end = end;
        this.moveData = new ArrayList<>();
        
        List<Shape> selectedShapes = underlyingAppService.getSelectedShapes();
        for (Shape shape : selectedShapes) {
            moveData.add(new ShapeMoveData(shape));
        }
    }
    
    @Override
    public void execute() {
        underlyingAppService.move(start, end);
    }
    
    @Override
    public void undo() {
        for (ShapeMoveData data : moveData) {
            data.shape.setLocation(new Point(data.originalLocation));
        }
    }
    
    @Override
    public void redo() {
        int dx = end.x - start.x;
        int dy = end.y - start.y;
        for (ShapeMoveData data : moveData) {
            Point newLoc = new Point(data.originalLocation.x + dx, data.originalLocation.y + dy);
            data.shape.setLocation(newLoc);
        }
    }
}
