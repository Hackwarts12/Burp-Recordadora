package org.example;

import javax.swing.table.AbstractTableModel;

public class PersistTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    private final PersistStore store;

    public static final String[] COLS = {
        "N°","Fecha/Hora","Tool","Método","Host","URL","Status","Mime","Size","Time(ms)"
    };

    public PersistTableModel(PersistStore store) {
        this.store = store;
    }

    @Override public int getRowCount() { return store.size(); }
    @Override public int getColumnCount() { return COLS.length; }
    @Override public String getColumnName(int c) { return COLS[c]; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        PersistEntry e = store.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> e.seq();
            case 1 -> e.time();
            case 2 -> e.toolName();
            case 3 -> e.method();
            case 4 -> e.host();
            case 5 -> e.url();
            case 6 -> e.status();
            case 7 -> e.mime();
            case 8 -> e.size();
            case 9 -> e.timeMs();
            default -> "";
        };
    }
}
