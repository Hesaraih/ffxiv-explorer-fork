package com.fragmenterworks.ffxivextract.gui;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.Strings;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.prefs.Preferences;
import javax.swing.filechooser.*;

class SettingsWindow extends JDialog {
    private final JTextField txtDatPath;

    public SettingsWindow(JFrame parent) {
        super(parent, ModalityType.APPLICATION_MODAL);
        this.setTitle(Strings.DIALOG_TITLE_SETTINGS);
        URL imageURL = getClass().getResource("/frameicon.png");
        ImageIcon image = new ImageIcon(imageURL);
        this.setIconImage(image.getImage());

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "一般設定", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        getContentPane().add(panel, BorderLayout.NORTH);
        GridBagLayout gbl_panel = new GridBagLayout();
        gbl_panel.rowHeights = new int[]{23, 0, 0};
        gbl_panel.columnWeights = new double[]{1.0};
        gbl_panel.rowWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
        panel.setLayout(gbl_panel);

        JPanel panel_4 = new JPanel();
        GridBagConstraints gbc_panel_4 = new GridBagConstraints();
        gbc_panel_4.insets = new Insets(0, 0, 5, 5);
        gbc_panel_4.fill = GridBagConstraints.BOTH;
        gbc_panel_4.gridx = 0;
        gbc_panel_4.gridy = 0;
        panel.add(panel_4, gbc_panel_4);
        panel_4.setLayout(new BoxLayout(panel_4, BoxLayout.LINE_AXIS));

        JLabel lblNewLabel = new JLabel("FFXIVのルートパス");
        panel_4.add(lblNewLabel);

        JPanel panel_3 = new JPanel();
        panel_4.add(panel_3);

        txtDatPath = new JTextField();
        panel_3.add(txtDatPath);
        txtDatPath.setText(Constants.datPath);
        txtDatPath.setPreferredSize(new Dimension(500, 20));

        JButton btnBrowse = new JButton(Strings.BUTTONNAMES_BROWSE);
        panel_3.add(btnBrowse);

        btnBrowse.addActionListener(arg0 -> setPath());

        JPanel panel_1 = new JPanel();
        getContentPane().add(panel_1, BorderLayout.SOUTH);

        JButton btnSave = new JButton("保存");
        btnSave.addActionListener(e -> saveSettings());

        panel_1.add(btnSave);

        JButton btnCancel = new JButton("キャンセル");
        btnCancel.addActionListener(e -> SettingsWindow.this.dispose());

        panel_1.add(btnCancel);

        @SuppressWarnings("unused")
        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();

        pack();
    }

    private void setPath() {
        //デフォルトの場所を指定しておく
        File folder = new File("C:\\Program Files (x86)\\SquareEnix\\FINAL FANTASY XIV - A Realm Reborn\\game");
        JFileChooser fileChooser;
        if (folder.exists()) {
            fileChooser = new JFileChooser(folder);
        }else{
            fileChooser = new JFileChooser();
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter("ffxivgame.verファイル", "ver");

        //fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        int retunval = fileChooser.showOpenDialog(SettingsWindow.this);

        if (retunval == JFileChooser.APPROVE_OPTION) {
            try {
                //gameフォルダの親フォルダ(FINAL FANTASY XIV - A Realm Reborn)を取得するらしいが明示されていないので変更
                String parentPath = fileChooser.getCurrentDirectory().getCanonicalPath();
                txtDatPath.setText(parentPath.substring(0, parentPath.lastIndexOf('/')));
                //txtDatPath.setText(fileChooser.getSelectedFile().getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveSettings() {
        Preferences prefs = Preferences
                .userNodeForPackage(com.fragmenterworks.ffxivextract.Main.class);
        prefs.put(Constants.PREF_DAT_PATH, txtDatPath.getText());
        Constants.datPath = txtDatPath.getText();
        dispose();
    }
}
