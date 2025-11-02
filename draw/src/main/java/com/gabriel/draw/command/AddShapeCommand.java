package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;

public class AddShapeCommand implements Command {
    private AppService underlyingAppService;
    private Shape shape;

    public AddShapeCommand(AppService appService, Shape shape) {
        if (appService instanceof DrawingCommandAppService) {
            this.underlyingAppService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
        } else {
            this.underlyingAppService = appService;
        }
        this.shape = shape;
    }

    @Override
    public void execute() {
        if (shape != null && underlyingAppService != null) {
            underlyingAppService.getDrawing().getShapes().add(shape);
            triggerRepaint();
        }
    }

    @Override
    public void undo() {
        if (shape != null && underlyingAppService != null) {
            underlyingAppService.getDrawing().getShapes().remove(shape);
            if (shape.isSelected()) {
                underlyingAppService.clearSelections();
            }
            triggerRepaint();
        }
    }

    private void triggerRepaint() {
        if (underlyingAppService instanceof com.gabriel.draw.service.DrawingAppService) {
            ((com.gabriel.draw.service.DrawingAppService) underlyingAppService).triggerRepaint();
        }
    }


    @Override
    public void redo() {
        execute();
    }
}