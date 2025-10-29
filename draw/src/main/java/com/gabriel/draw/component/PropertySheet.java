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
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PropertySheet extends PropertyPanel {

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
        super.clear(); // Clear rows from the underlying model as well

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

        // Note: 'Selected' is not added as it's not a user-editable property here.
        // Note: 'Current Shape' dropdown removed for uniform layout.

        // Initial render after adding all properties
        this.revalidate();
        this.repaint();
    }

    // Helper to add property to the panel and map
    private void addInternalProperty(Property<?> prop) {
        propertyMap.put(prop.getName(), prop);
        super.addProperty(prop); // Add to the table view
    }


    // Override addProperty to prevent external direct additions after init
    @Override
    public void addProperty(Property property) {
        // Optionally log a warning or throw exception
        System.err.println("Warning: PropertySheet properties should be initialized internally. Use populateTable to update values.");
        // super.addProperty(property); // Avoid adding duplicates
    }
    @Override
    public void addProperty(Property property, com.gabriel.property.cell.AbstractCellComponent cellComponent) {
        System.err.println("Warning: PropertySheet properties should be initialized internally. Use populateTable to update values.");
        // super.addProperty(property, cellComponent); // Avoid adding duplicates
    }

    // Override clear to re-initialize instead of just removing rows
    @Override
    public void clear() {
        // Re-initialize to maintain the structure
        initializeProperties();
    }


    // The main method to update property values based on selection
    public void populateTable(AppService appService) {
        isPopulating = true; // Prevent listener feedback loops

        Shape shape = appService.getSelectedShape();
        Drawing drawing = appService.getDrawing(); // Get drawing defaults
        boolean shapeSelected = (shape != null);
        setEditingEnabled(shapeSelected); // Set internal flag for isCellEditable

        // --- Update Property Values ---
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

        // Text/Font properties - Use shape's if Text, else drawing defaults
        Font fontToUse = drawing.getFont();
        String textToUse = drawing.getText();
        boolean isTextShape = shapeSelected && Objects.equals(shape.getClass().getSimpleName(), "Text");
        boolean isPictureShape = shapeSelected && Objects.equals(shape.getClass().getSimpleName(), "Picture");

        if (isTextShape) {
            fontToUse = shape.getFont() != null ? shape.getFont() : fontToUse;
            textToUse = shape.getText() != null ? shape.getText() : textToUse;
        } else {
            // If not a text shape, potentially use global defaults or clear/disable fields
            textToUse = ""; // Show empty if not text shape
            // Keep global font defaults visible but potentially disabled by isCellEditable
            fontToUse = drawing.getFont();
        }

        updateStringProperty("Text", textToUse);
        updateStringProperty("Font Family", fontToUse != null ? fontToUse.getFamily() : "SansSerif");
        updateSelectionProperty("Font Style", fontToUse != null ? fontToUse.getStyle() : Font.PLAIN);
        updateIntegerProperty("Font Size", fontToUse != null ? fontToUse.getSize() : 12);

        // Update ActionProperty's action and potentially state
        Property<?> imageProp = propertyMap.get("Image");
        if (imageProp instanceof ActionProperty) {
            ActionProperty actionProp = (ActionProperty) imageProp;
            // Update the action to use the current appService instance
            actionProp.setValue(() -> {
                if (appService != null && shapeSelected) { // Only allow change if shape selected
                    appService.setImageFileename(); // Call service method to open dialog
                } else {
                    System.err.println("Change Image action: AppService not available or no shape selected.");
                }
            });
            // Consider enabling/disabling the button component itself here based on isPictureShape
            int rowIndex = getPropertyRowIndex("Image");
            if (rowIndex >= 0) {
                Component editorComp = getCellEditor(rowIndex, 1).getTableCellEditorComponent(this, null, true, rowIndex, 1);
                Component rendererComp = getCellRenderer(rowIndex, 1).getTableCellRendererComponent(this, null, true, false, rowIndex, 1);
                editorComp.setEnabled(isPictureShape);
                rendererComp.setEnabled(isPictureShape);
            }

        }


        // --- Refresh Table UI ---
        if (getModel() instanceof PropertyModel) {
            PropertyModel model = (PropertyModel) getModel();
            model.fireTableDataChanged();
        } else {
            repaint(); // Fallback repaint
        }


        isPopulating = false; // Re-enable listener updates
    }

    // --- Helper methods to update specific property types ---
    private <T> void updateProperty(String name, T value) {
        Property<?> p = propertyMap.get(name);
        if (p != null) {
            try {
                // Avoid ClassCastException if types mismatch somehow
                if (p.getValue() == null || value == null || p.getValue().getClass().isAssignableFrom(value.getClass())) {
                    ((Property<T>) p).setValue(value);
                } else {
                    System.err.println("Type mismatch prevented update for property '" + name + "'. Expected " + p.getValue().getClass() + ", got " + value.getClass());
                }
            } catch (ClassCastException e) {
                System.err.println("Error updating property '" + name + "': Type mismatch. Value: " + value + ", Error: " + e.getMessage());
            }
        } else {
            System.err.println("Warning: Property '" + name + "' not found in propertyMap for update.");
        }
    }

    private void updateStringProperty(String name, String value) {
        updateProperty(name, value != null ? value : ""); // Default to empty string
    }

    private void updateIntegerProperty(String name, int value) {
        updateProperty(name, value);
    }

    private void updateBooleanProperty(String name, boolean value) {
        updateProperty(name, value);
    }

    private void updateColorProperty(String name, Color value) {
        updateProperty(name, value != null ? value : Color.GRAY); // Default color if null
    }

    private void updateSelectionProperty(String name, Object value) {
        Property<?> p = propertyMap.get(name);
        if (p instanceof SelectionProperty) {
            SelectionProperty sp = (SelectionProperty) p;
            // Find the Item corresponding to the value
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
                    sp.setValue(matchingItem.getValue()); // Update property model value
                    // Visually update the combo box in the table
                    int rowIndex = getPropertyRowIndex(name);
                    if (rowIndex >= 0) {
                        // Stop any active editing first
                        if (isEditing() && getEditingRow() == rowIndex && getEditingColumn() == 1) {
                            getCellEditor(rowIndex, 1).stopCellEditing();
                        }
                        // Update the renderer's value (which is often the editor itself)
                        TableCellRenderer renderer = getCellRenderer(rowIndex, 1);
                        if (renderer instanceof SelectionCellComponent) {
                            ((SelectionCellComponent)renderer).setCellEditorValue(matchingItem); // Update combo box selection
                        }
                        TableCellEditor editor = getCellEditor(rowIndex, 1);
                        if (editor instanceof SelectionCellComponent) {
                            ((SelectionCellComponent)editor).setCellEditorValue(matchingItem); // Ensure editor also has right item
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error setting selection property '" + name + "': " + e.getMessage());
                }
            } else {
                System.err.println("Warning: Could not find matching item for value '" + value + "' in SelectionProperty '" + name + "'");
            }
        }
    }

    // Helper to find the row index of a property by name
    private int getPropertyRowIndex(String name) {
        for(int i = 0; i < getRowCount(); i++) {
            Object propName = getValueAt(i, 0); // Property name is in column 0
            if (name.equals(propName)) {
                return i;
            }
        }
        return -1; // Not found
    }

    // --- Control Editability ---
    @Override
    public boolean isCellEditable(int row, int column) {
        // Only allow editing the value column (column 1)
        if (column != 1) {
            return false;
        }

        // Get the property name for the row
        Object propNameObj = getValueAt(row, 0);
        if (!(propNameObj instanceof String)) return false;
        String propName = (String) propNameObj;

        // Find the Property object
        Property<?> prop = propertyMap.get(propName);
        if (prop == null) return false;

        // Never allow editing Object Type
        if (propName.equals("Object Type")) return false;
        // Button handles its own action, cell isn't 'editable' in text sense
        if (prop instanceof ActionProperty) return true; // Let the button component handle enable/disable

        // --- Logic based on selection state ---
        if (!editingEnabled) { // No shape selected
            // Allow editing only global defaults
            return propName.equals("Fore Color") || propName.equals("Fill Color") ||
                    propName.equals("Start Color") || propName.equals("End Color") ||
                    propName.equals("Use Gradient") || propName.equals("Line Thickness") ||
                    propName.equals("Text") || propName.equals("Font Family") ||
                    propName.equals("Font Style") || propName.equals("Font Size");
        } else { // Shape is selected
            // Disable Text/Font properties if the selected shape isn't Text
            // Need access to AppService or shape type info here. Postpone this check.
            // TODO: Add check for shape type to disable Text/Font/Image for non-applicable shapes

            return true; // Allow editing most things when shape is selected
        }
    }

    // Override prepareRenderer/Editor to visually indicate disabled state
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        // Set enabled state based on editability rules
        c.setEnabled(isCellEditable(row, column));
        // Optional: Change background for non-editable rows/cells for clarity
        if (!isCellEditable(row, column) && column == 1) {
            c.setBackground(Color.LIGHT_GRAY); // Example disabled background
        } else {
            c.setBackground(getBackground()); // Use default background
        }
        return c;
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component c = super.prepareEditor(editor, row, column);
        // Ensure editor component also reflects enabled state
        c.setEnabled(isCellEditable(row, column));
        return c;
    }
}

