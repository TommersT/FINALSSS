package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.awt.Point;

public class SetPositionCommand implements Command {
    private AppService appService;
    private int oldX, oldY;
    private int newX, newY;
    private Shape targetShape;

    public SetPositionCommand(AppService appService, int oldX, int oldY, int newX, int newY) {
        this.appService = appService;
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
        this.targetShape = appService.getSelectedShape();
    }

    @Override
    public void execute() {
        if (targetShape != null && targetShape.isSelected()) {
            targetShape.setLocation(new Point(newX, newY));
            triggerRepaint();
        }
    }

    @Override
    public void undo() {
        if (targetShape != null) {
            targetShape.setLocation(new Point(oldX, oldY));
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
