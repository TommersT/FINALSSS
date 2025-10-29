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
import com.gabriel.drawfx.command.CommandService;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class PropertySheet extends PropertyPanel {

    private final Map<String, Property<?>> propertyMap = new HashMap<>();
    private volatile boolean isPopulating = false;
    private volatile boolean isUpdatingFromController = false;
    private boolean editingEnabled = false;
    private boolean isInitialized = false; // Track initialization


    public PropertySheet(PropertyOptions options) {
        super(options);
        System.out.println("PropertySheet: Constructor called");
        initializeProperties();
        configureColumns();
        isInitialized = true;
        System.out.println("PropertySheet: Initialization complete. Row count: " + getRowCount());
    }

    private void initializeProperties() {
        System.out.println("PropertySheet: Initializing properties...");
        propertyMap.clear();
        if (propertyModel != null) propertyModel.clear();

        // Add core properties - Object Type FIRST
        addInternalProperty(new StringProperty("Object Type", "None"));
        addInternalProperty(new ColorProperty("Fore Color", Color.BLACK));
        addInternalProperty(new ColorProperty("Fill Color", Color.WHITE));
        addInternalProperty(new ColorProperty("Start Color", Color.LIGHT_GRAY));
        addInternalProperty(new ColorProperty("End Color", Color.DARK_GRAY));
        addInternalProperty(new BooleanProperty("Use Gradient", false));
        addInternalProperty(new BooleanProperty("Visible", true));
        addInternalProperty(new IntegerProperty("X Location", 0));
        addInternalProperty(new IntegerProperty("Y Location", 0));
        addInternalProperty(new IntegerProperty("Width", 0));
        addInternalProperty(new IntegerProperty("Height", 0));
        addInternalProperty(new IntegerProperty("Line Thickness", 1));

        System.out.println("PropertySheet: Properties initialized. Count: " + propertyMap.size());
    }

    private void addInternalProperty(Property<?> prop) {
        if (prop == null) {
            System.err.println("PropertySheet.addInternalProperty: Property is null!");
            return;
        }
        if (propertyModel == null) {
            System.err.println("PropertySheet.addInternalProperty: propertyModel is null!");
            return;
        }

        String propName = prop.getName();
        System.out.println("PropertySheet: Adding property: " + propName + " with value: " + prop.getValue());

        propertyMap.put(propName, prop);
        try {
            super.addProperty(prop);
            System.out.println("PropertySheet: Successfully added property: " + propName);
        } catch (Exception e) {
            System.err.println("PropertySheet: Error adding property '" + propName + "': " + e.getMessage());
            e.printStackTrace();
            propertyModel.addRow(new Object[]{propName, "[Add Error]"});
        }
    }

    private void configureColumns() {
        if (getModel() == null || getColumnModel().getColumnCount() < 2) {
            System.err.println("PropertySheet.configureColumns: Model or columns not ready");
            return;
        }
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        try {
            TableColumn propertyColumn = getColumnModel().getColumn(0);
            propertyColumn.setPreferredWidth(100);
            propertyColumn.setMinWidth(80);
            TableColumn valueColumn = getColumnModel().getColumn(1);
            valueColumn.setPreferredWidth(160);
            valueColumn.setMinWidth(100);
        } catch (Exception e) {
            System.err.println("PropertySheet: Error configuring columns: " + e.getMessage());
        }
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // Set default row height for better appearance
        setRowHeight(24);
    }

    @Override
    public void clear() {
        System.out.println("PropertySheet: Clear called");
        if (isEditing()) {
            TableCellEditor editor = getCellEditor();
            if (editor != null) editor.stopCellEditing();
        }
        cellComponents.clear();
        properties.clear();
        propertyMap.clear();
        if (propertyModel != null) propertyModel.clear();
        isInitialized = false;
    }

    public void setEditingEnabled(boolean enabled) {
        if (this.editingEnabled == enabled) return;
        System.out.println("PropertySheet: Setting editing enabled: " + enabled);
        this.editingEnabled = enabled;
        if (getModel() instanceof PropertyModel) {
            if (isEditing()) {
                TableCellEditor editor = getCellEditor();
                if (editor != null) editor.stopCellEditing();
            }
            SwingUtilities.invokeLater(() -> {
                if (getModel() != null) {
                    ((PropertyModel) getModel()).fireTableDataChanged();
                }
            });
        } else {
            repaint();
        }
    }

    public boolean isUpdatingFromController() {
        return isUpdatingFromController;
    }

    public void populateTable(AppService appService) {
        System.out.println("\n=== PropertySheet.populateTable CALLED ===");

        if (appService == null) {
            System.err.println("PropertySheet.populateTable: AppService is null!");
            return;
        }

        // Check if we need to reinitialize
        if (!isInitialized || propertyMap.isEmpty() || getRowCount() != propertyMap.size()) {
            System.err.println("PropertySheet.populateTable: Structure mismatch detected.");
            System.err.println("  isInitialized: " + isInitialized);
            System.err.println("  propertyMap.size(): " + propertyMap.size());
            System.err.println("  getRowCount(): " + getRowCount());
            initializeProperties();
            configureColumns();
            isInitialized = true;
        }

        isPopulating = true;

        try {
            Shape shape = appService.getSelectedShape();
            Drawing drawing = appService.getDrawing();
            boolean shapeSelected = (shape != null);

            System.out.println("PropertySheet: Shape selected: " + shapeSelected);
            if (shapeSelected) {
                System.out.println("PropertySheet: Shape class: " + shape.getClass().getName());
            }

            // Determine Object Type
            String objectType = determineObjectType(shape, shapeSelected);
            System.out.println("PropertySheet: Determined Object Type: '" + objectType + "'");

            setEditingEnabled(shapeSelected);

            // Read current values
            Color currentForeColor = shapeSelected ? shape.getColor() : (drawing != null ? drawing.getColor() : Color.BLACK);
            Color currentFillColor = shapeSelected ? shape.getFill() : (drawing != null ? drawing.getFill() : Color.WHITE);
            Color currentStartColor = shapeSelected ? shape.getStartColor() : (drawing != null ? drawing.getStartColor() : Color.LIGHT_GRAY);
            Color currentEndColor = shapeSelected ? shape.getEndColor() : (drawing != null ? drawing.getEndColor() : Color.DARK_GRAY);
            boolean currentUseGradient = shapeSelected ? shape.isUseGradient() : (drawing != null && drawing.isUseGradient());
            boolean currentVisible = shapeSelected ? shape.isVisible() : true;
            int currentX = (shapeSelected && shape.getLocation() != null) ? shape.getLocation().x : 0;
            int currentY = (shapeSelected && shape.getLocation() != null) ? shape.getLocation().y : 0;
            int currentWidth = shapeSelected ? shape.getWidth() : 0;
            int currentHeight = shapeSelected ? shape.getHeight() : 0;
            int currentThickness = shapeSelected ? shape.getThickness() : (drawing != null ? drawing.getThickness() : 1);

            // Update properties with detailed logging
            System.out.println("PropertySheet: Updating Object Type to: '" + objectType + "'");
            updateStringPropertyDirect("Object Type", objectType);

            // Verify the update
            verifyPropertyValue("Object Type");

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

            System.out.println("=== PropertySheet.populateTable COMPLETE ===\n");

        } catch (Exception ex) {
            System.err.println("PropertySheet: Exception during populateTable:");
            ex.printStackTrace();
        } finally {
            isPopulating = false;
        }
    }

    /**
     * Verify that a property value was actually set
     */
    private void verifyPropertyValue(String propertyName) {
        Property<?> prop = propertyMap.get(propertyName);
        if (prop != null) {
            Object propValue = prop.getValue();
            System.out.println("  Property object value: " + propValue);
        } else {
            System.err.println("  Property '" + propertyName + "' not found in map!");
        }

        int rowIndex = getPropertyRowIndex(propertyName);
        if (rowIndex != -1 && propertyModel != null && rowIndex < propertyModel.getRowCount()) {
            Object modelValue = propertyModel.getValueAt(rowIndex, 1);
            System.out.println("  Model value at row " + rowIndex + ": " + modelValue);
        } else {
            System.err.println("  Could not find row for property '" + propertyName + "'");
        }
    }

    private String determineObjectType(Shape shape, boolean shapeSelected) {
        if (!shapeSelected || shape == null) {
            return "Drawing";
        }

        String className = shape.getClass().getSimpleName();
        System.out.println("PropertySheet.determineObjectType: Raw class name: '" + className + "'");

        if (className.isEmpty()) {
            className = shape.getClass().getName();
            int lastDot = className.lastIndexOf('.');
            if (lastDot > 0 && lastDot < className.length() - 1) {
                className = className.substring(lastDot + 1);
            }
            System.out.println("PropertySheet.determineObjectType: Extracted class name: '" + className + "'");
        }

        // Map specific class names to display names
        String displayName;
        switch (className) {
            case "TextShape":
            case "Text":
                displayName = "Text";
                break;
            case "PictureShape":
            case "Picture":
            case "ImageShape":
            case "Image":
                displayName = "Picture";
                break;
            case "RectangleShape":
                displayName = "Rectangle";
                break;
            case "EllipseShape":
                displayName = "Ellipse";
                break;
            case "LineShape":
                displayName = "Line";
                break;
            case "PolygonShape":
                displayName = "Polygon";
                break;
            case "PolylineShape":
                displayName = "Polyline";
                break;
            default:
                // Remove "Shape" suffix if present
                if (className.endsWith("Shape")) {
                    displayName = className.substring(0, className.length() - 5);
                } else {
                    displayName = className;
                }
        }

        System.out.println("PropertySheet.determineObjectType: Final display name: '" + displayName + "'");
        return displayName;
    }

    private void updatePropertyDirectly(String name, Object value) {
        System.out.println("PropertySheet.updatePropertyDirectly: " + name + " = " + value);

        if (isPopulating || (!isUpdatingFromController && CommandService.isExecutingCommand())) {
            // Allow updates during population
            if (!isPopulating) {
                System.out.println("  Skipping update (command executing)");
                return;
            }
        }

        Property<?> p = propertyMap.get(name);
        if (p == null) {
            System.err.println("  ERROR: Property '" + name + "' not found in propertyMap!");
            System.err.println("  Available properties: " + propertyMap.keySet());
            return;
        }

        int rowIndex = getPropertyRowIndex(name);
        if (rowIndex == -1) {
            System.err.println("  ERROR: Row index not found for property: " + name);
            return;
        }

        System.out.println("  Property found at row: " + rowIndex);
        final int finalRowIndex = rowIndex;

        try {
            Object currentModelValue = (propertyModel != null && finalRowIndex < propertyModel.getRowCount())
                    ? propertyModel.getValueAt(finalRowIndex, 1) : null;
            Object currentPropertyValue = p.getValue();

            System.out.println("  Current model value: " + currentModelValue);
            System.out.println("  Current property value: " + currentPropertyValue);
            System.out.println("  New value: " + value);

            if (!Objects.equals(currentModelValue, value) || !Objects.equals(currentPropertyValue, value)) {
                boolean typesCompatible = true;
                Object valueToSet = value;

                // Type checking
                if (p.getValue() != null && value != null && !p.getValue().getClass().isInstance(value)) {
                    if (!((p.getValue() instanceof Integer && value instanceof Integer) ||
                            (p.getValue() instanceof Boolean && value instanceof Boolean) ||
                            (p.getValue() instanceof String && value instanceof String) ||
                            (p.getValue() instanceof Color && value instanceof Color))) {
                        typesCompatible = false;
                        System.err.println("  ERROR: Type mismatch for '" + name + "'. Expected " +
                                p.getValue().getClass() + " but got " + value.getClass());
                    }
                }

                if (typesCompatible) {
                    try {
                        // Update property object
                        ((Property<Object>) p).setValue(valueToSet);
                        System.out.println("  Property object updated successfully");
                    } catch (ClassCastException e) {
                        System.err.println("  ERROR: Casting error for " + name + ": " + e);
                        return;
                    }

                    // Update table model
                    Runnable updateModelTask = () -> {
                        TableModel model = getModel();
                        if (model instanceof PropertyModel && finalRowIndex < model.getRowCount()) {
                            // Stop any active editing
                            if (isEditing() && getEditingRow() == finalRowIndex && getEditingColumn() == 1) {
                                TableCellEditor editor = getCellEditor(finalRowIndex, 1);
                                if (editor != null) {
                                    if (!editor.stopCellEditing()) {
                                        editor.cancelCellEditing();
                                    }
                                }
                            }

                            // Update the model
                            ((PropertyModel) model).setValueAt(valueToSet, finalRowIndex, 1);
                            System.out.println("  Model updated at row " + finalRowIndex + " with value: " + valueToSet);

                            // Force a repaint of this row
                            repaint(getCellRect(finalRowIndex, 1, true));
                        }
                    };

                    if (SwingUtilities.isEventDispatchThread()) {
                        updateModelTask.run();
                    } else {
                        SwingUtilities.invokeLater(updateModelTask);
                    }
                } else {
                    System.err.println("  Skipping update due to type incompatibility");
                }
            } else {
                System.out.println("  Values are equal, no update needed");
            }
        } catch (Exception e) {
            System.err.println("  ERROR in updatePropertyDirectly for '" + name + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStringPropertyDirect(String name, String value) {
        updatePropertyDirectly(name, value != null ? value : "");
    }

    private void updateIntegerPropertyDirect(String name, int value) {
        updatePropertyDirectly(name, value);
    }

    private void updateBooleanPropertyDirect(String name, boolean value) {
        updatePropertyDirectly(name, value);
    }

    private void updateColorPropertyDirect(String name, Color value) {
        updatePropertyDirectly(name, value != null ? value : Color.GRAY);
    }

    public void updateIntegerProperty(String name, int value) {
        updatePropertyDirectly(name, value);
    }

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

        if ("Object Type".equals(propName)) return false;

        if (!editingEnabled) {
            return "Fore Color".equals(propName) || "Fill Color".equals(propName) ||
                    "Start Color".equals(propName) || "End Color".equals(propName) ||
                    "Use Gradient".equals(propName) || "Line Thickness".equals(propName);
        }

        return true;
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (c == null) return null;

        boolean editable = isCellEditable(row, column);
        boolean rowIsSelectedInTable = isRowSelected(row);
        Color background;
        Color foreground;

        // Value column (column 1) styling
        if (column == 1) {
            // Set light gray background for value column
            background = new Color(240, 240, 240); // Light gray
            foreground = Color.BLACK;

            // If row is selected, use selection colors
            if (rowIsSelectedInTable) {
                background = getSelectionBackground();
                foreground = getSelectionForeground();
            }

            // Darker gray for non-editable cells
            if (!editable) {
                foreground = new Color(100, 100, 100); // Darker gray text
            }

            // Center align text components
            if (c instanceof JLabel) {
                ((JLabel) c).setHorizontalAlignment(JLabel.CENTER);
            } else if (c instanceof JTextField) {
                ((JTextField) c).setHorizontalAlignment(JTextField.CENTER);
            }
        } else {
            // Property name column (column 0) - default styling
            background = getBackground();
            foreground = getForeground();

            if (rowIsSelectedInTable) {
                background = getSelectionBackground();
                foreground = getSelectionForeground();
            }
        }

        c.setBackground(background);
        c.setForeground(foreground);
        c.setEnabled(editable || column == 0);

        // Special handling for checkboxes
        if (c instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) c;
            checkBox.setOpaque(true);
            checkBox.setBackground(background);
            checkBox.setEnabled(editable);
            checkBox.setHorizontalAlignment(JCheckBox.CENTER);
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

    @Override
    public String getToolTipText(java.awt.event.MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        int col = columnAtPoint(event.getPoint());
        if (row >= 0 && col >= 0 && row < getRowCount() && properties != null && row < properties.size()) {
            if (col == 0) {
                Object pName = getValueAt(row, 0);
                if (pName instanceof String) return (String) pName;
            }
            else if (col == 1) {
                Object v = getValueAt(row, 1);
                if (v != null) {
                    return v.toString();
                }
            }
        }
        return super.getToolTipText(event);
    }
}