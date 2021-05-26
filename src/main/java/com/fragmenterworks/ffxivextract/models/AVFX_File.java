package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.FileTools;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.SQLException;
import java.util.ArrayList;

public class AVFX_File extends Game_File {

    //他のファイルを見つけるために使用されます
    private final SqPack_IndexFile currentIndex;

    private final int fileSize;
    private final ArrayList<AVFX_Packet> packets = new ArrayList<>();

    public AVFX_File(SqPack_IndexFile index, byte[] data, ByteOrder endian) {
        super(endian);
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        currentIndex = index;

        String sig = getString(bb,4); //ファイルシグネチャ(reverseでAVFX)
        if (sig.equals("XFVA")) {
            fileSize = bb.getInt(); //シグネチャとサイズデータ(int)を除いたデータサイズ

            while (bb.hasRemaining()) {
                packets.add(new AVFX_Packet(bb));
            }
        }else{
            fileSize = bb.capacity();
        }
    }

    public int getFileSize() {
        return fileSize;
    }

    static class AVFX_Packet {
        final byte[] tag = new byte[4];
        int dataSize;
        final byte[] data;

        AVFX_Packet(ByteBuffer inBuff) {
            inBuff.get(tag);  //データタイプシグネチャ(xeT ,reV ,PFDbなど)

            dataSize = inBuff.getInt(); //シグネチャとサイズデータ(int)を除いたデータサイズ
            if(dataSize > inBuff.limit()){
                //なんらかのエラーが発生してる時ここに誘導
                data = new byte[0];
                return;
            }

            //文字列のデータサイズはすべて違います
            if (tag[0] == 0x78 && tag[1] == 0x65 && tag[2] == 0x54) {
                //xeT :テキスト格納ブロックのシグネチャ(reverseでTex)
                int increment = 0;
                int curPos = inBuff.position();
                inBuff.position(curPos + dataSize); //byteバッファの読み込み開始アドレスをデータサイズ分進める

                while (inBuff.hasRemaining() && inBuff.get() == 0x0) {
                    //文字列末尾の0x00データを検索
                    increment++;
                }

                dataSize += increment;

                inBuff.position(curPos);
            }
            //End this hack

            data = new byte[dataSize];
            inBuff.get(data);

        }

        @Override
        public String toString() {

            StringBuilder string = new StringBuilder();

            for (int i = 0; i < 4; i++) {
                string.append(String.format("%02x, ", data[i]));
            }

            return new StringBuffer(new String(tag)).reverse().toString().trim() + " : " + (data.length == 4 ? string.toString() : "");
        }
    }

    /**
     * AVFXファイル中のパス文字列をHashデータベースに登録する
     */
    public void regHash() {
        regHash(false);
    }

    /**
     * AVFXファイル中のパス文字列をHashデータベースに登録する
     * @param fileCheck true:ファイルチェックをする
     */
    public void regHash(boolean fileCheck) {

        SqPack_IndexFile index = null;

        HashDatabase.beginConnection();
        try {
            HashDatabase.setAutoCommit(false);
        } catch (SQLException e1) {
            Utils.getGlobalLogger().error(e1);
        }

        boolean folderCheck = true;

        for (AVFX_Packet ap : packets){
            String apTag = new StringBuffer(new String(ap.tag)).reverse().toString().trim();
            if (apTag.equals("Tex")){
                String fullPath = new String(ap.data).trim();
                String archive = HashDatabase.getArchiveID(fullPath);
                if (fileCheck) {
                    int pathCheck = 0;
                    if (currentIndex.getName().equals(archive)){
                        pathCheck = currentIndex.existsFile2(fullPath);
                    }else {
                        try {
                            if (index == null || !index.getName().equals(archive)) {
                                index = new SqPack_IndexFile(FileTools.ArchiveID2IndexFilePath(archive), true);
                            }
                            pathCheck = index.existsFile2(fullPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (pathCheck == 2) {
                        HashDatabase.addPathToDB(fullPath, archive);
                    } else if (pathCheck == 1) {
                        //ファイルパスのみ追加
                        String folder;
                        if (fullPath.contains(".")) {
                            folder = fullPath.substring(0, fullPath.lastIndexOf("/"));
                        } else {
                            folder = fullPath;
                        }

                        HashDatabase.addFolderToDB(folder, archive);
                    }
                }else{
                    HashDatabase.addPathToDB(fullPath, archive);
                }

                if (folderCheck){
                    //自ファイル名を推測して登録する
                    String folder = fullPath.substring(0, fullPath.lastIndexOf('/')).replace("/texture", "/eff");
                    if (folder.startsWith("vfx/")){
                        String[] pathParts = fullPath.split("/");

                        switch (pathParts[1]) {
                            case "cut":
                                for (int i = 1; i < 50; i++) {
                                    //vfx/cut/anvwil/anvwil00510/eff/anvwil00510_01a.avfx
                                    String avfxPath = String.format("%s/%s_%02da.avfx", folder, pathParts[3], i);
                                    int pathCheck = currentIndex.existsFile2(avfxPath);
                                    if (pathCheck == 2) {
                                        HashDatabase.addPathToDB(avfxPath, archive);
                                    }
                                }
                                break;
                            case "action": {
                                String[] actionSign = new String[]{"c", "t"};
                                for (int i = 0; i < 9; i++) {
                                    for (String sign1 : actionSign) {
                                        //vfx/action/ab_2kt010/eff/ab_2kt010c0t.avfx
                                        String avfxPath = String.format("%s/%s%s%dt.avfx", folder, pathParts[2], sign1, i);
                                        int pathCheck = currentIndex.existsFile2(avfxPath);
                                        if (pathCheck == 2) {
                                            HashDatabase.addPathToDB(avfxPath, archive);
                                        }
                                    }
                                }
                                break;
                            }
                            case "temporary": {
                                String[] tempSign = new String[]{"c", "t"};
                                String[] tempSign2 = new String[]{"w", "a1", "p", "m1"};
                                for (int i = 0; i < 3; i++) {
                                    for (String sign1 : tempSign) {
                                        for (String sign2 : tempSign2) {
                                            //vfx/temporary/abl_myc016/eff/abl_myc016_c0w.avfx
                                            String avfxPath = String.format("%s/%s_%s%d%s.avfx", folder, pathParts[2], sign1, i, sign2);
                                            int pathCheck = currentIndex.existsFile2(avfxPath);
                                            if (pathCheck == 2) {
                                                HashDatabase.addPathToDB(avfxPath, archive);
                                            }else{
                                                avfxPath = String.format("%s/%s%s%d%s.avfx", folder, pathParts[2], sign1, i, sign2);
                                                pathCheck = currentIndex.existsFile2(avfxPath);
                                                if (pathCheck == 2) {
                                                    HashDatabase.addPathToDB(avfxPath, archive);
                                                }
                                            }

                                        }
                                    }
                                }
                                break;
                            }
                            default:
                                //パスのみ登録
                                HashDatabase.addFolderToDB(folder, archive);
                                break;
                        }

                    }else{
                        //パスのみ登録
                        HashDatabase.addFolderToDB(folder, archive);
                    }
                    folderCheck = false;
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

    public void printOut() {
        packets.forEach(ap -> Utils.getGlobalLogger().trace(ap));
    }

    /**
     * Convenience function for reading a fixed length string from a ByteBuffer
     *
     * @param buffer     The buffer to read from
     * @param byteLength The number of bytes to read
     * @return A new non-trimmed string read from the given buffer with the given length.
     */
    private static String getString(ByteBuffer buffer, int byteLength) {
        byte[] input = new byte[byteLength];
        buffer.get(input);
        return new String(input);
    }
}
