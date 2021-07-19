package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.ByteArrayExtensions;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CutbFile extends Game_File {

    //他のファイルを見つけるために使用されます
    private static SqPack_IndexFile currentIndex; //現在表示中または呼び出し元のIndexファイル

    //region Struct
    public static class HeaderData {
        public String Magic1;  // CUTB
        public int FileSize;   //uint
        public int ChunkEntryCount;   //uint
    }
    //endregion

    public HeaderData Header;
    public CutbGroupEntry[] Entry;

    /**
     * コンストラクタ
     * @param data sgbデータ
     * @param endian エンディアンの種類
     */
    public CutbFile(SqPack_IndexFile index, byte[] data, ByteOrder endian){
        super(endian);
        currentIndex = index;
        loadCUTB(data);
    }

    private void loadCUTB(byte[] data){
        Header = new HeaderData();

        byte[] signature = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        bb.get(signature);
        Header.Magic1 = new String(signature).trim(); //CUTB
        Header.FileSize = bb.getInt(); //ファイルサイズ
        Header.ChunkEntryCount = bb.getInt(); //データチャンク数

        if (!Header.Magic1.equals("CUTB")) {
            Utils.getGlobalLogger().error("CUTB magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic1);
        }

        Entry = new CutbGroupEntry[Header.ChunkEntryCount];

        int entryOffset = bb.position();
        for (int i = 0; i < Header.ChunkEntryCount; i++){
            Entry[i] = new CutbGroupEntry(bb, entryOffset);
            entryOffset += 0x10;
        }
        Utils.DummyLog("cutb読み込み完了");
    }

    public class CutbGroupEntry {
        public class HeaderData {
            public String Magic2;  // CTRL等
            public int HeaderSize;   //uint
            public int EntryOffset;
            public int EntrySize;
        }
        public HeaderData Header;
        public ICutbEntry Entry;

        public CutbGroupEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header = new HeaderData();
            byte[] signature = new byte[4];
            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt(); //ヘッダーサイズ
            int BaseOffset = bb.position();
            Header.EntryOffset = bb.getInt(); //エントリーのオフセット
            Header.EntrySize = bb.getInt();

            switch (Header.Magic2) {
                case "CTRL":
                    Entry = new CtrlEntry(bb, BaseOffset + Header.EntryOffset);
                    break;
                case "CTIS":
                    Entry = new CtisEntry(bb, BaseOffset + Header.EntryOffset);
                    break;
                case "CTDS":
                    Entry = new CtdsEntry(bb, BaseOffset + Header.EntryOffset);
                    break;
                case "CTEX":
                    Entry = new CtexEntry(bb, BaseOffset + Header.EntryOffset);
                    break;
                case "CTAL":
                    Entry = new CtalEntry(bb, BaseOffset + Header.EntryOffset);
                    break;
                case "CTPC":
                    Entry = new CtpcEntry(bb, BaseOffset + Header.EntryOffset);
                    break;
                case "CTCB":
                    Entry = new CtcbEntry(bb, BaseOffset + Header.EntryOffset);
                    break;
                case "CTPA":
                    Entry = new CtpaEntry(bb, BaseOffset + Header.EntryOffset);
                    break;
                case "CTTL":
                    Entry = new CttlEntry(bb, BaseOffset + Header.EntryOffset, Header.EntrySize);
                    break;
                default:
                    Utils.getGlobalLogger().info(String.format("%sのEntry解析は未実装", Header.Magic2));
                    break;
            }

        }
    }

    public class CtrlEntry implements ICutbEntry{
        public class HeaderData{
            public int EntriesOffset;
            public int EntryCount;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();
        public CtrlDataEntry[] Entry;

        public CtrlEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.EntriesOffset = bb.getInt();
            Header.EntryCount = bb.getInt();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();

            Entry = new CtrlDataEntry[Header.EntryCount];
            for (int i = 0; i < Header.EntryCount; i++){
                int entryOffset = offset + Header.EntriesOffset + i * 8;
                Entry[i] = new CtrlDataEntry(bb, entryOffset);
            }
            Utils.DummyLog("CtrlEntry");
        }

        public class CtrlDataEntry {
            public class HeaderData{
                public int ModelFileOffset;
                public int Unknown1;
            }

            public CtrlDataEntry.HeaderData Header = new CtrlDataEntry.HeaderData();
            public String ModelFile;

            public CtrlDataEntry(ByteBuffer bb, int offset){
                bb.position(offset);
                Header.ModelFileOffset = bb.getInt();
                Header.Unknown1 = bb.getInt();

                ModelFile = ByteArrayExtensions.ReadString(bb, offset + Header.ModelFileOffset);

                cAddPathToDB(ModelFile);
            }
        }
    }

    public static class CtisEntry implements ICutbEntry{
        public static class HeaderData{
            public int NameOffset;
            public byte UnknownFlag1;
            public byte UnknownFlag2;
            public byte UnknownFlag3;
            public byte UnknownFlag4;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public CtisEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.NameOffset = bb.getInt();
            Header.UnknownFlag1 = bb.get();
            Header.UnknownFlag2 = bb.get();
            Header.UnknownFlag3 = bb.get();
            Header.UnknownFlag4 = bb.get();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            Utils.DummyLog("CtisEntry");
        }
    }

    public static class CtdsEntry implements ICutbEntry{
        public static class HeaderData{
            public int NameOffset;
            public int UnknownAddress;
            public int Unknown1;
            public int Unknown2;
            public Vector4 UnknownVector1;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public Vector3 UnknownVector2;
            public int EntriesOffset;
            public int EntryCount;
            public int Unknown7;
            public int Unknown8;
            public int NameOffset2;
            public int Unknown9;
        }

        public HeaderData Header = new HeaderData();
        public CtdsDataEntry[] DataEntry;
        public String Name;

        public CtdsEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.NameOffset = bb.getInt();
            Header.UnknownAddress = bb.getInt();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.UnknownVector1 = new Vector4(bb.getFloat(), bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.UnknownVector2 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            int BaseOffset = bb.position();
            Header.EntriesOffset = bb.getInt();
            Header.EntryCount = bb.getInt();
            Header.Unknown7 = bb.getInt();
            Header.NameOffset2 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.Unknown9 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            DataEntry = new CtdsDataEntry[Header.EntryCount];
            for (int i = 0; i < Header.EntryCount; i++){
                int entryOffset = BaseOffset + Header.EntriesOffset + i * 8;
                DataEntry[i] = new CtdsDataEntry(bb, entryOffset);
            }

            Utils.DummyLog("CtdsEntry");
        }

        public static class CtdsDataEntry{

            public short UnknownShort1;
            public short UnknownShort2;
            public int Unknown1;

            public CtdsDataEntry (ByteBuffer bb, int offset){
                bb.position(offset);
                UnknownShort1 = bb.getShort();
                UnknownShort2 = bb.getShort();
                Unknown1 = bb.getInt();

                Utils.DummyLog("CtdsDataEntry");
            }
        }

    }

    public static class CtexEntry implements ICutbEntry{
        public static class HeaderData{
            public int UnknownOffset;
            public int UnknownID1;
            public int ID1_Data;
            public int UnknownID2;
            public int ID2_Data;
            public int UnknownID3;
            public int ID3_Data;
        }

        public HeaderData Header = new HeaderData();
        public String Name;
        public CtexDataEntry[] Entry;

        public CtexEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            int EntryCount = 1;
            Header.UnknownOffset = bb.getInt();


            Header.UnknownID1 = bb.getInt();
            Header.ID1_Data = bb.getInt();
            int tmpOffset = bb.position();
            Header.UnknownID2 = bb.getInt();
            Header.ID2_Data = bb.getInt();
            if (Header.UnknownID2 == 1){
                bb.position(tmpOffset);
            }else {
                EntryCount = 2;
                tmpOffset = bb.position();
                Header.UnknownID3 = bb.getInt();
                Header.ID3_Data = bb.getInt();
                if (Header.UnknownID3 == 1){
                    bb.position(tmpOffset);
                }else{
                    EntryCount = 3;
                }
            }

            Entry = new CtexDataEntry[EntryCount];
            for(int i = 0; i < EntryCount; i++){
                int BaseOffset = bb.position();
                Entry[i] = new CtexDataEntry(bb, BaseOffset);
            }

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.UnknownOffset);

            Utils.DummyLog("CtexEntry");
        }

        public static class CtexDataEntry{
            public int EntryID;
            public int EntriesOffset;
            public String Flag;
            public CtexDataEntry(ByteBuffer bb, int offset){
                bb.position(offset);
                EntryID = bb.getInt();
                EntriesOffset = bb.getInt();
                if(EntriesOffset != 0) {
                    Flag = ByteArrayExtensions.ReadString(bb, offset + EntriesOffset);
                }
            }
        }

    }

    public static class CtalEntry implements ICutbEntry{
        public static class HeaderData{
            public int EntriesOffset1;
            public int EntryCount1;
            public int EntriesOffset2;
            public int EntryCount2;
        }

        public HeaderData Header = new HeaderData();
        public CtalDataEntry[] Entry;
        public CtalDataEntry2[] Entry2;

        public CtalEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.EntriesOffset1 = bb.getInt();
            Header.EntryCount1 = bb.getInt();
            Header.EntriesOffset2 = bb.getInt();
            Header.EntryCount2 = bb.getInt();

            int BaseOffset = bb.position();
            Entry = new CtalDataEntry[Header.EntryCount1];
            for (int i = 0; i < Header.EntryCount1; i++){
                int entriesOffset = offset + Header.EntriesOffset1 + i * 4;
                bb.position(entriesOffset);
                int entryOffset = BaseOffset + bb.getInt();
                Entry[i] = new CtalDataEntry(bb, entryOffset);
            }

            Entry2 = new CtalDataEntry2[Header.EntryCount2];
            for (int i = 0; i < Header.EntryCount2; i++){
                int entriesOffset = offset + Header.EntriesOffset2 + i * 4;
                bb.position(entriesOffset);
                int entryOffset = BaseOffset + bb.getInt();
                Entry2[i] = new CtalDataEntry2(bb, entryOffset);
            }

            Utils.DummyLog("CtalEntry");
        }

        public static class CtalDataEntry {
            //※要検証
            public static class HeaderData {
                public int EntryCount;
                public short SumID;
                public short UnknownShort1;
                public int NameOffset;
            }

            public HeaderData Header = new HeaderData();
            public Vector2[] Vectors;
            public String Name;

            public CtalDataEntry(ByteBuffer bb, int offset){
                bb.position(offset);
                Header.EntryCount = bb.getInt();
                Header.SumID = bb.getShort();
                Header.UnknownShort1 = bb.getShort();
                Header.NameOffset = bb.getInt();

                Vectors = new Vector2[Header.EntryCount];
                for (int i = 0; i < Header.EntryCount; i++){
                    Vectors[i] = new Vector2(bb.getFloat(), bb.getFloat());
                }

                Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

                Utils.DummyLog("※要検証");
            }

        }

        public static class CtalDataEntry2 {
            //size:0x8c
            public static class HeaderData {
                public int EntryCount;
                public short SumID;
                public short UnknownShort1;
                public int NameOffset;
            }

            public HeaderData Header = new HeaderData();
            public Vector2[] Vectors;
            public String Name;

            public CtalDataEntry2(ByteBuffer bb, int offset){
                bb.position(offset);
                Header.EntryCount = bb.getInt();
                Header.SumID = bb.getShort();
                Header.UnknownShort1 = bb.getShort();
                Header.NameOffset = bb.getInt();

                Vectors = new Vector2[Header.EntryCount];
                for (int i = 0; i < Header.EntryCount; i++){
                    Vectors[i] = new Vector2(bb.getFloat(), bb.getFloat());
                }

                Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

                Utils.DummyLog("※要検証");
            }

        }
    }

    public static class CtpcEntry implements ICutbEntry{
        public static class HeaderData{
            public int Entry1Offset;
            public int Entry1Count;
            public int Entry2Offset;
            public int Entry2Count;
        }

        public HeaderData Header = new HeaderData();
        public CtpcDataEntry[] DataEntry;
        public String[] Name;

        public CtpcEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.Entry1Offset = bb.getInt();
            Header.Entry1Count = bb.getInt();
            Header.Entry2Offset = bb.getInt();
            Header.Entry2Count = bb.getInt();
            int BaseOffset = offset + Header.Entry1Offset;

            Name = new String[Header.Entry1Count];
            for (int i = 0; i < Header.Entry1Count; i++){
                int NameOffset = bb.getInt();

                Name[i] = ByteArrayExtensions.ReadString(bb, BaseOffset + NameOffset);
            }

            BaseOffset = offset + Header.Entry2Offset;

            DataEntry = new CtpcDataEntry[Header.Entry2Count];
            for (int i = 0; i < Header.Entry2Count; i++){
                int entryOffset = BaseOffset + i * 8;
                DataEntry[i] = new CtpcDataEntry(bb, entryOffset);
            }

            Utils.DummyLog("CtpcEntry");
        }

        public static class CtpcDataEntry {

            public int EntryOffset;
            public int EntryCount;

            public CtpcData[] Entry;

            public CtpcDataEntry(ByteBuffer bb, int offset){
                bb.position(offset);
                EntryOffset = bb.getInt();
                EntryCount = bb.getInt();

                Entry = new CtpcData[EntryCount];
                for (int i = 0; i < EntryCount; i++){
                    Entry[i] = new CtpcData(bb, offset + EntryOffset);
                }

                Utils.DummyLog("CtpcDataEntry");
            }
        }

        public static class CtpcData {

            public int Unknown1;
            public short Unknown2;
            public short Unknown3;
            public int Unknown4;

            public CtpcData(ByteBuffer bb, int offset){
                bb.position(offset);
                Unknown1 = bb.getInt();
                Unknown2 = bb.getShort();
                Unknown3 = bb.getShort();
                Unknown4 = bb.getInt();

                Utils.DummyLog("CtpcData");
            }
        }
    }

    public static class CtcbEntry implements ICutbEntry{
        public static class HeaderData{
            public int NameOffset;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public CtcbEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.NameOffset = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);
            Utils.DummyLog("CtcbEntry");
        }
    }

    public static class CtpaEntry implements ICutbEntry{
        public static class HeaderData{
            public int EntriesOffset;
            public int EntryCount;
        }

        public HeaderData Header = new HeaderData();
        public CtpaDataEntry[] Entry;

        public CtpaEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.EntriesOffset = bb.getInt();
            Header.EntryCount = bb.getInt();

            Entry = new CtpaDataEntry[Header.EntryCount];
            for (int i = 0; i < Header.EntryCount; i++){
                int entriesOffset = offset + Header.EntriesOffset + i * 0x0C;
                Entry[i] = new CtpaDataEntry(bb, entriesOffset);
            }
            Utils.DummyLog("CtpaEntry");
        }

        public static class CtpaDataEntry{
            public static class HeaderData{
                public short CtpaID;
                public short SumFlag;
                public int EntriesOffset;
                public int EntryCount;
            }
            public HeaderData Header = new HeaderData();
            public CtpaSubDataEntry[] SubDataEntry;

            public CtpaDataEntry(ByteBuffer bb, int offset){
                bb.position(offset);
                Header.CtpaID = bb.getShort();
                Header.SumFlag = bb.getShort();
                Header.EntriesOffset = bb.getInt();
                Header.EntryCount = bb.getInt();

                SubDataEntry = new CtpaSubDataEntry[Header.EntryCount];
                for (int i = 0; i < Header.EntryCount; i++){
                    int entriesOffset = offset + Header.EntriesOffset + i * 0x0C;
                    SubDataEntry[i] = new CtpaSubDataEntry(bb, entriesOffset);
                }

            }
        }

        public static class CtpaSubDataEntry{
            public static class HeaderData{
                public short CtpaSubID;
                public short SumFlag;
                public int EntriesOffset;
                public int EntryCount;
            }
            public HeaderData Header = new HeaderData();
            public CtpaEventNameEntry[] EventNameEntry;

            public CtpaSubDataEntry(ByteBuffer bb, int offset){
                bb.position(offset);
                Header.CtpaSubID = bb.getShort();
                Header.SumFlag = bb.getShort();
                Header.EntriesOffset = bb.getInt();
                Header.EntryCount = bb.getInt();

                EventNameEntry = new CtpaEventNameEntry[Header.EntryCount];
                for (int i = 0; i < Header.EntryCount; i++){
                    int entriesOffset = offset + Header.EntriesOffset + i * 0x0C;
                    EventNameEntry[i] = new CtpaEventNameEntry(bb, entriesOffset);
                }

            }
        }

        public static class CtpaEventNameEntry {
            public static class HeaderData{
                public short ID;
                public short SumFlag;
                public int EntriesOffset;
                public int NameOffset;
            }
            public HeaderData Header = new HeaderData();
            public String PapFilePath;

            public CtpaEventNameEntry(ByteBuffer bb, int offset){
                bb.position(offset);
                Header.ID = bb.getShort();
                Header.SumFlag = bb.getShort();
                Header.EntriesOffset = bb.getInt();
                Header.NameOffset = bb.getInt();

                PapFilePath = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);
                //例：chara/human/c0101/animation/a0001/bt_common/event_npc/event_cid0_driving_id.papの
                //「event_npc/event_cid0_driving_id」が入っている

                Utils.DummyLog("CtpaFileDataEntry");
            }
        }

    }

    public static class CttlEntry implements ICutbEntry{
        public TmbFile tmbFile;

        public CttlEntry(ByteBuffer bb, int offset, int DataSize){
            bb.position(offset);
            byte[] data = new byte[DataSize];
            bb.get(data);
            tmbFile = new TmbFile(data, currentIndex.getEndian());

        }
    }

    public interface ICutbEntry{

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
