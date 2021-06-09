package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.FileTools;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SGB_File extends Game_File {

    @SuppressWarnings("unused")
    public String entryName;
    @SuppressWarnings("unused")
    public String modelName;
    @SuppressWarnings("unused")
    public String collisionName;

    public String SGB_FileName;

    //他のファイルを見つけるために使用されます
    private SqPack_IndexFile currentIndex; //現在表示中または呼び出し元のIndexファイル
    private SqPack_IndexFile sp_IndexFile; //上記以外のIndexファイル
    @SuppressWarnings("FieldCanBeLocal")
    private SqPack_IndexFile temp_IndexFile; //作業用

    @SuppressWarnings("unused")
    public SGB_File(String path, ByteOrder endian) throws IOException {
        super(endian);
        File file = new File(path);
        byte[] data;
        try (FileInputStream fis = new FileInputStream(file)) {
            data = new byte[(int) file.length()];
            while (fis.read(data) != -1) {
                Utils.getGlobalLogger().debug("SGB読み取り");
            }
        }
        loadSGB(data);
    }

    /**
     * コンストラクタ
     * @param data sgbデータ
     * @param endian エンディアンの種類
     * @throws IOException エラー
     */
    public SGB_File(SqPack_IndexFile index, byte[] data, ByteOrder endian) throws IOException {
        super(endian);
        this.currentIndex = index;
        loadSGB(data);
    }

    @SuppressWarnings("unused")
    private void loadSGB(byte[] data) {
        byte[] signature = new byte[4];
        byte[] signature2 = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        bb.get(signature); //SGB1
        int fileSize = bb.getInt(); //ファイルサイズ
        bb.getInt(); //不明 0x01
        bb.get(signature2); //SCN1
        int SCN1_Size = bb.getInt(); //SCN1のヘッダ部サイズ 0x48固定？　アドレス: 0x10
        int SharedOffset = bb.getInt(); //SCN1のデータ部サイズ？
        bb.getInt(); //不明 0x01
        int Offset1C = bb.getInt(); //何かのサイズ
        bb.getInt(); //何かのサイズ
        int StatesOffset = bb.getInt(); //0x0060からの実データサイズ？ 　アドレス: 0x28   ※0x60 + StatesOffset + 0x20でShared文字列

        bb.position(0x000C + SCN1_Size);
        bb.getInt(); //シグニチャ
        int block_Size = bb.getInt();

        String signatureString = new String(signature).trim();
        String signatureString2 = new String(signature2).trim();

        if (!signatureString.equals("SGB1")) {
            Utils.getGlobalLogger().error("SGB1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", signatureString);
        } else if (!signatureString2.equals("SCN1")) {
            Utils.getGlobalLogger().error("SCN1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", signatureString2);
        }

        //bb.position(0x0060 + StatesOffset + 0x20);
        bb.position(0x000C + SCN1_Size + block_Size);
        int modelStrType = 0;
        String sgbPath = "";
        temp_IndexFile = currentIndex;
        do {
            StringBuilder modelStringBld;
            modelStringBld = new StringBuilder();
            while (bb.position() < bb.capacity()) {
                byte c = bb.get();
                if (c < 0x20) {
                    if (modelStringBld.length() != 0) {
                        break;
                    }
                } else {
                    modelStringBld.append((char) c);
                }
            }

            if (modelStringBld.length() == 0) {
                break;
            }else if (modelStringBld.length() <= 5){
                continue;
            }

            String modelString = modelStringBld.toString();
            if (modelString.equals("Shared") && modelStrType == 0) {
                modelStrType = 1;
            }else if (modelStrType == 1){
                SGB_FileName = modelString;
                modelStrType = 2;
            }else if (modelStrType == 2){
                //sgbファイル中のパスを登録してみる
                if (modelString.contains(".")) {
                    String archive = HashDatabase.getArchiveID(modelString);
                    HashDatabase.addPathToDB(modelString, archive);
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
                    if (modelString.endsWith(".sgb")) {
                        sgbPath = modelString.substring(0, modelString.lastIndexOf('/'));
                        //sgbファイル内の入れ子パスも解析
                        try {
                            byte[] data2 = temp_IndexFile.extractFile(modelString);
                            SGB_File sgbFile = new SGB_File(temp_IndexFile, data2, temp_IndexFile.getEndian());
                        } catch (Exception sgbException) {
                            sgbException.printStackTrace();
                        }
                    }else if (modelString.endsWith(".mdl")) {
                        //mdlファイル内のパスも解析
                        try {
                            byte[] data2 = temp_IndexFile.extractFile(modelString);
                            Model tempModel = new Model(modelString, temp_IndexFile, data2, temp_IndexFile.getEndian());
                            tempModel.loadVariant(1); //mdlファイルに関連するmtrlとtexの登録を試みる。
                        } catch (Exception modelException) {
                            modelException.printStackTrace();
                        }
                    }else if (modelString.endsWith(".avfx")) {
                        try {
                            //avfxファイル内のパスも解析
                            byte[] data2 = temp_IndexFile.extractFile(modelString);
                            AVFX_File avfxFile = new AVFX_File(temp_IndexFile, data2, temp_IndexFile.getEndian());
                            avfxFile.regHash(true);
                        } catch (Exception avfxException) {
                            avfxException.printStackTrace();
                        }
                    }

                }else if (modelString.contains("/")) {
                    if (sgbPath.startsWith(modelString)){
                        //同じディレクトリのsgbファイルがあるようなら自ファイルも登録してみる
                        String fullPath = sgbPath + "/" + SGB_FileName + ".sgb";
                        String archive = HashDatabase.getArchiveID(fullPath);
                        HashDatabase.addPathToDB(fullPath, archive);
                    }else{
                        //なければ適当な既知のパス名でsgbのファイル名だけ登録してみる
                        String fullPath = "bgcommon/world/evt/shared/for_bg/" + SGB_FileName + ".sgb";
                        String archive = HashDatabase.getArchiveID(modelString);
                        HashDatabase.addPathToDB(fullPath, archive);
                    }
                }
            }


        } while (bb.position() < bb.capacity());
    }
}
