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

        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("マテリアルセット数： " + currentIMC.getCount());
        model.addElement(""); //改行

        for (int index = 0 ; index < currentIMC.getPartsSize() ; index++){
            int variantID = 0;
            for (IMC_File.ImcVariant imcVariant :currentIMC.getVariantsList(index)){

                model.addElement(String.format("    %02d= マテリアルセット:v%04d, マテリアルセット:v%04d, Hidden Parts:0x%x, VFX ID: %d, VFX ID2: %d", variantID, imcVariant.Variant, imcVariant.SubVariant, imcVariant.PartVisibilityMask, imcVariant.VFX_id1, imcVariant.VFX_id2));
                variantID++;
            }
            model.addElement(""); //改行

        }

        lstAnimationNames.setModel(model);

    }

}
