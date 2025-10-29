// tommerst/finalsss/FINALSSS-4b53253892a92ed882030feb653ef76e91b6ab5b/draw/src/main/java/com/gabriel/draw/command/ScaleCommand.java
package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService; // Needed for type check
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import com.gabriel.drawfx.SelectionMode;
import java.awt.Point;
import java.awt.Dimension;

public class ScaleCommand implements Command {
    private AppService appService; // Store the service passed in (likely wrapped)
    private AppService underlyingAppService; // Store the base service for repaint trigger
    private Shape shape;

    private Point originalLocation;
    private Dimension originalSize;
    private Point finalLocation;
    private Dimension finalSize;


    public ScaleCommand(AppService appService, Shape shape, Point start, Point end) {
        this.appService = appService; // Store potentially wrapped service
        // Get and store the underlying service right away
        if (appService instanceof DrawingCommandAppService) {
            this.underlyingAppService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
        } else {
            this.underlyingAppService = appService; // Assume it's the base if not wrapper
        }
        this.shape = shape;

        // Assumes DrawingController restored the shape to its pre-drag state
        this.originalLocation = new Point(shape.getLocation());
        this.originalSize = new Dimension(shape.getWidth(), shape.getHeight());

        // Calculate final state based on start->end drag
        calculateFinalState(start, end);
    }

    // Helper method (same as before)
    private void calculateFinalState(Point start, Point end) {
        Point tempLoc = new Point(originalLocation);
        Dimension tempSize = new Dimension(originalSize);
        int dx = end.x - start.x;
        int dy = end.y - start.y;
        SelectionMode selMode = shape.getSelectionMode();

        // Apply scaling logic based on the handle (same as before)
        // ... (logic from previous version) ...
        if(selMode == SelectionMode.UpperLeft) {
            tempLoc.translate(dx, dy);
            tempSize.width -= dx;
            tempSize.height -= dy;
        } else if(selMode == SelectionMode.LowerLeft) {
            tempLoc.translate(dx, 0);
            tempSize.width -= dx;
            tempSize.height += dy;
        } else if(selMode == SelectionMode.UpperRight){
            tempLoc.translate(0, dy);
            tempSize.width += dx;
            tempSize.height -= dy;
        } else if(selMode == SelectionMode.LowerRight){
            tempSize.width += dx;
            tempSize.height += dy;
        } else if(selMode == SelectionMode.MiddleRight){
            tempSize.width += dx;
        } else if(selMode == SelectionMode.MiddleLeft){
            tempLoc.translate(dx, 0);
            tempSize.width -= dx;
        } else if(selMode == SelectionMode.MiddleTop) {
            tempLoc.translate(0, dy);
            tempSize.height -= dy;
        } else if(selMode == SelectionMode.MiddleBottom){
            tempSize.height += dy;
        }

        // Optional Normalization for final state calculation
        if (tempSize.width < 0) {
            tempLoc.x += tempSize.width;
            tempSize.width *= -1;
        }
        if (tempSize.height < 0) {
            tempLoc.y += tempSize.height;
            tempSize.height *= -1;
        }

        this.finalLocation = tempLoc;
        this.finalSize = tempSize;
    }

    @Override
    public void execute() { // Also applies for Redo
        shape.setLocation(new Point(finalLocation));
        shape.setWidth(finalSize.width);
        shape.setHeight(finalSize.height);
        triggerRepaint(); // Repaint after applying changes
    }

    @Override
    public void undo() {
        shape.setLocation(new Point(originalLocation)); // Restore original location
        shape.setWidth(originalSize.width);            // Restore original width
        shape.setHeight(originalSize.height);          // Restore original height
        triggerRepaint(); // *** ESSENTIAL: Repaint after restoring state ***
    }

    @Override
    public void redo() {
        execute(); // Re-apply the scale
    }

    // Helper to call repaint on the base service
    private void triggerRepaint() {
        // Use the stored underlyingAppService
        if (underlyingAppService instanceof com.gabriel.draw.service.DrawingAppService) {
            ((com.gabriel.draw.service.DrawingAppService)underlyingAppService).triggerRepaint();
        } else {
            System.err.println("Warning: Could not trigger repaint from " + this.getClass().getSimpleName());
        }
    }
}