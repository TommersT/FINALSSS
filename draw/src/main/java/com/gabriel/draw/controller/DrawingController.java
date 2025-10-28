package com.gabriel.draw.controller;

import com.gabriel.draw.component.PropertySheet;
import com.gabriel.draw.model.*;
import com.gabriel.draw.model.Rectangle;
import com.gabriel.draw.view.DrawingStatusPanel;
import com.gabriel.drawfx.DrawMode;
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
                String newText = JOptionPane.showInputDialog(
                    drawingView,
                    "Edit text:",
                    currentText
                );
                if (newText != null) {
                    selectedShape.setText(newText);
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

    @Override
    public void mousePressed(MouseEvent e) {
        if(appService.getDrawMode() == DrawMode.Idle) {
            start = e.getPoint();
            ShapeMode currentShapeMode = appService.getShapeMode();
            if(currentShapeMode == ShapeMode.Select) {
                appService.search(start, !e.isControlDown());
            }
            else {
                if(currentShape!=null){
                    currentShape.setSelected(false);
                }
                switch (currentShapeMode) {
                    case Line:
                        currentShape = new Line(start);
                        currentShape.setColor(appService.getColor());
                        currentShape.getRendererService().render(drawingView.getGraphics(), currentShape, false);
                        break;
                    case Rectangle:
                        currentShape = new Rectangle(start);
                        currentShape.setColor(appService.getColor());
                        currentShape.getRendererService().render(drawingView.getGraphics(), currentShape, false);
                        break;
                    case Text:
                        String textContent = JOptionPane.showInputDialog(
                            drawingView,
                            "Enter text:",
                            "Text Input",
                            JOptionPane.PLAIN_MESSAGE
                        );
                        if (textContent != null && !textContent.trim().isEmpty()) {
                            drawing.setText(textContent);
                            currentShape = new Text(start);
                            currentShape.setColor(appService.getColor());
                            currentShape.setText(textContent);
                            currentShape.setFont(appService.getFont());
                            currentShape.getRendererService().render(drawingView.getGraphics(), currentShape, true);
                            appService.setDrawMode(DrawMode.MousePressed);
                        } else {
                            return;
                        }
                        break;
                    case Ellipse:
                        currentShape = new Ellipse(start);
                        currentShape.setColor(appService.getColor());
                        currentShape.getRendererService().render(drawingView.getGraphics(), currentShape, false);
                        break;
                    case Image:
                        currentShape = new Picture(start, start, drawing.getImageFilename());
                        currentShape.setColor(appService.getColor());
                        currentShape.setThickness(appService.getThickness());
                }

/* if(currentShape!=null) {
                    currentShape.getRendererService().render(drawingView.getGraphics(), currentShape, false);
                }
  */          }
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
            }
            else if(currentShape != null) {
                currentShape.setText(drawing.getText());
                currentShape.setFont(drawing.getFont());
                Normalizer.normalize(currentShape);
                appService.create(currentShape);
            }
            appService.setDrawMode(DrawMode.Idle);
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
                Shape selectedShape = drawing.getSelectedShape();
                if(selectedShape != null){
                    java.awt.Rectangle oldBounds = null;
                    java.awt.Rectangle newBounds = null;
                    
                    if(selectedShape.getSelectionMode() == SelectionMode.None){
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
                                appService.move(shape, start, end);
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
                    else {
                        Point loc = selectedShape.getLocation();
                        int margin = 20;
                        oldBounds = new java.awt.Rectangle(loc.x - margin, loc.y - margin, 
                            selectedShape.getWidth() + 2 * margin, selectedShape.getHeight() + 2 * margin);
                        appService.scale(selectedShape, start, end);
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
        drawingStatusPanel.setPoint(e.getPoint());
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