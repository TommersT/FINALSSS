package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.util.ArrayList;
import java.util.List;

public class SetUseGradientCommand implements Command {
    private AppService appService;
    private boolean oldValue;
    private boolean newValue;
    private boolean appliedToSelection;
    private List<Shape> targetShapes;

    public SetUseGradientCommand(AppService appService, boolean oldValue, boolean newValue) {
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
                shape.setUseGradient(newValue);
            }
        } else {
            baseService.getDrawing().setUseGradient(newValue); // Set global default
        }
        triggerRepaint();
    }

    @Override
    public void undo() {
        AppService baseService = getUnderlyingService();
        if (appliedToSelection) {
            for (Shape shape : targetShapes) {
                shape.setUseGradient(oldValue);
            }
        } else {
            baseService.getDrawing().setUseGradient(oldValue);
        }
        triggerRepaint(); // ESSENTIAL
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
