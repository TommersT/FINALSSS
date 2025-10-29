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
// No need to import SelectionCellComponent specifically here unless used directly

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
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
    private boolean editingEnabled = false; // Controls cell editability


    // *** CONSTRUCTOR: Initializes STRUCTURE ***
    public PropertySheet(PropertyOptions options) {
        super(options); // Creates table, empty model, sets basic options
        initializeProperties(); // Adds rows/structure to the model
        configureColumns();     // Configures columns based on the model structure
    }

    // *** Initializes the PROPERTIES and adds ROWS to the model ***
    private void initializeProperties() {
        propertyMap.clear(); // Clear map before rebuilding
        // DO NOT clear the model here; it's fresh from super()

        // Add properties one by one using the helper
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
        addInternalProperty(new StringProperty("Text", ""));
        addInternalProperty(new ActionProperty("Image", "Change...", () -> { /* Action set in populate */ }));
        addInternalProperty(new ActionProperty("Remove Image", "Remove", () -> { /* Action set in populate */ }));
        addInternalProperty(new StringProperty("Font Family", "SansSerif"));
        addInternalProperty(new SelectionProperty<>("Font Style", fontStyleItems)); // Uses the predefined list
        addInternalProperty(new IntegerProperty("Font Size", 12));
    }

    // Helper to add property to map AND call superclass to add row/component
    private void addInternalProperty(Property<?> prop) {
        if (prop == null) return;
        propertyMap.put(prop.getName(), prop);
        try {
            // This calls super.addProperty(Property), which finds/creates
            // the cell component and adds the row to the model.
            super.addProperty(prop);
        } catch (Exception e) {
            System.err.println("Error adding internal property '" + prop.getName() + "': " + e.getMessage());
            e.printStackTrace();
            if (propertyModel != null) {
                propertyModel.addRow(new Object[]{prop.getName(), "[Add Error]"});
            }
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

    // Override clear to rebuild the structure (use cautiously)
    @Override
    public void clear() {
        if (isEditing()) getCellEditor().stopCellEditing();
        // Clear internal lists first
        cellComponents.clear(); // List from superclass
        properties.clear();     // List from superclass
        propertyMap.clear();    // Local map
        // Clear the model rows
        if (propertyModel != null) propertyModel.clear();
        // Rebuild
        initializeProperties();
        configureColumns();
        // Notify UI of complete change
        if (propertyModel != null) propertyModel.fireTableStructureChanged();
    }

    // *** METHOD DEFINITION for setEditingEnabled ***
    public void setEditingEnabled(boolean enabled) {
        if (this.editingEnabled == enabled) return; // Avoid unnecessary updates
        this.editingEnabled = enabled;
        // Force refresh of table structure and potentially cell states
        if (getModel() instanceof PropertyModel) {
            ((PropertyModel) getModel()).fireTableDataChanged(); // Notify cells need rerender
        } else {
            repaint(); // Fallback repaint
        }
    }


    // *** POPULATE TABLE: Updates VALUES only ***
    public void populateTable(AppService appService) {
        // Basic validation
        if (appService == null) {
            System.err.println("PropertySheet.populateTable: appService is null.");
            return;
        }
        if (propertyMap.isEmpty() || getRowCount() != propertyMap.size()) {
            System.err.println("PropertySheet.populateTable: Property structure not initialized or inconsistent (Rows: "+getRowCount()+", Map: "+propertyMap.size()+"). Re-initializing.");
            clear(); // Attempt to rebuild if state is wrong
            if (propertyMap.isEmpty()) {
                System.err.println("PropertySheet.populateTable: Re-initialization failed. Cannot populate.");
                return;
            }
        }

        isPopulating = true; // Prevent event loops

        Shape shape = appService.getSelectedShape();
        Drawing drawing = appService.getDrawing();
        boolean shapeSelected = (shape != null);

        // *** METHOD CALL for setEditingEnabled ***
        setEditingEnabled(shapeSelected); // <<< THIS IS THE CALL

        // --- Update Values using helper methods ---
        try {
            updateStringProperty("Object Type", shapeSelected ? shape.getClass().getSimpleName() : "Drawing");
            updateColorProperty("Fore Color", shapeSelected ? shape.getColor() : drawing.getColor());
            updateColorProperty("Fill Color", shapeSelected ? shape.getFill() : drawing.getFill());
            updateColorProperty("Start Color", shapeSelected ? shape.getStartColor() : drawing.getStartColor());
            updateColorProperty("End Color", shapeSelected ? shape.getEndColor() : drawing.getEndColor());
            updateBooleanProperty("Use Gradient", shapeSelected ? shape.isUseGradient() : drawing.isUseGradient());
            updateBooleanProperty("Visible", shapeSelected ? shape.isVisible() : true);

            updateIntegerProperty("X Location", shapeSelected && shape.getLocation() != null ? shape.getLocation().x : 0);
            updateIntegerProperty("Y Location", shapeSelected && shape.getLocation() != null ? shape.getLocation().y : 0);
            updateIntegerProperty("Width", shapeSelected ? shape.getWidth() : 0);
            updateIntegerProperty("Height", shapeSelected ? shape.getHeight() : 0);
            updateIntegerProperty("Line Thickness", shapeSelected ? shape.getThickness() : drawing.getThickness());

            // Text/Font properties
            Font fontToUse = drawing.getFont();
            String textToUse = ""; // Default to empty if no shape or not text
            boolean isTextShape = shapeSelected && "Text".equals(shape.getClass().getSimpleName());

            if (isTextShape) {
                fontToUse = shape.getFont() != null ? shape.getFont() : fontToUse;
                textToUse = shape.getText() != null ? shape.getText() : "";
            } else {
                fontToUse = drawing.getFont(); // Use global default font if no text shape selected
            }

            updateStringProperty("Text", textToUse);
            updateStringProperty("Font Family", fontToUse != null ? fontToUse.getFamily() : "SansSerif");
            updateSelectionProperty("Font Style", fontToUse != null ? fontToUse.getStyle() : Font.PLAIN);
            updateIntegerProperty("Font Size", fontToUse != null ? fontToUse.getSize() : 12);

            // Update Image Action lambda
            Property<?> imageProp = propertyMap.get("Image");
            if (imageProp instanceof ActionProperty) {
                ((ActionProperty) imageProp).setValue(() -> {
                    if (appService != null) {
                        Shape currentSelection = appService.getSelectedShape();
                        if (currentSelection != null && "Picture".equals(currentSelection.getClass().getSimpleName())) {
                            appService.setImageFileename(); // Open dialog
                        } else {
                            System.out.println("Change Image: Select a Picture shape first.");
                        }
                    }
                });
            }
            
            // Update Remove Image Action lambda
            Property<?> removeImageProp = propertyMap.get("Remove Image");
            if (removeImageProp instanceof ActionProperty) {
                ((ActionProperty) removeImageProp).setValue(() -> {
                    if (appService != null) {
                        Shape currentSelection = appService.getSelectedShape();
                        if (currentSelection != null && "Picture".equals(currentSelection.getClass().getSimpleName())) {
                            // Delete the selected image
                            appService.delete(currentSelection);
                            System.out.println("Image removed successfully.");
                        } else {
                            System.out.println("Remove Image: Select a Picture shape first.");
                        }
                    }
                });
            }
        } catch (Exception ex) {
            System.err.println("Exception during PropertySheet.populateTable value updates:");
            ex.printStackTrace();
        } finally {
            isPopulating = false; // <<< Ensure this is ALWAYS reset
        }

        // --- Refresh Table UI ---
        if (isEditing()) {
            TableCellEditor editor = getCellEditor();
            if (editor != null) editor.stopCellEditing();
        }
        if (propertyModel != null) {
            propertyModel.fireTableDataChanged(); // Refresh display
        }
    }


    // --- Helper methods to UPDATE values in properties and model ---
    // (These remain the same - they update values using propertyMap and model)
    private void updateValue(String name, Object value) {
        if (isPopulating) return;

        Property<?> p = propertyMap.get(name);
        if (p == null) return;

        int rowIndex = getPropertyRowIndex(name);
        if (rowIndex == -1) return;

        try {
            Object currentPropertyValue = p.getValue();
            if (!Objects.equals(currentPropertyValue, value)) {
                if (value == null || p.getClass().getDeclaredField("value").getType().isAssignableFrom(value.getClass())) {
                    if (isEditing() && getEditingRow() == rowIndex && getEditingColumn() == 1) {
                        getCellEditor(rowIndex, 1).stopCellEditing();
                    }
                    ((Property<Object>) p).setValue(value);
                    if (propertyModel != null) {
                        propertyModel.setValueAt(value, rowIndex, 1);
                    }
                } else {
                    System.err.println("Type mismatch prevented updateValue for '" + name + "'. Expected compatible with " + p.getClass().getDeclaredField("value").getType() + ", got " + value.getClass());
                }
            }
        } catch (Exception e) {
            System.err.println("Error in updateValue for '" + name + "': " + e.getMessage());
        }
    }

    private void updateStringProperty(String name, String value) { updateValue(name, value != null ? value : ""); }
    private void updateIntegerProperty(String name, int value) { updateValue(name, value); }
    private void updateBooleanProperty(String name, boolean value) { updateValue(name, value); }
    private void updateColorProperty(String name, Color value) { updateValue(name, value != null ? value : Color.GRAY); }
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
        updateValue(name, (matchingItem != null) ? matchingItem.getValue() : null);
    }

    // Helper to find row index
    private int getPropertyRowIndex(String name) {
        // (Keep previous implementation)
        if (name == null || propertyModel == null) return -1;
        for (int i = 0; i < propertyModel.getRowCount(); i++) {
            Object nameInRow = propertyModel.getValueAt(i, 0);
            if (name.equals(nameInRow)) {
                return i;
            }
        }
        return -1;
    }

    // --- isCellEditable logic ---
    // (Keep previous implementation)
    @Override
    public boolean isCellEditable(int row, int column) {
        if (column != 1) return false;
        if (row < 0 || row >= getRowCount()) return false;
        Object propNameObj = getValueAt(row, 0);
        if (!(propNameObj instanceof String)) return false;
        String propName = (String) propNameObj;

        if ("Object Type".equals(propName)) return false;
        if ("Image".equals(propName)) {
            return editingEnabled && "Picture".equals(getStringValueFromModel("Object Type"));
        }
        if ("Remove Image".equals(propName)) {
            return editingEnabled && "Picture".equals(getStringValueFromModel("Object Type"));
        }
        if (!editingEnabled) {
            return "Fore Color".equals(propName) || "Fill Color".equals(propName) ||
                    "Start Color".equals(propName) || "End Color".equals(propName) ||
                    "Use Gradient".equals(propName) || "Line Thickness".equals(propName) ||
                    "Font Family".equals(propName) || "Font Style".equals(propName) || "Font Size".equals(propName);
        }
        boolean isTextShape = "Text".equals(getStringValueFromModel("Object Type"));
        if (!isTextShape && ("Text".equals(propName) || "Font Family".equals(propName) || "Font Style".equals(propName) || "Font Size".equals(propName))) {
            return false;
        }
        return true;
    }

    // Helper to get string value from model
    private String getStringValueFromModel(String propertyName) {
        // (Keep previous implementation)
        int rowIndex = getPropertyRowIndex(propertyName);
        if (rowIndex != -1 && propertyModel != null && rowIndex < propertyModel.getRowCount()) {
            Object val = propertyModel.getValueAt(rowIndex, 1);
            return (val != null) ? val.toString() : null;
        }
        return null;
    }

    // --- prepareRenderer / prepareEditor (Visuals) ---
    // (Keep previous implementations)
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (c == null) return null;
        boolean editable = isCellEditable(row, column);
        boolean rowIsSelected = isRowSelected(row);
        c.setEnabled(editable);
        Color background;
        Color foreground;
        if (rowIsSelected) {
            background = getSelectionBackground();
            foreground = getSelectionForeground();
            if (!editable && column == 1) {
                background = getSelectionBackground().darker();
                foreground = Color.LIGHT_GRAY;
            }
        } else {
            if (!editable && column == 1) {
                background = getBackground().darker();
                foreground = Color.GRAY;
            } else {
                background = getBackground();
                foreground = getForeground();
            }
        }
        c.setBackground(background);
        c.setForeground(foreground);
        if (c instanceof JCheckBox) {
            ((JCheckBox)c).setOpaque(true);
            ((JCheckBox)c).setBackground(background);
        }
        if (c instanceof JLabel && !editable && column == 1 && !rowIsSelected) {
            c.setForeground(Color.GRAY);
        }
        return c;
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component c = super.prepareEditor(editor, row, column);
        if (c == null) return null;
        c.setEnabled(isCellEditable(row, column));
        return c;
    }

    // --- Tooltip ---
    // (Keep previous implementation)
    @Override
    public String getToolTipText(java.awt.event.MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        int col = columnAtPoint(event.getPoint());
        if (row >= 0 && col >= 0) {
            if (col == 0) {
                Object propNameObj = getValueAt(row, 0);
                if (propNameObj instanceof String) return (String) propNameObj;
            } else if (col == 1) {
                Object value = getValueAt(row, 1);
                if (value != null) return value.toString();
            }
        }
        return super.getToolTipText(event);
    }

    // REMOVED: These overrides were preventing rows from being added during initialization!
    // The internal addInternalProperty method is the only one that should add properties anyway.
}