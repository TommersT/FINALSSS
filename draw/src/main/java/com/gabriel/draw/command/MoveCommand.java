// tommerst/finalsss/FINALSSS-4b53253892a92ed882030feb653ef76e91b6ab5b/draw/src/main/java/com/gabriel/draw/command/MoveCommand.java
package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService; // Needed for type check
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.awt.Point;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MoveCommand implements Command {
    private AppService underlyingAppService; // Stores the base service
    private Map<Shape, Point> originalLocations;
    private List<Shape> movedShapes; // Use List, could be multiple shapes
    private Point delta;

    public MoveCommand(AppService appService, Point start, Point end) {
        // Correctly store the underlying service
        if (appService instanceof DrawingCommandAppService) {
            this.underlyingAppService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
        } else {
            this.underlyingAppService = appService; // Assume it's the base if not wrapper
        }

        this.originalLocations = new HashMap<>();
        // Capture the shapes that are currently selected *before* the command executes
        // Use the underlying service to get the current selection state accurately
        this.movedShapes = new ArrayList<>(underlyingAppService.getSelectedShapes()); // Use underlying here
        this.delta = new Point(end.x - start.x, end.y - start.y);

        // Capture original locations (state before move)
        for (Shape shape : movedShapes) {
            // Assumes controller restored state *before* creating command
            originalLocations.put(shape, new Point(shape.getLocation()));
        }
    }

    @Override
    public void execute() { // Also applies for Redo
        for (Shape shape : movedShapes) {
            Point original = originalLocations.get(shape);
            if (original != null) {
                Point targetLocation = new Point(original.x + delta.x, original.y + delta.y);
                shape.setLocation(targetLocation); // Apply the final position
            }
        }
        triggerRepaint(); // Repaint after applying changes
    }

    @Override
    public void undo() {
        for (Shape shape : movedShapes) {
            Point original = originalLocations.get(shape);
            if (original != null) {
                shape.setLocation(new Point(original)); // Restore the original position
            }
        }
        triggerRepaint(); // ESSENTIAL: Repaint after restoring state
    }

    @Override
    public void redo() {
        execute(); // Re-apply the move
    }

    // Helper to call repaint on the base service
    private void triggerRepaint() {
        // *** FIX: Use the 'underlyingAppService' field ***
        if (underlyingAppService instanceof com.gabriel.draw.service.DrawingAppService) {
            ((com.gabriel.draw.service.DrawingAppService) underlyingAppService).triggerRepaint();
            // *** Removed redundant checks for 'appService' variable which doesn't exist here ***
        } else {
            System.err.println("Warning: Could not trigger repaint from " + this.getClass().getSimpleName() + " - underlyingAppService is not the expected type.");
        }
    }
}