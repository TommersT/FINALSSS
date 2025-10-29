package com.gabriel.drawfx.service;

// REMOVED: import com.gabriel.draw.model.Line;
// REMOVED: import com.gabriel.draw.model.Text;
import com.gabriel.drawfx.SelectionMode;
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape; // Use the base Shape from drawfx

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class SearchService {

    /**
     * Searches for shapes or handles at a given point and updates the selection state.
     *
     * @param appService      The application service providing access to the drawing and selection state.
     * @param p               The point where the mouse click occurred.
     * @param singleSelection True if it's a single selection click (no modifier like Ctrl held down),
     * false if it's potentially a multi-selection toggle (Ctrl held down).
     */
    public void search(AppService appService, Point p, boolean singleSelection) {
        Drawing drawing = appService.getDrawing();
        if (drawing == null) return;

        List<Shape> shapes = drawing.getShapes();
        Shape foundShape = null;
        SelectionMode handleFound = SelectionMode.None;
        Shape primarySelected = appService.getSelectedShape(); // Get current primary selection

        // --- 1. Check for Handle Hit on the primary selected shape first ---
        if (primarySelected != null && primarySelected.isSelected()) {
            handleFound = findHandleAtPoint(primarySelected, p);
            if (handleFound != SelectionMode.None) {
                // Clicked on a handle of the primary selected shape
                primarySelected.setSelectionMode(handleFound);
                return; // Found a handle, interaction is for scaling/resizing, stop searching further
            }
            // Click was near the primary selected shape but not on a handle, reset its mode
            primarySelected.setSelectionMode(SelectionMode.None);
        }

        // --- 2. Iterate BACKWARDS through shapes to find the top-most shape body ---
        for (int i = shapes.size() - 1; i >= 0; i--) {
            Shape shape = shapes.get(i);
            if (shape == null) continue;

            // Use instanceof to check for specific types without direct import of implementation classes
            boolean isLineType = "Line".equals(shape.getClass().getSimpleName()); // Check class name as a workaround
            boolean isTextType = "Text".equals(shape.getClass().getSimpleName()); // Check class name

            if (isPointInsideShape(shape, p, appService.getSearchRadius(), isLineType, isTextType)) { // Pass flags
                foundShape = shape;
                break; // Found the top-most shape under the cursor, stop searching
            }
        }

        // --- 3. Apply Selection Logic based on what was found ---
        if (foundShape != null) {
            // Clicked on a shape body
            if (singleSelection) { // No Ctrl key pressed
                if (!foundShape.isSelected()) {
                    // Clicked on a new, unselected shape
                    appService.clearSelections(); // Deselect all currently selected shapes
                    // appService.select(foundShape); // Select needs to add to list AND set primary
                    foundShape.setSelected(true); // Manually select
                    drawing.setSelectedShape(foundShape); // Set as primary
                } else {
                    // Clicked on an already selected shape (and not on a handle).
                    drawing.setSelectedShape(foundShape); // Ensure it's the primary for potential move
                    foundShape.setSelectionMode(SelectionMode.None); // Ensure handle mode is reset
                }
            } else { // Ctrl key IS pressed (multi-selection toggle)
                if (foundShape.isSelected()) {
                    // Ctrl-clicked an already selected shape -> deselect it
                    appService.unSelect(foundShape);
                } else {
                    // Ctrl-clicked a new shape -> add it to the selection
                    foundShape.setSelected(true);
                    drawing.setSelectedShape(foundShape); // Make newly added shape the primary
                }
            }
        } else {
            // Clicked on empty space
            if (singleSelection) {
                appService.clearSelections();
            }
        }

        // --- Final Check: Reset primary shape handle mode if no handle was clicked initially ---
        primarySelected = appService.getSelectedShape(); // Re-get primary as it might have changed
        if (primarySelected != null && handleFound == SelectionMode.None) {
            primarySelected.setSelectionMode(SelectionMode.None);
        }
    }

    /**
     * Checks if a point is visually inside the bounds or close to the edge of a shape.
     * Uses class name checks as a workaround for drawfx not knowing concrete types.
     *
     * @param shape        The shape to check.
     * @param p            The point to check.
     * @param searchRadius The tolerance radius for clicking near lines or edges.
     * @param isLineType   Flag indicating if the shape is a Line.
     * @param isTextType   Flag indicating if the shape is a Text.
     * @return True if the point is considered "inside" or "on" the shape.
     */
    private boolean isPointInsideShape(Shape shape, Point p, int searchRadius, boolean isLineType, boolean isTextType) {
        if (shape == null) return false;

        Point loc = shape.getLocation();
        // Check for null location before proceeding
        if (loc == null) {
            System.err.println("Warning: Shape ID " + shape.getId() + " has null location in isPointInsideShape.");
            return false;
        }
        int w = shape.getWidth();
        int h = shape.getHeight();
        Rectangle bounds;

        // Special handling for Line shapes (distance check)
        if (isLineType) {
            Line2D line = new Line2D.Double(loc.x, loc.y, loc.x + w, loc.y + h);
            return line.ptSegDist(p) <= searchRadius;
        }

        // Special handling for Text shapes (use visual bounds based on ascent)
        if (isTextType) {
            FontMetrics fm = getFontMetrics(shape.getFont()); // Use helper
            int ascent = (fm != null) ? fm.getAscent() : (int) (Math.abs(h) * 0.8); // Estimate ascent if needed
            int visualY = loc.y - ascent;
            int x1 = Math.min(loc.x, loc.x + w);
            int y1 = Math.min(visualY, visualY + h);
            int widthAbs = Math.abs(w);
            int heightAbs = Math.abs(h);
            bounds = new Rectangle(x1, y1, widthAbs, heightAbs);
        } else {
            // Standard bounding box for Rectangle, Ellipse, Image, etc.
            int x1 = Math.min(loc.x, loc.x + w);
            int y1 = Math.min(loc.y, loc.y + h);
            int widthAbs = Math.abs(w);
            int heightAbs = Math.abs(h);
            bounds = new Rectangle(x1, y1, widthAbs, heightAbs);
        }

        // Inflate the bounds slightly to make clicking near edges easier
        bounds.grow(searchRadius / 2, searchRadius / 2);

        return bounds.contains(p);
    }


    /**
     * Determines which selection handle (if any) is located at the given point for a shape.
     * Uses class name check for Line type.
     *
     * @param shape The shape whose handles are being checked.
     * @param p     The point to check.
     * @return The SelectionMode corresponding to the handle, or SelectionMode.None if no handle is at the point.
     */
    private SelectionMode findHandleAtPoint(Shape shape, Point p) {
        Point loc = shape.getLocation();
        // Check for null location before proceeding
        if (loc == null) {
            System.err.println("Warning: Shape ID " + shape.getId() + " has null location in findHandleAtPoint.");
            return SelectionMode.None;
        }
        int w = shape.getWidth();
        int h = shape.getHeight();
        int r = shape.getR(); // Handle radius/half-size

        boolean isLineType = "Line".equals(shape.getClass().getSimpleName()); // Check class name
        boolean isTextType = "Text".equals(shape.getClass().getSimpleName()); // Check class name

        // Special handling for Line end points
        if (isLineType) {
            Rectangle startHandle = new Rectangle(loc.x - r, loc.y - r, 2 * r, 2 * r);
            Rectangle endHandle = new Rectangle(loc.x + w - r, loc.y + h - r, 2 * r, 2 * r);
            if (startHandle.contains(p)) return SelectionMode.UpperLeft;
            if (endHandle.contains(p)) return SelectionMode.LowerRight;
            return SelectionMode.None;
        }

        // Standard 8 handles for other shapes
        // Adjust location for Text visual bounds before checking handles
        if (isTextType) {
            FontMetrics fm = getFontMetrics(shape.getFont());
            int ascent = (fm != null) ? fm.getAscent() : (int)(Math.abs(h) * 0.8);
            loc = new Point(loc.x, loc.y - ascent); // Use visual top-left for handle calculations
        }

        // Normalize dimensions and location for handle calculation
        int x1 = Math.min(loc.x, loc.x + w);
        int y1 = Math.min(loc.y, loc.y + h);
        int widthAbs = Math.abs(w);
        int heightAbs = Math.abs(h);

        // Define rectangles for each handle based on normalized bounds
        Rectangle ul = new Rectangle(x1 - r, y1 - r, 2*r, 2*r);
        Rectangle ll = new Rectangle(x1 - r, y1 + heightAbs - r, 2*r, 2*r);
        Rectangle ur = new Rectangle(x1 + widthAbs - r, y1 - r, 2*r, 2*r);
        Rectangle lr = new Rectangle(x1 + widthAbs - r, y1 + heightAbs - r, 2*r, 2*r);
        Rectangle mt = new Rectangle(x1 + widthAbs/2 - r, y1 - r, 2*r, 2*r);
        Rectangle ml = new Rectangle(x1 - r, y1 + heightAbs/2 - r, 2*r, 2*r);
        Rectangle mr = new Rectangle(x1 + widthAbs - r, y1 + heightAbs/2 - r, 2*r, 2*r);
        Rectangle mb = new Rectangle(x1 + widthAbs/2 - r, y1 + heightAbs - r, 2*r, 2*r);

        // Check handles
        if (ul.contains(p)) return SelectionMode.UpperLeft;
        if (ll.contains(p)) return SelectionMode.LowerLeft;
        if (ur.contains(p)) return SelectionMode.UpperRight;
        if (lr.contains(p)) return SelectionMode.LowerRight;
        if (mt.contains(p)) return SelectionMode.MiddleTop;
        if (ml.contains(p)) return SelectionMode.MiddleLeft;
        if (mr.contains(p)) return SelectionMode.MiddleRight;
        if (mb.contains(p)) return SelectionMode.MiddleBottom;

        return SelectionMode.None; // No handle found at the point
    }


    // --- Helper to get FontMetrics safely ---
    private FontMetrics getFontMetrics(Font font) {
        Graphics tempG = null;
        try {
            // Use a volatile image for potentially better performance
            Image volatileImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            tempG = volatileImage.getGraphics();
            // Or fallback to standard BufferedImage if the above causes issues
            // tempG = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();

            if (tempG != null) {
                return tempG.getFontMetrics(font != null ? font : new Font("SansSerif", Font.PLAIN, 12));
            }
        } catch (Exception e) {
            System.err.println("SearchService: Error getting FontMetrics via fallback: " + e.getMessage());
        } finally {
            if (tempG != null) tempG.dispose();
        }
        return null;
    }
}