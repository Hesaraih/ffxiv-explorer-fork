package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.FileTools;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.SQLException;

public class CUTB_File extends Game_File {

    //他のファイルを見つけるために使用されます
    private final SqPack_IndexFile currentIndex; //現在表示中または呼び出し元のIndexファイル
    private SqPack_IndexFile sp_IndexFile; //上記以外のIndexファイル
    private SqPack_IndexFile temp_IndexFile; //作業用

    /**
     * コンストラクタ
     * @param data sgbデータ
     * @param endian エンディアンの種類
     */
    public CUTB_File(SqPack_IndexFile index, byte[] data, ByteOrder endian)  throws IOException{
        super(endian);
        this.currentIndex = index;
        try {
            loadCUTB(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"unused", "ConstantConditions"})
    private void loadCUTB(byte[] data)  throws IOException{
        byte[] signature = new byte[4];
        byte[] signature2 = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        String signatureString = getString(bb); //CUTB
        int fileSize = bb.getInt(); //ファイルサイズ
        int chunkMax = bb.getInt(); //データチャンク数

        if (!signatureString.equals("CUTB")) {
            Utils.getGlobalLogger().error("CUTB magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", signatureString);
        }

        for (int chunkNum = 0; chunkNum < chunkMax + 1; chunkNum++){
            int chunkStartOffset = bb.position();
            String signatureString2 = getString(bb);  //CTRL等
            int chunkSize = bb.getInt(); //チャンクヘッダサイズ
            bb.getInt(); //不明 何かのID
            bb.getInt(); //不明 オフセット？
            bb.position(chunkStartOffset + chunkSize);
        }

        bb.getInt(); //不明 オフセット？

        int startPosition = bb.position();

        int jumpID = bb.getInt(); //ファイルパスのオフセット
        bb.getInt(); //不明
        int fileNum = jumpID / 8; //ファイル数
        int[] offset_All = new int[fileNum];
        offset_All[0] = jumpID;

        for (int i = 1; i < fileNum; i++){
            offset_All[i] = bb.getInt(); //ファイルパスのオフセット
            bb.getInt(); //不明
        }

        HashDatabase.beginConnection();
        try {
            HashDatabase.setAutoCommit(false);
        } catch (SQLException e1) {
            Utils.getGlobalLogger().error(e1);
        }

        for (int i = 1; i < offset_All.length; i++){
            bb.position(startPosition + 8 * i + offset_All[i]);

            StringBuilder modelStringBld;
            modelStringBld = new StringBuilder();
            while (bb.position() < bb.capacity()) {
                byte c = bb.get();
                if (c == 0) {
                    if (modelStringBld.length() != 0) {
                        break;
                    }
                } else {
                    modelStringBld.append((char) c);
                }
            }
            String modelString = modelStringBld.toString();
            if (modelString.contains(".")) {
                //cutbファイル中のパスを登録
                String archive = HashDatabase.getArchiveID(modelString);
                if (archive.equals("*")){
                    Utils.getGlobalLogger().error(modelString);
                    continue;
                }
                HashDatabase.addPathToDB(modelString, archive);

                if(true) {
                    if (currentIndex.getName().equals(archive)){
                        temp_IndexFile = currentIndex;
                    }else{
                        try {
                            if (sp_IndexFile == null || !sp_IndexFile.getName().equals(archive)) {
                                sp_IndexFile = new SqPack_IndexFile(FileTools.ArchiveID2IndexFilePath(archive), true);
                            }
                            temp_IndexFile = sp_IndexFile;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                    if(false)
                    if (modelString.endsWith(".mdl")) {
                        //mdlファイル内のパスも解析
                        try {
                            byte[] data2 = temp_IndexFile.extractFile(modelString);
                            Model tempModel = new Model(modelString, temp_IndexFile, data2, temp_IndexFile.getEndian());
                            tempModel.loadVariant(1); //mdlファイルに関連するmtrlとtexの登録を試みる。
                        } catch (Exception modelException) {
                            modelException.printStackTrace();
                        }
                    }

                    if (modelString.endsWith(".avfx")) {
                        try {
                            //avfxファイル内のパスも解析
                            byte[] data2 = temp_IndexFile.extractFile(modelString);
                            AVFX_File avfxFile = new AVFX_File(temp_IndexFile, data2, temp_IndexFile.getEndian());
                            avfxFile.regHash(true);
                        } catch (Exception avfxException) {
                            avfxException.printStackTrace();
                        }
                    }
                    if(false)
                    if (modelString.endsWith(".sgb")) {
                        //sgbファイル内のパスも解析
                        try {
                            byte[] data2 = temp_IndexFile.extractFile(modelString);
                            SGB_File sgbFile = new SGB_File(temp_IndexFile, data2, temp_IndexFile.getEndian());
                        } catch (Exception sgbException) {
                            sgbException.printStackTrace();
                        }
                    }
                }
            }

        }

        try {
            HashDatabase.commit();
        } catch (SQLException e) {
            Utils.getGlobalLogger().error(e);
        }
        HashDatabase.closeConnection();
    }

    private static String getString(ByteBuffer buffer) {
        byte[] input = new byte[4];
        buffer.get(input);
        return new String(input);
    }

}
