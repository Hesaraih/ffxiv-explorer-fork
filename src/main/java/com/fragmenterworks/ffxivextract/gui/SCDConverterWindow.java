package com.fragmenterworks.ffxivextract.gui;

import com.fragmenterworks.ffxivextract.Strings;
import com.fragmenterworks.ffxivextract.helpers.Utils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

class SCDConverterWindow extends JFrame {

    private final JTextField txtPath;
    private final JTextField edtVolume;
    private final JTextField edtChannels;
    private final JTextField edtSampleRate;
    private final JTextField edtNumSamples;
    private final JTextField edtLoopStart;
    private final JTextField edtLoopEnd;

    private final JLabel lblVolume;
    private final JLabel lblChannels;
    private final JLabel lblSample;
    private final JLabel lblLoopStart;
    private final JLabel lblLoopEnd;
    private final JRadioButton rdbtnLoop;
    private final JRadioButton rdbtnCustom;
    private final JPanel pnlOpt;

    private File currentOggFile;

    public SCDConverterWindow() {
        this.setTitle(Strings.DIALOG_TITLE_SCDCONVERTER);
        URL imageURL = getClass().getResource("/frameicon.png");
        ImageIcon image = new ImageIcon(Objects.requireNonNull(imageURL));
        this.setIconImage(image.getImage());

        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Input Ogg", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPane.add(panel, BorderLayout.NORTH);
        panel.setLayout(new BorderLayout(0, 0));

        JPanel panel_3 = new JPanel();
        panel.add(panel_3, BorderLayout.NORTH);

        JPanel panel_4 = new JPanel();
        panel.add(panel_4, BorderLayout.SOUTH);
        panel_4.setLayout(new BoxLayout(panel_4, BoxLayout.X_AXIS));

        JLabel lblNewLabel = new JLabel("変換されたファイルは「inputFile.ogg.scd」になります。");
        panel_4.add(lblNewLabel);

        txtPath = new JTextField();
        txtPath.setEditable(false);
        panel.add(txtPath, BorderLayout.CENTER);

        JButton btnBrowse = new JButton(Strings.BUTTONNAMES_BROWSE);
        panel.add(btnBrowse, BorderLayout.EAST);

        pnlOpt = new JPanel();
        pnlOpt.setBorder(new TitledBorder(null, "Options", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPane.add(pnlOpt, BorderLayout.CENTER);
        pnlOpt.setLayout(new BorderLayout(0, 0));

        JPanel panel_1 = new JPanel();
        pnlOpt.add(panel_1, BorderLayout.CENTER);
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));

        JPanel panel_5 = new JPanel();
        panel_5.setBorder(new EmptyBorder(0, 0, 2, 0));
        panel_1.add(panel_5);
        panel_5.setLayout(new BorderLayout(0, 0));

        lblVolume = new JLabel("音量: ");
        panel_5.add(lblVolume, BorderLayout.WEST);

        edtVolume = new JTextField();
        panel_5.add(edtVolume, BorderLayout.EAST);
        edtVolume.setColumns(10);

        JPanel panel_6 = new JPanel();
        panel_6.setBorder(new EmptyBorder(0, 0, 2, 0));
        panel_1.add(panel_6);
        panel_6.setLayout(new BorderLayout(0, 0));

        lblChannels = new JLabel("Number of Channels: ");
        panel_6.add(lblChannels, BorderLayout.WEST);

        edtChannels = new JTextField();
        edtChannels.setColumns(10);
        panel_6.add(edtChannels, BorderLayout.EAST);

        JPanel panel_7 = new JPanel();
        panel_7.setBorder(new EmptyBorder(0, 0, 2, 0));
        panel_1.add(panel_7);
        panel_7.setLayout(new BorderLayout(0, 0));

        lblSample = new JLabel("サンプリングレート(hz): ");
        panel_7.add(lblSample, BorderLayout.WEST);

        edtSampleRate = new JTextField();
        edtSampleRate.setColumns(10);
        panel_7.add(edtSampleRate, BorderLayout.EAST);

        JPanel panel_11 = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panel_11.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        panel_1.add(panel_11);

        ButtonGroup group = new ButtonGroup();

        rdbtnLoop = new JRadioButton("Loop Song");
        rdbtnLoop.setSelected(true);
        group.add(rdbtnLoop);
        panel_11.add(rdbtnLoop);
        rdbtnLoop.setActionCommand("loopNorm");

        rdbtnCustom = new JRadioButton("Custom");
        panel_11.add(rdbtnCustom);
        rdbtnCustom.setActionCommand("loopCustom");

        group.add(rdbtnCustom);

        JPanel panel_8 = new JPanel();
        panel_8.setBorder(new EmptyBorder(0, 0, 2, 0));
        panel_1.add(panel_8);
        panel_8.setLayout(new BorderLayout(0, 0));

        lblLoopStart = new JLabel("ループ開始(サンプル): ");
        panel_8.add(lblLoopStart, BorderLayout.WEST);

        edtLoopStart = new JTextField();
        edtLoopStart.setEnabled(false);
        edtLoopStart.setColumns(10);
        panel_8.add(edtLoopStart, BorderLayout.EAST);

        JPanel panel_9 = new JPanel();
        panel_1.add(panel_9);
        panel_9.setLayout(new BorderLayout(0, 0));

        lblLoopEnd = new JLabel("ループ終了(サンプル):   ");
        panel_9.add(lblLoopEnd, BorderLayout.WEST);

        edtLoopEnd = new JTextField();
        edtLoopEnd.setEnabled(false);
        edtLoopEnd.setColumns(10);
        panel_9.add(edtLoopEnd, BorderLayout.EAST);

        JPanel panel_10 = new JPanel();
        panel_1.add(panel_10);
        panel_10.setLayout(new BorderLayout(0, 0));

        JLabel lblNumSamples = new JLabel("Number of Samples: ");
        panel_10.add(lblNumSamples, BorderLayout.WEST);

        edtNumSamples = new JTextField();
        edtNumSamples.setEnabled(false);
        edtNumSamples.setColumns(10);
        panel_10.add(edtNumSamples, BorderLayout.EAST);

        JPanel panel_2 = new JPanel();
        contentPane.add(panel_2, BorderLayout.SOUTH);
        panel_2.setLayout(new BorderLayout(0, 0));

        JButton btnConvert = new JButton("Convert");
        panel_2.add(btnConvert, BorderLayout.NORTH);
        pack();
        setBounds(getBounds().x, getBounds().y, 500, getBounds().height);
        setResizable(false);

        setLocationRelativeTo(null);


        btnBrowse.addActionListener(arg0 -> setPath());

        rdbtnLoop.addActionListener(e -> {

        });

        ActionListener radioListener = e -> {
            if (e.getActionCommand().equals("loopNorm")) {
                edtLoopStart.setText("");
                edtLoopEnd.setText("");
                edtNumSamples.setText("");
                edtLoopStart.setEnabled(false);
                edtLoopEnd.setEnabled(false);
                edtNumSamples.setEnabled(false);
            } else if (e.getActionCommand().equals("loopCustom")) {
                edtLoopStart.setText("0");
                edtLoopEnd.setText("0");
                edtNumSamples.setText("0");
                edtLoopStart.setEnabled(true);
                edtLoopEnd.setEnabled(true);
                edtNumSamples.setEnabled(true);
            }
        };

        rdbtnLoop.addActionListener(radioListener);
        rdbtnCustom.addActionListener(radioListener);

        setEnabled(false);

        btnConvert.addActionListener(e -> {
            try {
                convert(currentOggFile.getCanonicalPath());
                JOptionPane
                        .showMessageDialog(
                                null,
                                "変換完了",
                                "Ogg to SCD Converter", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e1) {
                Utils.getGlobalLogger().error(e1);
                JOptionPane
                        .showMessageDialog(
                                null,
                                "変換できませんでした",
                                "Ogg to SCD Converter", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void convert(String oggPath) throws IOException {
        byte[] ogg = Utils.readContentIntoByteArray(new File(oggPath));

        float volume = 1.0f;
        int numChannels = 2;
        int sampleRate = 44100;
        int loopStart = 0;
        int loopEnd = ogg.length;


        //カスタムループを処理
        if (rdbtnCustom.isSelected()) {

            int positionStart;
            int positionEnd;
            int numSamples;

            try {
                positionStart = Integer.parseInt(edtLoopStart.getText());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            try {
                positionEnd = Integer.parseInt(edtLoopEnd.getText());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            try {
                numSamples = Integer.parseInt(edtNumSamples.getText());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            loopStart = getBytePosition(positionStart, numSamples, ogg.length - 0x10);
            loopEnd = getBytePosition(positionEnd, numSamples, ogg.length - 0x10);

        }
        //ヘッダ作成
        byte[] header = createSCDHeader(ogg.length, volume, numChannels, sampleRate, loopStart, loopEnd);

        //scd書き込み
        BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get(oggPath + ".scd")));
        out.write(header);
        out.write(ogg);
        out.close();
    }

    private byte[] createSCDHeader(int oggLength, float volume, int numChannels, int sampleRate, int loopStart, int loopEnd) {
        //scdテンプレートとscdヘッダーをロード
        InputStream inStream = getClass().getResourceAsStream("/scd_header.bin");
        int SCD_HEADER_SIZE = 0x540;
        byte[] scdHeader = new byte[SCD_HEADER_SIZE];
        try {
            //noinspection ResultOfMethodCallIgnored
            Objects.requireNonNull(inStream).read(scdHeader);
            inStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //必要な部分を編集する
        ByteBuffer bb = ByteBuffer.wrap(scdHeader);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.position(0x10);
        bb.putInt(scdHeader.length + oggLength);
        bb.position(0x1B0);
        bb.putInt(oggLength - 0x10);

        bb.position(0xA8);
        bb.putFloat(volume);
        bb.position(0x1B4);
        bb.putInt(numChannels);
        bb.position(0x1B8);
        bb.putInt(sampleRate);
        bb.position(0x1C0);
        bb.putInt(loopStart);
        bb.position(0x1C4);
        bb.putInt(loopEnd);

        return scdHeader;
    }

    private void setPath() {
        JFileChooser fileChooser = new JFileChooser(currentOggFile);

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        FileFilter filter = new FileFilter() {

            @Override
            public String getDescription() {
                return Strings.FILETYPE_OGG;
            }

            @Override
            public boolean accept(File f) {
                return f.getName().contains(".ogg")
                        || f.isDirectory();
            }
        };
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        int retunval = fileChooser.showOpenDialog(SCDConverterWindow.this);

        if (retunval == JFileChooser.APPROVE_OPTION) {
            try {
                txtPath.setText(fileChooser.getSelectedFile()
                        .getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                currentOggFile = fileChooser.getSelectedFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        edtVolume.setText("1.0");
        edtSampleRate.setText("44100");
        edtChannels.setText("2");
        edtLoopStart.setText("0");
        edtLoopEnd.setText("");

        setEnabled(true);
    }

    public void setEnabled(boolean b) {
        lblVolume.setEnabled(b);
        lblSample.setEnabled(b);
        lblChannels.setEnabled(b);
        lblLoopStart.setEnabled(b);
        lblLoopEnd.setEnabled(b);
        edtVolume.setEnabled(b);
        edtSampleRate.setEnabled(b);
        edtChannels.setEnabled(b);
        edtLoopStart.setEnabled(false);
        edtLoopEnd.setEnabled(false);
        edtNumSamples.setEnabled(false);
        rdbtnCustom.setEnabled(b);
        rdbtnLoop.setEnabled(b);
        pnlOpt.setEnabled(b);
    }

    private int getBytePosition(float samplePosition, float numSamples, float fileSize) {
        return (int) ((fileSize / numSamples) * samplePosition);
    }

}

