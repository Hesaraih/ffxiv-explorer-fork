package com.fragmenterworks.ffxivextract.models;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.EnumSet;

public class PcbFile extends Game_File {
    //他のファイルを見つけるために使用されます
    private static SqPack_IndexFile currentIndex; //現在表示中または呼び出し元のIndexファイル

    public static class HeaderData {
        public int Unknown1;   //uint
        public int Unknown2;   //uint
        public int EntryCount;
        public int IndicesCount;
        public long Padding;   //ulong
    }

    public HeaderData Header;
    public ArrayList<PcbBlockEntry> Data;

    public PcbFile(SqPack_IndexFile index, byte[] data, ByteOrder endian){
        super(endian);
        currentIndex = index;
        loadPCB(data);
    }

    private void loadPCB(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        int entryOffset = 0x18;

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
            }
            else {
                ParsePcbBlockEntry(bb, entryOffset, entryOffset);
            }
        }
    }

    private void ParsePcbBlockEntry(ByteBuffer bb, int entryOffset, int globalOffset){
        int offset = 0;
        int PcbBlockEntry_HeaderData_Size = 40;
        boolean isGroup = true;
        while (isGroup) {
            PcbBlockEntry entry = new PcbBlockEntry(bb, entryOffset);

            if (isGroup = (entry.Header.Type == PcbBlockDataType.Group)) {
                ParsePcbBlockEntry(bb, entryOffset + offset + 0x30, globalOffset + offset + entryOffset);
                offset += entry.Header.BlockSize;
            }
            else {
                int dOffset = PcbBlockEntry_HeaderData_Size + offset;
                long blockSize = PcbBlockEntry_HeaderData_Size +
                        (long) entry.Header.VerticesCount * 3 * 4 +
                        entry.Header.VerticesI16Count * 3 * 2 +
                        entry.Header.IndicesCount * 3 * 2;

                if (entry.Header.VerticesCount != 0) {
                    entry.Data.Vertices = new Vector3[entry.Header.VerticesCount];
                    for (int i = 0; i < entry.Header.VerticesCount; ++i) {
                        entry.Data.Vertices[i] = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
                    }
                }
                if (entry.Header.VerticesI16Count != 0) {
                    entry.Data.VerticesI16 = new PcbBlockData.VertexI16[entry.Header.VerticesI16Count];
                    for (int i = 0; i < entry.Header.VerticesI16Count; ++i) {
                        entry.Data.VerticesI16[i]  = new PcbBlockData.VertexI16(bb.getShort(), bb.getShort(), bb.getShort());
                    }
                }
                if (entry.Header.IndicesCount != 0) {
                    entry.Data.Indices = new PcbBlockData.IndexData[entry.Header.IndicesCount];
                    for (int i = 0; i < entry.Header.IndicesCount; ++i) {
                        byte[] dataByte = new byte[12];
                        bb.get(dataByte);
                        entry.Data.Indices[i] = new PcbBlockData.IndexData(dataByte);
                    }
                }
                Data.add(entry);
            }
        }
    }

    public static class PcbBlockEntry{
        public static class HeaderData{
            public PcbBlockDataType Type;       // 0 for entry, 0x30 for group
            public int BlockSize;       //uint // when group size in bytes for the group block
            public Vector3 Min;          // bounding box
            public Vector3 Max;
            public short VerticesI16Count; //ushort // number of vertices packed into 16 bit
            public short IndicesCount;  //ushort // number of indices
            public int VerticesCount;   //uint // number of normal float vertices
        }

        public int Offset;
        public HeaderData Header;
        public PcbBlockData Data;

        public PcbBlockEntry(ByteBuffer bb, int offset){
            Offset = offset;

            this.Header = new HeaderData();

            Header.Type = PcbBlockDataType.valueOf(bb.getInt());
            Header.BlockSize = bb.getInt();
            Header.Min = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Max = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.VerticesI16Count = bb.getShort();
            Header.IndicesCount = bb.getShort();
            Header.VerticesCount = bb.getInt();

            this.Data = new PcbBlockData();

            Build(bb);
        }

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

}
