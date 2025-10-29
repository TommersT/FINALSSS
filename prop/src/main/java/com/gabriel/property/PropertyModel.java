package com.gabriel.property;

import javax.swing.table.DefaultTableModel;

public class PropertyModel extends DefaultTableModel {
    public PropertyModel(String[] headers) {
        super(null, headers); // Start empty, headers are set
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        // Let PropertySheet handle actual editability logic
        return (column == 1);
    }

    /**
     * Efficiently removes all rows from the table model.
     */
    public void clear() {
        setRowCount(0); // Use the efficient DefaultTableModel method
    }
}