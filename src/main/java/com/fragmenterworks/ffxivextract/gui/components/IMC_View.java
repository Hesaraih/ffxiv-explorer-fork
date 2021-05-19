package com.fragmenterworks.ffxivextract.gui.components;

import com.fragmenterworks.ffxivextract.models.IMC_File;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class IMC_View extends JPanel {

    private final IMC_File currentIMC;

    public IMC_View(IMC_File file) {
        setBorder(new TitledBorder(null, "モデル情報ファイル", TitledBorder.LEADING, TitledBorder.TOP, null, null));

        currentIMC = file;

        setLayout(new BorderLayout(0, 0));

        JScrollPane scrollPane = new JScrollPane();
        add(scrollPane, BorderLayout.CENTER);

        JList<String> lstAnimationNames = new JList<>();
        scrollPane.setViewportView(lstAnimationNames);
        lstAnimationNames.setModel(new AbstractListModel<String>() {

            public int getSize() {
                return currentIMC.getNumVariances();
            }

            public String getElementAt(int index) {
                return currentIMC.getVariantsList(index).toString();
            }
        });


    }

}
