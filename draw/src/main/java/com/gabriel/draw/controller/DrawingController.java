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

        // Ensure selection happens on click when in select mode
        if (appService.getToolMode() == ToolMode.SELECT && e.getClickCount() == 1) {
            // Perform hit detection (SearchService determines selectionMode if handle is hit)
            appService.search(e.getPoint(), !e.isControlDown()); // Handle single/multi-select
            updateStatusBarShape();
            // *** Update PropertySheet on selection change ***
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
                            originalLocations.put(shape, new Point(shape.getLocation()));
                            if (canScale) { // Only store size if scaling is possible
                                originalSizes.put(shape, new Dimension(shape.getWidth(), shape.getHeight()));
                            }
                        }
                    } else {
                        isDraggingForMoveOrScale = false; // Clicked on selected shape but wrong tool mode
                    }
                }
                // Update UI based on new selection state
                updateStatusBarShape();
                // *** Update PropertySheet on selection change ***
                if (propertySheet != null) propertySheet.populateTable(appService);
                drawingView.repaint(); // Repaint to show selection handles/changes

            }
            // --- Handling Shape Creation Initiation ---
            else if (currentToolMode == ToolMode.DRAW) {
                // Always clear selection when starting a new shape
                appService.clearSelections();
                updateStatusBarShape();
                // *** Update PropertySheet to show global properties ***
                if (propertySheet != null) propertySheet.populateTable(appService); // Show global props

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
                    currentShape.setColor(appService.getColor());
                    currentShape.setThickness(appService.getThickness());
                    currentShape.setFill(appService.getFill());
                    currentShape.setStartColor(appService.getStartColor());
                    currentShape.setEndColor(appService.getEndColor());
                    currentShape.setUseGradient(appService.isUseGradient());
                    currentShape.setVisible(true); // New shapes are visible
                    if (!(currentShape instanceof Text)) { // Apply global font if not text
                        currentShape.setFont(appService.getFont());
                    }
                } else {
                    appService.setDrawMode(DrawMode.Idle);
                }
            } else {
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

            java.awt.Rectangle repaintBounds = null; // Use java.awt.Rectangle
            int margin = 20; // Margin for repainting handles/stroke

            // --- Live Preview for Shape Creation ---
            if (currentToolMode == ToolMode.DRAW && currentShape != null) {
                // Calculate old bounds before updating
                Point locOld = currentShape.getLocation();
                int wOld = currentShape.getWidth();
                int hOld = currentShape.getHeight();
                int yOffsetOld = calculateYOffset(currentShape); // Use helper

                repaintBounds = new java.awt.Rectangle(
                        locOld.x - margin,
                        locOld.y + yOffsetOld - margin, // Use visual top for Y
                        Math.abs(wOld) + 2 * margin,
                        Math.abs(hOld) + 2 * margin);

                // Update the preview shape's size based on the drag
                AppService baseService = getUnderlyingService(); // Use helper
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

                java.awt.Rectangle newBounds = new java.awt.Rectangle(
                        locNew.x - margin,
                        locNew.y + yOffsetNew - margin, // Use visual top for Y
                        Math.abs(wNew) + 2 * margin,
                        Math.abs(hNew) + 2 * margin);
                repaintBounds.add(newBounds); // Combine old and new repaint areas

                // *** NO real-time property sheet update during CREATION drag ***

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
                    int yOffset = calculateYOffset(shape); // Use helper

                    // Calculate old bounds for this shape (using visual top for Y)
                    java.awt.Rectangle oldShapeBounds = new java.awt.Rectangle(
                            loc.x - margin, loc.y + yOffset - margin,
                            Math.abs(w) + 2 * margin, Math.abs(h) + 2 * margin);
                    if (repaintBounds == null) repaintBounds = oldShapeBounds;
                    else repaintBounds.add(oldShapeBounds);

                    // --- Apply transformation for LIVE PREVIEW ---
                    if (isMove) {
                        // Use base service's move method for preview (applies delta to each selected shape)
                        baseService.move(shape, start, end);

                    } else if (isScale && shape == primarySelectedShape) { // Only scale primary interactively for preview
                        Point scaleEnd = end;
                        if (e.isShiftDown()) { // Apply aspect ratio constraint for preview
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

                    java.awt.Rectangle newShapeBounds = new java.awt.Rectangle(
                            newLoc.x - margin, newLoc.y + newYOffset - margin,
                            Math.abs(newW) + 2 * margin, Math.abs(newH) + 2 * margin);
                    repaintBounds.add(newShapeBounds);
                } // End loop through shapes

                // *** Update property sheet live during drag ***
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

                // Only normalize shapes that are NOT lines and NOT Text
                boolean isLine = currentShape instanceof com.gabriel.draw.model.Line;
                boolean isText = currentShape instanceof com.gabriel.draw.model.Text;
                if (!isLine && !isText) {
                    Normalizer.normalize(currentShape); // Ensure width/height positive
                }

                boolean hasSize = Math.abs(currentShape.getWidth()) > 0 || Math.abs(currentShape.getHeight()) > 0;
                // Don't create zero-size shapes unless it's a line
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
            currentShape = null; // Ensure preview shape is cleared

            // *** Update PropertySheet on final state ***
            if (propertySheet != null) {
                // Defer update slightly to allow command execution/listener to finish first
                SwingUtilities.invokeLater(() -> propertySheet.populateTable(appService));
            }
            updateStatusBarShape(); // Update status bar after selection might change
            // CommandService listener will handle repaint updates after command execution
        }
    }


    // --- Helper for applying scale PREVIEW logic ---
    private void applyScalePreview(Shape shape, Point previewStartUnused, Point previewEnd) {
        // We need the state from the beginning of the whole drag operation
        Dimension originalSize = originalSizes.get(shape);
        Point originalLoc = originalLocations.get(shape);

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
            newSize.width = originalSize.width - totalDx;
            newSize.height = originalSize.height + totalDy;
        } else if(selMode == SelectionMode.UpperRight){
            newLoc.y = originalLoc.y + totalDy;
            newSize.width = originalSize.width + totalDx;
            newSize.height = originalSize.height - totalDy;
        } else if(selMode == SelectionMode.LowerRight){
            newSize.width = originalSize.width + totalDx;
            newSize.height = originalSize.height + totalDy;
        } else if(selMode == SelectionMode.MiddleRight){
            newSize.width = originalSize.width + totalDx;
        } else if(selMode == SelectionMode.MiddleLeft){
            newLoc.x = originalLoc.x + totalDx;
            newSize.width = originalSize.width - totalDx;
        } else if(selMode == SelectionMode.MiddleTop) {
            newLoc.y = originalLoc.y + totalDy;
            newSize.height = originalSize.height - totalDy;
        } else if(selMode == SelectionMode.MiddleBottom){
            newSize.height = originalSize.height + totalDy;
        }
        // --- End Scaling Logic ---

        // Set the calculated preview state directly on the shape
        shape.setLocation(newLoc);
        shape.setWidth(newSize.width);
        shape.setHeight(newSize.height);
    }


    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) {
        if (drawingStatusPanel != null) {
            drawingStatusPanel.setPoint(new Point(-1, -1));
        }
    }


    @Override
    public void mouseMoved(MouseEvent e) {
        if (drawingStatusPanel != null) {
            drawingStatusPanel.setPoint(e.getPoint());
        }
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
        // Add padding
        return new Dimension(Math.max(10, textWidth + 10), Math.max(10, textHeight + 5));
    }

    // Helper to calculate Y offset for visual top-left (especially for Text)
    private int calculateYOffset(Shape shape) {
        if (shape instanceof Text && shape.getFont() != null) {
            FontMetrics fm = getFontMetrics(shape.getFont());
            if (fm != null) {
                return -fm.getAscent(); // Visual top is above baseline
            }
        }
        return 0; // No offset for other shapes
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
        updateCursor(null); // Update cursor immediately when tool changes
    }

    // Maintain aspect ratio during scaling preview/finalization
    private Point maintainAspectRatio(Shape shape, Point anchorPoint, Point dragPoint) {
        Dimension originalSize = originalSizes.get(shape);
        Point originalLoc = originalLocations.get(shape);
        SelectionMode selMode = shape.getSelectionMode();

        if (originalSize == null || originalLoc == null || originalSize.width == 0 || originalSize.height == 0) {
            return dragPoint;
        }

        double aspectRatio = (double)originalSize.width / originalSize.height;

        Point fixedPoint = new Point();
        switch (selMode) {
            case UpperLeft:  fixedPoint.setLocation(originalLoc.x + originalSize.width, originalLoc.y + originalSize.height); break;
            case LowerLeft:  fixedPoint.setLocation(originalLoc.x + originalSize.width, originalLoc.y); break;
            case UpperRight: fixedPoint.setLocation(originalLoc.x, originalLoc.y + originalSize.height); break;
            case LowerRight: fixedPoint.setLocation(originalLoc.x, originalLoc.y); break;
            case MiddleLeft: case MiddleRight: return new Point(dragPoint.x, anchorPoint.y);
            case MiddleTop: case MiddleBottom: return new Point(anchorPoint.x, dragPoint.y);
            default: return dragPoint;
        }

        int dx = dragPoint.x - fixedPoint.x;
        int dy = dragPoint.y - fixedPoint.y;

        if (Math.abs(dx) * originalSize.height > Math.abs(dy) * originalSize.width) {
            dy = (int)Math.round(dx / aspectRatio * (dy < 0 ? -1 : 1));
        } else {
            dx = (int)Math.round(dy * aspectRatio * (dx < 0 ? -1 : 1));
        }
        return new Point(fixedPoint.x + dx, fixedPoint.y + dy);
    }

    // --- Helper to get shape at point ---
    private Shape searchService_getShapeAtPoint(Point p) {
        List<Shape> shapes = drawing.getShapes();
        int searchRadius = appService.getSearchRadius();

        for (int i = shapes.size() - 1; i >= 0; i--) {
            Shape shape = shapes.get(i);
            if (shape == null || !shape.isVisible()) continue; // Skip null or invisible shapes

            Point loc = shape.getLocation();
            int w = shape.getWidth();
            int h = shape.getHeight();
            java.awt.Rectangle bounds;

            if (shape instanceof Text) {
                int yOffset = calculateYOffset(shape);
                int x1 = Math.min(loc.x, loc.x + w);
                int y1 = Math.min(loc.y + yOffset, loc.y + yOffset + h);
                bounds = new java.awt.Rectangle(x1, y1, Math.abs(w), Math.abs(h));
            } else if (shape instanceof Line) {
                Line2D line = new Line2D.Double(loc.x, loc.y, loc.x + w, loc.y + h);
                if (line.ptSegDist(p) <= searchRadius + (shape.getThickness()/2.0)) { // Consider thickness
                    return shape;
                }
                continue;
            } else {
                int x1 = Math.min(loc.x, loc.x + w);
                int y1 = Math.min(loc.y, loc.y + h);
                bounds = new java.awt.Rectangle(x1, y1, Math.abs(w), Math.abs(h));
            }

            bounds.grow(searchRadius / 2, searchRadius / 2);
            if (bounds.contains(p)) {
                return shape;
            }
        }
        return null;
    }

    // --- Helper to update cursor ---
    private void updateCursor(Point mousePoint) {
        if (drawingView == null) return;

        Cursor newCursor = Cursor.getDefaultCursor();
        ToolMode currentTool = appService.getToolMode();
        Shape primarySelected = appService.getSelectedShape();

        if (mousePoint != null && (currentTool == ToolMode.SELECT || currentTool == ToolMode.SCALE)) {
            if (primarySelected != null && primarySelected.isSelected()) {
                SelectionMode handle = searchService_getHandleAtPoint(primarySelected, mousePoint);
                newCursor = getCursorForHandle(handle);
            }
            if (newCursor == Cursor.getDefaultCursor() && currentTool == ToolMode.SELECT) {
                Shape hoveredShape = searchService_getShapeAtPoint(mousePoint);
                if (hoveredShape != null) {
                    newCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                }
            }
        } else {
            switch (currentTool) {
                case DRAW: newCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); break;
                case MOVE: newCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR); break;
                case SCALE:
                case SELECT:
                default: break;
            }
        }

        if (drawingView.getCursor() != newCursor) {
            drawingView.setCursor(newCursor);
        }
    }

    // --- Helper to find which handle is at a point ---
    private SelectionMode searchService_getHandleAtPoint(Shape shape, Point p) {
        if (!shape.isSelected() || !shape.isVisible()) return SelectionMode.None; // Only check selected, visible

        Point loc = shape.getLocation();
        int w = shape.getWidth();
        int h = shape.getHeight();
        int r = shape.getR();

        if (shape instanceof Line) {
            java.awt.Rectangle startHandle = new java.awt.Rectangle(loc.x - r, loc.y - r, 2 * r, 2 * r);
            java.awt.Rectangle endHandle = new java.awt.Rectangle(loc.x + w - r, loc.y + h - r, 2 * r, 2 * r);
            if (startHandle.contains(p)) return SelectionMode.UpperLeft;
            if (endHandle.contains(p)) return SelectionMode.LowerRight;
            return SelectionMode.None;
        }

        int yOffset = calculateYOffset(shape); // Use helper for Text
        loc = new Point(loc.x, loc.y + yOffset); // Use visual top-left

        int x1 = Math.min(loc.x, loc.x + w);
        int y1 = Math.min(loc.y, loc.y + h);
        int widthAbs = Math.abs(w);
        int heightAbs = Math.abs(h);

        // Check handles using java.awt.Rectangle.contains()
        if (new Rectangle(x1 - r, y1 - r, 2 * r, 2 * r).contains(p)) return SelectionMode.UpperLeft;
        if (new Rectangle(x1 - r, y1 + heightAbs - r, 2 * r, 2 * r).contains(p)) return SelectionMode.LowerLeft;
        if (new Rectangle(x1 + widthAbs - r, y1 - r, 2 * r, 2 * r).contains(p)) return SelectionMode.UpperRight;
        if (new Rectangle(x1 + widthAbs - r, y1 + heightAbs - r, 2 * r, 2 * r).contains(p)) return SelectionMode.LowerRight;
        if (new Rectangle(x1 + widthAbs / 2 - r, y1 - r, 2 * r, 2 * r).contains(p)) return SelectionMode.MiddleTop;
        if (new Rectangle(x1 - r, y1 + heightAbs / 2 - r, 2 * r, 2 * r).contains(p)) return SelectionMode.MiddleLeft;
        if (new Rectangle(x1 + widthAbs - r, y1 + heightAbs / 2 - r, 2 * r, 2 * r).contains(p)) return SelectionMode.MiddleRight;
        if (new Rectangle(x1 + widthAbs / 2 - r, y1 + heightAbs - r, 2 * r, 2 * r).contains(p)) return SelectionMode.MiddleBottom;

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
                List<Shape> shapesToDelete = new java.util.ArrayList<>(selected);
                for (Shape shape : shapesToDelete) {
                    appService.delete(shape); // Uses command wrapper
                }
                // Let command listener update UI
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
    public void keyReleased(KeyEvent e) { }
}