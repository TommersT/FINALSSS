package com.gabriel.draw.controller;

import com.gabriel.draw.component.PropertySheet;
import com.gabriel.draw.model.*;
import com.gabriel.draw.model.Rectangle;
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
import lombok.Setter;

import javax.swing.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;

public class DrawingController  implements MouseListener, MouseMotionListener, KeyListener {
    Point start;
    private Point end;
    private Point dragStartPoint;
    private boolean isDraggingForMoveOrScale = false;
    private java.util.Map<Shape, Point> originalLocations;
    private java.util.Map<Shape, java.awt.Dimension> originalSizes;

    private final AppService appService;
    private final Drawing drawing;

    @Setter
    private DrawingView drawingView;

    @Setter
    private DrawingStatusPanel drawingStatusPanel;

    @Setter
    private PropertySheet propertySheet;

    // FIX: Manually add setters that Lombok was supposed to create
    public void setDrawingView(DrawingView drawingView) {
        this.drawingView = drawingView;
    }

    public void setDrawingStatusPanel(DrawingStatusPanel drawingStatusPanel) {
        this.drawingStatusPanel = drawingStatusPanel;
    }

    public void setPropertySheet(PropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }


    private Shape currentShape = null;

    public DrawingController(AppService appService, DrawingView drawingView){
        this.appService = appService;
        this.drawing = appService.getDrawing();
        this.drawingView = drawingView;
        drawingView.addMouseListener(this);
        drawingView.addMouseMotionListener(this);
    }
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && appService.getShapeMode() == ShapeMode.Select) {
            Shape selectedShape = drawing.getSelectedShape();
            if (selectedShape != null && selectedShape.getClass().getSimpleName().equals("Text")) {
                String currentText = selectedShape.getText();
                if (currentText == null) currentText = "";
                Font currentFont = selectedShape.getFont();
                if (currentFont == null) currentFont = new Font("SansSerif", Font.PLAIN, 12);
                
                JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(drawingView);
                TextInputDialog dialog = TextInputDialog.showDialog(parentFrame, currentText, currentFont);
                
                if (dialog.isOkClicked()) {
                    String newText = dialog.getText();
                    if (newText != null && !newText.trim().isEmpty()) {
                        selectedShape.setText(newText);
                        selectedShape.setFont(dialog.getSelectedFont());
                        selectedShape.setWidth(0);
                        selectedShape.setHeight(0);
                        drawingView.repaint();
                        if(propertySheet != null) {
                            propertySheet.populateTable(appService);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(appService.getDrawMode() == DrawMode.Idle) {
            start = e.getPoint();
            dragStartPoint = new Point(start);
            isDraggingForMoveOrScale = false;
            originalLocations = new java.util.HashMap<>();
            originalSizes = new java.util.HashMap<>();
            
            ShapeMode currentShapeMode = appService.getShapeMode();
            if(currentShapeMode == ShapeMode.Select) {
                appService.search(start, !e.isControlDown());
                List<Shape> shapes = drawing.getShapes();
                for(Shape shape : shapes) {
                    if (shape.isSelected()) {
                        originalLocations.put(shape, new Point(shape.getLocation()));
                        originalSizes.put(shape, new java.awt.Dimension(shape.getWidth(), shape.getHeight()));
                    }
                }
                updateStatusBarShape();
            }
            else {
                if(currentShape!=null){
                    currentShape.setSelected(false);
                }
                switch (currentShapeMode) {
                    case Line:
                        currentShape = new Line(start);
                        currentShape.setColor(appService.getColor());
                        currentShape.setThickness(appService.getThickness());
                        currentShape.setFill(appService.getFill());
                        break;
                    case Rectangle:
                        currentShape = new Rectangle(start);
                        currentShape.setColor(appService.getColor());
                        currentShape.setThickness(appService.getThickness());
                        currentShape.setFill(appService.getFill());
                        break;
                    case Text:
                        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(drawingView);
                        TextInputDialog dialog = TextInputDialog.showDialog(parentFrame, "", appService.getFont());
                        
                        if (dialog.isOkClicked()) {
                            String textContent = dialog.getText();
                            if (textContent != null && !textContent.trim().isEmpty()) {
                                Font selectedFont = dialog.getSelectedFont();
                                drawing.setText(textContent);
                                drawing.setFont(selectedFont);
                                currentShape = new Text(start);
                                currentShape.setColor(appService.getColor());
                                currentShape.setThickness(appService.getThickness());
                                currentShape.setFill(appService.getFill());
                                currentShape.setText(textContent);
                                currentShape.setFont(selectedFont);
                                appService.setDrawMode(DrawMode.MousePressed);
                            } else {
                                return;
                            }
                        } else {
                            return;
                        }
                        break;
                    case Ellipse:
                        currentShape = new Ellipse(start);
                        currentShape.setColor(appService.getColor());
                        currentShape.setThickness(appService.getThickness());
                        currentShape.setFill(appService.getFill());
                        break;
                    case Image:
                        currentShape = new Picture(start, start, drawing.getImageFilename());
                        currentShape.setColor(appService.getColor());
                        currentShape.setThickness(appService.getThickness());
                        currentShape.setFill(appService.getFill());
                        break;
                }
            }
            appService.setDrawMode(DrawMode.MousePressed);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        end = e.getPoint();
        if(appService.getDrawMode() == DrawMode.MousePressed) {
            if (appService.getShapeMode() == ShapeMode.Select) {
                Shape selectedShape = drawing.getSelectedShape();
                if (selectedShape != null) {
                    if (selectedShape.getSelectionMode() != SelectionMode.None){
                        Normalizer.normalize(selectedShape);
                    }
                }
                
                if (isDraggingForMoveOrScale && dragStartPoint != null && originalLocations != null) {
                    for (Shape shape : originalLocations.keySet()) {
                        shape.setLocation(new Point(originalLocations.get(shape)));
                        java.awt.Dimension size = originalSizes.get(shape);
                        if (size != null) {
                            shape.setWidth(size.width);
                            shape.setHeight(size.height);
                        }
                    }
                    
                    ToolMode toolMode = appService.getToolMode();
                    if (toolMode == ToolMode.MOVE || (toolMode == ToolMode.SELECT && selectedShape != null && selectedShape.getSelectionMode() == SelectionMode.None)) {
                        appService.move(dragStartPoint, end);
                    } else if (toolMode == ToolMode.SCALE || (toolMode == ToolMode.SELECT && selectedShape != null && selectedShape.getSelectionMode() != SelectionMode.None)) {
                        if (selectedShape != null) {
                            appService.scale(selectedShape, dragStartPoint, end);
                        }
                    }
                }
            }
            else if(currentShape != null) {
                currentShape.setText(drawing.getText());
                currentShape.setFont(drawing.getFont());
                Normalizer.normalize(currentShape);
                appService.create(currentShape);
            }
            appService.setDrawMode(DrawMode.Idle);
            isDraggingForMoveOrScale = false;
        }
        if(propertySheet != null) {
            propertySheet.populateTable(appService);
        }
        drawingView.repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if(appService.getDrawMode() == DrawMode.MousePressed) {
            end = e.getPoint();
            if(drawing.getShapeMode() == ShapeMode.Select){
                ToolMode toolMode = appService.getToolMode();
                Shape selectedShape = drawing.getSelectedShape();
                if(selectedShape != null){
                    isDraggingForMoveOrScale = true;
                    java.awt.Rectangle oldBounds = null;
                    java.awt.Rectangle newBounds = null;

                    if(toolMode == ToolMode.MOVE || (toolMode == ToolMode.SELECT && selectedShape.getSelectionMode() == SelectionMode.None)){
                        List<Shape> shapes = drawing.getShapes();
                        for(Shape shape : shapes) {
                            if (shape.isSelected()) {
                                Point loc = shape.getLocation();
                                int margin = 20;
                                if(oldBounds == null) {
                                    oldBounds = new java.awt.Rectangle(loc.x - margin, loc.y - margin,
                                        shape.getWidth() + 2 * margin, shape.getHeight() + 2 * margin);
                                } else {
                                    oldBounds.add(new java.awt.Rectangle(loc.x - margin, loc.y - margin,
                                        shape.getWidth() + 2 * margin, shape.getHeight() + 2 * margin));
                                }
                                
                                int dx = end.x - start.x;
                                int dy = end.y - start.y;
                                shape.getLocation().x += dx;
                                shape.getLocation().y += dy;
                                
                                loc = shape.getLocation();
                                if(newBounds == null) {
                                    newBounds = new java.awt.Rectangle(loc.x - margin, loc.y - margin,
                                        shape.getWidth() + 2 * margin, shape.getHeight() + 2 * margin);
                                } else {
                                    newBounds.add(new java.awt.Rectangle(loc.x - margin, loc.y - margin,
                                        shape.getWidth() + 2 * margin, shape.getHeight() + 2 * margin));
                                }
                            }
                        }
                    }
                    else if(toolMode == ToolMode.SCALE || (toolMode == ToolMode.SELECT && selectedShape.getSelectionMode() != SelectionMode.None)) {
                        Point loc = selectedShape.getLocation();
                        int margin = 20;
                        oldBounds = new java.awt.Rectangle(loc.x - margin, loc.y - margin,
                            selectedShape.getWidth() + 2 * margin, selectedShape.getHeight() + 2 * margin);
                        
                        Point scaleEnd = end;
                        if (e.isShiftDown()) {
                            scaleEnd = maintainAspectRatio(selectedShape, start, end);
                        }
                        
                        int dx = scaleEnd.x - start.x;
                        int dy = scaleEnd.y - start.y;
                        int height = selectedShape.getHeight();
                        int width = selectedShape.getWidth();
                        if(selectedShape.getSelectionMode() == SelectionMode.UpperLeft) {
                            selectedShape.getLocation().x += dx;
                            selectedShape.getLocation().y += dy;
                            selectedShape.setWidth(width - dx);
                            selectedShape.setHeight(height - dy);
                        } else if(selectedShape.getSelectionMode() == SelectionMode.LowerLeft) {
                            selectedShape.getLocation().x += dx;
                            selectedShape.setWidth(width - dx);
                            selectedShape.setHeight(height + dy);
                        } else if(selectedShape.getSelectionMode() == SelectionMode.UpperRight){
                            selectedShape.getLocation().y += dy;
                            selectedShape.setWidth(width + dx);
                            selectedShape.setHeight(height - dy);
                        } else if(selectedShape.getSelectionMode() == SelectionMode.LowerRight){
                            selectedShape.setWidth(width + dx);
                            selectedShape.setHeight(height + dy);
                        } else if(selectedShape.getSelectionMode() == SelectionMode.MiddleRight){
                            selectedShape.setWidth(width + dx);
                        } else if(selectedShape.getSelectionMode() == SelectionMode.MiddleLeft){
                            selectedShape.setWidth(width - dx);
                            selectedShape.getLocation().x += dx;
                        } else if(selectedShape.getSelectionMode() == SelectionMode.MiddleTop) {
                            selectedShape.setHeight(height - dy);
                            selectedShape.getLocation().y += dy;
                        } else if(selectedShape.getSelectionMode() == SelectionMode.MiddleBottom){
                            selectedShape.setHeight(height + dy);
                        }
                        
                        loc = selectedShape.getLocation();
                        newBounds = new java.awt.Rectangle(loc.x - margin, loc.y - margin,
                            selectedShape.getWidth() + 2 * margin, selectedShape.getHeight() + 2 * margin);
                    }

                    if(oldBounds != null) {
                        drawingView.repaint(oldBounds);
                    }
                    if(newBounds != null) {
                        drawingView.repaint(newBounds);
                    }

                    if(propertySheet != null) {
                        propertySheet.populateTable(appService);
                    }
                }
                start = end;

            }
            else {
                if(currentShape != null) {
                    Point loc = currentShape.getLocation();
                    int margin = 20;
                    java.awt.Rectangle oldBounds = new java.awt.Rectangle(loc.x - margin, loc.y - margin,
                        currentShape.getWidth() + 2 * margin, currentShape.getHeight() + 2 * margin);
                    appService.scale(currentShape, end);
                    loc = currentShape.getLocation();
                    java.awt.Rectangle newBounds = new java.awt.Rectangle(loc.x - margin, loc.y - margin,
                        currentShape.getWidth() + 2 * margin, currentShape.getHeight() + 2 * margin);
                    drawingView.repaint(oldBounds);
                    drawingView.repaint(newBounds);
                }
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (drawingStatusPanel != null) {
            drawingStatusPanel.setPoint(e.getPoint());
        }
    }
    
    private void updateStatusBarShape() {
        if (drawingStatusPanel != null) {
            List<Shape> selectedShapes = appService.getSelectedShapes();
            int count = selectedShapes.size();
            if (count > 0) {
                if (count == 1) {
                    String shapeName = selectedShapes.get(0).getClass().getSimpleName();
                    drawingStatusPanel.setShapeName(shapeName);
                } else {
                    drawingStatusPanel.setShapeInfo(count + " shapes selected");
                }
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
    
    private Point maintainAspectRatio(Shape shape, Point start, Point end) {
        int originalWidth = shape.getWidth();
        int originalHeight = shape.getHeight();
        
        if (originalWidth == 0 || originalHeight == 0) {
            return end;
        }
        
        double aspectRatio = (double) originalWidth / originalHeight;
        
        int dx = end.x - start.x;
        int dy = end.y - start.y;
        
        int signX = dx >= 0 ? 1 : -1;
        int signY = dy >= 0 ? 1 : -1;
        
        int absDx = Math.abs(dx);
        int absDy = Math.abs(dy);
        
        if (absDx > absDy) {
            absDy = (int) (absDx / aspectRatio);
        } else {
            absDx = (int) (absDy * aspectRatio);
        }
        
        dx = absDx * signX;
        dy = absDy * signY;
        
        return new Point(start.x + dx, start.y + dy);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
    }


    @Override
    public void keyReleased(KeyEvent e) {

    }
}