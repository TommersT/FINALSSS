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
    // REMOVED: Font style items are no longer needed here if rows are removed
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
        // --- REMOVED TEXT PROPERTY ---
        // addInternalProperty(new StringProperty("Text", "")); // For Text shapes
        // --- END REMOVAL ---

        // --- REMOVED FONT PROPERTIES ---
        // addInternalProperty(new StringProperty("Font Family", "SansSerif")); // For Text shapes
        // addInternalProperty(new SelectionProperty<>("Font Style", fontStyleItems)); // For Text shapes
        // addInternalProperty(new IntegerProperty("Font Size", 12)); // For Text shapes
        // --- END REMOVAL ---

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
            repaint(); // Fallback repaint
        }
    }

    // Getter for the flag
    public boolean isUpdatingFromController() {
        return isUpdatingFromController;
    }

    // *** POPULATE TABLE: Updates VALUES only, called frequently ***
    public void populateTable(AppService appService) {
        if (appService == null) return;
        if (propertyMap.isEmpty() || getRowCount() != propertyMap.size()) {
            System.err.println("PropertySheet.populateTable: Structure mismatch. Re-initializing.");
            initializeProperties();
            configureColumns();
            if (propertyMap.isEmpty() || getRowCount() != propertyMap.size()) {
                System.err.println("PropertySheet.populateTable: Initialization failed. Aborting population.");
                return;
            }
        }

        isPopulating = true;

        Shape shape = appService.getSelectedShape();
        Drawing drawing = appService.getDrawing();
        boolean shapeSelected = (shape != null);
        String objectType = shapeSelected ? shape.getClass().getSimpleName() : "Drawing";

        setEditingEnabled(shapeSelected);

        try {
            // Read current values
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

            // --- REMOVED TEXT/FONT READING ---
            // String textToUse = "";
            // boolean isTextShape = "Text".equals(objectType);
            // if (shapeSelected && isTextShape) {
            //    textToUse = shape.getText() != null ? shape.getText() : "";
            // }
            // --- END REMOVAL ---

            // --- Update Property Objects AND Table Model Visually ---
            updateStringPropertyDirect("Object Type", objectType);
            updateColorPropertyDirect("Fore Color", currentForeColor);
            updateColorPropertyDirect("Fill Color", currentFillColor);
            updateColorPropertyDirect("Start Color", currentStartColor);
            updateColorPropertyDirect("End Color", currentEndColor);
            updateBooleanPropertyDirect("Use Gradient", currentUseGradient);
            updateBooleanPropertyDirect("Visible", currentVisible);

            updateIntegerPropertyDirect("X Location", currentX);
            updateIntegerPropertyDirect("Y Location", currentY);
            updateIntegerPropertyDirect("Width", currentWidth);
            updateIntegerPropertyDirect("Height", currentHeight);

            updateIntegerPropertyDirect("Line Thickness", currentThickness);

            // --- REMOVED TEXT/FONT PROPERTY UPDATES ---
            // updateStringPropertyDirect("Text", textToUse);
            // --- END REMOVAL ---

        } catch (Exception ex) {
            System.err.println("Exception during PropertySheet.populateTable value updates:");
            ex.printStackTrace();
        } finally {
            isPopulating = false;
        }
    }


    // --- Helper methods to UPDATE values directly (UNCHANGED) ---
    private void updatePropertyDirectly(String name, Object value) {
        if (isPopulating || (!isUpdatingFromController && CommandService.isExecutingCommand())) {
            return;
        }
        Property<?> p = propertyMap.get(name);
        if (p == null) return;
        int rowIndex = getPropertyRowIndex(name);
        if (rowIndex == -1) return;
        final int finalRowIndex = rowIndex;
        try {
            Object currentModelValue = (propertyModel != null && finalRowIndex < propertyModel.getRowCount()) ? propertyModel.getValueAt(finalRowIndex, 1) : null;
            Object currentPropertyValue = p.getValue();
            if (!Objects.equals(currentModelValue, value) || !Objects.equals(currentPropertyValue, value)) {
                boolean typesCompatible = true;
                Object valueToSet = value;
                if (p.getValue() != null && value != null && !p.getValue().getClass().isInstance(value)) {
                    if (!((p.getValue() instanceof Integer && value instanceof Integer) ||
                            (p.getValue() instanceof Boolean && value instanceof Boolean) ||
                            (p.getValue() instanceof Color && value instanceof Color))) {
                        typesCompatible = false;
                        System.err.println("Type mismatch prevented updateValueInModel for '" + name + "'. Expected " + p.getValue().getClass() + " but got " + value.getClass());
                    }
                }
                if (typesCompatible) {
                    try {
                        ((Property<Object>) p).setValue(valueToSet);
                    } catch (ClassCastException e) {
                        System.err.println("Error casting value in updatePropertyDirectly (setValue) for " + name + ": " + e);
                        return;
                    }
                    Runnable updateModelTask = () -> {
                        TableModel model = getModel();
                        if (model instanceof PropertyModel && finalRowIndex < model.getRowCount()) {
                            if (isEditing() && getEditingRow() == finalRowIndex && getEditingColumn() == 1) {
                                TableCellEditor editor = getCellEditor(finalRowIndex, 1);
                                if (editor != null) {
                                    if (!editor.stopCellEditing()) {
                                        editor.cancelCellEditing();
                                    }
                                }
                            }
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
    private void updateStringPropertyDirect(String name, String value) { updatePropertyDirectly(name, value != null ? value : ""); }
    private void updateIntegerPropertyDirect(String name, int value) { updatePropertyDirectly(name, value); }
    private void updateBooleanPropertyDirect(String name, boolean value) { updatePropertyDirectly(name, value); }
    private void updateColorPropertyDirect(String name, Color value) { updatePropertyDirectly(name, value != null ? value : Color.GRAY); }
    public void updateIntegerProperty(String name, int value) { updatePropertyDirectly(name, value); }
    // --- END Helper Methods ---


    // Helper to find row index by property name (UNCHANGED)
    private int getPropertyRowIndex(String name) {
        if (name == null || propertyModel == null) return -1;
        for (int i = 0; i < propertyModel.getRowCount(); i++) {
            try {
                Object nameInRow = propertyModel.getValueAt(i, 0);
                if (name.equals(nameInRow)) return i;
            } catch (Exception e) {
                System.err.println("Error getting row index for "+name+": "+e);
                return -1;
            }
        }
        return -1;
    }

    // --- isCellEditable logic --- (Simplified Further)
    @Override
    public boolean isCellEditable(int row, int column) {
        if (column != 1) return false;
        if (row < 0 || row >= getRowCount() || propertyModel == null) return false;

        Object propNameObj;
        try {
            propNameObj = propertyModel.getValueAt(row, 0);
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }

        if (!(propNameObj instanceof String)) return false;
        String propName = (String) propNameObj;

        if ("Object Type".equals(propName)) return false; // Never editable

        // Check general editing flag
        if (!editingEnabled) {
            // Allow editing global defaults (excluding removed font/text props)
            return "Fore Color".equals(propName) || "Fill Color".equals(propName) ||
                    "Start Color".equals(propName) || "End Color".equals(propName) ||
                    "Use Gradient".equals(propName) || "Line Thickness".equals(propName);
        }

        // --- REMOVED TEXT EDITABILITY CHECK ---
        // if ("Text".equals(propName)) { ... }
        // --- END REMOVAL ---

        // Other properties are generally editable when a shape is selected
        return true;
    }


    // Helper to get string value from model safely (UNCHANGED)
    private String getStringValueFromModel(String propertyName) {
        int rowIndex = getPropertyRowIndex(propertyName);
        if (rowIndex != -1 && propertyModel != null && rowIndex < propertyModel.getRowCount()) {
            Object val = propertyModel.getValueAt(rowIndex, 1);
            if (val instanceof Item) { return ((Item<?>) val).getDescription(); }
            return (val != null) ? val.toString() : null;
        }
        return null;
    }

    // --- prepareRenderer / prepareEditor --- (UNCHANGED)
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (c == null) return null;
        boolean editable = isCellEditable(row, column);
        boolean rowIsSelectedInTable = isRowSelected(row);
        Color background; Color foreground;

        if (rowIsSelectedInTable) {
            background = getSelectionBackground();
            foreground = getSelectionForeground();
            if (!editable && column == 1) {
                background = getSelectionBackground().darker();
                foreground = Color.LIGHT_GRAY;
            }
        } else {
            background = getBackground();
            foreground = getForeground();
            if (!editable && column == 1) {
                background = getBackground().darker();
                foreground = Color.GRAY;
            }
        }

        c.setBackground(background);
        c.setForeground(foreground);
        c.setEnabled(editable || column == 0);

        if (c instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) c;
            checkBox.setOpaque(true);
            checkBox.setBackground(background);
            checkBox.setEnabled(editable);
        } else if (c instanceof JLabel && !editable && column == 1 && !rowIsSelectedInTable) {
            c.setForeground(Color.GRAY);
        }
        return c;
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component c = super.prepareEditor(editor, row, column);
        if (c == null) return null;
        boolean editable = isCellEditable(row, column);
        c.setEnabled(editable);

        if (!editable) {
            c.setBackground(getBackground().darker());
            c.setForeground(Color.GRAY);
        } else {
            c.setBackground(getSelectionBackground());
            c.setForeground(getSelectionForeground());
            if (c instanceof JTextField) {
                ((JTextField) c).setSelectionColor(getForeground());
                ((JTextField) c).setSelectedTextColor(getBackground());
            }
        }
        return c;
    }


    // --- Tooltip --- (UNCHANGED)
    @Override
    public String getToolTipText(java.awt.event.MouseEvent event) {
        int row = rowAtPoint(event.getPoint()); int col = columnAtPoint(event.getPoint());
        if (row >= 0 && col >= 0 && row < getRowCount() && properties != null && row < properties.size()) {
            if (col == 0) { // Property name
                Object pName = getValueAt(row, 0);
                if (pName instanceof String) return (String) pName;
            }
            else if (col == 1) { // Value
                Object v = getValueAt(row, 1);
                if (v != null) {
                    return v.toString(); // Simple toString for tooltip
                }
            }
        }
        return super.getToolTipText(event);
    }
}