package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SvbFile extends Game_File {

    //region Struct
    public static class HeaderData {
        public String Magic1;  // LCB1
        public int FileSize;   //uint
        public int Unknown1;   //uint
        public String Magic2;  // LCC1

        public int SVC1_Size;   //uint
        public int SharedOffset;
        public int EntryOffset;   //uint

        public int EntryCount;   //uint
    }
    //endregion

    public HeaderData Header;
    public LccGroup[] Entries;

    public SvbFile(byte[] data, ByteOrder endian){
        super(endian);
        loadLCB(data);
    }

    private void loadLCB(byte[] data) {
        byte[] signature = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        //region SGBファイル ヘッダー
        Header = new HeaderData();
        bb.get(signature); //SVB1
        Header.Magic1 = new String(signature).trim();
        Header.FileSize = bb.getInt(); //ファイルサイズ
        Header.Unknown1 = bb.getInt(); //不明 0x01 SCN1等のチャンク数
        bb.get(signature); //SVC1
        Header.Magic2 = new String(signature).trim();
        Header.SVC1_Size = bb.getInt(); //SVC1のヘッダ部サイズ　アドレス: 0x10
        Header.SharedOffset = bb.getInt(); //共有データファイルのオフセット
        Header.EntryOffset = bb.getInt(); //不明 0x01
        Header.EntryCount = bb.getInt(); //エントリー数

        if (!Header.Magic1.equals("SVB1")) {
            Utils.getGlobalLogger().error("SVB1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic1);
        } else if (!Header.Magic2.equals("SVC1")) {
            Utils.getGlobalLogger().error("SVC1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic2);
        }

        Entries = new LccGroup[Header.EntryCount];
        for (int i = 0; i < Header.EntryCount; i++){
            Entries[i] = new LccGroup(bb);
        }

        Utils.getGlobalLogger().trace("Svc Load");
    }

    public static class LccGroup{
        public static class HeaderData {
            public Vector3 UnknownFloat;
        }

        public HeaderData Header = new HeaderData();

        public LccGroup(ByteBuffer bb){
            Header.UnknownFloat = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
        }
    }
}
