package com.gabriel.property;

import com.gabriel.property.cell.AbstractCellComponent;
import com.gabriel.property.event.EventDispatcher;
import com.gabriel.property.event.PropertyEventListener;
import com.gabriel.property.exception.PropertyNotSupportedException;
import com.gabriel.property.property.*;
import com.gabriel.property.property.selection.SelectionProperty;
import com.gabriel.property.cell.*; // Import cell components

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.util.ArrayList;
import java.util.List;

public class PropertyPanel extends JTable {
    protected PropertyOptions options;
    protected PropertyModel propertyModel;
    protected EventDispatcher eventDispatcher;
    protected List<AbstractCellComponent> cellComponents;
    protected List<Property> properties;
    private SelectionCellComponent selectionCellComponent; // Keep if needed

    public PropertyPanel(PropertyOptions options) {
        this.options = options;
        // Create the model FIRST
        this.propertyModel = new PropertyModel(options.getHeaders());
        // Set the model IMMEDIATELY
        setModel(propertyModel);

        // Initialize other components
        this.eventDispatcher = new EventDispatcher();
        this.cellComponents = new ArrayList<>();
        this.properties = new ArrayList<>();

        // Configure table appearance AFTER setting the model
        setRowHeight(options.getRowHeight());
        getTableHeader().setReorderingAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Optional: setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // If columns need specific widths
    }

    // --- Core method to add a row structure ---
    public void addProperty(Property property, AbstractCellComponent cellComponent) {
        if (propertyModel == null) {
            System.err.println("PropertyPanel.addProperty: propertyModel is null!");
            return;
        }
        // Add row data
        propertyModel.addRow(new Object[]{property.getName(), property.getValue()});
        // Store references needed for rendering/editing
        cellComponents.add(cellComponent);
        properties.add(property);
        // Initialize the cell
        cellComponent.init(options, eventDispatcher);
        // Optional: dispatch event
        // eventDispatcher.dispatchPropertyAddedEvent(property);
    }

    // --- Convenience method using default cell components ---
    public void addProperty(Property property) throws PropertyNotSupportedException {
        AbstractCellComponent cellComp = createCellComponent(property);
        addProperty(property, cellComp);
    }

    // --- Helper to create default cell components ---
    private AbstractCellComponent createCellComponent(Property property) throws PropertyNotSupportedException {
        if (property instanceof IntegerProperty) {
            return new IntegerCellComponent((IntegerProperty) property);
        } else if (property instanceof LongProperty) {
            return new LongCellComponent((LongProperty) property);
        } else if (property instanceof DoubleProperty) {
            return new DoubleCellComponent((DoubleProperty) property);
        } else if (property instanceof FloatProperty) {
            return new FloatCellComponent((FloatProperty) property);
        } else if (property instanceof StringProperty) {
            return new StringCellComponent((StringProperty) property);
        } else if (property instanceof ColorProperty) {
            return new ColorCellComponent((ColorProperty) property);
        } else if (property instanceof BooleanProperty) {
            return new BooleanCellComponent((BooleanProperty) property);
        } else if (property instanceof SelectionProperty) {
            selectionCellComponent = new SelectionCellComponent((SelectionProperty) property);
            return selectionCellComponent;
        } else if (property instanceof ActionProperty) {
            return new ActionCellComponent((ActionProperty) property);
        } else {
            throw new PropertyNotSupportedException(property);
        }
    }

    // --- Methods for removing properties ---
    public void removeProperty(Property property) {
        int index = properties.indexOf(property);
        if (index != -1) {
            removeProperty(index);
        }
    }

    public void removeProperty(int row) {
        if (row >= 0 && row < properties.size()) {
            cellComponents.remove(row);
            properties.remove(row);
            propertyModel.removeRow(row);
        }
    }

    // --- Clear method ---
    // Subclasses may override this to re-initialize structure
    public void clear() {
        if (propertyModel != null) {
            propertyModel.clear(); // Uses efficient setRowCount(0)
        }
        cellComponents.clear();
        properties.clear();
    }

    // --- Event Listener methods ---
    public void addEventListener(PropertyEventListener eventListener) {
        eventDispatcher.addEventListener(eventListener);
    }

    public void removeEventListener(PropertyEventListener eventListener) {
        eventDispatcher.removeEventListener(eventListener);
    }

    // --- Cell Editor/Renderer delegation ---
    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        if (column == 1 && row >= 0 && row < cellComponents.size()) {
            return cellComponents.get(row);
        }
        return super.getCellEditor(row, column);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (column == 1 && row >= 0 && row < cellComponents.size()) {
            return cellComponents.get(row);
        }
        return super.getCellRenderer(row, column);
    }

    public SelectionCellComponent getSelectionCellComponent() {
        return selectionCellComponent;
    }
}