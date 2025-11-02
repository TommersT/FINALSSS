package com.gabriel.draw.service;

import com.gabriel.draw.command.AddShapeCommand;
import com.gabriel.draw.command.MoveCommand;
import com.gabriel.draw.command.ScaleCommand;
import com.gabriel.draw.command.DeleteShapeCommand;
import com.gabriel.drawfx.DrawMode;
import com.gabriel.drawfx.ShapeMode;
import com.gabriel.drawfx.ToolMode;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.command.CommandService;
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;

import java.awt.*;
import java.util.List;

public class DrawingCommandAppService implements AppService {
    private final AppService appService;
    protected static DrawingCommandAppService drawingCommandAppService = null;

    private DrawingCommandAppService(AppService appService){
        this.appService = appService;
    }

    public static AppService getInstance(AppService underlyingService){
        if(drawingCommandAppService == null){
            if (underlyingService == null) {
                throw new IllegalArgumentException("Underlying AppService cannot be null for Singleton initialization.");
            }
            drawingCommandAppService = new DrawingCommandAppService(underlyingService);
        }
        return drawingCommandAppService;
    }

    public AppService getUnderlyingAppService() {
        return appService;
    }

    @Override
    public void undo() {
        CommandService.undo();
    }

    @Override
    public void redo() {
        CommandService.redo();
    }

    @Override
    public ShapeMode getShapeMode() {
        return appService.getShapeMode();
    }

    @Override
    public void setShapeMode(ShapeMode shapeMode) {
        appService.setShapeMode(shapeMode);
    }

    @Override
    public ToolMode getToolMode() {
        return appService.getToolMode();
    }

    @Override
    public void setToolMode(ToolMode toolMode) {
        appService.setToolMode(toolMode);
    }

    @Override
    public DrawMode getDrawMode() {
        return appService.getDrawMode();
    }

    @Override
    public void setDrawMode(DrawMode drawMode) {
        appService.setDrawMode(drawMode);
    }

    @Override
    public Color getColor() {
        return appService.getColor();
    }

    @Override
    public void setColor(Color color) {
        appService.setColor(color);
    }

    @Override
    public Color getFill() {
        return appService.getFill();
    }

    @Override
    public void setFill(Color color) {
        appService.setFill(color);
    }

    @Override
    public Color getStartColor() {
        return appService.getStartColor();
    }

    @Override
    public void setStartColor(Color color) {
        appService.setStartColor(color);
    }

    @Override
    public Color getEndColor() {
        return appService.getEndColor();
    }

    @Override
    public void setEndColor(Color color) {
        appService.setEndColor(color);
    }

    @Override
    public boolean isUseGradient() {
        return appService.isUseGradient();
    }

    @Override
    public void setUseGradient(boolean useGradient) {
        appService.setUseGradient(useGradient);
    }

    @Override
    public boolean isVisible() {
        return appService.isVisible();
    }

    @Override
    public void setVisible(boolean visible) {
        appService.setVisible(visible);
    }

    @Override
    public void move(Shape shape, Point start, Point end) {
        appService.move(shape, start, end);
    }

    @Override
    public void move(Point start, Point end) {
        if (!appService.getSelectedShapes().isEmpty() && !start.equals(end)) {
            Command command = new MoveCommand(this, start, end);
            CommandService.ExecuteCommand(command);
        }
    }

    @Override
    public void scale(Point start, Point end) {
        appService.scale(start, end);
    }

    @Override
    public void scale(Shape shape, Point start, Point end) {
        if (shape != null && !start.equals(end)) {
            Command command = new ScaleCommand(this, shape, start, end);
            CommandService.ExecuteCommand(command);
        }
    }

    @Override
    public void scale(Shape shape, Point end) {
        appService.scale(shape, end);
    }

    @Override
    public void create(Shape shape) {
        Command command = new AddShapeCommand(this, shape);
        CommandService.ExecuteCommand(command);
    }

    @Override
    public void insertAt(Shape shape, int index) {
        appService.insertAt(shape, index);
    }

    @Override
    public void delete(Shape shape) {
        Command command = new DeleteShapeCommand(this, shape);
        CommandService.ExecuteCommand(command);
    }

    @Override
    public void close() {
        appService.close();
    }

    @Override
    public Drawing getDrawing() {
        return appService.getDrawing();
    }

    @Override
    public void setDrawing(Drawing drawing) {
        CommandService.clear();
        appService.setDrawing(drawing);
    }

    @Override
    public int getSearchRadius() {
        return appService.getSearchRadius();
    }

    @Override
    public void setSearchRadius(int radius) {
        appService.setSearchRadius(radius);
    }

    @Override
    public void search(Point p) {
        appService.search(p);
    }

    @Override
    public void search(Point p, boolean single) {
        appService.search(p, single);
    }

    @Override
    public void open(String filename) {
        CommandService.clear();
        appService.open(filename);
    }

    @Override
    public void save() {
        appService.save();
    }

    @Override
    public void saveas(String filename) {
        appService.saveas(filename);
    }

    @Override
    public void newDrawing() {
        CommandService.clear();
        appService.newDrawing();
    }

    @Override
    public String getFileName() {
        return appService.getFileName();
    }

    @Override
    public void select(Shape selectedShape) {
        appService.select(selectedShape);
    }

    @Override
    public void unSelect(Shape selectedShape) {
        appService.unSelect(selectedShape);
    }

    @Override
    public Shape getSelectedShape() {
        return appService.getSelectedShape();
    }

    @Override
    public List<Shape> getSelectedShapes() {
        return appService.getSelectedShapes();
    }

    @Override
    public void clearSelections(){
        appService.clearSelections();
    }

    @Override
    public void setThickness(int thickness) {
        appService.setThickness(thickness);
    }

    @Override
    public int getThickness() {
        return appService.getThickness();
    }

    @Override
    public void setXLocation(int xLocation) {
        appService.setXLocation(xLocation);
    }

    @Override
    public int getXLocation() {
        return appService.getXLocation();
    }

    @Override
    public void setYLocation(int yLocation) {
        appService.setYLocation(yLocation);
    }

    @Override
    public int getYLocation() {
        return appService.getYLocation();
    }

    @Override
    public void setWidth(int width) {
        appService.setWidth(width);
    }

    @Override
    public int getWidth() {
        return appService.getWidth();
    }

    @Override
    public void setHeight(int height) {
        appService.setHeight(height);
    }

    @Override
    public int getHeight() {
        return appService.getHeight();
    }

    @Override
    public void setImageFileename() {
        appService.setImageFileename();
    }

    @Override
    public void setImageFileename(String filename) {
        appService.setImageFileename(filename);
    }

    @Override
    public String getImageFileename() {
        return appService.getImageFileename();
    }

    @Override
    public String getText() {
        return appService.getText();
    }

    @Override
    public void setText(String text) {
        appService.setText(text);
    }

    @Override
    public Font getFont() {
        return appService.getFont();
    }

    @Override
    public void setFontSize(int fontSize) {
        appService.setFontSize(fontSize);
    }

    @Override
    public void setFontFamily(String family) {
        appService.setFontFamily(family);
    }

    @Override
    public void setFontStyle(int style) {
        appService.setFontStyle(style);
    }

    @Override
    public void bringToFront(Shape shape) {
        appService.bringToFront(shape);
    }

    @Override
    public void sendToBack(Shape shape) {
        appService.sendToBack(shape);
    }

    @Override
    public void bringForward(Shape shape) {
        appService.bringForward(shape);
    }

    @Override
    public void sendBackward(Shape shape) {
        appService.sendBackward(shape);
    }
}
