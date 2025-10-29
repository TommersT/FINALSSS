package com.gabriel.drawfx.service;

import com.gabriel.drawfx.DrawMode;
import com.gabriel.drawfx.ShapeMode;
import com.gabriel.drawfx.ToolMode;
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;

// Removed javax.swing.* import as it's not needed here
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

    // --- Color ---
    Color getColor(); // Fore Color
    void setColor(Color color); // Fore Color
    Color getFill(); // Solid Fill Color
    void setFill(Color color); // Solid Fill Color

    // --- NEW Gradient Properties ---
    Color getStartColor();
    void setStartColor(Color color);
    Color getEndColor();
    void setEndColor(Color color);
    boolean isUseGradient();
    void setUseGradient(boolean useGradient);
    // --- END NEW Gradient Properties ---

    // --- NEW Visibility Property ---
    boolean isVisible();
    void setVisible(boolean visible);
    // --- END NEW Visibility Property ---


    void move (Shape shape, Point Start, Point newLoc);
    void move(Point start, Point newLoc) ;

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

    // Location (Applies to selected shape)
    void setXLocation(int xLocation);
    int getXLocation();
    void setYLocation(int yLocation);
    int getYLocation();

    // Dimensions (Applies to selected shape)
    void setWidth(int width);
    int getWidth();
    void setHeight(int height);
    int getHeight();

    // Image (Applies to selected Picture or global context)
    void setImageFileename(); // Opens dialog
    void setImageFileename(String filename); // Sets directly
    String getImageFileename();

    // Text (Applies to selected Text shape or global context)
    String getText();
    void setText(String text);

    // Font (Applies to selected Text shape or global context)
    Font getFont();
    void setFontSize(int fontSize);
    void setFontFamily(String family);
    void setFontStyle(int style);

    // Arrangement
    void bringToFront(Shape shape);
    void sendToBack(Shape shape);
    void bringForward(Shape shape);
    void sendBackward(Shape shape);
}
