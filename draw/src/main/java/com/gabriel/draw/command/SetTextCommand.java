package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.util.ArrayList;
import java.util.List;

public class SetTextCommand implements Command {
    private AppService appService;
    private String oldValue;
    private String newValue;
    private boolean appliedToSelection;
    private List<Shape> targetShapes;

    public SetTextCommand(AppService appService, String oldValue, String newValue) {
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
                // Should ideally only apply to Text shapes
                // if (shape instanceof com.gabriel.draw.model.Text)
                shape.setText(newValue);
            }
        } else {
            baseService.getDrawing().setText(newValue); // Set global default
        }
        triggerRepaint();
    }

    @Override
    public void undo() {
        AppService baseService = getUnderlyingService();
        if (appliedToSelection) {
            for (Shape shape : targetShapes) {
                // if (shape instanceof com.gabriel.draw.model.Text)
                shape.setText(oldValue);
            }
        } else {
            baseService.getDrawing().setText(oldValue);
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