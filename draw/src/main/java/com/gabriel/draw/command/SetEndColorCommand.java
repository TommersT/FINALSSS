package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class SetEndColorCommand implements Command {
    private AppService appService;
    private Color oldValue;
    private Color newValue;
    private boolean appliedToSelection;
    private List<Shape> targetShapes;

    public SetEndColorCommand(AppService appService, Color oldValue, Color newValue) {
        this.appService = appService;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.targetShapes = new ArrayList<>(appService.getSelectedShapes());
        this.appliedToSelection = !this.targetShapes.isEmpty();
    }

    @Override
    public void execute() {
        AppService baseService = getUnderlyingService();
        if (appliedToSelection) {
            for (Shape shape : targetShapes) {
                shape.setEndColor(newValue);
            }
        } else {
            baseService.getDrawing().setEndColor(newValue); // Set global default
        }
        triggerRepaint();
    }

    @Override
    public void undo() {
        AppService baseService = getUnderlyingService();
        if (appliedToSelection) {
            for (Shape shape : targetShapes) {
                shape.setEndColor(oldValue);
            }
        } else {
            baseService.getDrawing().setEndColor(oldValue);
        }
        triggerRepaint();
    }

    @Override
    public void redo() {
        execute();
    }

    private AppService getUnderlyingService() {
        if (appService instanceof DrawingCommandAppService) {
            return ((DrawingCommandAppService) appService).getUnderlyingAppService();
        }
        return appService;
    }

    private void triggerRepaint() {
        AppService baseService = getUnderlyingService();
        if (baseService instanceof com.gabriel.draw.service.DrawingAppService) {
            ((com.gabriel.draw.service.DrawingAppService) baseService).triggerRepaint();
        } else {
            System.err.println("Warning: Could not trigger repaint from " + this.getClass().getSimpleName());
        }
    }
}
