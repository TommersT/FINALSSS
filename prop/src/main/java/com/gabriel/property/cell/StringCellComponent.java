package com.gabriel.property.cell;

import com.gabriel.property.property.IntegerProperty;
import com.gabriel.property.util.Keys; // Assuming Keys utility is correctly defined

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Objects; // For comparing values

public class IntegerCellComponent extends AbstractCellComponent {

    private IntegerProperty property;
    private JTextField textField;
    private Integer originalValue; // Store value when editing starts

    public IntegerCellComponent(IntegerProperty property) {
        this.property = property;
        this.textField = new JTextField();
        // Right-align text for numbers
        textField.setHorizontalAlignment(JTextField.RIGHT);

        // Use ActionListener for Enter key press to commit edit
        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commitEdit();
                fireEditingStopped(); // Notify table editing has stopped
            }
        });

        // Use KeyAdapter for live validation feedback
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent event) {
                if (Keys.ignoreKey(event.getKeyCode()) || event.getKeyCode() == KeyEvent.VK_ENTER) {
                    return;
                }
                // Validate raw text input
                if (property.getValidator().validate(textField.getText())) {
                    textField.setBackground(options.getBackgroundColor());
                } else {
                    textField.setBackground(options.getInvalidColor());
                }
            }
        });
    }

    // Called when editing starts
    @Override
    public Component getTableCellEditorComponent(JTable jTable, Object value, boolean isSelected, int row, int column) {
        originalValue = property.getValue();
        textField.setText(originalValue != null ? originalValue.toString() : "0");
        textField.setBackground(options.getBackgroundColor());
        SwingUtilities.invokeLater(() -> textField.selectAll());
        return textField;
    }

    // Called when editing stops
    @Override
    public Object getCellEditorValue() {
        // Return the validated Integer value, or original if invalid
        String text = textField.getText();
        if (property.getValidator().validate(text)) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                return originalValue; // Should not happen if validator passed, but safety check
            }
        } else {
            return originalValue; // Return original if validation failed during commit attempt
        }
    }

    // Method to validate, parse, update property, and dispatch event
    private void commitEdit() {
        String text = textField.getText();
        if (property.getValidator().validate(text)) {
            try {
                int newValue = Integer.parseInt(text);
                // Check if value actually changed
                if (!Objects.equals(originalValue, newValue)) {
                    property.setValue(newValue); // Update the property object
                    if (eventDispatcher != null) {
                        eventDispatcher.dispatchUpdateEvent(property); // Notify listeners
                    }
                }
                textField.setBackground(options.getBackgroundColor());
            } catch (NumberFormatException e) {
                // Validator passed, but parsing failed? Should not happen.
                textField.setBackground(options.getInvalidColor());
            }
        } else {
            textField.setBackground(options.getInvalidColor());
        }
    }

    // Override stopCellEditing to perform commit logic
    @Override
    public boolean stopCellEditing() {
        commitEdit();
        return super.stopCellEditing();
    }

    // Cancel editing - revert to original value
    @Override
    public void cancelCellEditing() {
        super.cancelCellEditing();
        textField.setText(originalValue != null ? originalValue.toString() : "0");
        textField.setBackground(options.getBackgroundColor());
    }

    // Renderer part (displays value when not editing)
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String displayValue = (property.getValue() != null) ? property.getValue().toString() : "0";

        JLabel label = new JLabel(displayValue);
        label.setOpaque(true);
        label.setHorizontalAlignment(JLabel.RIGHT); // Right-align numbers

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
}