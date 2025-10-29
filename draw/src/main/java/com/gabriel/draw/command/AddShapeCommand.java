package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;

public class AddShapeCommand implements Command {
    private AppService underlyingAppService;
    private Shape shape; // The shape to be added or removed

    /**
     * Constructor for the AddShapeCommand.
     * @param appService The potentially wrapped AppService.
     * @param shape The shape that was added.
     */
    public AddShapeCommand(AppService appService, Shape shape) {
        if (appService instanceof DrawingCommandAppService) {
            this.underlyingAppService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
        } else {
            this.underlyingAppService = appService;
        }
        this.shape = shape;
    }

    /**
     * Executes (or Redoes) the command: Adds the shape to the drawing.
     */
    @Override
    public void execute() {
        if (shape != null) {
            // create() in service should handle repaint
            underlyingAppService.create(shape);
        }
    }

    /**
     * Undoes the command: Removes the shape from the drawing.
     */
    @Override
    public void undo() {
        if (shape != null) {
            // delete() in service should handle repaint
            underlyingAppService.delete(shape);
        }
    }

    /**
     * Redoes the command: Simply executes the command again.
     */
    @Override
    public void redo() {
        execute(); // Re-add the shape
    }
    // No explicit triggerRepaint needed here if service methods handle it
}