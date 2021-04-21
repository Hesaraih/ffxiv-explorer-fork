package com.fragmenterworks.ffxivextract.gui.components;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.models.Mesh;
import com.fragmenterworks.ffxivextract.models.Model;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;

public class OpenGL_View extends JPanel {

    //UI
    private final JLabel lblVertices;
    private final JLabel lblIndices;
    private final JLabel lblMeshes;
    private final JComboBox<String> cmbAnimation;
    private final JSpinner spnSpeed;
    private final JSlider sldFrame;
    private final JCheckBox chkGlowToggle;

    private final ModelRenderer renderer;

    private boolean leftMouseDown = false;
    private boolean rightMouseDown = false;

    private int currentLoD = 0;
    private int lastOriginX, lastOriginY;
    private int lastX, lastY;

    public OpenGL_View(final Model model) {
        GLProfile glProfile = GLProfile.getDefault();
        GLCapabilities glcapabilities = new GLCapabilities(glProfile);
        final GLCanvas glcanvas = new GLCanvas(glcapabilities);
        renderer = new ModelRenderer(model);
        model.loadVariant(1);
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
        setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "モデル情報", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        add(panel, BorderLayout.NORTH);
        panel.setLayout(new BorderLayout(0, 0));

        JPanel panel_1 = new JPanel();
        panel.add(panel_1, BorderLayout.EAST);
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));

        JPanel panel_3 = new JPanel();
        FlowLayout flowLayout_1 = (FlowLayout) panel_3.getLayout();
        flowLayout_1.setAlignment(FlowLayout.RIGHT);
        panel_1.add(panel_3);

        JLabel lbl1 = new JLabel("詳細レベル:");
        panel_3.add(lbl1);

        JComboBox<String> cmbLodChooser = new JComboBox<>();
        panel_3.add(cmbLodChooser);
        cmbLodChooser.setPreferredSize(new Dimension(50, 20));
        cmbLodChooser.addItem("0");
        cmbLodChooser.addItem("1");
        cmbLodChooser.addItem("2");
        cmbLodChooser.setLightWeightPopupEnabled(false);

        cmbLodChooser.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    currentLoD = Integer.parseInt((String) e.getItem());
                    StringBuilder vertList = new StringBuilder("頂点: ");
                    StringBuilder indicesList = new StringBuilder("インデックス: ");

                    if (model.getMeshes(currentLoD) != null) {
                        for (Mesh m : model.getMeshes(currentLoD)) {
                            vertList.append("(").append(m.numVerts).append(") ");
                            indicesList.append("(").append(m.numIndex).append(") ");
                        }
                    }

                    lblVertices.setText(vertList.toString());
                    lblIndices.setText(indicesList.toString());
                    lblMeshes.setText("メッシュ数: " + (model.getMeshes(currentLoD) == null ? "なし" : model.getMeshes(currentLoD).length));
                    renderer.setLoD(currentLoD);
                }
            }
        });

        JPanel panel_4 = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panel_4.getLayout();
        flowLayout.setAlignment(FlowLayout.RIGHT);
        panel_1.add(panel_4);

        chkGlowToggle = new JCheckBox("Glow(発光)シェーダ", true);
        panel_4.add(chkGlowToggle);

        chkGlowToggle.addChangeListener(e -> renderer.toggleGlow(chkGlowToggle.isSelected()));

        JLabel lblVariant = new JLabel("Variant(別マテリアルモデル):");
        panel_4.add(lblVariant);


        if (model.getNumVariants() > -1) {
            JComboBox<String> cmbVariantChooser = new JComboBox<>();
            cmbVariantChooser.setPreferredSize(new Dimension(50, 20));
            cmbVariantChooser.setLightWeightPopupEnabled(false);

            int[] variantChooserModel = new int[model.getNumVariants()];
            for (int i = 0; i < variantChooserModel.length; i++) {
                cmbVariantChooser.addItem("" + (i + 1));
            }

            cmbVariantChooser.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    model.loadVariant(Integer.parseInt((String) e.getItem()));
                    renderer.resetMaterial();
                }
            });

            panel_4.add(cmbVariantChooser);
        }

        JPanel panel_5 = new JPanel();
        panel.add(panel_5, BorderLayout.CENTER);
        panel_5.setLayout(new BoxLayout(panel_5, BoxLayout.X_AXIS));


        JPanel panel_2 = new JPanel();
        panel_5.add(panel_2);
        panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.Y_AXIS));

        lblVertices = new JLabel("頂点:");
        panel_2.add(lblVertices);

        lblIndices = new JLabel("インデックス:");
        panel_2.add(lblIndices);

        lblMeshes = new JLabel("メッシュ数: " + (model.getMeshes(currentLoD) == null ? "なし" : model.getMeshes(currentLoD).length));
        panel_2.add(lblMeshes);

        JPanel panel_6 = new JPanel();
        panel_6.setBorder(null);
        if (Constants.HAVOK_ENABLED) {
            add(panel_6, BorderLayout.SOUTH);
        }
        panel_6.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JLabel lblAnimation = new JLabel("アニメーション");
        panel_6.add(lblAnimation);

        cmbAnimation = new JComboBox<>();
        cmbAnimation.setLightWeightPopupEnabled(false);
        panel_6.add(cmbAnimation);

        int idleAnimIndex = 0;
        for (int i = 0; i < model.getNumAnimations(); i++) {
            String animName = model.getAnimationName(i);
            cmbAnimation.addItem(animName);
            if (animName.contains("id0")) {
                idleAnimIndex = i;
            }
        }

        if (model.getNumAnimations() == 0) {
            cmbAnimation.addItem("アニメーション未検出");
        } else {
            cmbAnimation.setSelectedIndex(idleAnimIndex);
        }

        cmbAnimation.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (!e.getItem().equals("アニメーション未検出")) {
                        //noinspection ConstantConditions
                        if (model != null) {
                            model.setCurrentAnimation(cmbAnimation.getSelectedIndex());
                            float speed = (float) ((Integer) spnSpeed.getValue()) / 100.0f;
                            model.setAnimationSpeed(speed);
                            int frames = model.getNumAnimationFrames(cmbAnimation.getSelectedIndex());
                            if (frames == -1) {
                                frames = 0;
                            }
                            sldFrame.setMaximum(frames);
                        }
                    }
                }
            }
        });

        if (model.getNumAnimations() != 0) {
            model.setCurrentAnimation(cmbAnimation.getSelectedIndex());
        }

        JSeparator separator_1 = new JSeparator();
        separator_1.setPreferredSize(new Dimension(1, 16));
        separator_1.setOrientation(SwingConstants.VERTICAL);
        panel_6.add(separator_1);

        JLabel lblAnimationSpeed = new JLabel("速度");
        panel_6.add(lblAnimationSpeed);

        spnSpeed = new JSpinner();
        spnSpeed.setPreferredSize(new Dimension(50, 23));
        SpinnerModel sm = new SpinnerNumberModel(100, 0, 300, 1);
        spnSpeed.setModel(sm);
        panel_6.add(spnSpeed);

        JLabel label = new JLabel("% ");
        panel_6.add(label);

        JSeparator separator = new JSeparator();
        separator.setOrientation(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 16));
        panel_6.add(separator);

        JLabel lblNewLabel = new JLabel("フレーム");
        panel_6.add(lblNewLabel);

        sldFrame = new JSlider();
        sldFrame.setSnapToTicks(true);
        sldFrame.setMinorTickSpacing(5);
        sldFrame.setMajorTickSpacing(10);
        sldFrame.setPaintTicks(true);
        sldFrame.setValue(0);
        sldFrame.setMinimum(0);
        sldFrame.setMaximum(model.getNumAnimationFrames(cmbAnimation.getSelectedIndex()));
        panel_6.add(sldFrame);

        spnSpeed.addChangeListener(e -> {

            float speed = (float) ((Integer) spnSpeed.getValue()) / 100.0f;

            //noinspection ConstantConditions
            if (model != null) {
                model.setAnimationSpeed(speed);
            }

        });

        StringBuilder vertList = new StringBuilder("頂点: ");
        StringBuilder indicesList = new StringBuilder("インデックス: ");

        if (model.getMeshes(currentLoD) != null) {
            for (Mesh m : model.getMeshes(currentLoD)) {
                vertList.append("(").append(m.numVerts).append(") ");
                indicesList.append("(").append(m.numIndex).append(") ");
            }
        } else {
            vertList.append("なし");
            indicesList.append("なし");
        }
        lblVertices.setText(vertList.toString());
        lblIndices.setText(indicesList.toString());
        if (model.getMeshes(currentLoD) == null) {
            lblMeshes.setText("メッシュ数: なし");
        } else {
            lblMeshes.setText("メッシュ数: " + model.getMeshes(currentLoD).length);
        }

        add(glcanvas, BorderLayout.CENTER);
    }

}
