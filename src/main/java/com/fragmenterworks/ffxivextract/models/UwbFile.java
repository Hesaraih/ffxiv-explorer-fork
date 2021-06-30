package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UwbFile extends Game_File {

    //region Struct
    public static class HeaderData {
        public String Magic1;  // LCB1
        public int FileSize;   //uint
        public int Unknown1;   //uint
        public String Magic2;  // LCC1

        public int UWC1_Size;   //uint
        public int SharedOffset;
        public int EntryOffset;   //uint
    }
    //endregion

    public HeaderData Header;
    public UwcGroup Entry;

    public UwbFile(byte[] data, ByteOrder endian){
        super(endian);
        loadUWB(data);
    }

    private void loadUWB(byte[] data) {
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
        Header.UWC1_Size = bb.getInt(); //LCC1のヘッダ部サイズ　アドレス: 0x10
        Header.SharedOffset = bb.getInt(); //共有データファイルのオフセット
        Header.EntryOffset = bb.getInt(); //不明 0x01

        if (!Header.Magic1.equals("UWB1")) {
            Utils.getGlobalLogger().error("UWB1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic1);
        } else if (!Header.Magic2.equals("UWC1")) {
            Utils.getGlobalLogger().error("UWC1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic2);
        }

        Entry = new UwcGroup(bb);

        Utils.getGlobalLogger().trace("Uwc Load");
    }

    public static class UwcGroup {
        public static class HeaderData {
            public Vector3 UnknownFloat1;
            public Vector3 UnknownFloat2;
            public Vector3 UnknownFloat3;
            public Vector3 UnknownFloat4;
            public Vector3 UnknownFloat5;
            public Vector3 UnknownFloat6;
        }

        public HeaderData Header = new HeaderData();

        public UwcGroup(ByteBuffer bb){
            Header.UnknownFloat1 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownFloat2 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownFloat3 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownFloat4 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownFloat5 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownFloat6 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
        }
    }
}
