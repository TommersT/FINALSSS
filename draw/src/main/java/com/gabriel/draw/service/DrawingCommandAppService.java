// tommerst/finalsss/FINALSSS-4b53253892a92ed882030feb653ef76e91b6ab5b/draw/src/main/java/com/gabriel/draw/service/DrawingCommandAppService.java
package com.gabriel.draw.service;

// FIX: Add missing import if needed, though SetDrawModeCommand is removed now
// import com.gabriel.draw.command.SetDrawModeCommand;
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

// Removed unused Swing import
import java.awt.*; // Keep AWT for Color, Point, Font
import java.util.List;

public class DrawingCommandAppService implements AppService {
    // FIX: Make the wrapped service private final for better encapsulation
    private final AppService appService;
    // FIX: Static instance management can be tricky. Consider dependency injection instead.
    // For now, keep the Singleton pattern but ensure it's initialized correctly.
    protected static DrawingCommandAppService drawingCommandAppService = null;

    // FIX: Constructor should be private for Singleton
    private DrawingCommandAppService(AppService appService){
        this.appService = appService;
    }

    // FIX: Standard Singleton getInstance method - lazy initialization
    public static AppService getInstance(AppService underlyingService){
        if(drawingCommandAppService == null){
            // Ensure the underlying service is not null
            if (underlyingService == null) {
                throw new IllegalArgumentException("Underlying AppService cannot be null for Singleton initialization.");
            }
            drawingCommandAppService = new DrawingCommandAppService(underlyingService);
        }
        // Optional: Check if the provided service matches the one used for initialization if called multiple times?
        // else if (drawingCommandAppService.appService != underlyingService) {
        //    // Handle mismatch? Log warning? Throw exception? Re-initialize?
        // }
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
        // ActionController or PropertyEventListener should create the command
        appService.setShapeMode(shapeMode);
    }

    @Override
    public ToolMode getToolMode() {
        return appService.getToolMode();
    }

    @Override
    public void setToolMode(ToolMode toolMode) {
        // Direct state change, not typically undone itself
        appService.setToolMode(toolMode);
    }

    @Override
    public DrawMode getDrawMode() {
        return appService.getDrawMode();
    }

    @Override
    public void setDrawMode(DrawMode drawMode) {
        // **** CHANGE ****
        // Directly call the underlying service. DrawMode changes during
        // operations like dragging aren't usually user-initiated undoable actions.
        // Command command = new SetDrawModeCommand(appService, drawMode);
        // CommandService.ExecuteCommand(command);
        appService.setDrawMode(drawMode); // Call directly
        // **** END CHANGE ****
    }

    // ... (Rest of the methods remain the same as the previous version) ...

    @Override
    public Color getColor() {
        return appService.getColor();
    }

    @Override
    public void setColor(Color color) {
        // Command is created by PropertyEventListener or ActionController
        appService.setColor(color);
    }

    @Override
    public Color getFill() {
        return appService.getFill();
    }

    @Override
    public void setFill(Color color) {
        // Command is created by PropertyEventListener or ActionController
        appService.setFill(color);
    }

    @Override
    public void move(Shape shape, Point start, Point end) {
        // Preview move - call underlying directly
        appService.move(shape, start, end);
    }

    @Override
    public void move(Point start, Point end) {
        // Final move - create command
        if (!appService.getSelectedShapes().isEmpty()) {
            Command command = new MoveCommand(this, start, end); // Pass 'this' (the wrapped service)
            CommandService.ExecuteCommand(command);
        } else {
            // If nothing selected, do nothing for now
        }
    }

    @Override
    public void scale(Point start, Point end) {
        // Scales the entire drawing? Unlikely use case.
        appService.scale(start, end);
    }

    @Override
    public void scale(Shape shape, Point start, Point end) {
        // Final scale - create command
        Command command = new ScaleCommand(this, shape, start, end); // Pass 'this'
        CommandService.ExecuteCommand(command);
    }

    @Override
    public void scale(Shape shape, Point end) {
        // Preview scale during creation - call underlying directly
        appService.scale(shape, end);
    }

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
        // Replacing the entire drawing model usually clears undo/redo history.
        CommandService.clear(); // Clear stacks when loading/new drawing
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
        // Clear history before opening
        CommandService.clear();
        appService.open(filename);
    }


    @Override
    public void save() {
        appService.save(); // Save is not typically undoable
    }

    @Override
    public void saveas(String filename) {
        appService.saveas(filename); // Save As is not typically undoable
    }

    @Override
    public void newDrawing() {
        // Clear history for new drawing
        CommandService.clear();
        appService.newDrawing();
    }

    @Override
    public String getFileName() {
        return appService.getFileName();
    }

    @Override
    public void select(Shape selectedShape) {
        // Selection - direct call
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

    // --- Property Setters ---
    // These are wrapped by PropertyEventListener which creates commands

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

    // --- Arrangement Methods ---
    // These need commands implemented eventually

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