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
import com.gabriel.draw.service.DrawingCommandAppService;
import lombok.Setter;

import javax.swing.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.awt.Rectangle;

public class DrawingController implements MouseListener, MouseMotionListener, KeyListener {

    private Point start;
    private Point end;
    private Point dragStartPoint = null;
    private boolean isDraggingForMoveOrScale = false;
    private Map<Shape, Point> originalLocations;
    private Map<Shape, Dimension> originalSizes;

    private final AppService appService;
    private final Drawing drawing;

    @Setter
    private DrawingView drawingView;

    @Setter
    private DrawingStatusPanel drawingStatusPanel;

    @Setter
    private PropertySheet propertySheet;

    private Shape currentShape = null;

    public Shape getPreviewShape() {
        if (appService.getToolMode() == ToolMode.DRAW && appService.getDrawMode() == DrawMode.MousePressed) {
            return currentShape;
        }
        return null;
    }

    public DrawingController(AppService appService, DrawingView drawingView) {
        this.appService = appService;
        this.drawing = appService.getDrawing();
        this.drawingView = drawingView;
        originalLocations = new HashMap<>();
        originalSizes = new HashMap<>();
    }

    public void setDrawingView(DrawingView drawingView) {
        this.drawingView = drawingView;
        if (this.drawingView != null) {
            this.drawingView.addMouseListener(this);
            this.drawingView.addMouseMotionListener(this);
            this.drawingView.addKeyListener(this);
        }
    }

    public void setDrawingStatusPanel(DrawingStatusPanel drawingStatusPanel) {
        this.drawingStatusPanel = drawingStatusPanel;
    }

    public void setPropertySheet(PropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (drawingView == null) return;
        drawingView.requestFocusInWindow();

        if (appService.getToolMode() == ToolMode.SELECT && e.getClickCount() == 1) {
            appService.search(e.getPoint(), !e.isControlDown());
            updateStatusBarShape();

            if (propertySheet != null) {
                SwingUtilities.invokeLater(() -> propertySheet.populateTable(appService));
            }
            drawingView.repaint();
        }

        if (e.getClickCount() == 2 && appService.getToolMode() == ToolMode.SELECT) {
            Shape clickedShape = searchService_getShapeAtPoint(e.getPoint());
            if (clickedShape instanceof Text) {
                appService.select(clickedShape);
                drawingView.repaint();

                String oldText = clickedShape.getText();
                Font oldFont = clickedShape.getFont();
                JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(drawingView);
                TextInputDialog dialog = TextInputDialog.showDialog(parentFrame, oldText, oldFont);

                if (dialog.isOkClicked()) {
                    String newText = dialog.getText();
                    Font newFont = dialog.getSelectedFont();
                    boolean textChanged = newText != null && !newText.equals(oldText);
                    boolean fontChanged = newFont != null && !newFont.equals(oldFont);

                    if (textChanged || fontChanged) {
                        if (textChanged) {
                            appService.setText(newText);
                        }
                        if (fontChanged) {
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
                    }
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (drawingView == null) return;
        drawingView.requestFocusInWindow();

        if (appService.getDrawMode() == DrawMode.Idle) {
            start = e.getPoint();
            appService.setDrawMode(DrawMode.MousePressed);

            dragStartPoint = null;
            isDraggingForMoveOrScale = false;
            originalLocations.clear();
            originalSizes.clear();

            ToolMode currentToolMode = appService.getToolMode();

            if (currentToolMode == ToolMode.SELECT || currentToolMode == ToolMode.MOVE || currentToolMode == ToolMode.SCALE) {
                appService.search(start, !e.isControlDown());

                List<Shape> selectedShapes = appService.getSelectedShapes();
                if (!selectedShapes.isEmpty()) {
                    dragStartPoint = new Point(start);

                    Shape primaryShape = appService.getSelectedShape();
                    boolean onHandle = primaryShape != null && primaryShape.getSelectionMode() != SelectionMode.None;
                    boolean canScale = (currentToolMode == ToolMode.SCALE || (currentToolMode == ToolMode.SELECT && onHandle));
                    boolean canMove = (currentToolMode == ToolMode.MOVE || (currentToolMode == ToolMode.SELECT && !onHandle));

                    if (canMove || canScale) {
                        isDraggingForMoveOrScale = true;
                        for (Shape shape : selectedShapes) {
                            if (shape.getLocation() != null) {
                                originalLocations.put(shape, new Point(shape.getLocation()));
                            } else {
                                System.err.println("Warning: Shape " + shape.getId() + " has null location in mousePressed.");
                            }
                            if (canScale) {
                                originalSizes.put(shape, new Dimension(shape.getWidth(), shape.getHeight()));
                            }
                        }
                    } else {
                        isDraggingForMoveOrScale = false;
                    }
                } else {
                    isDraggingForMoveOrScale = false;
                }

                updateStatusBarShape();

                if (propertySheet != null) {
                    SwingUtilities.invokeLater(() -> propertySheet.populateTable(appService));
                }
                drawingView.repaint();
            }
            else if (currentToolMode == ToolMode.DRAW) {
                appService.clearSelections();
                updateStatusBarShape();

                if (propertySheet != null) {
                    SwingUtilities.invokeLater(() -> propertySheet.populateTable(appService));
                }

                ShapeMode shapeToDraw = appService.getShapeMode();
                currentShape = null;

                switch (shapeToDraw) {
                    case Line:
                        currentShape = new Line(start);
                        break;
                    case Rectangle:
                        currentShape = new com.gabriel.draw.model.Rectangle(start);
                        break;
                    case Ellipse:
                        currentShape = new Ellipse(start);
                        break;
                    case Text:
                        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(drawingView);
                        TextInputDialog dialog = TextInputDialog.showDialog(parentFrame, appService.getText(), appService.getFont());
                        if (dialog.isOkClicked()) {
                            String textContent = dialog.getText();
                            if (textContent != null && !textContent.trim().isEmpty()) {
                                currentShape = new Text(start);
                                currentShape.setText(textContent);
                                currentShape.setFont(dialog.getSelectedFont());

                                Graphics g = drawingView.getGraphics();
                                if (g != null) {
                                    try {
                                        Dimension bounds = calculateTextBounds(g, (Text) currentShape);
                                        currentShape.setWidth(bounds.width);
                                        currentShape.setHeight(bounds.height);
                                    } finally {
                                        g.dispose();
                                    }
                                } else {
                                    System.err.println("Warning: Could not get Graphics to pre-calculate text bounds.");
                                    currentShape.setWidth(Math.max(10, textContent.length() * 8));
                                    currentShape.setHeight(16);
                                }
                            } else {
                                appService.setDrawMode(DrawMode.Idle);
                                return;
                            }
                        } else {
                            appService.setDrawMode(DrawMode.Idle);
                            return;
                        }
                        break;
                    case Image:
                        String imageFilename = appService.getImageFileename();
                        if (imageFilename == null || imageFilename.isEmpty()) {
                            appService.setImageFileename();
                            imageFilename = appService.getImageFileename();
                            if (imageFilename == null || imageFilename.isEmpty()) {
                                appService.setDrawMode(DrawMode.Idle);
                                return;
                            }
                        }
                        currentShape = new Picture(start, start, imageFilename);
                        break;
                    default:
                        if (shapeToDraw == ShapeMode.Select) {
                            System.err.println("Warning: ToolMode is DRAW but ShapeMode is Select. Resetting state.");
                            appService.setDrawMode(DrawMode.Idle);
                            return;
                        }
                        appService.setDrawMode(DrawMode.Idle);
                        return;
                }

                if (currentShape != null) {
                    currentShape.setColor(drawing.getColor());
                    currentShape.setThickness(drawing.getThickness());
                    currentShape.setFill(drawing.getFill());
                    currentShape.setStartColor(drawing.getStartColor());
                    currentShape.setEndColor(drawing.getEndColor());
                    currentShape.setUseGradient(drawing.isUseGradient());
                    currentShape.setVisible(true);
                    if (!(currentShape instanceof Text)) {
                        currentShape.setFont(drawing.getFont());
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
        if (appService.getDrawMode() == DrawMode.MousePressed) {
            end = e.getPoint();
            ToolMode currentToolMode = appService.getToolMode();
            Shape primarySelectedShape = appService.getSelectedShape();
            java.awt.Rectangle repaintBounds = null;
            int margin = 20;

            if (currentToolMode == ToolMode.DRAW && currentShape != null) {
                Point locOld = currentShape.getLocation();
                int wOld = currentShape.getWidth();
                int hOld = currentShape.getHeight();
                int yOffsetOld = calculateYOffset(currentShape);

                if (locOld != null) {
                    repaintBounds = new java.awt.Rectangle(
                            locOld.x - margin,
                            locOld.y + yOffsetOld - margin,
                            Math.abs(wOld) + 2 * margin,
                            Math.abs(hOld) + 2 * margin
                    );
                } else {
                    repaintBounds = drawingView != null ? drawingView.getBounds() : null;
                }

                AppService baseService = getUnderlyingService();
                baseService.scale(currentShape, end);

                if (currentShape instanceof Text) {
                    Graphics g = drawingView.getGraphics();
                    if (g != null) {
                        try {
                            Dimension bounds = calculateTextBounds(g, (Text) currentShape);
                            currentShape.setWidth(bounds.width);
                            currentShape.setHeight(bounds.height);
                        } finally {
                            g.dispose();
                        }
                    }
                }

                Point locNew = currentShape.getLocation();
                int wNew = currentShape.getWidth();
                int hNew = currentShape.getHeight();
                int yOffsetNew = calculateYOffset(currentShape);

                if (locNew != null) {
                    java.awt.Rectangle newBounds = new java.awt.Rectangle(
                            locNew.x - margin,
                            locNew.y + yOffsetNew - margin,
                            Math.abs(wNew) + 2 * margin,
                            Math.abs(hNew) + 2 * margin
                    );
                    if (repaintBounds != null) {
                        repaintBounds.add(newBounds);
                    } else {
                        repaintBounds = newBounds;
                    }
                } else if (repaintBounds == null) {
                    repaintBounds = drawingView != null ? drawingView.getBounds() : null;
                }
            }
            else if (isDraggingForMoveOrScale) {
                boolean isScale = (currentToolMode == ToolMode.SCALE) ||
                        (currentToolMode == ToolMode.SELECT && primarySelectedShape != null &&
                                primarySelectedShape.getSelectionMode() != SelectionMode.None);
                boolean isMove = (currentToolMode == ToolMode.MOVE) ||
                        (currentToolMode == ToolMode.SELECT && !isScale);

                int dx = end.x - start.x;
                int dy = end.y - start.y;

                List<Shape> shapesToUpdate = appService.getSelectedShapes();
                AppService baseService = getUnderlyingService();

                for (Shape shape : shapesToUpdate) {
                    Point loc = shape.getLocation();
                    int w = shape.getWidth();
                    int h = shape.getHeight();
                    int yOffset = calculateYOffset(shape);

                    if (loc != null) {
                        java.awt.Rectangle oldShapeBounds = new java.awt.Rectangle(
                                loc.x - margin,
                                loc.y + yOffset - margin,
                                Math.abs(w) + 2 * margin,
                                Math.abs(h) + 2 * margin
                        );
                        if (repaintBounds == null) repaintBounds = oldShapeBounds;
                        else repaintBounds.add(oldShapeBounds);
                    } else if (repaintBounds == null) {
                        repaintBounds = drawingView != null ? drawingView.getBounds() : null;
                    }

                    if (isMove) {
                        baseService.move(shape, start, end);
                    } else if (isScale && shape == primarySelectedShape) {
                        Point scaleEnd = end;
                        if (e.isShiftDown()) {
                            Dimension originalSize = originalSizes.get(shape);
                            if (originalSize != null) {
                                scaleEnd = maintainAspectRatio(shape, dragStartPoint, end);
                            }
                        }
                        applyScalePreview(shape, start, scaleEnd);
                    }

                    Point newLoc = shape.getLocation();
                    int newW = shape.getWidth();
                    int newH = shape.getHeight();
                    int newYOffset = calculateYOffset(shape);

                    if (newLoc != null) {
                        java.awt.Rectangle newShapeBounds = new java.awt.Rectangle(
                                newLoc.x - margin,
                                newLoc.y + newYOffset - margin,
                                Math.abs(newW) + 2 * margin,
                                Math.abs(newH) + 2 * margin
                        );
                        if (repaintBounds != null) repaintBounds.add(newShapeBounds);
                        else repaintBounds = newShapeBounds;
                    } else if (repaintBounds == null) {
                        repaintBounds = drawingView != null ? drawingView.getBounds() : null;
                    }
                }

                if (propertySheet != null && primarySelectedShape != null && primarySelectedShape.getLocation() != null) {
                    propertySheet.updateIntegerProperty("X Location", primarySelectedShape.getLocation().x);
                    propertySheet.updateIntegerProperty("Y Location", primarySelectedShape.getLocation().y);
                    propertySheet.updateIntegerProperty("Width", primarySelectedShape.getWidth());
                    propertySheet.updateIntegerProperty("Height", primarySelectedShape.getHeight());
                }

                start = new Point(end);
            }

            if (drawingView != null && repaintBounds != null) {
                repaintBounds.width = Math.max(1, repaintBounds.width);
                repaintBounds.height = Math.max(1, repaintBounds.height);
                drawingView.repaint(repaintBounds);
            } else if (drawingView != null) {
                drawingView.repaint();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (appService.getDrawMode() == DrawMode.MousePressed) {
            end = e.getPoint();
            ToolMode currentToolMode = appService.getToolMode();
            Shape primarySelectedShape = appService.getSelectedShape();

            if (isDraggingForMoveOrScale && dragStartPoint != null && !originalLocations.isEmpty()) {
                boolean wasScale = (currentToolMode == ToolMode.SCALE) ||
                        (currentToolMode == ToolMode.SELECT && primarySelectedShape != null &&
                                primarySelectedShape.getSelectionMode() != SelectionMode.None);
                boolean wasMove = (currentToolMode == ToolMode.MOVE) ||
                        (currentToolMode == ToolMode.SELECT && !wasScale);

                boolean mouseMoved = !end.equals(dragStartPoint);

                List<Shape> shapesToRestore = new ArrayList<>(originalLocations.keySet());
                for (Shape shape : shapesToRestore) {
                    Point originalLoc = originalLocations.get(shape);
                    Dimension originalSize = originalSizes.get(shape);
                    if (originalLoc != null) {
                        shape.setLocation(new Point(originalLoc));
                    }
                    if (wasScale && originalSize != null) {
                        shape.setWidth(originalSize.width);
                        shape.setHeight(originalSize.height);
                    }
                }

                if (mouseMoved && drawingView != null) {
                    drawingView.repaint();
                }

                if (mouseMoved) {
                    if (wasMove) {
                        appService.move(dragStartPoint, end);
                    } else if (wasScale && primarySelectedShape != null) {
                        Point scaleEnd = end;
                        if (e.isShiftDown()) {
                            Dimension originalSize = originalSizes.get(primarySelectedShape);
                            if (originalSize != null) {
                                scaleEnd = maintainAspectRatio(primarySelectedShape, dragStartPoint, end);
                            }
                        }
                        appService.scale(primarySelectedShape, dragStartPoint, scaleEnd);
                    }
                }
            }
            else if (currentToolMode == ToolMode.DRAW && currentShape != null) {
                AppService baseService = getUnderlyingService();
                baseService.scale(currentShape, end);

                if (currentShape instanceof Text) {
                    Graphics g = drawingView.getGraphics();
                    if (g != null) {
                        try {
                            Dimension bounds = calculateTextBounds(g, (Text) currentShape);
                            currentShape.setWidth(bounds.width);
                            currentShape.setHeight(bounds.height);
                        } finally {
                            g.dispose();
                        }
                    }
                }

                boolean isLine = currentShape instanceof com.gabriel.draw.model.Line;
                boolean isText = currentShape instanceof com.gabriel.draw.model.Text;
                if (!isLine && !isText) {
                    Normalizer.normalize(currentShape);
                }

                boolean hasSize = Math.abs(currentShape.getWidth()) > 0 || Math.abs(currentShape.getHeight()) > 0;

                if (isLine || hasSize) {
                    appService.create(currentShape);
                }

                currentShape = null;
            }

            appService.setDrawMode(DrawMode.Idle);
            isDraggingForMoveOrScale = false;
            dragStartPoint = null;
            originalLocations.clear();
            originalSizes.clear();
            currentShape = null;

            if (propertySheet != null) {
                SwingUtilities.invokeLater(() -> propertySheet.populateTable(appService));
            }

            updateStatusBarShape();
        }
    }

    private void applyScalePreview(Shape shape, Point previewStartUnused, Point previewEnd) {
        Dimension originalSize = originalSizes.get(shape);
        Point originalLoc = originalLocations.get(shape);

        if (originalSize == null || originalLoc == null || dragStartPoint == null) {
            System.err.println("applyScalePreview: Missing original state for shape ID " +
                    (shape != null ? shape.getId() : "null"));
            return;
        }

        int totalDx = previewEnd.x - dragStartPoint.x;
        int totalDy = previewEnd.y - dragStartPoint.y;

        SelectionMode selMode = shape.getSelectionMode();

        Point newLoc = new Point(originalLoc);
        Dimension newSize = new Dimension(originalSize);

        if (selMode == SelectionMode.UpperLeft) {
            newLoc.x = originalLoc.x + totalDx;
            newLoc.y = originalLoc.y + totalDy;
            newSize.width = originalSize.width - totalDx;
            newSize.height = originalSize.height - totalDy;
        } else if (selMode == SelectionMode.LowerLeft) {
            newLoc.x = originalLoc.x + totalDx;
            newSize.width = originalSize.width - totalDx;
            newSize.height = originalSize.height + totalDy;
        } else if (selMode == SelectionMode.UpperRight) {
            newLoc.y = originalLoc.y + totalDy;
            newSize.width = originalSize.width + totalDx;
            newSize.height = originalSize.height - totalDy;
        } else if (selMode == SelectionMode.LowerRight) {
            newSize.width = originalSize.width + totalDx;
            newSize.height = originalSize.height + totalDy;
        } else if (selMode == SelectionMode.MiddleRight) {
            newSize.width = originalSize.width + totalDx;
        } else if (selMode == SelectionMode.MiddleLeft) {
            newLoc.x = originalLoc.x + totalDx;
            newSize.width = originalSize.width - totalDx;
        } else if (selMode == SelectionMode.MiddleTop) {
            newLoc.y = originalLoc.y + totalDy;
            newSize.height = originalSize.height - totalDy;
        } else if (selMode == SelectionMode.MiddleBottom) {
            newSize.height = originalSize.height + totalDy;
        }

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

    private AppService getUnderlyingService() {
        if (appService instanceof DrawingCommandAppService) {
            return ((DrawingCommandAppService) appService).getUnderlyingAppService();
        }
        return appService;
    }

    private FontMetrics getFontMetrics(Font font) {
        if (drawingView != null) {
            Graphics g = drawingView.getGraphics();
            if (g != null) {
                try {
                    return g.getFontMetrics(font != null ? font : new Font("SansSerif", Font.PLAIN, 12));
                } finally {
                    g.dispose();
                }
            }
        }

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
        return null;
    }

    private Dimension calculateTextBounds(Graphics g, Text textShape) {
        Font font = textShape.getFont();
        String textContent = textShape.getText();

        if (font == null) font = new Font("SansSerif", Font.PLAIN, 12);
        if (textContent == null || textContent.isEmpty()) textContent = "Text";

        FontMetrics fm = g.getFontMetrics(font);
        if (fm == null) return new Dimension(10, 10);

        int textWidth = fm.stringWidth(textContent);
        int textHeight = fm.getAscent() + fm.getDescent();

        return new Dimension(Math.max(10, textWidth + 10), Math.max(10, textHeight + 5));
    }

    private int calculateYOffset(Shape shape) {
        if (shape instanceof Text && shape.getFont() != null) {
            FontMetrics fm = getFontMetrics(shape.getFont());
            if (fm != null) {
                return -fm.getAscent();
            }
        }
        return 0;
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
                drawingStatusPanel.setShapeName(null);
            }
        }
    }

    public void updateStatusBarTool(String toolName) {
        if (drawingStatusPanel != null) {
            drawingStatusPanel.setToolText(toolName);
        }
        updateCursor(null);
    }

    private Point maintainAspectRatio(Shape shape, Point anchorPoint, Point dragPoint) {
        Dimension originalSize = originalSizes.get(shape);
        Point originalLoc = originalLocations.get(shape);
        SelectionMode selMode = shape.getSelectionMode();

        if (originalSize == null || originalLoc == null || originalSize.width == 0 || originalSize.height == 0) {
            return dragPoint;
        }

        double aspectRatio = (double) originalSize.width / originalSize.height;

        Point fixedPoint = new Point();
        switch (selMode) {
            case UpperLeft:
                fixedPoint.setLocation(originalLoc.x + originalSize.width, originalLoc.y + originalSize.height);
                break;
            case LowerLeft:
                fixedPoint.setLocation(originalLoc.x + originalSize.width, originalLoc.y);
                break;
            case UpperRight:
                fixedPoint.setLocation(originalLoc.x, originalLoc.y + originalSize.height);
                break;
            case LowerRight:
                fixedPoint.setLocation(originalLoc.x, originalLoc.y);
                break;
            case MiddleLeft:
            case MiddleRight:
                return new Point(dragPoint.x, anchorPoint.y);
            case MiddleTop:
            case MiddleBottom:
                return new Point(anchorPoint.x, dragPoint.y);
            default:
                return dragPoint;
        }

        int dx = dragPoint.x - fixedPoint.x;
        int dy = dragPoint.y - fixedPoint.y;

        if (Math.abs(dx) * originalSize.height > Math.abs(dy) * originalSize.width) {
            dy = (int) Math.round(dx / aspectRatio * (dy < 0 ? -1 : 1));
        } else {
            dx = (int) Math.round(dy * aspectRatio * (dx < 0 ? -1 : 1));
        }

        return new Point(fixedPoint.x + dx, fixedPoint.y + dy);
    }

    private Shape searchService_getShapeAtPoint(Point p) {
        List<Shape> shapes = drawing.getShapes();
        int searchRadius = appService.getSearchRadius();

        for (int i = shapes.size() - 1; i >= 0; i--) {
            Shape shape = shapes.get(i);
            if (shape == null || !shape.isVisible()) continue;

            Point loc = shape.getLocation();
            int w = shape.getWidth();
            int h = shape.getHeight();

            if (loc == null) continue;

            java.awt.Rectangle bounds;

            if (shape instanceof Text) {
                int yOffset = calculateYOffset(shape);
                int x1 = Math.min(loc.x, loc.x + w);
                int y1 = Math.min(loc.y + yOffset, loc.y + yOffset + h);
                bounds = new java.awt.Rectangle(x1, y1, Math.abs(w), Math.abs(h));
            } else if (shape instanceof Line) {
                Line2D line = new Line2D.Double(loc.x, loc.y,loc.x + w, loc.y + h);
                if (line.ptSegDist(p) <= searchRadius + (shape.getThickness() / 2.0)) {
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
                case DRAW:
                    newCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
                    break;
                case MOVE:
                    newCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
                    break;
                case SCALE:
                case SELECT:
                default:
                    if (mousePoint != null && currentTool == ToolMode.SELECT) {
                        Shape hoveredShape = searchService_getShapeAtPoint(mousePoint);
                        if (hoveredShape != null) {
                            newCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                        }
                    }
                    break;
            }
        }

        if (drawingView.getCursor() != newCursor) {
            drawingView.setCursor(newCursor);
        }
    }

    private SelectionMode searchService_getHandleAtPoint(Shape shape, Point p) {
        if (!shape.isSelected() || !shape.isVisible()) return SelectionMode.None;

        Point loc = shape.getLocation();
        int w = shape.getWidth();
        int h = shape.getHeight();
        int r = shape.getR();

        if (loc == null) return SelectionMode.None;

        if (shape instanceof Line) {
            java.awt.Rectangle startHandle = new java.awt.Rectangle(loc.x - r, loc.y - r, 2 * r, 2 * r);
            java.awt.Rectangle endHandle = new java.awt.Rectangle(loc.x + w - r, loc.y + h - r, 2 * r, 2 * r);
            if (startHandle.contains(p)) return SelectionMode.UpperLeft;
            if (endHandle.contains(p)) return SelectionMode.LowerRight;
            return SelectionMode.None;
        }

        int yOffset = calculateYOffset(shape);
        loc = new Point(loc.x, loc.y + yOffset);

        int x1 = Math.min(loc.x, loc.x + w);
        int y1 = Math.min(loc.y, loc.y + h);
        int widthAbs = Math.abs(w);
        int heightAbs = Math.abs(h);

        if (new Rectangle(x1 - r, y1 - r, 2 * r, 2 * r).contains(p))
            return SelectionMode.UpperLeft;
        if (new Rectangle(x1 - r, y1 + heightAbs - r, 2 * r, 2 * r).contains(p))
            return SelectionMode.LowerLeft;
        if (new Rectangle(x1 + widthAbs - r, y1 - r, 2 * r, 2 * r).contains(p))
            return SelectionMode.UpperRight;
        if (new Rectangle(x1 + widthAbs - r, y1 + heightAbs - r, 2 * r, 2 * r).contains(p))
            return SelectionMode.LowerRight;
        if (new Rectangle(x1 + widthAbs / 2 - r, y1 - r, 2 * r, 2 * r).contains(p))
            return SelectionMode.MiddleTop;
        if (new Rectangle(x1 - r, y1 + heightAbs / 2 - r, 2 * r, 2 * r).contains(p))
            return SelectionMode.MiddleLeft;
        if (new Rectangle(x1 + widthAbs - r, y1 + heightAbs / 2 - r, 2 * r, 2 * r).contains(p))
            return SelectionMode.MiddleRight;
        if (new Rectangle(x1 + widthAbs / 2 - r, y1 + heightAbs - r, 2 * r, 2 * r).contains(p))
            return SelectionMode.MiddleBottom;

        return SelectionMode.None;
    }

    private Cursor getCursorForHandle(SelectionMode handle) {
        switch (handle) {
            case UpperLeft:
                return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
            case LowerLeft:
                return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
            case UpperRight:
                return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
            case LowerRight:
                return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
            case MiddleTop:
                return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
            case MiddleBottom:
                return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
            case MiddleLeft:
                return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
            case MiddleRight:
                return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            case None:
            default:
                ToolMode currentTool = appService.getToolMode();
                if (currentTool == ToolMode.MOVE)
                    return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
                return Cursor.getDefaultCursor();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            List<Shape> selected = appService.getSelectedShapes();
            if (!selected.isEmpty()) {
                List<Shape> shapesToDelete = new java.util.ArrayList<>(selected);
                for (Shape shape : shapesToDelete) {
                    appService.delete(shape);
                }
            }
        }
        else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z) {
            appService.undo();
        } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Y) {
            appService.redo();
        }
        else if (e.getKeyCode() >= KeyEvent.VK_LEFT && e.getKeyCode() <= KeyEvent.VK_DOWN) {
            List<Shape> selected = appService.getSelectedShapes();
            if (!selected.isEmpty()) {
                int dx = 0, dy = 0;
                int nudgeAmount = e.isShiftDown() ? 10 : 1;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        dx = -nudgeAmount;
                        break;
                    case KeyEvent.VK_RIGHT:
                        dx = nudgeAmount;
                        break;
                    case KeyEvent.VK_UP:
                        dy = -nudgeAmount;
                        break;
                    case KeyEvent.VK_DOWN:
                        dy = nudgeAmount;
                        break;
                }

                if (dx != 0 || dy != 0) {
                    Point startNudge = new Point(0, 0);
                    Point endNudge = new Point(dx, dy);
                    appService.move(startNudge, endNudge);
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) { }
}