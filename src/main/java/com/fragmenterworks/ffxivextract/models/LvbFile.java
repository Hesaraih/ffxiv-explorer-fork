package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.helpers.ByteArrayExtensions;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * LvbFileファイル読み込み
 * (sgbファイルに似ている)
 */
public class LvbFile extends Game_File {

    @SuppressWarnings("unused")
    public String entryName;
    @SuppressWarnings("unused")
    public String modelName;
    @SuppressWarnings("unused")
    public String collisionName;

    private String archive = "";

    //他のファイルを見つけるために使用されます
    private static SqPack_IndexFile currentIndex; //現在表示中または呼び出し元のIndexファイル
    private static SqPack_IndexFile bgcommonIndex;
    private static SqPack_IndexFile vfxIndex;

    //region Struct
    public static class HeaderData {
        public String Magic1;  // LVB1
        public int FileSize;   //uint
        public int Unknown1;   //uint
        public String Magic2;  // SCN1

        public int SCN1_Size;   //uint
        public int SharedOffset;
        public int SharedEntryCount;   //uint
        public int SoundOffset;

        public int NaviMeshOffset;   //uint
        public int StatesOffset;
        public int FileEntryOffset;   //uint
        public int FileEntryCount;   //uint

        public int Unknown30;   //uint
        public int Unknown34;   //uint
        public int Unknown38;   //uint
        public int Unknown3C;   //uint

        public int Unknown40;   //uint
        public int Unknown44;   //uint
        public int Unknown48;   //uint
        public int Unknown4C;   //uint

        public int Unknown50;   //uint
    }
    //endregion

    public HeaderData Header;
    public ILvbData[] Data;
    public String[] LgbFileName;

    /**
     * コンストラクタ
     * @param data sgbデータ
     * @param endian エンディアンの種類
     */
    public LvbFile(SqPack_IndexFile index, byte[] data, ByteOrder endian){
        super(endian);
        currentIndex = index;
        loadSGB(data);
    }

    @SuppressWarnings("unused")
    private void loadSGB(byte[] data) {
        final int BaseOffset = 0x14;

        byte[] signature = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        //region LVBファイル ヘッダー
        Header = new HeaderData();
        bb.get(signature); //LVB1
        Header.Magic1 = new String(signature).trim();
        Header.FileSize = bb.getInt(); //ファイルサイズ
        Header.Unknown1 = bb.getInt(); //不明 0x01 SCN1等のチャンク数
        bb.get(signature); //SCN1
        Header.Magic2 = new String(signature).trim();
        Header.SCN1_Size = bb.getInt(); //SCN1のヘッダ部サイズ 0x48固定？　アドレス: 0x10
        Header.SharedOffset = bb.getInt(); //共有データファイルのオフセット
        Header.SharedEntryCount = bb.getInt(); //共有データのエントリー数
        Header.SoundOffset = bb.getInt(); //環境音モデルデータのオフセット
        Header.NaviMeshOffset = bb.getInt(); //
        Header.StatesOffset = bb.getInt();
        Header.FileEntryOffset = bb.getInt(); //ファイルエントリーのオフセット
        Header.FileEntryCount = bb.getInt(); //ファイルエントリーのエントリー数

        Header.Unknown30 = bb.getInt();
        Header.Unknown34 = bb.getInt();
        Header.Unknown38 = bb.getInt();
        Header.Unknown3C = bb.getInt();

        Header.Unknown40 = bb.getInt();
        Header.Unknown44 = bb.getInt();
        Header.Unknown48 = bb.getInt();
        Header.Unknown4C = bb.getInt();

        Header.Unknown50 = bb.getInt();
        //ここまでSCN1
        //endregion

        if (!Header.Magic1.equals("LVB1")) {
            Utils.getGlobalLogger().error("LVB1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic1);
        } else if (!Header.Magic2.equals("SCN1")) {
            Utils.getGlobalLogger().error("SCN1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic2);
        }

        ArrayList<ILvbData> lvbData = new ArrayList<>();

        try {
            for (int i = 0; i < Header.SharedEntryCount; i++) {
                lvbData.add(new LvbSharedGroup(bb, BaseOffset + Header.SharedOffset));
            }

            lvbData.add(new LvbSoundGroup(bb, BaseOffset + Header.SoundOffset));
            lvbData.add(new LvbNaviMeshGroup(bb, BaseOffset + Header.NaviMeshOffset));

            LgbFileName = new String[Header.FileEntryCount];
            for (int i = 0; i < Header.FileEntryCount; i++) {
                bb.position(BaseOffset + Header.FileEntryOffset + i * 4);

                LgbFileName[i] = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.FileEntryOffset + bb.getInt());
            }


        } catch (Exception e) {
            Utils.getGlobalLogger().error(e);
        }

        Data = new ILvbData[lvbData.size()];
        lvbData.toArray(Data);
    }

    public class LvbSoundGroup implements ILvbData {
        public class HeaderData {
            public LvbDataType Type;
            public int NameOffset;
            public int Unknown08;   //uint

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
        public LvbDataType getType(){
            return Header.Type;
        }
        public String Name, ModelFile1, ModelFile2;
        public ILvbGroupEntry[] Entries;

        public LvbSoundGroup(ByteBuffer bb, int offset){
            int entriesOffset = offset;
            int count;
            //region HeaderData Read
            bb.position(entriesOffset);
            Header.Type = LvbDataType.valueOf(bb.getInt());
            Header.NameOffset = bb.getInt();
            Header.Unknown08 = bb.getInt();

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

            entriesOffset = bb.position();
            //endregion

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            ModelFile1 = ByteArrayExtensions.ReadString(bb, offset + Header.ModelFileOffset);
            ModelFile2 = ByteArrayExtensions.ReadString(bb, offset + Header.ModelFileOffset2);


            count = Header.EntryCount;
            Entries = new ILvbGroupEntry[count];

            for (int i = 0; i < count; ++i){
                try {
                    int entryOffset = entriesOffset + (i * 24);
                    Entries[i] = new LvbSoundGroupEntry(bb, entryOffset);
                    break;
                } catch (Exception e) {
                    Utils.getGlobalLogger().error(e);
                }
            }
            Utils.getGlobalLogger().trace("LvbSoundGroup");
        }
    }

    public class LvbSharedGroup implements ILvbData {
        public class HeaderData {
            public LvbDataType Type;
            public int NameOffset;
            public int Unknown08;   //uint
            public int Unknown0C;   //uint

            public int Unknown10;   //uint
            public int Unknown14;   //uint
            public int SgbNameOffset;   //uint
            public int Unknown1C;   //uint

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
        public LvbDataType getType(){
            return Header.Type;
        }
        public String Name, LvbName;
        public ILvbGroupEntry[] Entries;

        public LvbSharedGroup(ByteBuffer bb, int offset){
            int entriesOffset = offset;
            int LvbNameBase;
            int count;
            bb.position(entriesOffset);
            //region Header Read
            Header.Type = LvbDataType.valueOf(bb.getInt());
            Header.NameOffset = bb.getInt();
            Header.Unknown08 = bb.getInt();
            Header.Unknown0C = bb.getInt();

            Header.Unknown10 = bb.getInt();
            LvbNameBase = bb.position();
            Header.Unknown14 = bb.getInt();
            Header.SgbNameOffset = bb.getInt();
            Header.Unknown1C = bb.getInt();

            Header.EntryCount = bb.getInt();
            Header.Unknown24 = bb.getInt();
            Header.Unknown28 = bb.getInt();
            Header.Unknown2C = bb.getInt();

            Header.Unknown30 = bb.getInt();
            Header.Unknown34 = bb.getInt();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();

            Header.Unknown40 = bb.getInt();
            Header.Unknown44 = bb.getInt();
            //endregion

            entriesOffset = bb.position();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);
            LvbName = ByteArrayExtensions.ReadString(bb, LvbNameBase + Header.SgbNameOffset);
            count = Header.EntryCount;
            Entries = new ILvbGroupEntry[count];

            for (int i = 0; i < count; ++i){
                try {
                    bb.position(entriesOffset + i * 4);
                    int entryOffset = entriesOffset + bb.getInt();
                    bb.position(entryOffset);
                    int typeInt = bb.getInt();
                    LvbGroupEntryType type = LvbGroupEntryType.valueOf(typeInt);
                    @SuppressWarnings("unused")
                    String typeStr = LgbFile.LgbEntryType.valueOf(typeInt).name();

                    switch (type) {
                        case Model:
                            Entries[i] = new LvbModelEntry(bb, entryOffset);
                            break;
                        case Gimmick:
                            Entries[i] = new LvbGimmickEntry(bb, entryOffset);
                            break;
                        case Light:
                            Entries[i] = new LvbLightEntry(bb, entryOffset);
                            break;
                        case Vfx:
                            Entries[i] = new LvbVfxEntry(bb, entryOffset);
                            break;
                        case CollisionBox:
                            Entries[i] = new LvbCollisionBoxEntry(bb, entryOffset);
                            break;
                        case ChairMarker:
                            Entries[i] = new LvbChairMarkerEntry(bb, entryOffset);
                            break;
                        case TargetMarker:
                            Entries[i] = new LvbTargetMarkerEntry(bb, entryOffset);
                            break;
                        case Sound:
                            Entries[i] = new LvbSoundEntry(bb, entryOffset);
                            break;
                        default:
                            Utils.getGlobalLogger().info(String.format("%sのEntry解析は未実装", type.name()));
                            break;
                    }
                } catch (Exception e) {
                    Utils.getGlobalLogger().error(e);
                }
            }
        }
    }

    public static class LvbNaviMeshGroup implements ILvbData {
        public static class HeaderData {
            public LvbDataType Type;

            public int EntryCount;
        }

        public HeaderData Header = new HeaderData();
        public LvbDataType getType(){
            return Header.Type;
        }
        public ILvbGroupEntry[] Entries;

        public LvbNaviMeshGroup(ByteBuffer bb, int offset){
            int entriesOffset = offset;
            int count;
            //region HeaderData Read
            bb.position(entriesOffset);
            Header.Type = LvbDataType.valueOf(bb.getInt());
            Header.EntryCount = bb.getInt();

            entriesOffset = bb.position();
            //endregion

            count = Header.EntryCount;
            Entries = new ILvbGroupEntry[count];

            for (int i = 0; i < count; ++i){
                try {
                    int entryOffset = entriesOffset + (i * 24);
                    Entries[i] = new LvbNaviMeshEntry(bb, entryOffset);
                    break;
                } catch (Exception e) {
                    Utils.getGlobalLogger().error(e);
                }
            }

        }
    }

    public class LvbSoundGroupEntry implements ILvbGroupEntry {
        public class HeaderData {
            public int Unk;    //uint
            public int Unk2;    //uint
            public int NameOffset;
            public int Index;    //uint
            public int Unk3;    //uint
            public int ModelFileOffset;
        }

        public LvbGroupEntryType getType(){
            return LvbGroupEntryType.valueOf(0xFF);
        }
        public HeaderData Header = new HeaderData();
        public String Name;
        public Model Model;
        public LvbFile Gimmick;

        public LvbSoundGroupEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Unk = bb.getInt(); //LvbGroupEntryTypeかな？
            Header.Unk2 = bb.getInt();
            int strOffset = bb.position();
            Header.NameOffset = bb.getInt();
            Header.Index = bb.getInt();
            Header.Unk3 = bb.getInt();
            Header.ModelFileOffset = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, strOffset + Header.NameOffset);
            String mdlFilePath = ByteArrayExtensions.ReadString(bb, strOffset + Header.ModelFileOffset);
            SqPack_IndexFile tempIndex = currentIndex;
            if(mdlFilePath.length() != 0){
                if(cAddPathToDB(mdlFilePath) == 1) {
                    if (archive.equals("010000")){
                        tempIndex = bgcommonIndex;
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
                            Gimmick = new LvbFile(tempIndex, data, tempIndex.getEndian());
                        } catch (Exception modelException) {
                            Utils.getGlobalLogger().error(modelException);
                        }
                    }else if (mdlFilePath.endsWith(".essb")){
                        try {
                            byte[] data = tempIndex.extractFile(mdlFilePath);
                            new ENVB_File(tempIndex, data, tempIndex.getEndian());
                        } catch (Exception modelException) {
                            Utils.getGlobalLogger().error(modelException);
                        }
                    }
                }
            }


        }
    }

    public static class LvbNaviMeshEntry implements ILvbGroupEntry {
        public static class HeaderData {
            public int ServerDataOffset1;   //uint
            public int EntryID1;
            public int Unknown1;   //uint
            public int Unknown2;
            public int SumID;   //uint
            public int Offset3;   //uint
            public int ServerDataOffset2;   //uint
        }

        public LvbGroupEntryType getType(){
            return LvbGroupEntryType.valueOf(0xFF);
        }
        public HeaderData Header = new HeaderData();
        public String nvmFileName, ModelFile2, nvxFileName;

        public LvbNaviMeshEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.ServerDataOffset1 = bb.getInt();
            Header.EntryID1 = bb.getInt();
            int strOffset = bb.position();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.SumID = bb.getInt();
            Header.Offset3 = bb.getInt();
            Header.ServerDataOffset2 = bb.getInt();

            nvmFileName = ByteArrayExtensions.ReadString(bb, Header.ServerDataOffset1);
            ModelFile2 = ByteArrayExtensions.ReadString(bb, strOffset + Header.Offset3);
            nvxFileName = ByteArrayExtensions.ReadString(bb, strOffset + Header.ServerDataOffset2);

            Utils.getGlobalLogger().trace("NaviMesh");
        }
    }

    public class LvbModelEntry implements ILvbGroupEntry {
        public class HeaderData {
            public LvbGroupEntryType Type;
            public int GimmickId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int ModelFileOffset;
            public int CollisionFileOffset;
        }

        public LvbGroupEntryType getType(){
            return LvbGroupEntryType.valueOf(0xFF);
        }
        public HeaderData Header = new HeaderData();
        public String Name;
        public String ModelFilePath;
        public String CollisionFilePath;
        public Model Model;
        public PcbFile CollisionFile;

        public LvbModelEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = LvbGroupEntryType.valueOf(bb.getInt());
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

            SqPack_IndexFile tempIndex = currentIndex;

            if(ModelFilePath.length() != 0){
                if(cAddPathToDB(ModelFilePath) == 1) {
                    if (archive.equals("010000")){
                        tempIndex = bgcommonIndex;
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
                    if (archive.equals("010000")){
                        tempIndex = bgcommonIndex;
                    }

                    try {
                        byte[] data = tempIndex.extractFile(CollisionFilePath);
                        CollisionFile = new PcbFile(tempIndex, data, tempIndex.getEndian());
                    } catch (Exception CollisionException) {
                        Utils.getGlobalLogger().error(CollisionException);
                    }
                }
            }

        }
    }

    public class LvbGimmickEntry implements ILvbGroupEntry {
        public class HeaderData {
            public LvbGroupEntryType Type;
            public int GimmickId;   //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int GimmickFileOffset;
            public int CollisionFileOffset;
        }

        public LvbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;
        public LvbFile Gimmick;

        public LvbGimmickEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = LvbGroupEntryType.valueOf(bb.getInt());
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
                    SqPack_IndexFile tempIndex = currentIndex;
                    if (archive.equals("010000")){
                        tempIndex = bgcommonIndex;
                    }

                    try {
                        byte[] data = tempIndex.extractFile(sgbFileName);
                        Gimmick = new LvbFile(tempIndex, data, tempIndex.getEndian());
                    } catch (Exception modelException) {
                        Utils.getGlobalLogger().error(modelException);
                    }
                }
            }


        }
    }

    public static class LvbLightEntry implements ILvbGroupEntry {
        public static class HeaderData {
            public LvbGroupEntryType Type;
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
            // + unknowns
        }

        public LvbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;
        public String Entry2Name;

        public LvbLightEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = LvbGroupEntryType.valueOf(bb.getInt());
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

    public class LvbVfxEntry implements ILvbGroupEntry {
        public class HeaderData {
            public LvbGroupEntryType Type;
            public int UnknownId;   //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int AvfxFileOffset;
            public int Unknown5_2;
            public int Unknown5_3;
            public int Unknown5_4;

            public int Unknown6;

            public Vector3 SomeVec3;
            public Vector3 SomeVec4; //floatぽいデータがあったので追加 ※要検証

            public int Unknown7; //要検証
            public int Unknown8; //要検証
        }

        public LvbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;
        public String AvfxFilePath;
        public AVFX_File AvfxFile;

        public LvbVfxEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = LvbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.AvfxFileOffset = bb.getInt();
            Header.Unknown5_2 = bb.getInt();
            Header.Unknown5_3 = bb.getInt();
            Header.Unknown5_4 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.SomeVec3 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeVec4 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);
            AvfxFilePath = ByteArrayExtensions.ReadString(bb, offset + Header.AvfxFileOffset);
            if(AvfxFilePath.length() != 0){
                if(cAddPathToDB(AvfxFilePath) == 1) {
                    SqPack_IndexFile tempIndex = currentIndex;
                    if (archive.equals("010000")){
                        tempIndex = bgcommonIndex;
                    }else if (archive.equals("080000")){
                        tempIndex = vfxIndex;
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

    public static class LvbCollisionBoxEntry implements ILvbGroupEntry {
        public static class HeaderData {
            public LvbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int CollisionBoxType;
            public int CollisionFileOffset;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
            public int Unknown8;
            public int Unknown9;
            public int Unknown10;
            public int Unknown11;
        }

        public LvbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;

        public LvbCollisionBoxEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = LvbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.CollisionBoxType = bb.getInt();
            Header.CollisionFileOffset = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.Unknown9 = bb.getInt();
            Header.Unknown10 = bb.getInt();
            Header.Unknown11 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

        }
    }

    public static class LvbChairMarkerEntry implements ILvbGroupEntry {
        public static class HeaderData {
            public LvbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown3;
        }

        public LvbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;

        public LvbChairMarkerEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = LvbGroupEntryType.valueOf(bb.getInt());
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

    public static class LvbTargetMarkerEntry implements ILvbGroupEntry {
        public static class HeaderData {
            public LvbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown3;
        }

        public LvbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;

        public LvbTargetMarkerEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = LvbGroupEntryType.valueOf(bb.getInt());
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

    public class LvbSoundEntry implements ILvbGroupEntry {
        public class HeaderData {
            public LvbGroupEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int envbFileOffset;
            public int scdFileOffset;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int EntrySize;
            public int Unknown8;
            public int Unknown9; //ushort
            public int Unknown10; //ushort
            public int Unknown11;
            public int Unknown12;
            public int Unknown13;
        }

        public LvbGroupEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public Vector4[] SoundEnvEntry;
        public String Name;
        public String scdFile;

        public LvbSoundEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = LvbGroupEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.envbFileOffset = bb.getInt();
            Header.scdFileOffset = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.EntrySize = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.Unknown9 = Short.toUnsignedInt(bb.getShort());
            Header.Unknown10 = Short.toUnsignedInt(bb.getShort());
            Header.Unknown11 = bb.getInt();
            Header.Unknown12 = bb.getInt();
            Header.Unknown13 = bb.getInt();

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

    public interface ILvbGroupEntry {
        LvbGroupEntryType getType();
    }

    public enum LvbGroupEntryType {
        Unknown(0),
        Model (1),
        Light(3),
        Vfx(4),
        PositionMarker(5),
        Gimmick(6),
        Sound(7),
        EventNpc(8),
        BattleNpc(9),
        Aetheryte(12),
        EnvSpace(13),
        Gathering(14),
        SharedGroup15(15),// secondary variable is set to 13
        Treasure(16),
        Weapon(39),
        PopRange(40),
        ExitRange(41),
        MapRange(43),
        NaviMeshRange(44),
        EventObject(45),
        EnvLocation(47),
        EventRange(49),
        QuestMarker(51),
        CollisionBox(57),
        DoorRange(58),
        LineVfx(59),
        ClientPath(65),
        ServerPath(66),
        GimmickRange(67),
        TargetMarker(68),
        ChairMarker(69),
        ClickableRange(70),
        PrefetchRange(71),
        FateRange(72),
        SphereCastRange(75),
        ZoneMap(86);

        private final int id;

        LvbGroupEntryType(int i) {
            id = i;
        }

        public static LvbGroupEntryType valueOf(final int value){
            for(LvbGroupEntryType type : EnumSet.allOf(LvbGroupEntryType.class)){
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

    public interface ILvbData {
        LvbDataType getType();
    }

    public enum LvbDataType {
        Unknown0000(0),
        Unknown0008(0x0008),
        Group(0x0100),
        LgbGroup(0x0101);

        private final int id;

        LvbDataType(int i) {
            id = i;
        }

        public static LvbDataType valueOf(final int value){
            for(LvbDataType type : EnumSet.allOf(LvbDataType.class)){
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
            return cAddPathToDB(fullPath, archive,2);
        }
    }

    /**
     * ファイルの存在チェック後、ハッシュデータベース登録
     * @param fullPath フルパス
     * @param archive Indexファイル名
     * @param regMode 1:フォルダ名の一致のみでも登録する。2:ファイル名・パスが完全一致した時のみ登録
     * @return 登録結果 0:登録失敗 1:登録成功 2:ファイル名変更 3:ファイルパス変更 4:登録済みのため何もしない
     */
    @SuppressWarnings("SameParameterValue")
    private int cAddPathToDB(String fullPath, String archive, int regMode){
        SqPack_IndexFile sp_IndexFile;
        SqPack_IndexFile temp_IndexFile = currentIndex;

        int result = 0;

        if (currentIndex.getName().equals(archive)) {
            temp_IndexFile = currentIndex;
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
        } else if (archive.equals("080000")){
            if(vfxIndex == null) {
                try {
                    vfxIndex = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\080000.win32.index", false);
                    temp_IndexFile = vfxIndex;
                } catch (IOException e) {
                    Utils.getGlobalLogger().error(e);
                }
            }else{
                temp_IndexFile = vfxIndex;
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
                //該当ファイルが新規登録時のみ解析
                if (fullPath.endsWith(".envb")) {
                    try {
                        byte[] data = temp_IndexFile.extractFile(fullPath);
                        new ENVB_File(temp_IndexFile, data, temp_IndexFile.getEndian());
                    } catch (Exception e) {
                        Utils.getGlobalLogger().error(e);
                    }
                }
            }

        }else if (pathCheck == 1 && regMode == 1){
            //ファイルパスのみ追加
            String folder;
            if (fullPath.contains(".")) {
                folder = fullPath.substring(0, fullPath.lastIndexOf("/"));
            }else{
                folder = fullPath;
            }

            if (fullPath.endsWith(".mtrl") || fullPath.endsWith(".mdl")) {
                result = HashDatabase.addFolderToDB(folder, archive, false);
            }else {
                result = HashDatabase.addFolderToDB(folder, archive);
            }
        }
        return result;
    }
}
