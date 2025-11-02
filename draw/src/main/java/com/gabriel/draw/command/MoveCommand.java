package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.awt.Point;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MoveCommand implements Command {
    private AppService underlyingAppService;
    private Map<Shape, Point> originalLocations;
    private List<Shape> movedShapes;
    private Point delta;

    public MoveCommand(AppService appService, Point start, Point end) {
        if (appService instanceof DrawingCommandAppService) {
            this.underlyingAppService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
        } else {
            this.underlyingAppService = appService;
        }

        this.originalLocations = new HashMap<>();
        this.movedShapes = new ArrayList<>(underlyingAppService.getSelectedShapes());
        this.delta = new Point(end.x - start.x, end.y - start.y);

        for (Shape shape : movedShapes) {
            originalLocations.put(shape, new Point(shape.getLocation()));
        }
    }

    @Override
    public void execute() {
        for (Shape shape : movedShapes) {
            Point original = originalLocations.get(shape);
            if (original != null) {
                Point targetLocation = new Point(original.x + delta.x, original.y + delta.y);
                shape.setLocation(targetLocation);
            }
        }
        triggerRepaint();
    }

    @Override
    public void undo() {
        for (Shape shape : movedShapes) {
            Point original = originalLocations.get(shape);
            if (original != null) {
                shape.setLocation(new Point(original));
            }
        }
        triggerRepaint();
    }

    @Override
    public void redo() {
        execute();
    }

    private void triggerRepaint() {
        if (underlyingAppService instanceof com.gabriel.draw.service.DrawingAppService) {
            ((com.gabriel.draw.service.DrawingAppService) underlyingAppService).triggerRepaint();
        } else {
            System.err.println("Warning: Could not trigger repaint from " + this.getClass().getSimpleName() + " - underlyingAppService is not the expected type.");
        }
    }
}
