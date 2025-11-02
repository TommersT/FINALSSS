package com.gabriel.property;

import javax.swing.table.DefaultTableModel;

public class PropertyModel extends DefaultTableModel {
    public PropertyModel(String[] headers) {
        super(null, headers);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return (column == 1);
    }

    public void clear() {
        setRowCount(0);
    }
}
