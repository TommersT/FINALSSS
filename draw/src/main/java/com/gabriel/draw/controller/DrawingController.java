// tommerst/finalsss/FINALSSS-ab98287e5a251ea0f90f45adc2a537ebda4a1511/draw/src/main/java/com/gabriel/draw/controller/DrawingController.java
// Corrected imports and Rectangle usage, fixed status panel call
package com.gabriel.draw.controller;

import com.gabriel.draw.component.PropertySheet;
import com.gabriel.draw.model.*;
// Keep specific shape imports if needed
// import com.gabriel.draw.model.Rectangle;
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
    // Stores the original font size *before* a scale drag starts (for font scaling approach - NOT USED in this version)
    // private Map<Shape, Integer> originalFontSizes;
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
        // originalFontSizes = new HashMap<>(); // For font scaling approach
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

        // Ensure selection happens on click when in select mode
        if (appService.getToolMode() == ToolMode.SELECT && e.getClickCount() == 1) {
            // Perform hit detection (SearchService determines selectionMode if handle is hit)
            appService.search(e.getPoint(), !e.isControlDown()); // Handle single/multi-select
            updateStatusBarShape();
            if (propertySheet != null) propertySheet.populateTable(appService);
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
                            appService.setText(newText);
                        }
                        if (fontChanged) {
                            // PropertyEventListener should generate specific font commands
                            if (!newFont.getFamily().equals(oldFont.getFamily())) {
                                appService.setFontFamily(newFont.getFamily());
                            }
                            if (newFont.getStyle() != oldFont.getStyle()) {
                                appService.setFontStyle(newFont.getStyle());
                            }
                            if (newFont.getSize() != oldFont.getSize()) {
                                appService.setFontSize(newFont.getSize());
                            }
                        }

                        // Let renderer recalculate bounds on next repaint
                        // clickedShape.setWidth(0); // No longer needed if renderer calculates bounds
                        // clickedShape.setHeight(0);

                        // Repaint and update property sheet are essential AFTER changes
                        // The Command execution should trigger repaint via its listener
                        // But explicitly calling here ensures UI update if command pattern fails
                        drawingView.repaint();
                        if (propertySheet != null) {
                            // Ensure update happens after command processing
                            SwingUtilities.invokeLater(() -> propertySheet.populateTable(appService));
                        }
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
            // originalFontSizes.clear(); // For font scaling approach

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
                            originalLocations.put(shape, new Point(shape.getLocation()));
                            if (canScale) { // Only store size if scaling is possible
                                originalSizes.put(shape, new Dimension(shape.getWidth(), shape.getHeight()));
                                // if (shape instanceof Text) { // For font scaling approach
                                //    originalFontSizes.put(shape, shape.getFont().getSize());
                                // }
                            }
                        }
                    } else {
                        isDraggingForMoveOrScale = false; // Clicked on selected shape but wrong tool mode
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
                appService.clearSelections(); // Selection handled above, clear just in case
                updateStatusBarShape();
                if (propertySheet != null) propertySheet.populateTable(appService); // Show global props

                ShapeMode shapeToDraw = appService.getShapeMode();
                currentShape = null; // Reset preview shape

                // Create the appropriate shape instance (preview)
                switch (shapeToDraw) {
                    case Line:      currentShape = new Line(start); break;
                    case Rectangle: currentShape = new com.gabriel.draw.model.Rectangle(start); break; // Use full path to avoid ambiguity
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

                                // Pre-calculate bounds immediately after creation for initial rendering
                                // This requires a Graphics context. If view isn't visible yet, this might be tricky.
                                Graphics g = drawingView.getGraphics(); // Or use bufferGraphics if accessible and valid
                                if (g != null) {
                                    try {
                                        // Use FontMetrics to set initial width/height
                                        FontMetrics fm = g.getFontMetrics(currentShape.getFont());
                                        if (fm != null) {
                                            int textWidth = fm.stringWidth(currentShape.getText());
                                            int textHeight = fm.getAscent() + fm.getDescent();
                                            currentShape.setWidth(textWidth + 10); // Add padding
                                            currentShape.setHeight(textHeight + 5); // Add padding
                                        } else { // Fallback if FontMetrics fails
                                            currentShape.setWidth(Math.max(10, textContent.length() * 8));
                                            currentShape.setHeight(16);
                                        }
                                    } finally {
                                        g.dispose(); // Dispose graphics context if obtained directly
                                    }
                                } else {
                                    // Fallback if graphics not available - renderer will set later
                                    System.err.println("Warning: Could not get Graphics to pre-calculate text bounds.");
                                    // Set some initial non-zero size
                                    currentShape.setWidth(Math.max(10, textContent.length() * 8));
                                    currentShape.setHeight(16);
                                }

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
                        // Use correct Picture constructor
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
                    // Font/Text are handled specifically for Text shapes during creation
                    if (!(currentShape instanceof Text)) { // Apply global font if not text
                        currentShape.setFont(appService.getFont());
                    }
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

            java.awt.Rectangle repaintBounds = null; // <-- CORRECTED: Use java.awt.Rectangle
            int margin = 20; // Margin for repainting handles/stroke


            // --- Live Preview for Shape Creation ---
            if (currentToolMode == ToolMode.DRAW && currentShape != null) {
                // Calculate old bounds before updating
                Point locOld = currentShape.getLocation();
                int wOld = currentShape.getWidth();
                int hOld = currentShape.getHeight();
                int yOffsetOld = 0;

                // Special handling for Text's visual bounds based on ascent
                if (currentShape instanceof Text && currentShape.getFont() != null) {
                    FontMetrics fm = getFontMetrics(currentShape.getFont());
                    if (fm != null) {
                        yOffsetOld = -fm.getAscent(); // Visual top is above baseline
                    }
                }

                repaintBounds = new java.awt.Rectangle( // <-- CORRECTED: Use java.awt.Rectangle
                        locOld.x - margin,
                        locOld.y + yOffsetOld - margin, // Use visual top for Y
                        Math.abs(wOld) + 2 * margin, // Use absolute value for size safety
                        Math.abs(hOld) + 2 * margin);

                // Update the preview shape's size based on the drag
                AppService baseService = getUnderlyingService(); // Use helper
                baseService.scale(currentShape, end); // Use base service's scale for preview

                // Recalculate Text bounds after potential font change during creation (unlikely here but safe)
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
                int yOffsetNew = yOffsetOld; // Assume font hasn't changed during creation drag

                java.awt.Rectangle newBounds = new java.awt.Rectangle( // <-- CORRECTED: Use java.awt.Rectangle
                        locNew.x - margin,
                        locNew.y + yOffsetNew - margin, // Use visual top for Y
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

                // Use delta from *last* drag point for smooth preview update
                int dx = end.x - start.x;
                int dy = end.y - start.y;

                List<Shape> shapesToUpdate = appService.getSelectedShapes(); // Get all selected shapes

                AppService baseService = getUnderlyingService(); // Use helper


                for (Shape shape : shapesToUpdate) {
                    Point loc = shape.getLocation();
                    int w = shape.getWidth();
                    int h = shape.getHeight();
                    int yOffset = 0;

                    // Special handling for Text's visual bounds
                    if (shape instanceof Text && shape.getFont() != null) {
                        FontMetrics fm = getFontMetrics(shape.getFont());
                        if (fm != null) {
                            yOffset = -fm.getAscent(); // Visual top is above baseline
                        }
                    }

                    // Calculate old bounds for this shape (using visual top for Y)
                    java.awt.Rectangle oldShapeBounds = new java.awt.Rectangle( // <-- CORRECTED: Use java.awt.Rectangle
                            loc.x - margin, loc.y + yOffset - margin,
                            Math.abs(w) + 2 * margin, Math.abs(h) + 2 * margin);
                    if (repaintBounds == null) repaintBounds = oldShapeBounds;
                    else repaintBounds.add(oldShapeBounds);

                    // --- Apply transformation for LIVE PREVIEW ---
                    if (isMove) {
                        // Use base service's move method for preview (moves all selected)
                        baseService.move(shape, start, end); // Apply delta to each shape

                    } else if (isScale && shape == primarySelectedShape) { // Only scale primary interactively for preview
                        Point scaleEnd = end;
                        if (e.isShiftDown()) { // Apply aspect ratio constraint for preview
                            Dimension originalSize = originalSizes.get(shape);
                            if (originalSize != null) {
                                // NOTE: maintainAspectRatio needs original state, use dragStartPoint here
                                scaleEnd = maintainAspectRatio(shape, dragStartPoint, end);
                            }
                        }
                        // Use direct preview logic or a *non-command* scale method on baseService
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
                    // yOffset remains the same as font doesn't change during drag
                    java.awt.Rectangle newShapeBounds = new java.awt.Rectangle( // <-- CORRECTED: Use java.awt.Rectangle
                            newLoc.x - margin, newLoc.y + yOffset - margin,
                            Math.abs(newW) + 2 * margin, Math.abs(newH) + 2 * margin);
                    repaintBounds.add(newShapeBounds);
                } // End loop through shapes

                // Update property sheet live during drag
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
            Shape primarySelectedShape = appService.getSelectedShape(); // Get primary for scale check

            // --- Finalize Move or Scale ---
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
                List<Shape> shapesToRestore = new ArrayList<>(originalLocations.keySet()); // <-- CORRECTED: Use initialized ArrayList
                for (Shape shape : shapesToRestore) {
                    Point originalLoc = originalLocations.get(shape);
                    Dimension originalSize = originalSizes.get(shape);
                    // Integer originalFontSize = originalFontSizes.get(shape); // For font scaling

                    if (originalLoc != null) {
                        shape.setLocation(new Point(originalLoc)); // Restore location
                    }
                    if (wasScale && originalSize != null) { // Only restore size if it was a scale
                        shape.setWidth(originalSize.width);
                        shape.setHeight(originalSize.height);
                        // If scaling font:
                        // if (shape instanceof Text && originalFontSize != null) {
                        //     Font currentFont = shape.getFont();
                        //     shape.setFont(new Font(currentFont.getFamily(), currentFont.getStyle(), originalFontSize));
                        // }
                    }
                }
                // Repaint immediately after restoring to clear the preview state *before* command executes
                if (mouseMoved && drawingView != null) {
                    drawingView.repaint();
                }
                // <<<<< END RESTORE STATE >>>>>

                // Now create and execute the command if the mouse actually moved
                if (mouseMoved) {
                    if (wasMove) {
                        // Move command operates on all currently selected shapes implicitly
                        appService.move(dragStartPoint, end); // Creates MoveCommand via wrapper
                    } else if (wasScale && primarySelectedShape != null) {
                        // Apply final aspect ratio constraint if shift was held at release
                        Point scaleEnd = end;
                        if (e.isShiftDown()) {
                            Dimension originalSize = originalSizes.get(primarySelectedShape);
                            if (originalSize != null) {
                                scaleEnd = maintainAspectRatio(primarySelectedShape, dragStartPoint, end);
                            }
                        }
                        // Scale command operates only on the primary shape with the handle
                        appService.scale(primarySelectedShape, dragStartPoint, scaleEnd); // Creates ScaleCommand via wrapper
                    }
                }
            }
            // --- Finalize Shape Creation ---
            else if (currentToolMode == ToolMode.DRAW && currentShape != null) {
                AppService baseService = getUnderlyingService(); // Use helper
                baseService.scale(currentShape, end); // Set final size based on end point

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


                // Only normalize shapes that are NOT lines and NOT Text (bounds are calculated)
                boolean isLine = currentShape instanceof com.gabriel.draw.model.Line;
                boolean isText = currentShape instanceof com.gabriel.draw.model.Text;
                if (!isLine && !isText) {
                    Normalizer.normalize(currentShape); // Ensure width/height positive
                }


                boolean hasSize = Math.abs(currentShape.getWidth()) > 0 || Math.abs(currentShape.getHeight()) > 0;
                // Don't create zero-size shapes unless it's a line (which can have zero width/height visually)
                if (isLine || hasSize) {
                    appService.create(currentShape); // Creates AddShapeCommand via wrapper
                }
                currentShape = null; // Clear the preview shape
            }

            // --- Reset State ---
            appService.setDrawMode(DrawMode.Idle);
            isDraggingForMoveOrScale = false;
            dragStartPoint = null;
            originalLocations.clear();
            originalSizes.clear();
            // originalFontSizes.clear(); // For font scaling
            currentShape = null; // Ensure preview shape is cleared


            // --- Final UI Updates ---
            updateStatusBarShape(); // Update status bar after selection might change
            // CommandService listener will handle property sheet and repaint updates
        }
    }


    // --- Helper for applying scale PREVIEW logic ---
    // This duplicates logic from ScaleCommand but applies it directly for the drag preview
    private void applyScalePreview(Shape shape, Point previewStartUnused, Point previewEnd) {
        // We need the state from the beginning of the whole drag operation
        Dimension originalSize = originalSizes.get(shape);
        Point originalLoc = originalLocations.get(shape);
        // Integer originalFontSize = originalFontSizes.get(shape); // For font scaling

        if (originalSize == null || originalLoc == null || dragStartPoint == null) {
            System.err.println("applyScalePreview: Missing original state for shape ID " + shape.getId());
            return; // Cannot preview without original state
        }

        // Calculate the total delta from the absolute drag start to the current end point
        int totalDx = previewEnd.x - dragStartPoint.x;
        int totalDy = previewEnd.y - dragStartPoint.y;

        SelectionMode selMode = shape.getSelectionMode(); // Use the mode determined on press

        // Apply scaling based on handle and TOTAL delta from dragStartPoint, starting from original state
        Point newLoc = new Point(originalLoc);
        Dimension newSize = new Dimension(originalSize);

        // --- Standard Stretching Scaling Logic ---
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

        // --- Alternative: Font Size Scaling Logic (More complex) ---
        /* ... (font scaling logic omitted for now) ... */
        // --- End Font Size Scaling ---


        // Set the calculated preview state directly on the shape
        shape.setLocation(newLoc);
        shape.setWidth(newSize.width);
        shape.setHeight(newSize.height);

        // Important: Do NOT normalize during preview drag, normalize only on mouse release (if needed)
    }


    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) {
        if (drawingStatusPanel != null) {
            // Clear coordinates when mouse leaves? Or just stop updating?
            drawingStatusPanel.setPoint(new Point(-1, -1)); // <-- CORRECTED: Use setPoint
        }
    }


    @Override
    public void mouseMoved(MouseEvent e) {
        if (drawingStatusPanel != null) {
            drawingStatusPanel.setPoint(e.getPoint()); // <-- CORRECTED: Use setPoint
        }
        // Update cursor based on tool and hover state
        updateCursor(e.getPoint());
    }

    // --- Helper Methods ---

    // Helper to get underlying service
    private AppService getUnderlyingService() {
        if (appService instanceof DrawingCommandAppService) {
            return ((DrawingCommandAppService) appService).getUnderlyingAppService();
        }
        return appService; // Fallback
    }

    // Helper to get FontMetrics safely
    private FontMetrics getFontMetrics(Font font) {
        // Try getting from the view first
        if (drawingView != null) {
            Graphics g = drawingView.getGraphics();
            if (g != null) {
                try {
                    return g.getFontMetrics(font);
                } finally {
                    g.dispose();
                }
            }
        }
        // Fallback: create temporary graphics
        Graphics tempG = null;
        try {
            tempG = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
            if (tempG != null) {
                return tempG.getFontMetrics(font != null ? font : new Font("SansSerif", Font.PLAIN, 12));
            }
        } catch (Exception e) {
            System.err.println("Error getting FontMetrics via fallback: " + e.getMessage());
        } finally {
            if (tempG != null) tempG.dispose();
        }
        return null; // Should not happen
    }

    // Helper specifically for calculating Text bounds
    private Dimension calculateTextBounds(Graphics g, Text textShape) {
        Font font = textShape.getFont();
        String textContent = textShape.getText();
        if (font == null) font = new Font("SansSerif", Font.PLAIN, 12);
        if (textContent == null || textContent.isEmpty()) textContent = "Text";

        FontMetrics fm = g.getFontMetrics(font);
        if (fm == null) return new Dimension(10, 10); // Fallback

        int textWidth = fm.stringWidth(textContent);
        int textHeight = fm.getAscent() + fm.getDescent();
        return new Dimension(Math.max(10, textWidth + 10), Math.max(10, textHeight + 5));
    }


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
                drawingStatusPanel.setShapeName(null); // Clears to "No shape selected"
            }
        }
    }

    public void updateStatusBarTool(String toolName) {
        if (drawingStatusPanel != null) {
            drawingStatusPanel.setToolText(toolName);
        }
        // Update cursor immediately when tool changes
        updateCursor(null); // Pass null to set default cursor for the tool
    }

    // Maintain aspect ratio during scaling preview/finalization
    private Point maintainAspectRatio(Shape shape, Point anchorPoint, Point dragPoint) {
        // Get the *original* dimensions stored at the start of the drag
        Dimension originalSize = originalSizes.get(shape);
        Point originalLoc = originalLocations.get(shape); // Use original location as reference
        SelectionMode selMode = shape.getSelectionMode(); // Handle being dragged

        if (originalSize == null || originalLoc == null || originalSize.width == 0 || originalSize.height == 0) {
            return dragPoint; // Cannot maintain ratio if original size is unknown or zero
        }

        double aspectRatio = (double)originalSize.width / originalSize.height;

        // Determine the fixed corner/point opposite the handle being dragged (relative to original state)
        Point fixedPoint = new Point();
        switch (selMode) {
            case UpperLeft:  fixedPoint.setLocation(originalLoc.x + originalSize.width, originalLoc.y + originalSize.height); break;
            case LowerLeft:  fixedPoint.setLocation(originalLoc.x + originalSize.width, originalLoc.y); break;
            case UpperRight: fixedPoint.setLocation(originalLoc.x, originalLoc.y + originalSize.height); break;
            case LowerRight: fixedPoint.setLocation(originalLoc.x, originalLoc.y); break;
            // --- Aspect ratio for edge handles usually locks the perpendicular axis ---
            case MiddleLeft:
            case MiddleRight:
                return new Point(dragPoint.x, anchorPoint.y); // Lock Y, allow X change
            case MiddleTop:
            case MiddleBottom:
                return new Point(anchorPoint.x, dragPoint.y); // Lock X, allow Y change
            // --- End Edge Handle Logic ---
            default: return dragPoint; // Not a scaling handle or unknown
        }

        // Calculate deltas from the fixed point to the current drag point (for CORNER handles)
        int dx = dragPoint.x - fixedPoint.x;
        int dy = dragPoint.y - fixedPoint.y;

        // Adjust the smaller delta based on the aspect ratio and the larger delta
        if (Math.abs(dx) * originalSize.height > Math.abs(dy) * originalSize.width) {
            // Width change is dominant (or aspect ratio makes it so)
            dy = (int)Math.round(dx / aspectRatio * (dy < 0 ? -1 : 1)); // Adjust height proportionally
        } else {
            // Height change is dominant
            dx = (int)Math.round(dy * aspectRatio * (dx < 0 ? -1 : 1)); // Adjust width proportionally
        }

        // Return the adjusted point for corner handles
        return new Point(fixedPoint.x + dx, fixedPoint.y + dy);
    }

    // --- Helper to get shape at point (needed for double-click and cursor update) ---
    private Shape searchService_getShapeAtPoint(Point p) {
        // This simulates what SearchService does, iterating backward
        List<Shape> shapes = drawing.getShapes();
        int searchRadius = appService.getSearchRadius(); // Use configured radius

        for (int i = shapes.size() - 1; i >= 0; i--) {
            Shape shape = shapes.get(i);
            if (shape == null) continue;

            Point loc = shape.getLocation();
            int w = shape.getWidth();
            int h = shape.getHeight();
            java.awt.Rectangle bounds; // <-- CORRECTED: Use java.awt.Rectangle

            // Use visual bounds for Text hit detection
            if (shape instanceof Text) {
                FontMetrics fm = getFontMetrics(shape.getFont());
                int ascent = (fm != null) ? fm.getAscent() : (int)(h * 0.8);
                int x1 = Math.min(loc.x, loc.x + w);
                int y1 = Math.min(loc.y - ascent, loc.y - ascent + h);
                bounds = new java.awt.Rectangle(x1, y1, Math.abs(w), Math.abs(h)); // <-- CORRECTED
            } else if (shape instanceof Line) {
                // Use distance to line segment for Line hit detection
                Line2D line = new Line2D.Double(loc.x, loc.y, loc.x + w, loc.y + h);
                if (line.ptSegDist(p) <= searchRadius) {
                    return shape;
                }
                continue; // Skip bounds check for lines after distance check
            }
            else {
                // Standard bounds for Rect, Ellipse, Image
                // Normalize bounds for hit detection if width/height can be negative
                int x1 = Math.min(loc.x, loc.x + w);
                int y1 = Math.min(loc.y, loc.y + h);
                int widthAbs = Math.abs(w);
                int heightAbs = Math.abs(h);
                bounds = new java.awt.Rectangle(x1, y1, widthAbs, heightAbs); // <-- CORRECTED
            }

            // Inflate bounds slightly for easier clicking near edges
            bounds.grow(searchRadius / 2, searchRadius / 2); // <-- CORRECTED: Uses java.awt.Rectangle method

            if (bounds.contains(p)) { // <-- CORRECTED: Uses java.awt.Rectangle method
                return shape; // Found the top-most shape
            }
        }
        return null; // No shape found at this point
    }

    // --- Helper to update cursor ---
    private void updateCursor(Point mousePoint) {
        if (drawingView == null) return;

        Cursor newCursor = Cursor.getDefaultCursor(); // Default
        ToolMode currentTool = appService.getToolMode();
        Shape primarySelected = appService.getSelectedShape();

        if (mousePoint != null && (currentTool == ToolMode.SELECT || currentTool == ToolMode.SCALE)) {
            // Check for handle hover if a shape is selected
            if (primarySelected != null && primarySelected.isSelected()) {
                SelectionMode handle = searchService_getHandleAtPoint(primarySelected, mousePoint); // Need a helper for this
                newCursor = getCursorForHandle(handle); // Get resize cursor based on handle
            }
            // If not on a handle, check if hovering over any selectable shape
            if (newCursor == Cursor.getDefaultCursor() && currentTool == ToolMode.SELECT) {
                Shape hoveredShape = searchService_getShapeAtPoint(mousePoint);
                if (hoveredShape != null) {
                    newCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR); // Indicate selectable
                }
            }
        } else { // Set cursor based on the active tool itself
            switch (currentTool) {
                case DRAW: newCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); break;
                case MOVE: newCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR); break;
                case SCALE: // Default might be ok, or use specific if needed
                case SELECT: // Default arrow is usually fine
                default: break; // Keep default
            }
        }

        if (drawingView.getCursor() != newCursor) {
            drawingView.setCursor(newCursor);
        }
    }

    // --- Helper to find which handle is at a point --- (Needs SearchService logic)
    private SelectionMode searchService_getHandleAtPoint(Shape shape, Point p) {
        if (!shape.isSelected()) return SelectionMode.None; // Only check selected shapes

        Point loc = shape.getLocation();
        int w = shape.getWidth();
        int h = shape.getHeight();
        int r = shape.getR(); // Handle radius/half-size

        // Basic check for Line end handles
        if (shape instanceof Line) {
            // Note: Line loc/w/h represent start/deltaX/deltaY
            java.awt.Rectangle startHandle = new java.awt.Rectangle(loc.x - r, loc.y - r, 2 * r, 2 * r); // <-- CORRECTED
            java.awt.Rectangle endHandle = new java.awt.Rectangle(loc.x + w - r, loc.y + h - r, 2 * r, 2 * r); // <-- CORRECTED
            if (startHandle.contains(p)) return SelectionMode.UpperLeft; // Treat line start as UpperLeft handle
            if (endHandle.contains(p)) return SelectionMode.LowerRight; // Treat line end as LowerRight handle
            return SelectionMode.None;
        }

        // Adjust location for Text visual bounds
        if (shape instanceof Text) {
            FontMetrics fm = getFontMetrics(shape.getFont());
            int ascent = (fm != null) ? fm.getAscent() : (int)(h * 0.8);
            loc = new Point(loc.x, loc.y - ascent); // Use visual top-left for handle checks
        }

        // Normalize bounds for handle checks
        int x1 = Math.min(loc.x, loc.x + w);
        int y1 = Math.min(loc.y, loc.y + h);
        int widthAbs = Math.abs(w);
        int heightAbs = Math.abs(h);

        // Check handles using java.awt.Rectangle
        if (new java.awt.Rectangle(x1-r, y1-r, 2*r, 2*r).contains(p)) return SelectionMode.UpperLeft; // <-- CORRECTED
        if (new java.awt.Rectangle(x1-r, y1+heightAbs-r, 2*r, 2*r).contains(p)) return SelectionMode.LowerLeft; // <-- CORRECTED
        if (new java.awt.Rectangle(x1 + widthAbs -r, y1 -r, 2*r, 2*r).contains(p)) return SelectionMode.UpperRight; // <-- CORRECTED
        if (new java.awt.Rectangle(x1 + widthAbs -r, y1+heightAbs-r, 2*r, 2*r).contains(p)) return SelectionMode.LowerRight; // <-- CORRECTED
        if (new java.awt.Rectangle(x1 + widthAbs/2 -r, y1-r, 2*r, 2*r).contains(p)) return SelectionMode.MiddleTop; // <-- CORRECTED
        if (new java.awt.Rectangle(x1 -r, y1+heightAbs/2-r, 2*r, 2*r).contains(p)) return SelectionMode.MiddleLeft; // <-- CORRECTED
        if (new java.awt.Rectangle(x1 + widthAbs -r, y1+heightAbs/2-r, 2*r, 2*r).contains(p)) return SelectionMode.MiddleRight; // <-- CORRECTED
        if (new java.awt.Rectangle(x1 + widthAbs/2 -r, y1+heightAbs-r, 2*r, 2*r).contains(p)) return SelectionMode.MiddleBottom; // <-- CORRECTED

        return SelectionMode.None;
    }

    // --- Helper to get cursor for a handle ---
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
            case None:
            default:
                // If not on a handle, cursor depends on the active tool
                ToolMode currentTool = appService.getToolMode();
                if (currentTool == ToolMode.MOVE) return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
                // Could add HAND cursor if over selectable shape in SELECT mode here
                return Cursor.getDefaultCursor();
        }
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
                // Create a copy to avoid ConcurrentModificationException if service modifies list during iteration
                List<Shape> shapesToDelete = new java.util.ArrayList<>(selected); // <-- CORRECTED: Use java.util.ArrayList
                // Create and execute DeleteShapeCommand for each selected shape
                // Batching into a single "GroupDeleteCommand" might be better for undo
                boolean commandExecuted = false;
                for (Shape shape : shapesToDelete) {
                    appService.delete(shape); // Uses command wrapper
                    commandExecuted = true;
                }
                // Let command listener update UI if command was executed
                // if (commandExecuted) {
                //     updateStatusBarShape();
                //     if (propertySheet != null) propertySheet.populateTable(appService);
                //     if (drawingView != null) drawingView.repaint();
                // }
            }
        }
        // Handle Undo/Redo shortcuts
        else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z) {
            appService.undo();
        } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Y) {
            appService.redo();
        }
        // Handle Arrow Key Nudging
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

                    // Use the move command for nudge action to support undo
                    appService.move(startNudge, endNudge); // Creates MoveCommand via wrapper

                    // Let Command listener handle repaint and property updates
                }
            }
        }
    }


    @Override
    public void keyReleased(KeyEvent e) {
        // Could potentially check for Shift release during scaling drag to stop aspect ratio constraint
        // However, the constraint is checked again in mouseReleased, which is safer.
    }
}