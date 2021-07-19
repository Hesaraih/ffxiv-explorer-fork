package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.FileTools;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class AVFX_File extends Game_File {

    //他のファイルを見つけるために使用されます
    private final SqPack_IndexFile currentIndex;

    //region Struct
    public static class HeaderData {
        public String Magic1;  // CUTB
        public int FileSize;   //uint
    }
    //endregion

    public HeaderData Header;
    private final ArrayList<AVFX_Packet> packets = new ArrayList<>();

    public AVFX_File(SqPack_IndexFile index, byte[] data, ByteOrder endian) {
        super(endian);
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        currentIndex = index;

        Header = new HeaderData();

        byte[] signature = new byte[4];
        bb.get(signature);
        Header.Magic1 = new String(signature).trim(); //ファイルシグネチャ(reverseでAVFX)
        if (Header.Magic1.equals("XFVA")) {
            Header.FileSize = bb.getInt(); //シグネチャとサイズデータ(int)を除いたデータサイズ

            while (bb.hasRemaining()) {
                packets.add(new AVFX_Packet(bb));
            }
        }else{
            Header.FileSize = bb.capacity();
        }
    }

    public static class AVFX_Packet {
        public static class HeaderData{
            public String TagName;
            public int DataSize;
        }

        public HeaderData Header = new HeaderData();

        final byte[] tag = new byte[4];
        int dataSize;
        final byte[] data;

        AVFX_Packet(ByteBuffer bb) {
            bb.get(tag);  //データタイプシグネチャ(xeT ,reV ,PFDbなど)
            Header.TagName = new StringBuffer(new String(tag)).reverse().toString().trim();

            Header.DataSize = bb.getInt(); //シグネチャとサイズデータ(int)を除いたデータサイズ
            dataSize = Header.DataSize;
            if(Header.DataSize > bb.limit()){
                //なんらかのエラーが発生してる時ここに誘導
                data = new byte[0];
                return;
            }

            //パス文字列のデータサイズは実際のサイズ+空データ(1～3byte)で4の倍数になるように調整されている
            if (Header.TagName.equals("Tex")) {
                //テクスチャファイルのパス
                int increment = 0;
                int BaseOffset = bb.position();
                bb.position(BaseOffset + Header.DataSize); //byteバッファの読み込み開始アドレスをデータサイズ分進める

                while (bb.hasRemaining() && bb.get() == 0x0) {
                    //文字列末尾の0x00データを検索
                    increment++;
                }

                dataSize += increment;

                bb.position(BaseOffset);
            }
            //End this hack

            data = new byte[dataSize];
            bb.get(data);

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
     * @param fileCheck true:ファイル存在チェックをする
     */
    public void regHash(boolean fileCheck) {

        SqPack_IndexFile index = null;

        boolean folderCheck = true;

        for (AVFX_Packet ap : packets){
            String apTag = new StringBuffer(new String(ap.tag)).reverse().toString().trim();
            if (apTag.equals("Tex")){
                //テクスチャファイル関係
                String fullPath = new String(ap.data).trim();
                String archive = HashDatabase.getArchiveID(fullPath);
                if (fileCheck) {
                    int pathCheck;
                    if (currentIndex.getName().equals(archive)){
                        pathCheck = currentIndex.findFile(fullPath);
                    }else {
                        if (index == null || !index.getName().equals(archive)) {
                            index = new SqPack_IndexFile(FileTools.ArchiveID2IndexFilePath(archive), true);
                        }
                        pathCheck = index.findFile(fullPath);
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
                                    int pathCheck = currentIndex.findFile(avfxPath);
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
                                        int pathCheck = currentIndex.findFile(avfxPath);
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
                                            int pathCheck = currentIndex.findFile(avfxPath);
                                            if (pathCheck == 2) {
                                                HashDatabase.addPathToDB(avfxPath, archive);
                                            }else{
                                                avfxPath = String.format("%s/%s%s%d%s.avfx", folder, pathParts[2], sign1, i, sign2);
                                                pathCheck = currentIndex.findFile(avfxPath);
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
    }

    @SuppressWarnings("unused")
    public void printOut() {
        packets.forEach(ap -> Utils.getGlobalLogger().trace(ap));
    }

}
