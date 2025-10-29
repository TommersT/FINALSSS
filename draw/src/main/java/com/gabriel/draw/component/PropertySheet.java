// tommerst/finalsss/FINALSSS-ab98287e5a251ea0f90f45adc2a537ebda4a1511/draw/src/main/java/com/gabriel/draw/component/PropertySheet.java
// Updated to handle real-time updates, editability, object type, and remove Image rows.
// Added missing CommandService import.
// Fixed lambda variable capture error.
package com.gabriel.draw.component;

// --- Ensure ALL necessary imports are present ---
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import com.gabriel.property.PropertyModel;
import com.gabriel.property.PropertyOptions;
import com.gabriel.property.PropertyPanel;
import com.gabriel.property.property.*;
import com.gabriel.property.property.selection.Item;
import com.gabriel.property.property.selection.SelectionProperty;
import com.gabriel.draw.model.Picture; // Import Picture model
import com.gabriel.drawfx.command.CommandService; // Import CommandService

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class PropertySheet extends PropertyPanel { // Extends PropertyPanel

    // Map to quickly access Property objects by name for value updates
    private final Map<String, Property<?>> propertyMap = new HashMap<>();

    // --- Items for SelectionProperty (Font Style) ---
    private final Item<Integer> PlainItem = new Item<>(Font.PLAIN, "Plain");
    private final Item<Integer> BoldItem = new Item<>(Font.BOLD, "Bold");
    private final Item<Integer> ItalicItem = new Item<>(Font.ITALIC, "Italic");
    private final Item<Integer> BoldItalicItem = new Item<>(Font.BOLD | Font.ITALIC, "Bold Italic");
    private final ArrayList<Item<Integer>> fontStyleItems = new ArrayList<>(Arrays.asList(
            PlainItem, BoldItem, ItalicItem, BoldItalicItem
    ));
    // --- End Items ---

    private boolean isPopulating = false; // Flag to prevent event loops
    private boolean editingEnabled = false; // Controls general editability based on selection


    // *** CONSTRUCTOR: Initializes STRUCTURE ***
    public PropertySheet(PropertyOptions options) {
        super(options); // Creates table, empty model, sets basic options
        initializeProperties(); // Adds rows/structure to the model
        configureColumns();     // Configures columns based on the model structure
    }

    // *** Initializes the PROPERTIES and adds ROWS to the model ***
    private void initializeProperties() {
        propertyMap.clear(); // Clear map before rebuilding
        if (propertyModel != null) propertyModel.clear(); // Clear existing rows

        // Add properties one by one using the helper
        // These properties define the structure of the table
        addInternalProperty(new StringProperty("Object Type", "None"));
        addInternalProperty(new ColorProperty("Fore Color", Color.BLACK));
        addInternalProperty(new ColorProperty("Fill Color", Color.WHITE));
        addInternalProperty(new ColorProperty("Start Color", Color.LIGHT_GRAY));
        addInternalProperty(new ColorProperty("End Color", Color.DARK_GRAY));
        addInternalProperty(new BooleanProperty("Use Gradient", false));
        addInternalProperty(new BooleanProperty("Visible", true));
        addInternalProperty(new IntegerProperty("X Location", 0)); // <<< ADDED
        addInternalProperty(new IntegerProperty("Y Location", 0)); // <<< ADDED
        addInternalProperty(new IntegerProperty("Width", 0));      // <<< ADDED
        addInternalProperty(new IntegerProperty("Height", 0));     // <<< ADDED
        addInternalProperty(new IntegerProperty("Line Thickness", 1));
        addInternalProperty(new StringProperty("Text", ""));
        addInternalProperty(new StringProperty("Font Family", "SansSerif"));
        addInternalProperty(new SelectionProperty<>("Font Style", fontStyleItems));
        addInternalProperty(new IntegerProperty("Font Size", 12));

        // Removed "Image" ActionProperty
        // Removed "Remove Image" ActionProperty
    }

    // Helper to add property to map AND call superclass to add row/component
    private void addInternalProperty(Property<?> prop) {
        if (prop == null || propertyModel == null) return;
        propertyMap.put(prop.getName(), prop);
        try {
            // This calls super.addProperty(Property), which finds/creates
            // the cell component and adds the row to the model.
            super.addProperty(prop);
        } catch (Exception e) {
            System.err.println("Error adding internal property '" + prop.getName() + "': " + e.getMessage());
            e.printStackTrace();
            // Add a placeholder row in case of error
            propertyModel.addRow(new Object[]{prop.getName(), "[Add Error]"});
        }
    }

    // Configures column appearance
    private void configureColumns() {
        if (getModel() == null || getColumnModel().getColumnCount() < 2) {
            System.err.println("PropertySheet configureColumns: Model/Columns not ready.");
            return;
        }
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        try {
            TableColumn propertyColumn = getColumnModel().getColumn(0);
            propertyColumn.setPreferredWidth(100);
            propertyColumn.setMinWidth(80);
            TableColumn valueColumn = getColumnModel().getColumn(1);
            valueColumn.setPreferredWidth(160); // More space for values
            valueColumn.setMinWidth(100);
        } catch (Exception e) {
            System.err.println("Error configuring columns: " + e.getMessage());
        }
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN); // Allow last column to resize
    }

    // Override clear to rebuild the structure
    @Override
    public void clear() {
        if (isEditing()) getCellEditor().stopCellEditing();
        cellComponents.clear();
        properties.clear();
        propertyMap.clear();
        if (propertyModel != null) propertyModel.clear();
        initializeProperties(); // Rebuild structure
        configureColumns();     // Reconfigure columns
        if (propertyModel != null) propertyModel.fireTableStructureChanged();
    }

    // *** METHOD DEFINITION for setEditingEnabled ***
    public void setEditingEnabled(boolean enabled) {
        if (this.editingEnabled == enabled) return; // Avoid unnecessary updates
        this.editingEnabled = enabled;
        // Force refresh of table structure and potentially cell states
        if (getModel() instanceof PropertyModel) {
            // Stop editing before refreshing to avoid errors
            if (isEditing()) {
                getCellEditor().stopCellEditing();
            }
            ((PropertyModel) getModel()).fireTableDataChanged(); // Notify cells need rerender
        } else {
            repaint(); // Fallback repaint
        }
    }


    // *** POPULATE TABLE: Updates VALUES only, called frequently ***
    public void populateTable(AppService appService) {
        if (appService == null) {
            System.err.println("PropertySheet.populateTable: appService is null.");
            return;
        }
        // Basic check if structure is ready
        if (propertyMap.isEmpty() || getRowCount() != propertyMap.size()) {
            System.err.println("PropertySheet.populateTable: Property structure not initialized or inconsistent (Rows: "+getRowCount()+", Map: "+propertyMap.size()+"). Re-initializing.");
            clear();
            if (propertyMap.isEmpty()) { // Check again after clear
                System.err.println("PropertySheet.populateTable: Re-initialization failed. Cannot populate.");
                return; // Cannot proceed if structure is broken
            }
        }

        isPopulating = true; // Prevent event loops during direct value setting

        Shape shape = appService.getSelectedShape(); // Get the primary selected shape
        Drawing drawing = appService.getDrawing(); // Get the drawing model for defaults
        boolean shapeSelected = (shape != null);
        String objectType = shapeSelected ? shape.getClass().getSimpleName() : "Drawing"; // Get class name or "Drawing"

        // *** Update general editability based on selection ***
        setEditingEnabled(shapeSelected); // Enable/disable editing based on whether a shape is selected

        // --- Update Values using helper methods ---
        // These helpers update the internal Property objects and the table model visually
        try {
            // *** Update Object Type ***
            updateStringProperty("Object Type", objectType); // <<< UPDATED

            // Update Colors (use shape's if selected, otherwise drawing defaults)
            updateColorProperty("Fore Color", shapeSelected ? shape.getColor() : drawing.getColor());
            updateColorProperty("Fill Color", shapeSelected ? shape.getFill() : drawing.getFill());
            updateColorProperty("Start Color", shapeSelected ? shape.getStartColor() : drawing.getStartColor());
            updateColorProperty("End Color", shapeSelected ? shape.getEndColor() : drawing.getEndColor());

            // Update Booleans
            updateBooleanProperty("Use Gradient", shapeSelected ? shape.isUseGradient() : drawing.isUseGradient());
            updateBooleanProperty("Visible", shapeSelected ? shape.isVisible() : true); // Use shape visibility if selected

            // *** Update Location and Size (use shape's if selected, otherwise 0) ***
            updateIntegerProperty("X Location", shapeSelected && shape.getLocation() != null ? shape.getLocation().x : 0); // <<< UPDATED
            updateIntegerProperty("Y Location", shapeSelected && shape.getLocation() != null ? shape.getLocation().y : 0); // <<< UPDATED
            updateIntegerProperty("Width", shapeSelected ? shape.getWidth() : 0);           // <<< UPDATED
            updateIntegerProperty("Height", shapeSelected ? shape.getHeight() : 0);          // <<< UPDATED

            // Update Thickness
            updateIntegerProperty("Line Thickness", shapeSelected ? shape.getThickness() : drawing.getThickness());

            // --- Text/Font properties ---
            Font fontToUse = drawing.getFont(); // Default to drawing font
            String textToUse = ""; // Default text
            boolean isTextShape = "Text".equals(objectType); // Check if it's a Text shape

            if (shapeSelected) { // If a shape is selected
                if (isTextShape) { // And it's a Text shape
                    fontToUse = shape.getFont() != null ? shape.getFont() : fontToUse; // Use its font or default
                    textToUse = shape.getText() != null ? shape.getText() : "";       // Use its text or empty
                }
                // For non-text shapes, stick to the drawing's default font for display in table
                else {
                    fontToUse = drawing.getFont();
                }
            } else { // If no shape is selected
                fontToUse = drawing.getFont(); // Use global default font
            }

            // Update Text/Font related properties
            updateStringProperty("Text", textToUse); // Show shape text or empty
            updateStringProperty("Font Family", fontToUse != null ? fontToUse.getFamily() : "SansSerif");
            updateSelectionProperty("Font Style", fontToUse != null ? fontToUse.getStyle() : Font.PLAIN);
            updateIntegerProperty("Font Size", fontToUse != null ? fontToUse.getSize() : 12);

        } catch (Exception ex) {
            System.err.println("Exception during PropertySheet.populateTable value updates:");
            ex.printStackTrace(); // Print stack trace for debugging
        } finally {
            isPopulating = false; // <<< Ensure this is ALWAYS reset, even if errors occur
        }

        // --- Refresh Table UI ---
        // Refresh the table display if not currently being populated internally
        // Use invokeLater to ensure it happens after any pending AWT events if needed,
        // but often direct update is fine if populateTable is called from EDT.
        if (!isPopulating && getModel() instanceof PropertyModel) {
            // Check isPopulating again just in case
            // Use invokeLater for safety, especially if called during drag which might not be strictly on EDT
            SwingUtilities.invokeLater(() -> {
                if (getModel() != null) { // Check model again inside invokeLater
                    ((PropertyModel) getModel()).fireTableDataChanged(); // Refresh display
                }
            });
        }
    }


    // --- Helper methods to UPDATE values directly in properties and the table model ---
    // This is the core logic for updating a single property's value.
    private void updateValueInModel(String name, Object value) {
        // Prevent updates if called during internal population or triggered by command execution
        if (isPopulating || CommandService.isExecutingCommand()) { // <<< CORRECTED CHECK
            // System.out.println("Skipping updateValueInModel for "+name+" due to flag.");
            return; // Exit early to prevent loops or redundant updates
        }

        Property<?> p = propertyMap.get(name); // Find the Property object by name
        if (p == null) {
            // This can happen if initializeProperties hasn't run correctly
            // System.err.println("updateValueInModel: Property not found in map: " + name);
            return;
        }

        int rowIndex = getPropertyRowIndex(name); // Find the row index for this property
        if (rowIndex == -1) {
            // Should not happen if map and table are in sync
            // System.err.println("updateValueInModel: Row index not found for property: " + name);
            return;
        }
        // <<< FIX: Create effectively final variable for lambda >>>
        final int finalRowIndex = rowIndex; // Make index final for use in lambda

        try {
            // Get current values from both the table model and the property object
            Object currentModelValue = propertyModel.getValueAt(finalRowIndex, 1);
            Object currentPropertyValue = p.getValue();

            // Check if the new value is actually different from EITHER the current model value OR the property value
            // This prevents unnecessary updates and potential event cascades
            if (!Objects.equals(currentModelValue, value) || !Objects.equals(currentPropertyValue, value)) {
                // System.out.println("Updating "+name+" from ["+currentModelValue+"/"+currentPropertyValue+"] to "+value);

                // --- Type compatibility check (basic) ---
                // Get the expected type from the Property object (e.g., Integer.class, Color.class)
                Class<?> propertyType = p.getClass().getDeclaredField("value").getType(); // Reflection to get type
                boolean typesCompatible = (value == null || propertyType.isAssignableFrom(value.getClass()) ||
                        (propertyType == int.class && value instanceof Integer) ||
                        (propertyType == boolean.class && value instanceof Boolean) /* add other primitives */);

                if (typesCompatible) {
                    // 1. Update the underlying Property object FIRST
                    // Use unchecked cast, ensure type safety via the property system
                    ((Property<Object>) p).setValue(value);

                    // 2. Then update the table model to reflect the change visually
                    // Use SwingUtilities if not on Event Dispatch Thread (EDT)
                    Runnable updateModelTask = () -> {
                        // Stop editing if the currently edited cell is the one being updated programmatically
                        // Use finalRowIndex inside lambda
                        if (isEditing() && getEditingRow() == finalRowIndex && getEditingColumn() == 1) {
                            // Try to stop editing gracefully
                            TableCellEditor editor = getCellEditor(finalRowIndex, 1);
                            if (editor != null) editor.stopCellEditing();
                        }
                        // Update the value in the JTable model
                        if (propertyModel != null && finalRowIndex < propertyModel.getRowCount()) { // Add safety check
                            propertyModel.setValueAt(value, finalRowIndex, 1); // Use finalRowIndex
                        }
                    };

                    // Execute the model update on the EDT
                    if (SwingUtilities.isEventDispatchThread()) {
                        updateModelTask.run();
                    } else {
                        SwingUtilities.invokeLater(updateModelTask);
                    }

                } else { // Type mismatch detected
                    System.err.println("Type mismatch prevented updateValueInModel for '" + name + "'. Expected compatible with " + propertyType + ", got " + (value != null ? value.getClass() : "null"));
                }
            } // else { System.out.println("Skipping update for "+name+" - value hasn't changed."); }
        } catch (NoSuchFieldException | SecurityException | IllegalStateException e) { // Catch reflection or table model exceptions
            System.err.println("Error in updateValueInModel for '" + name + "': " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
        }
    }


    // --- Specific type update helpers using the safe updateValueInModel ---
    // These just call the main update logic with type casting/defaults
    private void updateStringProperty(String name, String value) { updateValueInModel(name, value != null ? value : ""); }
    private void updateIntegerProperty(String name, int value) { updateValueInModel(name, value); }
    private void updateBooleanProperty(String name, boolean value) { updateValueInModel(name, value); }
    private void updateColorProperty(String name, Color value) { updateValueInModel(name, value != null ? value : Color.GRAY); } // Use gray default if null

    // Special handler for SelectionProperty (like Font Style)
    private void updateSelectionProperty(String name, Object valueToSelect) {
        Property<?> p = propertyMap.get(name);
        if (!(p instanceof SelectionProperty)) return; // Ensure it's the right type
        SelectionProperty sp = (SelectionProperty) p;
        Item<?> matchingItem = null;
        // Find the Item object that matches the value we want to select
        for (Object itemObj : sp.getItems()) {
            if (itemObj instanceof Item) {
                Item item = (Item) itemObj;
                if (Objects.equals(item.getValue(), valueToSelect)) {
                    matchingItem = item;
                    break;
                }
            }
        }
        // Determine the actual value to store in the Property (not the Item wrapper)
        Object valueForProperty = (matchingItem != null) ? matchingItem.getValue() : null;

        // 1. Update the underlying property value first (if needed)
        // Check CommandService flag using the imported class
        if (!isPopulating && !CommandService.isExecutingCommand()) { // <<< CORRECTED CHECK
            if (!Objects.equals(p.getValue(), valueForProperty)) {
                ((Property<Object>) p).setValue(valueForProperty); // Update internal property state
            }
        }

        // 2. Update the model (needs the Item object for ComboBox renderer/editor)
        int rowIndex = getPropertyRowIndex(name);
        if (rowIndex != -1) {
            // <<< FIX: Create effectively final variable for lambda >>>
            final int finalRowIndex = rowIndex;
            final Item<?> finalMatchingItem = matchingItem; // Also make item final for lambda

            Object currentModelValue = propertyModel.getValueAt(finalRowIndex, 1); // Use finalRowIndex
            // Compare Item objects for the model update
            if (!Objects.equals(currentModelValue, finalMatchingItem)) {
                Runnable updateModelTask = () -> {
                    // Use finalRowIndex inside lambda
                    if (isEditing() && getEditingRow() == finalRowIndex && getEditingColumn() == 1) {
                        TableCellEditor editor = getCellEditor(finalRowIndex, 1);
                        if (editor != null) editor.stopCellEditing();
                    }
                    // Update model with the Item object
                    if (propertyModel != null && finalRowIndex < propertyModel.getRowCount()) { // Safety check
                        // Use finalMatchingItem inside lambda
                        propertyModel.setValueAt(finalMatchingItem, finalRowIndex, 1); // Set Item in model
                    }
                };
                // Run on EDT
                if (SwingUtilities.isEventDispatchThread()) updateModelTask.run();
                else SwingUtilities.invokeLater(updateModelTask);
            }
        }
    }


    // Helper to find row index by property name
    private int getPropertyRowIndex(String name) {
        if (name == null || propertyModel == null) return -1;
        for (int i = 0; i < propertyModel.getRowCount(); i++) {
            Object nameInRow = propertyModel.getValueAt(i, 0); // Name is in column 0
            if (name.equals(nameInRow)) {
                return i; // Found it
            }
        }
        // Should not happen if map and table are synced
        // System.err.println("getPropertyRowIndex: Row not found for property: " + name);
        return -1; // Not found
    }

    // --- isCellEditable logic ---
    // Determines which cells can be edited based on selection state and property type
    @Override
    public boolean isCellEditable(int row, int column) {
        if (column != 1) return false; // Only value column (column 1) is potentially editable
        if (row < 0 || row >= getRowCount()) return false; // Invalid row index

        Object propNameObj = getValueAt(row, 0); // Get property name from column 0
        if (!(propNameObj instanceof String)) return false; // Should always be a string
        String propName = (String) propNameObj;

        // *** Object Type is NEVER editable ***
        if ("Object Type".equals(propName)) return false; // <<< Read-only

        // If editing is generally disabled (e.g., no shape selected), allow only global defaults
        if (!editingEnabled) {
            // List of properties that represent global defaults
            return "Fore Color".equals(propName) || "Fill Color".equals(propName) ||
                    "Start Color".equals(propName) || "End Color".equals(propName) ||
                    "Use Gradient".equals(propName) || "Line Thickness".equals(propName) ||
                    "Font Family".equals(propName) || "Font Style".equals(propName) || "Font Size".equals(propName);
        }

        // --- Shape-Specific Editability (when a shape IS selected) ---
        String currentObjectType = getStringValueFromModel("Object Type"); // Get current type being displayed
        boolean isTextShape = "Text".equals(currentObjectType);
        boolean isPictureShape = "Picture".equals(currentObjectType); // Assuming Picture class exists

        // Text/Font properties are only editable if the selected shape is a Text shape
        if (!isTextShape && ("Text".equals(propName) || "Font Family".equals(propName) || "Font Style".equals(propName) || "Font Size".equals(propName))) {
            return false;
        }

        // Visible property should be editable whenever a shape is selected
        if ("Visible".equals(propName)) {
            return editingEnabled; // Enabled if any shape is selected (editingEnabled == true)
        }

        // *** X, Y, Width, Height are editable when a single shape is selected ***
        if ("X Location".equals(propName) || "Y Location".equals(propName) ||
                "Width".equals(propName) || "Height".equals(propName)) {
            // For now, allow editing if *any* shape is selected (editingEnabled is true)
            // Might refine later for multi-selection (show average? disable?)
            return editingEnabled; // <<< Editable if a shape is selected
        }


        // All other relevant properties are generally editable when a shape is selected
        return true;
    }


    // Helper to get string value from model safely
    private String getStringValueFromModel(String propertyName) {
        int rowIndex = getPropertyRowIndex(propertyName);
        if (rowIndex != -1 && propertyModel != null && rowIndex < propertyModel.getRowCount()) {
            Object val = propertyModel.getValueAt(rowIndex, 1); // Value is in column 1
            return (val != null) ? val.toString() : null; // Return string representation or null
        }
        return null; // Not found or model invalid
    }

    // --- prepareRenderer / prepareEditor (Visuals for disabled/read-only cells) ---
    // Controls how cells look (e.g., grayed out if not editable)
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (c == null) return null;

        boolean editable = isCellEditable(row, column); // Check if this specific cell is editable
        boolean rowIsSelectedInTable = isRowSelected(row); // Check if the JTable row itself is selected (UI selection)

        Color background;
        Color foreground;

        // Determine colors based on table selection state and cell editability
        if (rowIsSelectedInTable) { // If the table row is highlighted
            background = getSelectionBackground();
            foreground = getSelectionForeground();
            // Special case: Make non-editable value cells slightly darker when selected
            if (!editable && column == 1) {
                background = getSelectionBackground().darker();
                foreground = Color.LIGHT_GRAY; // Lighter text for disabled selected cells
            }
        } else { // If the table row is NOT highlighted
            if (!editable && column == 1) { // Value column, not editable, not selected
                background = getBackground().darker(); // Use a slightly darker background for disabled cells
                foreground = Color.GRAY; // Gray out the text
            } else { // Editable value cell or property name column
                background = getBackground(); // Default background
                foreground = getForeground(); // Default foreground
            }
        }

        // Apply colors and enabled state
        c.setBackground(background);
        c.setForeground(foreground);
        // Enable component visually if it's editable OR if it's the property name column (col 0)
        c.setEnabled(editable || column == 0);

        // Ensure checkboxes render correctly when disabled
        if (c instanceof JCheckBox) {
            ((JCheckBox)c).setOpaque(true); // Needed for background color to show reliably
            ((JCheckBox)c).setBackground(background); // Apply background
            ((JCheckBox)c).setEnabled(editable); // Explicitly set checkbox enabled state based on editability
        }
        // Explicitly gray out JLabel text when disabled (redundant but safe)
        if (c instanceof JLabel && !editable && column == 1 && !rowIsSelectedInTable) {
            c.setForeground(Color.GRAY);
        }

        return c;
    }


    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component c = super.prepareEditor(editor, row, column);
        if (c == null) return null;

        boolean editable = isCellEditable(row, column); // Check editability
        c.setEnabled(editable); // Ensure editor component respects editability

        // Make editor background match renderer background for consistency
        if (!editable) {
            // Use disabled appearance
            c.setBackground(getBackground().darker());
            c.setForeground(Color.GRAY);
        } else {
            // Use selection colors while editing for better focus indication
            c.setBackground(getSelectionBackground());
            c.setForeground(getSelectionForeground());
        }

        return c;
    }

    // --- Tooltip ---
    // Provides tooltips based on cell content
    // Suppress unchecked warning for SelectionProperty cast
    @SuppressWarnings("unchecked")
    @Override
    public String getToolTipText(java.awt.event.MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        int col = columnAtPoint(event.getPoint());
        // Check bounds and ensure properties list matches row count
        if (row >= 0 && col >= 0 && row < getRowCount() && properties != null && row < properties.size()) {
            if (col == 0) { // Column 0: Property Name
                Object propNameObj = getValueAt(row, 0);
                if (propNameObj instanceof String) return (String) propNameObj; // Tooltip is the name itself
            } else if (col == 1) { // Column 1: Property Value
                Object value = getValueAt(row, 1);
                Property<?> p = properties.get(row); // Get corresponding Property object
                // For selection property, show the description from the Item
                if (p instanceof SelectionProperty && value instanceof Item) {
                    // Cast is safe due to instanceof check
                    return ((Item<?>) value).getDescription(); // Show description like "Bold", "Italic"
                }
                // For other properties, show the string representation of the value
                if (value != null) return value.toString();
            }
        }
        return super.getToolTipText(event); // Default tooltip behavior
    }
}