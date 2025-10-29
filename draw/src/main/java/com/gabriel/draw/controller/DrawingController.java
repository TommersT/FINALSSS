// tommerst/finalsss/FINALSSS-4b53253892a92ed882030feb653ef76e91b6ab5b/draw/src/main/java/com/gabriel/draw/controller/DrawingController.java
package com.gabriel.draw.controller;

import com.gabriel.draw.component.PropertySheet;
import com.gabriel.draw.model.*;
import com.gabriel.draw.model.Rectangle; // Keep specific shape imports if needed
import com.gabriel.draw.view.DrawingStatusPanel;
import com.gabriel.draw.view.TextInputDialog;
import com.gabriel.drawfx.DrawMode;
import com.gabriel.drawfx.ToolMode;
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.util.Normalizer;
import com.gabriel.drawfx.SelectionMode;
import com.gabriel.drawfx.ShapeMode;
import com.gabriel.draw.view.DrawingView;
import com.gabriel.drawfx.service.AppService;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.draw.service.DrawingCommandAppService; // Import the wrapper
import lombok.Setter; // Assuming Lombok is used, otherwise remove/replace setters


import javax.swing.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.HashMap;

public class DrawingController implements MouseListener, MouseMotionListener, KeyListener {
    // Start point of the current drag operation OR the initial click point
    private Point start;
    // End point of the current drag operation
    private Point end;
    // --- State specifically for Move/Scale Undo ---
    // Point where the mouse was initially pressed to start a move/scale drag
    private Point dragStartPoint = null;
    // Flag indicating if a move/scale drag is in progress
    private boolean isDraggingForMoveOrScale = false;
    // Stores the original location of shapes *before* a move/scale drag starts
    private Map<Shape, Point> originalLocations;
    // Stores the original dimensions of shapes *before* a scale drag starts
    private Map<Shape, Dimension> originalSizes;
    // ---------------------------------------------

    private final AppService appService; // Use the command-wrapped service for triggering commands
    private final Drawing drawing; // Direct reference to the model (via service)

    @Setter // Lombok setter
    private DrawingView drawingView;

    @Setter // Lombok setter
    private DrawingStatusPanel drawingStatusPanel;

    @Setter // Lombok setter
    private PropertySheet propertySheet;

    // The shape being actively drawn (preview)
    private Shape currentShape = null;

    // Added getter for the preview shape (now called currentShape)
    public Shape getPreviewShape() {
        // Only return if actively drawing
        if (appService.getToolMode() == ToolMode.DRAW && appService.getDrawMode() == DrawMode.MousePressed) {
            return currentShape;
        }
        return null; // Return null if not actively drawing a shape
    }

    public DrawingController(AppService appService, DrawingView drawingView) {
        this.appService = appService;
        // Get the drawing model reference from the service
        this.drawing = appService.getDrawing();
        // Keep the view reference, will be set via setter
        this.drawingView = drawingView;

        // Initialize maps (safer than doing it only in mousePressed)
        originalLocations = new HashMap<>();
        originalSizes = new HashMap<>();
    }

    // --- Setters (if not using Lombok) ---
    public void setDrawingView(DrawingView drawingView) {
        this.drawingView = drawingView;
        // Add listeners ONLY when the view is set and valid
        if (this.drawingView != null) {
            this.drawingView.addMouseListener(this);
            this.drawingView.addMouseMotionListener(this);
            this.drawingView.addKeyListener(this); // Add key listener here
        }
    }

    public void setDrawingStatusPanel(DrawingStatusPanel drawingStatusPanel) {
        this.drawingStatusPanel = drawingStatusPanel;
    }

    public void setPropertySheet(PropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }
    // --- End Setters ---

    @Override
    public void mouseClicked(MouseEvent e) {
        if (drawingView == null) return;

        if (e.getClickCount() == 2 && appService.getToolMode() == ToolMode.SELECT) {
            Shape selectedShape = appService.getSelectedShape();
            if (selectedShape instanceof Text) {
                // Store old values for potential command creation
                String oldText = selectedShape.getText();
                Font oldFont = selectedShape.getFont();

                JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(drawingView);
                TextInputDialog dialog = TextInputDialog.showDialog(parentFrame, oldText, oldFont);

                if (dialog.isOkClicked()) {
                    String newText = dialog.getText();
                    Font newFont = dialog.getSelectedFont();

                    boolean textChanged = newText != null && !newText.equals(oldText);
                    boolean fontChanged = newFont != null && !newFont.equals(oldFont);

                    // --- Use Commands for Undo/Redo ---
                    // The command-wrapped service methods should handle command creation if called.
                    // If PropertyEventListener handles changes triggered by the service setters, this is fine.
                    // If not, explicit commands might be needed here.

                    if (textChanged) {
                        appService.setText(newText); // Assumes this triggers necessary commands via listener or wrapper
                    }
                    if (fontChanged) {
                        appService.setFontFamily(newFont.getFamily());
                        appService.setFontStyle(newFont.getStyle());
                        appService.setFontSize(newFont.getSize());
                    }

                    if (textChanged || fontChanged) {
                        // Let renderer recalculate bounds
                        selectedShape.setWidth(0);
                        selectedShape.setHeight(0);

                        // Repaint and update property sheet are essential AFTER changes
                        drawingView.repaint();
                        if (propertySheet != null) {
                            propertySheet.populateTable(appService);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (drawingView == null) return;
        drawingView.requestFocusInWindow(); // Ensure keyboard events are captured

        // Only proceed if not already in a drag operation
        if (appService.getDrawMode() == DrawMode.Idle) {
            start = e.getPoint(); // Record the starting point for this potential action
            // FIX: Set DrawMode using the wrapped service to potentially create a command if needed
            appService.setDrawMode(DrawMode.MousePressed); // Set state indicating interaction started

            // Clear previous drag state tracking
            dragStartPoint = null;
            isDraggingForMoveOrScale = false;
            originalLocations.clear();
            originalSizes.clear();

            ToolMode currentToolMode = appService.getToolMode();

            // --- Handling Selection, Move, Scale Initiation ---
            if (currentToolMode == ToolMode.SELECT || currentToolMode == ToolMode.MOVE || currentToolMode == ToolMode.SCALE) {
                // Perform hit detection (SearchService determines selectionMode if handle is hit)
                appService.search(start, !e.isControlDown()); // Handle single/multi-select

                List<Shape> selectedShapes = appService.getSelectedShapes();
                if (!selectedShapes.isEmpty()) {
                    // *** CRITICAL: Store original state *before* any dragging occurs ***
                    dragStartPoint = new Point(start); // Record the absolute start for undo/redo delta
                    isDraggingForMoveOrScale = true; // Assume a drag might start
                    for (Shape shape : selectedShapes) {
                        originalLocations.put(shape, new Point(shape.getLocation()));
                        originalSizes.put(shape, new Dimension(shape.getWidth(), shape.getHeight()));
                    }
                }
                // Update UI based on new selection state
                updateStatusBarShape();
                if (propertySheet != null) propertySheet.populateTable(appService);
                drawingView.repaint(); // Repaint to show selection handles/changes

            }
            // --- Handling Shape Creation Initiation ---
            else if (currentToolMode == ToolMode.DRAW) {
                // Always clear selection when starting a new shape
                appService.clearSelections();
                updateStatusBarShape();
                if (propertySheet != null) propertySheet.populateTable(appService); // Show global props

                ShapeMode shapeToDraw = appService.getShapeMode();
                currentShape = null; // Reset preview shape

                // Create the appropriate shape instance (preview)
                switch (shapeToDraw) {
                    case Line:      currentShape = new Line(start); break;
                    case Rectangle: currentShape = new Rectangle(start); break;
                    case Ellipse:   currentShape = new Ellipse(start); break;
                    case Text:
                        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(drawingView);
                        // Use current service defaults for the dialog
                        TextInputDialog dialog = TextInputDialog.showDialog(parentFrame, appService.getText(), appService.getFont());
                        if (dialog.isOkClicked()) {
                            String textContent = dialog.getText();
                            if (textContent != null && !textContent.trim().isEmpty()) {
                                currentShape = new Text(start);
                                currentShape.setText(textContent);
                                currentShape.setFont(dialog.getSelectedFont());
                            } else { // No text entered
                                appService.setDrawMode(DrawMode.Idle); // Abort draw operation
                                return;
                            }
                        } else { // Dialog cancelled
                            appService.setDrawMode(DrawMode.Idle); // Abort draw operation
                            return;
                        }
                        break;
                    case Image:
                        String imageFilename = appService.getImageFileename();
                        if (imageFilename == null || imageFilename.isEmpty()) {
                            // Prompt user only if filename is not already set
                            appService.setImageFileename();
                            imageFilename = appService.getImageFileename(); // Get potentially updated filename
                            if (imageFilename == null || imageFilename.isEmpty()) {
                                appService.setDrawMode(DrawMode.Idle); // Abort if user cancelled file selection
                                return;
                            }
                        }
                        currentShape = new Picture(start, start, imageFilename);
                        break;
                    default: // Handle Select case specifically or others
                        // If ShapeMode is Select but ToolMode is DRAW, something is wrong. Reset.
                        if (shapeToDraw == ShapeMode.Select) {
                            System.err.println("Warning: ToolMode is DRAW but ShapeMode is Select. Resetting state.");
                            appService.setDrawMode(DrawMode.Idle);
                            return;
                        }
                        // For any other unexpected ShapeMode, abort.
                        appService.setDrawMode(DrawMode.Idle);
                        return;
                }

                // Apply current global properties (color, fill, thickness) to the new shape preview
                if (currentShape != null) {
                    currentShape.setColor(appService.getColor());
                    currentShape.setThickness(appService.getThickness());
                    currentShape.setFill(appService.getFill());
                    // Font/Text are handled specifically for Text shapes
                } else {
                    // If shape creation failed (e.g., text cancelled), reset state
                    appService.setDrawMode(DrawMode.Idle);
                }
            } else {
                // Should not happen if ToolModes are handled correctly
                appService.setDrawMode(DrawMode.Idle);
            }
        }
    }


    @Override
    public void mouseDragged(MouseEvent e) {
        // Only process drag if mouse button was pressed
        if (appService.getDrawMode() == DrawMode.MousePressed) {
            end = e.getPoint(); // Current drag position
            ToolMode currentToolMode = appService.getToolMode();
            Shape primarySelectedShape = appService.getSelectedShape();

            java.awt.Rectangle repaintBounds = null; // Combined area needing repaint
            int margin = 20; // Margin for repainting handles/stroke


            // --- Live Preview for Shape Creation ---
            if (currentToolMode == ToolMode.DRAW && currentShape != null) {
                // Calculate old bounds before updating
                Point locOld = currentShape.getLocation();
                int wOld = currentShape.getWidth();
                int hOld = currentShape.getHeight();
                // Use start point and current location/size for bounds
                repaintBounds = new java.awt.Rectangle(
                        Math.min(start.x, locOld.x) - margin,
                        Math.min(start.y, locOld.y) - margin,
                        Math.abs(wOld) + 2 * margin, // Use absolute value for size
                        Math.abs(hOld) + 2 * margin);


                // Update the preview shape's size based on the drag
                // Use a direct method for preview, NOT the command-wrapped one
                // Assuming `appService` holds the wrapped service, get the underlying one
                AppService baseService;
                if (appService instanceof DrawingCommandAppService) {
                    baseService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
                } else {
                    baseService = appService; // Fallback, but might cause issues if it's the wrapper
                }
                baseService.scale(currentShape, end); // Use base service's scale for preview

                // Calculate new bounds after updating
                Point locNew = currentShape.getLocation();
                int wNew = currentShape.getWidth();
                int hNew = currentShape.getHeight();
                java.awt.Rectangle newBounds = new java.awt.Rectangle(
                        Math.min(start.x, locNew.x) - margin,
                        Math.min(start.y, locNew.y) - margin,
                        Math.abs(wNew) + 2 * margin,
                        Math.abs(hNew) + 2 * margin);
                repaintBounds.add(newBounds); // Combine old and new repaint areas

            }
            // --- Live Preview for Move/Scale ---
            else if (isDraggingForMoveOrScale) // Check flag set in mousePressed
            {
                boolean isScale = (currentToolMode == ToolMode.SCALE) ||
                        (currentToolMode == ToolMode.SELECT && primarySelectedShape != null && primarySelectedShape.getSelectionMode() != SelectionMode.None);
                boolean isMove = (currentToolMode == ToolMode.MOVE) ||
                        (currentToolMode == ToolMode.SELECT && !isScale);

                int dx = end.x - start.x; // Delta from *last* drag point
                int dy = end.y - start.y;

                List<Shape> shapesToUpdate = appService.getSelectedShapes(); // Get all selected shapes

                // Get base service for direct manipulation during preview
                AppService baseService;
                if (appService instanceof DrawingCommandAppService) {
                    baseService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
                } else {
                    baseService = appService;
                }


                for (Shape shape : shapesToUpdate) {
                    Point loc = shape.getLocation();
                    // Calculate old bounds for this shape
                    java.awt.Rectangle oldShapeBounds = new java.awt.Rectangle(
                            loc.x - margin, loc.y - margin,
                            shape.getWidth() + 2 * margin, shape.getHeight() + 2 * margin);
                    if (repaintBounds == null) repaintBounds = oldShapeBounds;
                    else repaintBounds.add(oldShapeBounds);

                    // --- Apply transformation for LIVE PREVIEW ---
                    if (isMove) {
                        // Use base service's move method for preview
                        baseService.move(shape, start, end);

                    } else if (isScale && shape == primarySelectedShape) { // Only scale primary interactively
                        Point scaleEnd = end;
                        if (e.isShiftDown()) { // Apply aspect ratio constraint for preview
                            Dimension originalSize = originalSizes.get(shape);
                            if (originalSize != null) {
                                scaleEnd = maintainAspectRatio(shape, start, end);
                            }
                        }
                        // Use direct preview logic or a *non-command* scale method on baseService
                        applyScalePreview(shape, start, scaleEnd); // Use helper for direct manipulation
                    }
                    // --- End Apply transformation ---

                    // Calculate new bounds for this shape after transformation
                    Point newLoc = shape.getLocation();
                    java.awt.Rectangle newShapeBounds = new java.awt.Rectangle(
                            newLoc.x - margin, newLoc.y - margin,
                            shape.getWidth() + 2 * margin, shape.getHeight() + 2 * margin);
                    repaintBounds.add(newShapeBounds);
                }

                // Update property sheet live if needed (can cause lag, optional)
                if (propertySheet != null) {
                    propertySheet.populateTable(appService);
                }

                start = new Point(end); // *** Update start for the next drag increment ***
            }


            // Perform repaint using calculated bounds
            if (drawingView != null && repaintBounds != null) {
                // Ensure bounds have positive dimensions before repainting
                repaintBounds.width = Math.max(1, repaintBounds.width); // At least 1 pixel
                repaintBounds.height = Math.max(1, repaintBounds.height);
                drawingView.repaint(repaintBounds);
            }
        }
    }


    @Override
    public void mouseReleased(MouseEvent e) {
        if (appService.getDrawMode() == DrawMode.MousePressed) {
            end = e.getPoint(); // Final position
            ToolMode currentToolMode = appService.getToolMode();
            Shape primarySelectedShape = appService.getSelectedShape();

            // --- Finalize Move or Scale ---
            if (isDraggingForMoveOrScale && dragStartPoint != null && !originalLocations.isEmpty()) {

                // *** CRITICAL: Restore original state BEFORE creating the command ***
                // This ensures the command captures the full start->end transformation
                AppService baseService; // Get base service for direct state restoration
                if (appService instanceof DrawingCommandAppService) {
                    baseService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
                } else {
                    baseService = appService;
                }
                for (Shape shape : appService.getSelectedShapes()) { // Iterate selected shapes CURRENTLY (might have changed?) - Safer to use originalLocations.keySet()
                    Point originalLoc = originalLocations.get(shape);
                    Dimension originalSize = originalSizes.get(shape);
                    if (originalLoc != null) {
                        // Use base service setters if they exist, otherwise direct set
                        shape.setLocation(new Point(originalLoc)); // Restore location
                        // baseService.setLocation(shape, new Point(originalLoc));
                    }
                    if (originalSize != null) {
                        shape.setWidth(originalSize.width);   // Restore size
                        shape.setHeight(originalSize.height);
                        // baseService.setSize(shape, originalSize.width, originalSize.height);
                    }
                }


                // Determine if it was a move or scale based on tool mode and handle interaction
                boolean wasScale = (currentToolMode == ToolMode.SCALE) ||
                        (currentToolMode == ToolMode.SELECT && primarySelectedShape != null && primarySelectedShape.getSelectionMode() != SelectionMode.None);
                boolean wasMove = (currentToolMode == ToolMode.MOVE) ||
                        (currentToolMode == ToolMode.SELECT && !wasScale);

                // Check if the mouse actually moved significantly (optional threshold)
                boolean mouseMoved = !end.equals(dragStartPoint);

                if (mouseMoved) { // Only create command if there was a change
                    if (wasMove) {
                        // Use the command-wrapped service: appService.move(start, end)
                        // Pass the ABSOLUTE start and end points of the drag
                        appService.move(dragStartPoint, end);
                    } else if (wasScale && primarySelectedShape != null) {
                        Point scaleEnd = end;
                        if (e.isShiftDown()) { // Check shift state at release
                            Dimension originalSize = originalSizes.get(primarySelectedShape);
                            if (originalSize != null) {
                                // Use dragStartPoint as anchor for final aspect ratio calc
                                scaleEnd = maintainAspectRatio(primarySelectedShape, dragStartPoint, end);
                            }
                        }
                        // Use the command-wrapped service: appService.scale(shape, start, end)
                        appService.scale(primarySelectedShape, dragStartPoint, scaleEnd);
                    }
                }
            }
            // --- Finalize Shape Creation ---
            else if (currentToolMode == ToolMode.DRAW && currentShape != null) {
                // Apply final size/position based on release point
                // Use a base service call temporarily to set final state before command
                AppService baseService;
                if (appService instanceof DrawingCommandAppService) {
                    baseService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
                } else {
                    baseService = appService;
                }
                baseService.scale(currentShape, end); // Set final size based on end point
                Normalizer.normalize(currentShape); // Ensure width/height positive

                // Create command only if the shape has a valid size
                if (currentShape.getWidth() > 1 || currentShape.getHeight() > 1) { // Threshold > 0
                    // Use command-wrapped service to make creation undoable
                    appService.create(currentShape);
                }
                currentShape = null; // Clear the preview shape
            }

            // --- Reset State ---
            // FIX: Use wrapped service to set mode potentially creating command
            appService.setDrawMode(DrawMode.Idle);
            isDraggingForMoveOrScale = false;
            dragStartPoint = null;
            originalLocations.clear();
            originalSizes.clear();
            currentShape = null; // Ensure preview shape is cleared


            // --- Final UI Updates ---
            // Update property sheet AFTER command potentially modified state
            if (propertySheet != null) {
                propertySheet.populateTable(appService);
            }
            // Final repaint covering the whole view potentially, or just affected area
            if (drawingView != null) {
                drawingView.repaint();
            }
        }
    }


    // --- Helper for applying scale PREVIEW logic ---
    // This duplicates logic from ScalerService but applies it directly for the drag preview
    private void applyScalePreview(Shape shape, Point previewStart, Point previewEnd) {
        int dx = previewEnd.x - previewStart.x; // Delta for this segment
        int dy = previewEnd.y - previewStart.y; // Delta for this segment

        // We need the state *before* this drag segment to apply the delta correctly.
        // However, the simpler approach is to calculate based on the absolute drag start and end.
        Point currentLoc = shape.getLocation();
        Dimension currentSize = new Dimension(shape.getWidth(), shape.getHeight());


        // Get the state from the beginning of the whole drag operation
        Dimension originalSize = originalSizes.get(shape);
        Point originalLoc = originalLocations.get(shape);
        if (originalSize == null || originalLoc == null || dragStartPoint == null) return; // Should not happen

        // Calculate the total delta from the absolute drag start to the current end point
        int totalDx = previewEnd.x - dragStartPoint.x;
        int totalDy = previewEnd.y - dragStartPoint.y;


        SelectionMode selMode = shape.getSelectionMode(); // Use the mode determined on press

        // Apply scaling based on handle and TOTAL delta from dragStartPoint, starting from original state
        Point newLoc = new Point(originalLoc);
        Dimension newSize = new Dimension(originalSize);

        if(selMode == SelectionMode.UpperLeft) {
            newLoc.x = originalLoc.x + totalDx;
            newLoc.y = originalLoc.y + totalDy;
            newSize.width = originalSize.width - totalDx;
            newSize.height = originalSize.height - totalDy;
        } else if(selMode == SelectionMode.LowerLeft) {
            newLoc.x = originalLoc.x + totalDx;
            // newLoc.y = originalLoc.y; // Y does not change from original
            newSize.width = originalSize.width - totalDx;
            newSize.height = originalSize.height + totalDy;
        } else if(selMode == SelectionMode.UpperRight){
            // newLoc.x = originalLoc.x; // X does not change
            newLoc.y = originalLoc.y + totalDy;
            newSize.width = originalSize.width + totalDx;
            newSize.height = originalSize.height - totalDy;
        } else if(selMode == SelectionMode.LowerRight){
            // newLoc = originalLoc; // Location does not change
            newSize.width = originalSize.width + totalDx;
            newSize.height = originalSize.height + totalDy;
        } else if(selMode == SelectionMode.MiddleRight){
            newSize.width = originalSize.width + totalDx;
            // Location and height don't change
            newLoc = originalLoc;
            newSize.height = originalSize.height;
        } else if(selMode == SelectionMode.MiddleLeft){
            newLoc.x = originalLoc.x + totalDx;
            newSize.width = originalSize.width - totalDx;
            // Y and height don't change
            newLoc.y = originalLoc.y;
            newSize.height = originalSize.height;
        } else if(selMode == SelectionMode.MiddleTop) {
            newLoc.y = originalLoc.y + totalDy;
            newSize.height = originalSize.height - totalDy;
            // X and width don't change
            newLoc.x = originalLoc.x;
            newSize.width = originalSize.width;
        } else if(selMode == SelectionMode.MiddleBottom){
            newSize.height = originalSize.height + totalDy;
            // X, Y, and width don't change
            newLoc = originalLoc;
            newSize.width = originalSize.width;
        }

        // Set the calculated preview state directly on the shape
        shape.setLocation(newLoc);
        shape.setWidth(newSize.width);
        shape.setHeight(newSize.height);

        // Important: Do NOT normalize during preview drag, normalize only on mouse release
    }


    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) {
        // If mouse leaves the drawing area during a drag, consider cancelling or completing?
        // For now, do nothing, release event will handle completion.
    }


    @Override
    public void mouseMoved(MouseEvent e) {
        if (drawingStatusPanel != null) {
            drawingStatusPanel.setPoint(e.getPoint());
        }
        // Potentially update cursor based on handle hover when in Select/Scale mode
        // (Requires checking shape handles against mouse position - complex)
    }

    // --- Helper Methods ---

    private void updateStatusBarShape() {
        if (drawingStatusPanel != null) {
            List<Shape> selectedShapes = appService.getSelectedShapes();
            int count = selectedShapes.size();
            if (count == 1) {
                String shapeName = selectedShapes.get(0).getClass().getSimpleName();
                drawingStatusPanel.setShapeName(shapeName);
            } else if (count > 1) {
                drawingStatusPanel.setShapeInfo(count + " shapes selected");
            } else {
                drawingStatusPanel.setShapeName(null);
            }
        }
    }

    public void updateStatusBarTool(String toolName) {
        if (drawingStatusPanel != null) {
            drawingStatusPanel.setToolText(toolName);
        }
    }

    // Maintain aspect ratio during scaling preview/finalization
    private Point maintainAspectRatio(Shape shape, Point anchorPoint, Point dragPoint) {
        // Get the *original* dimensions stored at the start of the drag
        Dimension originalSize = originalSizes.get(shape);
        Point originalLoc = originalLocations.get(shape);
        SelectionMode selMode = shape.getSelectionMode(); // Handle being dragged

        if (originalSize == null || originalLoc == null || originalSize.width == 0 || originalSize.height == 0) {
            return dragPoint; // Cannot maintain ratio if original size is unknown or zero
        }

        double aspectRatio = (double)originalSize.width / originalSize.height;

        // Determine the fixed corner/point opposite the handle being dragged
        Point fixedPoint = new Point();
        switch (selMode) {
            case UpperLeft:  fixedPoint.setLocation(originalLoc.x + originalSize.width, originalLoc.y + originalSize.height); break;
            case LowerLeft:  fixedPoint.setLocation(originalLoc.x + originalSize.width, originalLoc.y); break;
            case UpperRight: fixedPoint.setLocation(originalLoc.x, originalLoc.y + originalSize.height); break;
            case LowerRight: fixedPoint.setLocation(originalLoc.x, originalLoc.y); break;
            // FIX: Correctly qualify enum constants
            case MiddleLeft:
            case MiddleRight:
                // Primarily horizontal scaling, adjust Y based on X change to maintain ratio
                int currentWidth = dragPoint.x - fixedPoint.x;
                int newHeight = (int) Math.round(Math.abs(currentWidth) / aspectRatio);
                // Corrected logic: For edge drags with shift, keep the axis perpendicular to the drag fixed.
                // Since we are dragging Left/Right, the Y-coordinate from the *anchorPoint* (where drag started this segment) is kept.
                return new Point(dragPoint.x, anchorPoint.y); // Lock Y for edge drag


            // FIX: Correctly qualify enum constants
            case MiddleTop:
            case MiddleBottom:
                // Primarily vertical scaling, adjust X based on Y change
                int currentHeight = dragPoint.y - fixedPoint.y;
                int newWidth = (int) Math.round(Math.abs(currentHeight) * aspectRatio);
                // Since we are dragging Up/Down, the X-coordinate from the *anchorPoint* is kept.
                return new Point(anchorPoint.x, dragPoint.y); // Lock X for edge drag

            default: return dragPoint; // Not a scaling handle
        }

        // Calculate deltas from the fixed point to the current drag point
        int dx = dragPoint.x - fixedPoint.x;
        int dy = dragPoint.y - fixedPoint.y;

        // Adjust the smaller delta based on the aspect ratio and the larger delta
        // Determine leading axis based on magnitude relative to aspect ratio
        if (Math.abs(dx) / aspectRatio > Math.abs(dy)) {
            // Width change is dominant
            dy = (int)Math.round(dx / aspectRatio * (dy < 0 ? -1 : 1)); // Adjust dy based on dx and original aspect ratio
        } else {
            // Height change is dominant
            dx = (int)Math.round(dy * aspectRatio * (dx < 0 ? -1 : 1)); // Adjust dx based on dy
        }

        // Return the adjusted point
        return new Point(fixedPoint.x + dx, fixedPoint.y + dy);
    }


    // --- KeyListener Methods ---
    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) {
        // Handle Delete key
        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            List<Shape> selected = appService.getSelectedShapes();
            if (!selected.isEmpty()) {
                // Create a copy to avoid ConcurrentModificationException if service modifies list
                List<Shape> shapesToDelete = new java.util.ArrayList<>(selected);
                for (Shape shape : shapesToDelete) {
                    // Use command-wrapped service for deletion
                    appService.delete(shape);
                }
                // Update UI after deletion
                updateStatusBarShape();
                if (propertySheet != null) propertySheet.populateTable(appService);
                if (drawingView != null) drawingView.repaint();
            }
        }
        // --- Add other key bindings here ---
        // Example: Ctrl+Z for Undo, Ctrl+Y for Redo (though menus/toolbars also handle this)
        else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z) {
            appService.undo();
            // Commands should trigger repaint and property sheet updates
        } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Y) {
            appService.redo();
            // Commands should trigger repaint and property sheet updates
        }
        // Example: Nudge selected shapes with arrow keys
        else if (e.getKeyCode() >= KeyEvent.VK_LEFT && e.getKeyCode() <= KeyEvent.VK_DOWN) {
            List<Shape> selected = appService.getSelectedShapes();
            if (!selected.isEmpty()) {
                int dx = 0, dy = 0;
                int nudgeAmount = e.isShiftDown() ? 10 : 1; // Nudge further if Shift is held
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:  dx = -nudgeAmount; break;
                    case KeyEvent.VK_RIGHT: dx = nudgeAmount;  break;
                    case KeyEvent.VK_UP:    dy = -nudgeAmount; break;
                    case KeyEvent.VK_DOWN:  dy = nudgeAmount;  break;
                }
                if (dx != 0 || dy != 0) {
                    Point startNudge = new Point(0, 0); // Origin for delta calculation
                    Point endNudge = new Point(dx, dy);

                    // Store original state before creating command (essential!)
                    dragStartPoint = new Point(startNudge); // Use dummy start for nudge command
                    originalLocations.clear();
                    originalSizes.clear(); // Not needed for move
                    for (Shape shape : selected) {
                        originalLocations.put(shape, new Point(shape.getLocation()));
                    }

                    // Create a move command for the nudge action using the wrapped service
                    appService.move(startNudge, endNudge);

                    // Clear temporary state (though maybe not strictly needed for nudge)
                    dragStartPoint = null;
                    originalLocations.clear();


                    // Update UI after nudge command execution (Command should handle repaint)
                    if (propertySheet != null) propertySheet.populateTable(appService);
                    // if (drawingView != null) drawingView.repaint(); // Command should do this
                }
            }
        }
    }


    @Override
    public void keyReleased(KeyEvent e) {
        // Could potentially check for Shift release during scaling drag to stop aspect ratio constraint
    }
}