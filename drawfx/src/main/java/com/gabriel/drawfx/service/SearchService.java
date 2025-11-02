package com.gabriel.drawfx.service;

import com.gabriel.drawfx.SelectionMode;
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class SearchService {

    public void search(AppService appService, Point p, boolean singleSelection) {
        Drawing drawing = appService.getDrawing();
        if (drawing == null) return;

        List<Shape> shapes = drawing.getShapes();
        Shape foundShape = null;
        SelectionMode handleFound = SelectionMode.None;
        Shape primarySelected = appService.getSelectedShape();

        if (primarySelected != null && primarySelected.isSelected()) {
            handleFound = findHandleAtPoint(primarySelected, p);
            if (handleFound != SelectionMode.None) {
                primarySelected.setSelectionMode(handleFound);
                return;
            }
            primarySelected.setSelectionMode(SelectionMode.None);
        }

        for (int i = shapes.size() - 1; i >= 0; i--) {
            Shape shape = shapes.get(i);
            if (shape == null) continue;

            boolean isLineType = "Line".equals(shape.getClass().getSimpleName());
            boolean isTextType = "Text".equals(shape.getClass().getSimpleName());

            if (isPointInsideShape(shape, p, appService.getSearchRadius(), isLineType, isTextType)) {
                foundShape = shape;
                break;
            }
        }

        if (foundShape != null) {
            if (singleSelection) {
                if (!foundShape.isSelected()) {
                    appService.clearSelections();
                    foundShape.setSelected(true);
                    drawing.setSelectedShape(foundShape);
                } else {
                    drawing.setSelectedShape(foundShape);
                    foundShape.setSelectionMode(SelectionMode.None);
                }
            } else {
                if (foundShape.isSelected()) {
                    appService.unSelect(foundShape);
                } else {
                    foundShape.setSelected(true);
                    drawing.setSelectedShape(foundShape);
                }
            }
        } else {
            if (singleSelection) {
                appService.clearSelections();
            }
        }

        primarySelected = appService.getSelectedShape();
        if (primarySelected != null && handleFound == SelectionMode.None) {
            primarySelected.setSelectionMode(SelectionMode.None);
        }
    }

    private boolean isPointInsideShape(Shape shape, Point p, int searchRadius, boolean isLineType, boolean isTextType) {
        if (shape == null) return false;

        Point loc = shape.getLocation();
        if (loc == null) {
            System.err.println("Warning: Shape ID " + shape.getId() + " has null location in isPointInsideShape.");
            return false;
        }
        int w = shape.getWidth();
        int h = shape.getHeight();
        Rectangle bounds;

        if (isLineType) {
            Line2D line = new Line2D.Double(loc.x, loc.y, loc.x + w, loc.y + h);
            return line.ptSegDist(p) <= searchRadius;
        }

        if (isTextType) {
            FontMetrics fm = getFontMetrics(shape.getFont());
            int ascent = (fm != null) ? fm.getAscent() : (int) (Math.abs(h) * 0.8);
            int visualY = loc.y - ascent;
            int x1 = Math.min(loc.x, loc.x + w);
            int y1 = Math.min(visualY, visualY + h);
            int widthAbs = Math.abs(w);
            int heightAbs = Math.abs(h);
            bounds = new Rectangle(x1, y1, widthAbs, heightAbs);
        } else {
            int x1 = Math.min(loc.x, loc.x + w);
            int y1 = Math.min(loc.y, loc.y + h);
            int widthAbs = Math.abs(w);
            int heightAbs = Math.abs(h);
            bounds = new Rectangle(x1, y1, widthAbs, heightAbs);
        }

        bounds.grow(searchRadius / 2, searchRadius / 2);
        return bounds.contains(p);
    }

    private SelectionMode findHandleAtPoint(Shape shape, Point p) {
        Point loc = shape.getLocation();
        if (loc == null) {
            System.err.println("Warning: Shape ID " + shape.getId() + " has null location in findHandleAtPoint.");
            return SelectionMode.None;
        }
        int w = shape.getWidth();
        int h = shape.getHeight();
        int r = shape.getR();

        boolean isLineType = "Line".equals(shape.getClass().getSimpleName());
        boolean isTextType = "Text".equals(shape.getClass().getSimpleName());

        if (isLineType) {
            Rectangle startHandle = new Rectangle(loc.x - r, loc.y - r, 2 * r, 2 * r);
            Rectangle endHandle = new Rectangle(loc.x + w - r, loc.y + h - r, 2 * r, 2 * r);
            if (startHandle.contains(p)) return SelectionMode.UpperLeft;
            if (endHandle.contains(p)) return SelectionMode.LowerRight;
            return SelectionMode.None;
        }

        if (isTextType) {
            FontMetrics fm = getFontMetrics(shape.getFont());
            int ascent = (fm != null) ? fm.getAscent() : (int)(Math.abs(h) * 0.8);
            loc = new Point(loc.x, loc.y - ascent);
        }

        int x1 = Math.min(loc.x, loc.x + w);
        int y1 = Math.min(loc.y, loc.y + h);
        int widthAbs = Math.abs(w);
        int heightAbs = Math.abs(h);

        Rectangle ul = new Rectangle(x1 - r, y1 - r, 2*r, 2*r);
        Rectangle ll = new Rectangle(x1 - r, y1 + heightAbs - r, 2*r, 2*r);
        Rectangle ur = new Rectangle(x1 + widthAbs - r, y1 - r, 2*r, 2*r);
        Rectangle lr = new Rectangle(x1 + widthAbs - r, y1 + heightAbs - r, 2*r, 2*r);
        Rectangle mt = new Rectangle(x1 + widthAbs/2 - r, y1 - r, 2*r, 2*r);
        Rectangle ml = new Rectangle(x1 - r, y1 + heightAbs/2 - r, 2*r, 2*r);
        Rectangle mr = new Rectangle(x1 + widthAbs - r, y1 + heightAbs/2 - r, 2*r, 2*r);
        Rectangle mb = new Rectangle(x1 + widthAbs/2 - r, y1 + heightAbs - r, 2*r, 2*r);

        if (ul.contains(p)) return SelectionMode.UpperLeft;
        if (ll.contains(p)) return SelectionMode.LowerLeft;
        if (ur.contains(p)) return SelectionMode.UpperRight;
        if (lr.contains(p)) return SelectionMode.LowerRight;
        if (mt.contains(p)) return SelectionMode.MiddleTop;
        if (ml.contains(p)) return SelectionMode.MiddleLeft;
        if (mr.contains(p)) return SelectionMode.MiddleRight;
        if (mb.contains(p)) return SelectionMode.MiddleBottom;

        return SelectionMode.None;
    }

    private FontMetrics getFontMetrics(Font font) {
        Graphics tempG = null;
        try {
            Image volatileImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            tempG = volatileImage.getGraphics();
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
