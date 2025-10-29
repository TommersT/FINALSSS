package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.util.List; // Import List

public class DeleteShapeCommand implements Command {
    private AppService underlyingAppService;
    private Shape shape;
    private int originalIndex; // Store index for proper restoration

    public DeleteShapeCommand(AppService appService, Shape shape) {
        if (appService instanceof DrawingCommandAppService) {
            this.underlyingAppService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
        } else {
            this.underlyingAppService = appService;
        }
        this.shape = shape;
        // Capture index *before* deletion occurs
        // Ensure shape is not null before getting index
        if (this.shape != null) {
            this.originalIndex = underlyingAppService.getDrawing().getShapes().indexOf(this.shape);
        } else {
            this.originalIndex = -1; // Indicate invalid state if shape is null
        }
    }

    @Override
    public void execute() {
        // Only delete if shape is valid and was found (index >= 0)
        if (shape != null && originalIndex >= 0) {
            // Deletion inherently triggers repaint via the service method
            underlyingAppService.delete(shape);
        } else if (shape != null) {
            // If shape exists but wasn't found initially, try deleting anyway?
            // Maybe it was added after command creation but before execution? Unlikely.
            // Safer to just rely on index check. If index was -1, maybe log warning.
            System.err.println("Warning: DeleteShapeCommand execute called for shape not found initially.");
            underlyingAppService.delete(shape); // Try deleting anyway
        }
    }

    @Override
    public void undo() {
        if (shape == null) return; // Cannot undo if shape is null

        List<Shape> shapes = underlyingAppService.getDrawing().getShapes();
        // Restore at original index if possible and valid
        if (originalIndex >= 0 && originalIndex <= shapes.size()) {
            // insertAt should trigger repaint
            underlyingAppService.insertAt(shape, originalIndex);
        } else {
            // Fallback: add to end if index was invalid (shouldn't normally happen)
            System.err.println("Warning: DeleteShapeCommand undo called with invalid original index. Appending shape.");
            // create should trigger repaint
            underlyingAppService.create(shape);
        }
        // No need for explicit triggerRepaint() here if service methods handle it
    }

    @Override
    public void redo() {
        // Just delete the shape again. The service handles finding it.
        if (shape != null) {
            // Deletion inherently triggers repaint via the service method
            underlyingAppService.delete(shape);
        }
    }
}