package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;

public class DeleteShapeCommand implements Command {
    private AppService underlyingAppService;
    private Shape shape;
    private int originalIndex;
    
    public DeleteShapeCommand(AppService appService, Shape shape) {
        if (appService instanceof DrawingCommandAppService) {
            this.underlyingAppService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
        } else {
            this.underlyingAppService = appService;
        }
        this.shape = shape;
        this.originalIndex = -1;
    }
    
    @Override
    public void execute() {
        originalIndex = underlyingAppService.getDrawing().getShapes().indexOf(shape);
        underlyingAppService.delete(shape);
    }
    
    @Override
    public void undo() {
        if (originalIndex >= 0 && originalIndex <= underlyingAppService.getDrawing().getShapes().size()) {
            underlyingAppService.insertAt(shape, originalIndex);
        } else {
            underlyingAppService.create(shape);
        }
    }
    
    @Override
    public void redo() {
        underlyingAppService.delete(shape);
    }
}
