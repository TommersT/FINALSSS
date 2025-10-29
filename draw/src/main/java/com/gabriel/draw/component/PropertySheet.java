package com.gabriel.draw.component;

import com.gabriel.drawfx.ShapeMode;
import com.gabriel.property.PropertyOptions;
import com.gabriel.property.PropertyPanel;
// Import specific property types
import com.gabriel.property.property.*;
import com.gabriel.property.property.selection.Item;
import com.gabriel.property.property.selection.SelectionProperty;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import com.gabriel.drawfx.model.Drawing; // Import Drawing model

// --- NEW IMPORTS ---
import com.gabriel.property.PropertyModel; // Import needed for table model casting
import com.gabriel.property.cell.SelectionCellComponent; // Import needed for cell component casting
// --- END NEW IMPORTS ---


import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn; // Import TableColumn
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
// REMOVED: import java.awt.Rectangle; // No longer needed
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PropertySheet extends PropertyPanel { // PropertyPanel extends JTable

    // Store references to the Property objects to update their values
    private final Map<String, Property<?>> propertyMap = new HashMap<>();

    // Keep font style items needed for dropdown
    private final Item<Integer> PlainItem = new Item<>(Font.PLAIN, "Plain");
    private final Item<Integer> BoldItem = new Item<>(Font.BOLD, "Bold");
    private final Item<Integer> ItalicItem = new Item<>(Font.ITALIC, "Italic");
    private final Item<Integer> BoldItalicItem = new Item<>(Font.BOLD | Font.ITALIC, "Bold Italic");
    private final ArrayList<Item<Integer>> fontStyleItems = new ArrayList<>(Arrays.asList(
            PlainItem, BoldItem, ItalicItem, BoldItalicItem
    ));

    // Flag to prevent updates while populating
    private boolean isPopulating = false;

    // --- Flag to control editability based on selection ---
    private boolean editingEnabled = false;
    // --- End Flag ---

    public PropertySheet(PropertyOptions options) {
        super(options);
        initializeProperties(); // Create all properties upfront
        configureColumns(); // Configure columns after init
    }

    // Method for DrawingController to set editability
    public void setEditingEnabled(boolean enabled) {
        this.editingEnabled = enabled;
        // Optionally force repaint or revalidation if needed
        if (getModel() instanceof PropertyModel) {
            ((PropertyModel) getModel()).fireTableDataChanged(); // Refresh to update editable state visuals
        } else {
            repaint();
        }
    }


    // Create all Property objects once
    private void initializeProperties() {
        // Clear existing (if any)
        propertyMap.clear();
        if (getModel() instanceof PropertyModel) { // Check if model exists before clearing
            ((PropertyModel) getModel()).setRowCount(0); // More direct way to clear DefaultTableModel
        }

        // Define properties in the desired order
        addInternalProperty(new StringProperty("Object Type", "None")); // Non-editable type display
        addInternalProperty(new ColorProperty("Fore Color", Color.BLACK));
        addInternalProperty(new ColorProperty("Fill Color", Color.WHITE));
        addInternalProperty(new ColorProperty("Start Color", Color.LIGHT_GRAY)); // Gradient Start
        addInternalProperty(new ColorProperty("End Color", Color.DARK_GRAY));     // Gradient End
        addInternalProperty(new BooleanProperty("Use Gradient", false));
        addInternalProperty(new BooleanProperty("Visible", true));
        addInternalProperty(new IntegerProperty("X Location", 0));
        addInternalProperty(new IntegerProperty("Y Location", 0));
        addInternalProperty(new IntegerProperty("Width", 0));
        addInternalProperty(new IntegerProperty("Height", 0));
        addInternalProperty(new IntegerProperty("Line Thickness", 1));
        addInternalProperty(new StringProperty("Text", ""));
        // ActionProperty for Image Filename (Button to change)
        addInternalProperty(new ActionProperty("Image", "Change...", () -> {
            // Action implemented later, needs appService reference during populate
            System.out.println("Change Image button clicked (action set in populateTable)");
        }));
        addInternalProperty(new StringProperty("Font Family", "SansSerif"));
        addInternalProperty(new SelectionProperty<>("Font Style", fontStyleItems));
        addInternalProperty(new IntegerProperty("Font Size", 12));
    }

    // Configure column widths
    private void configureColumns() {
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Allow setting specific widths
        TableColumn propertyColumn = getColumnModel().getColumn(0);
        propertyColumn.setPreferredWidth(100); // Give property name column a decent width
        propertyColumn.setMinWidth(80);
        TableColumn valueColumn = getColumnModel().getColumn(1);
        valueColumn.setPreferredWidth(160); // Give value column more space
        valueColumn.setMinWidth(100);
        // Turn auto-resize back on if desired, or leave it off
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS); // Let value column take extra space
    }


    // Helper to add property to the panel and map
    private void addInternalProperty(Property<?> prop) {
        propertyMap.put(prop.getName(), prop);
        try {
            super.addProperty(prop); // Add to the table view using the superclass method
        } catch (Exception e) {
            // Catch potential PropertyNotSupportedException or others
            System.err.println("Error adding property '" + prop.getName() + "' internally: " + e.getMessage());
            // Optionally add a fallback row indicating the error
            if (getModel() instanceof PropertyModel) {
                ((PropertyModel) getModel()).addRow(new Object[]{prop.getName(), "Error loading editor"});
            }
        }
    }


    // Override addProperty to prevent external direct additions after init
    @Override
    public void addProperty(Property property) {
        System.err.println("Warning: PropertySheet properties should be initialized internally. Use populateTable to update values.");
    }
    @Override
    public void addProperty(Property property, com.gabriel.property.cell.AbstractCellComponent cellComponent) {
        System.err.println("Warning: PropertySheet properties should be initialized internally. Use populateTable to update values.");
    }

    // Override clear to re-initialize instead of just removing rows
    @Override
    public void clear() {
        initializeProperties();
        configureColumns(); // Reconfigure columns after re-initializing
    }


    // The main method to update property values based on selection
    public void populateTable(AppService appService) {
        if (!(getModel() instanceof PropertyModel) || getModel().getRowCount() == 0) {
            System.err.println("PropertySheet.populateTable called with invalid or empty model. Initializing.");
            initializeProperties();
            configureColumns();
            if (appService == null) return;
        }

        isPopulating = true; // Prevent listener feedback loops

        Shape shape = appService.getSelectedShape();
        Drawing drawing = appService.getDrawing(); // Get drawing defaults
        boolean shapeSelected = (shape != null);
        setEditingEnabled(shapeSelected); // Set internal flag for isCellEditable

        // --- Update Property Values (using existing update methods) ---
        updateStringProperty("Object Type", shapeSelected ? shape.getClass().getSimpleName() : "Drawing");
        updateColorProperty("Fore Color", shapeSelected ? shape.getColor() : drawing.getColor());
        updateColorProperty("Fill Color", shapeSelected ? shape.getFill() : drawing.getFill());
        updateColorProperty("Start Color", shapeSelected ? shape.getStartColor() : drawing.getStartColor());
        updateColorProperty("End Color", shapeSelected ? shape.getEndColor() : drawing.getEndColor());
        updateBooleanProperty("Use Gradient", shapeSelected ? shape.isUseGradient() : drawing.isUseGradient());
        updateBooleanProperty("Visible", shapeSelected ? shape.isVisible() : true); // Assume drawing is 'visible'

        updateIntegerProperty("X Location", shapeSelected && shape.getLocation() != null ? shape.getLocation().x : 0);
        updateIntegerProperty("Y Location", shapeSelected && shape.getLocation() != null ? shape.getLocation().y : 0);
        updateIntegerProperty("Width", shapeSelected ? shape.getWidth() : 0);
        updateIntegerProperty("Height", shapeSelected ? shape.getHeight() : 0);
        updateIntegerProperty("Line Thickness", shapeSelected ? shape.getThickness() : drawing.getThickness());

        // Text/Font properties
        Font fontToUse = drawing.getFont();
        String textToUse = drawing.getText();
        boolean isTextShape = shapeSelected && Objects.equals(shape.getClass().getSimpleName(), "Text");
        boolean isPictureShape = shapeSelected && Objects.equals(shape.getClass().getSimpleName(), "Picture");

        if (isTextShape) {
            fontToUse = shape.getFont() != null ? shape.getFont() : fontToUse;
            textToUse = shape.getText() != null ? shape.getText() : textToUse;
        } else {
            textToUse = ""; // Show empty if not text shape
            fontToUse = drawing.getFont(); // Use global defaults
        }

        updateStringProperty("Text", textToUse);
        updateStringProperty("Font Family", fontToUse != null ? fontToUse.getFamily() : "SansSerif");
        updateSelectionProperty("Font Style", fontToUse != null ? fontToUse.getStyle() : Font.PLAIN);
        updateIntegerProperty("Font Size", fontToUse != null ? fontToUse.getSize() : 12);

        // Update ActionProperty for Image
        Property<?> imageProp = propertyMap.get("Image");
        if (imageProp instanceof ActionProperty) {
            ActionProperty actionProp = (ActionProperty) imageProp;
            actionProp.setValue(() -> { // Update action lambda
                if (appService != null && shapeSelected && isPictureShape) { // Enable only for selected Picture
                    appService.setImageFileename();
                } else {
                    System.err.println("Change Image action: AppService not available or shape not a Picture.");
                }
            });
        }


        // --- Refresh Table UI ---
        if (isEditing()) {
            getCellEditor().stopCellEditing();
        }
        if (getModel() instanceof PropertyModel) {
            PropertyModel model = (PropertyModel) getModel();
            model.fireTableDataChanged(); // Notify table structure/data might have changed
        } else {
            repaint(); // Fallback repaint
        }


        isPopulating = false; // Re-enable listener updates
    }

    // --- Helper methods to update specific property types ---
    private <T> void updateProperty(String name, T value) {
        Property<?> p = propertyMap.get(name);
        if (p != null) {
            int rowIndex = getPropertyRowIndex(name);
            if (rowIndex < 0 || rowIndex >= getRowCount()) {
                System.err.println("Warning: Property '" + name + "' found in map but not in table model during update.");
                return;
            }

            try {
                Object currentModelValue = getModel().getValueAt(rowIndex, 1);
                if (!Objects.equals(p.getValue(), value) || !Objects.equals(currentModelValue, value)) {
                    if (p.getValue() == null || value == null || p.getValue().getClass().isAssignableFrom(value.getClass())) {
                        ((Property<T>) p).setValue(value);
                        if (isEditing() && getEditingRow() == rowIndex && getEditingColumn() == 1) {
                            getCellEditor(rowIndex, 1).cancelCellEditing();
                        }
                        getModel().setValueAt(value, rowIndex, 1);
                    } else if (value.getClass() == Integer.class && p.getValue().getClass() == Font.class && name.equals("Font Style")) {
                        ((Property<T>) p).setValue(value);
                        getModel().setValueAt(value, rowIndex, 1);
                    }
                    else {
                        System.err.println("Type mismatch prevented update for property '" + name + "'. Expected " + p.getValue().getClass() + ", got " + value.getClass());
                    }
                }
            } catch (ClassCastException e) {
                System.err.println("Error updating property '" + name + "': Type mismatch. Value: " + value + ", Error: " + e.getMessage());
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Error updating property '" + name + "' at row " + rowIndex + ": Index out of bounds. Table size might be incorrect.");
            }
        } else {
            System.err.println("Warning: Property '" + name + "' not found in propertyMap for update.");
        }
    }

    private void updateStringProperty(String name, String value) {
        updateProperty(name, value != null ? value : "");
    }

    private void updateIntegerProperty(String name, int value) {
        updateProperty(name, value);
    }

    private void updateBooleanProperty(String name, boolean value) {
        updateProperty(name, value);
    }

    private void updateColorProperty(String name, Color value) {
        updateProperty(name, value != null ? value : Color.GRAY);
    }

    private void updateSelectionProperty(String name, Object value) { // Value is the raw value (e.g., Integer for Font Style)
        Property<?> p = propertyMap.get(name);
        int rowIndex = getPropertyRowIndex(name);

        if (rowIndex < 0) {
            System.err.println("Warning: Row index not found for selection property '" + name + "' during update.");
            return;
        }

        if (p instanceof SelectionProperty) {
            SelectionProperty sp = (SelectionProperty) p;
            Item<?> matchingItem = null;
            for (Object itemObj : sp.getItems()) {
                if (itemObj instanceof Item) {
                    Item item = (Item) itemObj;
                    if (Objects.equals(item.getValue(), value)) {
                        matchingItem = item;
                        break;
                    }
                }
            }

            if (matchingItem != null) {
                try {
                    // Update property and model value IF they are different
                    if (!Objects.equals(sp.getValue(), matchingItem.getValue()) ||
                            !Objects.equals(getModel().getValueAt(rowIndex, 1), matchingItem.getValue()))
                    {
                        if (isEditing() && getEditingRow() == rowIndex && getEditingColumn() == 1) {
                            getCellEditor(rowIndex, 1).stopCellEditing();
                        }
                        sp.setValue(matchingItem.getValue()); // Update internal property value
                        getModel().setValueAt(matchingItem.getValue(), rowIndex, 1); // Update model value

                        // *** REMOVED invokeLater block for visual update ***
                        // Rely on fireTableDataChanged instead
                    }
                } catch (Exception e) {
                    System.err.println("Error setting selection property '" + name + "': " + e.getMessage());
                }
            } else {
                System.err.println("Warning: Could not find matching item for value '" + value + "' in SelectionProperty '" + name + "'");
                getModel().setValueAt(null, rowIndex, 1);
            }
        } else {
            System.err.println("Warning: Property '" + name + "' is not a SelectionProperty during updateSelectionProperty.");
        }
    }


    // Helper to find the row index of a property by name
    private int getPropertyRowIndex(String name) {
        for(int i = 0; i < getRowCount(); i++) {
            try {
                Object propName = getValueAt(i, 0); // Property name is in column 0
                if (name.equals(propName)) {
                    return i;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Error in getPropertyRowIndex: Index out of bounds at row " + i + ". Table size might be incorrect.");
                return -1; // Stop searching if table model is inconsistent
            }
        }
        return -1; // Not found
    }

    // --- Control Editability ---
    @Override
    public boolean isCellEditable(int row, int column) {
        if (column != 1) {
            return false;
        }
        if (row < 0 || row >= getRowCount()) {
            System.err.println("isCellEditable called with invalid row: " + row);
            return false;
        }

        Object propNameObj = getValueAt(row, 0);
        if (!(propNameObj instanceof String)) return false;
        String propName = (String) propNameObj;

        Property<?> prop = propertyMap.get(propName);
        if (prop == null) return false;

        if (propName.equals("Object Type")) return false;

        if (prop instanceof ActionProperty) {
            if (propName.equals("Image")) {
                boolean isPictureShape = false;
                int typeRowIndex = getPropertyRowIndex("Object Type");
                if (editingEnabled && typeRowIndex >=0 && typeRowIndex < getRowCount()) {
                    isPictureShape = "Picture".equals(getModel().getValueAt(typeRowIndex, 1));
                }
                return isPictureShape;
            }
            return true;
        }

        if (!editingEnabled) { // No shape selected
            return propName.equals("Fore Color") || propName.equals("Fill Color") ||
                    propName.equals("Start Color") || propName.equals("End Color") ||
                    propName.equals("Use Gradient") || propName.equals("Line Thickness") ||
                    propName.equals("Text") || propName.equals("Font Family") ||
                    propName.equals("Font Style") || propName.equals("Font Size");
        } else { // Shape is selected
            boolean isTextShape = false;
            int typeRowIndex = getPropertyRowIndex("Object Type");
            if (typeRowIndex >=0 && typeRowIndex < getRowCount()) {
                isTextShape = "Text".equals(getModel().getValueAt(typeRowIndex, 1));
            }

            if (!isTextShape && (propName.equals("Text") || propName.equals("Font Family") || propName.equals("Font Style") || propName.equals("Font Size"))) {
                return false;
            }
            return true;
        }
    }


    // Override prepareRenderer/Editor to visually indicate disabled state
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        boolean editable = isCellEditable(row, column);
        c.setEnabled(editable); // Set enabled state

        // Set background/foreground based on editability and selection
        if (!editable && column == 1) {
            c.setBackground(Color.LIGHT_GRAY);
            c.setForeground(Color.GRAY);
        } else {
            if (isRowSelected(row)) {
                c.setBackground(getSelectionBackground());
                c.setForeground(getSelectionForeground());
            } else {
                c.setBackground(getBackground());
                c.setForeground(getForeground());
            }
        }
        // Ensure combo box renderer (JLabel) text color is correct when disabled
        if (c instanceof JLabel && !editable) {
            c.setForeground(Color.GRAY);
        }

        return c;
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component c = super.prepareEditor(editor, row, column);
        c.setEnabled(isCellEditable(row, column));
        c.setBackground(getBackground()); // Use default editor background
        c.setForeground(getForeground());
        return c;
    }

    // --- ADDED: Override getToolTipText to show property description ---
    @Override
    public String getToolTipText(java.awt.event.MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        int col = columnAtPoint(event.getPoint());
        if (row >= 0 && col == 0) { // Tooltip for property name column
            Object propNameObj = getValueAt(row, 0);
            if (propNameObj instanceof String) {
                return (String) propNameObj; // For now, just show the name again
            }
        }
        return super.getToolTipText(event); // Default tooltip behavior elsewhere
    }
}