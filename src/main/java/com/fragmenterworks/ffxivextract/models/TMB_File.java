package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TMB_File extends Game_File {
    //avfxファイルに似てる

    //他のファイルを見つけるために使用されます
    private final SqPack_IndexFile currentIndex; //現在表示中または呼び出し元のIndexファイル
    private SqPack_IndexFile sp_IndexFile; //上記以外のIndexファイル

    /**
     * コンストラクタ
     * @param data sgbデータ
     * @param endian エンディアンの種類
     */
    public TMB_File(SqPack_IndexFile index, byte[] data, ByteOrder endian)  throws IOException{
        super(endian);
        this.currentIndex = index;
        try {
            loadTMB(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private void loadTMB(byte[] data)  throws IOException{
        byte[] signature = new byte[4];
        byte[] signature2 = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        String signatureString = getString(bb); //TMLB
        int fileSize = bb.getInt(); //ファイルサイズ
        int chunkMax = bb.getInt(); //データチャンク数
        if (!signatureString.equals("TMLB")) {
            Utils.getGlobalLogger().error("TMLB magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", signatureString);
        }

        for (int chunkNum = 0; chunkNum < chunkMax; chunkNum++){
            int chunkStartOffset = bb.position();
            String signatureString2 = getString(bb);  //TMDH等
            int chunkSize = bb.getInt(); //チャンクヘッダサイズ
            if (signatureString2.equals("TMAL")) {
                int tmalOffset = bb.getInt(); //TMALオフセット
                bb.getInt(); //チャンクID？
                if (tmalOffset != 1) {
                    bb.position(chunkStartOffset + tmalOffset);
                    break;
                }
            }else {
                bb.getInt(); //不明 チャンクID？ TMAL以外
            }
            bb.getInt(); //不明 オフセット？
            if (signatureString2.equals("TMAC")) {
                bb.getInt(); //不明
                int tmacDataSize = bb.getInt(); //TMACデータ部サイズ？
                bb.getInt(); //TMTRチャンク数
                //bb.position(chunkStartOffset + chunkSize + tmacDataSize);
            }
            bb.position(chunkStartOffset + chunkSize);
        }

        while (bb.position() < bb.capacity()) {
            int nowPosition = bb.position();
            int jumpID = bb.getShort();
            if (jumpID == 0) {
                //4byteデータ *n個 (ない場合もある)
                bb.getShort();
            } else if (jumpID >= chunkMax - 1){
                if (jumpID != chunkMax - 1) {
                    //chunkMax - 1 のIDが抜けていた場合
                    bb.position(nowPosition);
                }
                break;
            }
        }

        //作業用
        SqPack_IndexFile temp_IndexFile = currentIndex;
        //文字列
        while (bb.position() < bb.capacity()){
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
                //tmbファイル中のパスを登録
                String archive = HashDatabase.getArchiveID(modelString);
                if (!archive.equals("*")) {
                    HashDatabase.addPathToDB(modelString, archive);
                    if (currentIndex.getName().equals(archive)) {
                        temp_IndexFile = currentIndex;
                    } else {
                        try {
                            if (sp_IndexFile == null || !sp_IndexFile.getName().equals(archive)) {
                                sp_IndexFile = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\" + archive + ".win32.index", true);
                            }
                            temp_IndexFile = sp_IndexFile;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                    if (modelString.endsWith(".mdl")) {
                        //mdlファイル内のパスも解析
                        try {
                            byte[] data2 = temp_IndexFile.extractFile(modelString);
                            Model tempModel = new Model(modelString, temp_IndexFile, data2, temp_IndexFile.getEndian());
                            tempModel.loadVariant(1); //mdlファイルに関連するmtrlとtexの登録を試みる。
                        } catch (Exception modelException) {
                            modelException.printStackTrace();
                        }
                    } else if (modelString.endsWith(".avfx")) {
                        try {
                            //avfxファイル内のパスも解析
                            byte[] data2 = temp_IndexFile.extractFile(modelString);
                            AVFX_File avfxFile = new AVFX_File(temp_IndexFile, data2, temp_IndexFile.getEndian());
                            avfxFile.regHash(true);
                        } catch (Exception avfxException) {
                            avfxException.printStackTrace();
                        }
                    }
                }else{
                    Utils.getGlobalLogger().error("TMBファイルの定義が間違っているかも");
                }
            }
        }
    }

    private static String getString(ByteBuffer buffer) {
        byte[] input = new byte[4];
        buffer.get(input);
        return new String(input);
    }

}
