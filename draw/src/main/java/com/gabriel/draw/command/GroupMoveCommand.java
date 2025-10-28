package com.gabriel.draw.command;

import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.awt.Point;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GroupMoveCommand implements Command {
    private List<Shape> shapes;
    private Map<Shape, Point> originalLocations;
    private Point delta;

    public GroupMoveCommand(List<Shape> shapes, Point start, Point end) {
        this.shapes = new ArrayList<>(shapes);
        this.originalLocations = new HashMap<>();
        this.delta = new Point(end.x - start.x, end.y - start.y);

        for (Shape shape : shapes) {
            originalLocations.put(shape, new Point(shape.getLocation()));
        }
    }

    @Override
    public void execute() {
        for (Shape shape : shapes) {
            Point original = originalLocations.get(shape);
            shape.getLocation().x = original.x + delta.x;
            shape.getLocation().y = original.y + delta.y;
        }
    }

    @Override
    public void undo() {
        for (Shape shape : shapes) {
            Point original = originalLocations.get(shape);
            shape.setLocation(new Point(original));
        }
    }

    @Override
    public void redo() {
        execute();
    }
}
