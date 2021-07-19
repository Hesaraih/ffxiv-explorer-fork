package com.fragmenterworks.ffxivextract.gui.outfitter;

import com.fragmenterworks.ffxivextract.Strings;
import com.fragmenterworks.ffxivextract.gui.components.EXDF_View;
import com.fragmenterworks.ffxivextract.gui.modelviewer.Loading_Dialog;
import com.fragmenterworks.ffxivextract.models.EXHF_File;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile;

import javax.swing.*;
import java.net.URL;
import java.util.Objects;

public class OutfitterWindow extends JFrame {

    private final JFrame parent;
    private Loading_Dialog dialog;
    private final String sqPackPath;
    private SqPack_IndexFile exdIndexFile;
    private SqPack_IndexFile modelIndexFile;

    public OutfitterWindow(JFrame parent, String sqPackPath) {

        this.setTitle(Strings.DIALOG_TITLE_OUTFITTER);
        URL imageURL = getClass().getResource("/frameicon.png");
        ImageIcon image = new ImageIcon(Objects.requireNonNull(imageURL));
        this.setIconImage(image.getImage());
        setSize(800, 600);

        this.parent = parent;
        this.sqPackPath = sqPackPath;

    }

    private String getSqpackPath() {
        return sqPackPath;
    }

    public void beginLoad() {
        JOptionPane.showMessageDialog(this,
                Strings.MSG_OUTFITTER,
                Strings.MSG_OUTFITTER_TITLE,
                JOptionPane.INFORMATION_MESSAGE);

        OpenIndexTask task = new OpenIndexTask();
        dialog = new Loading_Dialog(OutfitterWindow.this, 3);
        task.execute();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    class OpenIndexTask extends SwingWorker<Void, Void> {

        OpenIndexTask() {

        }

        @Override
        protected Void doInBackground() {


            dialog.nextFile(0, "..\\game\\sqpack\\ffxiv\\0a0000.win32.index");
            exdIndexFile = SqPack_IndexFile.GetIndexFileForArchiveID("0a0000", true);
            dialog.nextFile(1, "..\\game\\sqpack\\ffxiv\\040000.win32.index");
            modelIndexFile = SqPack_IndexFile.GetIndexFileForArchiveID("040000", true);
            dialog.nextFile(2, "Loading initial models...");
            EXHF_File exhfFile = new EXHF_File(exdIndexFile.extractFile("exd/item.exh"));
            EXDF_View itemView = new EXDF_View(exdIndexFile, "exd/item.exh", exhfFile);
            getContentPane().add(new Outfitter(modelIndexFile, itemView));
            return null;
        }

        @Override
        protected void done() {
            dialog.dispose();

            OutfitterWindow.this.setLocationRelativeTo(parent);
            OutfitterWindow.this.setVisible(true);
        }
    }

}
