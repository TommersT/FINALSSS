package com.gabriel.draw.service;

import com.gabriel.draw.view.DrawingView;
import com.gabriel.drawfx.service.SearchService;
import com.gabriel.drawfx.DrawMode;
import com.gabriel.drawfx.SelectionMode;
import com.gabriel.drawfx.ShapeMode;
import com.gabriel.drawfx.ToolMode;
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.*;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.util.Objects;

public class DrawingAppService implements AppService {

    final private Drawing drawing;
    private ToolMode toolMode = ToolMode.SELECT;

    @Setter
    private DrawingView drawingView;

    private ImageFileService imageFileService;
    private MoverService moverService;
    private ScalerService scalerService;
    private SearchService searchService;
    private XmlDocumentService xmlDocumentService;

    public DrawingAppService() {
        drawing = new Drawing();
        moverService = new MoverService();
        scalerService = new ScalerService();
        searchService = new SearchService();
        xmlDocumentService = new XmlDocumentService(drawing);
        imageFileService = new ImageFileService();
    }

    @Override
    public void undo() {
        System.out.println("Warning: undo() called on base DrawingAppService. Should be handled by wrapper.");
    }

    @Override
    public void redo() {
        System.out.println("Warning: redo() called on base DrawingAppService. Should be handled by wrapper.");
    }

    @Override
    public ShapeMode getShapeMode() {
        return drawing.getShapeMode();
    }

    @Override
    public void setShapeMode(ShapeMode shapeMode) {
        drawing.setShapeMode(shapeMode);
        triggerRepaint();
    }

    @Override
    public ToolMode getToolMode() {
        return toolMode;
    }

    @Override
    public void setToolMode(ToolMode toolMode) {
        if (this.toolMode != toolMode) {
            this.toolMode = toolMode;
            triggerRepaint();
        }
    }

    @Override
    public DrawMode getDrawMode() {
        return drawing.getDrawMode();
    }

    @Override
    public void setDrawMode(DrawMode drawMode) {
        this.drawing.setDrawMode(drawMode);
        triggerRepaint();
    }

    @Override
    public Color getColor() {
        Shape selectedShape = getSelectedShape();
        return (selectedShape != null) ? selectedShape.getColor() : drawing.getColor();
    }

    @Override
    public void setColor(Color color) {
        if (color == null) return;

        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                shape.setColor(color);
            }
        } else {
            drawing.setColor(color);
        }
        triggerRepaint();
    }

    @Override
    public Color getFill() {
        Shape selectedShape = getSelectedShape();
        return (selectedShape != null) ? selectedShape.getFill() : drawing.getFill();
    }

    @Override
    public void setFill(Color color) {
        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                shape.setFill(color);
            }
        } else {
            drawing.setFill(color);
        }
        triggerRepaint();
    }

    @Override
    public Color getStartColor() {
        Shape selectedShape = getSelectedShape();
        return (selectedShape != null) ? selectedShape.getStartColor() : drawing.getStartColor();
    }

    @Override
    public void setStartColor(Color color) {
        if (color == null) color = Color.LIGHT_GRAY;
        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                shape.setStartColor(color);
            }
        } else {
            drawing.setStartColor(color);
        }
        triggerRepaint();
    }

    @Override
    public Color getEndColor() {
        Shape selectedShape = getSelectedShape();
        return (selectedShape != null) ? selectedShape.getEndColor() : drawing.getEndColor();
    }

    @Override
    public void setEndColor(Color color) {
        if (color == null) color = Color.DARK_GRAY;
        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                shape.setEndColor(color);
            }
        } else {
            drawing.setEndColor(color);
        }
        triggerRepaint();
    }

    @Override
    public boolean isUseGradient() {
        Shape selectedShape = getSelectedShape();
        return (selectedShape != null) ? selectedShape.isUseGradient() : drawing.isUseGradient();
    }

    @Override
    public void setUseGradient(boolean useGradient) {
        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                shape.setUseGradient(useGradient);
            }
        } else {
            drawing.setUseGradient(useGradient);
        }
        triggerRepaint();
    }

    @Override
    public boolean isVisible() {
        Shape selectedShape = getSelectedShape();
        return (selectedShape != null) ? selectedShape.isVisible() : true;
    }

    @Override
    public void setVisible(boolean visible) {
        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                shape.setVisible(visible);
            }
        }
        triggerRepaint();
    }

    @Override
    public void setThickness(int thickness) {
        if (thickness < 1) thickness = 1;

        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                shape.setThickness(thickness);
            }
        } else {
            drawing.setThickness(thickness);
        }
        triggerRepaint();
    }

    @Override
    public int getThickness() {
        Shape selectedShape = getSelectedShape();
        return (selectedShape != null) ? selectedShape.getThickness() : drawing.getThickness();
    }

    @Override
    public void move(Shape shape, Point start, Point newLoc) {
        moverService.move(shape, start, newLoc);
    }

    @Override
    public void move(Point start, Point newLoc) {
        moverService.move(drawing, start, newLoc);
        triggerRepaint();
    }

    @Override
    public void scale(Point start, Point end) {
        scalerService.scale(drawing, start, end);
        triggerRepaint();
    }

    @Override
    public void scale(Shape shape, Point start, Point end) {
        scalerService.scale(shape, start, end);
        triggerRepaint();
    }

    @Override
    public void scale(Shape shape, Point end) {
        scalerService.scale(shape, end);
    }

    @Override
    public void create(Shape shape) {
        if (shape == null) return;
        shape.setColor(drawing.getColor());
        shape.setThickness(drawing.getThickness());
        shape.setFill(drawing.getFill());
        shape.setStartColor(drawing.getStartColor());
        shape.setEndColor(drawing.getEndColor());
        shape.setUseGradient(drawing.isUseGradient());
        shape.setVisible(true);
        if (shape.getFont() == null) shape.setFont(drawing.getFont());
        if (shape.getText() == null) shape.setText(drawing.getText());

        shape.setR(drawing.getSearchRadius());
        shape.setId(this.drawing.getShapes().size() + 1);
        this.drawing.getShapes().add(shape);
        triggerRepaint();
    }

    @Override
    public void insertAt(Shape shape, int index) {
        if (shape == null || index < 0) return;
        List<Shape> shapes = drawing.getShapes();
        if (index > shapes.size()) index = shapes.size();
        shapes.add(index, shape);
        triggerRepaint();
    }

    @Override
    public void delete(Shape shape) {
        if (shape == null) return;
        boolean removed = drawing.getShapes().remove(shape);
        if (removed) {
            if (shape.equals(drawing.getSelectedShape())) {
                drawing.setSelectedShape(null);
            }
            triggerRepaint();
        }
    }

    @Override
    public void close() {
        System.exit(0);
    }

    @Override
    public Drawing getDrawing() {
        return drawing;
    }

    @Override
    public void setDrawing(Drawing newDrawing) {
        if (newDrawing != null) {
            drawing.getShapes().clear();
            drawing.getShapes().addAll(newDrawing.getShapes());
            drawing.setColor(newDrawing.getColor());
            drawing.setFill(newDrawing.getFill());
            drawing.setThickness(newDrawing.getThickness());
            drawing.setFont(newDrawing.getFont());
            drawing.setText(newDrawing.getText());
            drawing.setFilename(newDrawing.getFilename());
            drawing.setImageFilename(newDrawing.getImageFilename());
            drawing.setStartColor(newDrawing.getStartColor());
            drawing.setEndColor(newDrawing.getEndColor());
            drawing.setUseGradient(newDrawing.isUseGradient());

            clearSelections();
            drawing.setSelectedShape(null);
            drawing.setDrawMode(DrawMode.Idle);
            setToolMode(ToolMode.SELECT);
            setShapeMode(ShapeMode.Select);
        }
        triggerRepaint();
    }

    @Override
    public int getSearchRadius() {
        return drawing.getSearchRadius();
    }

    @Override
    public void setSearchRadius(int radius) {
        drawing.setSearchRadius(Math.max(1, radius));
    }

    @Override
    public void search(Point p) {
        searchService.search(this, p, true);
        triggerRepaint();
    }

    @Override
    public void search(Point p, boolean single) {
        searchService.search(this, p, single);
        triggerRepaint();
    }

    @Override
    public void open(String filename) {
        xmlDocumentService.open(filename);
        clearSelections();
        drawing.setSelectedShape(null);
        drawing.setDrawMode(DrawMode.Idle);
        setToolMode(ToolMode.SELECT);
        setShapeMode(ShapeMode.Select);
        triggerRepaint();
    }

    @Override
    public void save() {
        String currentFilename = drawing.getFilename();
        if (currentFilename != null && !currentFilename.isEmpty()) {
            xmlDocumentService.saveAs(currentFilename);
        } else {
            System.out.println("Warning: save() called with no filename. Trigger Save As.");
        }
    }

    @Override
    public void saveas(String filename) {
        if (filename == null || filename.isEmpty()) {
            System.out.println("Warning: saveas() called with empty filename. Need File Dialog.");
            return;
        }
        drawing.setFilename(filename);
        xmlDocumentService.saveAs(filename);
    }

    @Override
    public void newDrawing() {
        drawing.getShapes().clear();
        drawing.setFilename(null);
        clearSelections();
        drawing.setSelectedShape(null);
        drawing.setDrawMode(DrawMode.Idle);
        setToolMode(ToolMode.SELECT);
        setShapeMode(ShapeMode.Select);
        triggerRepaint();
    }

    @Override
    public String getFileName() {
        return drawing.getFilename();
    }

    @Override
    public void select(Shape shapeToSelect) {
        if (shapeToSelect == null) return;
        List<Shape> shapes = drawing.getShapes();
        for (Shape shape : shapes) {
            shape.setSelected(shape.equals(shapeToSelect));
        }
        drawing.setSelectedShape(shapeToSelect);
        triggerRepaint();
    }

    @Override
    public void unSelect(Shape shapeToUnselect) {
        if (shapeToUnselect == null) return;
        shapeToUnselect.setSelected(false);
        if (shapeToUnselect.equals(drawing.getSelectedShape())) {
            Shape newPrimary = null;
            for (Shape s : drawing.getShapes()) {
                if (s.isSelected()) {
                    newPrimary = s;
                    break;
                }
            }
            drawing.setSelectedShape(newPrimary);
        }
        triggerRepaint();
    }

    @Override
    public Shape getSelectedShape() {
        if (drawing.getSelectedShape() != null && drawing.getSelectedShape().isSelected()) {
            return drawing.getSelectedShape();
        }
        for (Shape shape : drawing.getShapes()) {
            if (shape.isSelected()) {
                drawing.setSelectedShape(shape);
                return shape;
            }
        }
        return null;
    }

    @Override
    public List<Shape> getSelectedShapes() {
        List<Shape> selectedShapes = new ArrayList<>();
        for (Shape shape : drawing.getShapes()) {
            if (shape.isSelected()) {
                selectedShapes.add(shape);
            }
        }
        return selectedShapes;
    }

    @Override
    public void clearSelections() {
        boolean selectionCleared = false;
        for (Shape shape : drawing.getShapes()) {
            if (shape.isSelected()) {
                shape.setSelected(false);
                shape.setSelectionMode(SelectionMode.None);
                selectionCleared = true;
            }
        }
        drawing.setSelectedShape(null);
        if (selectionCleared) {
            triggerRepaint();
        }
    }

    @Override
    public void setXLocation(int xLocation) {
        Shape selectedShape = getSelectedShape();
        if (selectedShape != null) {
            selectedShape.setLocation(new Point(xLocation, selectedShape.getLocation().y));
            triggerRepaint();
        }
    }

    @Override
    public int getXLocation() {
        Shape selectedShape = getSelectedShape();
        return (selectedShape != null && selectedShape.getLocation() != null) ? selectedShape.getLocation().x : 0;
    }

    @Override
    public void setYLocation(int yLocation) {
        Shape selectedShape = getSelectedShape();
        if (selectedShape != null) {
            selectedShape.setLocation(new Point(selectedShape.getLocation().x, yLocation));
            triggerRepaint();
        }
    }

    @Override
    public int getYLocation() {
        Shape selectedShape = getSelectedShape();
        return (selectedShape != null && selectedShape.getLocation() != null) ? selectedShape.getLocation().y : 0;
    }

    @Override
    public void setWidth(int width) {
        Shape selectedShape = getSelectedShape();
        if (selectedShape != null) {
            selectedShape.setWidth(Math.max(0, width));
            triggerRepaint();
        }
    }

    @Override
    public int getWidth() {
        Shape selectedShape = getSelectedShape();
        return (selectedShape != null) ? selectedShape.getWidth() : 0;
    }

    @Override
    public void setHeight(int height) {
        Shape selectedShape = getSelectedShape();
        if (selectedShape != null) {
            selectedShape.setHeight(Math.max(0, height));
            triggerRepaint();
        }
    }

    @Override
    public int getHeight() {
        Shape selectedShape = getSelectedShape();
        return (selectedShape != null) ? selectedShape.getHeight() : 0;
    }

    @Override
    public void setImageFileename() {
        imageFileService.setImage(drawing);
        if (drawing.getImageFilename() != null) {
            triggerRepaint();
        }
    }

    @Override
    public void setImageFileename(String filename) {
        drawing.setImageFilename(filename);
        triggerRepaint();
    }

    @Override
    public String getImageFileename() {
        Shape selectedShape = getSelectedShape();
        return drawing.getImageFilename();
    }

    @Override
    public String getText() {
        Shape selectedShape = getSelectedShape();
        return (selectedShape != null && selectedShape.getText() != null) ? selectedShape.getText() : drawing.getText();
    }

    @Override
    public void setText(String text) {
        if (text == null) text = "";
        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                if (Objects.equals(shape.getClass().getSimpleName(), "Text")) {
                    shape.setText(text);
                }
            }
        } else {
            drawing.setText(text);
        }
        triggerRepaint();
    }

    @Override
    public Font getFont() {
        Shape selectedShape = getSelectedShape();
        Font font = (selectedShape != null) ? selectedShape.getFont() : null;
        return (font != null) ? font : drawing.getFont();
    }

    @Override
    public void setFontSize(int fontSize) {
        if (fontSize < 1) fontSize = 1;
        Font currentFont = getFont();
        if (currentFont == null) return;
        Font newFont = new Font(currentFont.getFamily(), currentFont.getStyle(), fontSize);

        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                if (Objects.equals(shape.getClass().getSimpleName(), "Text")) {
                    Font shapeFont = shape.getFont() != null ? shape.getFont() : drawing.getFont();
                    if (shapeFont != null) {
                        shape.setFont(new Font(shapeFont.getFamily(), shapeFont.getStyle(), fontSize));
                    }
                }
            }
        } else {
            drawing.setFont(newFont);
        }
        triggerRepaint();
    }

    @Override
    public void setFontFamily(String family) {
        if (family == null || family.isEmpty()) return;
        Font currentFont = getFont();
        if (currentFont == null) return;
        Font newFont = new Font(family, currentFont.getStyle(), currentFont.getSize());

        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                if (Objects.equals(shape.getClass().getSimpleName(), "Text")) {
                    Font shapeFont = shape.getFont() != null ? shape.getFont() : drawing.getFont();
                    if (shapeFont != null) {
                        shape.setFont(new Font(family, shapeFont.getStyle(), shapeFont.getSize()));
                    }
                }
            }
        } else {
            drawing.setFont(newFont);
        }
        triggerRepaint();
    }

    @Override
    public void setFontStyle(int style) {
        Font currentFont = getFont();
        if (currentFont == null) return;
        Font newFont = new Font(currentFont.getFamily(), style, currentFont.getSize());

        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                if (Objects.equals(shape.getClass().getSimpleName(), "Text")) {
                    Font shapeFont = shape.getFont() != null ? shape.getFont() : drawing.getFont();
                    if (shapeFont != null) {
                        shape.setFont(new Font(shapeFont.getFamily(), style, shapeFont.getSize()));
                    }
                }
            }
        } else {
            drawing.setFont(newFont);
        }
        triggerRepaint();
    }

    @Override
    public void bringToFront(Shape shape) {
        if (shape == null) return;
        List<Shape> shapes = drawing.getShapes();
        if (shapes.remove(shape)) {
            shapes.add(shape);
            triggerRepaint();
        }
    }

    @Override
    public void sendToBack(Shape shape) {
        if (shape == null) return;
        List<Shape> shapes = drawing.getShapes();
        if (shapes.remove(shape)) {
            shapes.add(0, shape);
            triggerRepaint();
        }
    }

    @Override
    public void bringForward(Shape shape) {
        if (shape == null) return;
        List<Shape> shapes = drawing.getShapes();
        int index = shapes.indexOf(shape);
        if (index >= 0 && index < shapes.size() - 1) {
            shapes.remove(index);
            shapes.add(index + 1, shape);
            triggerRepaint();
        }
    }

    @Override
    public void sendBackward(Shape shape) {
        if (shape == null) return;
        List<Shape> shapes = drawing.getShapes();
        int index = shapes.indexOf(shape);
        if (index > 0) {
            shapes.remove(index);
            shapes.add(index - 1, shape);
            triggerRepaint();
        }
    }

    public void triggerRepaint() {
        if (drawingView != null) {
            drawingView.repaint();
        }
    }
}