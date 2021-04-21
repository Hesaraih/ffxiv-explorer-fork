package com.fragmenterworks.ffxivextract.gui;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.Strings;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile.SqPack_File;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile.SqPack_Folder;

import javax.swing.*;
import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

@SuppressWarnings("serial")
class SearchWindow extends JDialog {

    JMenuBar menu = new JMenuBar();

    // FILE IO
	private final SqPack_IndexFile currentIndexFile;
    private final ISearchComplete searchCallback;

    // UI
	private final JPanel pnlSearchString;
    private final JPanel pnlSearchBytes;
    private final JRadioButton rbtnSearchString;
	private final JRadioButton rbtnSearchBytes;
    private final JLabel txtSearchLabel;
	private final JLabel txtSearchLabel2;
    private final JTextField txtStringToSearch;
	private final JTextField txtBytesToSearch;
    JButton btnBrowse;
    private final ButtonGroup searchGroup = new ButtonGroup();

    private final JPanel pnlButtons;
    private final JButton btnSearch;
	private final JButton btnClose;

    // SAVED SEARCH
	private boolean lastSearchWasString;
    private String lastString;
    private int lastFolder = 0;
    private int lastFile = 0;

    public SearchWindow(JFrame parent, SqPack_IndexFile currentIndexFile,
                        ISearchComplete searchCallback) {
        super(parent, ModalityType.APPLICATION_MODAL);
        this.setTitle(Strings.DIALOG_TITLE_SEARCH);
        URL imageURL = getClass().getResource("/frameicon.png");
        ImageIcon image = new ImageIcon(imageURL);
        this.setIconImage(image.getImage());
        this.searchCallback = searchCallback;

        this.currentIndexFile = currentIndexFile;

        // String search
        GridBagLayout layout = new GridBagLayout();
        //pnlSearchString = new JPanel(new GridBagLayout());
        pnlSearchString = new JPanel(layout);

        txtSearchLabel = new JLabel(Strings.SEARCH_FRAMETITLE_BYSTRING);
        txtStringToSearch = new JTextField(lastString == null ? "" : lastString);
        txtStringToSearch.setPreferredSize(new Dimension(200, txtSearchLabel
                .getPreferredSize().height+6));
        rbtnSearchString = new JRadioButton(Strings.SEARCH_FRAMETITLE_BYSTRING);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 5, 5); //グリッドバッグ内の余白
        gbc.anchor = GridBagConstraints.WEST; //グリッドバッグ内での位置：左寄せ中央

        gbc.gridx = 0;
        gbc.gridy = 0;
        layout.setConstraints(rbtnSearchString, gbc);
        pnlSearchString.add(rbtnSearchString);

        gbc.gridx = 1;
        gbc.gridy = 0;
        layout.setConstraints(txtStringToSearch, gbc);
        pnlSearchString.add(txtStringToSearch);

        // Byte search
        pnlSearchBytes = new JPanel(new GridBagLayout());
        txtSearchLabel2 = new JLabel(Strings.SEARCH_FRAMETITLE_BYBYTES);
        txtBytesToSearch = new JTextField();
        txtBytesToSearch.setPreferredSize(new Dimension(200, txtSearchLabel2
                .getPreferredSize().height+6));
        rbtnSearchBytes = new JRadioButton(Strings.SEARCH_FRAMETITLE_BYBYTES);

        gbc.gridx = 0;
        gbc.gridy = 1;
        layout.setConstraints(rbtnSearchBytes, gbc);
        pnlSearchString.add(rbtnSearchBytes);

        gbc.gridx = 1;
        gbc.gridy = 1;
        layout.setConstraints(txtBytesToSearch, gbc);
        pnlSearchString.add(txtBytesToSearch);
        //pnlSearchBytes.add(rbtnSearchBytes);
        //pnlSearchBytes.add(txtBytesToSearch);

        // Buttons
        pnlButtons = new JPanel();
        pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
        btnSearch = new JButton(Strings.BUTTONNAMES_SEARCH);
        btnClose = new JButton(Strings.BUTTONNAMES_CLOSE);
        pnlButtons.add(btnSearch);
        pnlButtons.add(btnClose);

        // ROOT
        JPanel pnlRoot = new JPanel();
        pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.Y_AXIS));
        pnlRoot.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlRoot.add(pnlSearchString);
        pnlRoot.add(pnlSearchBytes);
        pnlRoot.add(pnlButtons);

        rbtnSearchString.setActionCommand("string");
        rbtnSearchBytes.setActionCommand("bytes");
        searchGroup.add(rbtnSearchString);
        searchGroup.add(rbtnSearchBytes);
        searchGroup.setSelected(rbtnSearchString.getModel(), true);
        txtBytesToSearch.setEnabled(false);

        getRootPane().setDefaultButton(btnSearch);

        rbtnSearchString.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (rbtnSearchString.isSelected()) {
                    txtStringToSearch.setEnabled(true);
                    txtBytesToSearch.setEnabled(false);

                }
            }
        });
        rbtnSearchBytes.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (rbtnSearchBytes.isSelected()) {
                    txtStringToSearch.setEnabled(false);
                    txtBytesToSearch.setEnabled(true);
                }
            }
        });

        ActionListener doSearchAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SearchWindow.this.setTitle("Searching...");
                if (searchGroup.getSelection().getActionCommand()
                        .equals("string")) {
                    doStringSearch(txtStringToSearch.getText());
                } else if (searchGroup.getSelection().getActionCommand()
                        .equals("bytes")) {

                    if (txtBytesToSearch.getText().length() % 2 != 0) {
                        JOptionPane.showMessageDialog(SearchWindow.this,
                                "The byte string must be divisible by 2.",
                                "Byte String Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    doBytesSearch(txtBytesToSearch.getText());
                }
                SearchWindow.this.setTitle(Strings.DIALOG_TITLE_SEARCH);
            }
        };

        btnSearch.addActionListener(doSearchAction);
        btnClose.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);

            }
        });

        txtBytesToSearch.registerKeyboardAction(doSearchAction, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), JComponent.WHEN_FOCUSED);
        txtStringToSearch.registerKeyboardAction(doSearchAction, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), JComponent.WHEN_FOCUSED);

        txtBytesToSearch.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();

                if ((c >= 'a' && c <= 'f'))
                    return;
                if ((c >= 'A' && c <= 'F'))
                    return;
                else if (Character.isDigit(e.getKeyChar()))
                    return;
                else
                    e.consume();
            }
        });

        getRootPane().registerKeyboardAction(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                setVisible(false);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        getContentPane().add(pnlRoot);
        pack();
    }

    private void doStringSearch(String string) {
        lastSearchWasString = true;
        lastString = string;
        for (int i = lastFolder; i < currentIndexFile.getPackFolders().length; i++) {
            SqPack_Folder f = currentIndexFile.getPackFolders()[i];
            for (int j = lastFile; j < f.getFiles().length; j++) {
                SqPack_File fi = f.getFiles()[j];
                byte[] data;
                try {
                    data = currentIndexFile.extractFile(fi.dataOffset, null);
                    if (data == null)
                        continue;

                    boolean breakOutOfFile = false;
                    for (int i2 = 0; i2 < data.length - string.length(); i2++) {
                        for (int j2 = 0; j2 < string.length(); j2++) {
                            if (Character.toLowerCase(data[i2 + j2]) == Character
                                    .toLowerCase(string.charAt(j2))) {
                                if (j2 == string.length() - 1) {

                                    if (Constants.DEBUG) {
                                        System.out.println(String.format(
                                                "Folder: %08X",
                                                f.getId()));
                                        System.out.println(String.format(
                                                "File: %08X",
                                                fi.getId()));
                                        System.out.println("---");
                                    }
                                    Object[] options = {"Continue", "Open",
                                            "Stop Search"};

                                    int n = JOptionPane.showOptionDialog(
                                            this,
                                            "Found result in folder: "
                                                    + f.getName() + ", file: "
                                                    + fi.getName(),
                                            "Searching through DAT...",
                                            JOptionPane.YES_NO_CANCEL_OPTION,
                                            JOptionPane.QUESTION_MESSAGE, null,
                                            options, options[2]);
                                    switch (n) {
                                        case 0:
                                            breakOutOfFile = true;
                                            break;
                                        case 1:
                                            lastFolder = i + 1;
                                            lastFile = j + 1;
                                            searchCallback.onSearchChosen(fi);
                                            this.setVisible(false);
                                            return;
                                        case 2:
                                            searchCallback.onSearchChosen(null);
                                            setVisible(false);
                                            return;
                                    }

                                } else {
								}
                            } else
                                break;
                        }
                        if (breakOutOfFile)
                            break;
                    }
                } catch (IOException e) {
                    Utils.getGlobalLogger().error(e);
                }

            }
        }

        JOptionPane.showMessageDialog(SearchWindow.this, "Search completed.",
                "Search Finished", JOptionPane.QUESTION_MESSAGE);
        searchCallback.onSearchChosen(null);
        this.dispose();
    }

    private void doBytesSearch(String bytes) {
        lastSearchWasString = false;
        lastString = bytes;

        // Convert String to Byte Array
        String temp = lastString.replace(" ", "");
        byte[] searchArray = toByteArray(temp);
        byte[] compareBuffer = new byte[searchArray.length];

        // Do search
        for (int i = lastFolder; i < currentIndexFile.getPackFolders().length; i++) {
            SqPack_Folder f = currentIndexFile.getPackFolders()[i];
            for (int j = lastFile; j < f.getFiles().length; j++) {
                SqPack_File fi = f.getFiles()[j];
                byte[] data;
                try {
                    data = currentIndexFile.extractFile(fi.dataOffset, null);

                    if (data == null)
                        continue;

                    ByteBuffer dataBB = ByteBuffer.wrap(data);

                    boolean breakOutOfFile = false;

                    while (!breakOutOfFile) {
                        try {
                            dataBB.get(compareBuffer);
                            dataBB.position(dataBB.position()
                                    - compareBuffer.length + 1);
                            if (Arrays.equals(compareBuffer, searchArray)) {
                                if (Constants.DEBUG) {
                                    System.out.println(String.format(
                                            "Folder: %08X",
                                            f.getId()));
                                    System.out.println(String.format(
                                            "File: %08X",
                                            fi.getId()));
                                    System.out.println("---");
                                }

                                Object[] options = {"Continue", "Open",
                                        "Stop Search"};

                                int n = JOptionPane.showOptionDialog(
                                        this,
                                        "Found result in folder: "
                                                + f.getName() + ", file: "
                                                + fi.getName(),
                                        "Searching through DAT...",
                                        JOptionPane.YES_NO_CANCEL_OPTION,
                                        JOptionPane.QUESTION_MESSAGE, null,
                                        options, options[2]);
                                switch (n) {
                                    case 0:
                                        breakOutOfFile = true;
                                        break;
                                    case 1:
                                        lastFolder = i + 1;
                                        lastFile = j + 1;
                                        searchCallback.onSearchChosen(fi);
                                        this.setVisible(false);
                                        return;
                                    case 2:
                                        searchCallback.onSearchChosen(null);
                                        this.dispose();
                                        return;
                                }
                            }
                        } catch (BufferUnderflowException e) {
                            break;
                        }
                    }

                } catch (IOException e) {
                    Utils.getGlobalLogger().error(e);
                }

            }
        }

        JOptionPane.showMessageDialog(SearchWindow.this, "Search completed.",
                "Search Finished", JOptionPane.QUESTION_MESSAGE);
        searchCallback.onSearchChosen(null);
        this.dispose();
    }

    public void searchAgain() {
        if (lastSearchWasString)
            doStringSearch(lastString);
        else
            doBytesSearch(lastString);
    }

    public interface ISearchComplete {
        void onSearchChosen(SqPack_File fi);
    }

    public void reset() {
        lastFile = 0;
        lastFolder = 0;
    }

    private static byte[] toByteArray(String s) {
        return DatatypeConverter.parseHexBinary(s);
    }

    @Override
    public void setVisible(boolean arg0) {
        txtStringToSearch.setText("");
        txtBytesToSearch.setText("");
        if (lastSearchWasString && lastString != null) {
            rbtnSearchString.setSelected(true);
            txtStringToSearch.setText(lastString);
            txtStringToSearch.requestFocus();
        } else if (!lastSearchWasString && lastString != null) {
            rbtnSearchBytes.setSelected(true);
            txtBytesToSearch.setText(lastString);
            txtBytesToSearch.requestFocus();
        }
        super.setVisible(arg0);
    }

}
