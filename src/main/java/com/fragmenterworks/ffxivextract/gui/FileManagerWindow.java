package com.fragmenterworks.ffxivextract.gui;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.Strings;
import com.fragmenterworks.ffxivextract.gui.SearchWindow.ISearchComplete;
import com.fragmenterworks.ffxivextract.gui.components.*;
import com.fragmenterworks.ffxivextract.gui.modelviewer.ModelViewerWindow;
import com.fragmenterworks.ffxivextract.gui.outfitter.OutfitterWindow;
import com.fragmenterworks.ffxivextract.helpers.*;
import com.fragmenterworks.ffxivextract.models.*;
import com.fragmenterworks.ffxivextract.models.SCD_File.SCD_Sound_Info;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile.SqPack_File;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;
import unluac.decompile.Decompiler;
import unluac.decompile.OutputProvider;
import unluac.parse.BHeader;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Objects;
import java.util.prefs.Preferences;

public class FileManagerWindow extends JFrame implements TreeSelectionListener, ISearchComplete, WindowListener {

    private final JMenuBar menu = new JMenuBar();

    //FILE IO
    private File lastOpenedIndexFile = null;
    private File lastOpenedTextFile = null;
    private File lastSaveLocation = null;
    private File lastSaveLocation_2 = null;
    private SqPack_IndexFile currentIndexFile;

    //UI
    private SearchWindow searchWindow;
    private final ExplorerPanel_View fileTree = new ExplorerPanel_View();
    private final JSplitPane splitPane;
    private final JLabel lblOffsetValue;
    private final JLabel lblHashValue;
    private final JLabel lblContentTypeValue;
    private final JLabel lblHashInfoValue;
    private final Hex_View hexView = new Hex_View(16);
    private EXDF_View exhfComponent;
    private final JProgressBar prgLoadingBar;
    private final JLabel lblLoadingBarString;
    private TexturePaint paint;
    private final JScrollPane defaultScrollPane;

    //MENU
    private JMenuItem file_Extract;
    private JMenuItem file_ExtractRaw;
    private JMenuItem file_Close;
    private JMenuItem search_search;
    private JMenuItem search_searchAgain;
    private JCheckBoxMenuItem options_enableUpdate;
    private JCheckBoxMenuItem options_showAsHex;
    private JCheckBoxMenuItem options_sortByOffset;

    public FileManagerWindow(String title) {
        addWindowListener(this);

        //Load generic bg img
        BufferedImage bg;
        try {
            bg = ImageIO.read(Objects.requireNonNull(getClass().getResource("/triangular.png")));
            paint = new TexturePaint(bg, new Rectangle(120, 120));
        } catch (IOException e) {
            Utils.getGlobalLogger().error(e);
        }

        setupMenu();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1200, 800);
        this.setTitle(title);

        URL imageURL = getClass().getResource("/frameicon.png");
        ImageIcon image = new ImageIcon(Objects.requireNonNull(imageURL));
        this.setIconImage(image.getImage());

        JPanel pnlContent = new JPanel();

        pnlContent.registerKeyboardAction(menuHandler, "search", KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        pnlContent.registerKeyboardAction(menuHandler, "extractc", KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        pnlContent.registerKeyboardAction(menuHandler, "extractr", KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        pnlContent.registerKeyboardAction(menuHandler, "searchagain", KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        getContentPane().add(pnlContent, BorderLayout.CENTER);
        pnlContent.setLayout(new BoxLayout(pnlContent, BoxLayout.X_AXIS));

        defaultScrollPane = new JScrollPane();
        JViewport defaultViewPort = new JViewport() {
            @Override
            public boolean isOpaque() {
                return false;
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setPaint(paint);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        defaultScrollPane.setViewport(defaultViewPort);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                fileTree, defaultScrollPane);
        pnlContent.add(splitPane);

        splitPane.setDividerLocation(250);

        fileTree.addTreeSelectionListener(this);
        //ファイルツリーパネルの最小値を設定し、大きい画像でもツリーが隠れないように設定
        fileTree.setMinimumSize(new Dimension(250,50));

        JPanel pnlStatusBar = new JPanel();
        getContentPane().add(pnlStatusBar, BorderLayout.SOUTH);
        pnlStatusBar.setLayout(new BorderLayout(0, 0));

        JSeparator separator = new JSeparator();
        pnlStatusBar.add(separator, BorderLayout.NORTH);

        JPanel pnlInfo = new JPanel();
        FlowLayout fl_pnlInfo = (FlowLayout) pnlInfo.getLayout();
        fl_pnlInfo.setVgap(4);
        pnlInfo.setBorder(null);
        pnlStatusBar.add(pnlInfo, BorderLayout.WEST);

        JLabel lblOffset = new JLabel("オフセット: ");
        pnlInfo.add(lblOffset);
        lblOffset.setHorizontalAlignment(SwingConstants.LEFT);

        lblOffsetValue = new JLabel("*");
        pnlInfo.add(lblOffsetValue);

        JSeparator separator_1 = new JSeparator();
        separator_1.setPreferredSize(new Dimension(1, 16));
        separator_1.setOrientation(SwingConstants.VERTICAL);
        pnlInfo.add(separator_1);

        JLabel lblHash = new JLabel("ハッシュ値: ");
        pnlInfo.add(lblHash);

        lblHashValue = new JLabel("*");
        pnlInfo.add(lblHashValue);

        JSeparator separator_2 = new JSeparator();
        separator_2.setPreferredSize(new Dimension(1, 16));
        separator_2.setOrientation(SwingConstants.VERTICAL);
        pnlInfo.add(separator_2);

        JLabel lblContentType = new JLabel("Content Type: ");
        pnlInfo.add(lblContentType);

        lblContentTypeValue = new JLabel("*");
        pnlInfo.add(lblContentTypeValue);

        JSeparator separator_3 = new JSeparator();
        separator_3.setPreferredSize(new Dimension(1, 16));
        separator_3.setOrientation(SwingConstants.VERTICAL);
        pnlInfo.add(separator_3);

        lblHashInfoValue = new JLabel("* / *");
        pnlInfo.add(lblHashInfoValue);

        JLabel lblHashInfo = new JLabel(" filenamesを読み込みました");
        pnlInfo.add(lblHashInfo);

        JPanel pnlProgBar = new JPanel();
        pnlStatusBar.add(pnlProgBar, BorderLayout.EAST);

        prgLoadingBar = new JProgressBar();
        prgLoadingBar.setVisible(false);

        lblLoadingBarString = new JLabel("0%");
        lblLoadingBarString.setHorizontalAlignment(SwingConstants.RIGHT);
        lblLoadingBarString.setVisible(false);
        pnlProgBar.add(lblLoadingBarString);
        pnlProgBar.add(prgLoadingBar);

        setLocationRelativeTo(null);

        Preferences prefs = Preferences.userNodeForPackage(com.fragmenterworks.ffxivextract.Main.class);

        if (prefs.get(Constants.PREF_LASTOPENED, null) != null) {
            lastOpenedIndexFile = new File(prefs.get(Constants.PREF_LASTOPENED, null));
        }

        HavokNative.initHavokNativ();
    }

    /**
     * Indexファイルを開く
     * @param selectedFile .indexファイル名
     */
    private void openFile(File selectedFile) {

        if (currentIndexFile != null) {
            if (splitPane.getRightComponent() instanceof JTabbedPane) {
                JTabbedPane tabs = (JTabbedPane) splitPane.getRightComponent();

                if (tabs.getTabCount() > 0) {
                    if (tabs.getComponentAt(0) instanceof Sound_View) {
                        ((Sound_View) tabs.getComponentAt(0)).stopPlayback();
                    }
                }
            }
            closeFile();
        }

        OpenIndexTask openTask = new OpenIndexTask(selectedFile);
        openTask.execute();
    }

    private void closeFile() {

        if (currentIndexFile == null) {
            return;
        }

        fileTree.fileClosed();
        currentIndexFile = null;

        setTitle(Constants.APPNAME);
        hexView.setBytes(null);
        splitPane.setRightComponent(defaultScrollPane);
        file_Close.setEnabled(false);
        search_search.setEnabled(false);
        search_searchAgain.setEnabled(false);

        lblOffsetValue.setText("*");
        lblHashValue.setText("*");
        lblContentTypeValue.setText("*");
    }

    private final ActionListener menuHandler = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (event.getActionCommand().equals("open")) {
                JFileChooser fileChooser = new JFileChooser(lastOpenedIndexFile);
                //FileNameExtensionFilter filter = new FileNameExtensionFilter(Strings.FILETYPE_FFXIV_INDEX, ".index");
                FileFilter filter = new FileFilter() {

                    @Override
                    public String getDescription() {
                        return Strings.FILETYPE_FFXIV_INDEX;
                    }

                    @Override
                    public boolean accept(File f) {
                        return f.getName().endsWith(".index") || f.isDirectory();
                    }
                };
                //FileNameExtensionFilter filter = new FileNameExtensionFilter(Strings.FILETYPE_FFXIV_INDEX2, ".index2");
                FileFilter filter2 = new FileFilter() {

                    @Override
                    public String getDescription() {
                        return Strings.FILETYPE_FFXIV_INDEX2;
                    }

                    @Override
                    public boolean accept(File f) {
                        return f.getName().endsWith(".index2") || f.isDirectory();
                    }
                };
                fileChooser.addChoosableFileFilter(filter);
                fileChooser.addChoosableFileFilter(filter2);

                fileChooser.setFileFilter(filter);
                fileChooser.setAcceptAllFileFilterUsed(false);
                int retunval = fileChooser.showOpenDialog(FileManagerWindow.this);
                if (retunval == JFileChooser.APPROVE_OPTION) {
                    lastOpenedIndexFile = fileChooser.getSelectedFile();
                    openFile(fileChooser.getSelectedFile());

                    Preferences prefs = Preferences.userNodeForPackage(com.fragmenterworks.ffxivextract.Main.class);
                    prefs.put(Constants.PREF_LASTOPENED, lastOpenedIndexFile.getAbsolutePath());
                }
            } else if (event.getActionCommand().equals("close")) {
                closeFile();
            } else if (event.getActionCommand().equals("extractc") && file_Extract.isEnabled()) {
                extract(true);
            } else if (event.getActionCommand().equals("extractr") && file_ExtractRaw.isEnabled()) {
                extract(false);
            } else if (event.getActionCommand().equals("search") && search_search.isEnabled()) {
                searchWindow.setLocationRelativeTo(FileManagerWindow.this);
                searchWindow.setVisible(true);
                searchWindow.reset();
            } else if (event.getActionCommand().equals("searchagain") && search_searchAgain.isEnabled()) {
                searchWindow.searchAgain();
            } else if (event.getActionCommand().equals("hashcalc")) { //パス -> ハッシュ値 計算
                Path_to_Hash_Window hasher = new Path_to_Hash_Window(currentIndexFile);
                hasher.setLocationRelativeTo(FileManagerWindow.this);
                hasher.setVisible(true);
            } else if (event.getActionCommand().equals("modelviewer")) {
                if (Constants.datPath == null || Constants.datPath.isEmpty() || !new File(Constants.datPath).exists()) {
                    JOptionPane.showMessageDialog(
                            FileManagerWindow.this,
                            "有効なFFXIVのパスを設定していません。 [オプション]メニューの[設定]で最初に設定してください。",
                            "FFXIVパス未設定",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                ModelViewerWindow modelviewer = new ModelViewerWindow(FileManagerWindow.this, Constants.datPath);
                //modelviewer.setLocationRelativeTo(FileManagerWindow.this);
                modelviewer.beginLoad();
            } else if (event.getActionCommand().equals("outfitter")) {
                if (Constants.datPath == null || Constants.datPath.isEmpty() || !new File(Constants.datPath).exists()) {
                    JOptionPane.showMessageDialog(
                            FileManagerWindow.this,
                            "有効なFFXIVのパスを設定していません。 [オプション]メニューの[設定]で最初に設定してください。",
                            "FFXIVパス未設定",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                OutfitterWindow outfitter = new OutfitterWindow(FileManagerWindow.this, Constants.datPath);
                //modelviewer.setLocationRelativeTo(FileManagerWindow.this);
                outfitter.beginLoad();
            } else if (event.getActionCommand().equals("musicswapper")) {
                MusicSwapperWindow swapper = new MusicSwapperWindow();
                swapper.setLocationRelativeTo(FileManagerWindow.this);
                swapper.setVisible(true);
            } else if (event.getActionCommand().equals("macroeditor")) {
                MacroEditorWindow macroEditor = new MacroEditorWindow();
                macroEditor.setLocationRelativeTo(FileManagerWindow.this);
                macroEditor.setVisible(true);
            } else if (event.getActionCommand().equals("logviewer")) {
                LogViewerWindow logViewer = new LogViewerWindow();
                logViewer.setLocationRelativeTo(FileManagerWindow.this);
                logViewer.setVisible(true);
            } else if (event.getActionCommand().equals("find_exh")) { //Exhハッシュ検索
                if (Constants.datPath == null || Constants.datPath.isEmpty() || !new File(Constants.datPath).exists()) {
                    JOptionPane.showMessageDialog(
                            FileManagerWindow.this,
                            "有効なFFXIVのパスを設定していません。 [オプション]メニューの[設定]で最初に設定してください。",
                            "FFXIVパス未設定",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                HashFinding_Utils.findExhHashes();

                JOptionPane.showMessageDialog(
                        FileManagerWindow.this,
                        "ハッシュの検索を終了しました。 現在開いているアーカイブがある場合は、アーカイブを再度開きます。",
                        "検索完了",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (event.getActionCommand().equals("find_music")) { //Musicハッシュ検索
                if (Constants.datPath == null || Constants.datPath.isEmpty() || !new File(Constants.datPath).exists()) {
                    JOptionPane.showMessageDialog(
                            FileManagerWindow.this,
                            "有効なFFXIVのパスを設定していません。 [オプション]メニューの[設定]で最初に設定してください。",
                            "FFXIVパス未設定",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                HashFinding_Utils.findMusicHashes();

                JOptionPane.showMessageDialog(
                        FileManagerWindow.this,
                        "ハッシュの検索を終了しました。 現在開いているアーカイブがある場合は、アーカイブを再度開きます。",
                        "検索完了",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (event.getActionCommand().equals("find_maps")) { //Mapハッシュ検索
                if (Constants.datPath == null || Constants.datPath.isEmpty() || !new File(Constants.datPath).exists()) {
                    JOptionPane.showMessageDialog(
                            FileManagerWindow.this,
                            "有効なFFXIVのパスを設定していません。 [オプション]メニューの[設定]で最初に設定してください。",
                            "FFXIVパス未設定",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                prgLoadingBar.setVisible(true);
                prgLoadingBar.setValue(0);
                lblLoadingBarString.setVisible(true);

                HashFinding_Utils.findMapHashes();

                JOptionPane.showMessageDialog(
                        FileManagerWindow.this,
                        "ハッシュの検索を終了しました。 現在開いているアーカイブがある場合は、アーカイブを再度開きます。",
                        "検索完了",
                        JOptionPane.INFORMATION_MESSAGE);

                prgLoadingBar.setValue(0);
                prgLoadingBar.setVisible(false);
                lblLoadingBarString.setVisible(false);
            } else if (event.getActionCommand().equals("tool_Test")) {
                HashFinding_Utils.TestPrg();
            } else if (event.getActionCommand().equals("settings")) {
                SettingsWindow settings = new SettingsWindow(FileManagerWindow.this);
                settings.setLocationRelativeTo(FileManagerWindow.this);
                settings.setVisible(true);
            } else if (event.getActionCommand().equals("options_update")) {
                Preferences prefs = Preferences.userNodeForPackage(com.fragmenterworks.ffxivextract.Main.class);
                prefs.putBoolean(Constants.PREF_DO_DB_UPDATE, options_enableUpdate.isSelected());
            } else if (event.getActionCommand().equals("quit")) {
                System.exit(0);
            } else if (event.getActionCommand().equals("about")) {
                AboutWindow aboutWindow = new AboutWindow(FileManagerWindow.this);
                aboutWindow.setLocationRelativeTo(FileManagerWindow.this);
                aboutWindow.setVisible(true);
            } else if (event.getActionCommand().equals("cedumpimport")) {
                //テキストファイルからインポート
                if (lastOpenedTextFile == null){
                    lastOpenedTextFile = new File("./");
                }
                JFileChooser fc = new JFileChooser(lastOpenedTextFile);
                int returnVal = fc.showOpenDialog(getParent()); //フレームが親コンポーネントである場合
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    int added  = HashDatabase.importFilePaths(fc.getSelectedFile());
                    if (added >= 0) {
                        Utils.getGlobalLogger().info("{}個の新しいパスをDBに追加した。", added);
                    } else {
                        Utils.getGlobalLogger().error("エラーが発生する前の新しいパスを{}個追加した。", added * -1);
                    }
                }
                lastOpenedTextFile = fc.getSelectedFile();
            } else if (event.getActionCommand().equals("dbimport")) {
                JFileChooser fc = new JFileChooser();
                FileFilter filter = new FileNameExtensionFilter("FFXIV Explorer database", "db");
                fc.setFileFilter(filter);
                int returnVal = fc.showOpenDialog(getParent()); //フレームが親コンポーネントである場合
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    HashDatabase.importDatabase(fc.getSelectedFile());
                }
            } else if (event.getActionCommand().equals("fileinject")) {
                FileInjectorWindow swapper = new FileInjectorWindow();
                swapper.setLocationRelativeTo(FileManagerWindow.this);
                swapper.setVisible(true);
            } else if (event.getActionCommand().equals("filename_save")) {
                //フルパス一覧出力
                if (currentIndexFile != null) {
                    SqPack_IndexFile.SqPack_Folder[] folders = currentIndexFile.getPackFolders();

                    if (lastSaveLocation_2 == null) {
                        lastSaveLocation_2 = new File("./" + Constants.DBFILE_NAME);
                    }
                    JFileChooser fileChooser = new JFileChooser(lastSaveLocation_2);
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    FileFilter filter = new FileFilter() {
                        @Override
                        public String getDescription() {
                            return "ファイル一覧出力";
                        }

                        @Override
                        public boolean accept(File f) {
                            return f.getName().endsWith(".txt") || f.isDirectory();
                        }
                    };
                    fileChooser.addChoosableFileFilter(filter);
                    fileChooser.setFileFilter(filter);
                    fileChooser.setAcceptAllFileFilterUsed(false);

                    //SaveDialogを開く
                    int retunval = fileChooser.showSaveDialog(FileManagerWindow.this);

                    if (retunval == JFileChooser.APPROVE_OPTION) {
                        lastSaveLocation_2 = fileChooser.getSelectedFile();
                        if (lastSaveLocation_2.getParentFile().mkdirs()){
                            Utils.getGlobalLogger().debug("フォルダ作成に成功");
                        }

                        String SavePath = "./" + currentIndexFile.getName() + "_AllFile.txt";
                        try {
                            SavePath = lastSaveLocation_2.getCanonicalPath() + "\\" + currentIndexFile.getName() + "_AllFile.txt";
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        try {
                            File file = new File(SavePath);
                            FileWriter filewriter = new FileWriter(file, true); //追記モード

                            for (SqPack_IndexFile.SqPack_Folder spPack_folder : folders){
                                SqPack_File[] files = spPack_folder.getFiles();
                                for (SqPack_File sqPack_file : files) {
                                    String folderName = spPack_folder.getName();

                                    String fileName = sqPack_file.getName();
                                    if (fileName == null) {
                                        fileName = String.format("%X", sqPack_file.getId());
                                    }
                                    if (folderName == null) {
                                        folderName = String.format("%X", spPack_folder.getId());
                                    }

                                    String path = folderName + "/" + fileName + "\r\n";

                                    filewriter.write(path);
                                }
                            }
                            filewriter.close();
                            Utils.getGlobalLogger().info("ファイル一覧出力完了");
                        } catch (FileNotFoundException e) {
                            Utils.getGlobalLogger().error(e);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    Utils.getGlobalLogger().debug("Indexファイルが読み込まれていません");
                }
            }
        }
    };

    private void setupMenu() {

        //File Menu
        JMenu file = new JMenu(Strings.MENU_FILE);
        file.setMnemonic(KeyEvent.VK_F);
        //ImageIcon icon1 = new ImageIcon("./icon01.png");
        //file.setIcon(icon1); アイコンを表示する場合
        JMenu search = new JMenu(Strings.MENU_SEARCH);
        search.setMnemonic(KeyEvent.VK_S);
        JMenu dataviewers = new JMenu(Strings.MENU_DATAVIEWERS);
        dataviewers.setMnemonic(KeyEvent.VK_V);
        JMenu tools = new JMenu(Strings.MENU_TOOLS);
        tools.setMnemonic(KeyEvent.VK_T);
        JMenu database = new JMenu(Strings.MENU_DATABASE);
        database.setMnemonic(KeyEvent.VK_D);
        JMenu options = new JMenu(Strings.MENU_OPTIONS);
        options.setMnemonic(KeyEvent.VK_O);
        JMenu help = new JMenu(Strings.MENU_HELP);
        help.setMnemonic(KeyEvent.VK_H);

        JMenuItem file_Open = new JMenuItem(Strings.MENUITEM_OPEN);
        file_Open.setActionCommand("open");

        file_Close = new JMenuItem(Strings.MENUITEM_CLOSE);
        file_Close.setEnabled(false);
        file_Close.setActionCommand("close");

        file_Extract = new JMenuItem(Strings.MENUITEM_EXTRACT);
        file_Extract.setEnabled(false);
        file_Extract.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK));

        file_ExtractRaw = new JMenuItem(Strings.MENUITEM_EXTRACTRAW);
        file_ExtractRaw.setEnabled(false);
        file_ExtractRaw.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK));
        file_Extract.setActionCommand("extractc");
        file_ExtractRaw.setActionCommand("extractr");

        JMenuItem file_Quit = new JMenuItem(Strings.MENUITEM_QUIT);
        file_Quit.setActionCommand("quit");
        file_Open.addActionListener(menuHandler);
        file_Close.addActionListener(menuHandler);
        file_Extract.addActionListener(menuHandler);
        file_ExtractRaw.addActionListener(menuHandler);
        file_Quit.addActionListener(menuHandler);

        search_search = new JMenuItem(Strings.MENUITEM_SEARCH);
        search_search.setEnabled(false);
        search_search.setActionCommand("search");
        search_search.addActionListener(menuHandler);
        search_search.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));

        search_searchAgain = new JMenuItem(Strings.MENUITEM_SEARCHAGAIN);
        search_searchAgain.setEnabled(false);
        search_searchAgain.setActionCommand("searchagain");
        search_searchAgain.addActionListener(menuHandler);
        search_searchAgain.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));

        JMenuItem dataviewer_modelViewer = new JMenuItem(Strings.MENUITEM_MODELVIEWER);
        dataviewer_modelViewer.setActionCommand("modelviewer");
        dataviewer_modelViewer.addActionListener(menuHandler);

        JMenuItem dataviewer_outfitter = new JMenuItem(Strings.MENUITEM_OUTFITTER);
        dataviewer_outfitter.setActionCommand("outfitter");
        dataviewer_outfitter.addActionListener(menuHandler);

        JMenuItem tools_musicswapper = new JMenuItem(Strings.MENUITEM_MUSICSWAPPER);
        tools_musicswapper.setActionCommand("musicswapper");
        tools_musicswapper.addActionListener(menuHandler);

        JMenuItem db_hashcalculator = new JMenuItem(Strings.MENUITEM_HASHCALC);
        db_hashcalculator.setActionCommand("hashcalc");
        db_hashcalculator.addActionListener(menuHandler);

        JMenuItem tools_macroEditor = new JMenuItem(Strings.MENUITEM_MACROEDITOR);
        tools_macroEditor.setActionCommand("macroeditor");
        tools_macroEditor.addActionListener(menuHandler);

        JMenuItem tools_logViewer = new JMenuItem(Strings.MENUITEM_LOGVIEWER);
        tools_logViewer.setActionCommand("logviewer");
        tools_logViewer.addActionListener(menuHandler);

        JMenuItem tools_findexhs = new JMenuItem(Strings.MENUITEM_FIND_EXH);
        tools_findexhs.setActionCommand("find_exh");
        tools_findexhs.addActionListener(menuHandler);

        JMenuItem tools_findMusic = new JMenuItem(Strings.MENUITEM_FIND_MUSIC);
        tools_findMusic.setActionCommand("find_music");
        tools_findMusic.addActionListener(menuHandler);

        JMenuItem tools_findMaps = new JMenuItem(Strings.MENUITEM_FIND_MAPS);
        tools_findMaps.setActionCommand("find_maps");
        tools_findMaps.addActionListener(menuHandler);

        JMenuItem tools_Test = new JMenuItem("ツールテスト");
        tools_Test.setActionCommand("tool_Test");
        tools_Test.addActionListener(menuHandler);

        JMenuItem options_settings = new JMenuItem(Strings.MENUITEM_SETTINGS);
        options_settings.setActionCommand("settings");
        options_settings.addActionListener(menuHandler);

        options_showAsHex = new JCheckBoxMenuItem(Strings.MENUITEM_EXD_HEX_OPTION, false);
        options_showAsHex.setActionCommand("options_showAsHex");

        options_sortByOffset = new JCheckBoxMenuItem(Strings.MENUITEM_EXD_OFFSET_OPTION, false);
        options_sortByOffset.setActionCommand("options_sortByOffset");

        Preferences prefs = Preferences.userNodeForPackage(com.fragmenterworks.ffxivextract.Main.class);
        options_enableUpdate = new JCheckBoxMenuItem(Strings.MENUITEM_ENABLEUPDATE, prefs.getBoolean(Constants.PREF_DO_DB_UPDATE, false));
        options_enableUpdate.setActionCommand("options_update");
        options_enableUpdate.addActionListener(menuHandler);

        JMenuItem help_About = new JMenuItem(Strings.MENUITEM_ABOUT);

        help_About.setActionCommand("about");
        help_About.addActionListener(menuHandler);

        JMenuItem db_importCeDump = new JMenuItem(Strings.MENUITEM_CEDUMPIMPORT);
        db_importCeDump.setActionCommand("cedumpimport");
        db_importCeDump.addActionListener(menuHandler);

        JMenuItem db_importDB = new JMenuItem(Strings.MENUITEM_DBIMPORT);
        db_importDB.setActionCommand("dbimport");
        db_importDB.addActionListener(menuHandler);


        JMenuItem tools_fileinject = new JMenuItem(Strings.MENUITEM_FILEINJECT);
        tools_fileinject.setActionCommand("fileinject");
        tools_fileinject.addActionListener(menuHandler);

        JMenuItem tools_filename_save = new JMenuItem("ファイルパス一覧出力");
        tools_filename_save.setActionCommand("filename_save");
        tools_filename_save.addActionListener(menuHandler);

        file.add(file_Open);
        file.add(file_Close);
        file.addSeparator();
        file.add(file_Extract);
        file.add(file_ExtractRaw);
        file.addSeparator();
        file.add(file_Quit);

        search.add(search_search);
        search.add(search_searchAgain);

        dataviewers.add(dataviewer_modelViewer);
        dataviewers.add(dataviewer_outfitter);

        tools.add(tools_musicswapper);
        tools.add(tools_filename_save);
        tools.add(tools_fileinject);
        tools.add(tools_macroEditor);
        tools.add(tools_logViewer);
        tools.addSeparator();
        tools.add(tools_findexhs);
        tools.add(tools_findMusic);
        tools.add(tools_findMaps);
        tools.add(tools_Test);

        database.add(db_hashcalculator);
        database.add(db_importCeDump);
        database.add(db_importDB);

        options.add(options_settings);
        options.add(options_enableUpdate);
        options.add(options_showAsHex);
        options.add(options_sortByOffset);

        help.add(help_About);

        //Super Menus
        menu.add(file);
        menu.add(search);
        menu.add(dataviewers);
        menu.add(tools);
        menu.add(database);
        menu.add(options);
        menu.add(help);

        this.setJMenuBar(menu);
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {

        if (fileTree.isOnlyFolder()) {
            splitPane.setRightComponent(defaultScrollPane);
            lblOffsetValue.setText("*");
            lblHashValue.setText("*");
            lblContentTypeValue.setText("*");
            file_Extract.setEnabled(true);
            file_ExtractRaw.setEnabled(true);
            return;
        }

        if (fileTree.getSelectedFiles().size() == 0) {
            file_Extract.setEnabled(false);
            file_ExtractRaw.setEnabled(false);
            return;
        } else {
            file_Extract.setEnabled(true);
            file_ExtractRaw.setEnabled(true);
        }

        if (fileTree.getSelectedFiles().size() > 1) {
            lblOffsetValue.setText("*");
            lblHashValue.setText("*");
            lblContentTypeValue.setText("*");
        } else {
            int datNum = currentIndexFile.getDatNum(fileTree.getSelectedFiles().get(0).getOffset());
            long realOffset = currentIndexFile.getOffsetInBytes(fileTree.getSelectedFiles().get(0).getOffset());

            lblOffsetValue.setText(String.format("0x%08X", realOffset) + " (Dat: " + datNum + ")");
            lblHashValue.setText(String.format("0x%08X", fileTree.getSelectedFiles().get(0).getId()));

            try {
                lblContentTypeValue.setText("" + currentIndexFile.getContentType(fileTree.getSelectedFiles().get(0).getOffset()));
            } catch (IOException ioe) {
                lblContentTypeValue.setText("Content Type Error");
            }
        }

        if (splitPane.getRightComponent() instanceof JTabbedPane) {
            JTabbedPane tabs = (JTabbedPane) splitPane.getRightComponent();

            if (tabs.getTabCount() > 0) {
                if (tabs.getComponentAt(0) instanceof Sound_View) {
                    ((Sound_View) tabs.getComponentAt(0)).stopPlayback();
                }
            }
        }

        openData(fileTree.getSelectedFiles().get(0));
    }

    /**
     * ファイルデータを閲覧
     * @param file SqPack_Fileクラス
     */
    private void openData(SqPack_File file) {

        JTabbedPane tabs = new JTabbedPane();

        byte[] data;
        int contentType;
        try {
            contentType = currentIndexFile.getContentType(file.getOffset());

            //プレースホルダーの場合は、気にしないでください
            if (contentType == 0x01) {
                JLabel lblFNFError = new JLabel("このファイルは現在プレースホルダーで、データはありません。");
                tabs.addTab("データなし", lblFNFError);
                hexView.setBytes(null);
                splitPane.setRightComponent(tabs);
                return;
            }

            data = currentIndexFile.extractFile(file.dataOffset, null);
        } catch (FileNotFoundException eFNF) {
            Utils.getGlobalLogger().error("{}のデータ{}が見つかりませんでした", currentIndexFile.getDatNum(file.getOffset()), file.getName(), eFNF);
            JLabel lblFNFError = new JLabel("このファイルのデータがありません!");
            tabs.addTab("Extractエラー", lblFNFError);
            hexView.setBytes(null);
            splitPane.setRightComponent(tabs);
            return;
        } catch (IOException e) {
            Utils.getGlobalLogger().error("ファイル{}を抽出できませんでした", file.getName(), e);
            JLabel lblLoadError = new JLabel("ファイルの抽出で何かが間違っています");
            tabs.addTab("Extractエラー", lblLoadError);
            hexView.setBytes(null);
            splitPane.setRightComponent(tabs);
            return;
        }

        //disable opengl for BE packs
        boolean threeDee = !currentIndexFile.isBigEndian();

        if (contentType == 3 && threeDee) {
            OpenGL_View view;

            try {
                if (HashDatabase.getFolder(file.getId2()) == null) {
                    view = new OpenGL_View(new Model(null, currentIndexFile, data, currentIndexFile.getEndian()));
                } else {
                    view = new OpenGL_View(new Model(HashDatabase.getFolder(file.getId2()) + "/" + file.getName(), currentIndexFile, data, currentIndexFile.getEndian()));
                }
            } catch (Exception modelException) {
                modelException.printStackTrace();
                JLabel lblLoadError = new JLabel("モデルの読み込み中にエラーが発生しました。");
                tabs.addTab("エラー", lblLoadError);
                hexView.setBytes(data);
                tabs.addTab("Raw Hex", hexView);
                splitPane.setRightComponent(tabs);
                return;
            }

            tabs.addTab("3Dモデル", view);
        }

        if (data == null) {
            JLabel lblLoadError = new JLabel("ファイルの抽出で何かが間違っています");
            tabs.addTab("Extractエラー", lblLoadError);
            hexView.setBytes(null);
            splitPane.setRightComponent(tabs);
            return;
        }

        //TODO: refactor this to use byte magic
        if (data.length >= 3 && checkMagic(data, "EXDF")) {
            if (exhfComponent == null || !exhfComponent.isSame(file.getName())) {
                exhfComponent = new EXDF_View(currentIndexFile, HashDatabase.getFolder(file.getId2()) + "/" + file.getName(), options_showAsHex.getState(), options_sortByOffset.getState());
            }
            tabs.addTab("EXDF File", exhfComponent);
        } else if (data.length >= 3 && checkMagic(data, "EXHF")) {
            try {
                if (exhfComponent == null || !exhfComponent.isSame(file.getName())) {
                    exhfComponent = new EXDF_View(currentIndexFile, HashDatabase.getFolder(file.getId2()) + "/" + file.getName(), new EXHF_File(data), options_showAsHex.getState(), options_sortByOffset.getState());
                }
                tabs.addTab("EXHF File", exhfComponent);
            } catch (IOException e) {
                Utils.getGlobalLogger().error(e);
            }
        } else if (data.length >= 8 && checkMagic(data, "SEDBSSCF")) {
            Sound_View scdComponent;
            try {
                scdComponent = new Sound_View(new SCD_File(data, currentIndexFile.getEndian()));
                tabs.addTab("SCD File", scdComponent);
            } catch (IOException e) {
                Utils.getGlobalLogger().error(e);
            }
        } else if (data.length >= 4 && checkMagic(data, "CUTB")) {
            try {
                @SuppressWarnings("unused")
                CUTB_File cutbFile = new CUTB_File(currentIndexFile, data, currentIndexFile.getEndian());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (data.length >= 4 && checkMagic(data, "XFVA")) {
            AVFX_File avfxFile = new AVFX_File(currentIndexFile, data, currentIndexFile.getEndian());
            avfxFile.regHash();
        } else if (data.length >= 4 && contentType == 2 && file.getName().endsWith("mtrl")) {
            //魚拓や絵画などのmdlファイルがないものでもmtrlファイル情報からtexをDB登録できるようにした
            new Material(HashDatabase.getFolder(file.getId2()), currentIndexFile, data, currentIndexFile.getEndian());
        } else if (contentType == 4 || file.getName().endsWith(".atex")
                ||(data.length >= 4 && (data[0]==0 && data[1]==0 && data[2] == (byte)0x80 && data[3] == 0))) {
            Image_View imageComponent = new Image_View(new Texture_File(data, currentIndexFile.getEndian()));
            tabs.addTab("テクスチャ", imageComponent);
        } else if (data.length >= 5 && checkMagic(data, "LuaQ", 1)) {
            //TODO: double-check this if you ever feel like working on SDAT
            try {
                Lua_View luaView;

                String text = getDecompiledLuaString(data);

                luaView = new Lua_View(("-- unluac_2021_04_09 1.2.3.446 by tehtmi (https://sourceforge.net/projects/unluac/)を使用して逆コンパイルしました。\n" + text).split("\\r?\\n"));
                tabs.addTab("Lua(デコンパイル)", luaView);
            } catch (Exception e) {
                Utils.getGlobalLogger().error(e);
            }
        } else if (data.length >= 4 && checkMagic(data, "pap ")) {
            try {
                PAP_View papView = new PAP_View(new PAP_File(data, currentIndexFile.getEndian()));
                tabs.addTab("アニメーションファイル", papView);
            } catch (IOException e) {
                Utils.getGlobalLogger().error(e);
            }
        } else if (data.length >= 4 && checkMagic(data, "ShCd")) {
            try {
                Shader_View shaderView = new Shader_View(new SHCD_File(data, currentIndexFile.getEndian()));
                tabs.addTab("シェーダーファイル", shaderView);
            } catch (IOException e) {
                Utils.getGlobalLogger().error(e);
            }
        } else if (data.length >= 4 && checkMagic(data, "ShPk")) {
            try {
                Shader_View shaderView = new Shader_View(new SHPK_File(data, currentIndexFile.getEndian()));
                tabs.addTab("シェーダーパック", shaderView);
            } catch (IOException e) {
                Utils.getGlobalLogger().error(e);
            }
        } else if (data.length >= 4 && checkMagic(data, "imc ")) {
            //file.getName().endsWith(".imc")
            IMC_View imcView = new IMC_View(new IMC_File(data, currentIndexFile.getEndian()));
            tabs.addTab("モデル情報ファイル", imcView);
        } else if (data.length >= 4 && checkMagic(data, "SGB1")) {
            try {
                @SuppressWarnings("unused")
                SGB_File sgbFile = new SGB_File(currentIndexFile, data, currentIndexFile.getEndian());
            } catch (IOException e) {
                Utils.getGlobalLogger().error(e);
            }
        } else if (data.length >= 4 && checkMagic(data, "TMLB")) {
            try {
                @SuppressWarnings("unused")
                TMB_File tmbFile = new TMB_File(currentIndexFile, data, currentIndexFile.getEndian());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (data.length >= 4 && checkMagic(data, "uldh")) {
            try {
                ULD_View uldView = new ULD_View(new ULD_File(currentIndexFile, data, currentIndexFile.getEndian()));
                tabs.addTab("ULDレンダラー", uldView);
            } catch (Exception e) {
                Utils.getGlobalLogger().error(e);
            }
        } else if (file.getName().equals("human.cmp")) {
            CMP_File cmpFile = new CMP_File(data);
            CMP_View cmpView = new CMP_View(cmpFile);
            tabs.addTab("CMP Viewer", cmpView);
        }

        //条件なし
        hexView.setBytes(data);
        //if (contentType != 3)
        tabs.addTab("Raw Hex", hexView);
        splitPane.setRightComponent(tabs);
    }

    private boolean checkMagic(byte[] data, String magic) {
        return checkMagic(data, magic, 0);
    }

    private boolean checkMagic(byte[] data, String magic, int startOffset) {
        byte[] littleBuf = new byte[magic.length()];
        byte[] bigBuf = new byte[magic.length()];

        System.arraycopy(data, startOffset, littleBuf, 0, magic.length());
        System.arraycopy(data, startOffset, bigBuf, 0, magic.length());

        // Didn't want to take the chance that an alternating pattern, i.e. big, little, big, little
        // would somehow happen to match a magic
        boolean matchesLittle = true;
        boolean matchesBig = true;

        for (int i = 0; i < magic.length(); i++) {
            if (littleBuf[i] != magic.charAt(i)) {
                matchesLittle = false;
                break;
            }
        }

        if (matchesLittle) {
            return true;
        }

        for (int i = magic.length() - 1; i >= 0; i--) {
            if (bigBuf[i] != magic.charAt(magic.length() - 1 - i)) {
                matchesBig = false;
                break;
            }
        }

        return matchesBig;
    }

    /**
     * データをファイルに変換
     * @param doConvert True:汎用データ、False:生データ
     */
    private void extract(boolean doConvert) {
        JFileChooser fileChooser = new JFileChooser(lastSaveLocation);

        ArrayList<SqPack_File> files = fileTree.getSelectedFiles();


        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        FileFilter filter = new FileFilter() {

            @Override
            public String getDescription() {
                return "FFXIV Converted";
            }

            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".csv") || f.getName().endsWith(".ogg") || f.getName().endsWith(".wav") || f.getName().endsWith(".png") || f.getName().endsWith(".hkx") || f.getName().endsWith(".obj") || f.isDirectory();
            }
        };
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false);


        int retunval = fileChooser.showSaveDialog(FileManagerWindow.this);

        if (retunval == JFileChooser.APPROVE_OPTION) {
            lastSaveLocation = fileChooser.getSelectedFile();
            if (lastSaveLocation.getParentFile().mkdirs()){
                Utils.getGlobalLogger().trace("フォルダ作成に成功");
            }else{
                Utils.getGlobalLogger().trace("フォルダが既にあるか、作成に失敗した");
            }
            Loading_Dialog loadingDialog = new Loading_Dialog(FileManagerWindow.this, files.size());
            loadingDialog.setTitle("抽出中...");
            ExtractTask task = new ExtractTask(files, loadingDialog, doConvert);
            task.execute();
            loadingDialog.setLocationRelativeTo(this);
            loadingDialog.setVisible(true);
        }
    }

    private String getDecompiledLuaString(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.rewind();

        BHeader header = new BHeader(buffer, new unluac.Configuration());
        Decompiler d = new Decompiler(header.main);
        Decompiler.State t = d.decompile();
        final StringBuilder pout = new StringBuilder();

        d.print(t, new OutputProvider() {
            @Override
            public void print(String s) {
                pout.append(s);
            }

            @Override
            public void print(byte b) {
                pout.append(b);
            }

            @Override
            public void println() {
                pout.append("\n");
            }
        });
        return pout.toString();
    }

    private String getExtension(int contentType, byte[] data) {
        if (data.length >= 4 && data[0] == 'E' && data[1] == 'X' && data[2] == 'D' && data[3] == 'F') {
            return ".exd";
        } else if (data.length >= 4 && data[0] == 'E' && data[1] == 'X' && data[2] == 'H' && data[3] == 'F') {
            return ".exh";
        } else if (data.length >= 5 && data[1] == 'L' && data[2] == 'u' && data[3] == 'a' && data[4] == 'Q') {
            return ".luab";
        } else if (data.length >= 4 && data[0] == 'S' && data[1] == 'E' && data[2] == 'D' && data[3] == 'B') {
            return ".scd";
        } else if (data.length >= 4 && data[0] == 'p' && data[1] == 'a' && data[2] == 'p' && data[3] == ' ') {
            return ".hkx";
        } else if (data.length >= 4 && data[0] == 'b' && data[1] == 'l' && data[2] == 'k' && data[3] == 's') {
            return ".hkx";
        } else if (data.length >= 4 && data[0] == 'S' && data[1] == 'h' && data[2] == 'C' && data[3] == 'd') {
            return ".cso";
        } else if (data.length >= 4 && data[0] == 'S' && data[1] == 'h' && data[2] == 'P' && data[3] == 'k') {
            return ".shpk";
        } else if (contentType == 3) {
            return ".obj";
        } else if (contentType == 4) {
            return ".png";
        } else {
            return "";
        }
    }

    /**
     * Indexファイル読み込みタスク
     */
    class OpenIndexTask extends SwingWorker<Void, Void> {

        final File selectedFile;

        /**
         * Indexファイル読み込みタスクのコンストラクタ
         * @param selectedFile .indexファイル名
         */
        OpenIndexTask(File selectedFile) {
            this.selectedFile = selectedFile;
            menu.setEnabled(false);
            prgLoadingBar.setVisible(true);
            prgLoadingBar.setValue(0);
            lblLoadingBarString.setVisible(true);
            for (int i = 0; i < menu.getMenuCount(); i++) {
                menu.getMenu(i).setEnabled(false);
            }
        }

        @Override
        protected Void doInBackground() {
            try {
                HashDatabase.beginConnection();
                currentIndexFile = new SqPack_IndexFile(selectedFile.getAbsolutePath(), prgLoadingBar, lblLoadingBarString);
                HashDatabase.closeConnection();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(FileManagerWindow.this,
                        "このインデックスファイルを開くときにエラーが発生しました。",
                        "ファイルオープンエラー(" + e + ")",
                        JOptionPane.ERROR_MESSAGE);
                Utils.getGlobalLogger().error(e);
                return null;
            }
            return null;
        }

        @Override
        protected void done() {
            Utils.getGlobalLogger().trace("{}", currentIndexFile);

            setTitle(Constants.APPNAME + " [" + selectedFile.getName() + "]");
            file_Close.setEnabled(true);
            search_search.setEnabled(true);
            prgLoadingBar.setValue(0);
            prgLoadingBar.setVisible(false);
            lblLoadingBarString.setVisible(false);
            fileTree.fileOpened(currentIndexFile);
            searchWindow = new SearchWindow(FileManagerWindow.this, currentIndexFile, FileManagerWindow.this);
            for (int i = 0; i < menu.getMenuCount(); i++) {
                menu.getMenu(i).setEnabled(true);
            }
            lblHashInfoValue.setText(String.format("%d / %d", currentIndexFile.getNumUnHashedFiles(), currentIndexFile.getTotalFiles()));
        }
    }

    class ExtractTask extends SwingWorker<Void, Void> {

        final ArrayList<SqPack_File> files;
        final Loading_Dialog loadingDialog;
        final boolean doConvert;

        ExtractTask(ArrayList<SqPack_File> files, Loading_Dialog loadingDialog, boolean doConvert) {
            this.files = files;
            this.loadingDialog = loadingDialog;
            this.doConvert = doConvert;
        }

        @Override
        protected Void doInBackground() throws Exception {
            EXDF_View tempView = null;

            for (int i = 0; i < files.size(); i++) {
                try {
                    String folderName = HashDatabase.getFolder(files.get(i).getId2());
                    String fileName = files.get(i).getName();
                    if (fileName == null) {
                        fileName = String.format("%X", files.get(i).getId());
                    }
                    if (folderName == null) {
                        folderName = String.format("%X", files.get(i).getId2());
                    }

                    loadingDialog.nextFile(i, folderName + "/" + fileName);

                    if (currentIndexFile.getContentType(files.get(i).getOffset()) == 1) {
                        continue;
                    }

                    byte[] data = currentIndexFile.extractFile(files.get(i).getOffset(), loadingDialog);
                    byte[] dataToSave = null;

                    if (data == null) {
                        continue;
                    }

                    int contentType = currentIndexFile.getContentType(files.get(i).getOffset());
                    String extension = getExtension(contentType, data);

                    if (doConvert) {
                        switch (extension) {
                            case ".exh": {
                                if (tempView != null && tempView.isSame(files.get(i).getName())) {
                                    continue;
                                }
                                EXHF_File file = new EXHF_File(data);

                                tempView = new EXDF_View(currentIndexFile,
                                        HashDatabase.getFolder(fileTree.getSelectedFiles().get(i).getId2()) + "/" + fileTree.getSelectedFiles().get(i).getName(),
                                        file,
                                        options_showAsHex.getState(),
                                        options_sortByOffset.getState());

                                for (int l = 0; l < (tempView.getNumLanguages()); l++) {

                                    String path;

                                    if (fileName == null) {
                                        fileName = String.format("%X", files.get(i).getId());
                                    }

                                    if (folderName == null) {
                                        folderName = String.format("%X", files.get(i).getId2());
                                    }

                                    path = lastSaveLocation.getCanonicalPath() + "\\" + folderName + "\\" + fileName;

                                    File mkDirPath = new File(path);
                                    if (mkDirPath.getParentFile().mkdirs()) {
                                        Utils.getGlobalLogger().debug("フォルダ作成に成功");
                                    }
                                    tempView.saveCSV(path + EXHF_File.languageCodes[tempView.getExhFile().getLanguageTable()[l]] + ".csv", l);
                                }

                                continue;
                            }
                            case ".exd":
                                if (tempView != null && tempView.isSame(files.get(i).getName())) {
                                    continue;
                                }

                                tempView = new EXDF_View(currentIndexFile,
                                        HashDatabase.getFolder(fileTree.getSelectedFiles().get(i).getId2()) + "/" + fileTree.getSelectedFiles().get(i).getName(),
                                        options_showAsHex.getState(),
                                        options_sortByOffset.getState());

                                //Remove the thing
                                String exhName = files.get(i).getName();

                                for (int l = 0; l < EXHF_File.languageCodes.length; l++) {
                                    exhName = exhName.replace(String.format("%s.exd", EXHF_File.languageCodes[l]), "");
                                }

                                exhName = exhName.substring(0, exhName.lastIndexOf("_")) + ".exh";

                                for (int l = 0; l < (tempView.getNumLanguages()); l++) {
                                    String path;

                                    fileName = exhName;

                                    if (folderName == null) {
                                        folderName = String.format("%X", files.get(i).getId2());
                                    }

                                    path = lastSaveLocation.getCanonicalPath() + "\\" + folderName + "\\" + fileName;

                                    File mkDirPath = new File(path);
                                    if (mkDirPath.getParentFile().mkdirs()) {
                                        Utils.getGlobalLogger().trace("フォルダ作成に成功");
                                    }
                                    tempView.saveCSV(path + EXHF_File.languageCodes[tempView.getExhFile().getLanguageTable()[l]] + ".csv", l);
                                }

                                continue;
                            case ".obj":
                                Model model = new Model(folderName + "/" + fileName, currentIndexFile, data, currentIndexFile.getEndian());

                                String path;

                                if (fileName == null) {
                                    fileName = String.format("%X", files.get(i).getId());
                                }

                                if (folderName == null) {
                                    folderName = String.format("%X", files.get(i).getId2());
                                }

                                path = lastSaveLocation.getCanonicalPath() + "\\" + folderName + "\\" + fileName;

                                File mkDirPath = new File(path);
                                if (mkDirPath.getParentFile().mkdirs()) {
                                    Utils.getGlobalLogger().trace("フォルダ作成に成功");
                                }
                                WavefrontObjectWriter.writeObj(path, model, currentIndexFile.getEndian());
                                continue;
                            case ".hkx":
                                if (data.length >= 4 && data[0] == 'p' && data[1] == 'a' && data[2] == 'p' && data[3] == ' ') {
                                    PAP_File pap = new PAP_File(data, currentIndexFile.getEndian());
                                    dataToSave = pap.getHavokData();
                                } else if (data.length >= 4 && data[0] == 'b' && data[1] == 'l' && data[2] == 'k' && data[3] == 's') {
                                    SKLB_File pap = new SKLB_File(data, currentIndexFile.getEndian());
                                    dataToSave = pap.getHavokData();
                                }

                                extension = ".hkx";
                                break;
                            case ".cso":
                                SHCD_File cd_shader = new SHCD_File(data, currentIndexFile.getEndian());
                                dataToSave = cd_shader.getShaderBytecode();
                                extension = ".cso";
                                break;
                            case ".shpk":
                                try {
                                    SHPK_File pk_shader = new SHPK_File(data, currentIndexFile.getEndian());

                                    for (int j = 0; j < pk_shader.getNumVertShaders(); j++) {
                                        dataToSave = pk_shader.getShaderBytecode(j);
                                        extension = ".vs.cso";

                                        if (fileName == null) {
                                            fileName = String.format("%X", files.get(i).getId());
                                        }

                                        if (folderName == null) {
                                            folderName = String.format("%X", files.get(i).getId2());
                                        }

                                        path = lastSaveLocation.getCanonicalPath() + "\\" + folderName + "\\" + fileName;

                                        mkDirPath = new File(path);
                                        if (mkDirPath.getParentFile().mkdirs()) {
                                            Utils.getGlobalLogger().trace("フォルダ作成に成功");
                                        }
                                        EARandomAccessFile out = new EARandomAccessFile(path + j + extension, "rw", ByteOrder.LITTLE_ENDIAN);
                                        out.write(dataToSave, 0, dataToSave.length);
                                        out.close();
                                    }
                                    for (int j = 0; j < pk_shader.getNumPixelShaders(); j++) {
                                        dataToSave = pk_shader.getShaderBytecode(pk_shader.getNumVertShaders() + j);
                                        extension = ".ps.cso";

                                        if (fileName == null) {
                                            fileName = String.format("%X", files.get(i).getId());
                                        }

                                        if (folderName == null) {
                                            folderName = String.format("%X", files.get(i).getId2());
                                        }

                                        path = lastSaveLocation.getCanonicalPath() + "\\" + folderName + "\\" + fileName;

                                        mkDirPath = new File(path);
                                        if (mkDirPath.getParentFile().mkdirs()) {
                                            Utils.getGlobalLogger().trace("フォルダ作成に成功");
                                        }
                                        EARandomAccessFile out = new EARandomAccessFile(path + j + extension, "rw", ByteOrder.LITTLE_ENDIAN);
                                        out.write(dataToSave, 0, dataToSave.length);
                                        out.close();
                                    }

                                } catch (IOException e) {
                                    Utils.getGlobalLogger().error(e);
                                }
                                break;
                            case ".png":
                                Texture_File tex = new Texture_File(data, currentIndexFile.getEndian());
                                dataToSave = tex.getImage("png");
                                extension = ".png";
                                break;
                            case ".luab":
                                String text = getDecompiledLuaString(data);
                                dataToSave = text.getBytes();
                                break;
                            case ".scd": {
                                SCD_File file = new SCD_File(data, currentIndexFile.getEndian());

                                for (int s = 0; s < file.getNumEntries(); s++) {
                                    SCD_Sound_Info info = file.getSoundInfo(s);
                                    if (info == null) {
                                        continue;
                                    }
                                    if (info.dataType == 0x06) {
                                        dataToSave = file.getConverted(s);
                                        extension = ".ogg";
                                    } else if (info.dataType == 0x0C) {
                                        dataToSave = file.getConverted(s);
                                        extension = ".wav";
                                    } else {
                                        continue;
                                    }

                                    if (fileName == null) {
                                        fileName = String.format("%X", files.get(i).getId());
                                    }

                                    if (folderName == null) {
                                        folderName = String.format("%X", files.get(i).getId2());
                                    }

                                    path = lastSaveLocation.getCanonicalPath() + "\\" + folderName + "\\" + fileName;

                                    mkDirPath = new File(path);
                                    if (mkDirPath.getParentFile().mkdirs()) {
                                        Utils.getGlobalLogger().trace("フォルダ作成に成功");
                                    }
                                    EARandomAccessFile out = new EARandomAccessFile(path + (file.getNumEntries() == 1 ? "" : "_" + s) + extension, "rw", ByteOrder.LITTLE_ENDIAN);
                                    out.write(dataToSave, 0, dataToSave.length);
                                    out.close();
                                }
                                continue;
                            }
                        }
                    } else {
                        dataToSave = data;
                    }

                    if (dataToSave == null) {
                        JOptionPane.showMessageDialog(FileManagerWindow.this,
                                String.format("%X", files.get(i).getId()) + " could not be converted to " + extension.substring(1).toUpperCase() + ".",
                                "Export Error",
                                JOptionPane.ERROR_MESSAGE);
                        continue;
                    }

                    String path;

                    if (fileName == null) {
                        fileName = String.format("%X", files.get(i).getId());
                    }

                    if (folderName == null) {
                        folderName = String.format("%X", files.get(i).getId2());
                    }

                    if (!doConvert) {
                        extension = "";
                    }

                    path = lastSaveLocation.getCanonicalPath() + "\\" + folderName + "\\" + fileName;
                    if (currentIndexFile.isBigEndian()) {
                        int period = path.lastIndexOf(".");
                        String ext = path.substring(period);
                        path = path.replace(ext, ".ps3.d" + ext);
                    }

                    File mkDirPath = new File(path);
                    if (mkDirPath.getParentFile().mkdirs()){
                        Utils.getGlobalLogger().trace("フォルダ作成に成功");
                    }
                    EARandomAccessFile out = new EARandomAccessFile(path + extension, "rw", ByteOrder.LITTLE_ENDIAN);
                    out.write(dataToSave, 0, dataToSave.length);
                    out.close();
                } catch (FileNotFoundException e) {
                    Utils.getGlobalLogger().error(e);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void done() {
            loadingDialog.setVisible(false);
            loadingDialog.dispose();
        }

    }


    @Override
    public void onSearchChosen(SqPack_File file) {

        if (file == null) {
            search_searchAgain.setEnabled(false);
            searchWindow.reset();
            return;
        }

        if (splitPane.getRightComponent() instanceof JTabbedPane) {
            JTabbedPane tabs = (JTabbedPane) splitPane.getRightComponent();

            if (tabs.getTabCount() > 0) {
                if (tabs.getComponentAt(0) instanceof Sound_View) {
                    ((Sound_View) tabs.getComponentAt(0)).stopPlayback();
                }
            }
        }

        openData(file);
        fileTree.select(file.getOffset());
        search_searchAgain.setEnabled(true);

    }

    @Override
    public void windowActivated(WindowEvent arg0) {
    }

    @Override
    public void windowClosed(WindowEvent arg0) {
    }

    @Override
    public void windowClosing(WindowEvent arg0) {
        closeFile();
    }

    @Override
    public void windowDeactivated(WindowEvent arg0) {
    }

    @Override
    public void windowDeiconified(WindowEvent arg0) {
    }

    @Override
    public void windowIconified(WindowEvent arg0) {
    }

    @Override
    public void windowOpened(WindowEvent arg0) {
    }


}
