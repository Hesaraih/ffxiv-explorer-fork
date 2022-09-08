package com.fragmenterworks.ffxivextract.gui.components;

import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.models.ParameterInfo;
import com.fragmenterworks.ffxivextract.models.SHCD_File;
import com.fragmenterworks.ffxivextract.models.SHPK_File;
import com.fragmenterworks.ffxivextract.models.directx.D3DXShader_ConstantTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ItemEvent;

public class Shader_View extends JPanel {

    private JTree treeParameters;
    private JComboBox<String> cmbShaderIndex;
    private Hex_View hexView;

    private SHCD_File shader = null;
    private SHPK_File shaderPack = null;

    @SuppressWarnings("unused")
    DefaultMutableTreeNode root;

    private JPanel panel;

    public Shader_View(SHCD_File shader) {

        this.shader = shader;

        initUi();

        if (shader.getShaderType() == 0) {
            cmbShaderIndex.addItem("Vertex Shader #0");
        } else {
            cmbShaderIndex.addItem("Pixel Shader #0");
        }
        cmbShaderIndex.setEnabled(false);

        loadShader(0);
    }

    /**
     * コンストラクタ
     * @param shaderPack シェーダーパックファイルクラス
     */
    public Shader_View(SHPK_File shaderPack) {

        this.shaderPack = shaderPack;

        initUi();

        @SuppressWarnings("unused")
        String[] list = new String[shaderPack.getNumVertShaders() + shaderPack.getNumPixelShaders()];

        for (int i = 0; i < shaderPack.getNumVertShaders(); i++) {
            cmbShaderIndex.addItem("Vertex Shader #" + i);
        }

        for (int i = 0; i < shaderPack.getNumPixelShaders(); i++) {
            cmbShaderIndex.addItem("Pixel Shader #" + i);
        }

        cmbShaderIndex.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                loadShader(cmbShaderIndex.getSelectedIndex());
            }
        });

        loadShader(0);
    }

    private void loadShader(int i) {
        if (shader != null) {
            processCTable(0, shader.getShaderType(), shader.getConstantTable());
            hexView.setBytes(shader.getShaderBytecode());

            for (ParameterInfo pi : shader.getShaderHeader().paramInfo) {
                Utils.getGlobalLogger().info("名前: {}, ID: {}", pi.parameterName, String.format("0x%04X", pi.id));
            }
        } else {
            processCTable(i, shaderPack.getShaderType(i), shaderPack.getConstantTable(i));
            hexView.setBytes(shaderPack.getShaderBytecode(i));

            for (ParameterInfo pi : shaderPack.getShaderHeader(i).paramInfo) {
                Utils.getGlobalLogger().info("名前: {}, ID: {}", pi.parameterName, String.format("0x%04X", pi.id));
            }
        }
    }

    private void processCTable(int num, int type, D3DXShader_ConstantTable cTable) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        if (type == 0) {
            root.setUserObject("Vertex Shader");
        } else {
            root.setUserObject("Pixel Shader");
        }

        if (cTable != null){
            for (int i = 0; i < cTable.constantInfo.length; i++) {
                String paramId = "";

                if (shader == null) {
                    for (int j = 0; j < shaderPack.getShaderHeader(num).paramInfo.length; j++) {
                        if (shaderPack.getShaderHeader(num).paramInfo[j].parameterName.equals(cTable.constantInfo[i].Name)) {
                            paramId = String.format("0x%x", shaderPack.getShaderHeader(num).paramInfo[j].id);
                            break;
                        }
                    }
                }else{
                    paramId = String.format("0x%x", shader.getShaderHeader().paramInfo[i].id);
                }
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(cTable.constantInfo[i].Name + "[" + cTable.constantInfo[i].TypeInfo.Columns + "x" + cTable.constantInfo[i].TypeInfo.Rows + "]" + " Index: " + cTable.constantInfo[i].RegisterIndex + ", ParamId: " + paramId);
                root.add(node);

                for (int j = 0; j < cTable.constantInfo[i].TypeInfo.StructMemberInfo.length; j++) {
                    DefaultMutableTreeNode node2 = new DefaultMutableTreeNode(cTable.constantInfo[i].TypeInfo.StructMemberInfo[j].Name + "[" + cTable.constantInfo[i].TypeInfo.StructMemberInfo[j].TypeInfo.Columns + "x" + cTable.constantInfo[i].TypeInfo.StructMemberInfo[j].TypeInfo.Rows + "]");
                    node.add(node2);
                }
            }
        }

        treeParameters = new JTree(root);
        panel.removeAll();
        panel.add(treeParameters);
        treeParameters.updateUI();
    }

    private void initUi() {
        setLayout(new BorderLayout(0, 0));

        JPanel panel_2 = new JPanel();
        add(panel_2, BorderLayout.NORTH);
        panel_2.setLayout(new BorderLayout(0, 0));

        JPanel panel_3 = new JPanel();
        panel_3.setBorder(new EmptyBorder(0, 5, 0, 0));
        FlowLayout flowLayout = (FlowLayout) panel_3.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        panel_2.add(panel_3, BorderLayout.NORTH);

        JLabel lblNewLabel = new JLabel("シェーダ: ");
        panel_3.add(lblNewLabel);

        cmbShaderIndex = new JComboBox<>();
        panel_3.add(cmbShaderIndex);

        panel = new JPanel();
        panel_2.add(panel, BorderLayout.CENTER);
        panel.setBorder(new TitledBorder(null, "パラメータ", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.setLayout(new BorderLayout(0, 0));

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width, 300));
        panel.add(scrollPane, BorderLayout.NORTH);


        treeParameters = new JTree(new DefaultMutableTreeNode());
        treeParameters.setShowsRootHandles(true);

        treeParameters.setVisibleRowCount(8);

        scrollPane.setViewportView(treeParameters);
        JPanel panel_1 = new JPanel();
        panel_1.setBorder(new TitledBorder(null, "バイトコード", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        add(panel_1, BorderLayout.CENTER);
        panel_1.setLayout(new BorderLayout(0, 0));

        hexView = new Hex_View(16);
        panel_1.add(hexView);
    }

}
