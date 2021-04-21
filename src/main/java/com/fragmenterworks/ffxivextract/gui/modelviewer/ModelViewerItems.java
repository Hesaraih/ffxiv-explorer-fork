package com.fragmenterworks.ffxivextract.gui.modelviewer;

import com.fragmenterworks.ffxivextract.gui.components.EXDF_View;
import com.fragmenterworks.ffxivextract.gui.components.ModelRenderer;
import com.fragmenterworks.ffxivextract.gui.components.OpenGL_View;
import com.fragmenterworks.ffxivextract.helpers.EXDDef;
import com.fragmenterworks.ffxivextract.helpers.SparseArray;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.models.Model;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

class ModelViewerItems extends JPanel {

    private final ArrayList<ModelItemEntry>[] entries = new ArrayList[22];

    private final ArrayList<ModelItemEntry> filteredEntries = new ArrayList<>();

    @SuppressWarnings("unused")
    ModelItemEntry[] addedItems = new ModelItemEntry[22];

    private final SparseArray<String> slots = new SparseArray<>();

    private final SparseArray<String> charIds = new SparseArray<>();

    private int currentBody = 1;
    private int currentCategory = 0;

    @SuppressWarnings("unused")
    OpenGL_View view3D;
    private final JList<String> lstItems;
    private final JComboBox<String> cmbBodyStyle;
    private final JComboBox<String> cmbCategory;

    //Info Section
    private final JLabel txtPath;
    private final JLabel txtModelInfo;
    @SuppressWarnings("unused")
    JButton btnColorSet;
    private final JCheckBox chkGlowToggle;
    private final JTextField edtSearch;

    private final ModelRenderer renderer;

    private boolean leftMouseDown = false;
    private boolean rightMouseDown = false;

    @SuppressWarnings("unused")
    private final int currentLoD = 0;
    private int lastOriginX, lastOriginY;
    private int lastX, lastY;

    private final SqPack_IndexFile modelIndexFile;
    private final EXDF_View itemView;

    @SuppressWarnings("unused")
    public ModelViewerItems(ModelViewerWindow parent, SqPack_IndexFile modelIndex, EXDF_View itemView) {

        this.modelIndexFile = modelIndex;
        this.itemView = itemView;

        //Fill the Equipment Slots
        slots.append(1, "片手武器");
        slots.append(13, "両手武器");
        slots.append(2, "素手");
        slots.append(3, "頭");
        slots.append(4, "胴");
        slots.append(5, "手");
        slots.append(7, "脚");
        slots.append(8, "足");
        slots.append(9, "耳");
        slots.append(10, "首");
        slots.append(11, "腰");
        slots.append(12, "指輪");

        slots.append(15, "胴 + 頭");
        slots.append(16, "全身 - 頭");
        //slots.append(17, "SoulStone");
        slots.append(18, "脚 + 足");
        slots.append(19, "全身");
        slots.append(20, "胴 + 手");
        slots.append(21, "胴 + 脚 + 足");

        slots.append(0, "食べ物");

        //Fill the char ids
        charIds.append(1, "ミッドランダー♂");
        charIds.append(2, "ミッドランダー♀");
        charIds.append(3, "ハイランダー♂");
        charIds.append(4, "ハイランダー♀");
        charIds.append(5, "エレゼン♂");
        charIds.append(6, "エレゼン♀");
        charIds.append(7, "ミコッテ♂");
        charIds.append(8, "ミコッテ♀");
        charIds.append(9, "ルガディン♂");
        charIds.append(10, "ルガディン♀");
        charIds.append(11, "ララフェル♂");
        charIds.append(12, "ララフェル♀");
        charIds.append(13, "アウラ♂");
        charIds.append(14, "アウラ♀");
        charIds.append(15, "ロスガル♂");
        //charIds.append(16, "ロスガル♀");
        //charIds.append(17, "ヴィエラ♂");
        charIds.append(18, "ヴィエラ♀");

        Arrays.fill(entries, new ArrayList<ModelItemEntry>());

        setLayout(new BorderLayout(0, 0));

        JPanel panel_1 = new JPanel();
        add(panel_1, BorderLayout.CENTER);
        panel_1.setLayout(new BorderLayout(0, 0));

        JPanel panel_2 = new JPanel();
        panel_1.add(panel_2, BorderLayout.NORTH);
        panel_2.setBorder(new TitledBorder(null, "情報", TitledBorder.LEADING, TitledBorder.TOP, null, null));
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

        JPanel panelInfo_2 = new JPanel();
        FlowLayout flowLayout_1 = (FlowLayout) panelInfo_2.getLayout();
        flowLayout_1.setVgap(1);
        flowLayout_1.setAlignment(FlowLayout.LEFT);
        panel_2.add(panelInfo_2);

        JLabel label = new JLabel("情報: ");
        panelInfo_2.add(label);

        txtModelInfo = new JLabel("タイプ : -, ID: -, モデル: -, Variant: -");
        panelInfo_2.add(txtModelInfo);

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
        panel.setBorder(new TitledBorder(null, "アイテム", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        add(panel, BorderLayout.WEST);
        panel.setLayout(new BorderLayout(0, 0));

        JPanel panel_4 = new JPanel();
        panel.add(panel_4, BorderLayout.NORTH);
        panel_4.setLayout(new BoxLayout(panel_4, BoxLayout.Y_AXIS));

        JPanel panel_5 = new JPanel();
        panel_5.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel_5.setBorder(new EmptyBorder(2, 1, 0, 1));
        panel_4.add(panel_5);
        panel_5.setLayout(new BoxLayout(panel_5, BoxLayout.X_AXIS));

        cmbBodyStyle = new JComboBox<>();
        panel_5.add(cmbBodyStyle);

        cmbBodyStyle.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    int selected = cmbBodyStyle.getSelectedIndex();
                    currentBody = charIds.keyAt(selected);

                    if (currentCategory != -1 && lstItems.getSelectedIndex() != -1) {
                        loadModel(-1, lstItems.getSelectedIndex());
                    }

                }
            }
        });

        JPanel panel_6 = new JPanel();
        panel_6.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel_6.setBorder(new EmptyBorder(2, 1, 2, 1));
        panel_4.add(panel_6);
        panel_6.setLayout(new BoxLayout(panel_6, BoxLayout.X_AXIS));

        cmbCategory = new JComboBox<>();
        panel_6.add(cmbCategory);

        cmbCategory.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    int selected = cmbCategory.getSelectedIndex();

                    currentCategory = slots.keyAt(selected);

                    filteredEntries.clear();
                    filteredEntries.addAll(entries[currentCategory]);
                    ((ItemsListModel) lstItems.getModel()).refresh();
                }
            }
        });

        JPanel panel_7 = new JPanel();
        panel_7.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel_4.add(panel_7);
        panel_7.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JScrollPane scrollPane = new JScrollPane();
        panel.add(scrollPane, BorderLayout.CENTER);

        lstItems = new JList<>();

        scrollPane.setViewportView(lstItems);

        ActionListener searchListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentCategory == -1) {
                    return;
                }

                lstItems.clearSelection();

                String filter = edtSearch.getText();

                filteredEntries.clear();

                for (int i = 0; i < entries[currentCategory].size(); i++) {
                    if (entries[currentCategory].get(i).name.toLowerCase().contains(filter.toLowerCase())) {
                        filteredEntries.add(entries[currentCategory].get(i));
                    }
                }

                ((ItemsListModel) lstItems.getModel()).refresh();
            }
        };

        edtSearch = new JTextField();
        edtSearch.addActionListener(searchListener);
        edtSearch.setColumns(10);

        JButton btnSearch = new JButton("検索");
        btnSearch.addActionListener(searchListener);

        JPanel panel_8 = new JPanel();
        panel.add(panel_8, BorderLayout.SOUTH);
        panel_8.setLayout(new BorderLayout(0, 0));

        panel_8.add(edtSearch, BorderLayout.CENTER);
        panel_8.add(btnSearch, BorderLayout.EAST);

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

        if (!loadItems()) {
            removeAll();
            JLabel errorLabel = new JLabel("アイテムリストの読み込み中にエラーが発生しました。");
            add(errorLabel);
            return;
        }

        for (int i = 0; i < charIds.size(); i++) {
            cmbBodyStyle.addItem(charIds.valueAt(i));
        }

        for (int i = 0; i < slots.size(); i++) {
            cmbCategory.addItem(slots.valueAt(i));
        }

        //Add all the slots        
        lstItems.getSelectionModel().addListSelectionListener(event -> {

            if (event.getValueIsAdjusting() || lstItems.getModel().getSize() == 0 || currentCategory == -1) {
                return;
            }

            int selected = lstItems.getSelectedIndex();

            if (selected == -1) {
                return;
            }

            loadModel(-1, selected);
        });

        panel_3.add(glcanvas, BorderLayout.CENTER);


        ((ItemsListModel) lstItems.getModel()).refresh();
        lstItems.clearSelection();

        filteredEntries.addAll(entries[currentCategory]);

    }

    private void loadModel(int charNumberOverride, int selected) {

        if (selected == -1) {
            return;
        }

        String modelPath = null;
        byte[] modelData = null;
        ModelItemEntry currentItem = filteredEntries.get(selected);

        int characterNumber = ((charNumberOverride == -1 ? currentBody * 100 + 1 : charNumberOverride));

        try {

            switch (currentCategory) {
                case 13:
                case 0:
                case 1:
                case 2:
                    modelPath = String.format("chara/weapon/w%04d/obj/body/b%04d/model/w%04db%04d.mdl", currentItem.id, currentItem.model, currentItem.id, currentItem.model);
                    break;
                case 3:
                    modelPath = String.format("chara/equipment/e%04d/model/c%04de%04d_met.mdl", currentItem.id, characterNumber, currentItem.id);
                    break;
                case 4:
                case 21:
                case 20:
                case 19:
                case 16:
                case 15:
                    modelPath = String.format("chara/equipment/e%04d/model/c%04de%04d_top.mdl", currentItem.id, characterNumber, currentItem.id);
                    break;
                case 5:
                    modelPath = String.format("chara/equipment/e%04d/model/c%04de%04d_glv.mdl", currentItem.id, characterNumber, currentItem.id);
                    break;
                case 7:
                case 18:
                    modelPath = String.format("chara/equipment/e%04d/model/c%04de%04d_dwn.mdl", currentItem.id, characterNumber, currentItem.id);
                    break;
                case 8:
                    modelPath = String.format("chara/equipment/e%04d/model/c%04de%04d_sho.mdl", currentItem.id, characterNumber, currentItem.id);
                    break;
                case 9:
                    modelPath = String.format("chara/accessory/a%04d/model/c%04da%04d_ear.mdl", currentItem.id, characterNumber, currentItem.id);
                    break;
                case 10:
                    modelPath = String.format("chara/accessory/a%04d/model/c%04da%04d_nek.mdl", currentItem.id, characterNumber, currentItem.id);
                    break;
                case 11:
                    modelPath = String.format("chara/accessory/a%04d/model/c%04da%04d_wrs.mdl", currentItem.id, characterNumber, currentItem.id);
                    break;
                case 12:
                    modelPath = String.format("chara/accessory/a%04d/model/c%04da%04d_rir.mdl", currentItem.id, characterNumber, currentItem.id);
                    break;
            }

            modelData = modelIndexFile.extractFile(Objects.requireNonNull(modelPath));
        } catch (IOException e) {
            Utils.getGlobalLogger().error(e);
        }

        if (modelData == null && (characterNumber != 101 && characterNumber != 201)) {
            if (currentBody % 2 == 0) {
                Utils.getGlobalLogger().info("CharID:{}のモデルが検出されなかったため、ヒューラン{}モデルに落とし込みます。", String.format("%04d", characterNumber), "♀");
            } else {
                Utils.getGlobalLogger().info("CharID:{}のモデルが検出されなかったため、ヒューラン{}モデルに落とし込みます。", String.format("%04d", characterNumber), "♂");
            }

            if (currentBody % 2 == 0) {
                loadModel(201, selected);
            } else {
                loadModel(101, selected);
            }


            return;
        }

        if (modelData != null) {
            HashDatabase.addPathToDB(modelPath, "040000");

            Model model = new Model(modelPath, modelIndexFile, modelData, modelIndexFile.getEndian());
            model.loadVariant(currentItem.varient == 0 ? 1 : currentItem.varient);
            renderer.setModel(model);
        }

        txtPath.setText(modelPath);
        txtModelInfo.setText(String.format("ID: %d, モデル: %d, Variant: %d", currentItem.id, currentItem.model, currentItem.varient));

    }

    private boolean loadItems() {
        EXDF_View view = itemView;
        view.setLangOverride(1);
        try {
            for (int i = 0; i < view.getTable().getRowCount(); i++) {
                if (view.getTable().getValueAt(i, 0) instanceof Integer && !view.getTable().getValueAt(i, EXDDef.INDEX_ITEM_MODEL1).equals("0, 0, 0, 0")) {
                    String[] model1Split = ((String) view.getTable().getValueAt(i, EXDDef.INDEX_ITEM_MODEL1)).split(",");
                    @SuppressWarnings("unused")
                    String[] model2Split = ((String) view.getTable().getValueAt(i, EXDDef.INDEX_ITEM_MODEL1)).split(",");

                    int slot = (Integer) view.getTable().getValueAt(i, EXDDef.INDEX_ITEM_SLOT);

                    String name = (String) view.getTable().getValueAt(i, EXDDef.INDEX_ITEM_NAME);
                    int id = Integer.parseInt(model1Split[0].trim());

                    boolean isWeapon = slot == 0 || slot == 1 || slot == 2 || slot == 13;

                    int model;
                    if (!isWeapon) {
                        model = Integer.parseInt(model1Split[2].trim());
                    } else {
                        model = Integer.parseInt(model1Split[1].trim());
                    }
                    int varient;
                    if (!isWeapon) {
                        varient = Integer.parseInt(model1Split[1].trim());
                    } else {
                        varient = Integer.parseInt(model1Split[2].trim());
                    }

                    entries[slot].add(new ModelItemEntry(name, id, model, varient, slot));
                }
            }
        } catch (Exception e) {
            Utils.getGlobalLogger().error(e);
            return false;
        }
        lstItems.setModel(new ItemsListModel());

        return true;
    }

    @SuppressWarnings({"unused", "StatementWithEmptyBody"})
    private int fallback(int characterCode) {
        switch (characterCode) {

        }

        return 101;
    }

    class ItemsListModel extends AbstractListModel<String> {
        public int getSize() {

            if (currentCategory == -1) {
                return 0;
            }

            return filteredEntries.size();
        }

        public String getElementAt(int index) {

            if (currentCategory == -1) {
                return "";
            }

            return filteredEntries.get(index).name;
        }

        void refresh() {
            if (currentCategory == -1) {
                fireContentsChanged(this, 0, 0);
            } else {
                fireContentsChanged(this, 0, filteredEntries.size());
            }
        }
    }

}
