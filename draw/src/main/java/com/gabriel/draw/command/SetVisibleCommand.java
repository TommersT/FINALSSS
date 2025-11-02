package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.util.ArrayList;
import java.util.List;

public class SetVisibleCommand implements Command {
    private AppService appService;
    private boolean oldValue;
    private boolean newValue;
    private List<Shape> targetShapes;

    public SetVisibleCommand(AppService appService, boolean oldValue, boolean newValue) {
        this.appService = appService;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.targetShapes = new ArrayList<>(appService.getSelectedShapes());
    }

    @Override
    public void execute() {
        if (!targetShapes.isEmpty()) {
            for (Shape shape : targetShapes) {
                shape.setVisible(newValue);
            }
            triggerRepaint();
        }
    }

    @Override
    public void undo() {
        if (!targetShapes.isEmpty()) {
            for (Shape shape : targetShapes) {
                shape.setVisible(oldValue);
            }
            triggerRepaint();
        }
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
