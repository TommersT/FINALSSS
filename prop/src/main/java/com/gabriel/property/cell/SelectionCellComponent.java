package com.gabriel.property.cell;

import com.gabriel.property.property.selection.Item;
import com.gabriel.property.property.selection.SelectionProperty;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent; // <<<--- ADDED for ActionListener
import java.awt.event.ActionListener; // <<<--- ADDED for ActionListener
import java.util.Objects; // For comparing values
import com.gabriel.property.property.Property; // <<<--- ADD THIS LINE

public class SelectionCellComponent extends AbstractCellComponent {

    private SelectionProperty<?> property; // Use wildcard for type safety
    private JComboBox<Item<?>> comboBox; // Use wildcard
    private Object originalPropertyValue; // Store original property *value*

    public SelectionCellComponent(SelectionProperty<?> property) { // Use wildcard
        this.property = property;
        this.comboBox = new JComboBox<>();

        // Populate combo box - ensure type safety
        for (Item<?> item : property.getItems()) {
            comboBox.addItem(item);
        }

        // Use ActionListener for better control over selection changes
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Check if it's a selection event from the user (not programmatic)
                if ("comboBoxChanged".equals(e.getActionCommand())) {
                    Item<?> selectedItem = (Item<?>) comboBox.getSelectedItem();
                    if (selectedItem != null) {
                        Object newValue = selectedItem.getValue();
                        // Update property *only if value changed* and dispatch
                        if (!Objects.equals(property.getValue(), newValue)) {
                            try {
                                // Cast needed for setting value - relies on correct Item type
                                ((Property<Object>)property).setValue(newValue);
                                if (eventDispatcher != null) {
                                    eventDispatcher.dispatchUpdateEvent(property);
                                }
                                // Stop editing after successful selection change
                                // fireEditingStopped(); // Let stopCellEditing handle this
                            } catch (ClassCastException cce) {
                                System.err.println("SelectionCellComponent: Error setting property value due to type mismatch.");
                            }
                        }
                    }
                }
            }
        });
    }

    // Called when editing starts
    @Override
    public Component getTableCellEditorComponent(JTable jTable, Object value, boolean isSelected, int row, int column) {
        originalPropertyValue = property.getValue(); // Store original *value*

        // Find and select the Item corresponding to the current property value
        Item<?> itemToSelect = null;
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            Item<?> item = comboBox.getItemAt(i);
            if (item != null && Objects.equals(item.getValue(), originalPropertyValue)) {
                itemToSelect = item;
                break;
            }
        }
        comboBox.setSelectedItem(itemToSelect); // Select the correct item

        return comboBox;
    }

    // Called when editing stops - returns the *selected Item's value*
    @Override
    public Object getCellEditorValue() {
        Item<?> selectedItem = (Item<?>) comboBox.getSelectedItem();
        return (selectedItem != null) ? selectedItem.getValue() : originalPropertyValue; // Return value, not Item
    }


    // Optional: Override stopCellEditing if needed, but ActionListener should handle updates
    // @Override
    // public boolean stopCellEditing() {
    //     // The ActionListener already updated the property and dispatched the event
    //     return super.stopCellEditing();
    // }

    // Optional: Override cancelCellEditing
    // @Override
    // public void cancelCellEditing() {
    //     super.cancelCellEditing();
    //     // Optionally reset combo box selection if needed, but usually not required
    // }


    // Renderer part (displays value when not editing)
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // 'value' from the model should be the currently selected Item object
        // if populateTable sets it correctly. If not, find the item based on property value.

        Item<?> itemToDisplay = null;
        Object currentPropertyValue = property.getValue();

        // Find the Item matching the current property value
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            Item<?> item = comboBox.getItemAt(i);
            if (item != null && Objects.equals(item.getValue(), currentPropertyValue)) {
                itemToDisplay = item;
                break;
            }
        }

        String displayText = (itemToDisplay != null) ? itemToDisplay.getDescription() : "N/A";

        JLabel label = new JLabel(displayText);
        label.setOpaque(true);

        if (isSelected) {
            label.setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        } else {
            label.setBackground(table.getBackground());
            label.setForeground(table.getForeground());
        }
        label.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        return label;
    }

    // Public method needed by PropertySheet to update the combo box selection visually
    // when populateTable runs
    public void setEditorValueBasedOnProperty() {
        originalPropertyValue = property.getValue(); // Store value being set
        Item<?> itemToSelect = null;
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            Item<?> item = comboBox.getItemAt(i);
            if (item != null && Objects.equals(item.getValue(), originalPropertyValue)) {
                itemToSelect = item;
                break;
            }
        }
        // Prevent listener firing during programmatic update
        ActionListener[] listeners = comboBox.getActionListeners();
        for(ActionListener l : listeners) comboBox.removeActionListener(l);

        comboBox.setSelectedItem(itemToSelect);

        // Re-add listeners
        for(ActionListener l : listeners) comboBox.addActionListener(l);
    }
}