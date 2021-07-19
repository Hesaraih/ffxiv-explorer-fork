package com.fragmenterworks.ffxivextract.gui.modelviewer;

import com.fragmenterworks.ffxivextract.gui.components.EXDF_View;
import com.fragmenterworks.ffxivextract.gui.components.ModelRenderer;
import com.fragmenterworks.ffxivextract.gui.components.OpenGL_View;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.models.EXHF_File;
import com.fragmenterworks.ffxivextract.models.Model;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

class ModelViewerFurniture extends JPanel {

    private static final int INDEX_ITEM_NAME = 10;
    private static final int INDEX_HOUSINGFURNITURE_ITEMID = 7;
    private static final int INDEX_HOUSINGFURNITURE_TYPEID = 2;
    private static final int INDEX_HOUSINGFURNITURE_MODELNUMBER = 1;
    private static final int INDEX_FURNITURETYPE_NAME = 1;

    private final ModelViewerWindow parent;

    private final ArrayList<ModelFurnitureEntry> entries = new ArrayList<>();

    @SuppressWarnings("unused")
    OpenGL_View view3D;
    private final JList<String> lstFurniture;

    private final JLabel txtPath;
    private final JCheckBox chkGlowToggle;

    private final ModelRenderer renderer;

    private boolean leftMouseDown = false;
    private boolean rightMouseDown = false;

    @SuppressWarnings("unused")
    private final int currentLoD = 0;
    private int lastOriginX, lastOriginY;
    private int lastX, lastY;

    private final SqPack_IndexFile modelIndexFile;

    public ModelViewerFurniture(ModelViewerWindow parent, SqPack_IndexFile modelIndex) {

        this.parent = parent;
        this.modelIndexFile = modelIndex;

        setLayout(new BorderLayout(0, 0));

        JPanel panel_1 = new JPanel();
        add(panel_1, BorderLayout.CENTER);
        panel_1.setLayout(new BorderLayout(0, 0));

        JPanel panel_2 = new JPanel();
        panel_2.setBorder(new TitledBorder(null, "情報", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel_1.add(panel_2, BorderLayout.NORTH);
        panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.Y_AXIS));

        JPanel panelInfo_1 = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panelInfo_1.getLayout();
        flowLayout.setVgap(1);
        flowLayout.setAlignment(FlowLayout.LEFT);
        panel_2.add(panelInfo_1);

        JLabel lblPath = new JLabel("パス: ");
        panelInfo_1.add(lblPath);

        txtPath = new JLabel("-");
        panelInfo_1.add(txtPath);

        JPanel panelInfo_3 = new JPanel();
        FlowLayout flowLayout_2 = (FlowLayout) panelInfo_3.getLayout();
        flowLayout_2.setAlignment(FlowLayout.LEFT);
        panel_2.add(panelInfo_3);

        JButton btnResetCamera = new JButton("カメラをリセット");
        panelInfo_3.add(btnResetCamera);

        chkGlowToggle = new JCheckBox("Glow(発光)シェーダ", true);
        panelInfo_3.add(chkGlowToggle);

        chkGlowToggle.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                renderer.toggleGlow(chkGlowToggle.isSelected());
            }
        });

        btnResetCamera.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                renderer.resetCamera();
            }
        });

        JPanel panel_3 = new JPanel();
        panel_1.add(panel_3, BorderLayout.CENTER);
        panel_3.setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        add(panel, BorderLayout.WEST);
        panel.setLayout(new BorderLayout(0, 0));

        JScrollPane scrollPane = new JScrollPane();
        panel.add(scrollPane, BorderLayout.WEST);

        lstFurniture = new JList<>();

        scrollPane.setViewportView(lstFurniture);

        GLProfile glProfile = GLProfile.getDefault();
        GLCapabilities glcapabilities = new GLCapabilities(glProfile);
        final GLCanvas glcanvas = new GLCanvas(glcapabilities);
        renderer = new ModelRenderer();
        glcanvas.addGLEventListener(renderer);
        FPSAnimator animator = new FPSAnimator(glcanvas, 30);
        animator.start();
        glcanvas.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseMoved(MouseEvent e) {

            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (leftMouseDown) {
                    renderer.pan((e.getX() - lastX), (e.getY() - lastY));
                    lastX = e.getX();
                    lastY = e.getY();
                }
                if (rightMouseDown) {
                    renderer.rotate(e.getX() - lastX, e.getY() - lastY);
                    lastX = e.getX();
                    lastY = e.getY();
                }
            }
        });
        glcanvas.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    leftMouseDown = false;
                }
                if (e.getButton() == MouseEvent.BUTTON3) {
                    rightMouseDown = false;
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    leftMouseDown = true;
                    lastOriginX = e.getX();
                    lastOriginY = e.getY();
                    lastX = lastOriginX;
                    lastY = lastOriginY;
                }
                if (e.getButton() == MouseEvent.BUTTON3) {
                    rightMouseDown = true;
                    lastOriginX = e.getX();
                    lastOriginY = e.getY();
                    lastX = lastOriginX;
                    lastY = lastOriginY;
                }
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
            }

            @Override
            public void mouseClicked(MouseEvent arg0) {
            }
        });
        addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            renderer.zoom(-notches);
        });

        try {
            if (!loadFurniture()) {
                removeAll();
                JLabel errorLabel = new JLabel("家具リストの読み込み中にエラーが発生しました。");
                add(errorLabel);
                return;
            }
        } catch (IOException e1) {
            Utils.getGlobalLogger().error(e1);
        }

        lstFurniture.getSelectionModel().addListSelectionListener(event -> {

            if (event.getValueIsAdjusting() || lstFurniture.getModel().getSize() == 0) {
                return;
            }

            int selected = lstFurniture.getSelectedIndex();

            if (selected == -1) {
                return;
            }

            String modelPath = null;

            if (entries.get(selected).modelType == ModelFurnitureEntry.TYPE_FURNITURE) {
                modelPath = String.format("bgcommon/hou/indoor/general/%04d/bgparts/fun_b0_m%04d.mdl", entries.get(selected).model, entries.get(selected).model);
            } else if (entries.get(selected).modelType == ModelFurnitureEntry.TYPE_YARDOBJECT) {
                modelPath = String.format("bgcommon/hou/outdoor/general/%04d/bgparts/gar_b0_m%04d.mdl", entries.get(selected).model, entries.get(selected).model);
            }

            byte[] modelData = modelIndexFile.extractFile(Objects.requireNonNull(modelPath));


            if (modelData != null) {
                HashDatabase.addPathToDB(modelPath, "040000");
                Model model = new Model(modelPath, modelIndexFile, modelData, modelIndex.getEndian());
                renderer.setModel(model);
            }

            txtPath.setText(modelPath);
        });

        panel_3.add(glcanvas, BorderLayout.CENTER);

    }

    private boolean loadFurniture() throws IOException {
        SqPack_IndexFile indexFile = parent.getExdIndexFile();
        EXHF_File exhfFileHousingFurniture = new EXHF_File(indexFile.extractFile("exd/HousingFurniture.exh"));
        EXHF_File exhfFileHousingYardObject = new EXHF_File(indexFile.extractFile("exd/HousingYardObject.exh"));
        EXHF_File exhfFileItem = new EXHF_File(indexFile.extractFile("exd/Item.exh"));
        EXHF_File exhfFileYardCatalogItemList = new EXHF_File(indexFile.extractFile("exd/YardCatalogItemList.exh"));
        //EXHF_File exhfFileHousingCategory = new EXHF_File(indexFile.extractFile("exd/HousingItemCategory.exh")); //廃止されてる？
        //代替先候補 こっちかも
        //EXHF_File exhfFileYardCatalogCategory = new EXHF_File(indexFile.extractFile("exd/YardCatalogCategory.exh"));

        EXDF_View view1 = new EXDF_View(indexFile, "exd/HousingFurniture.exh", exhfFileHousingFurniture);
        EXDF_View view2 = new EXDF_View(indexFile, "exd/Item.exh", exhfFileItem);
        EXDF_View view3 = new EXDF_View(indexFile, "exd/YardCatalogItemList.exh", exhfFileYardCatalogItemList);
        //EXDF_View view3 = new EXDF_View(indexFile, "exd/HousingItemCategory.exh", exhfFileHousingCategory); //廃止されてる？
        //代替先候補 こっちかも
        //EXDF_View view3 = new EXDF_View(indexFile, "exd/YardCatalogCategory.exh", exhfFileYardCatalogCategory);
        EXDF_View view4 = new EXDF_View(indexFile, "exd/HousingYardObject.exh", exhfFileHousingYardObject);

        try {
            for (int i = 0; i < view1.getTable().getRowCount(); i++) {

                long itemId = (Long) view1.getTable().getValueAt(i, INDEX_HOUSINGFURNITURE_ITEMID);
                int modelNumber = (Integer) view1.getTable().getValueAt(i, INDEX_HOUSINGFURNITURE_MODELNUMBER);
                int furnitureType = (Integer) view1.getTable().getValueAt(i, INDEX_HOUSINGFURNITURE_TYPEID);

                String name = (String) view2.getTable().getValueAt((int) itemId, INDEX_ITEM_NAME);

                if (itemId == 0) {
                    name = "不明";
                }

                if (name.isEmpty()) {
                    name = "空欄?";
                }

                if (modelNumber == 0) {
                    continue;
                }

                String furnitureTypeName = (String) view3.getTable().getValueAt(furnitureType, INDEX_FURNITURETYPE_NAME);

                entries.add(new ModelFurnitureEntry(ModelFurnitureEntry.TYPE_FURNITURE, i, name, modelNumber, furnitureTypeName));
            }
            for (int i = 0; i < view4.getTable().getRowCount(); i++) {

                long itemId = (Long) view4.getTable().getValueAt(i, INDEX_HOUSINGFURNITURE_ITEMID);
                int modelNumber = (Integer) view4.getTable().getValueAt(i, INDEX_HOUSINGFURNITURE_MODELNUMBER);
                int furnitureType = (Integer) view4.getTable().getValueAt(i, INDEX_HOUSINGFURNITURE_TYPEID);

                String name = (String) view2.getTable().getValueAt((int) itemId, INDEX_ITEM_NAME);

                if (itemId == 0) {
                    name = "不明";
                }

                if (name.isEmpty()) {
                    name = "空欄?";
                }

                if (modelNumber == 0) {
                    continue;
                }

                String furnitureTypeName = (String) view3.getTable().getValueAt(furnitureType, INDEX_FURNITURETYPE_NAME);

                entries.add(new ModelFurnitureEntry(ModelFurnitureEntry.TYPE_YARDOBJECT, i, name, modelNumber, furnitureTypeName));
            }
        } catch (Exception e) {
            //Utils.getGlobalLogger().error(e);
            return false;
        }

        lstFurniture.setModel(new AbstractListModel<String>() {
            public int getSize() {
                return entries.size();
            }

            public String getElementAt(int index) {
                if (entries.get(index).type.isEmpty()) {
                    return entries.get(index).name + "";
                }
                return entries.get(index).name + "(" + entries.get(index).type + ")";
            }
        });

        return true;
    }

}
