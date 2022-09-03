package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.ByteArrayExtensions;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.EnumSet;

public class SgbFile extends Game_File {

    private String archive = "";
    private String FilePath = "";

    //region Struct
    public static class HeaderData {
        public String Magic1;  // SGB1
        public int FileSize;   //uint
        public int Unknown1;   //uint
        public String Magic2;  // SCN1

        public int SCN1_Size;   //uint
        public int SharedOffset;
        public int Unknown18;   //uint
        public int Offset1C;

        public int Offset20;   //uint
        public int StatesOffset;
        public int Offset28;   //uint
        public int Offset2C;   //uint

        public int Offset30;   //uint
        public int Offset34;   //uint
        public int Offset38;   //uint
        public int Offset3C;   //uint

        public int Offset40;   //uint
        public int Offset44;   //uint
        public int Offset48;   //uint
        public int Offset4C;   //uint

        public int Offset50;   //uint
    }
    //endregion

    public HeaderData Header;
    public ISgbData[] Data;
    public TmbFile[] TmbFileData;

    /**
     * コンストラクタ(ファイルツリーからの呼び出し用)
     * @param data sgbデータ
     * @param endian エンディアンの種類
     */
    public SgbFile(byte[] data, ByteOrder endian, String filePath){
        super(endian);
        FilePath = filePath;
        loadSGB(data);
    }

    /**
     * コンストラクタ
     * @param data sgbデータ
     * @param endian エンディアンの種類
     */
    public SgbFile(byte[] data, ByteOrder endian){
        super(endian);
        loadSGB(data);
    }

    @SuppressWarnings("unused")
    private void loadSGB(byte[] data) {
        final int BaseOffset = 0x14;

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
        Header.SCN1_Size = bb.getInt(); //SCN1のヘッダ部サイズ 通常0x48、旧タイプver2 = 0x4C、旧タイプver1 = 0x60
        Header.SharedOffset = bb.getInt(); //共有データファイルのオフセット
        Header.Unknown18 = bb.getInt(); //不明 0x01
        Header.Offset1C = bb.getInt(); //モデルデータのオフセット
        Header.Offset20 = bb.getInt(); //何かのサイズ
        Header.StatesOffset = bb.getInt(); //0x0060からの実データサイズ？ 　アドレス: 0x28   ※0x60 + StatesOffset + 0x20でShared文字列
        Header.Offset28 = bb.getInt();
        Header.Offset2C = bb.getInt();

        Header.Offset30 = bb.getInt();
        Header.Offset34 = bb.getInt();
        Header.Offset38 = bb.getInt();
        Header.Offset3C = bb.getInt();

        Header.Offset40 = bb.getInt();
        Header.Offset44 = bb.getInt();
        Header.Offset48 = bb.getInt();
        Header.Offset4C = bb.getInt();

        Header.Offset50 = bb.getInt();
        //ここまでSCN1
        //endregion

        if (!Header.Magic1.equals("SGB1")) {
            Utils.getGlobalLogger().error("SGB1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic1);
            return;
        } else if (!Header.Magic2.equals("SCN1")) {
            Utils.getGlobalLogger().error("SCN1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic2);
            return;
        }

        ArrayList<ISgbData> sgbData = new ArrayList<>();

        try {
            //TODO: Header.SharedOffset > 0x40のとき TMLBデータがここに存在

            sgbData.add(new SgbSharedGroup(bb, BaseOffset + Header.SharedOffset));
            sgbData.add(new SgbGroup(bb, BaseOffset + Header.Offset1C));
        } catch (Exception e) {
            Utils.getGlobalLogger().error(e);
        }

        Data = new ISgbData[2];
        sgbData.toArray(Data);

        if (Header.SharedOffset > 0x40 && Header.SCN1_Size == 0x48){
            ArrayList<TmbFile> tmbData = new ArrayList<>();
            bb.position(0x60);
            int TmlbEntryBase;
            int TmlbSize = 0;

            do {
                TmlbEntryBase = bb.position();
                bb.get(signature); //TMLB
                String Sig = new String(signature).trim();
                if (Sig.equals("TMLB")) {
                    TmlbSize = bb.getInt();
                    if(TmlbSize % 0x10 != 0){
                        //TMLBのブロックの開始アドレスが16進数の末尾0で始まるように0埋めされているため
                        TmlbSize += (0x10 - (TmlbSize % 0x10));
                    }

                    byte[] TmlbData = new byte[TmlbSize];
                    bb.position(TmlbEntryBase);
                    bb.get(TmlbData);
                    tmbData.add(new TmbFile(TmlbData, endian));


                }
            }while (BaseOffset + Header.SharedOffset > TmlbEntryBase + TmlbSize);

            TmbFileData = new TmbFile[tmbData.size()];
            tmbData.toArray(TmbFileData);
        }
    }

    public class SgbGroup implements ISgbData{
        public class HeaderData {
            public SgbDataType Type;
            public int NameOffset;
            public int Offset08;   //uint

            public int EntryCount;
            public int Unknown14;   //uint
            public int ModelFileOffset;
            public Vector3 UnknownFloat3;
            public Vector3 UnknownFloat3_2;
            public int StateOffset;
            public int ModelFileOffset2;
            public int Unknown3;   //uint
            public float Unknown4;
            public int NameOffset2; //short値Index×32へのオフセットだと思う
            public Vector3 UnknownFloat3_3;
        }

        public HeaderData Header = new HeaderData();
        public SgbDataType getType(){
            return Header.Type;
        }
        public String SharedPathName, ModelFile1, ModelFile2;
        public ISgbGroupEntry[] Entries;

        public SgbGroup(ByteBuffer bb, int offset){
            int count;
            //region HeaderData Read
            bb.position(offset);
            Header.Type = SgbDataType.valueOf(bb.getInt());
            Header.NameOffset = bb.getInt();
            Header.Offset08 = bb.getInt();

            Header.EntryCount = bb.getInt();
            Header.Unknown14 = bb.getInt();
            Header.ModelFileOffset = bb.getInt();
            Header.UnknownFloat3 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownFloat3_2 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.StateOffset = bb.getInt();
            Header.ModelFileOffset2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getFloat();
            Header.NameOffset2 = bb.getInt();
            Header.UnknownFloat3_3 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            //endregion

            int entriesOffset = offset + Header.Offset08;

            SharedPathName = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            ModelFile1 = ByteArrayExtensions.ReadString(bb, offset + Header.ModelFileOffset);
            ModelFile2 = ByteArrayExtensions.ReadString(bb, offset + Header.ModelFileOffset2);


            count = Header.EntryCount;
            Entries = new ISgbGroupEntry[count];

            for (int i = 0; i < count; ++i){
                try {
                    int entryOffset = entriesOffset + (i * 0x18);
                    Entries[i] = new SgbGroup1CEntry(bb, entryOffset);
                    //break;
                } catch (Exception e) {
                    Utils.getGlobalLogger().error(e);
                }
            }

        }
    }

    public class SgbSharedGroup implements ISgbData{
        public class HeaderData {
            public SgbDataType Type;
            public int NameOffset;
            public int Offset08;   //uint
            public int Unknown0C;   //uint

            public int Offset10;   //uint
            public int Unknown14;   //uint
            public int SgbNameOffset;   //uint
            public int EntriesOffset;   //uint

            public int EntryCount;
            public int Unknown24;   //uint
            public int Unknown28;   //uint
            public int Unknown2C;   //uint

            public int Unknown30;   //uint
            public int Unknown34;   //uint
            public int Unknown38;   //uint
            public int Unknown3C;   //uint

            public int Unknown40;   //uint
            public int Unknown44;   //uint
        }

        public HeaderData Header = new HeaderData();
        public SgbDataType getType(){
            return Header.Type;
        }
        public String Name, SgbName;
        public ISgbGroupEntry[] Entries;

        public SgbSharedGroup(ByteBuffer bb, int offset){
            int entriesOffset = offset;
            int BaseOffset;
            int count;
            bb.position(entriesOffset);
            //region Header Read
            Header.Type = SgbDataType.valueOf(bb.getInt());
            Header.NameOffset = bb.getInt();
            Header.Offset08 = bb.getInt();
            Header.Unknown0C = bb.getInt();

            bb.position(offset + Header.Offset08);

            Header.Offset10 = bb.getInt();
            bb.position(offset + Header.Offset08 + Header.Offset10);
            BaseOffset = bb.position();
            Header.Unknown14 = bb.getInt();
            Header.SgbNameOffset = bb.getInt();
            Header.EntriesOffset = bb.getInt();

            Header.EntryCount = bb.getInt();
            Header.Unknown24 = bb.getInt(); //Byte flag?
            Header.Unknown28 = bb.getInt(); //12byteデータへのOffset
            Header.Unknown2C = bb.getInt(); //Zero

            Header.Unknown30 = bb.getInt(); //Offset
            Header.Unknown34 = bb.getInt(); //Zero
            Header.Unknown38 = bb.getInt(); //Offset
            Header.Unknown3C = bb.getInt(); //Zero

            Header.Unknown40 = bb.getInt(); //Offset
            Header.Unknown44 = bb.getInt(); //Zero
            //endregion

            entriesOffset = BaseOffset + Header.EntriesOffset;

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);
            SgbName = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.SgbNameOffset);

            if (!FilePath.equals("")){
                String folder = FilePath.substring(0, FilePath.lastIndexOf('/'));
                String filename = FilePath.substring(FilePath.lastIndexOf('/') + 1);

                if (filename.contains("~")){
                    if (!folder.contains("~")){
                        String sgbFilePath = folder + "/" + SgbName + ".sgb";
                        cAddPathToDB(sgbFilePath);
                    }
                }

            }

            count = Header.EntryCount;
            Entries = new ISgbGroupEntry[count];

            for (int i = 0; i < count; ++i){
                try {
                    bb.position(entriesOffset + i * 4);
                    int entryOffset = entriesOffset + bb.getInt();
                    bb.position(entryOffset);
                    int typeInt = bb.getInt();
                    SgbGroupEntryType type = SgbGroupEntryType.valueOf(typeInt);

                    switch (type) {
                        case Model:
                            Entries[i] = new SgbModelEntry(bb, entryOffset);
                            break;
                        case Gimmick:
                            Entries[i] = new SgbGimmickEntry(bb, entryOffset);
                            break;
                        case Light:
                            Entries[i] = new SgbLightEntry(bb, entryOffset);
                            break;
                        case Vfx:
                            Entries[i] = new SgbVfxEntry(bb, entryOffset);
                            break;
                        case CollisionBox:
                            Entries[i] = new SgbCollisionBoxEntry(bb, entryOffset);
                            break;
                        case ChairMarker:
                            Entries[i] = new SgbChairMarkerEntry(bb, entryOffset);
                            break;
                        case TargetMarker:
                            Entries[i] = new SgbTargetMarkerEntry(bb, entryOffset);
                            break;
                        case Sound:
                            Entries[i] = new SgbSoundEntry(bb, entryOffset);
                            break;
                        case ClickableRange:
                            //64byte(0x40 byte)
                            Entries[i] = new SgbClickableRangeEntry(bb, entryOffset);
                            break;
                        case Weapon:
                            Entries[i] = new SgbWeaponEntry(bb, entryOffset);
                            break;
                        case DoorRange:
                            Entries[i] = new SgbDoorRangeEntry(bb, entryOffset);
                            break;
                        case SharedGroup83:
                            Entries[i] = new SgbSharedGroup83Entry(bb, entryOffset);
                            break;
                        case ClientPath:
                            Entries[i] = new SgbClientPathEntry(bb, entryOffset);
                            break;
                        case EnvSpace:
                            Entries[i] = new SgbEnvSpaceEntry(bb, entryOffset);
                            break;
                        case EventNpc:
                            //88byte(0x58 byte)
                            Entries[i] = new SgbENpcEntry(bb, entryOffset);
                            break;
                        case MapRange:
                            //108byte(0x6c byte)
                            Entries[i] = new SgbMapRangeEntry(bb, entryOffset);
                            break;
                        case SphereCastRange:
                            //60byte(0x3C byte)
                            Entries[i] = new SgbSphereCastRangeEntry(bb, entryOffset);
                            break;
                        default:
                            Utils.getGlobalLogger().trace(String.format("%sのEntry解析は未実装", type.name()));
                            break;
                    }
                } catch (Exception e) {
                    Utils.getGlobalLogger().error(e);
                }
            }
        }
    }

    public class SgbGroup1CEntry implements ISgbGroupEntry{
        public class HeaderData {
            public int NameOffset;
            public int Index;    //uint
            public int Unknown08;    //uint
            public int ModelFileOffset;
            public int Unknown10;    //uint
            public int Unknown14;    //uint
        }

        public SgbGroupEntryType getType(){
            return SgbGroupEntryType.valueOf(0xFF);
        }
        public HeaderData Header = new HeaderData();
        public String Name;
        public Model Model;
        public SgbFile Gimmick;

        public SgbGroup1CEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            int strOffset = bb.position();
            Header.NameOffset = bb.getInt();
            Header.Index = bb.getInt();
            Header.Unknown08 = bb.getInt();
            Header.ModelFileOffset = bb.getInt();
            Header.Unknown10 = bb.getInt();
            Header.Unknown14 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, strOffset + Header.NameOffset);
            String mdlFilePath = ByteArrayExtensions.ReadString(bb, strOffset + Header.ModelFileOffset);
            if(mdlFilePath.length() != 0){
                if(cAddPathToDB(mdlFilePath) == 1) {
                    //sgbファイルをモデル表示に使用するのであれば上記の分岐は外す必要があります
                    SqPack_IndexFile tempIndex = SqPack_IndexFile.GetIndexFileForArchiveID(archive, false);

                    if (tempIndex == null){
                        return;
                    }

                    if (mdlFilePath.endsWith(".mdl")) {
                        try {
                            byte[] data = tempIndex.extractFile(mdlFilePath);
                            Model = new Model(null, tempIndex, data, tempIndex.getEndian());
                        } catch (Exception modelException) {
                            Utils.getGlobalLogger().error(modelException);
                        }
                    }else if (mdlFilePath.endsWith(".sgb")){
                        try {
                            byte[] data = tempIndex.extractFile(mdlFilePath);
                            Gimmick = new SgbFile(data, tempIndex.getEndian());
                        } catch (Exception modelException) {
                            Utils.getGlobalLogger().error(modelException);
                        }
                    }
                }
            }


        }
    }

    public class SgbModelEntry implements ISgbGroupEntry{
        public class HeaderData {
            public SgbGroupEntryType Type;
            public int GimmickId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int ModelFileOffset;
            public int CollisionFileOffset;
        }

        public SgbGroupEntryType getType(){
            return SgbGroupEntryType.valueOf(0xFF);
        }
        public HeaderData Header = new HeaderData();
        public String Name;
        public String ModelFilePath;
        public String CollisionFilePath;
        public Model Model;
        public PcbFile CollisionFile;

        public SgbModelEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.GimmickId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.ModelFileOffset = bb.getInt();
            Header.CollisionFileOffset = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            ModelFilePath = ByteArrayExtensions.ReadString(bb, offset + Header.ModelFileOffset);
            CollisionFilePath = ByteArrayExtensions.ReadString(bb, offset + Header.CollisionFileOffset);

            SqPack_IndexFile tempIndex;

            if(ModelFilePath.length() != 0){
                if(cAddPathToDB(ModelFilePath) == 1) {
                    //sgbファイルをモデル表示に使用するのであればここの分岐は外す必要があります
                    tempIndex = SqPack_IndexFile.GetIndexFileForArchiveID(archive, false);

                    if (tempIndex == null){
                        return;
                    }

                    try {
                        byte[] data = tempIndex.extractFile(ModelFilePath);
                        Model = new Model(ModelFilePath, tempIndex, data, tempIndex.getEndian());
                    } catch (Exception modelException) {
                        Utils.getGlobalLogger().error(modelException);
                    }
                }
            }

            if(CollisionFilePath.length() != 0){
                if(cAddPathToDB(CollisionFilePath) == 1) {
                    //sgbファイルをモデル表示に使用するのであればここの分岐は外す必要があります
                    tempIndex = SqPack_IndexFile.GetIndexFileForArchiveID(archive, false);

                    if (tempIndex == null){
                        return;
                    }

                    try {
                        byte[] data = tempIndex.extractFile(CollisionFilePath);
                        CollisionFile = new PcbFile(data, tempIndex.getEndian());
                    } catch (Exception CollisionException) {
                        Utils.getGlobalLogger().error(CollisionException);
                    }
                }
            }

        }
    }

    public class SgbGimmickEntry implements ISgbGroupEntry{
        public class HeaderData {
            public SgbGroupEntryType Type;
            public int GimmickId;   //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int GimmickFileOffset;
            public int CollisionFileOffset;
        }

        public SgbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;
        public SgbFile Gimmick;

        public SgbGimmickEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.GimmickId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.GimmickFileOffset = bb.getInt();
            Header.CollisionFileOffset = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);
            String sgbFileName = ByteArrayExtensions.ReadString(bb, offset + Header.GimmickFileOffset);
            if(sgbFileName.length() != 0){
                if(cAddPathToDB(sgbFileName) == 1) {
                    //sgbファイルをモデル表示に使用するのであればここの分岐は外す必要があります
                    SqPack_IndexFile tempIndex = SqPack_IndexFile.GetIndexFileForArchiveID(archive, false);

                    if (tempIndex == null){
                        return;
                    }

                    try {
                        byte[] data = tempIndex.extractFile(sgbFileName);
                        Gimmick = new SgbFile(data, tempIndex.getEndian());
                    } catch (Exception modelException) {
                        Utils.getGlobalLogger().error(modelException);
                    }
                }
            }


        }
    }

    public static class SgbLightEntry implements ISgbGroupEntry{
        public static class HeaderData {
            public SgbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int EntryCount;  //uint
            public Vector2 Entry1;
            public int UnknownFlag1;
            public Vector2 Entry2;
            public int Entry2NameOffset;
            public int Entry3NameOffset;  //ushort
            public short UnknownFlag2;
            public Vector2 Entry3;
            public short UnknownFlag3;
            public short UnknownFlag4;
            public Vector2 Entry4;
            public Vector2 Entry5;
        }

        public SgbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;
        public String Entry2Name;

        public SgbLightEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.EntryCount = bb.getInt();
            Header.Entry1 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.UnknownFlag1 = bb.getInt();
            Header.Entry2 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.Entry2NameOffset = bb.getInt();
            Header.Entry3NameOffset = Short.toUnsignedInt(bb.getShort());
            Header.UnknownFlag2 = bb.getShort();
            Header.Entry3 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.UnknownFlag3 = bb.getShort();
            Header.UnknownFlag4 = bb.getShort();
            Header.Entry4 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.Entry5 = new Vector2(bb.getFloat(), bb.getFloat());

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);
            Entry2Name = ByteArrayExtensions.ReadString(bb, offset + Header.Entry2NameOffset);

        }
    }

    public class SgbVfxEntry implements ISgbGroupEntry{
        public class HeaderData {
            public SgbGroupEntryType Type;
            public int UnknownId;   //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int AvfxFileOffset;
            public int Unknown34;
            public int Unknown38;
            public int Unknown3C;

            public int Unknown40;

            public Vector3 SomeVec3;
            public Vector3 SomeVec4; //floatぽいデータがあったので追加 ※要検証

            public int Unknown5C; //要検証
            public int Unknown60; //要検証
        }

        public SgbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;
        public String AvfxFilePath;
        public AVFX_File AvfxFile;

        public SgbVfxEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.AvfxFileOffset = bb.getInt();
            Header.Unknown34 = bb.getInt();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();
            Header.Unknown40 = bb.getInt();
            Header.SomeVec3 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeVec4 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Unknown5C = bb.getInt();
            Header.Unknown60 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);
            AvfxFilePath = ByteArrayExtensions.ReadString(bb, offset + Header.AvfxFileOffset);
            if(AvfxFilePath.length() != 0){
                if(cAddPathToDB(AvfxFilePath) == 1) {
                    //sgbファイルをモデル表示に使用するのであればここの分岐は外す必要があります
                    SqPack_IndexFile tempIndex = SqPack_IndexFile.GetIndexFileForArchiveID(archive, false);

                    if (tempIndex == null){
                        return;
                    }

                    try {
                        byte[] data = tempIndex.extractFile(AvfxFilePath);
                        AvfxFile = new AVFX_File(tempIndex, data, tempIndex.getEndian());
                        AvfxFile.regHash();
                    } catch (Exception e) {
                        Utils.getGlobalLogger().error(e);
                    }
                }
            }
        }
    }

    public static class SgbCollisionBoxEntry implements ISgbGroupEntry{
        public static class HeaderData {
            public SgbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int CollisionBoxType;
            public int CollisionFileOffset;
            public int Unknown38;
            public int Unknown3C;
            public int Unknown40;
            public int Unknown44;
            public int Unknown48;
            public int Unknown4C;
            public int Unknown50;
            public int Unknown54;
        }

        public SgbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;

        public SgbCollisionBoxEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.CollisionBoxType = bb.getInt();
            Header.CollisionFileOffset = bb.getInt();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();
            Header.Unknown40 = bb.getInt();
            Header.Unknown44 = bb.getInt();
            Header.Unknown48 = bb.getInt();
            Header.Unknown4C = bb.getInt();
            Header.Unknown50 = bb.getInt();
            Header.Unknown54 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

        }
    }

    public static class SgbChairMarkerEntry implements ISgbGroupEntry{
        public static class HeaderData {
            public SgbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown34;
        }

        public SgbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;

        public SgbChairMarkerEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown34 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

        }
    }

    public static class SgbTargetMarkerEntry implements ISgbGroupEntry{
        public static class HeaderData {
            public SgbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown3;
        }

        public SgbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;

        public SgbTargetMarkerEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown3 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

        }
    }

    public class SgbSoundEntry implements ISgbGroupEntry{
        public class HeaderData {
            public SgbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int envbFileOffset;
            public int scdFileOffset;
            public int Unknown38;
            public int Unknown3C;
            public int Unknown40;
            public int EntrySize;
            public int Unknown48;
            public int Unknown4C; //ushort
            public int Unknown50; //ushort
            public int Unknown54;
            public int Unknown58;
            public int Unknown5C;
        }

        public SgbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public Vector4[] SoundEnvEntry;
        public String Name;
        public String scdFile;

        public SgbSoundEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.envbFileOffset = bb.getInt();
            Header.scdFileOffset = bb.getInt();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();
            Header.Unknown40 = bb.getInt();
            Header.EntrySize = bb.getInt();
            Header.Unknown48 = bb.getInt();
            Header.Unknown4C = Short.toUnsignedInt(bb.getShort());
            Header.Unknown50 = Short.toUnsignedInt(bb.getShort());
            Header.Unknown54 = bb.getInt();
            Header.Unknown58 = bb.getInt();
            Header.Unknown5C = bb.getInt();

            //floatデータが並んでいるが詳細不明(BGMや効果音・環境音のエリア範囲とかかな？)
            int SoundEnvEntryNum = Header.EntrySize / 16 - 1;
            SoundEnvEntry = new Vector4[SoundEnvEntryNum];
            for (int i = 0; i < SoundEnvEntryNum; i++){
                SoundEnvEntry[i] = new Vector4(bb.getFloat(), bb.getFloat(), bb.getFloat(), bb.getFloat());
            }

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            scdFile = ByteArrayExtensions.ReadString(bb,offset + Header.scdFileOffset);

            cAddPathToDB(scdFile);

        }
    }

    public static class SgbClickableRangeEntry implements ISgbGroupEntry{
        public static class HeaderData {
            public SgbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown34;
            public int Unknown38;
            public int Unknown3C;
        }

        public SgbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;

        public SgbClickableRangeEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown34 = bb.getInt();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            Utils.DummyLog("ClickableRange");
        }
    }

    public static class SgbSphereCastRangeEntry implements ISgbGroupEntry{
        public static class HeaderData {
            public SgbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown34;
            public int Unknown36;
            public int Unknown38;
        }

        public SgbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;

        public SgbSphereCastRangeEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown34 = bb.getShort();
            Header.Unknown36 = bb.getShort();
            Header.Unknown38 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            Utils.DummyLog("SphereCastRange");
        }
    }

    public static class SgbMapRangeEntry implements ISgbGroupEntry {
        public static class HeaderData {
            public SgbGroupEntryType Type;
            public int Unknown04; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown34;
            public int Unknown38;
            public int Unknown3C;
            public int Unknown40;
            public int Unknown44;
            public int Unknown48;
            public int Unknown4C;
            public int Unknown50;
            public int Unknown54;
            public int Unknown58; //ushort
            public int Unknown5A; //ushort
            public int Unknown5C;
            public int Unknown60;
            public int Unknown64;
            public int Unknown68;
        }
        public SgbGroupEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public SgbMapRangeEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.Unknown04 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown34 = bb.getInt();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();
            Header.Unknown40 = bb.getInt();
            Header.Unknown44 = bb.getInt();
            Header.Unknown48 = bb.getInt();
            Header.Unknown4C = bb.getInt();
            Header.Unknown50 = bb.getInt();
            Header.Unknown54 = bb.getInt();
            Header.Unknown58 = Short.toUnsignedInt(bb.getShort());
            Header.Unknown5A = Short.toUnsignedInt(bb.getShort());
            Header.Unknown5C = bb.getInt();
            Header.Unknown60 = bb.getInt();
            Header.Unknown64 = bb.getInt();
            Header.Unknown68 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
        }

    }

    public static class SgbENpcEntry implements ISgbGroupEntry {
        public static class HeaderData {
            public SgbGroupEntryType Type;
            public int Unknown04; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int ENpcId;
            public int Unknown34;
            public int Unknown38;
            public int Unknown3C;
            public int Unknown40;
            public int Unknown44;
            public int Unknown48;
            public int Unknown4C;
            public int Unknown50;
            public int Unknown54;
        }
        public SgbGroupEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public SgbENpcEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.Unknown04 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.ENpcId = bb.getInt();
            Header.Unknown34 = bb.getInt();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();
            Header.Unknown40 = bb.getInt();
            Header.Unknown44 = bb.getInt();
            Header.Unknown48 = bb.getInt();
            Header.Unknown4C = bb.getInt();
            Header.Unknown50 = bb.getInt();
            Header.Unknown54 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
        }

    }

    public static class SgbDoorRangeEntry implements ISgbGroupEntry{
        public static class HeaderData {
            public SgbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown34;
            public int Unknown38;
            public int Unknown3C;
            public int Unknown40;
        }

        public SgbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;

        public SgbDoorRangeEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown34 = bb.getInt();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();
            Header.Unknown40 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            Utils.DummyLog("DoorRange");
        }
    }

    public static class SgbWeaponEntry implements ISgbGroupEntry{
        public static class HeaderData {
            public SgbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown34;
            public int Unknown38;
            public int Unknown3C;
            public int Unknown40;
            public int Unknown44;
        }

        public SgbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;

        public SgbWeaponEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown34 = bb.getInt();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();
            Header.Unknown40 = bb.getInt();
            Header.Unknown44 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            Utils.DummyLog("WeaponEntry");
        }
    }

    public static class SgbSharedGroup83Entry implements ISgbGroupEntry{
        public static class HeaderData {
            public SgbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Offset34;
            public int Offset38;
            public int Unknown3C;
            public int Unknown40;
            public int Unknown44;
            public float Unknown48;
            public int Unknown4C;
            public int Unknown50;
            public float Unknown54;
            public float Unknown58;
            public int Unknown5C;
        }

        public SgbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;

        public SgbSharedGroup83Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Offset34 = bb.getInt();
            Header.Offset38 = bb.getInt();
            Header.Unknown3C = bb.getInt();
            Header.Unknown40 = bb.getInt();
            Header.Unknown44 = bb.getInt();
            Header.Unknown48 = bb.getFloat();
            Header.Unknown4C = bb.getInt();

            Header.Unknown50 = bb.getInt();
            Header.Unknown54 = bb.getFloat();
            Header.Unknown58 = bb.getFloat();
            Header.Unknown5C = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            Utils.DummyLog("Unknown83");
        }
    }

    public static class SgbClientPathEntry implements ISgbGroupEntry{
        public static class HeaderData {
            public SgbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int EntryOffset;
            public int EntryCount;
            public int Offset38;
            public int Unknown3C;
            public int Unknown40;
            public int Unknown44;
            public int Unknown48;
            public int Unknown4C;
        }

        public SgbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;
        public SgbClientPathData[] Entry;

        public SgbClientPathEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.EntryOffset = bb.getInt();
            Header.EntryCount = bb.getInt();
            Header.Offset38 = bb.getInt();
            Header.Unknown3C = bb.getInt();
            Header.Unknown40 = bb.getInt();
            Header.Unknown44 = bb.getInt();
            Header.Unknown48 = bb.getInt();
            Header.Unknown4C = bb.getInt();


            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            bb.position(offset + Header.EntryOffset);
            Entry = new SgbClientPathData[Header.EntryCount];
            for (int i = 0; i < Header.EntryCount; i++){
                Entry[i] = new SgbClientPathData(bb);
            }

            Utils.DummyLog("SgbClientPathEntry");
        }

        public static class SgbClientPathData{
            public Vector3 UnknownVector;
            public int SumID;

            public SgbClientPathData(ByteBuffer bb){
                UnknownVector = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
                SumID = bb.getInt();
            }
        }
    }

    public static class SgbEnvSpaceEntry implements ISgbGroupEntry{
        public static class HeaderData {
            public SgbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown34;
            public int Unknown38;
            public int Unknown3C;

            public float Unknown40;
            public int Unknown44;
            public int Unknown48;
            public int Unknown4C;

            public int Unknown50;
        }

        public SgbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;

        public SgbEnvSpaceEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = SgbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown34 = bb.getInt();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();

            Header.Unknown40 = bb.getFloat();
            Header.Unknown44 = bb.getInt();
            Header.Unknown48 = bb.getInt();
            Header.Unknown4C = bb.getInt();

            Header.Unknown50 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            Utils.DummyLog("EnvSpaceEntry");
        }
    }

    public interface ISgbGroupEntry{
        SgbGroupEntryType getType();
    }

    public enum SgbGroupEntryType {
        BgParts(0x00),
        //Keep this for backwards compatibility
        Model (0x01),
        Light(0x03),
        Vfx(0x04),
        PositionMarker(0x05),
        Gimmick(0x06),
        Sound(0x07),
        EventNpc(0x08),
        BattleNpc(0x09),
        Aetheryte(0x0c),
        EnvSpace(0x0d),
        Gathering(0x0e),
        SharedGroup15(0x0f),// secondary variable is set to 13
        Treasure(0x10),
        Weapon(0x27),
        PopRange(0x29),
        ExitRange(0x2a),
        MapRange(0x2b),
        NaviMeshRange(0x2c),
        EventObject(0x2d),
        EnvLocation(0x2f),
        EventRange(0x31),
        QuestMarker(0x33),
        CollisionBox(0x39),
        DoorRange(0x3a),
        LineVfx(0x3b),
        ClientPath(0x41),
        ServerPath(0x42),
        GimmickRange(0x43),
        TargetMarker(0x44),
        ChairMarker(0x45),
        ClickableRange(0x46),
        PrefetchRange(0x47),
        FateRange(0x48),
        SphereCastRange(0x4b),
        SharedGroup83(0x53),
        ZoneMap(0x56),
        UnknownType( 0xffff);

        private final int id;

        SgbGroupEntryType(int i) {
            id = i;
        }

        public static SgbGroupEntryType valueOf(final int value){
            for(SgbGroupEntryType type : EnumSet.allOf(SgbGroupEntryType.class)){
                if(type.getId() == value) {
                    return type;
                }
            }
            Utils.getGlobalLogger().trace(String.format("未知のEntryType : %s", value));
            return UnknownType;
        }

        public int getId() {
            return id;
        }

    }

    public interface ISgbData{
        SgbDataType getType();
    }

    public enum SgbDataType {
        Unknown0000(0),
        Unknown0008(0x0008),
        Group(0x0100);

        private final int id;

        SgbDataType(int i) {
            id = i;
        }

        public static SgbDataType valueOf(final int value){
            for(SgbDataType type : EnumSet.allOf(SgbDataType.class)){
                if(type.getId() == value) {
                    return type;
                }
            }
            return Unknown0000;
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
            archive = HashDatabase.getArchiveID(fullPath);
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
                //該当ファイルが新規登録時のみ解析
                if (fullPath.endsWith(".envb") || fullPath.endsWith(".essb")) {
                    try {
                        byte[] data = temp_IndexFile.extractFile(fullPath);
                        new ENVB_File(data, temp_IndexFile.getEndian());
                    } catch (Exception e) {
                        Utils.getGlobalLogger().error(e);
                    }
                }
            }

        }
        return result;
    }
}
