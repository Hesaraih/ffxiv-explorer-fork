package com.fragmenterworks.ffxivextract.gui.components;

import com.fragmenterworks.ffxivextract.Strings;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.Objects;

public class Path_to_Hash_Window extends JFrame {

    private final JTextField edtFullPath;
    private final JTextArea txtOutput;
    /**
     * ファイルパスの照合結果状態 2:ファイルがある 1:パスは合ってる 0:ファイル名もパスもなし
     */
    private Integer pathCheck = 0;

    SqPack_IndexFile currentIndex;

    public Path_to_Hash_Window(SqPack_IndexFile currentIndex) {

        this.currentIndex = currentIndex;

        setTitle(Strings.PATHTOHASH_TITLE + " (" + currentIndex.getName() + ")");
        URL imageURL = getClass().getResource("/frameicon.png");
        ImageIcon image = new ImageIcon(Objects.requireNonNull(imageURL));
        this.setIconImage(image.getImage());
        setBounds(100, 100, 510, 220);
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        JPanel panel_1 = new JPanel();
        contentPane.add(panel_1, BorderLayout.CENTER);
        panel_1.setBorder(new EmptyBorder(5, 0, 0, 0));
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));

        JScrollPane scrollPane = new JScrollPane();
        panel_1.add(scrollPane);

        txtOutput = new JTextArea(Strings.PATHTOHASH_INTRO);
        txtOutput.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 12));
        scrollPane.setViewportView(txtOutput);
        txtOutput.setRows(2);
        txtOutput.setEditable(false);

        JPanel panel_2 = new JPanel();
        panel_2.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.add(panel_2, BorderLayout.NORTH);
        panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        panel_2.add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        JLabel lblNewLabel = new JLabel(Strings.PATHTOHASH_PATH);
        panel.add(lblNewLabel);

        edtFullPath = new JTextField();
        panel.add(edtFullPath);
        edtFullPath.setColumns(10);

        JPanel panel_3 = new JPanel();
        contentPane.add(panel_3, BorderLayout.SOUTH);

        JButton btnCalculate = new JButton(Strings.PATHTOHASH_BUTTON_HASHTHIS);
        panel_3.add(btnCalculate);

        edtFullPath.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                calcHash();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                calcHash();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                calcHash();
            }
        });

        btnCalculate.addActionListener(e -> commit());

        JButton btnClose = new JButton(Strings.PATHTOHASH_BUTTON_CLOSE);
        panel_3.add(btnClose);
        btnClose.addActionListener(arg0 -> Path_to_Hash_Window.this.dispose());
    }

    private void commit() {
        //計算ボタンを押したときの動作
        String path = edtFullPath.getText();



        HashDatabase.beginConnection();
        try {
            HashDatabase.setAutoCommit(false);
        } catch (SQLException e1) {
            Utils.getGlobalLogger().error(e1);
        }
        boolean result = false;
        if (pathCheck == 2){
            //ファイルパスとファイル名を強制的に追加
            result = HashDatabase.addPathToDB(edtFullPath.getText(), currentIndex.getName(), HashDatabase.globalConnection,true);
        }else if (pathCheck == 1){
            //ファイルパスのみ追加
            result = HashDatabase.addFolderToDB(edtFullPath.getText(), currentIndex.getName());
        }

        if (result) {
            JOptionPane.showMessageDialog(this,
                    "データベースにパスを追加しました。",
                    "HashListデータベース",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        else {
            JOptionPane.showMessageDialog(this,
                    "データベースにパスを追加できませんでした。",
                    "HashListデータベース",
                    JOptionPane.ERROR_MESSAGE);
        }

        try {
            HashDatabase.commit();
        } catch (SQLException e) {
            Utils.getGlobalLogger().error(e);
        }
        HashDatabase.closeConnection();
    }

    /**
     * ハッシュ値計算とHashListDB登録
     */
    private void calcHash() {
        //パスのテキストボックスの文字列を変更したときの動作
        String path = edtFullPath.getText();

        if (!path.contains("/"))
        {
            txtOutput.setText(Strings.PATHTOHASH_ERROR_INVALID);
            return;
        }

        path = path.trim();

        String folder = path.substring(0, path.lastIndexOf('/'));
        String folder_L = folder.toLowerCase();
        String filename = path.substring(path.lastIndexOf('/') + 1);
        String filename_L = filename.toLowerCase();

        int folderHash = HashDatabase.computeCRC(folder_L.getBytes(), 0, folder_L.getBytes().length);
        int fileHash = HashDatabase.computeCRC(filename_L.getBytes(), 0, filename_L.getBytes().length);
        int fullHash = HashDatabase.computeCRC(path.getBytes(), 0, path.getBytes().length);

        String base = "";
        Border border = BorderFactory.createLineBorder(Color.RED, 2);

        try {
            pathCheck = currentIndex.existsFile2(path);
            if (pathCheck == 2){
                HashDatabase.addPathToDB(edtFullPath.getText(), currentIndex.getName());
                border = BorderFactory.createLineBorder(Color.GREEN, 2);
            }else if (pathCheck == 1){
                border = BorderFactory.createLineBorder(Color.ORANGE, 2);
            }
//            if (currentIndex.extractFile(path) != null) {
//                HashDatabase.addPathToDB(edtFullPath.getText(), currentIndex.getName());
//                border = BorderFactory.createLineBorder(Color.GREEN, 2);
//            }
        } catch (Exception e) {
            Utils.getGlobalLogger().error(e);
        }

        txtOutput.setBorder(border);

        txtOutput.setText(base +
                Strings.PATHTOHASH_FOLDER_HASH + String.format("0x%08X (%11d)", folderHash, folderHash) + "\n"+
                Strings.PATHTOHASH_FILE_HASH + String.format("0x%08X (%11d)", fileHash, fileHash) + "\n" +
                Strings.PATHTOHASH_FULL_HASH + String.format("0x%08X (%11d)", fullHash, fullHash));
//                Strings.PATHTOHASH_FOLDER_HASH + String.format("0x%08X (%s)", folderHash, Long.toString(folderHash & 0xFFFFFFFFL)) + "\n"+
//                Strings.PATHTOHASH_FILE_HASH + String.format("0x%08X (%s)", fileHash, Long.toString(fileHash & 0xFFFFFFFFL)) + "\n" +
//                Strings.PATHTOHASH_FULL_HASH + String.format("0x%08X (%s)", fullHash, Long.toString(fullHash & 0xFFFFFFFFL)));
    }

}
