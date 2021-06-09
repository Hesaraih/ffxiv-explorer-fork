package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class EXDF_File extends Game_File {

    private byte[] data;
    private EXDF_Offset[] entryOffsets;

    public EXDF_File(byte[] data) throws IOException {
        super(ByteOrder.BIG_ENDIAN);
        loadEXDF(data);
    }

    @SuppressWarnings("unused")
    public EXDF_File(String path) throws IOException {
        super(ByteOrder.BIG_ENDIAN);
        File file = new File(path);
        byte[] data;
        try (FileInputStream fis = new FileInputStream(file)) {
            data = new byte[(int) file.length()];
            while (fis.read(data) != -1){
                Utils.getGlobalLogger().debug("EXDF読み取り");
            }
        }

        loadEXDF(data);
    }

    private void loadEXDF(byte[] data) {
        this.data = data;
        ByteBuffer buffer = ByteBuffer.wrap(data);

        try {
            int magic = buffer.getInt();
            int version = buffer.getShort();

            if (magic != 0x45584446 || version != 2) {
                Utils.getGlobalLogger().error("EXDF magic was incorrect.");
                Utils.getGlobalLogger().debug("Magic was {}", String.format("0x%08X", magic));
                return;
            }

            buffer.getShort();

            int offsetTableSize = buffer.getInt();
            //int dataSectionSize = buffer.getInt();

            buffer.position(0x20);

            entryOffsets = new EXDF_Offset[offsetTableSize / 8];
            //データのオフセットテーブルはIndex(int),Offset(int)の8byteで構成されているためデータ数は
            //テーブルサイズを8で割った物となる
            for (int i = 0; i < offsetTableSize / 8; i++) {
                entryOffsets[i] = new EXDF_Offset(buffer.getInt(), buffer.getInt());
            }

        } catch (BufferUnderflowException | BufferOverflowException flowException) {
            Utils.getGlobalLogger().error(flowException);
        }
    }

    public EXDF_Entry getEntry(int index, int subIndex, short variant) {
        if (entryOffsets.length > index) {
            return new EXDF_Entry(data, entryOffsets[index].index, subIndex, entryOffsets[index].offset, variant);
        }else{
            //Indexに欠番があるとentryOffsetsの数がindex総数より不足する
            Utils.getGlobalLogger().error("インデックス番号が不正です");

            return new EXDF_Entry(data, entryOffsets[0].index, subIndex, entryOffsets[0].offset, variant);
        }
    }

    @SuppressWarnings("unused")
    static class EXDF_DataBlock {
        private final byte[] data;

        public EXDF_DataBlock(byte[] dataBlock) {
            this.data = dataBlock;
        }

        public byte[] getData() {
            return data;
        }

    }

    public static class EXDF_Offset {
        final int index;
        final int offset;

        EXDF_Offset(int index, int offset) {
            this.index = index;
            this.offset = offset;
        }
    }

    @SuppressWarnings("unused")
    public byte[] getRawData() {
        return data;
    }

    public static class EXDF_Entry {

        final int index;
        public String indexID2 = "0.0";
        public int subIndexNum = 0;
        final int offset;
        private final byte[] dataChunk;

        EXDF_Entry(byte[] data, int index, int subIndex, int offset ,short variant) {
            this.index = index;
            this.offset = offset;
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.position(offset);
            int size = buffer.getInt(); //データ部のサイズ
            if (variant == 2) {
                //exhのVariantが2の時
                subIndexNum = buffer.getShort(); //subデータ数
                int tmpPos = buffer.position();
                if (subIndexNum > 0){
                    int subSize = size / subIndexNum;
                    if (subIndex >= subIndexNum){
                        subIndex = subIndexNum - 1; //バッファあふれ防止
                    }
                    buffer.position(tmpPos + subSize * subIndex);
                    size = subSize - 2;
                }
            }
            dataChunk = new byte[size];
            int subIndexID = buffer.getShort(); //IndexID または SubID
            if (variant == 2) {
                //SubIDがあるexdファイルのキーはメインIndex:整数部、SubIndex:小数部として表記する
                //SaintCoinachの表記に合わせた
                indexID2 = index + "." + subIndexID;
            }

            buffer.get(dataChunk);
        }

        @SuppressWarnings("unused")
        public byte[] getRawData() {
            return dataChunk;
        }

        public byte getByte(int offset) {
            ByteBuffer buffer = ByteBuffer.wrap(dataChunk);
            buffer.position(offset);
            return buffer.get();
        }

        public short getShort(int offset) {
            ByteBuffer buffer = ByteBuffer.wrap(dataChunk);
            buffer.position(offset);
            return buffer.getShort();
        }

        public int[] getQuad(short offset) {
            ByteBuffer buffer = ByteBuffer.wrap(dataChunk);
            buffer.position(offset);

            int[] quad = new int[4];
            quad[0] = buffer.getShort();
            quad[1] = buffer.getShort();
            quad[2] = buffer.getShort();
            quad[3] = buffer.getShort();

            return quad;
        }

        public int[] getDual(short offset) {
            ByteBuffer buffer = ByteBuffer.wrap(dataChunk);
            buffer.position(offset);

            int[] dual = new int[2];
            dual[0] = buffer.getShort();
            dual[1] = buffer.getShort();

            return dual;
        }

        public boolean getByteBool(int datatype, int offset) {
            ByteBuffer buffer = ByteBuffer.wrap(dataChunk);
            buffer.position(offset);
            int val = buffer.get();
            int shift = (datatype - 0x19);
            int i = 1 << shift;
            val &= i;
            return (val & i) == i;
        }

        public int getInt(int offset) {
            ByteBuffer buffer = ByteBuffer.wrap(dataChunk);
            buffer.position(offset);
            return buffer.getInt();
        }

        public float getFloat(short offset) {
            ByteBuffer buffer = ByteBuffer.wrap(dataChunk);
            buffer.position(offset);
            return buffer.getFloat();
        }

        public byte[] getString(int datasetChunkSize, short offset) {
            ByteBuffer buffer = ByteBuffer.wrap(dataChunk);
            buffer.position(offset);
            int stringOffset = buffer.getInt();
            buffer.position(datasetChunkSize + stringOffset);

            //Find the null terminator
            int nullTermPos;
            while (true) {
                byte in = buffer.get();
                if (in == 0x00) {
                    nullTermPos = buffer.position() - (datasetChunkSize + stringOffset);
                    break;
                }
            }

            //Read in
            byte[] stringBytes = new byte[nullTermPos - 1];
            buffer.position(datasetChunkSize + stringOffset);
            buffer.get(stringBytes);

            return stringBytes;
        }

        public boolean getBoolean(short offset) {
            byte b = getByte(offset);
            return b == 1;
        }

        public int getIndex() {
            return index;
        }

        @SuppressWarnings("unused")
        public boolean getByteBool(short offset2) {
            return false;
        }

    }

    @SuppressWarnings("unused")
    public EXDF_Offset[] getIndexOffsetTable() {
        return entryOffsets;
    }

    public int getNumEntries() {
        return entryOffsets.length;
    }
}
