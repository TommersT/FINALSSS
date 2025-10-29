package com.gabriel.draw.service;

// Commands...
import com.gabriel.draw.command.AddShapeCommand;
import com.gabriel.draw.command.MoveCommand;
import com.gabriel.draw.command.ScaleCommand;
import com.gabriel.draw.command.DeleteShapeCommand;
// drawfx imports...
import com.gabriel.drawfx.DrawMode;
import com.gabriel.drawfx.ShapeMode;
import com.gabriel.drawfx.ToolMode;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.command.CommandService;
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;

// AWT imports...
import java.awt.*;
import java.util.List;

public class DrawingCommandAppService implements AppService {
    private final AppService appService; // Wrapped service (final)
    protected static DrawingCommandAppService drawingCommandAppService = null; // Singleton instance

    // Private constructor for Singleton
    private DrawingCommandAppService(AppService appService){
        this.appService = appService;
    }

    // Singleton getInstance method
    public static AppService getInstance(AppService underlyingService){
        if(drawingCommandAppService == null){
            if (underlyingService == null) {
                throw new IllegalArgumentException("Underlying AppService cannot be null for Singleton initialization.");
            }
            drawingCommandAppService = new DrawingCommandAppService(underlyingService);
        }
        return drawingCommandAppService;
    }

    // Method to get the base service (used by commands)
    public AppService getUnderlyingAppService() {
        return appService;
    }

    // --- Undo/Redo ---
    @Override
    public void undo() {
        CommandService.undo();
    }

    @Override
    public void redo() {
        CommandService.redo();
    }

    // --- Mode Getters/Setters ---
    @Override
    public ShapeMode getShapeMode() {
        return appService.getShapeMode();
    }

    @Override
    public void setShapeMode(ShapeMode shapeMode) {
        // Command created elsewhere (PropertyEventListener/ActionController)
        appService.setShapeMode(shapeMode);
    }

    @Override
    public ToolMode getToolMode() {
        return appService.getToolMode();
    }

    @Override
    public void setToolMode(ToolMode toolMode) {
        // Direct state change, not typically command-based
        appService.setToolMode(toolMode);
    }

    @Override
    public DrawMode getDrawMode() {
        return appService.getDrawMode();
    }

    @Override
    public void setDrawMode(DrawMode drawMode) {
        // Direct state change during drag etc., not command-based
        appService.setDrawMode(drawMode);
    }

    // --- Color Properties ---
    @Override
    public Color getColor() { // Fore Color
        return appService.getColor();
    }

    @Override
    public void setColor(Color color) { // Fore Color
        // Command created by listener
        appService.setColor(color);
    }

    @Override
    public Color getFill() { // Solid Fill
        return appService.getFill();
    }

    @Override
    public void setFill(Color color) { // Solid Fill
        // Command created by listener
        appService.setFill(color);
    }

    // --- Gradient Properties ---
    @Override
    public Color getStartColor() {
        return appService.getStartColor();
    }

    @Override
    public void setStartColor(Color color) {
        // Command created by listener
        appService.setStartColor(color);
    }

    @Override
    public Color getEndColor() {
        return appService.getEndColor();
    }

    @Override
    public void setEndColor(Color color) {
        // Command created by listener
        appService.setEndColor(color);
    }

    @Override
    public boolean isUseGradient() {
        return appService.isUseGradient();
    }

    @Override
    public void setUseGradient(boolean useGradient) {
        // Command created by listener
        appService.setUseGradient(useGradient);
    }

    // --- Visibility Property ---
    @Override
    public boolean isVisible() {
        return appService.isVisible();
    }

    @Override
    public void setVisible(boolean visible) {
        // Command created by listener
        appService.setVisible(visible);
    }

    // --- Movement ---
    @Override
    public void move(Shape shape, Point start, Point end) {
        // Preview move - call underlying directly
        appService.move(shape, start, end);
    }

    @Override
    public void move(Point start, Point end) {
        // Final move - create command
        if (!appService.getSelectedShapes().isEmpty() && !start.equals(end)) { // Check if move occurred
            Command command = new MoveCommand(this, start, end); // Pass 'this' (the wrapped service)
            CommandService.ExecuteCommand(command);
        }
    }

    // --- Scaling ---
    @Override
    public void scale(Point start, Point end) {
        // Scales the entire drawing? Unlikely use case.
        appService.scale(start, end);
    }

    @Override
    public void scale(Shape shape, Point start, Point end) {
        // Final scale - create command
        if (shape != null && !start.equals(end)) { // Check if scale occurred
            Command command = new ScaleCommand(this, shape, start, end); // Pass 'this'
            CommandService.ExecuteCommand(command);
        }
    }

    @Override
    public void scale(Shape shape, Point end) {
        // Preview scale during creation - call underlying directly
        appService.scale(shape, end);
    }

    // --- Shape Lifecycle ---
    @Override
    public void create(Shape shape) {
        // Final creation - create command
        Command command = new AddShapeCommand(this, shape); // Pass 'this'
        CommandService.ExecuteCommand(command);
    }

    @Override
    public void insertAt(Shape shape, int index) {
        // Used by Undo logic - call underlying directly
        appService.insertAt(shape, index);
    }

    @Override
    public void delete(Shape shape) {
        // Final delete - create command
        Command command = new DeleteShapeCommand(this, shape); // Pass 'this'
        CommandService.ExecuteCommand(command);
    }

    // --- Application Lifecycle & File Ops ---
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
        // Replacing the model clears history
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
        // Selection - direct call
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
        appService.save(); // Not undoable
    }

    @Override
    public void saveas(String filename) {
        appService.saveas(filename); // Not undoable
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

    // --- Selection Management ---
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

    // --- Property Setters (Wrapped by Listener) ---
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

    // --- Arrangement Methods (Need Commands) ---
    @Override
    public void bringToFront(Shape shape) {
        // TODO: Implement BringToFrontCommand
        appService.bringToFront(shape);
    }

    @Override
    public void sendToBack(Shape shape) {
        // TODO: Implement SendToBackCommand
        appService.sendToBack(shape);
    }

    @Override
    public void bringForward(Shape shape) {
        // TODO: Implement BringForwardCommand
        appService.bringForward(shape);
    }

    @Override
    public void sendBackward(Shape shape) {
        // TODO: Implement SendBackwardCommand
        appService.sendBackward(shape);
    }
}
