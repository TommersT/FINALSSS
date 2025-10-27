package com.gabriel.draw.command;

import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;

public class DeleteShapeCommand implements Command {
    private AppService appService;
    private Shape shape;
    private int originalIndex;
    
    public DeleteShapeCommand(AppService appService, Shape shape) {
        this.appService = appService;
        this.shape = shape;
        this.originalIndex = -1;
    }
    
    @Override
    public void execute() {
        originalIndex = appService.getDrawing().getShapes().indexOf(shape);
        appService.delete(shape);
    }
    
    @Override
    public void undo() {
        if (originalIndex >= 0 && originalIndex <= appService.getDrawing().getShapes().size()) {
            appService.insertAt(shape, originalIndex);
        } else {
            appService.create(shape);
        }
    }
    
    @Override
    public void redo() {
        appService.delete(shape);
    }
}
