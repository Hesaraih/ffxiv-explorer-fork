package com.fragmenterworks.ffxivextract.gui.components;

import com.fragmenterworks.ffxivextract.helpers.JOrbisPlayer;
import com.fragmenterworks.ffxivextract.helpers.MSADPCM_Decode;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.models.SCD_File;
import com.fragmenterworks.ffxivextract.models.SCD_File.SCD_Sound_Info;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

public class Sound_View extends JPanel {
    private final JTable tblSoundEntryList;
    private final SCD_File file;

    private final JOrbisPlayer oggPlayer = new JOrbisPlayer();

    public Sound_View(SCD_File scdFile) {

        setLayout(new BorderLayout(0, 0));

        file = scdFile;

        JPanel pnlFileList = new JPanel();
        pnlFileList.setBorder(new TitledBorder(null, "SCD Contents",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        add(pnlFileList, BorderLayout.CENTER);
        pnlFileList.setLayout(new BoxLayout(pnlFileList, BoxLayout.X_AXIS));

        JScrollPane scrollPane = new JScrollPane();
        pnlFileList.add(scrollPane);

        tblSoundEntryList = new JTable();
        tblSoundEntryList.setShowVerticalLines(false);
        scrollPane.setViewportView(tblSoundEntryList);
        tblSoundEntryList.setModel(new SCDTableModel(scdFile));
        tblSoundEntryList.getColumnModel().getColumn(4).setPreferredWidth(79);
        tblSoundEntryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblSoundEntryList.getSelectionModel().addListSelectionListener(e -> {


            if (!e.getValueIsAdjusting()) {

                SCD_Sound_Info info = file.getSoundInfo(tblSoundEntryList.getSelectedRow());
                if (info != null) {
                    oggPlayer.stop();

                    if (info.dataType == 0x0C) {
                        final byte[] header = file.getADPCMHeader(tblSoundEntryList.getSelectedRow());
                        final byte[] body = file.getADPCMData(tblSoundEntryList.getSelectedRow());
                        new Thread(() -> playMsAdpcm(header, body)).start();
                    } else if (info.dataType == 0x06) {
                        final byte[] body = file.getConverted(tblSoundEntryList.getSelectedRow());
                        new Thread(() -> playOgg(body)).start();
                    }
                }
            }
        });

        this.addComponentListener(new ComponentListener() {

            @Override
            public void componentShown(ComponentEvent arg0) {
            }

            @Override
            public void componentResized(ComponentEvent arg0) {
            }

            @Override
            public void componentMoved(ComponentEvent arg0) {
            }

            @Override
            public void componentHidden(ComponentEvent arg0) {
                oggPlayer.stop();
            }
        });
    }

    static class SCDTableModel extends AbstractTableModel {

        final SCD_File file;
        final String[] columns = {"Index", "File Size", "Data Type", "Frequency",
                "Num Channels", "Loop Start", "Loop End"};

        SCDTableModel(SCD_File file) {
            this.file = file;
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public int getRowCount() {
            return file.getNumEntries();
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {

            SCD_Sound_Info info = file.getSoundInfo(rowIndex);

            if (info == null) {
                if (columnIndex == 0)
                    return rowIndex;
                else if (columnIndex == 1)
                    return "Empty entry";
                else
                    return "N/A";
            }

            if (columnIndex == 0)
                return rowIndex;
            else if (columnIndex == 1)
                return info.fileSize;
            else if (columnIndex == 2) {
                switch (info.dataType) {
                    case 0x06:
                        return "OGG";
                    case 0x0C:
                        return "MS-ADPCM";
                    case 0x07:
                        return "PS3";
                    default:
                        return Integer.toHexString(info.dataType);
                }
            } else if (columnIndex == 3)
                return info.frequency;
            else if (columnIndex == 4)
                return info.numChannels;
            else if (columnIndex == 5)
                return info.loopStart;
            else if (columnIndex == 6)
                return info.loopEnd;
            else
                return "";
//            return "";
        }

    }

    public void stopPlayback() {
        oggPlayer.stop();
    }

    private void playOgg(byte[] body) {
        Utils.getGlobalLogger().info("Trying to play {} bytes...", body.length);
        oggPlayer.play(new ByteArrayInputStream(body));
    }

    private void playMsAdpcm(byte[] header, byte[] body) {
        if (header == null || body == null)
            return;

        ByteBuffer bb = ByteBuffer.wrap(header);
        bb.order(file.getEndian());
        bb.getShort();
        int channels = bb.getShort();
        int rate = bb.getInt();
        bb.getInt();
        int blockAlign = bb.getShort();
        int bitsPerSample = bb.getShort();

        // AudioFormatオブジェクトとDataLine.Infoオブジェクトを作成します。
        AudioFormat audioFormat = new AudioFormat((float) rate, 16, channels,
                true, false);
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class,
                audioFormat, AudioSystem.NOT_SPECIFIED);

        SourceDataLine outputLine;

        // オーディオ出力ラインがサポートされているかどうかを確認
        if (!AudioSystem.isLineSupported(dataLineInfo)) {
            Utils.getGlobalLogger().error("オーディオ出力ラインはサポートされていません。");
            return;
        }

        /*
         * すべてが大丈夫のようです。 指定された形式の行を開いて、ソースデータ行を開始してみましょう。
         */
        try {
            outputLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            outputLine.open(audioFormat);
        } catch (LineUnavailableException exception) {
            Utils.getGlobalLogger().error("リソースの制限により、オーディオ出力ラインを開くことができませんでした。", exception);
            return;
        } catch (IllegalStateException exception) {
            Utils.getGlobalLogger().error("オーディオ出力ラインはすでに開いています。", exception);
            return;
        } catch (SecurityException exception) {
            Utils.getGlobalLogger().error("セキュリティ上の理由により、音声出力ラインを開くことができませんでした。", exception);
            return;
        }

        // 始めよう
        outputLine.start();

        int bufferSize = MSADPCM_Decode.getBufferSize(body.length, channels, blockAlign, bitsPerSample);

        if (bufferSize % 4 != 0)
            bufferSize += bufferSize % 4;

        byte[] outputBuffer = new byte[bufferSize];

        MSADPCM_Decode.decode(body, outputBuffer, body.length, channels, blockAlign);

        outputLine.write(outputBuffer, 0, outputBuffer.length);

        outputLine.close();
    }

}
