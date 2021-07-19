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
import java.util.Objects;

public class Path_to_Hash_Window extends JFrame {

    private final JTextField edtFullPath;
    private final JTextArea txtOutput;
    private final JButton btnCalculate;
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
        //txtOutput.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 12)); //<-これ使うと開くのが遅くなる
        txtOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
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

        btnCalculate = new JButton(Strings.PATHTOHASH_BUTTON_HASHTHIS);
        panel_3.add(btnCalculate);

        edtFullPath.getDocument().addDocumentListener(new DocumentListener() {
            //テキストボックスの内容変更時の動作
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
        String archive = HashDatabase.getArchiveID(path);

        int result = 0;

        if (archive.equals("*")){
            return;
        }

        if (pathCheck == 2){
            //ファイルパスとファイル名を強制的に追加
            result = HashDatabase.addPathToDB(path, archive);
        }else if (pathCheck == 1){
            //ファイルパスのみ追加
            String folder = path.substring(0, path.lastIndexOf('/'));
            result = HashDatabase.addFolderToDB(folder, archive,true);
        }

        if (result == 1) {
            JOptionPane.showMessageDialog(this,
                    "データベースにパスを追加しました。",
                    "HashListデータベース",
                    JOptionPane.INFORMATION_MESSAGE);
        }else if(result == 2)  {
            JOptionPane.showMessageDialog(this,
                    "データベースのファイル名を変更しました。",
                    "HashListデータベース",
                    JOptionPane.INFORMATION_MESSAGE);
        }else if(result == 3)  {
            JOptionPane.showMessageDialog(this,
                    "データベースのファイルパスを変更しました。",
                    "HashListデータベース",
                    JOptionPane.INFORMATION_MESSAGE);
        }else if(result == 4)  {
            JOptionPane.showMessageDialog(this,
                    "パスはすでに登録済みです。",
                    "HashListデータベース",
                    JOptionPane.INFORMATION_MESSAGE);
        }else if(result == 0)  {
            JOptionPane.showMessageDialog(this,
                    "データベースにパスを追加できませんでした。",
                    "HashListデータベース",
                    JOptionPane.ERROR_MESSAGE);
        }

    }

    /**
     * ハッシュ値計算とHashListDB登録
     */
    private void calcHash() {
        //パスのテキストボックスの文字列を変更したときの動作
        String path = edtFullPath.getText();

        if (path == null || !path.contains("/"))
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
        btnCalculate.setEnabled(false); //強制登録用のボタン無効化

        try {
            String archive = HashDatabase.getArchiveID(path);
            SqPack_IndexFile temp_IndexFile = currentIndex;
            if (!currentIndex.getName().equals(archive) && !archive.equals("*")) {
                //↑自由に入力できるためarchiveに存在しないフォルダを指定しエラーが発生する場合があるので"*"禁止
                temp_IndexFile = SqPack_IndexFile.GetIndexFileForArchiveID(archive, false);
            }

            pathCheck = temp_IndexFile.findFile(path);
            if (pathCheck == 2){
                HashDatabase.addPathToDB(edtFullPath.getText(), archive);
                border = BorderFactory.createLineBorder(Color.GREEN, 2);
                btnCalculate.setEnabled(true); //強制登録用のボタン有効化
            }else if (pathCheck == 1){
                border = BorderFactory.createLineBorder(Color.ORANGE, 2);
                btnCalculate.setEnabled(true); //強制登録用のボタン有効化
            }

        } catch (Exception e) {
            Utils.getGlobalLogger().error(e);
        }

        txtOutput.setBorder(border);

        txtOutput.setText(base +
                Strings.PATHTOHASH_FOLDER_HASH + String.format("0x%08X (%11d)", folderHash, folderHash) + "\n"+
                Strings.PATHTOHASH_FILE_HASH + String.format("0x%08X (%11d)", fileHash, fileHash) + "\n" +
                Strings.PATHTOHASH_FULL_HASH + String.format("0x%08X (%11d)", fullHash, fullHash));
    }

}
