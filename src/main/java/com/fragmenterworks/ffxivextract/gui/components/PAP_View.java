package com.fragmenterworks.ffxivextract.gui.components;

import com.fragmenterworks.ffxivextract.models.PAP_File;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class PAP_View extends JPanel {

    private final PAP_File currentPAP;

    public PAP_View(PAP_File file) {
        setBorder(new TitledBorder(null, "アニメーション", TitledBorder.LEADING, TitledBorder.TOP, null, null));

        currentPAP = file;

        setLayout(new BorderLayout(0, 0));

        JScrollPane scrollPane = new JScrollPane();
        add(scrollPane, BorderLayout.CENTER);

        JList<String> lstAnimationNames = new JList<>();
        scrollPane.setViewportView(lstAnimationNames);
        lstAnimationNames.setModel(new AbstractListModel<String>() {

            public int getSize() {
                return currentPAP.getAnimationNames().length;
            }

            public String getElementAt(int index) {
                return currentPAP.getAnimationNames()[index];
            }
        });


    }

}
