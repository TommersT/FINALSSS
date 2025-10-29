// tommerst/finalsss/FINALSSS-ab98287e5a251ea0f90f45adc2a537ebda4a1511/draw/src/main/java/com/gabriel/draw/controller/DrawingController.java
// Corrected imports and Rectangle usage, fixed status panel call
// Enhanced for real-time property sheet updates during drag
package com.gabriel.draw.controller;

import com.gabriel.draw.component.PropertySheet;
import com.gabriel.draw.model.*;
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
import java.awt.geom.Line2D; // Import for Line2D
import java.awt.image.BufferedImage; // Import for BufferedImage
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList; // <-- ADDED: Import ArrayList

// Import java.awt.Rectangle explicitly to avoid confusion with com.gabriel.draw.model.Rectangle
import java.awt.Rectangle;


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
        drawingView.requestFocusInWindow(); // Request focus for keyboard events

        // Ensure selection happens on click when in select mode
        if (appService.getToolMode() == ToolMode.SELECT && e.getClickCount() == 1) {
            // Perform hit detection (SearchService determines selectionMode if handle is hit)
            appService.search(e.getPoint(), !e.isControlDown()); // Handle single/multi-select
            updateStatusBarShape();
            // *** Update PropertySheet on selection change ***
            if (propertySheet != null) {
                // Use invokeLater to ensure it runs after any pending AWT events from the click
                SwingUtilities.invokeLater(() -> propertySheet.populateTable(appService)); // <<< UPDATED
            }
            drawingView.repaint(); // Repaint to show selection handles/changes
        }

        // Handle double-click editing for Text
        if (e.getClickCount() == 2 && appService.getToolMode() == ToolMode.SELECT) {
            // Check if the double-click hit a text shape
            Shape clickedShape = searchService_getShapeAtPoint(e.getPoint()); // Use helper for hit check
            if (clickedShape instanceof Text) {
                appService.select(clickedShape); // Ensure it's selected
                drawingView.repaint(); // Show selection

                // Store old values for potential command creation
                String oldText = clickedShape.getText();
                Font oldFont = clickedShape.getFont();

                JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(drawingView);
                TextInputDialog dialog = TextInputDialog.showDialog(parentFrame, oldText, oldFont);

                if (dialog.isOkClicked()) {
                    String newText = dialog.getText();
                    Font newFont = dialog.getSelectedFont();

                    boolean textChanged = newText != null && !newText.equals(oldText);
                    boolean fontChanged = newFont != null && !newFont.equals(oldFont);

                    // Use wrapped AppService setters which should trigger commands via PropertyEventListener
                    if (textChanged || fontChanged) {
                        // Batch changes for a single undo step if possible, though individual commands might be fine
                        if (textChanged) {
                            appService.setText(newText); // Triggers SetTextCommand via listener
                        }
                        if (fontChanged) {
                            // PropertyEventListener should generate specific font commands based on detected changes
                            if (!newFont.getFamily().equals(oldFont.getFamily())) {
                                appService.setFontFamily(newFont.getFamily()); // Triggers SetFontFamilyCommand
                            }
                            if (newFont.getStyle() != oldFont.getStyle()) {
                                appService.setFontStyle(newFont.getStyle()); // Triggers SetFontStyleCommand
                            }
                            if (newFont.getSize() != oldFont.getSize()) {
                                appService.setFontSize(newFont.getSize()); // Triggers SetFontSizeCommand
                            }
                        }
                        // Let command listener handle repaint and property sheet updates
                    }
                }
            } // End if clickedShape is Text
        } // End if double-click
    }


    @Override
    public void mousePressed(MouseEvent e) {
        if (drawingView == null) return;
        drawingView.requestFocusInWindow(); // Ensure keyboard events are captured

        // Only proceed if not already in a drag operation
        if (appService.getDrawMode() == DrawMode.Idle) {
            start = e.getPoint(); // Record the starting point for this potential action
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
                    // Determine if the click potentially starts a move or scale
                    Shape primaryShape = appService.getSelectedShape();
                    boolean onHandle = primaryShape != null && primaryShape.getSelectionMode() != SelectionMode.None;
                    boolean canScale = (currentToolMode == ToolMode.SCALE || (currentToolMode == ToolMode.SELECT && onHandle));
                    boolean canMove = (currentToolMode == ToolMode.MOVE || (currentToolMode == ToolMode.SELECT && !onHandle));

                    if (canMove || canScale) {
                        isDraggingForMoveOrScale = true; // Flag potential drag
                        for (Shape shape : selectedShapes) {
                            // Defensive copy for location
                            if (shape.getLocation() != null) {
                                originalLocations.put(shape, new Point(shape.getLocation()));
                            } else {
                                System.err.println("Warning: Shape " + shape.getId() + " has null location in mousePressed.");
                                // Handle appropriately, maybe skip or default
                            }
                            if (canScale) { // Only store size if scaling is possible
                                originalSizes.put(shape, new Dimension(shape.getWidth(), shape.getHeight()));
                            }
                        }
                    } else {
                        isDraggingForMoveOrScale = false; // Clicked on selected shape but wrong tool mode
                    }
                } else {
                    // Clicked empty space, ensure drag flag is off
                    isDraggingForMoveOrScale = false;
                }
                // Update UI based on new selection state
                updateStatusBarShape();
                // *** Update PropertySheet on selection change ***
                if (propertySheet != null) {
                    // Use invokeLater for safety after mouse event processing
                    SwingUtilities.invokeLater(() -> propertySheet.populateTable(appService)); // <<< UPDATED
                }
                drawingView.repaint(); // Repaint to show selection handles/changes

            }
            // --- Handling Shape Creation Initiation ---
            else if (currentToolMode == ToolMode.DRAW) {
                // Always clear selection when starting a new shape
                appService.clearSelections();
                updateStatusBarShape();
                // *** Update PropertySheet to show global properties ***
                if (propertySheet != null) {
                    // Use invokeLater
                    SwingUtilities.invokeLater(() -> propertySheet.populateTable(appService)); // <<< UPDATED
                }

                ShapeMode shapeToDraw = appService.getShapeMode();
                currentShape = null; // Reset preview shape

                // Create the appropriate shape instance (preview)
                switch (shapeToDraw) {
                    case Line:      currentShape = new Line(start); break;
                    case Rectangle: currentShape = new com.gabriel.draw.model.Rectangle(start); break; // Use full path
                    case Ellipse:   currentShape = new Ellipse(start); break;
                    case Text:
                        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(drawingView);
                        // Use current service defaults for the dialog
                        TextInputDialog dialog = TextInputDialog.showDialog(parentFrame, appService.getText(), appService.getFont());
                        if (dialog.isOkClicked()) {
                            String textContent = dialog.getText();
                            if (textContent != null && !textContent.trim().isEmpty()) {
                                currentShape = new Text(start); // Location is baseline start
                                currentShape.setText(textContent);
                                currentShape.setFont(dialog.getSelectedFont());

                                // Pre-calculate bounds immediately after creation
                                Graphics g = drawingView.getGraphics();
                                if (g != null) {
                                    try {
                                        Dimension bounds = calculateTextBounds(g, (Text)currentShape);
                                        currentShape.setWidth(bounds.width);
                                        currentShape.setHeight(bounds.height);
                                    } finally { g.dispose(); }
                                } else {
                                    System.err.println("Warning: Could not get Graphics to pre-calculate text bounds.");
                                    // Set some initial non-zero size
                                    currentShape.setWidth(Math.max(10, textContent.length() * 8));
                                    currentShape.setHeight(16);
                                }

                            } else { // No text entered
                                appService.setDrawMode(DrawMode.Idle); return;
                            }
                        } else { // Dialog cancelled
                            appService.setDrawMode(DrawMode.Idle); return;
                        }
                        break;
                    case Image:
                        String imageFilename = appService.getImageFileename();
                        if (imageFilename == null || imageFilename.isEmpty()) {
                            appService.setImageFileename();
                            imageFilename = appService.getImageFileename();
                            if (imageFilename == null || imageFilename.isEmpty()) {
                                appService.setDrawMode(DrawMode.Idle); return;
                            }
                        }
                        // Use correct Picture constructor
                        currentShape = new Picture(start, start, imageFilename);
                        break;
                    default: // Handle Select case specifically or others
                        if (shapeToDraw == ShapeMode.Select) {
                            System.err.println("Warning: ToolMode is DRAW but ShapeMode is Select. Resetting state.");
                            appService.setDrawMode(DrawMode.Idle); return;
                        }
                        appService.setDrawMode(DrawMode.Idle); return;
                }

                // Apply current global properties to the new shape preview
                if (currentShape != null) {
                    currentShape.setColor(drawing.getColor()); // Use drawing defaults
                    currentShape.setThickness(drawing.getThickness());
                    currentShape.setFill(drawing.getFill());
                    currentShape.setStartColor(drawing.getStartColor());
                    currentShape.setEndColor(drawing.getEndColor());
                    currentShape.setUseGradient(drawing.isUseGradient());
                    currentShape.setVisible(true); // New shapes are visible
                    if (!(currentShape instanceof Text)) { // Apply global font if not text
                        currentShape.setFont(drawing.getFont());
                    }
                    // Assign temporary ID maybe?
                    // currentShape.setId(-1); // Indicate it's a preview
                } else {
                    // Shape creation failed (e.g., dialog cancelled)
                    appService.setDrawMode(DrawMode.Idle);
                }
            } else {
                // Unknown tool mode, reset state
                appService.setDrawMode(DrawMode.Idle);
            }
        } // End if DrawMode is Idle
    }


    @Override
    public void mouseDragged(MouseEvent e) {
        // Only process drag if mouse button was pressed
        if (appService.getDrawMode() == DrawMode.MousePressed) {
            end = e.getPoint(); // Current drag position
            ToolMode currentToolMode = appService.getToolMode();
            Shape primarySelectedShape = appService.getSelectedShape();

            java.awt.Rectangle repaintBounds = null; // Use java.awt.Rectangle for dirty region
            int margin = 20; // Margin for repainting handles/stroke/shadows

            // --- Live Preview for Shape Creation ---
            if (currentToolMode == ToolMode.DRAW && currentShape != null) {
                // Calculate old bounds before updating
                Point locOld = currentShape.getLocation();
                int wOld = currentShape.getWidth();
                int hOld = currentShape.getHeight();
                int yOffsetOld = calculateYOffset(currentShape); // Use helper for visual top

                // Ensure location isn't null before using it
                if (locOld != null) {
                    repaintBounds = new java.awt.Rectangle(
                            locOld.x - margin,
                            locOld.y + yOffsetOld - margin, // Use visual top for Y
                            Math.abs(wOld) + 2 * margin,
                            Math.abs(hOld) + 2 * margin);
                } else {
                    // If location is null, need to repaint whole view maybe?
                    repaintBounds = drawingView != null ? drawingView.getBounds() : null;
                }


                // Update the preview shape's size based on the drag
                AppService baseService = getUnderlyingService(); // Use helper
                // scale(Shape, Point end) updates width/height relative to start point
                baseService.scale(currentShape, end); // Use base service's scale for preview

                // Recalculate Text bounds during creation drag
                if (currentShape instanceof Text) {
                    Graphics g = drawingView.getGraphics();
                    if (g != null) {
                        try {
                            Dimension bounds = calculateTextBounds(g, (Text)currentShape);
                            currentShape.setWidth(bounds.width);
                            currentShape.setHeight(bounds.height);
                        } finally { g.dispose(); }
                    }
                }

                // Calculate new bounds after updating
                Point locNew = currentShape.getLocation();
                int wNew = currentShape.getWidth();
                int hNew = currentShape.getHeight();
                int yOffsetNew = calculateYOffset(currentShape); // Recalculate offset

                if (locNew != null) {
                    java.awt.Rectangle newBounds = new java.awt.Rectangle(
                            locNew.x - margin,
                            locNew.y + yOffsetNew - margin, // Use visual top for Y
                            Math.abs(wNew) + 2 * margin,
                            Math.abs(hNew) + 2 * margin);
                    // Add new bounds to the repaint region
                    if (repaintBounds != null) {
                        repaintBounds.add(newBounds);
                    } else {
                        repaintBounds = newBounds;
                    }
                } else if (repaintBounds == null) {
                    // If still null, repaint whole view as fallback
                    repaintBounds = drawingView != null ? drawingView.getBounds() : null;
                }


                // *** NO real-time property sheet update during CREATION drag ***

            }
            // --- Live Preview for Move/Scale ---
            else if (isDraggingForMoveOrScale) // Check flag set in mousePressed
            {
                // Determine if the current drag action is scaling or moving
                boolean isScale = (currentToolMode == ToolMode.SCALE) ||
                        (currentToolMode == ToolMode.SELECT && primarySelectedShape != null && primarySelectedShape.getSelectionMode() != SelectionMode.None);
                boolean isMove = (currentToolMode == ToolMode.MOVE) ||
                        (currentToolMode == ToolMode.SELECT && !isScale);

                // Use delta from *last* drag point ('start' field) for smooth preview update
                int dx = end.x - start.x;
                int dy = end.y - start.y;

                List<Shape> shapesToUpdate = appService.getSelectedShapes(); // Get all selected shapes

                AppService baseService = getUnderlyingService(); // Use helper

                for (Shape shape : shapesToUpdate) {
                    Point loc = shape.getLocation();
                    int w = shape.getWidth();
                    int h = shape.getHeight();
                    int yOffset = calculateYOffset(shape); // Use helper for visual top

                    // Calculate old bounds for this shape (using visual top for Y)
                    if (loc != null) {
                        java.awt.Rectangle oldShapeBounds = new java.awt.Rectangle(
                                loc.x - margin, loc.y + yOffset - margin,
                                Math.abs(w) + 2 * margin, Math.abs(h) + 2 * margin);
                        if (repaintBounds == null) repaintBounds = oldShapeBounds;
                        else repaintBounds.add(oldShapeBounds);
                    } else if (repaintBounds == null){
                        // Fallback repaint whole view
                        repaintBounds = drawingView != null ? drawingView.getBounds() : null;
                    }


                    // --- Apply transformation for LIVE PREVIEW ---
                    if (isMove) {
                        // Use base service's move method for preview (applies delta to each selected shape)
                        baseService.move(shape, start, end); // Updates shape's location in place

                    } else if (isScale && shape == primarySelectedShape) { // Only scale primary interactively for preview
                        Point scaleEnd = end;
                        // Apply aspect ratio constraint LIVE during drag if Shift is held
                        if (e.isShiftDown()) {
                            Dimension originalSize = originalSizes.get(shape);
                            if (originalSize != null) {
                                // maintainAspectRatio needs original state, use dragStartPoint here
                                scaleEnd = maintainAspectRatio(shape, dragStartPoint, end);
                            }
                        }
                        // applyScalePreview uses original state + total delta for accuracy
                        applyScalePreview(shape, start, scaleEnd); // Use helper for direct manipulation

                        // If scaling text via font size, recalculate bounds here
                        // if (shape instanceof Text) { ... recalculate width/height ... }
                    }
                    // --- End Apply transformation ---

                    // Calculate new bounds for this shape after transformation (using visual top for Y)
                    Point newLoc = shape.getLocation();
                    int newW = shape.getWidth();
                    int newH = shape.getHeight();
                    int newYOffset = calculateYOffset(shape); // Recalculate offset

                    if (newLoc != null) {
                        java.awt.Rectangle newShapeBounds = new java.awt.Rectangle(
                                newLoc.x - margin, newLoc.y + newYOffset - margin,
                                Math.abs(newW) + 2 * margin, Math.abs(newH) + 2 * margin);
                        if (repaintBounds != null) repaintBounds.add(newShapeBounds);
                        else repaintBounds = newShapeBounds;
                    } else if (repaintBounds == null) {
                        // Fallback repaint whole view
                        repaintBounds = drawingView != null ? drawingView.getBounds() : null;
                    }

                } // End loop through shapes

                // *** Update property sheet live during drag ***
                if (propertySheet != null) {
                    propertySheet.populateTable(appService); // <<< ENSURE THIS LINE IS PRESENT
                }

                start = new Point(end); // *** Update start for the next drag increment ***
            } // End if isDraggingForMoveOrScale


            // Perform repaint using calculated bounds
            if (drawingView != null && repaintBounds != null) {
                // Ensure bounds have positive dimensions before repainting
                repaintBounds.width = Math.max(1, repaintBounds.width); // At least 1 pixel
                repaintBounds.height = Math.max(1, repaintBounds.height);
                drawingView.repaint(repaintBounds); // Repaint only the dirty region
            } else if (drawingView != null) {
                drawingView.repaint(); // Fallback to full repaint if bounds are null
            }
        } // End if DrawMode is MousePressed
    }


    @Override
    public void mouseReleased(MouseEvent e) {
        // Only finalize if a drag operation was actually started
        if (appService.getDrawMode() == DrawMode.MousePressed) {
            end = e.getPoint(); // Final position
            ToolMode currentToolMode = appService.getToolMode();
            Shape primarySelectedShape = appService.getSelectedShape(); // Get primary for scale check

            // --- Finalize Move or Scale ---
            // Check if a drag was initiated and we have original state stored
            if (isDraggingForMoveOrScale && dragStartPoint != null && !originalLocations.isEmpty()) {

                // Determine if it was a move or scale based on tool mode and handle interaction at start
                boolean wasScale = (currentToolMode == ToolMode.SCALE) ||
                        (currentToolMode == ToolMode.SELECT && primarySelectedShape != null && primarySelectedShape.getSelectionMode() != SelectionMode.None);
                boolean wasMove = (currentToolMode == ToolMode.MOVE) ||
                        (currentToolMode == ToolMode.SELECT && !wasScale);

                // Check if the mouse actually moved significantly (avoid empty commands)
                boolean mouseMoved = !end.equals(dragStartPoint);

                // <<<<< START RESTORE STATE (Needed Before Creating Command) >>>>>
                // Restore all affected shapes to their original state captured in mousePressed
                // This ensures the command calculates the correct final state based on the *total* drag delta
                List<Shape> shapesToRestore = new ArrayList<>(originalLocations.keySet());
                for (Shape shape : shapesToRestore) {
                    Point originalLoc = originalLocations.get(shape);
                    Dimension originalSize = originalSizes.get(shape);

                    if (originalLoc != null) {
                        shape.setLocation(new Point(originalLoc)); // Restore location
                    }
                    if (wasScale && originalSize != null) { // Only restore size if it was a scale
                        shape.setWidth(originalSize.width);
                        shape.setHeight(originalSize.height);
                    }
                    // Restore font size if scaling text that way?
                    // if (wasScale && shape instanceof Text && originalFontSizes.containsKey(shape)) { ... restore font ... }
                }
                // Repaint immediately after restoring to clear the preview state *before* command executes
                // Only repaint if mouse actually moved, otherwise it just flashes unnecessarily
                if (mouseMoved && drawingView != null) {
                    drawingView.repaint();
                    // Add a small delay if needed for visual consistency, but usually direct repaint is fine
                    // try { Thread.sleep(10); } catch (InterruptedException ie) {}
                }
                // <<<<< END RESTORE STATE >>>>>

                // Now create and execute the command if the mouse actually moved
                if (mouseMoved) {
                    if (wasMove) {
                        // Move command operates on all currently selected shapes implicitly
                        // Pass the absolute start and end points of the drag
                        appService.move(dragStartPoint, end); // Creates MoveCommand via wrapper
                    } else if (wasScale && primarySelectedShape != null) {
                        // Apply final aspect ratio constraint if shift was held at release
                        Point scaleEnd = end;
                        if (e.isShiftDown()) {
                            Dimension originalSize = originalSizes.get(primarySelectedShape);
                            if (originalSize != null) {
                                // Use the original state and absolute points for aspect ratio calculation
                                scaleEnd = maintainAspectRatio(primarySelectedShape, dragStartPoint, end);
                            }
                        }
                        // Scale command operates only on the primary shape with the handle
                        // Pass absolute start and calculated final end points
                        appService.scale(primarySelectedShape, dragStartPoint, scaleEnd); // Creates ScaleCommand via wrapper
                    }
                } // end if mouseMoved
            }
            // --- Finalize Shape Creation ---
            else if (currentToolMode == ToolMode.DRAW && currentShape != null) {
                AppService baseService = getUnderlyingService(); // Use helper
                // Set final size based on end point relative to the shape's start
                baseService.scale(currentShape, end);

                // Recalculate Text bounds based on final font/text (important after dialog)
                if (currentShape instanceof Text) {
                    Graphics g = drawingView.getGraphics();
                    if (g != null) {
                        try {
                            Dimension bounds = calculateTextBounds(g, (Text)currentShape);
                            currentShape.setWidth(bounds.width);
                            currentShape.setHeight(bounds.height);
                        } finally { g.dispose(); }
                    }
                }

                // Only normalize shapes that are NOT lines and NOT Text (to ensure positive width/height)
                boolean isLine = currentShape instanceof com.gabriel.draw.model.Line;
                boolean isText = currentShape instanceof com.gabriel.draw.model.Text;
                if (!isLine && !isText) {
                    Normalizer.normalize(currentShape); // Ensure width/height positive
                }

                boolean hasSize = Math.abs(currentShape.getWidth()) > 0 || Math.abs(currentShape.getHeight()) > 0;
                // Don't create zero-size shapes unless it's a line (lines have length even if width/height diff is 0)
                // Or maybe allow zero-size for text if needed? Check requirements.
                if (isLine || hasSize) {
                    appService.create(currentShape); // Creates AddShapeCommand via wrapper
                }
                // Don't auto-select the newly created shape
                // appService.select(currentShape); // <<< REMOVED Auto-selection

                currentShape = null; // Clear the preview shape
            }

            // --- Reset State after any operation ---
            appService.setDrawMode(DrawMode.Idle);
            isDraggingForMoveOrScale = false;
            dragStartPoint = null;
            originalLocations.clear();
            originalSizes.clear();
            currentShape = null; // Ensure preview shape is cleared

            // *** Update PropertySheet on final state ***
            if (propertySheet != null) {
                // Defer update slightly using invokeLater to allow command execution/listener to finish first
                SwingUtilities.invokeLater(() -> propertySheet.populateTable(appService)); // <<< ENSURE THIS IS PRESENT
            }
            updateStatusBarShape(); // Update status bar after selection might change (e.g., after creation clears selection)
            // CommandService listener will handle repaint updates after command execution
            // drawingView.repaint(); // Repaint handled by Command Listener
        } // End if DrawMode was MousePressed
    }


    // --- Helper for applying scale PREVIEW logic ---
    // This method directly modifies the shape for preview purposes. It needs the original state.
    private void applyScalePreview(Shape shape, Point previewStartUnused, Point previewEnd) {
        // We need the state from the beginning of the whole drag operation (stored in maps)
        Dimension originalSize = originalSizes.get(shape);
        Point originalLoc = originalLocations.get(shape);

        // Check if we have the necessary original state information
        if (originalSize == null || originalLoc == null || dragStartPoint == null) {
            System.err.println("applyScalePreview: Missing original state for shape ID " + (shape != null ? shape.getId() : "null"));
            return; // Cannot preview without original state
        }

        // Calculate the total delta from the absolute drag start to the current preview end point
        int totalDx = previewEnd.x - dragStartPoint.x;
        int totalDy = previewEnd.y - dragStartPoint.y;

        SelectionMode selMode = shape.getSelectionMode(); // Use the handle mode determined on press

        // Apply scaling based on handle and TOTAL delta from dragStartPoint, starting from original state
        Point newLoc = new Point(originalLoc); // Start calculation from original location
        Dimension newSize = new Dimension(originalSize); // Start calculation from original size

        // --- Standard Stretching Scaling Logic (apply delta to original state) ---
        if(selMode == SelectionMode.UpperLeft) {
            newLoc.x = originalLoc.x + totalDx;
            newLoc.y = originalLoc.y + totalDy;
            newSize.width = originalSize.width - totalDx;
            newSize.height = originalSize.height - totalDy;
        } else if(selMode == SelectionMode.LowerLeft) {
            newLoc.x = originalLoc.x + totalDx;
            // newLoc.y remains originalLoc.y
            newSize.width = originalSize.width - totalDx;
            newSize.height = originalSize.height + totalDy;
        } else if(selMode == SelectionMode.UpperRight){
            // newLoc.x remains originalLoc.x
            newLoc.y = originalLoc.y + totalDy;
            newSize.width = originalSize.width + totalDx;
            newSize.height = originalSize.height - totalDy;
        } else if(selMode == SelectionMode.LowerRight){
            // newLoc remains originalLoc
            newSize.width = originalSize.width + totalDx;
            newSize.height = originalSize.height + totalDy;
        } else if(selMode == SelectionMode.MiddleRight){
            // newLoc remains originalLoc
            newSize.width = originalSize.width + totalDx;
            // newSize.height remains originalSize.height
        } else if(selMode == SelectionMode.MiddleLeft){
            newLoc.x = originalLoc.x + totalDx;
            // newLoc.y remains originalLoc.y
            newSize.width = originalSize.width - totalDx;
            // newSize.height remains originalSize.height
        } else if(selMode == SelectionMode.MiddleTop) {
            // newLoc.x remains originalLoc.x
            newLoc.y = originalLoc.y + totalDy;
            // newSize.width remains originalSize.width
            newSize.height = originalSize.height - totalDy;
        } else if(selMode == SelectionMode.MiddleBottom){
            // newLoc remains originalLoc
            // newSize.width remains originalSize.width
            newSize.height = originalSize.height + totalDy;
        }
        // --- End Scaling Logic ---

        // Set the calculated preview state directly on the shape
        shape.setLocation(newLoc);
        shape.setWidth(newSize.width);
        shape.setHeight(newSize.height);
        // If scaling font size, apply that preview change here too
        // if (shape instanceof Text) { ... set font size based on scale factor ... }
    }


    @Override
    public void mouseEntered(MouseEvent e) { } // Not used

    @Override
    public void mouseExited(MouseEvent e) {
        // Clear coordinates when mouse leaves the drawing area
        if (drawingStatusPanel != null) {
            drawingStatusPanel.setPoint(new Point(-1, -1)); // Use -1,-1 or similar indicator
        }
    }


    @Override
    public void mouseMoved(MouseEvent e) {
        // Update coordinates in status bar as mouse moves
        if (drawingStatusPanel != null) {
            drawingStatusPanel.setPoint(e.getPoint());
        }
        // Update cursor based on tool and position
        updateCursor(e.getPoint());
    }

    // --- Helper Methods ---

    // Helper to get underlying service (skips command wrapper)
    private AppService getUnderlyingService() {
        if (appService instanceof DrawingCommandAppService) {
            return ((DrawingCommandAppService) appService).getUnderlyingAppService();
        }
        return appService; // Fallback if not wrapped (shouldn't happen with proper setup)
    }

    // Helper to get FontMetrics safely (needed for text bounds calculation)
    private FontMetrics getFontMetrics(Font font) {
        // Try getting from the view first if available
        if (drawingView != null) {
            Graphics g = drawingView.getGraphics(); // Get transient graphics context
            if (g != null) {
                try {
                    // Use default font if shape's font is somehow null
                    return g.getFontMetrics(font != null ? font : new Font("SansSerif", Font.PLAIN, 12));
                } finally {
                    g.dispose(); // Always dispose temporary graphics
                }
            }
        }
        // Fallback: create temporary graphics if view is unavailable
        Graphics tempG = null;
        try {
            // Use a BufferedImage to create a temporary graphics context
            tempG = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
            if (tempG != null) {
                return tempG.getFontMetrics(font != null ? font : new Font("SansSerif", Font.PLAIN, 12));
            }
        } catch (Exception e) {
            System.err.println("Error getting FontMetrics via fallback: " + e.getMessage());
        } finally {
            if (tempG != null) tempG.dispose(); // Dispose temporary graphics
        }
        return null; // Should ideally not happen
    }

    // Helper specifically for calculating Text bounds based on font and content
    private Dimension calculateTextBounds(Graphics g, Text textShape) {
        Font font = textShape.getFont();
        String textContent = textShape.getText();
        // Use defaults if font or text is missing
        if (font == null) font = new Font("SansSerif", Font.PLAIN, 12);
        if (textContent == null || textContent.isEmpty()) textContent = "Text"; // Use placeholder

        FontMetrics fm = g.getFontMetrics(font); // Get metrics from provided graphics
        if (fm == null) return new Dimension(10, 10); // Fallback if metrics failed

        int textWidth = fm.stringWidth(textContent);
        int textHeight = fm.getAscent() + fm.getDescent(); // Use ascent+descent for height
        // Add some padding for visual spacing and easier handle interaction
        return new Dimension(Math.max(10, textWidth + 10), Math.max(10, textHeight + 5));
    }

    // Helper to calculate Y offset for visual top-left (especially for Text baseline)
    private int calculateYOffset(Shape shape) {
        if (shape instanceof Text && shape.getFont() != null) {
            FontMetrics fm = getFontMetrics(shape.getFont()); // Use helper
            if (fm != null) {
                return -fm.getAscent(); // Visual top is above the baseline Y coordinate by the ascent amount
            }
        }
        return 0; // No offset for other shapes (their location IS the top-left)
    }


    // Helper to update the shape info in the status bar
    private void updateStatusBarShape() {
        if (drawingStatusPanel != null) {
            List<Shape> selectedShapes = appService.getSelectedShapes();
            int count = selectedShapes.size();
            if (count == 1) { // Single selection
                String shapeName = selectedShapes.get(0).getClass().getSimpleName();
                drawingStatusPanel.setShapeName(shapeName); // Display "Shape: Rectangle", etc.
            } else if (count > 1) { // Multi-selection
                drawingStatusPanel.setShapeInfo(count + " shapes selected"); // Display count
            } else { // No selection
                drawingStatusPanel.setShapeName(null); // Clears to default "No shape selected"
            }
        }
    }

    // Helper to update the tool name in the status bar (called by ActionController)
    public void updateStatusBarTool(String toolName) {
        if (drawingStatusPanel != null) {
            drawingStatusPanel.setToolText(toolName);
        }
        updateCursor(null); // Update cursor immediately when tool changes
    }

    // Helper: Maintain aspect ratio during scaling preview/finalization
    // Calculates the adjusted 'end' point based on aspect ratio
    private Point maintainAspectRatio(Shape shape, Point anchorPoint, Point dragPoint) {
        // Needs original state which is stored during mousePressed
        Dimension originalSize = originalSizes.get(shape);
        Point originalLoc = originalLocations.get(shape);
        SelectionMode selMode = shape.getSelectionMode(); // Handle being dragged

        // Check if we have valid original state and dimensions
        if (originalSize == null || originalLoc == null || originalSize.width == 0 || originalSize.height == 0) {
            return dragPoint; // Cannot maintain aspect ratio without valid info
        }

        double aspectRatio = (double)originalSize.width / originalSize.height;

        // Determine the fixed corner opposite the handle being dragged
        Point fixedPoint = new Point();
        switch (selMode) {
            // Corner handles have an opposite fixed corner
            case UpperLeft:  fixedPoint.setLocation(originalLoc.x + originalSize.width, originalLoc.y + originalSize.height); break;
            case LowerLeft:  fixedPoint.setLocation(originalLoc.x + originalSize.width, originalLoc.y); break;
            case UpperRight: fixedPoint.setLocation(originalLoc.x, originalLoc.y + originalSize.height); break;
            case LowerRight: fixedPoint.setLocation(originalLoc.x, originalLoc.y); break;
            // Middle handles constrain movement to one axis, aspect ratio doesn't apply directly
            // We return a point constrained to the drag axis
            case MiddleLeft: case MiddleRight: return new Point(dragPoint.x, anchorPoint.y); // Lock Y
            case MiddleTop: case MiddleBottom: return new Point(anchorPoint.x, dragPoint.y); // Lock X
            default: return dragPoint; // No handle or unknown handle
        }

        // Calculate delta from the fixed point to the current drag point
        int dx = dragPoint.x - fixedPoint.x;
        int dy = dragPoint.y - fixedPoint.y;

        // Adjust the smaller delta based on the aspect ratio and the larger delta
        // This preserves the aspect ratio relative to the fixed corner
        if (Math.abs(dx) * originalSize.height > Math.abs(dy) * originalSize.width) {
            // Width change is dominant, adjust height
            dy = (int)Math.round(dx / aspectRatio * (dy < 0 ? -1 : 1)); // Maintain sign of original dy
        } else {
            // Height change is dominant, adjust width
            dx = (int)Math.round(dy * aspectRatio * (dx < 0 ? -1 : 1)); // Maintain sign of original dx
        }
        // Return the adjusted drag point
        return new Point(fixedPoint.x + dx, fixedPoint.y + dy);
    }

    // --- Helper to get shape at point (used for double-click and cursor updates) ---
    // This needs to be robust and consider different shape types
    private Shape searchService_getShapeAtPoint(Point p) {
        List<Shape> shapes = drawing.getShapes();
        int searchRadius = appService.getSearchRadius(); // Tolerance for clicking near shapes

        // Iterate backwards to find the top-most shape
        for (int i = shapes.size() - 1; i >= 0; i--) {
            Shape shape = shapes.get(i);
            // Skip null or invisible shapes
            if (shape == null || !shape.isVisible()) continue;

            Point loc = shape.getLocation();
            int w = shape.getWidth();
            int h = shape.getHeight();
            // Handle null location defensively
            if (loc == null) continue;

            java.awt.Rectangle bounds; // Use AWT Rectangle for bounds checking

            if (shape instanceof Text) {
                // For text, use visual bounds based on baseline and ascent
                int yOffset = calculateYOffset(shape); // Get offset from baseline to visual top
                // Normalize coordinates for bounds calculation
                int x1 = Math.min(loc.x, loc.x + w);
                int y1 = Math.min(loc.y + yOffset, loc.y + yOffset + h); // Use visual top-left Y
                bounds = new java.awt.Rectangle(x1, y1, Math.abs(w), Math.abs(h));
            } else if (shape instanceof Line) {
                // For lines, check distance from point to line segment
                Line2D line = new Line2D.Double(loc.x, loc.y, loc.x + w, loc.y + h);
                // Increase tolerance based on line thickness
                if (line.ptSegDist(p) <= searchRadius + (shape.getThickness()/2.0)) {
                    return shape; // Point is close enough to the line
                }
                continue; // Skip bounds check if distance check failed
            } else {
                // For other shapes (Rectangle, Ellipse, Picture), use standard bounding box
                // Normalize coordinates
                int x1 = Math.min(loc.x, loc.x + w);
                int y1 = Math.min(loc.y, loc.y + h);
                bounds = new java.awt.Rectangle(x1, y1, Math.abs(w), Math.abs(h));
            }

            // Inflate bounds slightly to make clicking near edges easier
            bounds.grow(searchRadius / 2, searchRadius / 2);
            // Check if the point is within the inflated bounds
            if (bounds.contains(p)) {
                return shape; // Found the top-most shape containing the point
            }
        }
        return null; // No shape found at this point
    }

    // --- Helper to update cursor based on context ---
    private void updateCursor(Point mousePoint) {
        if (drawingView == null) return; // Need view to set cursor

        Cursor newCursor = Cursor.getDefaultCursor(); // Start with default
        ToolMode currentTool = appService.getToolMode();
        Shape primarySelected = appService.getSelectedShape();

        // Check for handle hover if in Select or Scale mode
        if (mousePoint != null && (currentTool == ToolMode.SELECT || currentTool == ToolMode.SCALE)) {
            if (primarySelected != null && primarySelected.isSelected()) {
                // Find which handle (if any) is under the mouse
                SelectionMode handle = searchService_getHandleAtPoint(primarySelected, mousePoint);
                // Get the appropriate resize cursor for that handle
                newCursor = getCursorForHandle(handle);
            }
            // If default cursor and in SELECT mode, check if hovering over any shape body
            if (newCursor == Cursor.getDefaultCursor() && currentTool == ToolMode.SELECT) {
                Shape hoveredShape = searchService_getShapeAtPoint(mousePoint);
                if (hoveredShape != null) {
                    // Change to hand cursor to indicate the shape is clickable/movable
                    newCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                }
            }
        } else { // Not checking handles, determine cursor based on active tool
            switch (currentTool) {
                case DRAW: newCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); break;
                case MOVE: newCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR); break;
                // SELECT and SCALE defaults handled above or fall through to default
                case SCALE:
                case SELECT:
                default:
                    // If mousePoint is available, check for hand cursor over shape in SELECT mode
                    if (mousePoint != null && currentTool == ToolMode.SELECT) {
                        Shape hoveredShape = searchService_getShapeAtPoint(mousePoint);
                        if (hoveredShape != null) {
                            newCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                        }
                    }
                    // Otherwise, keep default cursor
                    break;
            }
        }

        // Only update the cursor if it has actually changed
        if (drawingView.getCursor() != newCursor) {
            drawingView.setCursor(newCursor);
        }
    }

    // --- Helper to find which handle is at a point ---
    // Checks the 8 handle regions for a given shape
    private SelectionMode searchService_getHandleAtPoint(Shape shape, Point p) {
        // Only check handles for selected, visible shapes
        if (!shape.isSelected() || !shape.isVisible()) return SelectionMode.None;

        Point loc = shape.getLocation();
        int w = shape.getWidth();
        int h = shape.getHeight();
        int r = shape.getR(); // Handle half-size/radius

        // Handle null location
        if (loc == null) return SelectionMode.None;

        // Special case for Line: only check start and end points
        if (shape instanceof Line) {
            java.awt.Rectangle startHandle = new java.awt.Rectangle(loc.x - r, loc.y - r, 2 * r, 2 * r);
            java.awt.Rectangle endHandle = new java.awt.Rectangle(loc.x + w - r, loc.y + h - r, 2 * r, 2 * r);
            if (startHandle.contains(p)) return SelectionMode.UpperLeft; // Treat line start as UL handle
            if (endHandle.contains(p)) return SelectionMode.LowerRight; // Treat line end as LR handle
            return SelectionMode.None; // No handle hit for line
        }

        // For other shapes, calculate visual top-left (especially for Text)
        int yOffset = calculateYOffset(shape); // Get offset for Text baseline
        loc = new Point(loc.x, loc.y + yOffset); // Use visual top-left for handle calculations

        // Normalize coordinates and dimensions
        int x1 = Math.min(loc.x, loc.x + w);
        int y1 = Math.min(loc.y, loc.y + h);
        int widthAbs = Math.abs(w);
        int heightAbs = Math.abs(h);

        // Define rectangles for each of the 8 handles
        // Use java.awt.Rectangle for contains() check
        if (new Rectangle(x1 - r, y1 - r, 2 * r, 2 * r).contains(p)) return SelectionMode.UpperLeft;
        if (new Rectangle(x1 - r, y1 + heightAbs - r, 2 * r, 2 * r).contains(p)) return SelectionMode.LowerLeft;
        if (new Rectangle(x1 + widthAbs - r, y1 - r, 2 * r, 2 * r).contains(p)) return SelectionMode.UpperRight;
        if (new Rectangle(x1 + widthAbs - r, y1 + heightAbs - r, 2 * r, 2 * r).contains(p)) return SelectionMode.LowerRight;
        if (new Rectangle(x1 + widthAbs / 2 - r, y1 - r, 2 * r, 2 * r).contains(p)) return SelectionMode.MiddleTop;
        if (new Rectangle(x1 - r, y1 + heightAbs / 2 - r, 2 * r, 2 * r).contains(p)) return SelectionMode.MiddleLeft;
        if (new Rectangle(x1 + widthAbs - r, y1 + heightAbs / 2 - r, 2 * r, 2 * r).contains(p)) return SelectionMode.MiddleRight;
        if (new Rectangle(x1 + widthAbs / 2 - r, y1 + heightAbs - r, 2 * r, 2 * r).contains(p)) return SelectionMode.MiddleBottom;

        return SelectionMode.None; // No handle found at the point
    }


    // --- Helper to get the appropriate cursor for a specific handle ---
    private Cursor getCursorForHandle(SelectionMode handle) {
        switch (handle) {
            case UpperLeft: return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
            case LowerLeft: return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
            case UpperRight: return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
            case LowerRight: return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
            case MiddleTop: return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
            case MiddleBottom: return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
            case MiddleLeft: return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
            case MiddleRight: return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            case None: // If not on a handle
            default:
                ToolMode currentTool = appService.getToolMode();
                // Show MOVE cursor if Move tool active, otherwise default (or Hand if over shape body)
                if (currentTool == ToolMode.MOVE) return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
                // Hand cursor logic is handled in updateCursor based on shape hover
                return Cursor.getDefaultCursor();
        }
    }


    // --- KeyListener Methods ---
    @Override
    public void keyTyped(KeyEvent e) { } // Not used

    @Override
    public void keyPressed(KeyEvent e) {
        // Handle Delete key to remove selected shapes
        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            List<Shape> selected = appService.getSelectedShapes();
            if (!selected.isEmpty()) {
                // Create a copy to avoid ConcurrentModificationException if deleting modifies the list immediately
                List<Shape> shapesToDelete = new java.util.ArrayList<>(selected);
                for (Shape shape : shapesToDelete) {
                    appService.delete(shape); // Uses command wrapper, which handles undo/redo
                }
                // Let command listener update UI (repaint, property sheet)
            }
        }
        // Handle Undo/Redo shortcuts (Ctrl+Z, Ctrl+Y)
        else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z) {
            appService.undo(); // Calls CommandService.undo()
        } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Y) {
            appService.redo(); // Calls CommandService.redo()
        }
        // Handle Arrow Key Nudging for selected shapes
        else if (e.getKeyCode() >= KeyEvent.VK_LEFT && e.getKeyCode() <= KeyEvent.VK_DOWN) {
            List<Shape> selected = appService.getSelectedShapes();
            if (!selected.isEmpty()) { // Only nudge if shapes are selected
                int dx = 0, dy = 0;
                int nudgeAmount = e.isShiftDown() ? 10 : 1; // Nudge further if Shift is held
                // Determine direction based on arrow key
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:  dx = -nudgeAmount; break;
                    case KeyEvent.VK_RIGHT: dx = nudgeAmount;  break;
                    case KeyEvent.VK_UP:    dy = -nudgeAmount; break;
                    case KeyEvent.VK_DOWN:  dy = nudgeAmount;  break;
                }
                // If there's movement, create and execute a Move command
                if (dx != 0 || dy != 0) {
                    Point startNudge = new Point(0, 0); // Origin for delta calculation
                    Point endNudge = new Point(dx, dy); // End point representing the nudge vector
                    // Use the move command for nudge action to support undo
                    // The command wrapper will apply this delta to all selected shapes
                    appService.move(startNudge, endNudge); // Creates MoveCommand via wrapper
                    // Let Command listener handle repaint and property updates
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) { } // Not used
}