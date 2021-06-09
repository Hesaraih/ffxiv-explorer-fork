package com.fragmenterworks.ffxivextract.gui.components;

import com.fragmenterworks.ffxivextract.helpers.ImageDecoding.ImageDecodingException;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.models.Texture_File;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Image_View extends JPanel {

    private final Texture_File currentTexture;
    private final NavigableImagePanel imgPreviewCanvas;

    public Image_View(Texture_File texture) {
        currentTexture = texture;
        setLayout(new BorderLayout(0, 0));

        JPanel pnlTexInfo = new JPanel();
        add(pnlTexInfo, BorderLayout.NORTH);
        pnlTexInfo.setBorder(new TitledBorder(null, "テクスチャ情報", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        pnlTexInfo.setLayout(new BorderLayout(0, 0));

        JScrollPane scrollPane_1 = new JScrollPane();
        pnlTexInfo.add(scrollPane_1, BorderLayout.CENTER);

        JList<String> list = new JList<>();
        list.setAutoscrolls(false);
        list.setEnabled(false);

        final String[] values = new String[]{"圧縮タイプ: " + texture.getCompressionTypeString(), "幅: " + texture.uncompressedWidth, "高さ: " + texture.uncompressedHeight, "MipMaps番号: " + texture.numMipMaps};

        list.setModel(new AbstractListModel<String>() {

            public int getSize() {
                return values.length;
            }

            public String getElementAt(int index) {
                return values[index];
            }
        });
        scrollPane_1.setViewportView(list);

        JPanel pnlTexPreview = new JPanel();
        pnlTexPreview.setBorder(new TitledBorder(null, "プレビュー", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        add(pnlTexPreview, BorderLayout.CENTER);
        pnlTexPreview.setLayout(new BorderLayout(0, 0));

        JScrollPane scrollPane = new JScrollPane();
        pnlTexPreview.add(scrollPane);
        imgPreviewCanvas = new NavigableImagePanel();
        scrollPane.setViewportView(imgPreviewCanvas);

        setImage(0);
    }

    @SuppressWarnings("SameParameterValue")
    private void setImage(int index) {
        try {
            BufferedImage preview = currentTexture.decode(index, null);
            imgPreviewCanvas.setImage(preview);
            if (currentTexture.compressionType == 0x2460) {
                imgPreviewCanvas.setHighQualityRenderingEnabled(false);
            }
        } catch (ImageDecodingException e) {
            Utils.getGlobalLogger().error(e);
        }
    }

}
