package org.example;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class PersistUI {
    private final MontoyaApi api;
    private final PersistStore store;

    private final JPanel root;
    private final JTable table;
    private final PersistTableModel model;

    private final JTextField txtFilter;
    private final JCheckBox chkRegex, chkMatchCase, chkWrap, chkSearchRaw;
    private final JComboBox<String> cbTool, cbMethod, cbStatus;
    private final JButton btnExport, btnImport, btnClear, btnPackCols, btnPause, btnCols;

    private final HttpRequestEditor reqEditor;
    private final HttpResponseEditor respEditor;

    private final TableRowSorter<PersistTableModel> sorter;

    private final java.util.Map<Integer, TableColumn> hiddenColumns = new java.util.HashMap<>();

    private static final Color GREEN = new Color(40, 167, 69);
    private static final Color RED   = new Color(220, 53, 69);

    public PersistUI(MontoyaApi api, PersistStore store) {
        this.api = api;
        this.store = store;

        this.root = new JPanel(new BorderLayout(6, 6));

        // ── Barra superior ───────────────────────────────────────────
        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));

        JLabel lblTitle = new JLabel("Recordadora");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 14f));

        btnExport = new JButton("Exportar (.log)");
        btnImport = new JButton("Importar (.log)");
        btnClear  = new JButton("Limpiar");
        chkWrap   = new JCheckBox("Ajuste de línea", true);
        btnPause  = new JButton("Pausar");
        stylePauseButton();

        btnPackCols = new JButton("Ajustar columnas");
        btnCols     = new JButton("Columnas…");

        actions.add(lblTitle);
        actions.add(Box.createHorizontalStrut(10));
        actions.add(btnExport);
        actions.add(Box.createHorizontalStrut(6));
        actions.add(btnImport);
        actions.add(Box.createHorizontalStrut(6));
        actions.add(btnClear);
        actions.add(Box.createHorizontalStrut(12));
        actions.add(chkWrap);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(btnPause);
        actions.add(Box.createHorizontalGlue());
        actions.add(btnPackCols);
        actions.add(Box.createHorizontalStrut(6));
        actions.add(btnCols);

        // ── Barra de filtros ─────────────────────────────────────────
        JPanel filterContainer = new JPanel(new BorderLayout(8, 4));
        txtFilter = new JTextField(50);
        txtFilter.setMinimumSize(new Dimension(300, txtFilter.getPreferredSize().height));

        chkSearchRaw = new JCheckBox("Buscar en RAW", true);
        chkRegex     = new JCheckBox("Regex");
        chkMatchCase = new JCheckBox("Match case");
        cbTool   = new JComboBox<>(new String[]{"Any","Proxy","Repeater","Intruder","Extender","Target","Scanner","Sequencer","Comparer","Decoder","Logger"});
        cbMethod = new JComboBox<>(new String[]{"Any","GET","POST","PUT","DELETE","PATCH","HEAD","OPTIONS"});
        cbStatus = new JComboBox<>(new String[]{"Any","200","201","204","301","302","400","401","403","404","500","502","503"});

        JPanel filterLeft = new JPanel(new BorderLayout(6, 0));
        filterLeft.add(new JLabel("Filtro:"), BorderLayout.WEST);
        filterLeft.add(txtFilter, BorderLayout.CENTER);
        filterContainer.add(filterLeft, BorderLayout.CENTER);

        JPanel filterRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        filterRight.add(chkSearchRaw);
        filterRight.add(chkRegex);
        filterRight.add(chkMatchCase);
        filterRight.add(new JLabel("Tool:"));
        filterRight.add(cbTool);
        filterRight.add(new JLabel("Método:"));
        filterRight.add(cbMethod);
        filterRight.add(new JLabel("Status:"));
        filterRight.add(cbStatus);
        filterContainer.add(filterRight, BorderLayout.EAST);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(actions);
        north.add(filterContainer);
        root.add(north, BorderLayout.NORTH);

        // ── Tabla ────────────────────────────────────────────────────
        this.model = new PersistTableModel(store);
        this.table = new JTable(model);
        table.setRowHeight(22);
        table.setIntercellSpacing(new Dimension(6, 1));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setInitialColumnWidths();
        installRenderers();

        this.sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        JScrollPane spTable = new JScrollPane(table);

        // ── Editores ─────────────────────────────────────────────────
        this.reqEditor  = api.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);
        this.respEditor = api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY);

        JSplitPane editorsSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            wrapWithTitled(reqEditor.uiComponent(), "REQUEST"),
            wrapWithTitled(respEditor.uiComponent(), "RESPONSE")
        );
        editorsSplit.setResizeWeight(0.5);
        editorsSplit.setDividerLocation(0.5);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spTable, editorsSplit);
        mainSplit.setResizeWeight(0.55);
        mainSplit.setDividerLocation(0.55);
        root.add(mainSplit, BorderLayout.CENTER);

        // ── Filtros ──────────────────────────────────────────────────
        Runnable applyFilters = () -> sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends PersistTableModel, ? extends Integer> entry) {
                int r = entry.getIdentifier();
                PersistEntry e = store.get(r);

                String selTool = (String) cbTool.getSelectedItem();
                if (!"Any".equals(selTool) && !e.toolName().equalsIgnoreCase(selTool)) return false;

                String selMethod = (String) cbMethod.getSelectedItem();
                if (!"Any".equals(selMethod) && !e.method().equalsIgnoreCase(selMethod)) return false;

                String selStatus = (String) cbStatus.getSelectedItem();
                if (!"Any".equals(selStatus) && (e.status() == null || !selStatus.equals(String.valueOf(e.status())))) return false;

                String q = txtFilter.getText().trim();
                if (!q.isEmpty()) {
                    String haystack = chkSearchRaw.isSelected()
                        ? e.rawConcat()
                        : (e.url() + "\n" + Optional.ofNullable(e.host()).orElse(""));
                    int flags = chkMatchCase.isSelected() ? 0 : Pattern.CASE_INSENSITIVE;
                    if (chkRegex.isSelected()) {
                        try {
                            if (!Pattern.compile(q, flags).matcher(haystack).find()) return false;
                        } catch (Exception ex) { /* regex inválida */ }
                    } else {
                        String H = chkMatchCase.isSelected()? haystack : haystack.toLowerCase();
                        String Q = chkMatchCase.isSelected()? q : q.toLowerCase();
                        if (!H.contains(Q)) return false;
                    }
                }
                return true;
            }
        });
        cbTool.addActionListener(e -> applyFilters.run());
        cbMethod.addActionListener(e -> applyFilters.run());
        cbStatus.addActionListener(e -> applyFilters.run());
        chkSearchRaw.addActionListener(e -> applyFilters.run());
        chkRegex.addActionListener(e -> applyFilters.run());
        chkMatchCase.addActionListener(e -> applyFilters.run());
        txtFilter.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e){ applyFilters.run(); }
            public void removeUpdate(DocumentEvent e){ applyFilters.run(); }
            public void changedUpdate(DocumentEvent e){ applyFilters.run(); }
        });

        // Selección → editores
        table.getSelectionModel().addListSelectionListener(e -> {
            int v = table.getSelectedRow();
            if (v < 0) return;
            int m = table.convertRowIndexToModel(v);
            PersistEntry pe = store.get(m);
            reqEditor.setRequest(pe.request());
            respEditor.setResponse(pe.response() != null ? pe.response() : null);
        });

        // Refresco por eventos del store
        store.addListener(() -> model.fireTableDataChanged());

        // Acciones
        btnClear.addActionListener(e -> {
            store.clear();
            reqEditor.setRequest(null);
            respEditor.setResponse(null);
        });
        btnPackCols.addActionListener(e -> packColumns(table));
        btnCols.addActionListener(e -> showColumnChooser());
        btnPause.addActionListener(e -> togglePause());
        btnExport.addActionListener(e -> doExport());
        btnImport.addActionListener(e -> doImport());

        // Menú contextual a lo Burp
        attachBurpLikeContextMenu(table);

        // Mensaje estilo “Recordadora”
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(
                root,
                "✨ Está activa tu Recordadora, se cargó bien.",
                "Recordadora",
                JOptionPane.INFORMATION_MESSAGE
            )
        );
    }

    // ── Renderers / estética ────────────────────────────────────────
    private void installRenderers() {
        table.getColumnModel().getColumn(6).setCellRenderer(new StatusCellRenderer());
        DefaultTableCellRenderer right = new RightAlignRenderer();
        table.getColumnModel().getColumn(8).setCellRenderer(right); // Size
        table.getColumnModel().getColumn(9).setCellRenderer(right); // Time(ms)
    }

    static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(CENTER);
            if (!isSelected) {
                setForeground(Color.BLACK);
                int s = -1;
                try { if (value != null) s = Integer.parseInt(value.toString()); } catch (Exception ignored) {}
                setBackground(badgeFor(s));
            }
            setText(value == null ? "" : value.toString());
            return this;
        }
        private Color badgeFor(int s){
            if      (s >= 200 && s < 300) return new Color(0xC8E6C9); // pastel green
            else if (s >= 300 && s < 400) return new Color(0xBBDEFB); // pastel blue
            else if (s >= 400 && s < 500) return new Color(0xFFE0B2); // pastel amber
            else if (s >= 500)            return new Color(0xFFCDD2); // pastel red
            return new Color(0xE0E0E0);                               // pastel grey
        }
    }
    static class RightAlignRenderer extends DefaultTableCellRenderer {
        private final NumberFormat nf = NumberFormat.getIntegerInstance(Locale.getDefault());
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(RIGHT);
            if (value instanceof Number n) setText(nf.format(n.longValue()));
            return this;
        }
    }

    // ── Export / Import ─────────────────────────────────────────────
    private void doExport() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar log de Recordadora");
        fc.setFileFilter(new FileNameExtensionFilter("Persist Logs (*.log)", "log"));
        if (fc.showSaveDialog(root) == JFileChooser.APPROVE_OPTION) {
            File chosen = fc.getSelectedFile();
            if (!chosen.getName().toLowerCase().endsWith(".log")) {
                chosen = new File(chosen.getParentFile(), chosen.getName() + ".log");
            }
            if (chosen.exists()) {
                int opt = JOptionPane.showConfirmDialog(
                    root,
                    "El archivo ya existe:\n" + chosen.getAbsolutePath() + "\n¿Deseas sobrescribirlo?",
                    "Confirmar sobrescritura",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (opt != JOptionPane.YES_OPTION) return;
            }
            try {
                int count = LogIO.exportTo(chosen, store);
                JOptionPane.showMessageDialog(
                    root,
                    "Exportación completada.\nEntradas guardadas: " + count +
                    "\nArchivo: " + chosen.getAbsolutePath(),
                    "Exportado correctamente",
                    JOptionPane.INFORMATION_MESSAGE
                );
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(root, "Error exportando:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doImport() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Importar log de Recordadora");
        fc.setFileFilter(new FileNameExtensionFilter("Persist Logs (*.log)", "log"));
        if (fc.showOpenDialog(root) == JFileChooser.APPROVE_OPTION) {
            File chosen = fc.getSelectedFile();
            try {
                LogIO.ImportResult r = LogIO.importFrom(chosen, store);
                if (r.imported > 0 && r.skipped == 0) {
                    JOptionPane.showMessageDialog(
                        root,
                        "Importación completada.\nBloques detectados: " + r.totalBlocks +
                        "\nEntradas importadas: " + r.imported,
                        "Importado correctamente",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                } else if (r.imported > 0) {
                    JOptionPane.showMessageDialog(
                        root,
                        "Importación parcialmente completada.\nBloques: " + r.totalBlocks +
                        "\nImportadas: " + r.imported + "\nIgnoradas: " + r.skipped +
                        (r.errors.isEmpty() ? "" : "\n\nDetalles:\n- " + String.join("\n- ", r.errors)),
                        "Importación con advertencias",
                        JOptionPane.WARNING_MESSAGE
                    );
                } else {
                    JOptionPane.showMessageDialog(
                        root,
                        "No se pudo importar ninguna entrada.\nBloques detectados: " + r.totalBlocks +
                        (r.errors.isEmpty() ? "" : "\n\nDetalles:\n- " + String.join("\n- ", r.errors)),
                        "Importación fallida",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(root, "Error importando:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Pausa/Reanudar ──────────────────────────────────────────────
    private void togglePause() {
        boolean newState = !store.isPaused();
        store.setPaused(newState);
        stylePauseButton();
    }
    private void stylePauseButton() {
        boolean paused = store.isPaused();
        btnPause.setText(paused ? "Reanudar" : "Pausar");
        btnPause.setForeground(Color.WHITE);
        btnPause.setBackground(paused ? RED : GREEN);
        btnPause.setOpaque(true);
        btnPause.setContentAreaFilled(true);
        btnPause.setBorderPainted(false);
        btnPause.setToolTipText(paused ? "Reanudar captura" : "Pausar captura");
    }

    // ── Columnas visibles ───────────────────────────────────────────
    private void showColumnChooser() {
        String[] names = PersistTableModel.COLS;
        JCheckBox[] checks = new JCheckBox[names.length];
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 6));

        TableColumnModel cm = table.getColumnModel();
        java.util.Set<Integer> visible = new java.util.HashSet<>();
        for (int i = 0; i < cm.getColumnCount(); i++) visible.add(cm.getColumn(i).getModelIndex());

        for (int i = 0; i < names.length; i++) {
            checks[i] = new JCheckBox(names[i], visible.contains(i));
            panel.add(checks[i]);
        }

        int res = JOptionPane.showConfirmDialog(root, panel, "Columnas visibles", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        for (int i = 0; i < names.length; i++) {
            boolean wantVisible = checks[i].isSelected();
            boolean isVisible   = visible.contains(i);

            if (wantVisible && !isVisible) {
                TableColumn col = hiddenColumns.remove(i);
                if (col != null) {
                    table.getColumnModel().addColumn(col);
                    moveColumnToModelIndex(i);
                }
            } else if (!wantVisible && isVisible) {
                TableColumn col = getColumnByModelIndex(i);
                if (col != null) {
                    hiddenColumns.put(i, col);
                    table.getColumnModel().removeColumn(col);
                }
            }
        }
        table.revalidate();
        table.repaint();
    }
    private TableColumn getColumnByModelIndex(int modelIndex) {
        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn c = cm.getColumn(i);
            if (c.getModelIndex() == modelIndex) return c;
        }
        return null;
    }
    private void moveColumnToModelIndex(int modelIndex) {
        TableColumnModel cm = table.getColumnModel();
        int to = 0;
        for (int i = 0; i < cm.getColumnCount(); i++) {
            if (cm.getColumn(i).getModelIndex() < modelIndex) to = i + 1;
        }
        int from = -1;
        for (int i = 0; i < cm.getColumnCount(); i++) {
            if (cm.getColumn(i).getModelIndex() == modelIndex) { from = i; break; }
        }
        if (from != -1 && from != to) table.moveColumn(from, to);
    }

    // ── Utilidades de tabla ─────────────────────────────────────────
    private void setInitialColumnWidths() {
        int[] widths = {60, 160, 80, 80, 220, 700, 80, 140, 90, 90};
        TableColumnModel tcm = table.getColumnModel();
        for (int i = 0; i < Math.min(widths.length, tcm.getColumnCount()); i++) {
            TableColumn col = tcm.getColumn(i);
            col.setPreferredWidth(widths[i]);
            col.setMinWidth(Math.min(50, widths[i]));
        }
    }

    private void packColumns(JTable table) {
        TableColumnModel colModel = table.getColumnModel();
        int margin = 12;
        int maxRows = Math.min(200, table.getRowCount());

        for (int col = 0; col < table.getColumnCount(); col++) {
            int width;
            TableColumn tc = colModel.getColumn(col);
            TableCellRenderer renderer = tc.getHeaderRenderer();
            if (renderer == null) renderer = table.getTableHeader().getDefaultRenderer();
            Component comp = renderer.getTableCellRendererComponent(table, tc.getHeaderValue(), false, false, 0, col);
            width = comp.getPreferredSize().width;

            for (int row = 0; row < maxRows; row++) {
                renderer = table.getCellRenderer(row, col);
                comp = table.prepareRenderer(renderer, row, col);
                width = Math.max(width, comp.getPreferredSize().width);
            }
            width += margin;
            tc.setPreferredWidth(width);
        }
    }

    private JPanel wrapWithTitled(Component c, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    public JComponent root() { return root; }

    // ── Menú contextual ─────────────────────────────────────────────
    private void attachBurpLikeContextMenu(JTable table) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem miSendToRepeater = new JMenuItem("Send to Repeater");
        JMenuItem miSendToIntruder = new JMenuItem("Send to Intruder");
        JMenuItem miSendToComparer = new JMenuItem("Send to Comparer");
        JMenuItem miSendToDecoder  = new JMenuItem("Send to Decoder");
        popup.add(miSendToRepeater); popup.add(miSendToIntruder);
        popup.add(miSendToComparer); popup.add(miSendToDecoder);
        popup.addSeparator();
        JMenuItem miCopyUrl  = new JMenuItem("Copy URL");
        JMenuItem miCopyReq  = new JMenuItem("Copy Request");
        JMenuItem miCopyResp = new JMenuItem("Copy Response");
        popup.add(miCopyUrl); popup.add(miCopyReq); popup.add(miCopyResp);

        java.util.function.Supplier<Optional<PersistEntry>> sel = () -> {
            int row = table.getSelectedRow();
            if (row < 0) return Optional.empty();
            int modelRow = table.convertRowIndexToModel(row);
            return Optional.of(store.get(modelRow));
        };

        miSendToRepeater.addActionListener(ev ->
            sel.get().ifPresent(e -> {
                if (e.request() != null) api.repeater().sendToRepeater(e.request(), "recordadora");
            })
        );
        miSendToIntruder.addActionListener(ev ->
            sel.get().ifPresent(e -> {
                if (e.request() != null) api.intruder().sendToIntruder(e.request());
            })
        );
        miSendToComparer.addActionListener(ev ->
            sel.get().ifPresent(e -> {
                if (e.request() != null) api.comparer().sendToComparer(e.request().toByteArray());
                if (e.response() != null) api.comparer().sendToComparer(e.response().toByteArray());
            })
        );
        miSendToDecoder.addActionListener(ev ->
            sel.get().ifPresent(e -> {
                if (e.request() != null) api.decoder().sendToDecoder(e.request().toByteArray());
            })
        );

        miCopyUrl.addActionListener(ev ->
            sel.get().ifPresent(e -> {
                String u = e.url();
                if (u == null || u.isEmpty()) u = (e.host() == null ? "" : e.host());
                if (u != null) copy(u);
            })
        );
        miCopyReq.addActionListener(ev ->
            sel.get().ifPresent(e -> {
                if (e.request() != null) {
                    String s = new String(e.request().toByteArray().getBytes());
                    copy(s);
                }
            })
        );
        miCopyResp.addActionListener(ev ->
            sel.get().ifPresent(e -> {
                if (e.response() != null) {
                    String s = new String(e.response().toByteArray().getBytes());
                    copy(s);
                }
            })
        );

        table.addMouseListener(new MouseAdapter() {
            private void maybe(MouseEvent e){
                if (e.isPopupTrigger()){
                    int r = table.rowAtPoint(e.getPoint());
                    if (r >= 0 && !table.isRowSelected(r)) table.setRowSelectionInterval(r, r);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            @Override public void mousePressed(MouseEvent e){ maybe(e); }
            @Override public void mouseReleased(MouseEvent e){ maybe(e); }
        });
    }

    private void copy(String s){
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new java.awt.datatransfer.StringSelection(s), null);
    }
}
