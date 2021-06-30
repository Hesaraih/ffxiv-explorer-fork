package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LcbFile extends Game_File {

    //region Struct
    public static class HeaderData {
        public String Magic1;  // LCB1
        public int FileSize;   //uint
        public int Unknown1;   //uint
        public String Magic2;  // LCC1

        public int LCC1_Size;   //uint
        public int SharedOffset;
        public int EntryOffset;   //uint

        public int EntryCount;   //uint
    }
    //endregion

    public HeaderData Header;
    public LccGroup[] Entries;

    public LcbFile(byte[] data, ByteOrder endian){
        super(endian);
        loadLCB(data);
    }

    private void loadLCB(byte[] data) {
        byte[] signature = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        //region SGBファイル ヘッダー
        Header = new HeaderData();
        bb.get(signature); //SGB1
        Header.Magic1 = new String(signature).trim();
        Header.FileSize = bb.getInt(); //ファイルサイズ
        Header.Unknown1 = bb.getInt(); //不明 0x01 SCN1等のチャンク数
        bb.get(signature); //SCN1
        Header.Magic2 = new String(signature).trim();
        Header.LCC1_Size = bb.getInt(); //LCC1のヘッダ部サイズ　アドレス: 0x10
        Header.SharedOffset = bb.getInt(); //共有データファイルのオフセット
        Header.EntryOffset = bb.getInt(); //不明 0x01
        Header.EntryCount = bb.getInt(); //エントリー数

        if (!Header.Magic1.equals("LCB1")) {
            Utils.getGlobalLogger().error("LCB1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic1);
        } else if (!Header.Magic2.equals("LCC1")) {
            Utils.getGlobalLogger().error("LCC1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic2);
        }

        Entries = new LccGroup[Header.EntryCount];
        for (int i = 0; i < Header.EntryCount; i++){
            Entries[i] = new LccGroup(bb);
        }

        Utils.getGlobalLogger().trace("Lcc Load");
    }

    public static class LccGroup{
        public static class HeaderData {
            public int Unknown1;
            public int Unknown2;
            public Vector3 UnknownFloat3;
            public Vector3 UnknownFloat4;
        }

        public HeaderData Header = new HeaderData();

        public LccGroup(ByteBuffer bb){
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.UnknownFloat3 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownFloat4 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
        }
    }
}
