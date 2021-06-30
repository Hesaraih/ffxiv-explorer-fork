package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.helpers.ByteArrayExtensions;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CutbFile extends Game_File {

    //他のファイルを見つけるために使用されます
    private static SqPack_IndexFile currentIndex; //現在表示中または呼び出し元のIndexファイル
    private static SqPack_IndexFile bgcommonIndex;

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
        Utils.getGlobalLogger().trace("cutb読み込み完了");
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
                case "CTAL":
                    Entry = new CtalEntry(bb, BaseOffset + Header.EntryOffset);
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
            public int EntryCount;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public CtisEntry(ByteBuffer bb, int offset){
            bb.position(offset);
            Header.NameOffset = bb.getInt();
            Header.EntryCount = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            if (Header.EntryCount > 1){
                Utils.getGlobalLogger().trace("Nameが複数あるかも");
            }

            Utils.getGlobalLogger().trace("CtisEntry");
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
            Header.Unknown8 = bb.getInt();
            Header.NameOffset2 = bb.getInt();
            Header.Unknown9 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

            DataEntry = new CtdsDataEntry[Header.EntryCount];
            for (int i = 0; i < Header.EntryCount + 1; i++){
                int entryOffset = BaseOffset + Header.EntriesOffset + i * 8;
                DataEntry[i] = new CtdsDataEntry(bb, entryOffset);
            }

            Utils.getGlobalLogger().trace("CtdsEntry");
        }

        public static class CtdsDataEntry{
            public float UnknownFloat;
            public int Unknown1;

            public CtdsDataEntry (ByteBuffer bb, int offset){
                bb.position(offset);
                UnknownFloat = bb.getFloat();
                Unknown1 = bb.getInt();

                Utils.getGlobalLogger().trace("CtdsDataEntry");
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

            Utils.getGlobalLogger().trace("CtalEntry");
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

                Utils.getGlobalLogger().trace("※要検証");
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

                Utils.getGlobalLogger().trace("※要検証");
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
        }
    }

    public class CtpaEntry implements ICutbEntry{
        public class HeaderData{
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

        }

        public class CtpaDataEntry{
            public class HeaderData{
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

        public class CtpaSubDataEntry{
            public class HeaderData{
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

        public class CtpaEventNameEntry {
            public class HeaderData{
                public short ID;
                public short SumFlag;
                public int EntriesOffset;
                public int NameOffset;
            }
            public HeaderData Header = new HeaderData();
            public String Name;

            public CtpaEventNameEntry(ByteBuffer bb, int offset){
                bb.position(offset);
                Header.ID = bb.getShort();
                Header.SumFlag = bb.getShort();
                Header.EntriesOffset = bb.getInt();
                Header.NameOffset = bb.getInt();

                Name = ByteArrayExtensions.ReadString(bb, offset + Header.NameOffset);

                cAddPathToDB(Name);
                Utils.getGlobalLogger().trace("CtpaFileDataEntry");
            }
        }

    }

    public static class CttlEntry implements ICutbEntry{
        public TmbFile tmbFile;

        public CttlEntry(ByteBuffer bb, int offset, int DataSize){
            bb.position(offset);
            byte[] data = new byte[DataSize];
            bb.get(data);
            tmbFile = new TmbFile(currentIndex, data, currentIndex.getEndian());

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
                if (fullPath.endsWith(".mdl")) {
                    //mdlファイル内のパスも解析
                    try {
                        byte[] data2 = temp_IndexFile.extractFile(fullPath);
                        Model tempModel = new Model(fullPath, temp_IndexFile, data2, temp_IndexFile.getEndian());
                        tempModel.loadVariant(1); //mdlファイルに関連するmtrlとtexの登録を試みる。
                    } catch (Exception modelException) {
                        modelException.printStackTrace();
                    }
                } else if (fullPath.endsWith(".avfx")) {
                    try {
                        //avfxファイル内のパスも解析
                        byte[] data2 = temp_IndexFile.extractFile(fullPath);
                        AVFX_File avfxFile = new AVFX_File(temp_IndexFile, data2, temp_IndexFile.getEndian());
                        avfxFile.regHash(true);
                    } catch (Exception avfxException) {
                        avfxException.printStackTrace();
                    }
                } else if (fullPath.endsWith(".sgb")) {
                    //sgbファイル内のパスも解析
                    try {
                        byte[] data2 = temp_IndexFile.extractFile(fullPath);
                        new SgbFile(temp_IndexFile, data2, temp_IndexFile.getEndian());
                    } catch (Exception sgbException) {
                        sgbException.printStackTrace();
                    }
                }
            }
        }
        return result;
    }
    //endregion

}
