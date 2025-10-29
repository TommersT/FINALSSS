package com.gabriel.draw.service;

import com.gabriel.draw.view.DrawingView; // Import DrawingView
import com.gabriel.drawfx.DrawMode;
import com.gabriel.drawfx.SelectionMode;
import com.gabriel.drawfx.ShapeMode;
import com.gabriel.drawfx.ToolMode;
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.*;
import lombok.Setter; // Keep if using Lombok, ensure it's configured

import java.util.ArrayList;
import java.util.List;
import java.awt.*;

public class DrawingAppService implements AppService {

    // Make drawing final as it's initialized in the constructor and shouldn't be replaced
    final private Drawing drawing;
    private ToolMode toolMode = ToolMode.SELECT; // Default tool

    // Use @Setter from Lombok OR provide manual setter
    @Setter
    private DrawingView drawingView;

    // Keep service references package-private or private
    private ImageFileService imageFileService;
    private MoverService moverService;
    private ScalerService scalerService;
    private SearchService searchService;
    private XmlDocumentService xmlDocumentService;

    // DocumentService documentService; // This seems unused, XmlDocumentService handles file ops

    // Manual setter if not using Lombok's @Setter
    // public void setDrawingView(DrawingView drawingView) {
    //    this.drawingView = drawingView;
    // }

    public DrawingAppService() {
        drawing = new Drawing(); // Initialize the core data model
        moverService = new MoverService();
        scalerService = new ScalerService();
        searchService = new SearchService();
        xmlDocumentService = new XmlDocumentService(drawing); // Pass drawing model to file service
        imageFileService = new ImageFileService();
        // Set initial drawing states if needed (already handled in Drawing model constructor)
        // drawing.setDrawMode(DrawMode.Idle);
        // drawing.setShapeMode(ShapeMode.Select); // Start with Select tool as default?
    }

    // --- Undo/Redo ---
    // These are handled by the DrawingCommandAppService wrapper.
    // The base service doesn't manage the command stacks directly.
    @Override
    public void undo() {
        // Implementation delegated to DrawingCommandAppService
        System.out.println("Warning: undo() called on base DrawingAppService. Should be handled by wrapper.");
    }

    @Override
    public void redo() {
        // Implementation delegated to DrawingCommandAppService
        System.out.println("Warning: redo() called on base DrawingAppService. Should be handled by wrapper.");
    }

    // --- Mode Getters/Setters ---
    @Override
    public ShapeMode getShapeMode() {
        return drawing.getShapeMode();
    }

    @Override
    public void setShapeMode(ShapeMode shapeMode) {
        drawing.setShapeMode(shapeMode);
        // Setting shape mode might imply changing tool mode back to DRAW
        // This logic might be better placed in the ActionController or DrawingController
        if (shapeMode != ShapeMode.Select) {
            // setToolMode(ToolMode.DRAW); // Consider if this side-effect is desired here
        }
        triggerRepaint(); // Repaint if mode change affects UI elements (unlikely here)
    }

    @Override
    public ToolMode getToolMode() {
        return toolMode;
    }

    @Override
    public void setToolMode(ToolMode toolMode) {
        // Only change if different to avoid unnecessary updates
        if (this.toolMode != toolMode) {
            this.toolMode = toolMode;
            // If switching away from DRAW, maybe clear any preview shape? (Controller logic)
            // If switching TO Select/Move/Scale, maybe ensure ShapeMode is Select?
            // if (toolMode == ToolMode.SELECT || toolMode == ToolMode.MOVE || toolMode == ToolMode.SCALE) {
            //     setShapeMode(ShapeMode.Select);
            // }
            triggerRepaint(); // Repaint if cursor or visual state changes
        }
    }

    @Override
    public DrawMode getDrawMode() {
        return drawing.getDrawMode();
    }

    @Override
    public void setDrawMode(DrawMode drawMode) {
        // Setting internal state, usually done by DrawingController
        this.drawing.setDrawMode(drawMode);
        // Repaint might be needed if state change affects rendering (e.g., stopping preview)
        triggerRepaint();
    }

    // --- Color Properties ---
    @Override
    public Color getColor() {
        // Return selected shape's color if one is selected, otherwise global color
        Shape selectedShape = getSelectedShape(); // Use helper method
        return (selectedShape != null) ? selectedShape.getColor() : drawing.getColor();
    }

    @Override
    public void setColor(Color color) {
        if (color == null) return; // Prevent setting null color

        List<Shape> shapes = getSelectedShapes(); // Use helper
        if (!shapes.isEmpty()) {
            // Apply to all selected shapes
            for (Shape shape : shapes) {
                shape.setColor(color);
            }
        } else {
            // Apply globally if no shape is selected
            drawing.setColor(color);
        }
        triggerRepaint(); // Repaint needed after color change
    }

    @Override
    public Color getFill() {
        Shape selectedShape = getSelectedShape();
        return (selectedShape != null) ? selectedShape.getFill() : drawing.getFill();
    }

    @Override
    public void setFill(Color color) {
        // Allow null fill color (transparent)
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

    // --- Thickness Property ---
    @Override
    public void setThickness(int thickness) {
        // Ensure thickness is positive
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


    // --- Movement ---
    @Override
    public void move(Shape shape, Point start, Point newLoc) {
        // Moves a single shape (used for drag preview)
        moverService.move(shape, start, newLoc);
        // Repaint is usually handled by the caller (DrawingController) during drag
    }

    @Override
    public void move(Point start, Point newLoc) {
        // Moves all *currently selected* shapes (used by MoveCommand)
        moverService.move(drawing, start, newLoc);
        triggerRepaint(); // Repaint after final move
    }

    // --- Scaling ---
    @Override
    public void scale(Point start, Point end) {
        // Scales the entire drawing? Unlikely use case.
        scalerService.scale(drawing, start, end);
        triggerRepaint();
    }

    @Override
    public void scale(Shape shape, Point start, Point end) {
        // Scales a single shape based on start/end drag relative to a handle (used by ScaleCommand)
        scalerService.scale(shape, start, end);
        triggerRepaint(); // Repaint after final scale
    }

    @Override
    public void scale(Shape shape, Point end) {
        // Scales a shape during creation preview (relative to its origin)
        scalerService.scale(shape, end);
        // Repaint is handled by DrawingController during drag preview
    }

    // --- Shape Lifecycle ---
    @Override
    public void create(Shape shape) {
        if (shape == null) return;
        // Assign default properties if not already set? (Usually done in Controller)
        // shape.setColor(drawing.getColor());
        // shape.setThickness(drawing.getThickness());
        // shape.setFill(drawing.getFill());
        // shape.setFont(drawing.getFont());
        // shape.setText(drawing.getText());
        shape.setR(drawing.getSearchRadius()); // Radius for handle detection
        shape.setId(this.drawing.getShapes().size() + 1); // Simple ID generation (consider UUID?)
        this.drawing.getShapes().add(shape);
        triggerRepaint();
    }

    @Override
    public void insertAt(Shape shape, int index) {
        // Used by Undo logic for Delete command
        if (shape == null || index < 0) return;
        List<Shape> shapes = drawing.getShapes();
        if (index > shapes.size()) index = shapes.size(); // Append if index out of bounds
        shapes.add(index, shape);
        triggerRepaint();
    }

    @Override
    public void delete(Shape shape) {
        if (shape == null) return;
        boolean removed = drawing.getShapes().remove(shape);
        if (removed) {
            // If the deleted shape was the primary selection, clear it
            if (shape.equals(drawing.getSelectedShape())) {
                drawing.setSelectedShape(null);
                // Try find another selected shape to be primary? Optional.
            }
            triggerRepaint();
        }
    }

    // --- Application Lifecycle ---
    @Override
    public void close() {
        // Handle saving unsaved changes?
        System.exit(0);
    }

    // --- Drawing Model Access ---
    @Override
    public Drawing getDrawing() {
        return drawing;
    }

    @Override
    public void setDrawing(Drawing newDrawing) {
        // Usually called after loading a file. Replace the contents, not the object.
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
            // Reset selection and modes
            clearSelections();
            drawing.setSelectedShape(null);
            drawing.setDrawMode(DrawMode.Idle);
            setToolMode(ToolMode.SELECT); // Reset tool to Select
            setShapeMode(ShapeMode.Select);
        }
        triggerRepaint();
    }

    // --- Search/Selection ---
    @Override
    public int getSearchRadius() {
        return drawing.getSearchRadius();
    }

    @Override
    public void setSearchRadius(int radius) {
        drawing.setSearchRadius(Math.max(1, radius)); // Ensure positive radius
    }

    @Override
    public void search(Point p) {
        // Default search (single selection)
        searchService.search(this, p, true);
        triggerRepaint(); // Repaint needed to show selection changes
    }

    @Override
    public void search(Point p, boolean single) {
        // Search with explicit single/multi-select toggle
        searchService.search(this, p, single);
        triggerRepaint();
    }

    // --- File Operations ---
    @Override
    public void open(String filename) {
        // Let XmlDocumentService handle parsing and updating the drawing model
        xmlDocumentService.open(filename);
        // XmlDocumentService modifies the 'drawing' object directly.
        // No need to call setDrawing here, but reset state and repaint.
        clearSelections();
        drawing.setSelectedShape(null);
        drawing.setDrawMode(DrawMode.Idle);
        setToolMode(ToolMode.SELECT);
        setShapeMode(ShapeMode.Select);
        triggerRepaint();
    }

    @Override
    public void save() {
        // Save to the current filename stored in the drawing model
        String currentFilename = drawing.getFilename();
        if (currentFilename != null && !currentFilename.isEmpty()) {
            xmlDocumentService.saveAs(currentFilename);
        } else {
            // If no filename, trigger Save As logic (usually handled by ActionController)
            System.out.println("Warning: save() called with no filename. Trigger Save As.");
            // Maybe call saveas("") and let it handle the dialog? Requires ActionController cooperation.
        }
    }

    @Override
    public void saveas(String filename) {
        if (filename == null || filename.isEmpty()) {
            // Trigger file dialog logic here or in ActionController
            System.out.println("Warning: saveas() called with empty filename. Need File Dialog.");
            return;
        }
        // Save to the specified filename AND update the drawing's filename
        drawing.setFilename(filename);
        xmlDocumentService.saveAs(filename);
    }

    @Override
    public void newDrawing() {
        drawing.getShapes().clear();
        drawing.setFilename(null); // Clear filename for new drawing
        // Reset other drawing properties? (Color, Fill, etc.?) - Optional
        // drawing.setColor(Color.RED);
        // drawing.setFill(Color.WHITE);
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

    // --- Selection Management ---
    @Override
    public void select(Shape shapeToSelect) {
        if (shapeToSelect == null) return;
        List<Shape> shapes = drawing.getShapes();
        // Single selection behavior: deselect others
        for (Shape shape : shapes) {
            shape.setSelected(shape.equals(shapeToSelect));
        }
        drawing.setSelectedShape(shapeToSelect); // Set the primary selected shape
        triggerRepaint();
    }

    @Override
    public void unSelect(Shape shapeToUnselect) {
        if (shapeToUnselect == null) return;
        shapeToUnselect.setSelected(false);
        // If the unselected shape was the primary, find a new primary or set to null
        if (shapeToUnselect.equals(drawing.getSelectedShape())) {
            Shape newPrimary = null;
            for (Shape s : drawing.getShapes()) {
                if (s.isSelected()) {
                    newPrimary = s;
                    break; // Found another selected shape
                }
            }
            drawing.setSelectedShape(newPrimary);
        }
        triggerRepaint();
    }

    @Override
    public Shape getSelectedShape() {
        // Return the designated primary selected shape
        // Or, if null, find the first selected shape in the list? Choose one convention.
        // Current convention seems to rely on drawing.getSelectedShape() being set correctly.
        if (drawing.getSelectedShape() != null && drawing.getSelectedShape().isSelected()) {
            return drawing.getSelectedShape();
        }
        // Fallback: find first selected if primary is null or somehow deselected
        for (Shape shape : drawing.getShapes()) {
            if (shape.isSelected()) {
                drawing.setSelectedShape(shape); // Update primary
                return shape;
            }
        }
        return null; // No shape selected
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
                shape.setSelectionMode(SelectionMode.None); // Reset handle mode
                selectionCleared = true;
            }
        }
        drawing.setSelectedShape(null); // Clear primary selection
        if (selectionCleared) {
            triggerRepaint(); // Repaint only if something changed
        }
    }

    // --- Location Properties ---
    @Override
    public void setXLocation(int xLocation) {
        Shape selectedShape = getSelectedShape(); // Only affects primary selected shape
        if (selectedShape != null) {
            selectedShape.getLocation().x = xLocation;
            triggerRepaint();
        }
    }

    @Override
    public int getXLocation() {
        Shape selectedShape = getSelectedShape();
        // Return 0 or some default if nothing selected?
        return (selectedShape != null) ? selectedShape.getLocation().x : 0; // drawing.getLocation().x for global?
    }

    @Override
    public void setYLocation(int yLocation) {
        Shape selectedShape = getSelectedShape();
        if (selectedShape != null) {
            selectedShape.getLocation().y = yLocation;
            triggerRepaint();
        }
    }

    @Override
    public int getYLocation() {
        Shape selectedShape = getSelectedShape();
        return (selectedShape != null) ? selectedShape.getLocation().y : 0;
    }

    // --- Dimension Properties ---
    @Override
    public void setWidth(int width) {
        Shape selectedShape = getSelectedShape();
        if (selectedShape != null) {
            // Ensure width is non-negative? Normalizer handles this usually.
            selectedShape.setWidth(Math.max(0, width));
            triggerRepaint();
        }
    }

    @Override
    public int getWidth() {
        Shape selectedShape = getSelectedShape();
        // drawing.getWidth() seems incorrect here, return 0 or shape width
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
        // drawing.getHeight() is wrong context. Return 0 if no shape.
        return (selectedShape != null) ? selectedShape.getHeight() : 0;
    }

    // --- Image Filename ---
    @Override
    public void setImageFileename() {
        // Delegates to ImageFileService to show dialog and update drawing model
        imageFileService.setImage(drawing);
        // If filename was set, trigger repaint (might show image preview or change tool state)
        if (drawing.getImageFilename() != null) {
            triggerRepaint();
        }
    }

    @Override
    public void setImageFileename(String filename) {
        // Directly sets the filename (e.g., when loading or potentially undoing)
        drawing.setImageFilename(filename);
        triggerRepaint(); // Repaint potentially affected image shapes
    }

    @Override
    public String getImageFileename() {
        return drawing.getImageFilename();
    }

    // --- Text Properties ---
    @Override
    public String getText() {
        Shape selectedShape = getSelectedShape();
        // Return shape's text or global default
        return (selectedShape != null && selectedShape.getText() != null) ? selectedShape.getText() : drawing.getText();
    }

    @Override
    public void setText(String text) {
        if (text == null) text = ""; // Ensure non-null text
        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            // Apply to all selected (relevant for Text shapes)
            for (Shape shape : shapes) {
                // Check if shape is capable of having text? (e.g., instanceof Text)
                // if (shape instanceof com.gabriel.draw.model.Text) { // Assuming Text class path
                shape.setText(text);
                // }
            }
        } else {
            // Set global default
            drawing.setText(text);
        }
        triggerRepaint(); // Text change requires repaint
    }

    @Override
    public Font getFont() {
        Shape selectedShape = getSelectedShape();
        // Return shape's font or global default
        return (selectedShape != null && selectedShape.getFont() != null) ? selectedShape.getFont() : drawing.getFont();
    }

    // --- Font Component Setters ---
    // These modify parts of the font for selected shapes or the global default

    @Override
    public void setFontSize(int fontSize) {
        if (fontSize < 1) fontSize = 1; // Basic validation
        Font currentFont = getFont(); // Get current font (selected or global)
        Font newFont = new Font(currentFont.getFamily(), currentFont.getStyle(), fontSize);

        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                // if (shape instanceof com.gabriel.draw.model.Text) { // Apply only to text shapes?
                Font shapeFont = shape.getFont() != null ? shape.getFont() : drawing.getFont(); // Use shape font or global default
                shape.setFont(new Font(shapeFont.getFamily(), shapeFont.getStyle(), fontSize));
                // }
            }
        } else {
            drawing.setFont(newFont); // Update global font
        }
        triggerRepaint();
    }

    @Override
    public void setFontFamily(String family) {
        if (family == null || family.isEmpty()) return; // Validation
        Font currentFont = getFont();
        Font newFont = new Font(family, currentFont.getStyle(), currentFont.getSize());

        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                // if (shape instanceof com.gabriel.draw.model.Text) {
                Font shapeFont = shape.getFont() != null ? shape.getFont() : drawing.getFont();
                shape.setFont(new Font(family, shapeFont.getStyle(), shapeFont.getSize()));
                // }
            }
        } else {
            drawing.setFont(newFont);
        }
        triggerRepaint();
    }

    @Override
    public void setFontStyle(int style) {
        // Basic validation for style bits might be needed
        Font currentFont = getFont();
        Font newFont = new Font(currentFont.getFamily(), style, currentFont.getSize());

        List<Shape> shapes = getSelectedShapes();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                // if (shape instanceof com.gabriel.draw.model.Text) {
                Font shapeFont = shape.getFont() != null ? shape.getFont() : drawing.getFont();
                shape.setFont(new Font(shapeFont.getFamily(), style, shapeFont.getSize()));
                // }
            }
        } else {
            drawing.setFont(newFont);
        }
        triggerRepaint();
    }

    // --- Arrangement (Z-Order) ---
    // These methods modify the order of shapes in the drawing's list

    @Override
    public void bringToFront(Shape shape) {
        if (shape == null) return;
        List<Shape> shapes = drawing.getShapes();
        if (shapes.remove(shape)) {
            shapes.add(shape); // Add to the end (top)
            triggerRepaint();
        }
    }

    @Override
    public void sendToBack(Shape shape) {
        if (shape == null) return;
        List<Shape> shapes = drawing.getShapes();
        if (shapes.remove(shape)) {
            shapes.add(0, shape); // Add to the beginning (bottom)
            triggerRepaint();
        }
    }

    @Override
    public void bringForward(Shape shape) {
        if (shape == null) return;
        List<Shape> shapes = drawing.getShapes();
        int index = shapes.indexOf(shape);
        // Check if shape exists and is not already at the front
        if (index >= 0 && index < shapes.size() - 1) {
            shapes.remove(index);
            shapes.add(index + 1, shape); // Insert one position higher
            triggerRepaint();
        }
    }

    @Override
    public void sendBackward(Shape shape) {
        if (shape == null) return;
        List<Shape> shapes = drawing.getShapes();
        int index = shapes.indexOf(shape);
        // Check if shape exists and is not already at the back
        if (index > 0) {
            shapes.remove(index);
            shapes.add(index - 1, shape); // Insert one position lower
            triggerRepaint();
        }
    }

    // --- Repaint Helper ---
    /**
     * Helper method to trigger a repaint on the associated DrawingView, if available.
     * Used internally and by commands after modifying shape state that requires redraw.
     */
    public void triggerRepaint() {
        if (drawingView != null) {
            drawingView.repaint();
        }
    }
}