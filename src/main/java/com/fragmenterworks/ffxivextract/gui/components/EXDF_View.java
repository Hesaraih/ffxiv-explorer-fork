package com.fragmenterworks.ffxivextract.gui.components;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.helpers.FFXIV_String;
import com.fragmenterworks.ffxivextract.helpers.SparseArray;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.models.EXDF_File;
import com.fragmenterworks.ffxivextract.models.EXDF_File.EXDF_Entry;
import com.fragmenterworks.ffxivextract.models.EXHF_File;
import com.fragmenterworks.ffxivextract.models.EXHF_File.EXDF_Dataset;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class EXDF_View extends JScrollPane implements ItemListener {

    //EXH Context
	private SqPack_IndexFile currentIndex;
    private EXHF_File exhFile = null;
    private EXDF_File[] exdFile = null;

    private String exhFolder;
    private String exhName;

    //To speed things up
    private int numPages = -1;
    private int numLanguages = -1;

    //UI
    private final JLabel lblExhName;
    private final JLabel lblExhNumEntries;
    private final JLabel lblExhNumPages;
    private final JLabel lblExhNumLanguages;
    private final JComboBox<String> cmbLanguage;
    private final JTable table;

    private int langOverride = -1;
    private boolean showAsHex = false;
    private boolean sortByOffset = false;

    private final SparseArray<String> columnNames = new SparseArray<>();

    //exdファイルが与えられたら、exh名を見つけて、それを探します。
    public EXDF_View(SqPack_IndexFile currentIndex, String fullPath, boolean showAsHex, boolean sortByOffset) {

        this();

        this.currentIndex = currentIndex;
        this.showAsHex = showAsHex;

        //TODO 多分これをその場で交換することを実装します
        this.sortByOffset = sortByOffset;

        fullPath = fullPath.toLowerCase();

        String exhName;

        //名前がわからない場合は、除外
        if (!fullPath.contains(".exd")) {
            setupUI_noExhFile();
            return;
        }

        //exdファイルからexhファイルのパスを生成
        exhName = fullPath;
        exhName = exhName.replace("_en.exd", "");
        exhName = exhName.replace("_ja.exd", "");
        exhName = exhName.replace("_de.exd", "");
        exhName = exhName.replace("_fr.exd", "");
        exhName = exhName.replace("_cht.exd", "");
        exhName = exhName.replace("_chs.exd", "");
        exhName = exhName.replace("_ko.exd", "");
        exhName = exhName.substring(0, exhName.lastIndexOf("_"));
        String folderName = exhName.substring(0, fullPath.lastIndexOf("/"));
        exhName = exhName.substring(fullPath.lastIndexOf("/") + 1) + ".exh";

        exhFolder = folderName;

        //検索
        try {
            byte[] data = currentIndex.extractFile(folderName, exhName);

            if (data != null) {
                exhFile = new EXHF_File(data);
            }
        } catch (IOException e) {
            Utils.getGlobalLogger().error(e);
        }

        //exhファイルが見つからない
        if (exhFile == null) {
            setupUI_noExhFile();
            return;
        }

        this.exhName = exhName;

        //numLanguagesとnumPagesを初期化します
        numPages = exhFile.getNumPages();
        numLanguages = exhFile.getNumLanguages();

        //exhファイルからexdファイルのパスを生成
        String parsedExdName;
        parsedExdName = exhName.replace(".exh", "");
        parsedExdName += "_%s%s.exd"; // name_0_en.exd

        getEXDFiles(exhFile, parsedExdName, numPages, numLanguages);

        setupUI();
    }

    public EXDF_View(SqPack_IndexFile currentIndex, String fullPath, EXHF_File file) {
        this(currentIndex, fullPath, file, false, false);
    }

    //exhファイルが与えられたら、exdファイル名を見つけて、それを探します。
    public EXDF_View(SqPack_IndexFile currentIndex, String fullPath, EXHF_File file, boolean showAsHex, boolean sortByOffset) {

        this();

        //fullPath = fullPath.toLowerCase();

        this.currentIndex = currentIndex;
        this.exhFile = file;
        this.showAsHex = showAsHex;
        this.sortByOffset = sortByOffset;

        //名前がわからない場合無視
        if (!fullPath.contains(".exh")) {
            setupUI_noExhFile();
            return;
        }

        //numLanguagesとnumPagesを初期化
        numPages = exhFile.getNumPages();
        numLanguages = exhFile.getNumLanguages();

        this.exhName = fullPath.substring(fullPath.lastIndexOf("/") + 1);
        exhFolder = fullPath.substring(0, fullPath.lastIndexOf("/"));

        //exhファイルからexdファイルのパスを生成
        String exdName;
        exdName = fullPath.replace(".exh", "");
        exdName += "_%s%s.exd"; // name_0_en.exd

        getEXDFiles(exhFile, exdName, numPages, numLanguages);

        setupUI();

        ///if (fullPath.contains("item"))
        //addAllWeaponModels();
    }

    private EXDF_View() {

        JPanel panel = new JPanel();
        setViewportView(panel);
        panel.setLayout(new BorderLayout(0, 0));

        JPanel panel_1 = new JPanel();
        panel_1.setBorder(new CompoundBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "EXHヘッダー", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)), new EmptyBorder(5, 10, 5, 10)));
        panel.add(panel_1, BorderLayout.NORTH);
        panel_1.setLayout(new BorderLayout(0, 0));

        JPanel panel_7 = new JPanel();
        panel_1.add(panel_7, BorderLayout.WEST);
        panel_7.setLayout(new BoxLayout(panel_7, BoxLayout.Y_AXIS));

        JPanel panel_3 = new JPanel();
        panel_7.add(panel_3);
        panel_3.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.X_AXIS));

        JLabel lblNewLabel = new JLabel("EXH名: ");
        panel_3.add(lblNewLabel);

        lblExhName = new JLabel("32");
        lblExhName.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel_3.add(lblExhName);

        JPanel panel_4 = new JPanel();
        panel_7.add(panel_4);
        panel_4.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel_4.setLayout(new BoxLayout(panel_4, BoxLayout.X_AXIS));

        JLabel lblNewLabel_1 = new JLabel("エントリー番号: ");
        panel_4.add(lblNewLabel_1);

        lblExhNumEntries = new JLabel("32");
        panel_4.add(lblExhNumEntries);

        JPanel panel_5 = new JPanel();
        panel_7.add(panel_5);
        panel_5.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel_5.setLayout(new BoxLayout(panel_5, BoxLayout.X_AXIS));

        JLabel lblNewLabel_2 = new JLabel("ページ番号: ");
        panel_5.add(lblNewLabel_2);

        lblExhNumPages = new JLabel("32");
        panel_5.add(lblExhNumPages);

        JPanel panel_6 = new JPanel();
        panel_7.add(panel_6);
        panel_6.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel_6.setLayout(new BoxLayout(panel_6, BoxLayout.X_AXIS));

        JLabel lblNewLabel_3 = new JLabel("言語コード: ");
        panel_6.add(lblNewLabel_3);

        lblExhNumLanguages = new JLabel("32");
        panel_6.add(lblExhNumLanguages);

        JPanel panel_8 = new JPanel();
        panel_8.setBorder(null);
        panel_1.add(panel_8, BorderLayout.EAST);
        panel_8.setLayout(new BoxLayout(panel_8, BoxLayout.Y_AXIS));

        JPanel panel_9 = new JPanel();
        panel_9.setAlignmentY(0.0f);
        panel_9.setBorder(null);
        panel_8.add(panel_9);
        panel_9.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

        JLabel lblNewLabel_4 = new JLabel("言語: ");
        lblNewLabel_4.setVerticalAlignment(SwingConstants.TOP);
        panel_9.add(lblNewLabel_4);

        cmbLanguage = new JComboBox<>();
        cmbLanguage.setPreferredSize(new Dimension(100, 20));
        cmbLanguage.setModel(new DefaultComboBoxModel<>(new String[]{"N/A"}));
        cmbLanguage.setSelectedIndex(0);
        panel_9.add(cmbLanguage);

        JPanel panel_2 = new JPanel();
        panel_2.setBorder(new TitledBorder(null, "EXDコンテンツ", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.add(panel_2);
        panel_2.setLayout(new BorderLayout(0, 0));

        JScrollPane scrollPane = new JScrollPane();
        panel_2.add(scrollPane, BorderLayout.CENTER);

        table = new JTable(){
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                int rendererWidth = component.getPreferredSize().width;

                TableColumn tableColumn = getColumnModel().getColumn(column);
                Object value = tableColumn.getHeaderValue();
                TableCellRenderer renderer2 = tableColumn.getHeaderRenderer();

                if (renderer2 == null) {
                    renderer = table.getTableHeader().getDefaultRenderer();
                }

                Component c = renderer.getTableCellRendererComponent(table, value, false, false, -1, column);
                int headerWidth = c.getPreferredSize().width;

                tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, Math.max(headerWidth, tableColumn.getPreferredWidth())));
                return component;
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        scrollPane.setViewportView(table);
    }

    /**
     * exdファイル名を指定して、関連するすべてのexdファイルを(ページ/言語)検索します
     * @param exhFile exhファイルクラス
     * @param exdName exdファイル名
     * @param numPages ページ
     * @param numLanguages 言語
     */
    private void getEXDFiles(EXHF_File exhFile, String exdName, int numPages, int numLanguages) {
        exdFile = new EXDF_File[exhFile.getNumPages() * exhFile.getNumLanguages()];
        for (int i = 0; i < numPages; i++) {

            for (int j = 0; j < numLanguages; j++) {

                if (EXHF_File.languageCodes[exhFile.getLanguageTable()[j]].equals("Unknown")) {
                    continue;
                }

                String formattedExdName;
                formattedExdName = String.format(exdName, exhFile.getPageTable()[i].pageNum, EXHF_File.languageCodes[exhFile.getLanguageTable()[j]]);
                formattedExdName = formattedExdName.substring(formattedExdName.lastIndexOf("/") + 1);

                try {
                    //誤って何かを見つけました
                    if (HashDatabase.getFileName(HashDatabase.computeCRC(formattedExdName.getBytes(), 0, formattedExdName.getBytes().length)) == null) {
                        if (!(numLanguages > 5 && (formattedExdName.contains("chs") || formattedExdName.contains("cht") || formattedExdName.contains("ko")))) {
                            HashDatabase.addPathToDB(exhFolder + "/" + formattedExdName, currentIndex.getName());
                        }
                    }
                    byte[] data = currentIndex.extractFile(exhFolder, formattedExdName);

                    if (exdFile != null && data != null) {
                        exdFile[(i * numLanguages) + j] = new EXDF_File(data);
                    }
                } catch (IOException e) {
                    Utils.getGlobalLogger().error(e);
                }
            }
        }

    }

    //既知のデータを使用してUIをセットアップする
    private void setupUI() {

        loadColumnNames(exhName);
        table.setModel(new EXDTableModel(exhFile, exdFile));

        lblExhName.setText(exhName);
        lblExhNumEntries.setText("" + exhFile.getNumEntries() + ((exhFile.getNumEntries() == exhFile.getTrueNumEntries() ? "" : " (Page Sum: " + exhFile.getTrueNumEntries() + ")")));
        lblExhNumLanguages.setText("" + (exhFile.getLanguageTable()[0] == 0x0 ? 0 : exhFile.getNumLanguages()));
        lblExhNumPages.setText("" + exhFile.getNumPages());

        cmbLanguage.removeAllItems();
        if (exhFile.getNumLanguages() != 0 && exhFile.getLanguageTable()[0] != 0x0) {
            for (int i = 0; i < numLanguages; i++) {
                cmbLanguage.addItem(EXHF_File.languageNames[exhFile.getLanguageTable()[i]]);
            }
            cmbLanguage.addItemListener(this);
            if (Constants.defaultLanguage > exhFile.getNumLanguages() - 1) {
                cmbLanguage.setSelectedIndex(0);
            } else {
                cmbLanguage.setSelectedIndex(Constants.defaultLanguage);
            }
        } else {
            cmbLanguage.setModel(new DefaultComboBoxModel<>(new String[]{"N/A"}));
            cmbLanguage.setEnabled(false);
            if (Constants.defaultLanguage > exhFile.getNumLanguages() - 1) {
                cmbLanguage.setSelectedIndex(0);
            } else {
                cmbLanguage.setSelectedIndex(Constants.defaultLanguage);
            }
        }

        if (this.sortByOffset)
        {
            java.util.List<TableColumn> tempColumns = new ArrayList<>();

            // keep index column, love that column
            for (int i = 1; i < table.getColumnModel().getColumnCount(); i++) {
                tempColumns.add(table.getColumnModel().getColumn(i));
            }

            int count = table.getColumnModel().getColumnCount();
            for (int i = count - 1; i >= 1; i--) {
                table.getColumnModel().removeColumn(table.getColumnModel().getColumn(i));
            }

            tempColumns.sort((o1, o2) -> {
                String headerOne = (String) o1.getHeaderValue();
                String headerTwo = (String) o2.getHeaderValue();

                String offsetTextOne = headerOne.substring(headerOne.lastIndexOf("[") + 3, headerOne.lastIndexOf("]"));
                String offsetTextTwo = headerTwo.substring(headerTwo.lastIndexOf("[") + 3, headerTwo.lastIndexOf("]"));

                int offsetOne = Integer.parseInt(offsetTextOne, 16);
                int offsetTwo = Integer.parseInt(offsetTextTwo, 16);

                return Integer.compare(offsetOne, offsetTwo);
            });

            for (TableColumn c : tempColumns) {
                table.getColumnModel().addColumn(c);
            }
        }
    }

    //EXHファイルが見つからないときのUI
    private void setupUI_noExhFile() {
        lblExhName.setText("EXHファイルが見つかりません");
        lblExhName.setForeground(Color.RED);
        lblExhNumEntries.setText("N/A");
        lblExhNumLanguages.setText("N/A");
        lblExhNumPages.setText("N/A");
        cmbLanguage.setModel(new DefaultComboBoxModel<>(new String[]{"N/A"}));
    }

    class EXDTableModel extends AbstractTableModel {

        final EXHF_File exhFile;
        final EXDF_File[] exdFiles;

        EXDTableModel(EXHF_File exh, EXDF_File[] exd) {
            this.exhFile = exh;
            this.exdFiles = exd;
        }

        @Override
        public int getColumnCount() {
            return exhFile.getDatasetTable().length + 1;
        }

        @Override
        public int getRowCount() {
            return Math.min(exhFile.getTrueNumEntries(), exhFile.getNumEntries());
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return "Index";
            } else {
                String columnType = "[" + resolveTypeToString(exhFile.getDatasetTable()[column - 1].type) + "]";
                String offset = String.format("[0x%s]", String.format("%x", exhFile.getDatasetTable()[column - 1].offset).toUpperCase());
                String mainTitle = columnNames.get(column - 1, columnType);

                return String.format("%d %s %s", column - 1, mainTitle, offset);
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            try {
                int page = 0;

//				rowIndex += exhFile.getPageTable()[0].pageNum;

                //この言語のデータを取得したかどうかを確認
                if (langOverride != -1) {
                    if (exdFile[langOverride] == null) {
                        return "";
                    }
                } else {
                    if (exdFile[cmbLanguage.getSelectedIndex()] == null) {
                        return "";
                    }
                }

                //Find Page
                int totalRealEntries = 0;
                if (numPages != 1) {
                    for (int i = 0; i <= exhFile.getPageTable().length; i++) {
                        if (i == exhFile.getPageTable().length) {
                            if (i <= exhFile.getPageTable()[i - 1].pageNum + exhFile.getPageTable()[i - 1].numEntries) {
                                page = i - 1;
                                break;
                            } else {
                                return "ERROR";
                            }
                        }

                        totalRealEntries += exhFile.getPageTable()[i].numEntries;
                        if (totalRealEntries > rowIndex) {
                            page = i;
                            //noinspection UnusedAssignment
                            totalRealEntries -= exhFile.getPageTable()[i].numEntries;
                            break;
                        }
                    }

                }

                //Grab Data
                totalRealEntries = 0;
                for (int i = 0; i < page; i++) {
                    if (langOverride != -1) {
                        totalRealEntries += exdFiles[(numLanguages * i) + langOverride].getNumEntries();
                    } else {
                        totalRealEntries += exdFiles[(numLanguages * i) + cmbLanguage.getSelectedIndex()].getNumEntries();
                    }
                }

                EXDF_Entry entry;
                if (langOverride != -1) {
                    entry = exdFiles[(numLanguages * page) + langOverride].getEntry(rowIndex - totalRealEntries);
                } else {
                    entry = exdFiles[(numLanguages * page) + cmbLanguage.getSelectedIndex()].getEntry(rowIndex - totalRealEntries);
                }

                //Index
                if (columnIndex == 0) {
                    return entry.getIndex();
                }

                //Data
                EXDF_Dataset dataset = exhFile.getDatasetTable()[columnIndex - 1];

                //Special case for byte bools
                if (dataset.type >= 0x19) {
                    return entry.getByteBool(dataset.type, dataset.offset);
                } else {
                    switch (dataset.type) {
                        case 0x0b: // QUAD
                            int[] quad = entry.getQuad(dataset.offset);
                            return quad[3] + ", " + quad[2] + ", " + quad[1] + ", " + quad[0];
                        case 0x09: // FLOAT
                            //case 0x08:
                            return entry.getFloat(dataset.offset);
                        case 0x07: // UINT
                            if (showAsHex) {
                                return String.format("%02X ", (long) entry.getInt(dataset.offset));
                            }
                            return (long) entry.getInt(dataset.offset);
                        case 0x06: // INT
                            if (showAsHex) {
                                return String.format("%02X ", entry.getInt(dataset.offset));
                            }
                            return entry.getInt(dataset.offset);
                        case 0x05: // USHORT
                            if (showAsHex) {
                                return String.format("%02X ", (int) entry.getShort(dataset.offset) & 0xFFFF);
                            }
                            return ((int) entry.getShort(dataset.offset) & 0xFFFF);
                        case 0x04: // SHORT
                            if (showAsHex) {
                                return String.format("%02X ", entry.getShort(dataset.offset));
                            }
                            return entry.getShort(dataset.offset);
                        case 0x03: // UBYTE
                            if (showAsHex) {
                                return String.format("%02X ", (((int) entry.getByte(dataset.offset)) & 0xFF));
                            }
                            return (((int) entry.getByte(dataset.offset)) & 0xFF);
                        case 0x02: // BYTE
                            if (showAsHex) {
                                return String.format("%02X ", (entry.getByte(dataset.offset)));
                            }
                            return entry.getByte(dataset.offset);
                        case 0x01: // BOOL
                            return entry.getBoolean(dataset.offset);
                        case 0x00: // STRING; Points to offset from end of dataset part. Read until 0x0.
                            //return new String(entry.getString(exhFile.getDatasetChunkSize(), dataset.offset));
                            return FFXIV_String.parseFFXIVString(entry.getString(exhFile.getDatasetChunkSize(), dataset.offset));
                        default:
                            return "?";// Value: " + ((int)entry.getByte(dataset.offset)&0xFF);
                    }
                }
            } catch (Exception e) {
                Utils.getGlobalLogger().error(e);
                return "";
            }
        }

    }

    @Override
    public void itemStateChanged(ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
            ((EXDTableModel) table.getModel()).fireTableDataChanged();
        }
    }

    public boolean isSame(String name) {
        if (exhName == null || name == null) {
            return false;
        }
        if (name.contains(".exh")) {
            return exhName.equals(name);
        }
        if (!name.contains(".exd")) {
            return false;
        }
        String checkString = name;
        checkString = checkString.replace("_en.exd", "");
        checkString = checkString.replace("_ja.exd", "");
        checkString = checkString.replace("_de.exd", "");
        checkString = checkString.replace("_fr.exd", "");
        checkString = checkString.replace("_cht.exd", "");
        checkString = checkString.replace("_ko.exd", "");
        checkString = checkString.substring(0, checkString.lastIndexOf("_")) + ".exh";
        return exhName.equals(checkString);
    }

    private String resolveTypeToString(int type) {
        //バイトブールの特殊なケース
        if (type >= 0x19) {
            return "BBOOL";
        }

        switch (type) {
            case 0x0b: // QUAD
                return "QUAD";
            case 0x09: // FLOAT
                //case 0x08:
                return "FLOAT";
            case 0x07: // UINT
                return "UINT";
            case 0x06: // INT
                return "INT";
            case 0x05: // USHORT
                return "USHORT";
            case 0x04: // SHORT
                return "SHORT";
            case 0x03: // UBYTE
                return "UBYTE";
            case 0x02: // BYTE
                return "BYTE";
            case 0x01: // BOOL
                return "BOOL";
            case 0x00: // STRING; データセット部の終わりからオフセットするポイント。 0x0まで読む
                //return new String(entry.getString(exhFile.getDatasetChunkSize(), dataset.offset));
                return "STRING";
            default:
                return "UNK";
        }
    }

    @SuppressWarnings("unused")
    public void addAllWeaponModels() {
        //データ書き込み
        for (int row = 0; row < table.getRowCount(); row++) {

            byte slot = (Byte) table.getValueAt(row, 48);
            String[] model1String = ((String) table.getValueAt(row, 11)).split(",");
            int[] model1 = new int[model1String.length];
            for (int i = 0; i < model1.length; i++) {
                model1[i] = Integer.parseInt(model1String[i].trim());
            }
            String[] model2 = ((String) table.getValueAt(row, 12)).split(",");

            String path = null, path2 = null;
            int[] chara_id = new int[] { 101, 103, 104, 201, 204, 301, 401, 501, 504, 601, 604, 701, 801, 804, 901, 1001, 1101, 1201, 1301, 1304, 1401, 1404, 1501, 1801, 9104, 9204 };

            switch (slot) {
                case 13: //Weapon：副
                case 2: //Weapon：主
                    HashDatabase.addPathToDB(String.format("chara/weapon/w%04d/obj/body/b%04d/model/w%04db%04d.mdl", model1[0], model1[1], model1[0], model1[1]), "040000");
                    break;
                case 3: //Equipment:頭
                    for (int humanID : chara_id){
                        HashDatabase.addPathToDB(String.format("chara/equipment/e%04d/model/c%04de%04d_%s.mdl", model1[0], humanID, model1[0], "met"), "040000");
                    }
                    break;
                case 5: //Equipment:手
                    for (int humanID : chara_id){
                        HashDatabase.addPathToDB(String.format("chara/equipment/e%04d/model/c%04de%04d_%s.mdl", model1[0], humanID, model1[0], "glv"), "040000");
                    }
                    break;
                case 6: //Equipment:腰
                    break;
                case 7: //Equipment:脚
                    for (int humanID : chara_id){
                        HashDatabase.addPathToDB(String.format("chara/equipment/e%04d/model/c%04de%04d_%s.mdl", model1[0], humanID, model1[0], "dwn"), "040000");
                    }
                    break;
                case 8: //Equipment:足
                    for (int humanID : chara_id){
                        HashDatabase.addPathToDB(String.format("chara/equipment/e%04d/model/c%04de%04d_%s.mdl", model1[0], humanID, model1[0], "sho"), "040000");
                    }
                    break;
                case 9: //Accessory：耳
                    for (int humanID : chara_id){
                        HashDatabase.addPathToDB(String.format("chara/equipment/a%04d/model/c%04da%04d_%s.mdl", model1[0], humanID, model1[0], "ear"), "040000");
                    }
                    break;
                case 10: //Accessory：首
                    for (int humanID : chara_id){
                        HashDatabase.addPathToDB(String.format("chara/equipment/a%04d/model/c%04da%04d_%s.mdl", model1[0], humanID, model1[0], "nek"), "040000");
                    }
                    break;
                case 11: //Accessory：腕
                    for (int humanID : chara_id) {
                        HashDatabase.addPathToDB(String.format("chara/equipment/a%04d/model/c%04da%04d_%s.mdl", model1[0], humanID, model1[0], "wrs"), "040000");
                    }
                    break;
                case 12: //Accessory：指輪
                    for (int humanID : chara_id) {
                        HashDatabase.addPathToDB(String.format("chara/accessory/a%04d/model/c%04da%04d_%s.mdl", model1[0], humanID, model1[0], "rir"), "040000"); //右
                        HashDatabase.addPathToDB(String.format("chara/accessory/a%04d/model/c%04da%04d_%s.mdl", model1[0], humanID, model1[0], "ril"), "040000"); //左
                    }
                    break;
                default:
                    continue;
            }

            //noinspection ConstantConditions
            if (path != null) {
                HashDatabase.addPathToDB(path, "040000");
                if (path2 != null) {
                    HashDatabase.addPathToDB(path2, "040000");
                }
            }
        }
    }

    public void saveCSV(String path, int lang) throws IOException {
        langOverride = lang;

        //言語が存在しない場合はスキップ
        if (langOverride != -1) {
            if (exdFile[langOverride] == null) {
                return;
            }
        } else {
            if (exdFile[cmbLanguage.getSelectedIndex()] == null) {
                return;
            }
        }

        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8);

        //Write columns
        for (int col = 0; col < table.getColumnCount(); col++) {
            out.write(table.getColumnName(col));
            if (col != table.getColumnCount() - 1) {
                out.write(",");
            }
        }

        out.write("\r\n");

        //Write data
        for (int row = 0; row < table.getRowCount(); row++) {
            for (int col = 0; col < table.getColumnCount(); col++) {
                Object value = table.getValueAt(row, col);
                if (value instanceof String) {
                    String string = (String) value;
                    string = string.replace("\"", "\"\"");
                    out.write("\"" + string + "\"");
                } else {
                    out.write("" + value);
                }
                if (col != table.getColumnCount() - 1) {
                    out.write(",");
                }
            }
            out.write("\r\n");
        }
        out.close();
        langOverride = -1;
    }

    public void setLangOverride(int override) {
        langOverride = override;
    }

    public int getNumLanguages() {
        return exhFile.getNumLanguages();
    }

    public JTable getTable() {
        return table;
    }

    private void loadColumnNames(String exhName) {

        String path = Constants.EXH_NAMES_PATH + exhName.replace("exh", "lst");
        if (!Files.exists(Paths.get(path))) {
            return;
        }
        Utils.getGlobalLogger().info("Loading column names from {}", path);

        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            for (String line; (line = br.readLine()) != null; ) {
                //Skip comments and whitespace
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                if (line.contains(":")) {
                    String[] split = line.split(":", 2);
                    if (split.length != 2) {
                        continue;
                    }

                    if (split[1].isEmpty()) {
                        continue;
                    }
                    columnNames.put(Integer.parseInt(split[0]), split[1]);
                }
            }
            br.close();
        } catch (IOException e) {
            Utils.getGlobalLogger().error(e);
        }
    }

    public EXHF_File getExhFile() {
        return exhFile;
    }

}