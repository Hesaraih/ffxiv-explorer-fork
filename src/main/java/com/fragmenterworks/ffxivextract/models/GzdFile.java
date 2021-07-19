package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.ByteArrayExtensions;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GzdFile extends Game_File {
    private final String FilePath;

    public GzdFile (byte[] data, ByteOrder endian, String filePath){
        super(endian);
        FilePath = filePath;
        loadGzd(data);
    }

    //region Struct
    public static class HeaderData {
        public String Magic1;  // gzd
        public short Unknown04;   //ushort
        public short Unknown06;   //ushort
        public short Unknown08;   //ushort
        public short Unknown0A;   //ushort
        public short Unknown0C;   //ushort
        public byte Unknown0E;   //byte
        public byte EntryCount;   //byte
    }
    //endregion

    public HeaderData Header;

    private void loadGzd(byte[] data) {
        final int GrassOffset = 0x10;
        final int ModelFileOffset = 0x50;
        final int ModelFileEntrySize = 0x100;

        Header = new HeaderData();

        byte[] signature = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        bb.get(signature);
        Header.Magic1 = new StringBuffer(new String(signature)).reverse().toString().trim(); //gzd
        Header.Unknown04 = bb.getShort();
        Header.Unknown06 = bb.getShort();
        Header.Unknown08 = bb.getShort();
        Header.Unknown0A = bb.getShort();
        Header.Unknown0C = bb.getShort();
        Header.Unknown0E = bb.get();
        Header.EntryCount = bb.get();

        if (!Header.Magic1.equals("gzd")) {
            Utils.getGlobalLogger().error("GZD magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic1);
        }

        String folder = FilePath.substring(0, FilePath.lastIndexOf('/'));

        String Name;
        for (int i = 0; i < 2; i++) {
            Name = ByteArrayExtensions.ReadString(bb, GrassOffset + Header.Unknown0E * i);
            if (!Name.equals("")) {
                if (!folder.contains("~")){
                    String texFilePath = folder + "/" + Name + ".tex";
                    cAddPathToDB(texFilePath);
                }
            }
            bb.position(GrassOffset + Header.Unknown0E * (i + 1));
        }

        for (int i = 0; i < Header.EntryCount; i++) {
            Name = ByteArrayExtensions.ReadString(bb, ModelFileOffset + ModelFileEntrySize * i);
            if (!Name.equals("")) {
                cAddPathToDB(Name);
            }
            bb.position(ModelFileOffset + ModelFileEntrySize * (i + 1));
        }

        HashDatabase.setAutoCommit(false);
        do{
            new GzdData(bb, folder);
        }while (bb.position() < bb.limit());
        HashDatabase.commit();
    }

    public class GzdData{
        public class HeaderData {
            public Vector4 UnknownFloat;
            public byte GlassType;
            public int GlassPos_C; //ubyte
            public int GlassPos_B; //ubyte
            public int GlassPos_A; //ubyte
        }

        public HeaderData Header = new HeaderData();

        public GzdData(ByteBuffer bb, String Folder){
            Header.UnknownFloat = new Vector4(bb.getFloat(), bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.GlassType = bb.get();
            Header.GlassPos_C = Byte.toUnsignedInt(bb.get());
            Header.GlassPos_B = Byte.toUnsignedInt(bb.get());
            Header.GlassPos_A = Byte.toUnsignedInt(bb.get());

            //bg/ex3/01_nvt_n4/dun/n4db/grass/000_003_027_h.ggd
            String ggdName = "";
            switch (Header.GlassType) {
                case 0:
                    //High
                    ggdName = String.format("%03d_%03d_%03d_h.ggd", Header.GlassPos_A, Header.GlassPos_B, Header.GlassPos_C);
                    break;
                case 1:
                    //Medium
                    ggdName = String.format("%03d_%03d_%03d_m.ggd", Header.GlassPos_A, Header.GlassPos_B, Header.GlassPos_C);
                    break;
                case 2:
                    //Low
                    ggdName = String.format("%03d_%03d_%03d_l.ggd", Header.GlassPos_A, Header.GlassPos_B, Header.GlassPos_C);
                    break;
            }

            if (!ggdName.equals("")) {
                if (!Folder.contains("~")){
                    String texFilePath = Folder + "/" + ggdName;
                    cAddPathToDB(texFilePath);
                }
            }


        }
    }


    //region ハッシュデータベース登録関係
    /**
     * ファイルの存在チェック後、ハッシュデータベース登録
     * (フォルダ名の一致のみでも登録する。)
     * @param fullPath フルパス
     * @return 登録結果 0:登録失敗 1:登録成功 2:ファイル名変更 3:ファイルパス変更 4:登録済みのため何もしない
     */
    @SuppressWarnings("UnusedReturnValue")
    private int cAddPathToDB(String fullPath){
        if(fullPath.length() == 0){
            //ここでnullチェックしておく
            return 4;
        }else{
            String archive = HashDatabase.getArchiveID(fullPath);
            if (archive.equals("*")){
                return 0;
            }
            return cAddPathToDB(fullPath, archive);
        }
    }

    /**
     * ファイルの存在チェック後、ハッシュデータベース登録
     * @param fullPath フルパス
     * @param archive Indexファイル名
     * @return 登録結果 0:登録失敗 1:登録成功 2:ファイル名変更 3:ファイルパス変更 4:登録済みのため何もしない
     */
    @SuppressWarnings("SameParameterValue")
    private int cAddPathToDB(String fullPath, String archive){
        int result = 0;
        SqPack_IndexFile temp_IndexFile = SqPack_IndexFile.GetIndexFileForArchiveID(archive, false);

        if (temp_IndexFile == null){
            return 0;
        }

        int pathCheck = temp_IndexFile.findFile(fullPath);
        if (pathCheck == 2){
            result = HashDatabase.addPathToDB(fullPath, archive);

            if (result == 1) {
                if (fullPath.endsWith(".mdl")) {
                    //mdlファイル内のパスも解析
                    byte[] data2 = temp_IndexFile.extractFile(fullPath);
                    Model tempModel = new Model(fullPath, temp_IndexFile, data2, temp_IndexFile.getEndian());
                    tempModel.loadVariant(1); //mdlファイルに関連するmtrlとtexの登録を試みる。
                } else if (fullPath.endsWith(".avfx")) {
                    //avfxファイル内のパスも解析
                    byte[] data2 = temp_IndexFile.extractFile(fullPath);
                    AVFX_File avfxFile = new AVFX_File(temp_IndexFile, data2, temp_IndexFile.getEndian());
                    avfxFile.regHash(true);
                } else if (fullPath.endsWith(".sgb")) {
                    //sgbファイル内のパスも解析
                    byte[] data2 = temp_IndexFile.extractFile(fullPath);
                    new SgbFile(data2, temp_IndexFile.getEndian());
                }
            }
        }
        return result;
    }
    //endregion

}
