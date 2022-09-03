package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.ByteArrayExtensions;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Objects;

public class LgbFile extends Game_File {

    public static class HeaderData {
        public String Magic1;   //uint  // LGB1
        public int FileSize;    //uint
        public int Unknown1;    //uint
        public String Magic2;   //uint  // LGP1
        public int LGP1_Size;    //uint
        public int Unknown14;    //uint
        public int NameOffset;    //uint
        public int GroupOffset;    //uint
        public int GroupCount;
        public int EntriesGroupSize; //old
        public int EntriesGroupCount; //old
    }

    public HeaderData Header;
    public ILgbData[] Data;
    public Vector3[] SubEntry;

    /**
     * コンストラクタ
     * @param data sgbデータ
     * @param endian エンディアンの種類
     */
    public LgbFile(byte[] data, ByteOrder endian){
        super(endian);
        loadLGB(data);
    }

    @SuppressWarnings("unused")
    private void loadLGB(byte[] data) {
        int baseOffset;

        Header = new HeaderData();

        byte[] signature = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);

        //region LGBファイルヘッダー
        bb.get(signature); //LGB1
        Header.Magic1 = new String(signature).trim();
        Header.FileSize = bb.getInt(); //ファイルサイズ
        Header.Unknown1 = bb.getInt(); //チャンク数 0x01 (LGP1しか存在しない)

        if (!Header.Magic1.equals("LGB1")) {
            Utils.getGlobalLogger().error("LGB1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic1);
            return;
        }
        //endregion

        bb.get(signature); //LGP1
        if (signature[0] == 1){
            //旧lgb？
            bb.position(0x20);
            bb.get(signature); //LGP1
            Header.Magic2 = new String(signature).trim();
            Header.LGP1_Size = bb.getInt(); //LGP1のヘッダ部サイズ
            bb.getInt(); //0
            bb.getInt(); //0
            baseOffset = bb.position();
            Header.Unknown14 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.EntriesGroupSize = bb.getInt(); //エントリーグループのサイズ
            Header.EntriesGroupCount = bb.getInt(); //グループ数

            Header.GroupOffset = 0x10;
            Header.GroupCount = 1; //グループ数


            if (!Header.Magic2.equals("LGP1")) {
                Utils.getGlobalLogger().error("LGP1 magic was incorrect.");
                Utils.getGlobalLogger().debug("Magic was {}", Header.Magic2);
                return;
            }

            Data = new LgbOldGroup[Header.GroupCount];
            for (int i = 0; i < Header.GroupCount; i++) {
                bb.position(baseOffset + Header.GroupOffset + i * 4);
                int groupOffset = baseOffset + Header.GroupOffset;
                if (groupOffset + 56 >= bb.limit()){
                    return;
                }
                Data[i] = new LgbOldGroup(bb, groupOffset, Header.EntriesGroupSize, Header.EntriesGroupCount);
            }

            bb.position(baseOffset + Header.GroupOffset + Header.EntriesGroupSize);

            SubEntry = new Vector3[Header.EntriesGroupCount];
            for (int i = 0; i < Header.EntriesGroupCount; i++) {
                SubEntry[i] = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            }

            Utils.DummyLog("旧lgb");

        }else{
            //通常
            Header.Magic2 = new String(signature).trim();
            Header.LGP1_Size = bb.getInt(); //LGP1のヘッダ部サイズ アドレス: +0x04
            baseOffset = bb.position();
            Header.Unknown14 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.GroupOffset = bb.getInt(); //エントリーグループヘッダのオフセット
            Header.GroupCount = bb.getInt(); //グループ数

            if (!Header.Magic2.equals("LGP1")) {
                Utils.getGlobalLogger().error("LGP1 magic was incorrect.");
                Utils.getGlobalLogger().debug("Magic was {}", Header.Magic2);
                return;
            }

            int EntriesOffset = bb.position();

            Data = new LgbGroup[Header.GroupCount];
            for (int i = 0; i < Header.GroupCount; ++i) {
                bb.position(baseOffset + Header.GroupOffset + i * 4);
                int groupOffset = EntriesOffset + bb.getInt();
                if (groupOffset + 56 >= bb.limit()){
                    return;
                }
                Data[i] = new LgbGroup(bb, groupOffset);
            }
        }

    }

    public class LgbGroup implements ILgbData{
        public class HeaderData{
            public int Unknown1; //uint
            public int GroupNameOffset;
            public int EntriesOffset;
            public int EntryCount;
            public int Unknown2; //uint
            public int UnknownOffset3; //uint
            public int FestivalId; //uint
            public int Unknown5; //uint
            public int MapIndex; //uint
            public int SubEntriesOffset; //uint
            public int SubEntryCount; //uint
            public int Entries3Offset; //uint
            public int Entries3Count; //uint
            public int Unknown11; //uint
        }
        public HeaderData Header = new HeaderData();
        public String Name;
        public ILgbEntry[] Entries;
        public ILgbEntry[] SubEntries;

        LgbGroup(ByteBuffer bb, int offset){
            bb.position(offset);

            Header.Unknown1 = bb.getInt();
            Header.GroupNameOffset = bb.getInt();
            Header.EntriesOffset = bb.getInt();
            Header.EntryCount = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.UnknownOffset3 = bb.getInt();  //offset？
            Header.FestivalId = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.MapIndex = bb.getInt();
            Header.SubEntriesOffset = bb.getInt();
            Header.SubEntryCount = bb.getInt();
            Header.Entries3Offset = bb.getInt();
            Header.Entries3Count = bb.getInt();
            Header.Unknown11 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.GroupNameOffset);

            if (Header.EntryCount < 0){
                return;
            }

            int entriesOffset = offset + Header.EntriesOffset;
            Entries = new ILgbEntry[Header.EntryCount];

            for(int i = 0; i < Header.EntryCount; i++){
                if(entriesOffset + i * 4 > bb.limit()){
                    return;
                }
                bb.position(entriesOffset + i * 4);
                int entryOffset = entriesOffset + bb.getInt(); //安全のためにlong化したほうがいいかも
                if (entryOffset >= bb.limit()){
                    return;
                }
                bb.position(entryOffset);

                LgbEntryType type = LgbEntryType.valueOf(bb.getInt());
                switch (Objects.requireNonNull(type)) {
                    case Model:
                        //92byte(0x5c byte)
                        Entries[i] = new LgbModelEntry(bb, entryOffset);
                        break;
                    case Gimmick:
                    case SharedGroup15:
                        //152byte(0x98 byte)
                        Entries[i] = new LgbGimmickEntry(bb, entryOffset);
                        break;
                    case EventObject:
                        //68byte(0x44 byte)
                        Entries[i] = new LgbEventObjectEntry(bb, entryOffset);
                        break;
                    case Light:
                        //108byte(0x6c byte)
                        Entries[i] = new LgbLightEntry(bb, entryOffset);
                        break;
                    case EventNpc:
                        //88byte(0x58 byte)
                        Entries[i] = new LgbENpcEntry(bb, entryOffset);
                        break;
                    case Vfx:
                        //88byte(0x58 byte)
                        Entries[i] = new LgbVfxEntry(bb, entryOffset);
                        break;
                    case CollisionBox:
                        //88byte(0x58 byte)
                        Entries[i] = new LgbCollisionBoxEntry(bb, entryOffset);
                        break;
                    case EnvLocation:
                        //56byte(0x38 byte)
                        Entries[i] = new LgbEnvLocationEntry(bb, entryOffset);
                        break;
                    case EnvSpace:
                        //84byte(0x54 byte)
                        Entries[i] = new LgbEnvSpaceEntry(bb, entryOffset);
                        break;
                    case ClientPath:
                        //224byte(0xE0 byte)
                        Entries[i] = new LgbClientPathEntry(bb, entryOffset);
                        break;
                    case ZoneMap:
                        //64byte(0x40 byte)
                        Entries[i] = new LgbZoneMapEntry(bb, entryOffset);
                        break;
                    case MapRange:
                        //108byte(0x6c byte)
                        Entries[i] = new LgbMapRangeEntry(bb, entryOffset);
                        break;
                    case Sound:
                        //684byte(0x2ac byte)
                        Entries[i] = new LgbSoundEntry(bb, entryOffset);
                        break;
                    case ChairMarker:
                        //56byte(0x38 byte)
                        Entries[i] = new LgbChairMarkerEntry(bb, entryOffset);
                        break;
                    case PopRange:
                        //312byte(0x138 byte)
                        Entries[i] = new LgbPopRangeEntry(bb, entryOffset);
                        break;
                    case SharedGroup40:
                        //312byte(0x138 byte)
                        Entries[i] = new LgbSharedGroup40Entry(bb, entryOffset);
                        break;
                    case Aetheryte:
                    case LineVfx:
                        //60byte(0x3c byte)
                        Entries[i] = new LgbLineVfxEntry(bb, entryOffset);
                        break;
                    case EventRange:
                        //72byte(0x48 byte)
                        Entries[i] = new LgbEventRangeEntry(bb, entryOffset);
                        break;
                    case Treasure:
                        //64byte(0x40 byte)
                        Entries[i] = new LgbTreasureEntry(bb, entryOffset);
                        break;
                    case TargetMarker:
                        //64byte(0x40 byte)
                        Entries[i] = new LgbTargetMarkerEntry(bb, entryOffset);
                        break;
                    case SharedGroup83:
                        //100byte(0x64 byte)
                        Entries[i] = new LgbSharedGroup83Entry(bb, entryOffset);
                        break;
                    case SharedGroup87:
                        //72～100byte(0x48～0x64 byte)
                        Entries[i] = new LgbSharedGroup87Entry(bb, entryOffset);
                        break;
                    case PrefetchRange:
                        //68byte(0x44 byte)
                        Entries[i] = new LgbPrefetchRangeEntry(bb, entryOffset);
                        break;
                    case FateRange:
                        //64byte(0x40 byte)
                        Entries[i] = new LgbFateRangeEntry(bb, entryOffset);
                        break;
                    case DoorRange:
                        //34byte(0x52 byte)
                        Entries[i] = new LgbDoorRangeEntry(bb, entryOffset);
                        break;
                    default:
                        Utils.getGlobalLogger().trace(String.format("%sのEntry解析は未実装", type.name()));
                        break;
                }
            }

            int subEntriesOffset = offset + Header.SubEntriesOffset;
            SubEntries = new ILgbEntry[Header.SubEntryCount];
            for(int i = 0; i < Header.SubEntryCount; i++){
                bb.position(subEntriesOffset + i * 12);

                SubEntries[i] = new LgbObstructionEntry(bb, subEntriesOffset);
            }
        }

    }

    public class LgbOldGroup implements ILgbData{
        public class HeaderData{
            public int Unknown1; //uint
            public int GroupNameOffset;
            public int EntriesOffset;
            public int EntryCount;
            public int Unknown2; //uint
            public int UnknownOffset3; //uint
            public int FestivalId; //uint
            public int Unknown5; //uint
            public int MapIndex; //uint
            public int SubEntriesOffset; //uint
            public int SubEntryCount; //uint
        }
        public HeaderData Header = new HeaderData();
        public String Name;
        public ILgbEntry[] Entries;

        LgbOldGroup(ByteBuffer bb, int offset, int DataSize, int GroupCount){
            bb.position(offset);

            Header.Unknown1 = bb.getInt();
            Header.GroupNameOffset = bb.getInt();
            Header.EntriesOffset = bb.getInt();
            Header.EntryCount = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.UnknownOffset3 = bb.getInt();  //offset？
            Header.FestivalId = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.MapIndex = bb.getInt();
            Header.SubEntriesOffset = bb.getInt();
            Header.SubEntryCount = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.GroupNameOffset);

            if (GroupCount < 0){
                return;
            }

            int entriesOffset = offset + Header.EntriesOffset;
            Entries = new ILgbEntry[GroupCount];

            for(int i = 0; i < GroupCount; i++){
                if(entriesOffset >= offset + DataSize){
                    return;
                }
                bb.position(entriesOffset);
                int entryOffset = entriesOffset + bb.getInt(); //安全のためにlong化したほうがいいかも
                if(entryOffset >= bb.limit()){
                    return;
                }
                bb.position(entryOffset);

                LgbEntryType type = LgbEntryType.valueOf(bb.getInt());
                switch (Objects.requireNonNull(type)) {
                    case Model:
                        //92byte(0x5c byte)
                        Entries[i] = new LgbModelEntry(bb, entryOffset);
                        break;
                    case Gimmick:
                    case SharedGroup15:
                        //152byte(0x98 byte)
                        Entries[i] = new LgbGimmickEntry(bb, entryOffset);
                        break;
                    case EventObject:
                        //68byte(0x44 byte)
                        Entries[i] = new LgbEventObjectEntry(bb, entryOffset);
                        break;
                    case Light:
                        //108byte(0x6c byte)
                        Entries[i] = new LgbLightEntry(bb, entryOffset);
                        break;
                    case EventNpc:
                        //88byte(0x58 byte)
                        Entries[i] = new LgbENpcEntry(bb, entryOffset);
                        break;
                    case Vfx:
                        //88byte(0x58 byte)
                        Entries[i] = new LgbVfxEntry(bb, entryOffset);
                        break;
                    case PositionMarker:
                        //88byte(0x58 byte)
                        Entries[i] = new LgbPositionMarkerEntry(bb, entryOffset);
                        break;
                    case CollisionBox:
                        //88byte(0x58 byte)
                        Entries[i] = new LgbCollisionBoxEntry(bb, entryOffset);
                        break;
                    case EnvLocation:
                        //56byte(0x38 byte)
                        Entries[i] = new LgbEnvLocationEntry(bb, entryOffset);
                        break;
                    case EnvSpace:
                        //84byte(0x54 byte)
                        Entries[i] = new LgbEnvSpaceEntry(bb, entryOffset);
                        break;
                    case ClientPath:
                        //224byte(0xE0 byte)
                        Entries[i] = new LgbClientPathEntry(bb, entryOffset);
                        break;
                    case ZoneMap:
                        //64byte(0x40 byte)
                        Entries[i] = new LgbZoneMapEntry(bb, entryOffset);
                        break;
                    case MapRange:
                        //108byte(0x6c byte)
                        Entries[i] = new LgbMapRangeEntry(bb, entryOffset);
                        break;
                    case Sound:
                        //684byte(0x2ac byte)
                        Entries[i] = new LgbSoundEntry(bb, entryOffset);
                        break;
                    case ChairMarker:
                        //56byte(0x38 byte)
                        Entries[i] = new LgbChairMarkerEntry(bb, entryOffset);
                        break;
                    case PopRange:
                        //312byte(0x138 byte)
                        Entries[i] = new LgbPopRangeEntry(bb, entryOffset);
                        break;
                    case SharedGroup40:
                        //312byte(0x138 byte)
                        Entries[i] = new LgbSharedGroup40Entry(bb, entryOffset);
                        break;
                    case Aetheryte:
                    case LineVfx:
                        //60byte(0x3c byte)
                        Entries[i] = new LgbLineVfxEntry(bb, entryOffset);
                        break;
                    case EventRange:
                        //72byte(0x48 byte)
                        Entries[i] = new LgbEventRangeEntry(bb, entryOffset);
                        break;
                    case Treasure:
                        //64byte(0x40 byte)
                        Entries[i] = new LgbTreasureEntry(bb, entryOffset);
                        break;
                    case TargetMarker:
                        //64byte(0x40 byte)
                        Entries[i] = new LgbTargetMarkerEntry(bb, entryOffset);
                        break;
                    case SharedGroup83:
                        //100byte(0x64 byte)
                        Entries[i] = new LgbSharedGroup83Entry(bb, entryOffset);
                        break;
                    case SharedGroup87:
                        //72～100byte(0x48～0x64 byte)
                        Entries[i] = new LgbSharedGroup87Entry(bb, entryOffset);
                        break;
                    case PrefetchRange:
                        //68byte(0x44 byte)
                        Entries[i] = new LgbPrefetchRangeEntry(bb, entryOffset);
                        break;
                    case FateRange:
                        //64byte(0x40 byte)
                        Entries[i] = new LgbFateRangeEntry(bb, entryOffset);
                        break;
                    default:
                        Utils.getGlobalLogger().trace(String.format("%sのEntry解析は未実装", type.name()));
                        break;
                }

                entriesOffset = bb.position();
            }
        }

    }

    public class LgbModelEntry implements ILgbEntry{
        public class HeaderData {
            public LgbEntryType Type;
            public int Unknown2; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int ModelFileOffset;
            public int CollisionFileOffset;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
            public int Unknown8;
            public int Unknown9;
            public int Unknown10;
            public int Unknown11;
            public float Unknown12;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;
        public String ModelFilePath;
        public String CollisionFilePath;

        public LgbModelEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.Unknown2 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.ModelFileOffset = bb.getInt();
            Header.CollisionFileOffset = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.Unknown9 = bb.getInt();
            Header.Unknown10 = bb.getInt();
            Header.Unknown11 = bb.getInt();
            Header.Unknown12 = bb.getFloat();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
            ModelFilePath = ByteArrayExtensions.ReadString(bb,offset + Header.ModelFileOffset);
            CollisionFilePath = ByteArrayExtensions.ReadString(bb,offset + Header.CollisionFileOffset);
            cAddPathToDB(ModelFilePath);
            cAddPathToDB(CollisionFilePath);

        }

    }

    public class LgbObstructionEntry implements ILgbEntry{
        public class HeaderData {
            public LgbEntryType Type;
            public int Unknown2; //uint
            public int ObsetNameOffset;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String ObsetName;

        public LgbObstructionEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.Unknown2 = bb.getInt();
            Header.ObsetNameOffset = bb.getInt();

            ObsetName = ByteArrayExtensions.ReadString(bb,offset + Header.ObsetNameOffset);
            cAddPathToDB(ObsetName);
        }

    }

    public class LgbGimmickEntry implements ILgbEntry{
        public class HeaderData {
            public LgbEntryType Type;
            public int GimmickId; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int GimmickFileOffset;
            public int Unknown1; //uint
            public int Unknown2; //uint
            public int Unknown3; //uint
            public int Unknown4; //uint
            public int Unknown5; //uint
            public int Unknown6; //uint
            public int Unknown7; //uint
            public int Unknown8; //uint
            public int Unknown9; //uint
            public int Unknown10; //uint
            public int Unknown11; //uint
            public int Unknown12; //uint
            public int Unknown13; //uint
            public int Unknown14; //uint
            public int Unknown15; //uint
            public int Unknown16; //uint
            public int Unknown17; //uint
            public int Unknown18; //uint
            public int Unknown19; //uint
            public Vector2 Entry1;
            public int Unknown20; //uint
            public int Unknown21; //uint
            public Vector2 Entry2;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;
        public String GimmickFilePath;

        public LgbGimmickEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.GimmickId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.GimmickFileOffset = bb.getInt();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.Unknown9 = bb.getInt();
            Header.Unknown10 = bb.getInt();
            Header.Unknown11 = bb.getInt();
            Header.Unknown12 = bb.getInt();
            Header.Unknown13 = bb.getInt();
            Header.Unknown14 = bb.getInt();
            Header.Unknown15 = bb.getInt();
            Header.Unknown16 = bb.getInt();
            Header.Unknown17 = bb.getInt();
            Header.Unknown18 = bb.getInt();
            Header.Unknown19 = bb.getInt();
            Header.Entry1 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.Unknown20 = bb.getInt();
            Header.Unknown21 = bb.getInt();
            Header.Entry2 = new Vector2(bb.getFloat(), bb.getFloat());

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
            GimmickFilePath = ByteArrayExtensions.ReadString(bb,offset + Header.GimmickFileOffset);
            cAddPathToDB(GimmickFilePath);
        }

    }

    public static class LgbEventObjectEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int Unknown2; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int EventObjectId;
            public int GimmickId;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbEventObjectEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.Unknown2 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.EventObjectId = bb.getInt();
            Header.GimmickId = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);

        }

    }

    public static class LgbLightEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int Unknown2; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int EntryCount; //uint
            public Vector2 Entry1;
            public int UnknownFlag1;
            public Vector2 Entry2;
            public int Entry2NameOffset;
            public int Entry3NameOffset; //ushort
            public int UnknownFlag2; //ushort
            public Vector2 Entry3;
            public int UnknownFlag3; //ushort
            public int UnknownFlag4; //ushort
            public Vector2 Entry4;
            public Vector2 Entry5;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbLightEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.Unknown2 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.EntryCount = bb.getInt();
            Header.Entry1 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.UnknownFlag1 = bb.getInt();
            Header.Entry2 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.Entry2NameOffset = bb.getInt();
            Header.Entry3NameOffset = bb.getShort() & 0xffff;
            Header.UnknownFlag2 = bb.getShort() & 0xffff;
            Header.Entry3 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.UnknownFlag3 = bb.getShort() & 0xffff;
            Header.UnknownFlag4 = bb.getShort() & 0xffff;
            Header.Entry4 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.Entry5 = new Vector2(bb.getFloat(), bb.getFloat());

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
        }

    }

    public static class LgbENpcEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
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
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbENpcEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
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

    public class LgbVfxEntry implements ILgbEntry{
        public class HeaderData {
            public LgbEntryType Type;
            public int Unknown04; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown34;
            public int Unknown38;
            public int Unknown3C;
            public int ExtensionNameOffset;
            public int Unknown44;
            public int Unknown48;
            public int Unknown4C;
            public int Unknown50;
            public int Unknown54;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;
        public String VfxPass;

        public LgbVfxEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.Unknown04 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown34 = bb.getInt();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();
            Header.ExtensionNameOffset = bb.getInt();
            Header.Unknown44 = bb.getInt();
            Header.Unknown48 = bb.getInt();
            Header.Unknown4C = bb.getInt();
            Header.Unknown50 = bb.getInt();
            Header.Unknown54 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
            VfxPass = ByteArrayExtensions.ReadString(bb,offset + Header.SomeId);
            cAddPathToDB(VfxPass);
        }

    }

    public static class LgbPositionMarkerEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int Unknown04; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown34;
            public int Unknown38;
            public int Unknown3C;
            public int ExtensionNameOffset;
            public int Unknown44;
            public int Unknown48;
            public int Unknown4C;

            public int Unknown50;
            public int Unknown54;
            public int Unknown58;
            public int Unknown5C;

            public int Unknown60;
            public int Unknown64;
            public int Unknown68;
            public int Unknown6C;

            public int Unknown70;
            public int Unknown74;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;
        public String ExtensionName;

        public LgbPositionMarkerEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.Unknown04 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown34 = bb.getInt();
            Header.Unknown38 = bb.getInt();
            int BaseOffset = bb.position();
            Header.Unknown3C = bb.getInt();
            Header.ExtensionNameOffset = bb.getInt();
            Header.Unknown44 = bb.getInt();
            Header.Unknown48 = bb.getInt();
            Header.Unknown4C = bb.getInt();

            Header.Unknown50 = bb.getInt();
            Header.Unknown54 = bb.getInt();
            Header.Unknown58 = bb.getInt();
            Header.Unknown5C = bb.getInt();

            Header.Unknown60 = bb.getInt();
            Header.Unknown64 = bb.getInt();
            Header.Unknown68 = bb.getInt();
            Header.Unknown6C = bb.getInt();

            Header.Unknown70 = bb.getInt();
            Header.Unknown74 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
            ExtensionName = ByteArrayExtensions.ReadString(bb,BaseOffset + Header.ExtensionNameOffset);

            Utils.DummyLog("PositionMarkerEntry");
        }

    }

    public static class LgbCollisionBoxEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int CollisionBoxId; //uint
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
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbCollisionBoxEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.CollisionBoxId = bb.getInt();
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

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);

        }

    }

    public class LgbEnvLocationEntry implements ILgbEntry{
        public class HeaderData {
            public LgbEntryType Type;
            public int EnvLocationId; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int ambFileOffset;
            public int textureFileOffset;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;
        public String ambFile;
        public String EnvTextureFile;

        public LgbEnvLocationEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.EnvLocationId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.ambFileOffset = bb.getInt();
            Header.textureFileOffset = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
            ambFile = ByteArrayExtensions.ReadString(bb,offset + Header.ambFileOffset);
            EnvTextureFile = ByteArrayExtensions.ReadString(bb,offset + Header.textureFileOffset);
            cAddPathToDB(ambFile);
            cAddPathToDB(EnvTextureFile);
        }

    }

    public class LgbEnvSpaceEntry implements ILgbEntry{
        public class HeaderData {
            public LgbEntryType Type;
            public int EnvSpaceId; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int envbFileOffset;
            public int UnknownOffset; //子ID？
            public int Unknown4;
            public int Unknown5; //ID？ type？ flag？
            public float Unknown6; //float
            public int Unknown7;
            public float Unknown8; //float
            public float Unknown9; //float
            public int essbFileOffset;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;
        public String envbFile;
        public String essbFile;

        public LgbEnvSpaceEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.EnvSpaceId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.envbFileOffset = bb.getInt();
            Header.UnknownOffset = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getFloat();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getFloat();
            Header.Unknown9 = bb.getFloat();
            Header.essbFileOffset = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
            envbFile = ByteArrayExtensions.ReadString(bb,offset + Header.envbFileOffset);
            essbFile = ByteArrayExtensions.ReadString(bb,offset + Header.essbFileOffset);

            cAddPathToDB(envbFile);
            cAddPathToDB(essbFile);
        }

    }

    public static class LgbClientPathEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int ClientPathId; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int ClientEntryOffset;
            public int ClientEntryNum;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
            public int Unknown8;
            public int Unknown9;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public HashMap<Integer, Vector3> ClientEntry;
        public String Name;

        public LgbClientPathEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.ClientPathId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.ClientEntryOffset = bb.getInt();
            Header.ClientEntryNum = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.Unknown9 = bb.getInt();

            ClientEntry = new HashMap<>();

            for(int i = 0; i < Header.ClientEntryNum; i++){
                Vector3 tmpClientEntry = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
                int key = bb.getInt();
                ClientEntry.put(key, tmpClientEntry);
            }

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
        }

    }

    public static class LgbZoneMapEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int ZoneMapId; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int ZoneMapType;
            public int UnknownOffset;
            public int Unknown4;
            public int Unknown5;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbZoneMapEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.ZoneMapId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.ZoneMapType = bb.getInt();
            Header.UnknownOffset = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
        }

    }

    public class LgbSoundEntry implements ILgbEntry{
        public class HeaderData {
            public LgbEntryType Type;
            public int EnvSpaceId; //uint
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
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public Vector4[] SoundEnvEntry;
        public String Name;
        public String scdFile;

        public LgbSoundEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.EnvSpaceId = bb.getInt();
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

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
            scdFile = ByteArrayExtensions.ReadString(bb,offset + Header.scdFileOffset);

            cAddPathToDB(scdFile);
        }

    }

    public static class LgbMapRangeEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
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
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbMapRangeEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
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

    public static class LgbChairMarkerEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int Unknown2; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown3;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbChairMarkerEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.Unknown2 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown3 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
        }

    }

    public static class LgbPopRangeEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int PopRangeId; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public short EntryCount;
            public short Unknown36;
            public int Unknown38;
            public int Unknown3C;
            public int Unknown40;
            public int Unknown44;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;
        public Vector3[] PopPositionEntry;
        public int Unknown48, Unknown4C, Unknown54, Unknown58;
        public float Unknown50;

        public LgbPopRangeEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.PopRangeId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.EntryCount = bb.getShort();
            Header.Unknown36 = bb.getShort();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();
            Header.Unknown40 = bb.getInt();
            Header.Unknown44 = bb.getInt();

            if(Header.Unknown36 == 1){
                //ブロックサイズ0x5C(92)の時
                Unknown48 = bb.getInt();
                Unknown4C = bb.getInt();
                Unknown50 = bb.getFloat();
                Unknown54 = bb.getInt();
                Unknown58 = bb.getInt();
                Utils.DummyLog("要検証");
            }else {
                PopPositionEntry = new Vector3[Header.EntryCount];
                for (int i = 0; i < Header.EntryCount; i++) {
                    if (bb.position() + 12 <= bb.limit()) {
                        PopPositionEntry[i] = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
                    } else {
                        Utils.getGlobalLogger().error("エントリー数が多いかも");
                        break;
                    }
                }
            }
            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
        }

    }

    public static class LgbSharedGroup40Entry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int PopRangeId; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int EntryCount;
            public int Unknown38;
            public int Unknown3C;
            public int Unknown40;
            public int Unknown44;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;
        public Vector3[] PopPositionEntry;

        public LgbSharedGroup40Entry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.PopRangeId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.EntryCount = bb.getInt();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();
            Header.Unknown40 = bb.getInt();
            Header.Unknown44 = bb.getInt();

            PopPositionEntry = new Vector3[Header.EntryCount];
            for (int i = 0; i < Header.EntryCount; i++){
                if (bb.position() + 12 <= bb.limit()) {
                    PopPositionEntry[i] = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
                }else{
                    Utils.getGlobalLogger().error("エントリー数が多いかも");
                    break;
                }
            }

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
        }

    }

    public static class LgbLineVfxEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int Unknown2; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown3;
            public int Unknown4;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbLineVfxEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.Unknown2 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
        }

    }

    public static class LgbEventRangeEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int Unknown2; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
            public int Unknown8;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbEventRangeEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.Unknown2 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = Short.toUnsignedInt(bb.getShort());
            Header.Unknown6 = Short.toUnsignedInt(bb.getShort());
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
        }

    }

    public static class LgbTreasureEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int Unknown2; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbTreasureEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.Unknown2 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);
        }

    }

    public static class LgbTargetMarkerEntry implements ILgbEntry {
        public static class HeaderData {
            public LgbEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown3;
        }

        public LgbEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbTargetMarkerEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
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

    public static class LgbSharedGroup83Entry implements ILgbEntry {
        public static class HeaderData {
            public LgbEntryType Type;
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

        public LgbEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbSharedGroup83Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
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

            Utils.DummyLog("SharedGroup83Entry");
        }
    }

    public static class LgbSharedGroup87Entry implements ILgbEntry {
        public static class HeaderData {
            public LgbEntryType Type;
            public int UnknownId;  //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public int Unknown34;
            public int Unknown38;
            public int Unknown3C;
            public int EntryCount;
        }

        public LgbEntryType getType(){
            return Header.Type;
        }
        public HeaderData Header = new HeaderData();
        public String Name;
        public float[] Entry;

        public LgbSharedGroup87Entry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.UnknownId = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown34 = bb.getInt();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();
            Header.EntryCount = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            Entry = new float[Header.EntryCount];
            for (int i = 0; i < Header.EntryCount; i++){
                Entry[i] = Float.intBitsToFloat(bb.getInt() << 12);
            }

            Utils.DummyLog("SharedGroup87Entry");
        }
    }

    public static class LgbPrefetchRangeEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int Unknown2; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public short Unknown34;
            public short Unknown36;
            public int Unknown38;
            public int Unknown3C;
            public int Unknown40;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbPrefetchRangeEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.Unknown2 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown34 = bb.getShort();
            Header.Unknown36 = bb.getShort();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();
            Header.Unknown40 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);

            Utils.DummyLog("PrefetchRangeEntry");
        }

    }

    public static class LgbFateRangeEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int Unknown2; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
            public short Unknown34;
            public short Unknown36;
            public int Unknown38;
            public int Unknown3C;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbFateRangeEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.Unknown2 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();
            Header.Unknown34 = bb.getShort();
            Header.Unknown36 = bb.getShort();
            Header.Unknown38 = bb.getInt();
            Header.Unknown3C = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);

            Utils.DummyLog("FateRangeEntry");
        }

    }

    public static class LgbDoorRangeEntry implements ILgbEntry{
        public static class HeaderData {
            public LgbEntryType Type;
            public int Unknown2; //uint
            public int NameOffset;
            public Vector3 Translation;
            public Vector3 Rotation;
            public Vector3 Scale;
            public int SomeId;
        }
        public LgbEntryType getType(){
            return Header.Type;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public LgbDoorRangeEntry(ByteBuffer bb, int offset) {
            bb.position(offset);
            Header.Type = LgbEntryType.valueOf(bb.getInt());
            Header.Unknown2 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Translation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Rotation = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Scale = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.SomeId = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb,offset + Header.NameOffset);

            Utils.DummyLog("DoorRangeEntry");
        }

    }

    public interface ILgbData{
    }

    public interface ILgbEntry{
        LgbEntryType getType();
    }

    public enum LgbEntryType{
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
        SharedGroup40(0x28),
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
        SharedGroup87(0x57),
        UnknownType( 0xffff);

        private final int id;

        LgbEntryType(int i) {
            id = i;
        }

        public static LgbEntryType valueOf(final int value){
            for(LgbEntryType type : EnumSet.allOf(LgbEntryType.class)){
                if(type.getId() == value) {
                    return type;
                }
            }
            Utils.getGlobalLogger().info(String.format("未知のEntryType : %s", value));
            return UnknownType;
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
                if (fullPath.endsWith(".mdl")) {
                    try {
                        byte[] data = temp_IndexFile.extractFile(fullPath);
                        Model tempModel = new Model(fullPath, temp_IndexFile, data, temp_IndexFile.getEndian());
                        tempModel.loadVariant(1); //mdlファイルに関連するmtrlとtexの登録を試みる。
                    } catch (Exception modelException) {
                        modelException.printStackTrace();
                    }
                }else if (fullPath.endsWith(".avfx")) {
                    try {
                    byte[] data = temp_IndexFile.extractFile(fullPath);
                    AVFX_File avfxFile = new AVFX_File(temp_IndexFile, data, temp_IndexFile.getEndian());
                    avfxFile.regHash();
                    } catch (Exception e) {
                        Utils.getGlobalLogger().error(e);
                    }
                }else if (fullPath.endsWith(".sgb")) {
                    try {
                        byte[] data = temp_IndexFile.extractFile(fullPath);
                        new SgbFile(data, temp_IndexFile.getEndian());
                    } catch (Exception e) {
                        Utils.getGlobalLogger().error(e);
                    }
                }else if (fullPath.endsWith(".envb")) {
                    try {
                        byte[] data = temp_IndexFile.extractFile(fullPath);
                        new ENVB_File(data, temp_IndexFile.getEndian());
                    } catch (Exception e) {
                        Utils.getGlobalLogger().error(e);
                    }
                }else if (fullPath.endsWith(".essb")) {
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
