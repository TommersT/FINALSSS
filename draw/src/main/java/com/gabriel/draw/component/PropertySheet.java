// package com.gabriel.draw.component; // Make sure package declaration is correct
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.lang.reflect.Field; // Keep reflection
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

    private volatile boolean isPopulating = false; // Flag to prevent event loops
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
            valueColumn.setPreferredWidth(160);
            valueColumn.setMinWidth(100);
        } catch (Exception e) {
            System.err.println("Error configuring columns: " + e.getMessage());
        }
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
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
            SwingUtilities.invokeLater(() -> {
                if (getModel() != null) {
                    ((PropertyModel) getModel()).fireTableDataChanged(); // Redraw cells
                }
            });
        } else {
            repaint();
        }
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
            boolean currentVisible = shapeSelected ? shape.isVisible() : true;
            int currentX = shapeSelected && shape.getLocation() != null ? shape.getLocation().x : 0;
            int currentY = shapeSelected && shape.getLocation() != null ? shape.getLocation().y : 0;
            int currentWidth = shapeSelected ? shape.getWidth() : 0;
            int currentHeight = shapeSelected ? shape.getHeight() : 0;
            int currentThickness = shapeSelected ? shape.getThickness() : drawing.getThickness();

            Font fontToUse = drawing.getFont();
            String textToUse = "";
            boolean isTextShape = "Text".equals(objectType);

            if (shapeSelected) {
                if (isTextShape) {
                    fontToUse = shape.getFont() != null ? shape.getFont() : fontToUse;
                    textToUse = shape.getText() != null ? shape.getText() : "";
                } else {
                    fontToUse = drawing.getFont(); // Use drawing font for non-text shapes
                }
            } else {
                fontToUse = drawing.getFont(); // Use drawing font when nothing selected
            }
            String currentFontFamily = fontToUse != null ? fontToUse.getFamily() : "SansSerif";
            int currentFontStyle = fontToUse != null ? fontToUse.getStyle() : Font.PLAIN;
            int currentFontSize = fontToUse != null ? fontToUse.getSize() : 12;

            // --- Update Property Objects AND Table Model Visually ---
            // Use update helpers that manage EDT safety and prevent loops
            updateStringProperty("Object Type", objectType);
            updateColorProperty("Fore Color", currentForeColor);
            updateColorProperty("Fill Color", currentFillColor);
            updateColorProperty("Start Color", currentStartColor);
            updateColorProperty("End Color", currentEndColor);
            updateBooleanProperty("Use Gradient", currentUseGradient);
            updateBooleanProperty("Visible", currentVisible);

            // *** Explicitly update X, Y, Width, Height using helpers ***
            updateIntegerProperty("X Location", currentX);
            updateIntegerProperty("Y Location", currentY);
            updateIntegerProperty("Width", currentWidth);
            updateIntegerProperty("Height", currentHeight);

            updateIntegerProperty("Line Thickness", currentThickness);
            updateStringProperty("Text", textToUse);
            updateStringProperty("Font Family", currentFontFamily);
            updateSelectionProperty("Font Style", currentFontStyle); // Use updateSelectionProperty
            updateIntegerProperty("Font Size", currentFontSize);

        } catch (Exception ex) {
            System.err.println("Exception during PropertySheet.populateTable value updates:");
            ex.printStackTrace();
        } finally {
            isPopulating = false; // Ensure flag is reset
        }
        // No full table refresh needed - individual updates handle it.
    }


    // --- Helper methods to UPDATE values directly in properties and the table model ---
    // Core update logic with EDT safety
    private void updateValueInModel(String name, Object value) {
        if (isPopulating || CommandService.isExecutingCommand()) {
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

                // Basic type check
                boolean typesCompatible = true;
                if (p.getValue() != null && value != null && !p.getValue().getClass().isInstance(value)) {
                    if (!((p.getValue() instanceof Integer && value instanceof Integer) ||
                            (p.getValue() instanceof Boolean && value instanceof Boolean)
                            // Add other primitive/wrapper checks if needed
                    )) {
                        typesCompatible = false;
                    }
                }

                if (typesCompatible) {
                    // 1. Update Property object
                    ((Property<Object>) p).setValue(value);

                    // 2. Update table model on EDT
                    Runnable updateModelTask = () -> {
                        if (isEditing() && getEditingRow() == finalRowIndex && getEditingColumn() == 1) {
                            TableCellEditor editor = getCellEditor(finalRowIndex, 1);
                            if (editor != null) editor.stopCellEditing();
                        }
                        if (propertyModel != null && finalRowIndex < propertyModel.getRowCount()) {
                            propertyModel.setValueAt(value, finalRowIndex, 1); // Update cell
                        }
                    };

                    if (SwingUtilities.isEventDispatchThread()) { updateModelTask.run(); }
                    else { SwingUtilities.invokeLater(updateModelTask); }

                } else {
                    System.err.println("Type mismatch prevented updateValueInModel for '" + name + "'");
                }
            }
        } catch (Exception e) {
            System.err.println("Error in updateValueInModel for '" + name + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Specific type update helpers ---
    private void updateStringProperty(String name, String value) { updateValueInModel(name, value != null ? value : ""); }
    private void updateIntegerProperty(String name, int value) { updateValueInModel(name, value); }
    private void updateBooleanProperty(String name, boolean value) { updateValueInModel(name, value); }
    private void updateColorProperty(String name, Color value) { updateValueInModel(name, value != null ? value : Color.GRAY); }

    // Special handler for SelectionProperty (like Font Style)
    private void updateSelectionProperty(String name, Object valueToSelect) {
        Property<?> p = propertyMap.get(name);
        if (!(p instanceof SelectionProperty)) return;
        SelectionProperty<?> sp = (SelectionProperty<?>) p;
        Item<?> matchingItem = null;

        for (Object itemObj : sp.getItems()) {
            if (itemObj instanceof Item) {
                Item<?> item = (Item<?>) itemObj;
                if (Objects.equals(item.getValue(), valueToSelect)) {
                    matchingItem = item; break;
                }
            }
        }
        Object valueForProperty = (matchingItem != null) ? matchingItem.getValue() : null;

        // 1. Update underlying property (if needed)
        if (!isPopulating && !CommandService.isExecutingCommand()) {
            if (!Objects.equals(p.getValue(), valueForProperty)) {
                try { ((Property<Object>) p).setValue(valueForProperty); }
                catch (ClassCastException cce) { /* Handle error */ }
            }
        }

        // 2. Update the model (needs the Item object), ensuring EDT safety
        int rowIndex = getPropertyRowIndex(name);
        if (rowIndex == -1) return;

        final int finalRowIndex = rowIndex;
        final Item<?> finalMatchingItem = matchingItem;

        Object currentModelValue = (propertyModel != null && finalRowIndex < propertyModel.getRowCount()) ? propertyModel.getValueAt(finalRowIndex, 1) : null;

        if (!Objects.equals(currentModelValue, finalMatchingItem)) {
            Runnable updateModelTask = () -> {
                if (isEditing() && getEditingRow() == finalRowIndex && getEditingColumn() == 1) {
                    TableCellEditor editor = getCellEditor(finalRowIndex, 1);
                    if (editor != null) editor.stopCellEditing();
                }
                if (propertyModel != null && finalRowIndex < propertyModel.getRowCount()) {
                    propertyModel.setValueAt(finalMatchingItem, finalRowIndex, 1); // Set Item in model
                }
            };
            if (SwingUtilities.isEventDispatchThread()) updateModelTask.run();
            else SwingUtilities.invokeLater(updateModelTask);
        }
    }


    // Helper to find row index by property name
    private int getPropertyRowIndex(String name) {
        if (name == null || propertyModel == null) return -1;
        for (int i = 0; i < propertyModel.getRowCount(); i++) {
            try {
                Object nameInRow = propertyModel.getValueAt(i, 0);
                if (name.equals(nameInRow)) return i;
            } catch (Exception e) { return -1; } // Error accessing model
        }
        return -1; // Not found
    }

    // --- isCellEditable logic --- (Adjusted for structure)
    @Override
    public boolean isCellEditable(int row, int column) {
        if (column != 1) return false;
        if (row < 0 || row >= getRowCount()) return false;

        Object propNameObj = getValueAt(row, 0);
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
        String currentObjectType = getStringValueFromModel("Object Type");
        boolean isTextShape = "Text".equals(currentObjectType);

        // Text/Font properties only editable for Text shapes
        if (!isTextShape && ("Text".equals(propName) || "Font Family".equals(propName) ||
                "Font Style".equals(propName) || "Font Size".equals(propName))) {
            return false;
        }

        // Other properties are editable when a shape is selected
        return true; // Includes X, Y, Width, Height, Colors, Visible, etc.
    }


    // Helper to get string value from model safely
    private String getStringValueFromModel(String propertyName) {
        int rowIndex = getPropertyRowIndex(propertyName);
        if (rowIndex != -1 && propertyModel != null && rowIndex < propertyModel.getRowCount()) {
            Object val = propertyModel.getValueAt(rowIndex, 1);
            return (val != null) ? val.toString() : null;
        }
        return null;
    }

    // --- prepareRenderer / prepareEditor --- (Visuals for disabled cells - unchanged)
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (c == null) return null;
        boolean editable = isCellEditable(row, column);
        boolean rowIsSelectedInTable = isRowSelected(row);
        Color background; Color foreground;
        if (rowIsSelectedInTable) {
            background = getSelectionBackground(); foreground = getSelectionForeground();
            if (!editable && column == 1) { background = getSelectionBackground().darker(); foreground = Color.LIGHT_GRAY; }
        } else {
            if (!editable && column == 1) { background = getBackground().darker(); foreground = Color.GRAY; }
            else { background = getBackground(); foreground = getForeground(); }
        }
        c.setBackground(background); c.setForeground(foreground);
        c.setEnabled(editable || column == 0);
        if (c instanceof JCheckBox) { ((JCheckBox)c).setOpaque(true); ((JCheckBox)c).setBackground(background); ((JCheckBox)c).setEnabled(editable); }
        if (c instanceof JLabel && !editable && column == 1 && !rowIsSelectedInTable) { c.setForeground(Color.GRAY); }
        return c;
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component c = super.prepareEditor(editor, row, column);
        if (c == null) return null;
        boolean editable = isCellEditable(row, column);
        c.setEnabled(editable);
        if (!editable) { c.setBackground(getBackground().darker()); c.setForeground(Color.GRAY); }
        else { c.setBackground(getSelectionBackground()); c.setForeground(getSelectionForeground()); }
        return c;
    }

    // --- Tooltip --- (Unchanged)
    @SuppressWarnings("unchecked")
    @Override
    public String getToolTipText(java.awt.event.MouseEvent event) {
        int row = rowAtPoint(event.getPoint()); int col = columnAtPoint(event.getPoint());
        if (row >= 0 && col >= 0 && row < getRowCount() && properties != null && row < properties.size()) {
            if (col == 0) { Object p = getValueAt(row, 0); if (p instanceof String) return (String) p; }
            else if (col == 1) { Object v = getValueAt(row, 1); Property<?> p = properties.get(row);
                if (p instanceof SelectionProperty && v instanceof Item) return ((Item<?>) v).getDescription();
                if (v != null) return v.toString();
            }
        }
        return super.getToolTipText(event);
    }
}