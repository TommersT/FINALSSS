package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;

public class SetHeightCommand implements Command {
    private AppService appService;
    private int oldValue;
    private int newValue;
    private Shape targetShape;

    public SetHeightCommand(AppService appService, int oldValue, int newValue) {
        this.appService = appService;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.targetShape = appService.getSelectedShape();
    }

    @Override
    public void execute() {
        if (targetShape != null && targetShape.isSelected()) {
            targetShape.setHeight(newValue);
            triggerRepaint();
        }
    }

    @Override
    public void undo() {
        if (targetShape != null) {
            targetShape.setHeight(oldValue);
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
