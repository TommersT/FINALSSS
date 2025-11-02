package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

public class SetFontStyleCommand implements Command {
    private AppService appService;
    private int oldValue;
    private int newValue;
    private Font oldFontContext;
    private boolean appliedToSelection;
    private List<Shape> targetShapes;

    public SetFontStyleCommand(AppService appService, int oldValue, int newValue, Font oldFontContext) {
        this.appService = appService;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.oldFontContext = oldFontContext;
        this.targetShapes = new ArrayList<>(appService.getSelectedShapes());
        this.appliedToSelection = !this.targetShapes.isEmpty();
    }

    @Override
    public void execute() {
        AppService baseService = getUnderlyingService();
        if (appliedToSelection) {
            for (Shape shape : targetShapes) {
                Font currentShapeFont = shape.getFont() != null ? shape.getFont() : oldFontContext;
                shape.setFont(new Font(currentShapeFont.getFamily(), newValue, currentShapeFont.getSize()));
            }
        } else {
            baseService.getDrawing().setFont(new Font(oldFontContext.getFamily(), newValue, oldFontContext.getSize()));
        }
        triggerRepaint();
    }

    @Override
    public void undo() {
        AppService baseService = getUnderlyingService();
        if (appliedToSelection) {
            for (Shape shape : targetShapes) {
                Font currentShapeFont = shape.getFont();
                if (currentShapeFont != null) {
                    shape.setFont(new Font(currentShapeFont.getFamily(), oldValue, currentShapeFont.getSize()));
                }
            }
        } else {
            baseService.getDrawing().setFont(oldFontContext);
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
