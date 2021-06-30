package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.helpers.ByteArrayExtensions;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;

public class ENVB_File extends Game_File {

    //他のファイルを見つけるために使用されます
    private final SqPack_IndexFile currentIndex; //現在表示中または呼び出し元のIndexファイル
    private static SqPack_IndexFile bgcommonIndex;

    //region Struct
    public static class HeaderData {
        public String Magic1;  // ESSB
        public int FileSize;   //uint
        public int Unknown1;   //uint
        public String Magic2;  // ENVS

        public int ENVS_Size;   //uint
        public int UnknownOffset1;
        public int SoundOffset;
        public int EntryCount;   //uint
        public int DataSize;

        public int EntryOffset;   //uint
    }
    //endregion

    public HeaderData Header;
    public ENVS_Entry[] Entry;

    /**
     * コンストラクタ
     * @param data sgbデータ
     * @param endian エンディアンの種類
     */
    public ENVB_File(SqPack_IndexFile index, byte[] data, ByteOrder endian) {
        super(endian);
        this.currentIndex = index;
        Build(data);
    }

    private void Build(byte[] data){
        final int BaseOffset = 0x14;
        Header = new HeaderData();
        @SuppressWarnings("unused")
        byte[] signature = new byte[4];
        @SuppressWarnings("unused")
        byte[] signature2 = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);

        bb.get(signature);
        Header.Magic1 = new String(signature).trim(); //ENVB,ESSB,OBSBなど
        Header.FileSize = bb.getInt(); //ファイルサイズ
        Header.Unknown1 = bb.getInt(); //データチャンク数
        bb.get(signature);
        Header.Magic2 = new String(signature).trim();  // ENVS

        Header.ENVS_Size = bb.getInt();   //uint
        Header.UnknownOffset1 = bb.getInt();
        Header.SoundOffset = bb.getInt();
        Header.EntryCount = bb.getInt();   //uint
        Header.DataSize = bb.getInt();

        Header.EntryOffset = bb.getInt();   //uint


        if (!Header.Magic1.equals("ENVB") && !Header.Magic1.equals("ESSB") && !Header.Magic1.equals("OBSB")) {
            Utils.getGlobalLogger().error("ENVB関係のファイルではありません");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic1);
        }


        Entry = new ENVS_Entry[Header.EntryCount];
        for (int i = 0; i < Header.EntryCount; i++){
            int entriesOffset = BaseOffset + Header.SoundOffset + 0x10 * i;
            Entry[i] = new ENVS_Entry(bb, entriesOffset);
        }
        Utils.getGlobalLogger().trace("env読み込み完了");
    }

    /**
     * ENVS_Entryの構造体
     */
    public class ENVS_Entry {
        public class HeaderData {
            public int EntryOffset;
            public int EntryCount;
            public int SumID;
            public int SubEntryOffset;
        }

        public HeaderData Header;
        public IEnvsGroupEntry[] Entries;
        public EnvsSubEntry SubEntry;

        ENVS_Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header = new HeaderData();
            Header.EntryOffset = bb.getInt();
            Header.EntryCount = bb.getInt();
            Header.SumID = bb.getInt();
            Header.SubEntryOffset = bb.getInt();

            int entryOffset = offset + Header.EntryOffset;
            bb.position(entryOffset);
            int SubEntryOffset = offset + Header.SubEntryOffset;

            Entries = new IEnvsGroupEntry[Header.EntryCount];

            for (int i = 0; i < Header.EntryCount; i++) {
                Entries[i] = new EnvsDataEntry(bb, entryOffset + i * 0x0C);
            }

            SubEntry = new EnvsSubEntry(bb, SubEntryOffset);

        }


    }

    public class EnvsDataEntry implements IEnvsGroupEntry {
        public class HeaderData {
            public int EntriesBaseOffset;
            public int FileGroupCount;  //uint
            public EnvsGroupEntryType Type;
        }

        public EnvsGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public IEnvsGroupEntry[] Entry;

        public EnvsDataEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.EntriesBaseOffset = bb.getInt();
            Header.FileGroupCount = bb.getInt();
            Header.Type = EnvsGroupEntryType.valueOf(bb.getInt());

            int BaseOffset = offset + Header.EntriesBaseOffset;

            Entry = new IEnvsGroupEntry[Header.FileGroupCount];
            for (int i = 0; i < Header.FileGroupCount; i++) {
                bb.position(BaseOffset + i * 4);
                int entryOffset = BaseOffset + bb.getInt();
                switch (Header.Type) {
                    case Unknown00:
                        Entry[i] = new Group00Entry(bb, entryOffset);
                        break;
                    case Unknown01:
                        Entry[i] = new Group01Entry(bb, entryOffset);
                        break;
                    case Unknown02:
                        Entry[i] = new Group02Entry(bb, entryOffset);
                        break;
                    case Unknown03:
                        Entry[i] = new Group03Entry(bb, entryOffset);
                        break;
                    case Unknown04:
                        Entry[i] = new Group04Entry(bb, entryOffset);
                        break;
                    case Unknown05:
                        Entry[i] = new Group05Entry(bb, entryOffset);
                        break;
                    case Unknown06:
                        Entry[i] = new Group06Entry(bb, entryOffset);
                        break;
                    case Unknown07:
                        Entry[i] = new Group07Entry(bb, entryOffset);
                        break;
                    case Unknown08:
                        //0x14 byte
                        Entry[i] = new Group08Entry(bb, entryOffset);
                        break;
                    case Unknown09:
                        Entry[i] = new Group09Entry(bb, entryOffset);
                        break;
                    case VfxGroup:
                        Entry[i] = new VfxGroupEntry(bb, entryOffset);
                        break;
                    case Unknown0A:
                        Entry[i] = new Group0AEntry(bb, entryOffset);
                        break;
                    case Unknown0C:
                        //0Dと同じサイズだがデータ型が違うっぽい
                        Entry[i] = new Group0CEntry(bb, entryOffset);
                        break;
                    case Unknown0D:
                        Entry[i] = new Group0DEntry(bb, entryOffset);
                        break;
                    case SoundGroup:
                        Entry[i] = new SoundGroupEntry(bb, entryOffset);
                        break;
                    case Unknown1D:
                        Entry[i] = new Group1DEntry(bb, entryOffset);
                        break;
                    case Unknown1E:
                        Entry[i] = new Group1EEntry(bb, entryOffset);
                        break;
                    case Unknown1F:
                        Entry[i] = new Group1FEntry(bb, entryOffset);
                        break;
                    case Unknown20:
                        Entry[i] = new Group20Entry(bb, entryOffset);
                        break;
                    case Unknown21:
                        Entry[i] = new Group21Entry(bb, entryOffset);
                        break;
                    case Unknown22:
                        Entry[i] = new Group22Entry(bb, entryOffset);
                        break;
                    case Unknown23:
                        Entry[i] = new Group23Entry(bb, entryOffset);
                        break;
                    default:
                        Utils.getGlobalLogger().info(String.format("%sのEntry解析は未実装", Header.Type.name()));
                        break;
                }
            }
        }
    }

    public static class Group00Entry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public int Unknown1;
            public Vector3 UnknownVector1;
            public int Unknown2;
            public int Unknown3;
            public Vector2 UnknownVector2;
            public int Unknown4;
            public float UnknownFloat2;
            public short UnknownShort1;
            public short UnknownShort2;
            public float UnknownFloat3;
            public short UnknownShort3;
            public short UnknownShort4;
            public float UnknownFloat4;
            public short UnknownShort5;
            public short UnknownShort6;
            public float UnknownFloat5;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown00;
        }

        public HeaderData Header = new HeaderData();

        public Group00Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.Unknown1 = bb.getInt();
            Header.UnknownVector1 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.UnknownVector2 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.Unknown4 = bb.getInt();
            Header.UnknownFloat2 = bb.getFloat();
            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.UnknownFloat3 = bb.getFloat();
            Header.UnknownShort3 = bb.getShort();
            Header.UnknownShort4 = bb.getShort();
            Header.UnknownFloat4 = bb.getFloat();
            Header.UnknownShort5 = bb.getShort();
            Header.UnknownShort6 = bb.getShort();
            Header.UnknownFloat5 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group00Entry");
        }
    }

    public static class Group01Entry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public Vector2 UnknownVector1;
            public Vector2 UnknownVector2;

            public short UnknownShort1;
            public short UnknownShort2;
            public float UnknownFloat2;
            public short UnknownShort3;
            public short UnknownShort4;
            public float UnknownFloat3;
            public short UnknownShort5;
            public short UnknownShort6;
            public float UnknownFloat4;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown01;
        }

        public HeaderData Header = new HeaderData();

        public Group01Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.UnknownVector1 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.UnknownVector2 = new Vector2(bb.getFloat(), bb.getFloat());

            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.UnknownFloat2 = bb.getFloat();
            Header.UnknownShort3 = bb.getShort();
            Header.UnknownShort4 = bb.getShort();
            Header.UnknownFloat3 = bb.getFloat();
            Header.UnknownShort5 = bb.getShort();
            Header.UnknownShort6 = bb.getShort();
            Header.UnknownFloat4 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group01Entry");
        }
    }

    public static class Group02Entry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public int Unknown1;
            public int Unknown2;
            public Vector2 UnknownVector1;
            public int Unknown3;
            public int Unknown4;

            public short UnknownShort1;
            public short UnknownShort2;
            public float UnknownFloat2;
            public short UnknownShort3;
            public short UnknownShort4;
            public float UnknownFloat3;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown02;
        }

        public HeaderData Header = new HeaderData();

        public Group02Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.UnknownVector1 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();

            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.UnknownFloat2 = bb.getFloat();
            Header.UnknownShort3 = bb.getShort();
            Header.UnknownShort4 = bb.getShort();
            Header.UnknownFloat3 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group02Entry");
        }
    }

    public static class Group03Entry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public float UnknownFloat2;
            public float UnknownFloat3;
            public float UnknownFloat4;
            public float UnknownFloat5;
            public float UnknownFloat6;
            public float UnknownFloat7;
            public float UnknownFloat8;
            public int Unknown1;
            public int Unknown2;

            public short UnknownShort1; //byte×4かな？
            public short UnknownShort2;

            public float UnknownFloat9;

        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown03;
        }

        public HeaderData Header = new HeaderData();

        public Group03Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();
            Header.UnknownFloat3 = bb.getFloat();
            Header.UnknownFloat4 = bb.getFloat();
            Header.UnknownFloat5 = bb.getFloat();
            Header.UnknownFloat6 = bb.getFloat();
            Header.UnknownFloat7 = bb.getFloat();
            Header.UnknownFloat8 = bb.getFloat();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();

            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();

            Header.UnknownFloat9 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group03Entry");
        }
    }

    public static class Group04Entry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public float UnknownFloat2;
            public float UnknownFloat3;
            public float UnknownFloat4;
            public float UnknownFloat5;
            public float UnknownFloat6;
            public float UnknownFloat7;
            public float UnknownFloat8;
            public int Unknown1;
            public int Unknown2;

            public short UnknownShort1; //byte×4かな？
            public short UnknownShort2;

            public float UnknownFloat9;

        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown04;
        }

        public HeaderData Header = new HeaderData();

        public Group04Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();
            Header.UnknownFloat3 = bb.getFloat();
            Header.UnknownFloat4 = bb.getFloat();
            Header.UnknownFloat5 = bb.getFloat();
            Header.UnknownFloat6 = bb.getFloat();
            Header.UnknownFloat7 = bb.getFloat();
            Header.UnknownFloat8 = bb.getFloat();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();

            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();

            Header.UnknownFloat9 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group04Entry");
        }
    }

    public static class Group05Entry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public float UnknownFloat2;
            public float UnknownFloat3;
            public float UnknownFloat4;
            public float UnknownFloat5;
            public float UnknownFloat6;
            public float UnknownFloat7;
            public float UnknownFloat8;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public float UnknownFloat9;

        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown05;
        }

        public HeaderData Header = new HeaderData();

        public Group05Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();
            Header.UnknownFloat3 = bb.getFloat();
            Header.UnknownFloat4 = bb.getFloat();
            Header.UnknownFloat5 = bb.getFloat();
            Header.UnknownFloat6 = bb.getFloat();
            Header.UnknownFloat7 = bb.getFloat();
            Header.UnknownFloat8 = bb.getFloat();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.UnknownFloat9 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group05Entry");
        }
    }

    public static class Group06Entry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public float UnknownFloat2;
            public float UnknownFloat3;
            public float UnknownFloat4;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown06;
        }

        public HeaderData Header = new HeaderData();

        public Group06Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();
            Header.UnknownFloat3 = bb.getFloat();
            Header.UnknownFloat4 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group06Entry");
        }
    }

    public static class Group07Entry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public Vector3 UnknownVector1;
            public Vector2 UnknownVector2;
            public short UnknownShort1;
            public short UnknownShort2;
            public float UnknownFloat2;
            public short UnknownShort3;
            public short UnknownShort4;
            public Vector2 UnknownVector3;
            public float UnknownFloat3;

        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown07;
        }

        public HeaderData Header = new HeaderData();

        public Group07Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownVector1 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownVector2 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.UnknownFloat2 = bb.getFloat();
            Header.UnknownShort3 = bb.getShort();
            Header.UnknownShort4 = bb.getShort();
            Header.UnknownVector3 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.UnknownFloat3 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group07Entry");
        }
    }

    public static class Group08Entry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public Vector2 UnknownVector1;
            public Vector2 UnknownVector2;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown08;
        }

        public HeaderData Header = new HeaderData();

        public Group08Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownVector1 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.UnknownVector2 = new Vector2(bb.getFloat(), bb.getFloat());

            Utils.getGlobalLogger().trace("Group08Entry");
        }
    }

    public static class Group09Entry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public Vector2 UnknownVector1;
            public Vector2 UnknownVector2;
            public Vector2 UnknownVector3;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown09;
        }

        public HeaderData Header = new HeaderData();

        public Group09Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownVector1 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.UnknownVector2 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.UnknownVector3 = new Vector2(bb.getFloat(), bb.getFloat());

            Utils.getGlobalLogger().trace("Group09Entry");
        }
    }

    public static class Group0AEntry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public int Unknown1;
            public Vector3 UnknownVector1;
            public int Unknown2;
            public Vector3 UnknownVector2;
            public int Unknown3;
            public Vector2 UnknownVector3;
            public int Unknown4;

            public short UnknownShort1;
            public short UnknownShort2;
            public float UnknownFloat2;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown0A;
        }

        public HeaderData Header = new HeaderData();

        public Group0AEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.Unknown1 = bb.getInt();
            Header.UnknownVector1 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Unknown2 = bb.getInt();
            Header.UnknownVector2 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Unknown3 = bb.getInt();
            Header.UnknownVector3 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.Unknown4 = bb.getInt();
            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.UnknownFloat2 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group0AEntry");
        }
    }

    public class VfxGroupEntry implements IEnvsGroupEntry {
        public class HeaderData {
            public float UnknownFloat1;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public float Unknown6;
            public int Unknown7;
            public int Unknown8;
            public Vector3 UnknownVector1;
            public int FileOffset1;
            public int FileOffset2;

            public int Unknown9;
            public float UnknownFloat2;
            public int Unknown10;
            public float UnknownFloat3;

            public short UnknownShort1;
            public short UnknownShort2;
            public float UnknownFloat4;
            public short UnknownShort3;
            public short UnknownShort4;
            public Vector2 UnknownVector2;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.VfxGroup;
        }

        public HeaderData Header = new HeaderData();
        public String FileName1, FileName2;

        public VfxGroupEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getFloat();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.UnknownVector1 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            int BaseOffset = bb.position();
            Header.FileOffset1 = bb.getInt();
            Header.FileOffset2 = bb.getInt();

            Header.Unknown9 = bb.getInt();
            Header.UnknownFloat2 = bb.getFloat();
            Header.Unknown10 = bb.getInt();
            Header.UnknownFloat3 = bb.getFloat();

            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.UnknownFloat4 = bb.getFloat();
            Header.UnknownShort3 = bb.getShort();
            Header.UnknownShort4 = bb.getShort();
            Header.UnknownVector2 = new Vector2(bb.getFloat(), bb.getFloat());

            FileName1 = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.FileOffset1);
            FileName2 = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.FileOffset2);

            cAddPathToDB(FileName1);
            cAddPathToDB(FileName2);

            Utils.getGlobalLogger().trace("VfxGroupEntry");
        }
    }

    public static class Group0CEntry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public Vector3 UnknownVector1;

            public float UnknownFloat2;
            public int Unknown1;
            public float UnknownFloat3;

            public float UnknownFloat4;

            public short UnknownShort1;
            public short UnknownShort2;
            public float UnknownFloat5;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown0C;
        }

        public HeaderData Header = new HeaderData();

        public Group0CEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownVector1 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());

            Header.UnknownFloat2 = bb.getFloat();
            Header.Unknown1 = bb.getInt();
            Header.UnknownFloat3 = bb.getFloat();

            Header.UnknownFloat4 = bb.getFloat();
            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.UnknownFloat5 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group0CEntry");
        }

    }

    public static class Group0DEntry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public Vector3 UnknownVector1;
            public Vector3 UnknownVector2;

            public float UnknownFloat4;

            public short UnknownShort1;
            public short UnknownShort2;
            public float UnknownFloat5;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown0D;
        }

        public HeaderData Header = new HeaderData();

        public Group0DEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownVector1 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownVector2 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());

            Header.UnknownFloat4 = bb.getFloat();
            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.UnknownFloat5 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group0DEntry");
        }

    }

    public class SoundGroupEntry implements IEnvsGroupEntry {
        public class HeaderData {
            public float UnknownFloat;
            public int Unknown1;
            public int Unknown2;

            public int NameOffset;
            public int ModelFileOffset1;
            public int ModelFileOffset2;
            public int ModelFileOffset3;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.SoundGroup;
        }

        public HeaderData Header = new HeaderData();
        public String Name;
        public String ModelFile1, ModelFile2, ModelFile3;

        public SoundGroupEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat = bb.getFloat();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            int BaseOffset1 = bb.position();
            Header.NameOffset = bb.getInt();
            Header.ModelFileOffset1 = bb.getInt();
            Header.ModelFileOffset2 = bb.getInt();
            Header.ModelFileOffset3 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, BaseOffset1 + Header.NameOffset);
            ModelFile1 = ByteArrayExtensions.ReadString(bb, BaseOffset1 + Header.ModelFileOffset1);
            ModelFile2 = ByteArrayExtensions.ReadString(bb, BaseOffset1 + Header.ModelFileOffset2);
            ModelFile3 = ByteArrayExtensions.ReadString(bb, BaseOffset1 + Header.ModelFileOffset3);

            cAddPathToDB(Name); //loop
            cAddPathToDB(ModelFile1); //spot
            cAddPathToDB(ModelFile2);
            cAddPathToDB(ModelFile3);

            if (!ModelFile3.equals("")){
                Utils.getGlobalLogger().trace("データ未検証のため要確認");
            }

        }
    }

    public static class Group1DEntry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public int Unknown1;
            public float UnknownFloat2;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown1D;
        }

        public HeaderData Header = new HeaderData();

        public Group1DEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.Unknown1 = bb.getInt();
            Header.UnknownFloat2 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group1DEntry");
        }

    }

    public static class Group1EEntry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public float UnknownFloat2;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown1E;
        }

        public HeaderData Header = new HeaderData();

        public Group1EEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group1EEntry");
        }

    }

    public static class Group1FEntry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public Vector2 UnknownVector1;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown1F;
        }

        public HeaderData Header = new HeaderData();

        public Group1FEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownVector1 = new Vector2(bb.getFloat(), bb.getFloat());

            Utils.getGlobalLogger().trace("Group1FEntry");
        }

    }

    public static class Group20Entry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public float UnknownFloat2;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown20;
        }

        public HeaderData Header = new HeaderData();

        public Group20Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group20Entry");
        }

    }

    public static class Group21Entry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public int Unknown1;
            public short UnknownShort1;
            public short UnknownShort2;
            public float UnknownFloat2;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown21;
        }

        public HeaderData Header = new HeaderData();

        public Group21Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.Unknown1 = bb.getInt();

            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();

            Header.UnknownFloat2 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group21Entry");
        }

    }

    public static class Group22Entry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public int Unknown1;
            public int Unknown2;
            public short UnknownShort1;
            public short UnknownShort2;
            public float UnknownFloat2;

            public short UnknownShort3;
            public short UnknownShort4;
            public float UnknownFloat3;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown22;
        }

        public HeaderData Header = new HeaderData();

        public Group22Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.UnknownFloat2 = bb.getFloat();

            Header.UnknownShort3 = bb.getShort();
            Header.UnknownShort4 = bb.getShort();
            Header.UnknownFloat3 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group22Entry");
        }

    }

    public static class Group23Entry implements IEnvsGroupEntry {
        public static class HeaderData {
            public float UnknownFloat1;
            public int Unknown1;

            public short UnknownShort1; //byte×4かな？
            public short UnknownShort2;

            public float UnknownFloat2;
        }
        public EnvsGroupEntryType getType(){
            return EnvsGroupEntryType.Unknown1D;
        }

        public HeaderData Header = new HeaderData();

        public Group23Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.UnknownFloat1 = bb.getFloat();
            Header.Unknown1 = bb.getInt();
            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.UnknownFloat2 = bb.getFloat();

            Utils.getGlobalLogger().trace("Group23Entry");
        }

    }

    public class EnvsSubEntry {
        public class HeaderData {
            public Vector3 Translation; //ここへのオフセットが存在するため分ける必要があるかも

            public int NameOffset;
            public int ModelFileOffset;
        }

        public HeaderData Header = new HeaderData();
        public String Name;
        public String ModelFile1;

        public EnvsSubEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.NameOffset = bb.getInt();
            Header.ModelFileOffset = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);
            ModelFile1 = ByteArrayExtensions.ReadString(bb, offset + Header.ModelFileOffset);

            cAddPathToDB(Name);
            cAddPathToDB(ModelFile1);

            if (!ModelFile1.equals("")){
                Utils.getGlobalLogger().trace("データ未検証のため要確認");
            }

        }
    }

    public interface IEnvsGroupEntry {
        EnvsGroupEntryType getType();
    }

    public enum EnvsGroupEntryType {
        Unknown00(0x00),
        Unknown01(0x01),
        Unknown02(0x02),
        Unknown03(0x03),
        Unknown04(0x04),
        Unknown05(0x05),
        Unknown06(0x06),
        Unknown07(0x07),
        Unknown08(0x08),
        Unknown09(0x09),
        Unknown0A(0x0A),
        VfxGroup(0x0B),
        Unknown0C(0x0C),
        Unknown0D(0x0D),
        Unknown0E(0x0E),
        Unknown0F(0x0F),
        Unknown10(0x10),
        Unknown11(0x11),
        Unknown12(0x12),
        Unknown13(0x13),
        SoundGroup(0x14),
        Unknown15(0x15),
        Unknown16(0x16),
        Unknown17(0x17),
        Unknown18(0x18),
        Unknown19(0x19),
        Unknown1A(0x1A),
        Unknown1B(0x1B),
        Unknown1C(0x1C),
        Unknown1D(0x1D),
        Unknown1E(0x1E),
        Unknown1F(0x1F),
        Unknown20(0x20),
        Unknown21(0x21),
        Unknown22(0x22),
        Unknown23(0x23),
        Unknown(0xFF),;

        private final int id;

        EnvsGroupEntryType(int i) {
            id = i;
        }

        public static EnvsGroupEntryType valueOf(final int value){
            for(EnvsGroupEntryType type : EnumSet.allOf(EnvsGroupEntryType.class)){
                if(type.getId() == value) {
                    return type;
                }
            }
            return Unknown;
        }

        public int getId() {
            return id;
        }

    }

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
        SqPack_IndexFile sp_IndexFile;
        SqPack_IndexFile temp_IndexFile = currentIndex;

        int result = 0;

        if (currentIndex.getName().equals(archive)) {
            Utils.getGlobalLogger().trace("");
        } else if (archive.equals("010000")){
            if(bgcommonIndex == null) {
                try {
                    bgcommonIndex = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\010000.win32.index", false);
                    temp_IndexFile = bgcommonIndex;
                } catch (IOException e) {
                    Utils.getGlobalLogger().error(e);
                }
            }else{
                temp_IndexFile = bgcommonIndex;
            }
        } else {
            try {
                sp_IndexFile = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\" + archive + ".win32.index", true);
                temp_IndexFile = sp_IndexFile;
            } catch (IOException e) {
                Utils.getGlobalLogger().error(e);
            }
        }

        int pathCheck = temp_IndexFile.findFile(fullPath);
        if (pathCheck == 2){
            result = HashDatabase.addPathToDB(fullPath, archive);

            if (result == 1) {
                if (fullPath.endsWith(".avfx")) {
                    try {
                        //avfxファイル内のパスも解析
                        byte[] data2 = temp_IndexFile.extractFile(fullPath);
                        AVFX_File avfxFile = new AVFX_File(temp_IndexFile, data2, temp_IndexFile.getEndian());
                        avfxFile.regHash(true);
                    } catch (Exception avfxException) {
                        avfxException.printStackTrace();
                    }
                }
            }
        }
        return result;
    }
}
