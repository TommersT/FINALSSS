// tommerst/finalsss/FINALSSS-ab98287e5a251ea0f90f45adc2a537ebda4a1511/draw/src/main/java/com/gabriel/draw/command/SetWidthCommand.java
package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;

public class SetWidthCommand implements Command {
    private AppService appService;
    private int oldValue;
    private int newValue;
    private Shape targetShape; // Store the specific shape affected

    public SetWidthCommand(AppService appService, int oldValue, int newValue) {
        this.appService = appService;
        this.oldValue = oldValue;
        this.newValue = newValue;
        // Width usually applies only to the primary selected shape
        this.targetShape = appService.getSelectedShape(); // Capture at creation
    }

    @Override
    public void execute() {
        if (targetShape != null && targetShape.isSelected()) { // Optional check
            targetShape.setWidth(newValue);
            triggerRepaint();
        }
    }

    @Override
    public void undo() {
        if (targetShape != null) { // Apply even if no longer selected?
            targetShape.setWidth(oldValue);
            triggerRepaint(); // ESSENTIAL
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