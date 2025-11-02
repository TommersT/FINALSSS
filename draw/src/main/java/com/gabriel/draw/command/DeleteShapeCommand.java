package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.util.List;

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
        if (this.shape != null) {
            this.originalIndex = underlyingAppService.getDrawing().getShapes().indexOf(this.shape);
        } else {
            this.originalIndex = -1;
        }
    }

    @Override
    public void execute() {
        if (shape != null && originalIndex >= 0) {
            underlyingAppService.getDrawing().getShapes().remove(shape);
            if (shape.isSelected()) {
                underlyingAppService.clearSelections();
            }
            triggerRepaint();
        }
    }

    @Override
    public void undo() {
        if (shape == null) return;

        List<Shape> shapes = underlyingAppService.getDrawing().getShapes();
        if (originalIndex >= 0 && originalIndex <= shapes.size()) {
            shapes.add(originalIndex, shape);
        } else {
            shapes.add(shape);
        }
        triggerRepaint();
    }

    @Override
    public void redo() {
        if (shape != null) {
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
}
