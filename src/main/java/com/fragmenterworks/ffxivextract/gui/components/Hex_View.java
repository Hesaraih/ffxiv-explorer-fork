package com.fragmenterworks.ffxivextract.gui.components;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public class Hex_View extends JScrollPane {

    private final JTable txtHexData;
    private final int columnCount;
    private byte[] bytes = null;
    private final String[] byteToStr = new String[256];
    private final String[] byteToChar = new String[256];

    public Hex_View(int columnCount) {
        //1行あたりの列数
        this.columnCount = columnCount;

        txtHexData = new JTable(new HexTableModel());

        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL
                | GridBagConstraints.VERTICAL;
        getViewport().add(txtHexData);

        for (int i = 0; i < 256; i++) {
            byteToStr[i] = String.format("%02X", i);

            byteToChar[i] = "" + (char) i;
        }

        //見出し列の幅
        txtHexData.getColumnModel().getColumn(0).setMinWidth(70);

        DefaultTableCellRenderer cellRender = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value, boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JTableHeader header = table.getTableHeader();
                if (header != null) {
                    setForeground(Color.BLACK);
                    setBackground(Color.BLACK);
                    setFont(header.getFont());
                }

                if (column == 0) {
                    setHorizontalAlignment(JLabel.RIGHT);
                } else {
                    setHorizontalAlignment(JLabel.CENTER);
                }

                if (value == null) {
                    setText("");
                } else {
                    setText(value.toString());
                }

                if (column == 0) {
                    setBorder(BorderFactory.createMatteBorder(0, 0, 1, 2,
                            Color.LIGHT_GRAY));
                } else if (column == Hex_View.this.columnCount) {
                    if (value == null) {
                        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0,
                                Color.LIGHT_GRAY));
                    } else {
                        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 2,
                                Color.LIGHT_GRAY));
                    }
                } else {
                    if (value == null) {
                        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0,
                                Color.LIGHT_GRAY));
                    } else {
                        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1,
                                Color.LIGHT_GRAY));
                    }
                }

                return this;
            }
        };

        txtHexData.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        txtHexData.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        getViewport().add(txtHexData);

        for (int column = 0; column < txtHexData.getColumnCount(); column++) {
            TableColumn tableColumn = txtHexData.getColumnModel().getColumn(column);
            int preferredWidth = tableColumn.getMinWidth();

            for (int row = 0; row < txtHexData.getRowCount(); row++) {
                TableCellRenderer cellRenderer = txtHexData.getCellRenderer(row, column);
                Component c = txtHexData.prepareRenderer(cellRenderer, row, column);
                int width = c.getPreferredSize().width + txtHexData.getIntercellSpacing().width;
                preferredWidth = Math.max(preferredWidth, width);
            }
            if (column == 0) {
                tableColumn.setPreferredWidth(70); //アドレス表示列の幅(指定しないと15)
            }else if (column <= 16){
                tableColumn.setPreferredWidth(17); //byte値表示列の幅(指定しないと15)
            }else{
                tableColumn.setPreferredWidth(12);  //キャラクタ表示列の幅(指定しないと15)
            }
        }

        txtHexData.setTableHeader(null);
        txtHexData.setShowGrid(false);
        txtHexData.setIntercellSpacing(new Dimension(0, 0));
        txtHexData.setDefaultRenderer(Object.class, cellRender);

    }

    public void setBytes(byte[] byteArray) {

        getVerticalScrollBar().setValue(0);

        if (byteArray == null) {
            bytes = new byte[1];
            return;
        }

        bytes = byteArray;
        ((AbstractTableModel) txtHexData.getModel()).fireTableDataChanged();

    }

    class HexTableModel extends AbstractTableModel {

        @Override
        public int getColumnCount() {
            return columnCount + 1 + columnCount; // Address Column + 16 Bytes
            // of Hex + 16 Bytes of
            // Chars
        }

        @Override
        public int getRowCount() {
            if (bytes == null || bytes.length == 0) {
                return 0;
            } else {
                if (bytes.length % columnCount == 0) {
                    return (bytes.length / columnCount);
                } else {
                    return (bytes.length / columnCount) + 1;
                }
            }

        }

        @Override
        public String getColumnName(int column) {
            if (column == 0){
                return "アドレス";
            }else{
                return String.format("%X: ", (column - 1) % 16);
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                //見出し列
                return String.format("%08X: ", columnCount * rowIndex);
            } else if (columnIndex >= 1 && columnIndex <= columnCount) {
                if (((rowIndex * columnCount) + columnIndex - 1) > bytes.length - 1) {
                    return null;
                }

                int value = bytes[(rowIndex * columnCount) + columnIndex - 1];
                return byteToStr[value & 0xFF];
            } else {
                final int allColumns = (rowIndex * columnCount) + columnIndex - columnCount - 1;
                if (allColumns > bytes.length - 1) {
                    return null;
                }

                int value = bytes[allColumns];
                return byteToChar[value & 0xFF];
            }
        }

    }

}
