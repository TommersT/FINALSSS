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
        addInternalProperty(new StringProperty("Object Type", "None"));
        addInternalProperty(new ColorProperty("Fore Color", Color.BLACK));
        addInternalProperty(new ColorProperty("Fill Color", Color.WHITE));
        addInternalProperty(new ColorProperty("Start Color", Color.LIGHT_GRAY));
        addInternalProperty(new ColorProperty("End Color", Color.DARK_GRAY));
        addInternalProperty(new BooleanProperty("Use Gradient", false));
        addInternalProperty(new BooleanProperty("Visible", true)); // Keep Visible
        addInternalProperty(new IntegerProperty("X Location", 0));
        addInternalProperty(new IntegerProperty("Y Location", 0));
        addInternalProperty(new IntegerProperty("Width", 0));
        addInternalProperty(new IntegerProperty("Height", 0));
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
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    }

    // Override clear to rebuild the structure
    @Override
    public void clear() {
        if (isEditing()) getCellEditor().stopCellEditing();
        cellComponents.clear();
        properties.clear();
        propertyMap.clear();
        if (propertyModel != null) propertyModel.clear();
        initializeProperties();
        configureColumns();
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


    // *** POPULATE TABLE: Updates VALUES only ***
    public void populateTable(AppService appService) {
        if (appService == null) {
            System.err.println("PropertySheet.populateTable: appService is null.");
            return;
        }
        if (propertyMap.isEmpty() || getRowCount() != propertyMap.size()) {
            System.err.println("PropertySheet.populateTable: Property structure not initialized or inconsistent (Rows: "+getRowCount()+", Map: "+propertyMap.size()+"). Re-initializing.");
            clear();
            if (propertyMap.isEmpty()) {
                System.err.println("PropertySheet.populateTable: Re-initialization failed. Cannot populate.");
                return;
            }
        }

        isPopulating = true; // Prevent event loops during direct value setting

        Shape shape = appService.getSelectedShape();
        Drawing drawing = appService.getDrawing();
        boolean shapeSelected = (shape != null);
        String objectType = shapeSelected ? shape.getClass().getSimpleName() : "Drawing";

        // *** Update general editability based on selection ***
        setEditingEnabled(shapeSelected);

        // --- Update Values using helper methods ---
        try {
            updateStringProperty("Object Type", objectType);
            updateColorProperty("Fore Color", shapeSelected ? shape.getColor() : drawing.getColor());
            updateColorProperty("Fill Color", shapeSelected ? shape.getFill() : drawing.getFill());
            updateColorProperty("Start Color", shapeSelected ? shape.getStartColor() : drawing.getStartColor());
            updateColorProperty("End Color", shapeSelected ? shape.getEndColor() : drawing.getEndColor());
            updateBooleanProperty("Use Gradient", shapeSelected ? shape.isUseGradient() : drawing.isUseGradient());
            updateBooleanProperty("Visible", shapeSelected ? shape.isVisible() : true); // Use shape visibility

            updateIntegerProperty("X Location", shapeSelected && shape.getLocation() != null ? shape.getLocation().x : 0);
            updateIntegerProperty("Y Location", shapeSelected && shape.getLocation() != null ? shape.getLocation().y : 0);
            updateIntegerProperty("Width", shapeSelected ? shape.getWidth() : 0);
            updateIntegerProperty("Height", shapeSelected ? shape.getHeight() : 0);
            updateIntegerProperty("Line Thickness", shapeSelected ? shape.getThickness() : drawing.getThickness());

            // Text/Font properties
            Font fontToUse = drawing.getFont(); // Default to drawing font
            String textToUse = ""; // Default text
            boolean isTextShape = "Text".equals(objectType);

            if (shapeSelected) {
                if (isTextShape) {
                    fontToUse = shape.getFont() != null ? shape.getFont() : fontToUse;
                    textToUse = shape.getText() != null ? shape.getText() : "";
                }
                // For non-text shapes, we might still want to get a default font context
                else {
                    fontToUse = drawing.getFont(); // Stick to global default if not text shape
                }
            } else {
                fontToUse = drawing.getFont(); // Use global default font if nothing selected
            }


            updateStringProperty("Text", textToUse);
            updateStringProperty("Font Family", fontToUse != null ? fontToUse.getFamily() : "SansSerif");
            updateSelectionProperty("Font Style", fontToUse != null ? fontToUse.getStyle() : Font.PLAIN);
            updateIntegerProperty("Font Size", fontToUse != null ? fontToUse.getSize() : 12);

        } catch (Exception ex) {
            System.err.println("Exception during PropertySheet.populateTable value updates:");
            ex.printStackTrace();
        } finally {
            isPopulating = false; // <<< Ensure this is ALWAYS reset
        }

        // --- Refresh Table UI ---
        // Don't stop editing here, let the drag continue updating
        if (!isPopulating && getModel() != null) { // Check isPopulating again just in case
            ((PropertyModel) getModel()).fireTableDataChanged(); // Refresh display
        }
    }


    // --- Helper methods to UPDATE values directly in properties and the table model ---
    private void updateValueInModel(String name, Object value) {
        // Prevent updates if called during internal population or from listener feedback
        // Check CommandService flag using the imported class
        if (isPopulating || CommandService.isExecutingCommand()) return; // <<< CORRECTED CHECK

        Property<?> p = propertyMap.get(name);
        if (p == null) {
            // System.err.println("updateValueInModel: Property not found in map: " + name);
            return;
        }

        int rowIndex = getPropertyRowIndex(name);
        if (rowIndex == -1) {
            // System.err.println("updateValueInModel: Row index not found for property: " + name);
            return;
        }
        // <<< FIX: Create effectively final variable for lambda >>>
        final int finalRowIndex = rowIndex;

        try {
            Object currentModelValue = propertyModel.getValueAt(finalRowIndex, 1); // Use finalRowIndex
            Object currentPropertyValue = p.getValue();

            // Check if the new value is actually different from both model and property
            if (!Objects.equals(currentModelValue, value) || !Objects.equals(currentPropertyValue, value)) {
                // Check type compatibility before setting
                Class<?> propertyType = p.getClass().getDeclaredField("value").getType();
                if (value == null || propertyType.isAssignableFrom(value.getClass()) ||
                        (propertyType == int.class && value instanceof Integer) ||
                        (propertyType == boolean.class && value instanceof Boolean) /* add other primitives */)
                {
                    // Update the underlying Property object FIRST
                    // Use unchecked cast, ensure type safety via the property system
                    ((Property<Object>) p).setValue(value);

                    // Then update the table model to reflect the change visually
                    // Use SwingUtilities if called from non-EDT (though populateTable is usually EDT)
                    Runnable updateModelTask = () -> {
                        // Stop editing if the currently edited cell is the one being updated
                        // Use finalRowIndex inside lambda
                        if (isEditing() && getEditingRow() == finalRowIndex && getEditingColumn() == 1) {
                            getCellEditor(finalRowIndex, 1).stopCellEditing();
                        }
                        propertyModel.setValueAt(value, finalRowIndex, 1); // Use finalRowIndex
                    };

                    if (SwingUtilities.isEventDispatchThread()) {
                        updateModelTask.run();
                    } else {
                        SwingUtilities.invokeLater(updateModelTask);
                    }

                } else {
                    System.err.println("Type mismatch prevented updateValueInModel for '" + name + "'. Expected compatible with " + propertyType + ", got " + value.getClass());
                }
            }
        } catch (NoSuchFieldException | SecurityException | IllegalStateException e) { // Catch table model exceptions too
            System.err.println("Error in updateValueInModel for '" + name + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Specific type update helpers using the safe updateValueInModel ---
    private void updateStringProperty(String name, String value) { updateValueInModel(name, value != null ? value : ""); }
    private void updateIntegerProperty(String name, int value) { updateValueInModel(name, value); }
    private void updateBooleanProperty(String name, boolean value) { updateValueInModel(name, value); }
    private void updateColorProperty(String name, Color value) { updateValueInModel(name, value != null ? value : Color.GRAY); }
    private void updateSelectionProperty(String name, Object valueToSelect) {
        Property<?> p = propertyMap.get(name);
        if (!(p instanceof SelectionProperty)) return;
        SelectionProperty sp = (SelectionProperty) p;
        Item<?> matchingItem = null;
        for (Object itemObj : sp.getItems()) {
            if (itemObj instanceof Item) {
                Item item = (Item) itemObj;
                if (Objects.equals(item.getValue(), valueToSelect)) {
                    matchingItem = item;
                    break;
                }
            }
        }
        // Update model with the *Item object* itself for ComboBox, but update underlying Property with *value*
        Object valueForProperty = (matchingItem != null) ? matchingItem.getValue() : null;

        // Update the underlying property value first
        // Check CommandService flag using the imported class
        if (!isPopulating && !CommandService.isExecutingCommand()) { // <<< CORRECTED CHECK
            if (!Objects.equals(p.getValue(), valueForProperty)) {
                ((Property<Object>) p).setValue(valueForProperty);
            }
        }

        // Update the model (needs the Item for renderer/editor)
        int rowIndex = getPropertyRowIndex(name);
        if (rowIndex != -1) {
            // <<< FIX: Create effectively final variable for lambda >>>
            final int finalRowIndex = rowIndex;
            final Item<?> finalMatchingItem = matchingItem; // Also make item final

            Object currentModelValue = propertyModel.getValueAt(finalRowIndex, 1); // Use finalRowIndex
            if (!Objects.equals(currentModelValue, finalMatchingItem)) { // Compare Item objects for model update
                Runnable updateModelTask = () -> {
                    // Use finalRowIndex inside lambda
                    if (isEditing() && getEditingRow() == finalRowIndex && getEditingColumn() == 1) {
                        getCellEditor(finalRowIndex, 1).stopCellEditing();
                    }
                    // Use finalMatchingItem inside lambda
                    propertyModel.setValueAt(finalMatchingItem, finalRowIndex, 1); // Set Item in model
                };
                if (SwingUtilities.isEventDispatchThread()) updateModelTask.run();
                else SwingUtilities.invokeLater(updateModelTask);
            }
        }
    }


    // Helper to find row index
    private int getPropertyRowIndex(String name) {
        if (name == null || propertyModel == null) return -1;
        for (int i = 0; i < propertyModel.getRowCount(); i++) {
            Object nameInRow = propertyModel.getValueAt(i, 0);
            if (name.equals(nameInRow)) {
                return i;
            }
        }
        // System.err.println("getPropertyRowIndex: Row not found for property: " + name);
        return -1;
    }

    // --- isCellEditable logic ---
    @Override
    public boolean isCellEditable(int row, int column) {
        if (column != 1) return false; // Only value column is editable
        if (row < 0 || row >= getRowCount()) return false;
        Object propNameObj = getValueAt(row, 0);
        if (!(propNameObj instanceof String)) return false;
        String propName = (String) propNameObj;

        // Object Type is never editable
        if ("Object Type".equals(propName)) return false;

        // If editing is generally disabled (no shape selected), allow only global defaults
        if (!editingEnabled) {
            return "Fore Color".equals(propName) || "Fill Color".equals(propName) ||
                    "Start Color".equals(propName) || "End Color".equals(propName) ||
                    "Use Gradient".equals(propName) || "Line Thickness".equals(propName) ||
                    "Font Family".equals(propName) || "Font Style".equals(propName) || "Font Size".equals(propName);
        }

        // --- Shape-Specific Editability ---
        String currentObjectType = getStringValueFromModel("Object Type");
        boolean isTextShape = "Text".equals(currentObjectType);
        boolean isPictureShape = "Picture".equals(currentObjectType);

        // Text/Font properties only editable for Text shapes
        if (!isTextShape && ("Text".equals(propName) || "Font Family".equals(propName) || "Font Style".equals(propName) || "Font Size".equals(propName))) {
            return false;
        }

        // Visible property should be editable whenever a shape is selected
        if ("Visible".equals(propName)) {
            return editingEnabled; // Enabled if any shape is selected
        }

        // All other properties are generally editable when a shape is selected
        return true;
    }


    // Helper to get string value from model
    private String getStringValueFromModel(String propertyName) {
        int rowIndex = getPropertyRowIndex(propertyName);
        if (rowIndex != -1 && propertyModel != null && rowIndex < propertyModel.getRowCount()) {
            Object val = propertyModel.getValueAt(rowIndex, 1);
            return (val != null) ? val.toString() : null;
        }
        return null;
    }

    // --- prepareRenderer / prepareEditor (Visuals for disabled cells) ---
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (c == null) return null;
        boolean editable = isCellEditable(row, column);
        boolean rowIsSelected = isRowSelected(row); // Check if the JTable row itself is selected

        Color background;
        Color foreground;

        if (rowIsSelected) {
            background = getSelectionBackground();
            foreground = getSelectionForeground();
            if (!editable && column == 1) { // Value column, not editable
                background = getSelectionBackground().darker(); // Darker background for disabled selected cells
                foreground = Color.LIGHT_GRAY; // Lighter text for disabled selected cells
            }
        } else {
            if (!editable && column == 1) { // Value column, not editable, not selected
                background = getBackground().darker(); // Use a slightly darker background
                foreground = Color.GRAY; // Gray out the text
            } else { // Editable cell or property name column
                background = getBackground();
                foreground = getForeground();
            }
        }

        c.setBackground(background);
        c.setForeground(foreground);
        c.setEnabled(editable || column == 0); // Always enable property name column

        // Ensure checkboxes render correctly when disabled
        if (c instanceof JCheckBox) {
            ((JCheckBox)c).setOpaque(true); // Needed for background color to show reliably
            ((JCheckBox)c).setBackground(background); // Apply background
            ((JCheckBox)c).setEnabled(editable); // Explicitly set checkbox enabled state
        }
        // Explicitly gray out JLabel text when disabled (redundant but safe)
        if (c instanceof JLabel && !editable && column == 1 && !rowIsSelected) {
            c.setForeground(Color.GRAY);
        }


        return c;
    }


    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component c = super.prepareEditor(editor, row, column);
        if (c == null) return null;
        boolean editable = isCellEditable(row, column);
        c.setEnabled(editable); // Ensure editor component respects editability

        // Make editor background match renderer background for consistency
        if (!editable) {
            c.setBackground(getBackground().darker());
            c.setForeground(Color.GRAY);
        } else {
            c.setBackground(getSelectionBackground()); // Use selection color when editing
            c.setForeground(getSelectionForeground());
        }

        return c;
    }

    // --- Tooltip ---
    // Suppress unchecked warning for SelectionProperty cast
    @SuppressWarnings("unchecked")
    @Override
    public String getToolTipText(java.awt.event.MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        int col = columnAtPoint(event.getPoint());
        if (row >= 0 && col >= 0 && row < getRowCount() && row < properties.size()) { // Add bounds check for properties list
            if (col == 0) {
                Object propNameObj = getValueAt(row, 0);
                if (propNameObj instanceof String) return (String) propNameObj;
            } else if (col == 1) {
                Object value = getValueAt(row, 1);
                // For selection property, show the description from the Item
                Property<?> p = properties.get(row); // Get property from the list
                if (p instanceof SelectionProperty && value instanceof Item) {
                    // Cast is safe due to instanceof check
                    return ((Item<?>) value).getDescription();
                }
                if (value != null) return value.toString();
            }
        }
        return super.getToolTipText(event);
    }
}