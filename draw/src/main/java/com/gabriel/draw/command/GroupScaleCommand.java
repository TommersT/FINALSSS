package com.gabriel.draw.command;

import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import java.awt.Point;

public class GroupScaleCommand implements Command {
    private Shape shape;
    private Point originalLocation;
    private int originalWidth;
    private int originalHeight;
    private Point newLocation;
    private int newWidth;
    private int newHeight;

    public GroupScaleCommand(Shape shape) {
        this.shape = shape;
        this.originalLocation = new Point(shape.getLocation());
        this.originalWidth = shape.getWidth();
        this.originalHeight = shape.getHeight();
    }

    public void captureNewState() {
        this.newLocation = new Point(shape.getLocation());
        this.newWidth = shape.getWidth();
        this.newHeight = shape.getHeight();
    }

    @Override
    public void execute() {
        shape.setLocation(new Point(newLocation));
        shape.setWidth(newWidth);
        shape.setHeight(newHeight);
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
