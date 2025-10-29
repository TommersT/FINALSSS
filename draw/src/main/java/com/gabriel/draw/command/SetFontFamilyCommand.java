package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

public class SetFontFamilyCommand implements Command {
    private AppService appService;
    private Font oldFontContext; // Store the complete font context (size, style)
    private String oldFamily;    // Specific property changed
    private String newFamily;
    private boolean appliedToSelection;
    private List<Shape> targetShapes;

    public SetFontFamilyCommand(AppService appService, Font oldFontContext, String newFamily) {
        this.appService = appService;
        this.oldFontContext = oldFontContext;
        this.oldFamily = oldFontContext.getFamily(); // Extract old value
        this.newFamily = newFamily;
        this.targetShapes = new ArrayList<>(appService.getSelectedShapes());
        this.appliedToSelection = !this.targetShapes.isEmpty();
    }

    @Override
    public void execute() {
        AppService baseService = getUnderlyingService();
        if (appliedToSelection) {
            for (Shape shape : targetShapes) {
                Font currentShapeFont = shape.getFont() != null ? shape.getFont() : oldFontContext; // Use context if shape has no font
                shape.setFont(new Font(newFamily, currentShapeFont.getStyle(), currentShapeFont.getSize()));
            }
        } else {
            baseService.getDrawing().setFont(new Font(newFamily, oldFontContext.getStyle(), oldFontContext.getSize()));
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
                    shape.setFont(new Font(oldFamily, currentShapeFont.getStyle(), currentShapeFont.getSize()));
                }
            }
        } else {
            // Restore global font using the original context
            baseService.getDrawing().setFont(oldFontContext);
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