// tommerst/finalsss/FINALSSS-6bcea5b67907ccf7b6ff1ee8d4aed3787d183a76/draw/src/main/java/com/gabriel/draw/command/ScaleCommand.java
// Updated to handle Text shape scaling (stretching) correctly if needed
package com.gabriel.draw.command;

import com.gabriel.draw.model.Text; // Import Text model
import com.gabriel.draw.service.DrawingCommandAppService; // Needed for type check
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import com.gabriel.drawfx.SelectionMode;
import java.awt.*; // Keep AWT imports

public class ScaleCommand implements Command {
    private AppService appService; // Store the service passed in (likely wrapped)
    private AppService underlyingAppService; // Store the base service for repaint trigger
    private Shape shape;

    private Point originalLocation;
    private Dimension originalSize;
    // private int originalFontSize; // For font scaling approach

    private Point finalLocation;
    private Dimension finalSize;
    // private int finalFontSize; // For font scaling approach

    // Constructor captures state *before* the change (assumes controller restored it)
    public ScaleCommand(AppService appService, Shape shape, Point start, Point end) {
        this.appService = appService; // Store potentially wrapped service
        // Get and store the underlying service right away
        if (appService instanceof DrawingCommandAppService) {
            this.underlyingAppService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
        } else {
            this.underlyingAppService = appService; // Assume it's the base if not wrapper
        }
        this.shape = shape;

        // Capture state *before* the scale is applied by the command
        this.originalLocation = new Point(shape.getLocation());
        this.originalSize = new Dimension(shape.getWidth(), shape.getHeight());
        // if (shape instanceof Text) {
        //     this.originalFontSize = shape.getFont().getSize();
        // }


        // Calculate the final state based on the drag delta (start -> end)
        calculateFinalState(start, end);
    }

    // Helper method to determine the final state after scaling
    private void calculateFinalState(Point start, Point end) {
        // Start with the original state
        Point tempLoc = new Point(originalLocation);
        Dimension tempSize = new Dimension(originalSize);
        // int tempFontSize = (shape instanceof Text) ? originalFontSize : -1;

        // Calculate total delta from the drag operation
        int dx = end.x - start.x;
        int dy = end.y - start.y;
        SelectionMode selMode = shape.getSelectionMode(); // Get handle used

        // --- Standard Stretch Scaling Logic ---
        // (Same logic as in DrawingController.applyScalePreview)
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
        // --- End Scaling Logic ---

        // --- Font Size Scaling Logic (Alternative) ---
        /* if (shape instanceof Text) {
            double scaleX = (originalSize.width != 0) ? (double)tempSize.width / originalSize.width : 1.0;
            double scaleY = (originalSize.height != 0) ? (double)tempSize.height / originalSize.height : 1.0;
            double scaleFactor = (Math.abs(scaleX - 1.0) > Math.abs(scaleY - 1.0)) ? scaleX : scaleY;
            tempFontSize = Math.max(1, (int)Math.round(originalFontSize * scaleFactor));

            // Recalculate bounds based on new font size for final state (needs Graphics context - tricky here)
            // Ideally, the renderer recalculates on execute/redo. Store font size change.
             tempLoc = originalLocation; // Keep original baseline
             tempSize = originalSize; // Keep original bounds, let renderer update based on font
        } */
        // --- End Font Scaling ---


        // Optional Normalization for final state calculation (only if NOT scaling font size)
        // Normalizing here ensures the command stores positive width/height.
        // Avoid if scaling font size, as bounds depend on font.
        // if (!(shape instanceof Text)) { // Or check if font scaling is active
        if (tempSize.width < 0) {
            tempLoc.x += tempSize.width; // Adjust location
            tempSize.width *= -1;         // Make positive
        }
        if (tempSize.height < 0) {
            tempLoc.y += tempSize.height; // Adjust location
            tempSize.height *= -1;        // Make positive
        }
        // }


        // Store the calculated final state
        this.finalLocation = tempLoc;
        this.finalSize = tempSize;
        // this.finalFontSize = tempFontSize; // For font scaling
    }

    @Override
    public void execute() { // Also applies for Redo
        shape.setLocation(new Point(finalLocation));
        shape.setWidth(finalSize.width);
        shape.setHeight(finalSize.height);
        // if (shape instanceof Text && finalFontSize > 0) { // Apply font size if using that approach
        //     Font currentFont = shape.getFont();
        //     shape.setFont(new Font(currentFont.getFamily(), currentFont.getStyle(), finalFontSize));
        // }
        triggerRepaint(); // Repaint after applying changes
    }

    @Override
    public void undo() {
        shape.setLocation(new Point(originalLocation)); // Restore original location
        shape.setWidth(originalSize.width);            // Restore original width
        shape.setHeight(originalSize.height);          // Restore original height
        // if (shape instanceof Text && originalFontSize > 0) { // Restore font size
        //     Font currentFont = shape.getFont();
        //     shape.setFont(new Font(currentFont.getFamily(), currentFont.getStyle(), originalFontSize));
        // }
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