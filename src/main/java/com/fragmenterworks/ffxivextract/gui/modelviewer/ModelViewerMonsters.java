package com.fragmenterworks.ffxivextract.gui.modelviewer;

import com.fragmenterworks.ffxivextract.gui.components.EXDF_View;
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

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
                        case 2:
                            EquippableRender demihuman = new EquippableRender();

                            switch (filteredEntries.get(selected).id) {
                                case 1: //チョコボ
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d0001/obj/equipment/e0001/model/d0001e0001_dwn.mdl", 1);
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d0001/obj/equipment/e0001/model/d0001e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.SHO, modelIndexFile, "chara/demihuman/d0001/obj/equipment/e0001/model/d0001e0001_sho.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d0001/obj/equipment/e0001/model/d0001e0001_top.mdl", 1);
                                    break;
                                case 2: //魔導アーマー
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d0002/obj/equipment/e0001/model/d0002e0001_top.mdl", 1);
                                    break;
                                case 1001: //アマルジャ
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1001/obj/equipment/e0001/model/d1001e0001_dwn.mdl", 1);
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1001/obj/equipment/e0001/model/d1001e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.SHO, modelIndexFile, "chara/demihuman/d1001/obj/equipment/e0001/model/d1001e0001_sho.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1001/obj/equipment/e0001/model/d1001e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.GLV, modelIndexFile, "chara/demihuman/d1001/obj/equipment/e0001/model/d1001e0001_glv.mdl", 1);
                                    break;
                                case 1002: //イクサル
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1002/obj/equipment/e0001/model/d1002e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.SHO, modelIndexFile, "chara/demihuman/d1002/obj/equipment/e0001/model/d1002e0001_sho.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1002/obj/equipment/e0001/model/d1002e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.GLV, modelIndexFile, "chara/demihuman/d1002/obj/equipment/e0001/model/d1002e0001_glv.mdl", 1);
                                    break;
                                case 1003: //コボルド
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1003/obj/equipment/e0001/model/d1003e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1003/obj/equipment/e0001/model/d1003e0001_top.mdl", 1);
                                    break;
                                case 1004: //ゴブリン
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1004/obj/equipment/e0001/model/d1004e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1004/obj/equipment/e0001/model/d1004e0001_top.mdl", 1);
                                    break;
                                case 1005: //シルフィー
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1005/obj/equipment/e0001/model/d1005e0001_top.mdl", 1);
                                    break;
                                case 1006: //モーグリ
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1006/obj/equipment/e0001/model/d1006e0001_top.mdl", 1);
                                    break;
                                case 1007: //サハギン
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1007/obj/equipment/e0001/model/d1007e0001_top.mdl", 1);
                                    break;
                                case 1008: //マムージャ
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1008/obj/equipment/e0001/model/d1008e0001_top.mdl", 1);
                                    break;
                                case 1009: //ギガース
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1009/obj/equipment/e0001/model/d1009e0001_dwn.mdl", 1);
                                    demihuman.setModel(EquippableRender.GLV, modelIndexFile, "chara/demihuman/d1009/obj/equipment/e0001/model/d1009e0001_glv.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1009/obj/equipment/e0001/model/d1009e0001_top.mdl", 1);
                                    break;
                                case 1010: //かに
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1010/obj/equipment/e0001/model/d1010e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1010/obj/equipment/e0001/model/d1010e0001_top.mdl", 1);
                                    break;
                                case 1011: //馬
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1011/obj/equipment/e0001/model/d1011e0001_dwn.mdl", filteredEntries.get(selected).varient);
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1011/obj/equipment/e0001/model/d1011e0001_met.mdl", filteredEntries.get(selected).varient);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1011/obj/equipment/e0001/model/d1011e0001_top.mdl", filteredEntries.get(selected).varient);
                                    break;
                                case 1012: //キキルン
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1012/obj/equipment/e0001/model/d1012e0001_top.mdl", 1);
                                    break;
                                case 1013: //ポストモーグリ
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1013/obj/equipment/e0001/model/d1013e0001_dwn.mdl", 1);
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1013/obj/equipment/e0001/model/d1013e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1013/obj/equipment/e0001/model/d1013e0001_top.mdl", 1);
                                    break;
                                case 1014: //ラミア
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1014/obj/equipment/e0001/model/d1014e0001_top.mdl", 1);
                                    break;
                                case 1015: //スケルトン
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1015/obj/equipment/e0001/model/d1015e0001_top.mdl", 1);
                                    break;
                                case 1016: //サキュバス
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1016/obj/equipment/e0001/model/d1016e0001_top.mdl", 1);
                                    break;
                                case 1017: //デーモン
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1017/obj/equipment/e0001/model/d1017e0001_top.mdl", 1);
                                    break;
                                case 1018: //皇帝
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1018/obj/equipment/e0001/model/d1018e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1018/obj/equipment/e0001/model/d1018e0001_top.mdl", 1);
                                    break;
                                case 1019: //トルーダン7世
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1019/obj/equipment/e0001/model/d1019e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1019/obj/equipment/e0001/model/d1019e0001_top.mdl", 1);
                                    break;
                                case 1020: //グナース
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1020/obj/equipment/e0001/model/d1020e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1020/obj/equipment/e0001/model/d1020e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1020/obj/equipment/e0001/model/d1020e0001_dwn.mdl", 1);
                                    break;
                                case 1021: //レプトイド
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1021/obj/equipment/e0001/model/d1021e0001_dwn.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1021/obj/equipment/e0001/model/d1021e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.GLV, modelIndexFile, "chara/demihuman/d1021/obj/equipment/e0001/model/d1021e0001_glv.mdl", 1);
                                    break;
                                case 1022: //エターナルチョコボ
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1022/obj/equipment/e0001/model/d1022e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1022/obj/equipment/e0001/model/d1022e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1022/obj/equipment/e0001/model/d1022e0001_dwn.mdl", 1);
                                    demihuman.setModel(EquippableRender.SHO, modelIndexFile, "chara/demihuman/d1022/obj/equipment/e0001/model/d1022e0001_sho.mdl", 1);
                                    break;
                                case 1023: //マトーヤ
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1023/obj/equipment/e0001/model/d1023e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1023/obj/equipment/e0001/model/d1023e0001_top.mdl", 1);
                                    break;
                                case 1024: //トルーダン
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1024/obj/equipment/e0001/model/d1024e0001_top.mdl", 1);
                                    break;
                                case 1025: //ナイツオブラウンド
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1025/obj/equipment/e0001/model/d1025e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1025/obj/equipment/e0001/model/d1025e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1025/obj/equipment/e0001/model/d1025e0001_dwn.mdl", 1);
                                    break;
                                case 1026: //エキドナ
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1026/obj/equipment/e0001/model/d1026e0001_top.mdl", 1);
                                    break;
                                case 1027: //魔法人形・ドワーフ
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1027/obj/equipment/e0001/model/d1027e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1027/obj/equipment/e0001/model/d1027e0001_top.mdl", 1);
                                    break;
                                case 1028: //コウジン族
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1028/obj/equipment/e0001/model/d1028e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1028/obj/equipment/e0001/model/d1028e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1028/obj/equipment/e0001/model/d1028e0001_dwn.mdl", 1);
                                    demihuman.setModel(EquippableRender.SHO, modelIndexFile, "chara/demihuman/d1028/obj/equipment/e0001/model/d1028e0001_sho.mdl", 1);
                                    break;
                                case 1029: //アナンタ族
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1029/obj/equipment/e0001/model/d1029e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1029/obj/equipment/e0001/model/d1029e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1029/obj/equipment/e0001/model/d1029e0001_dwn.mdl", 1);
                                    demihuman.setModel(EquippableRender.SHO, modelIndexFile, "chara/demihuman/d1029/obj/equipment/e0001/model/d1029e0001_sho.mdl", 1);
                                    break;
                                case 1030: //人狼族
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1030/obj/equipment/e0001/model/d1030e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1030/obj/equipment/e0001/model/d1030e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1030/obj/equipment/e0001/model/d1030e0001_dwn.mdl", 1);
                                    break;
                                case 1031: //バンガ族
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1031/obj/equipment/e0001/model/d1031e0001_top.mdl", 1);
                                    break;
                                case 1032: //アナンタ族
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1032/obj/equipment/e0001/model/d1032e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1032/obj/equipment/e0001/model/d1032e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1032/obj/equipment/e0001/model/d1032e0001_dwn.mdl", 1);
                                    demihuman.setModel(EquippableRender.SHO, modelIndexFile, "chara/demihuman/d1032/obj/equipment/e0001/model/d1032e0001_sho.mdl", 1);
                                    break;
                                case 1033: //モーグリ・ノッケン
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1033/obj/equipment/e0001/model/d1033e0001_top.mdl", 1);
                                    break;
                                case 1034: //ヴィエラ族
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1034/obj/equipment/e0001/model/d1034e0001_dwn.mdl", 1);
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1034/obj/equipment/e0001/model/d1034e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.SHO, modelIndexFile, "chara/demihuman/d1034/obj/equipment/e0001/model/d1034e0001_sho.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1034/obj/equipment/e0001/model/d1034e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.GLV, modelIndexFile, "chara/demihuman/d1034/obj/equipment/e0001/model/d1034e0001_glv.mdl", 1);
                                    break;
                                case 1035: //ドゥリア・チャイ(チャイ夫妻の妻の方)
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1035/obj/equipment/e0001/model/d1035e0001_dwn.mdl", 1);
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1035/obj/equipment/e0001/model/d1035e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.SHO, modelIndexFile, "chara/demihuman/d1035/obj/equipment/e0001/model/d1035e0001_sho.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1035/obj/equipment/e0001/model/d1035e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.GLV, modelIndexFile, "chara/demihuman/d1035/obj/equipment/e0001/model/d1035e0001_glv.mdl", 1);
                                    break;
                                case 1036: //アマロ
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1036/obj/equipment/e0001/model/d1036e0001_top.mdl", 1);
                                    break;
                                case 1037: //ン・モゥ族
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1037/obj/equipment/e0001/model/d1037e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1037/obj/equipment/e0001/model/d1037e0001_top.mdl", 1);
                                    break;
                                case 1038: //ピクシー族
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1038/obj/equipment/e0001/model/d1038e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1038/obj/equipment/e0001/model/d1038e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.GLV, modelIndexFile, "chara/demihuman/d1038/obj/equipment/e0001/model/d1038e0001_glv.mdl", 1);
                                    break;
                                case 1039: //アマロ(飛行)
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1039/obj/equipment/e0001/model/d1039e0001_dwn.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1039/obj/equipment/e0001/model/d1039e0001_top.mdl", 1);
                                    break;
                                case 1040: //ホブゴブリン
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1040/obj/equipment/e0001/model/d1040e0001_top.mdl", 1);
                                    break;
                                case 1041: //ガイア
                                    demihuman.setModel(EquippableRender.DWN, modelIndexFile, "chara/demihuman/d1041/obj/equipment/e0001/model/d1041e0001_dwn.mdl", 1);
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1041/obj/equipment/e0001/model/d1041e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.SHO, modelIndexFile, "chara/demihuman/d1041/obj/equipment/e0001/model/d1041e0001_sho.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1041/obj/equipment/e0001/model/d1041e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.GLV, modelIndexFile, "chara/demihuman/d1041/obj/equipment/e0001/model/d1041e0001_glv.mdl", 1);
                                    break;
                                case 1042: //ヴァリス・ゾス・ガルヴァス皇帝
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1042/obj/equipment/e0001/model/d1042e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1042/obj/equipment/e0001/model/d1042e0001_top.mdl", 1);
                                    break;
                                case 1043: //エル・トゥ(子竜)
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1043/obj/equipment/e0001/model/d1043e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.SHO, modelIndexFile, "chara/demihuman/d1043/obj/equipment/e0001/model/d1043e0001_sho.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1043/obj/equipment/e0001/model/d1043e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.GLV, modelIndexFile, "chara/demihuman/d1043/obj/equipment/e0001/model/d1043e0001_glv.mdl", 1);
                                    break;
                                case 1044: //エル・トゥ(成長)
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1044/obj/equipment/e0001/model/d1044e0001_top.mdl", 1);
                                    break;
                                case 1045: //女王グンヒルド
                                    demihuman.setModel(EquippableRender.MET, modelIndexFile, "chara/demihuman/d1045/obj/equipment/e0001/model/d1045e0001_met.mdl", 1);
                                    demihuman.setModel(EquippableRender.SHO, modelIndexFile, "chara/demihuman/d1045/obj/equipment/e0001/model/d1045e0001_sho.mdl", 1);
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1045/obj/equipment/e0001/model/d1045e0001_top.mdl", 1);
                                    demihuman.setModel(EquippableRender.GLV, modelIndexFile, "chara/demihuman/d1045/obj/equipment/e0001/model/d1045e0001_glv.mdl", 1);
                                    break;
                                case 1048: //ニーア関係？
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1048/obj/equipment/e0001/model/d1048e0001_top.mdl", 1);
                                    break;
                                case 1049: //闇の巫女
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1049/obj/equipment/e0001/model/d1049e0001_top.mdl", 1);
                                    break;
                                case 1050: //赤い少女(戦闘)
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1050/obj/equipment/e0001/model/d1050e0001_top.mdl", 1);
                                    break;
                                case 1056: //赤い少女
                                    demihuman.setModel(EquippableRender.TOP, modelIndexFile, "chara/demihuman/d1056/obj/equipment/e0001/model/d1056e0001_top.mdl", 1);
                                    break;
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

        loadAndParseNames("./monsters.lst");

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
                if (line.startsWith("#") || line.isEmpty() || line.equals("")) {
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
