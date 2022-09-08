package com.fragmenterworks.ffxivextract.gui.modelviewer;

import com.fragmenterworks.ffxivextract.gui.components.EXDF_View;
import com.fragmenterworks.ffxivextract.gui.components.ModelCharacterRenderer;
import com.fragmenterworks.ffxivextract.gui.components.ModelRenderer;
import com.fragmenterworks.ffxivextract.gui.components.OpenGL_View;
import com.fragmenterworks.ffxivextract.helpers.SparseArray;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

class ModelViewerMonsters extends JPanel {

    private static final int INDEX_MODELCHARA_TYPE = 1;
    private static final int INDEX_MODELCHARA_ID = 2;
    private static final int INDEX_MODELCHARA_MODEL = 3;
    private static final int INDEX_MODELCHARA_VARIANT = 4;

    private final ModelViewerWindow parent;

    private final ArrayList<ModelCharaEntry> entries = new ArrayList<>();
    private final ArrayList<ModelCharaEntry> filteredEntries = new ArrayList<>();

    @SuppressWarnings("unused")
    OpenGL_View view3D;
    private final JList<String> lstMonsters;

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

    private int lastOriginX, lastOriginY;
    private int lastX, lastY;

    private final SqPack_IndexFile modelIndexFile;

    private final SparseArray<String> names = new SparseArray<>();

    public ModelViewerMonsters(ModelViewerWindow parent, SqPack_IndexFile modelIndex) {

        this.parent = parent;
        this.modelIndexFile = modelIndex;

        setLayout(new BorderLayout(0, 0));

        JSplitPane splitPane = new JSplitPane();


        JPanel panel_1 = new JPanel();
        add(splitPane, BorderLayout.CENTER);
        panel_1.setLayout(new BorderLayout(0, 0));

        JPanel panel_2 = new JPanel();
        panel_2.setBorder(new TitledBorder(null, "Info", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel_1.add(panel_2, BorderLayout.NORTH);
        panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.Y_AXIS));

        JPanel panel_4 = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panel_4.getLayout();
        flowLayout.setVgap(1);
        flowLayout.setAlignment(FlowLayout.LEFT);
        panel_2.add(panel_4);

        JLabel lblInfoAndControls = new JLabel("Path: ");
        panel_4.add(lblInfoAndControls);

        txtPath = new JLabel("-");
        panel_4.add(txtPath);

        JPanel panel_5 = new JPanel();
        FlowLayout flowLayout_1 = (FlowLayout) panel_5.getLayout();
        flowLayout_1.setVgap(1);
        flowLayout_1.setAlignment(FlowLayout.LEFT);
        panel_2.add(panel_5);

        JLabel label = new JLabel("Info: ");
        panel_5.add(label);

        txtModelInfo = new JLabel("Type: -, Id: -, Model: -, Variant: -");
        panel_5.add(txtModelInfo);

        JPanel panel_6 = new JPanel();
        FlowLayout flowLayout_2 = (FlowLayout) panel_6.getLayout();
        flowLayout_2.setAlignment(FlowLayout.LEFT);
        panel_2.add(panel_6);

        JButton btnResetCamera = new JButton("Reset Camera");
        panel_6.add(btnResetCamera);

        chkGlowToggle = new JCheckBox("Glow Shader", true);
        panel_6.add(chkGlowToggle);

        btnResetCamera.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                renderer.resetCamera();
            }
        });

        chkGlowToggle.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                renderer.toggleGlow(chkGlowToggle.isSelected());
            }
        });

        JPanel panel_3 = new JPanel();
        panel_1.add(panel_3, BorderLayout.CENTER);
        panel_3.setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        //add(panel, BorderLayout.WEST);
        panel.setLayout(new BorderLayout(0, 0));

        JScrollPane scrollPane = new JScrollPane();
        panel.add(scrollPane, BorderLayout.CENTER);
        lstMonsters = new JList<>();

        scrollPane.setViewportView(lstMonsters);

        lstMonsters.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent event) {


                if (event.getValueIsAdjusting() || lstMonsters.getModel().getSize() == 0) {
                    return;
                }

                int selected = lstMonsters.getSelectedIndex();

                if (selected == -1) {
                    return;
                }

                String modelPath;
                byte[] modelData;
                try {
                    Utils.getGlobalLogger().info("タイプ{}をID{}で読み込んでいます", entries.get(selected).type, entries.get(selected).id);

                    switch (filteredEntries.get(selected).type) {
                        case 1:
                            if(filteredEntries.get(selected).id == 0) {
                                //type=1はhumanモデルなので、通常はBNpcBaseによってBNpcCustomize、BNpcParts、NpcEquip、
                                //ModelCharaの参照値が渡されるため、ModelCharaのみの値が渡されることはない。
                                txtPath.setText("chara/human/c????/obj/");
                                txtModelInfo.setText(String.format("Type: %d, Id: %d, Model: %d, Variant: %d", filteredEntries.get(selected).type, filteredEntries.get(selected).id, filteredEntries.get(selected).model, filteredEntries.get(selected).varient));
                                renderer.clear();
                            }else{
                                ModelCharacterRenderer human = new ModelCharacterRenderer();
                                int pathCheck;
                                Model model;
                                for (int i = 0; i < 3; i++) {
                                    modelPath = String.format("chara/human/c%04d/obj/body/b%04d/model/c%04db%04d.mdl", filteredEntries.get(selected).id, filteredEntries.get(selected).model, filteredEntries.get(selected).id, filteredEntries.get(selected).model);
                                    //部位ごとに存在チェック
                                    pathCheck = modelIndexFile.findFile(modelPath);
                                    if (pathCheck == 2) {
                                        modelData = modelIndexFile.extractFile(modelPath);
                                        model = new Model(modelPath, modelIndexFile, modelData, modelIndexFile.getEndian());
                                        //該当部位が存在したらhumanモデルに追加
                                        human.setModel(i, model);
                                    }
                                }

                                //renderer.setModels(human.getModels());
                            }
                            break;
                        case 2:
                            //demihumanはパーツごとに分かれているため、組み合わせる必要がある
                            String[] equip = new String[]{"met", "top", "glv", "dwn", "sho"};
                            EquippableRender demihuman = new EquippableRender();

                            int pathCheck;
                            for (int i = 0; i < 5; i++) {
                                modelPath = String.format("chara/demihuman/d%04d/obj/equipment/e0001/model/d%04de0001_%s.mdl", filteredEntries.get(selected).id, filteredEntries.get(selected).id, equip[i]);
                                //部位ごとに存在チェック
                                pathCheck = modelIndexFile.findFile(modelPath);
                                if (pathCheck == 2) {
                                    //該当部位が存在したらdemihumanモデルに追加
                                    demihuman.setModel(i, modelIndexFile, modelPath, 1);
                                }
                            }

                            renderer.setModels(demihuman.getModels());

                            txtPath.setText(String.format("chara/demihuman/d%04d/obj/equipment/e%04d/model/d%04de%04d_XXX.mdl", filteredEntries.get(selected).id, 1, filteredEntries.get(selected).id, 1));
                            txtModelInfo.setText(String.format("Type: %d, Id: %d, Model: %d, Variant: %d", filteredEntries.get(selected).type, filteredEntries.get(selected).id, filteredEntries.get(selected).model, filteredEntries.get(selected).varient));

                            break;
                        case 3:
                            modelPath = String.format("chara/monster/m%04d/obj/body/b%04d/model/m%04db%04d.mdl", filteredEntries.get(selected).id, filteredEntries.get(selected).model, filteredEntries.get(selected).id, filteredEntries.get(selected).model);
                            modelData = modelIndexFile.extractFile(modelPath);
                            if (modelData != null) {
                                HashDatabase.addPathToDB(modelPath, "040000");
                                Model model = new Model(modelPath, modelIndexFile, modelData, modelIndex.getEndian());
                                model.loadVariant(filteredEntries.get(selected).varient);
                                renderer.setModel(model);
                            } else {
                                Utils.getGlobalLogger().debug("モデル{}が見つかりません!", modelPath);
                                txtPath.setText(modelPath);
                                txtModelInfo.setText("モデルが見つかりません!");
                                renderer.clear();
                                return;
                            }

                            txtPath.setText(modelPath);
                            txtModelInfo.setText(String.format("Type: %d, Id: %d, Model: %d, Variant: %d", filteredEntries.get(selected).type, filteredEntries.get(selected).id, filteredEntries.get(selected).model, filteredEntries.get(selected).varient));

                            break;
                        default:
                            txtPath.setText("-");
                            txtModelInfo.setText("Type: -, Id: -, Model: -, Variant: -");
                            renderer.clear();
                            break;
                    }
                } catch (IOException e) {
                    Utils.getGlobalLogger().error(e);
                }


            }
        });

        JPanel panel_7 = new JPanel();
        panel.add(panel_7, BorderLayout.SOUTH);
        panel_7.setLayout(new BorderLayout(0, 0));

        ActionListener searchListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String filter = edtSearch.getText();

                lstMonsters.clearSelection();

                filteredEntries.clear();

                int number = -1;

                try {
                    number = Integer.parseUnsignedInt(filter);
                } catch (NumberFormatException ignored) {}

                for (ModelCharaEntry entry : entries) {
                    if (number != -1 && entry.index == number) {
                        filteredEntries.add(entry);
                    }

                    if (number == -1 && names.get(entry.index, "Monster " + entry.index).toLowerCase().contains(filter.toLowerCase())) {
                        filteredEntries.add(entry);
                    }
                }

                ((MonsterListModel) lstMonsters.getModel()).refresh();
            }
        };

        edtSearch = new JTextField();
        panel_7.add(edtSearch, BorderLayout.CENTER);
        edtSearch.addActionListener(searchListener);
        edtSearch.setColumns(10);

        JButton btnSearch = new JButton("検索");
        btnSearch.addActionListener(searchListener);
        panel_7.add(btnSearch, BorderLayout.EAST);

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

        //多言語化テスト
        if (Locale.getDefault().getLanguage().equals(Locale.JAPANESE.getLanguage())){
            loadAndParseNames("./monsters_jp.lst");
        }else{
            loadAndParseNames("./monsters.lst");
        }

        try {
            if (!loadMonsters()) {
                removeAll();
                JLabel errorLabel = new JLabel("ゲームモデルリストの読み込み中にエラーが発生しました。");
                add(errorLabel);
                return;
            }
        } catch (IOException e1) {
            Utils.getGlobalLogger().error(e1);
        }

        panel_3.add(glcanvas, BorderLayout.CENTER);


        splitPane.setLeftComponent(panel);
        splitPane.setRightComponent(panel_1);
        splitPane.setDividerLocation(200);

        // lstMonsters.setSelectedIndex(311);

    }

    private boolean loadMonsters() throws IOException {
        SqPack_IndexFile indexFile = parent.getExdIndexFile();
        EXHF_File exhfFile = new EXHF_File(indexFile.extractFile("exd/ModelChara.exh"));
        EXDF_View view = new EXDF_View(indexFile, "exd/ModelChara.exh", exhfFile);

        try {
            for (int i = 0; i < view.getTable().getRowCount(); i++) {

                int index = (Integer) view.getTable().getValueAt(i, 0);
                int v1 = (Integer) view.getTable().getValueAt(i, INDEX_MODELCHARA_ID);
                int v2 = (Integer) view.getTable().getValueAt(i, INDEX_MODELCHARA_MODEL);
                int v3 = (Integer) view.getTable().getValueAt(i, INDEX_MODELCHARA_VARIANT);
                int v4 = (Integer) view.getTable().getValueAt(i, INDEX_MODELCHARA_TYPE);

                if (v4 <= 1 || (names.get(index) != null && names.get(index).equals("BLANK"))) {
                    continue;
                }

                entries.add(new ModelCharaEntry(index, v1, v2, v3, v4));

            }
        } catch (Exception e) {
            //Utils.getGlobalLogger().error(e);
            return false;
        }

        lstMonsters.setModel(new MonsterListModel());

        filteredEntries.addAll(entries);

        return true;
    }

    private void loadAndParseNames(String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            for (String line; (line = br.readLine()) != null; ) {
                //Skip comments and whitespace
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                if (line.contains(":")) {
                    String[] split = line.split(":", 2);
                    if (split.length != 2) {
                        continue;
                    }

                    if (split[1].isEmpty()) {
                        continue;
                    }
                    names.put(Integer.parseInt(split[0]), split[1]);
                }
            }
        } catch (IOException e) {
            Utils.getGlobalLogger().error("{}から名前を読み込めませんでした", path, e);
        }
    }

    private class MonsterListModel extends AbstractListModel<String> {
        public int getSize() {
            return filteredEntries.size();
        }

        public String getElementAt(int index) {
            return names.get(filteredEntries.get(index).index, "Monster " + filteredEntries.get(index).index);
        }

        void refresh() {
            fireContentsChanged(this, 0, filteredEntries.size());
        }
    }
}
