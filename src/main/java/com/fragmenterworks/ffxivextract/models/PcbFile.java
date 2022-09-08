package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.EnumSet;

public class PcbFile extends Game_File {
    //他のファイルを見つけるために使用されます
    private final String FilePath;
    //現在表示中または呼び出し元のIndexファイル

    public static class HeaderData {
        public int Unknown1;   //uint
        public int Unknown2;   //uint
        public int EntryCount;
        public int IndicesCount;
        public long Padding;   //ulong
    }

    public static class HeaderData2 {
        public int EntryCount;   //uint
        public Vector3 UnknownVector1;
        public Vector3 UnknownVector2;
        public int Unknown1;   //uint
    }

    public HeaderData Header;
    public HeaderData2 Header2;

    public ArrayList<PcbBlockEntry> Data;

    public PcbFile(byte[] data, ByteOrder endian, String filePath){
        super(endian);
        FilePath = filePath;
        loadPCB(data);
    }

    public PcbFile(byte[] data, ByteOrder endian){
        super(endian);
        FilePath = "";
        loadPCB(data);
    }

    private void loadPCB(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        int entryOffset = 0x18;

        int signature = bb.getInt();
        bb.position(0);

        if (signature == 0) {
            Header = new HeaderData();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.EntryCount = bb.getInt();
            Header.IndicesCount = bb.getInt();
            Header.Padding = bb.getLong();


            Data = new ArrayList<>(Header.EntryCount);
            boolean isGroup = true;
            while (isGroup) {
                PcbBlockEntry entry = new PcbBlockEntry(bb, entryOffset);

                if (isGroup = (entry.Header.Type == PcbBlockDataType.Group)) {
                    ParsePcbBlockEntry(bb, entryOffset + 0x30, entryOffset);
                    entryOffset += entry.Header.BlockSize;
                } else {
                    ParsePcbBlockEntry(bb, entryOffset, entryOffset);
                }
            }
        }else{
            Header2 = new HeaderData2();
            Header2.EntryCount = bb.getInt();
            Header2.UnknownVector1 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header2.UnknownVector2 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header2.Unknown1 = bb.getInt();

            String folder = "~";
            if (!FilePath.equals("")) {
                folder = FilePath.substring(0, FilePath.lastIndexOf('/'));
            }

            HashDatabase.setAutoCommit(false);
            for (int i = 0; i < Header2.EntryCount; i++){
                new PcbTrBlockEntry(bb, folder);
            }
            HashDatabase.commit();
        }

    }

    private void ParsePcbBlockEntry(ByteBuffer bb, int entryOffset, int globalOffset){
        int offset = 0;
        int PcbBlockEntry_HeaderData_Size = 40;
        boolean isGroup = true;
        while (isGroup) {
            PcbBlockEntry entry = new PcbBlockEntry(bb, entryOffset + offset);

            if (isGroup = (entry.Header.Type == PcbBlockDataType.Group)) {
                ParsePcbBlockEntry(bb, entryOffset + offset + 0x30, globalOffset + offset + entryOffset);
                offset += entry.Header.BlockSize;
            }
            else {
                @SuppressWarnings("unused")
                int dOffset = PcbBlockEntry_HeaderData_Size + offset;
                @SuppressWarnings("unused")
                long blockSize = PcbBlockEntry_HeaderData_Size +
                        (long) entry.Header.VerticesCount * 3 * 4 +
                        (long) entry.Header.VerticesI16Count * 3 * 2 +
                        (long) entry.Header.IndicesCount * 3 * 2;

                if (entry.Header.VerticesCount != 0) {

                    entry.Data.Vertices = new Vector3[entry.Header.VerticesCount];
                    for (int i = 0; i < entry.Header.VerticesCount; ++i) {
                        if (bb.position() + 12 < bb.limit()) {
                            entry.Data.Vertices[i] = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
                        }else{
                            Utils.getGlobalLogger().trace("エントリー数が多すぎます");
                            break;
                        }
                    }
                }
                if (entry.Header.VerticesI16Count != 0) {
                    entry.Data.VerticesI16 = new PcbBlockData.VertexI16[entry.Header.VerticesI16Count];
                    for (int i = 0; i < entry.Header.VerticesI16Count; ++i) {
                        if (bb.position() + 6 < bb.limit()) {
                            entry.Data.VerticesI16[i]  = new PcbBlockData.VertexI16(bb.getShort(), bb.getShort(), bb.getShort());
                        }else{
                            Utils.getGlobalLogger().trace("エントリー数が多すぎます");
                            break;
                        }
                    }
                }
                if (entry.Header.IndicesCount != 0) {
                    entry.Data.Indices = new PcbBlockData.IndexData[entry.Header.IndicesCount];
                    for (int i = 0; i < entry.Header.IndicesCount; ++i) {
                        if (bb.position() + 12 <= bb.limit()) {
                            byte[] dataByte = new byte[12];
                            bb.get(dataByte);
                            entry.Data.Indices[i] = new PcbBlockData.IndexData(dataByte);
                        }else{
                            Utils.getGlobalLogger().trace("エントリー数が多すぎます");
                            break;
                        }
                    }
                }
                Data.add(entry);
            }
        }
    }

    public class PcbTrBlockEntry{
        public int trPcbID;   //uint
        public Vector3 UnknownVector1;
        public Vector3 UnknownVector2;
        public int Unknown1;   //uint

        public PcbTrBlockEntry(ByteBuffer bb, String Folder){
            trPcbID = bb.getInt();
            UnknownVector1 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            UnknownVector2 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Unknown1 = bb.getInt();

            if (!Folder.contains("~")){
                String pcbFilePath = String.format("%s/tr%04d.pcb", Folder, trPcbID);
                cAddPathToDB(pcbFilePath);
            }

            //bg/ex3/01_nvt_n4/rad/n4r3/collision/tr0000.pcb
        }
    }
    public static class PcbBlockEntry{
        public static class HeaderData{
            public PcbBlockDataType Type;       // 0 for entry, 0x30 for group
            public int BlockSize;       //uint // when group size in bytes for the group block
            public Vector3 Min;          // bounding box
            public Vector3 Max;
            public int VerticesI16Count; //ushort // number of vertices packed into 16 bit
            public int IndicesCount;  //ushort // number of indices
            public int VerticesCount;   //uint // number of normal float vertices
        }

        public int Offset;
        public HeaderData Header;
        public PcbBlockData Data;

        public PcbBlockEntry(ByteBuffer bb, int offset){
            bb.position(offset);

            this.Header = new HeaderData();

            Header.Type = PcbBlockDataType.valueOf(bb.getInt());
            Header.BlockSize = bb.getInt();
            Header.Min = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Max = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.VerticesI16Count = Short.toUnsignedInt(bb.getShort());
            Header.IndicesCount = Short.toUnsignedInt(bb.getShort());
            Header.VerticesCount = bb.getInt();

            Offset = bb.position();
            this.Data = new PcbBlockData();

            Build(bb);
        }

        @SuppressWarnings("unused")
        private void Build(ByteBuffer bb){
            //TODO
        }
    }

    public static class PcbBlockData {
        public static class VertexI16 {
            public short X; //ushort
            public short Y; //ushort
            public short Z; //ushort

            public VertexI16(short X, short Y, short Z) {
                this.X = X;
                this.Y = Y;
                this.Z = Z;
            }
        }

        public static class IndexData {
            public byte Index1;
            public byte Index2;
            public byte Index3;
            public byte Unknown1;
            public byte Unknown2;
            public byte Unknown3;
            public byte Unknown4;
            public byte Unknown5;
            public byte Unknown6;
            public byte Unknown7;
            public byte Unknown8;
            public byte Unknown9;

            public IndexData(byte[] indexData) {
                Index1 = indexData[0];
                Index2 = indexData[1];
                Index3 = indexData[2];
                Unknown1 = indexData[3];
                Unknown2 = indexData[4];
                Unknown3 = indexData[5];
                Unknown4 = indexData[6];
                Unknown5 = indexData[7];
                Unknown6 = indexData[8];
                Unknown7 = indexData[9];
                Unknown8 = indexData[10];
                Unknown9 = indexData[11];
            }
        }

        @SuppressWarnings("unused")
        public Vector3[] Vertices = new Vector3[1];
        @SuppressWarnings("unused")
        public VertexI16[] VerticesI16 = new VertexI16[1];
        @SuppressWarnings("unused")
        public IndexData[] Indices= new IndexData[1];

    }

    @SuppressWarnings("unused")
    public interface IPcbBlockData{
        PcbBlockDataType getType();
    }

    public enum PcbBlockDataType {
        Entry(0),
        Group(0x30);

        private final int id;

        PcbBlockDataType(int i) {
            id = i;
        }

        public static PcbBlockDataType valueOf(final int value){
            for(PcbBlockDataType type : EnumSet.allOf(PcbBlockDataType.class)){
                if(type.getId() == value) {
                    return type;
                }
            }
            return Entry;
        }

        public int getId() {
            return id;
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
