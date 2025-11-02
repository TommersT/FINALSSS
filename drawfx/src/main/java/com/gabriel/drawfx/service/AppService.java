package com.gabriel.drawfx.service;

import com.gabriel.drawfx.DrawMode;
import com.gabriel.drawfx.ShapeMode;
import com.gabriel.drawfx.ToolMode;
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;

import java.awt.*;
import java.util.List;

public interface AppService {
    void undo();
    void redo();

    ShapeMode getShapeMode();
    void setShapeMode(ShapeMode shapeMode);

    ToolMode getToolMode();
    void setToolMode(ToolMode toolMode);

    DrawMode getDrawMode();
    void setDrawMode(DrawMode drawMode);

    Color getColor();
    void setColor(Color color);
    Color getFill();
    void setFill(Color color);

    Color getStartColor();
    void setStartColor(Color color);
    Color getEndColor();
    void setEndColor(Color color);
    boolean isUseGradient();
    void setUseGradient(boolean useGradient);

    boolean isVisible();
    void setVisible(boolean visible);

    void move(Shape shape, Point Start, Point newLoc);
    void move(Point start, Point newLoc);

    void scale(Point start, Point end);
    void scale(Shape shape, Point start, Point end);
    void scale(Shape shape, Point end);

    void create(Shape shape);
    void insertAt(Shape shape, int index);
    void delete(Shape shape);

    void close();

    Drawing getDrawing();
    void setDrawing(Drawing drawing);

    int getSearchRadius();
    void setSearchRadius(int radius);

    void search(Point p);
    void search(Point p, boolean single);

    void open(String filename);
    void save();
    void saveas(String filename);
    void newDrawing();
    String getFileName();

    void select(Shape shape);
    void unSelect(Shape shape);

    Shape getSelectedShape();
    List<Shape> getSelectedShapes();
    void clearSelections();

    void setThickness(int thickness);
    int getThickness();

    void setXLocation(int xLocation);
    int getXLocation();
    void setYLocation(int yLocation);
    int getYLocation();

    void setWidth(int width);
    int getWidth();
    void setHeight(int height);
    int getHeight();

    void setImageFileename();
    void setImageFileename(String filename);
    String getImageFileename();

    String getText();
    void setText(String text);

    Font getFont();
    void setFontSize(int fontSize);
    void setFontFamily(String family);
    void setFontStyle(int style);

    void bringToFront(Shape shape);
    void sendToBack(Shape shape);
    void bringForward(Shape shape);
    void sendBackward(Shape shape);
}
