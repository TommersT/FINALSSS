package com.gabriel.draw.component;


import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import com.gabriel.property.PropertyModel;
import com.gabriel.property.PropertyOptions;
import com.gabriel.property.PropertyPanel;
import com.gabriel.property.property.*;
import com.gabriel.property.property.selection.Item;
import com.gabriel.property.property.selection.SelectionProperty;
import com.gabriel.drawfx.command.CommandService; // Correct import

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel; // <<<--- ADDED IMPORT
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
//import java.lang.reflect.Field; // Keep reflection if used elsewhere, remove if not
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

    private volatile boolean isPopulating = false; // Flag to prevent event loops during full population
    private volatile boolean isUpdatingFromController = false; // <<<--- NEW FLAG
    private boolean editingEnabled = false; // Controls general editability


    // *** CONSTRUCTOR: Initializes STRUCTURE ***
    public PropertySheet(PropertyOptions options) {
        super(options); // Base setup
        initializeProperties(); // Add rows/structure to the model
        configureColumns();     // Configure columns
    }

    // *** Initializes the PROPERTIES and adds ROWS to the model (Called ONCE) ***
    private void initializeProperties() {
        propertyMap.clear();
        if (propertyModel != null) propertyModel.clear();

        // Add core properties needed for synchronization and display
        addInternalProperty(new StringProperty("Object Type", "None")); // Read-only type display
        addInternalProperty(new ColorProperty("Fore Color", Color.BLACK));
        addInternalProperty(new ColorProperty("Fill Color", Color.WHITE));
        addInternalProperty(new ColorProperty("Start Color", Color.LIGHT_GRAY)); // For Gradient
        addInternalProperty(new ColorProperty("End Color", Color.DARK_GRAY));   // For Gradient
        addInternalProperty(new BooleanProperty("Use Gradient", false));
        addInternalProperty(new BooleanProperty("Visible", true));
        addInternalProperty(new IntegerProperty("X Location", 0)); // Editable X
        addInternalProperty(new IntegerProperty("Y Location", 0)); // Editable Y
        addInternalProperty(new IntegerProperty("Width", 0));      // Editable Width
        addInternalProperty(new IntegerProperty("Height", 0));     // Editable Height
        addInternalProperty(new IntegerProperty("Line Thickness", 1));
        addInternalProperty(new StringProperty("Text", "")); // For Text shapes
        addInternalProperty(new StringProperty("Font Family", "SansSerif")); // For Text shapes
        addInternalProperty(new SelectionProperty<>("Font Style", fontStyleItems)); // For Text shapes
        addInternalProperty(new IntegerProperty("Font Size", 12)); // For Text shapes

        // Add other properties if needed, ensuring they have corresponding logic
        // in populateTable, isCellEditable, and PropertyEventListener
    }

    // Helper to add property to map AND call superclass to add row/component
    private void addInternalProperty(Property<?> prop) {
        if (prop == null || propertyModel == null) return;
        propertyMap.put(prop.getName(), prop);
        try {
            // This calls super.addProperty(Property), adding the row and component
            super.addProperty(prop);
        } catch (Exception e) {
            System.err.println("Error adding internal property '" + prop.getName() + "': " + e.getMessage());
            e.printStackTrace();
            // Add a row indicating error to maintain row count consistency
            propertyModel.addRow(new Object[]{prop.getName(), "[Add Error]"});
        }
    }

    // Configures column appearance
    private void configureColumns() {
        if (getModel() == null || getColumnModel().getColumnCount() < 2) return;
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        try {
            TableColumn propertyColumn = getColumnModel().getColumn(0);
            propertyColumn.setPreferredWidth(100);
            propertyColumn.setMinWidth(80);
            TableColumn valueColumn = getColumnModel().getColumn(1);
            valueColumn.setPreferredWidth(160); // Adjusted for potentially wider values
            valueColumn.setMinWidth(100);
        } catch (Exception e) {
            System.err.println("Error configuring columns: " + e.getMessage());
        }
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN); // Allow last column to take extra space
    }

    // Override clear to rebuild the structure if needed (e.g., dynamic properties)
    @Override
    public void clear() {
        if (isEditing()) {
            TableCellEditor editor = getCellEditor();
            if (editor != null) editor.stopCellEditing();
        }
        cellComponents.clear();
        properties.clear();
        propertyMap.clear();
        if (propertyModel != null) propertyModel.clear();
        // Re-initialize structure only if properties might change fundamentally
        // For simple value updates, this full clear isn't needed by populateTable
        // initializeProperties(); // Optional: Uncomment if structure needs full reset
        // configureColumns();
        // if (propertyModel != null) propertyModel.fireTableStructureChanged();
    }

    // Enable/disable editing visually
    public void setEditingEnabled(boolean enabled) {
        if (this.editingEnabled == enabled) return;
        this.editingEnabled = enabled;
        if (getModel() instanceof PropertyModel) {
            if (isEditing()) {
                TableCellEditor editor = getCellEditor();
                if (editor != null) editor.stopCellEditing();
            }
            // Use invokeLater to ensure table redraw happens after current event processing
            SwingUtilities.invokeLater(() -> {
                if (getModel() != null) {
                    ((PropertyModel) getModel()).fireTableDataChanged(); // Redraw cells
                }
            });
        } else {
            repaint(); // Fallback repaint
        }
    }

    // <<<--- NEW GETTER for the flag --->>>
    public boolean isUpdatingFromController() {
        return isUpdatingFromController;
    }

    // *** POPULATE TABLE: Updates VALUES only, called frequently ***
    public void populateTable(AppService appService) {
        if (appService == null) return;
        // Ensure structure is initialized correctly before proceeding
        if (propertyMap.isEmpty() || getRowCount() != propertyMap.size()) {
            System.err.println("PropertySheet.populateTable: Structure mismatch. Re-initializing.");
            initializeProperties(); // Attempt to fix structure
            configureColumns();
            if (propertyMap.isEmpty() || getRowCount() != propertyMap.size()) {
                System.err.println("PropertySheet.populateTable: Initialization failed. Aborting population.");
                return; // Abort if still inconsistent
            }
        }

        isPopulating = true; // Prevent internal updates from causing loops

        Shape shape = appService.getSelectedShape();
        Drawing drawing = appService.getDrawing();
        boolean shapeSelected = (shape != null);
        String objectType = shapeSelected ? shape.getClass().getSimpleName() : "Drawing";

        setEditingEnabled(shapeSelected); // Enable/disable based on selection

        try {
            // Read current values from the service/model
            Color currentForeColor = shapeSelected ? shape.getColor() : drawing.getColor();
            Color currentFillColor = shapeSelected ? shape.getFill() : drawing.getFill();
            Color currentStartColor = shapeSelected ? shape.getStartColor() : drawing.getStartColor();
            Color currentEndColor = shapeSelected ? shape.getEndColor() : drawing.getEndColor();
            boolean currentUseGradient = shapeSelected ? shape.isUseGradient() : drawing.isUseGradient();
            boolean currentVisible = shapeSelected ? shape.isVisible() : true; // Default to true if no shape
            int currentX = shapeSelected && shape.getLocation() != null ? shape.getLocation().x : 0;
            int currentY = shapeSelected && shape.getLocation() != null ? shape.getLocation().y : 0;
            int currentWidth = shapeSelected ? shape.getWidth() : 0;
            int currentHeight = shapeSelected ? shape.getHeight() : 0;
            int currentThickness = shapeSelected ? shape.getThickness() : drawing.getThickness();

            Font fontToUse = drawing.getFont(); // Start with drawing default
            String textToUse = "";
            boolean isTextShape = "Text".equals(objectType);

            if (shapeSelected) {
                if (isTextShape) {
                    fontToUse = shape.getFont() != null ? shape.getFont() : fontToUse; // Prefer shape's font
                    textToUse = shape.getText() != null ? shape.getText() : "";
                }
                // For non-text shapes, font properties reflect the drawing default
            }
            // If no shape selected, fontToUse remains the drawing default

            // Handle potential null font before accessing properties
            String currentFontFamily = "SansSerif";
            int currentFontStyle = Font.PLAIN;
            int currentFontSize = 12;
            if (fontToUse != null) {
                currentFontFamily = fontToUse.getFamily();
                currentFontStyle = fontToUse.getStyle();
                currentFontSize = fontToUse.getSize();
            }


            // --- Update Property Objects AND Table Model Visually ---
            // Use the direct update helpers to avoid triggering listeners during population
            updateStringPropertyDirect("Object Type", objectType);
            updateColorPropertyDirect("Fore Color", currentForeColor);
            updateColorPropertyDirect("Fill Color", currentFillColor);
            updateColorPropertyDirect("Start Color", currentStartColor);
            updateColorPropertyDirect("End Color", currentEndColor);
            updateBooleanPropertyDirect("Use Gradient", currentUseGradient);
            updateBooleanPropertyDirect("Visible", currentVisible);

            // *** Use integer helper for X, Y, Width, Height ***
            updateIntegerPropertyDirect("X Location", currentX);
            updateIntegerPropertyDirect("Y Location", currentY);
            updateIntegerPropertyDirect("Width", currentWidth);
            updateIntegerPropertyDirect("Height", currentHeight);

            updateIntegerPropertyDirect("Line Thickness", currentThickness);
            updateStringPropertyDirect("Text", textToUse);
            updateStringPropertyDirect("Font Family", currentFontFamily);
            updateSelectionPropertyDirect("Font Style", currentFontStyle); // Use direct selection update
            updateIntegerPropertyDirect("Font Size", currentFontSize);

        } catch (Exception ex) {
            System.err.println("Exception during PropertySheet.populateTable value updates:");
            ex.printStackTrace();
        } finally {
            isPopulating = false; // Ensure flag is reset
        }
        // No full table refresh needed - individual updates handle it.
    }


    // --- Helper methods to UPDATE values directly in properties and the table model ---
    // Core update logic with EDT safety (Used by populateTable and real-time updates)
    // Renamed slightly to differentiate from potential public update methods
    private void updatePropertyDirectly(String name, Object value) {
        // Prevent updates if the sheet itself is populating OR if a command is running
        // Allow updates initiated by the controller (isUpdatingFromController flag)
        if (isPopulating || (!isUpdatingFromController && CommandService.isExecutingCommand())) {
            return; // Prevent loops
        }

        Property<?> p = propertyMap.get(name);
        if (p == null) { /*System.err.println("updateValueInModel: Prop not found: "+name);*/ return; }

        int rowIndex = getPropertyRowIndex(name);
        if (rowIndex == -1) { /*System.err.println("updateValueInModel: Row not found: "+name);*/ return; }

        final int finalRowIndex = rowIndex;

        try {
            Object currentModelValue = (propertyModel != null && finalRowIndex < propertyModel.getRowCount()) ? propertyModel.getValueAt(finalRowIndex, 1) : null;
            Object currentPropertyValue = p.getValue();

            // Only proceed if value changed
            if (!Objects.equals(currentModelValue, value) || !Objects.equals(currentPropertyValue, value)) {

                // Basic type check/conversion (important for integer/boolean wrappers)
                boolean typesCompatible = true;
                Object valueToSet = value; // Use this potentially converted value

                if (p.getValue() != null && value != null && !p.getValue().getClass().isInstance(value)) {
                    // Allow Integer property to accept Integer value, etc.
                    if (!((p.getValue() instanceof Integer && value instanceof Integer) ||
                            (p.getValue() instanceof Boolean && value instanceof Boolean) ||
                            (p.getValue() instanceof Color && value instanceof Color)
                            // Add other primitive/wrapper checks if needed
                    )) {
                        typesCompatible = false;
                        System.err.println("Type mismatch prevented updateValueInModel for '" + name + "'. Expected " + p.getValue().getClass() + " but got " + value.getClass());
                    }
                }


                if (typesCompatible) {
                    // 1. Update Property object
                    try {
                        ((Property<Object>) p).setValue(valueToSet);
                    } catch (ClassCastException e) {
                        System.err.println("Error casting value in updatePropertyDirectly (setValue) for " + name + ": " + e);
                        return; // Stop if type is wrong during setting
                    }


                    // 2. Update table model on EDT
                    Runnable updateModelTask = () -> {
                        TableModel model = getModel(); // Get model inside EDT task
                        if (model instanceof PropertyModel && finalRowIndex < model.getRowCount()) {
                            // Check if the cell is currently being edited *before* stopping it
                            if (isEditing() && getEditingRow() == finalRowIndex && getEditingColumn() == 1) {
                                TableCellEditor editor = getCellEditor(finalRowIndex, 1);
                                if (editor != null) {
                                    // Attempt to stop editing gracefully
                                    if (!editor.stopCellEditing()) {
                                        // If stop fails, cancel editing to prevent inconsistent state
                                        editor.cancelCellEditing();
                                    }
                                }
                            }
                            // Directly set the value in the table model
                            ((PropertyModel) model).setValueAt(valueToSet, finalRowIndex, 1);
                        }
                    };

                    if (SwingUtilities.isEventDispatchThread()) { updateModelTask.run(); }
                    else { SwingUtilities.invokeLater(updateModelTask); }

                }
            }
        } catch (Exception e) {
            System.err.println("Error in updateValueInModel for '" + name + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Specific type update helpers FOR POPULATION (call direct update) ---
    private void updateStringPropertyDirect(String name, String value) { updatePropertyDirectly(name, value != null ? value : ""); }
    private void updateIntegerPropertyDirect(String name, int value) { updatePropertyDirectly(name, value); }
    private void updateBooleanPropertyDirect(String name, boolean value) { updatePropertyDirectly(name, value); }
    private void updateColorPropertyDirect(String name, Color value) { updatePropertyDirectly(name, value != null ? value : Color.GRAY); }


    // <<<--- NEW: Specific update helper FOR REAL-TIME CONTROLLER UPDATES --->>>
    // Public method called by DrawingController during drag
    public void updateIntegerProperty(String name, int value) {
        updatePropertyDirectly(name, value);
    }
    // Add similar public methods if other types need real-time updates from controller
    // public void updateStringProperty(String name, String value) { updatePropertyDirectly(name, value); }


    // Special handler for SelectionProperty (like Font Style) - Direct Update for Population
    private void updateSelectionPropertyDirect(String name, Object valueToSelect) {
        Property<?> p = propertyMap.get(name);
        if (!(p instanceof SelectionProperty)) return;
        SelectionProperty<?> sp = (SelectionProperty<?>) p;
        Item<?> matchingItem = null;

        // Find the Item whose *value* matches the target value
        for (Object itemObj : sp.getItems()) {
            if (itemObj instanceof Item) {
                Item<?> item = (Item<?>) itemObj;
                if (Objects.equals(item.getValue(), valueToSelect)) {
                    matchingItem = item;
                    break;
                }
            }
        }
        Object valueForProperty = (matchingItem != null) ? matchingItem.getValue() : sp.getItems().get(0).getValue(); // Fallback value

        // 1. Update underlying property
        if (!Objects.equals(p.getValue(), valueForProperty)) {
            try { ((Property<Object>) p).setValue(valueForProperty); }
            catch (ClassCastException cce) { System.err.println("Cast Error updating Sel Prop: "+cce); }
        }


        // 2. Update the model (needs the Item object itself), ensuring EDT safety
        int rowIndex = getPropertyRowIndex(name);
        if (rowIndex == -1) return;

        final int finalRowIndex = rowIndex;
        // Use the found item or a default if not found (shouldn't happen with valid values)
        final Item<?> itemToSetInModel = (matchingItem != null) ? matchingItem : sp.getItems().get(0);


        Runnable updateModelTask = () -> {
            TableModel model = getModel();
            if (model instanceof PropertyModel && finalRowIndex < model.getRowCount()) {
                Object currentModelValue = model.getValueAt(finalRowIndex, 1);
                // Only update model if the Item object itself is different
                if (!Objects.equals(currentModelValue, itemToSetInModel)) {
                    if (isEditing() && getEditingRow() == finalRowIndex && getEditingColumn() == 1) {
                        TableCellEditor editor = getCellEditor(finalRowIndex, 1);
                        if (editor != null) editor.stopCellEditing(); // Stop editing before model update
                    }
                    ((PropertyModel) model).setValueAt(itemToSetInModel, finalRowIndex, 1); // Set Item in model
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) updateModelTask.run();
        else SwingUtilities.invokeLater(updateModelTask);

    }


    // Helper to find row index by property name
    private int getPropertyRowIndex(String name) {
        if (name == null || propertyModel == null) return -1;
        for (int i = 0; i < propertyModel.getRowCount(); i++) {
            try {
                Object nameInRow = propertyModel.getValueAt(i, 0);
                if (name.equals(nameInRow)) return i;
            } catch (Exception e) {
                System.err.println("Error getting row index for "+name+": "+e);
                return -1;
            } // Error accessing model
        }
        return -1; // Not found
    }

    // --- isCellEditable logic --- (Adjusted for structure)
    @Override
    public boolean isCellEditable(int row, int column) {
        if (column != 1) return false; // Only value column is editable
        if (row < 0 || row >= getRowCount() || propertyModel == null) return false;

        Object propNameObj;
        try {
            propNameObj = propertyModel.getValueAt(row, 0);
        } catch (ArrayIndexOutOfBoundsException e) {
            return false; // Invalid row index
        }

        if (!(propNameObj instanceof String)) return false;
        String propName = (String) propNameObj;

        if ("Object Type".equals(propName)) return false; // Never editable

        // Check general editing flag (set based on shape selection)
        if (!editingEnabled) {
            // Only allow editing global defaults when nothing is selected
            return "Fore Color".equals(propName) || "Fill Color".equals(propName) ||
                    "Start Color".equals(propName) || "End Color".equals(propName) ||
                    "Use Gradient".equals(propName) || "Line Thickness".equals(propName) ||
                    "Font Family".equals(propName) || "Font Style".equals(propName) || "Font Size".equals(propName);
        }

        // --- Shape-Specific Editability (when editingEnabled is true) ---
        String currentObjectType = getStringValueFromModel("Object Type"); // Use helper
        boolean isTextShape = "Text".equals(currentObjectType);

        // Text/Font properties only editable for Text shapes
        if (!isTextShape && ("Text".equals(propName) || "Font Family".equals(propName) ||
                "Font Style".equals(propName) || "Font Size".equals(propName))) {
            return false;
        }

        // Gradient properties might depend on Fill being used? (Optional rule)
        // if (!currentUseGradient && ("Start Color".equals(propName) || "End Color".equals(propName))) return false;

        // Other properties are generally editable when a shape is selected
        return true; // Includes X, Y, Width, Height, Colors, Visible, etc.
    }


    // Helper to get string value from model safely
    private String getStringValueFromModel(String propertyName) {
        int rowIndex = getPropertyRowIndex(propertyName);
        if (rowIndex != -1 && propertyModel != null && rowIndex < propertyModel.getRowCount()) {
            Object val = propertyModel.getValueAt(rowIndex, 1);
            // Handle Item case for SelectionProperty
            if (val instanceof Item) {
                return ((Item<?>) val).getDescription(); // Or .getValue().toString() depending on need
            }
            return (val != null) ? val.toString() : null;
        }
        return null;
    }

    // --- prepareRenderer / prepareEditor --- (Visuals for disabled cells - unchanged from original)
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (c == null) return null;
        boolean editable = isCellEditable(row, column);
        boolean rowIsSelectedInTable = isRowSelected(row); // Table selection, not canvas selection
        Color background; Color foreground;

        // Determine background and foreground based on table selection and cell editability
        if (rowIsSelectedInTable) {
            background = getSelectionBackground();
            foreground = getSelectionForeground();
            if (!editable && column == 1) { // Value column, not editable, selected row
                background = getSelectionBackground().darker(); // Darker background for disabled selected cell
                foreground = Color.LIGHT_GRAY; // Lighter text for disabled selected cell
            }
        } else { // Row not selected in the table
            background = getBackground(); // Default background
            foreground = getForeground(); // Default foreground
            if (!editable && column == 1) { // Value column, not editable, not selected row
                background = getBackground().darker(); // Slightly darker background for disabled cell
                foreground = Color.GRAY; // Gray text for disabled cell
            }
        }

        c.setBackground(background);
        c.setForeground(foreground);
        // Ensure component reflects enabled state visually
        c.setEnabled(editable || column == 0); // Property name column is always enabled visually

        // Special handling for checkboxes
        if (c instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) c;
            checkBox.setOpaque(true); // Needed for background color to show reliably
            checkBox.setBackground(background);
            checkBox.setEnabled(editable); // Checkbox itself should match editability
        }
        // Special handling for labels (often used as renderers for non-editable fields)
        else if (c instanceof JLabel && !editable && column == 1 && !rowIsSelectedInTable) {
            // Make text slightly dimmer for disabled, non-selected cells
            c.setForeground(Color.GRAY);
        }


        return c;
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component c = super.prepareEditor(editor, row, column);
        if (c == null) return null;
        boolean editable = isCellEditable(row, column);
        c.setEnabled(editable); // Editor component should only be enabled if editable

        // Set background/foreground for the editor component
        if (!editable) {
            // Should not happen if isCellEditable returns false, but as a fallback
            c.setBackground(getBackground().darker());
            c.setForeground(Color.GRAY);
        } else {
            // Use selection colors for the active editor for better visibility
            c.setBackground(getSelectionBackground());
            c.setForeground(getSelectionForeground());
            // For text fields, ensure selected text is visible
            if (c instanceof JTextField) {
                ((JTextField) c).setSelectionColor(getForeground());
                ((JTextField) c).setSelectedTextColor(getBackground());
            }
        }
        return c;
    }


    // --- Tooltip --- (Unchanged from original)
    @SuppressWarnings("unchecked")
    @Override
    public String getToolTipText(java.awt.event.MouseEvent event) {
        int row = rowAtPoint(event.getPoint()); int col = columnAtPoint(event.getPoint());
        if (row >= 0 && col >= 0 && row < getRowCount() && properties != null && row < properties.size()) {
            if (col == 0) { // Tooltip for property name column
                Object pName = getValueAt(row, 0);
                if (pName instanceof String) return (String) pName;
            }
            else if (col == 1) { // Tooltip for value column
                Object v = getValueAt(row, 1);
                Property<?> p = properties.get(row); // Get the underlying Property object
                // If it's a SelectionProperty and the value is an Item, show the Item's description
                if (p instanceof SelectionProperty && v instanceof Item) {
                    return ((Item<?>) v).getDescription();
                }
                // Otherwise, just show the toString() representation of the value
                if (v != null) {
                    return v.toString();
                }
            }
        }
        return super.getToolTipText(event); // Default tooltip behavior
    }
}