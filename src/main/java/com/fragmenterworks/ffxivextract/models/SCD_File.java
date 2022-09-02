package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.Utils;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;

public class SCD_File extends Game_File {

    final int ScdEntryHeaderSize = 0x20;

    private byte[] scdFile;

    public static class ScdHeader {
        public short Table1_Count;
        public short Table2_Count;
        public short SoundEntryCount;
        public short Unknown1;
        public int Table1_DataEntryOffset;
        public int SoundEntryTableOffset;
        public int Table2_Offset;
        public int UnknownOffset;
        public int Table3_EntryTableOffset;
    }

    public static class ScdEntryHeader {
        public int DataSize;
        public int ChannelCount; //1: Mono, 2: Stereo
        public int Frequency;
        public ScdCodec Codec;  //0x0C: MS-ADPCM, 0x06: OGG
        public int LoopStartSample;
        public int LoopEndSample;
        public int SamplesOffset;
        public short AuxChunkCount;
        public short Unknown1;
    }

    public ScdHeader ScdHeader;
    public ScdEntryHeader[] EntryHeaders;
    public ScdEntry[] Entries;

    public SCD_File(byte[] data, ByteOrder endian){
        super(endian);
        loadSCD(data);
    }


    private void loadSCD(byte[] data){

        scdFile = data;

        ByteBuffer bb = ByteBuffer.allocateDirect(data.length);
        bb.order(endian);

        bb.put(data);
        bb.rewind();

        try {
            //Check Signature
            byte[] sigBuf = new byte[8];
            bb.get(sigBuf, 0, 8);
            if (!(new String(sigBuf)).equals("SEDBSSCF")) //SEDBSSCF in hex
            {
                Utils.getGlobalLogger().error("Not a valid SCD file");
                return;
            }

            //Check Version
            int version = bb.getInt();
            if (version != 3) {
                Utils.getGlobalLogger().error("Only version 3 SCD files supported");
                return;
            }

            bb.rewind();
            bb.position(0xE);
            int fileHeaderSize = bb.getShort();

            //Offset Header
            bb.rewind();
            ReadScdHeader(bb, fileHeaderSize);

            //Load Sound Entry Offsets
            EntryHeaders = new ScdEntryHeader[ScdHeader.SoundEntryCount];
            int[] entryChunkOffsets = new int[ScdHeader.SoundEntryCount];
            int[] entryDataOffsets = new int[ScdHeader.SoundEntryCount];
            bb.rewind();
            bb.position(ScdHeader.SoundEntryTableOffset);

            for (int i = 0; i < ScdHeader.SoundEntryCount; i++) {
                bb.position(ScdHeader.SoundEntryTableOffset + 4 * i);
                int headerOffset = bb.getInt();

                EntryHeaders[i] = ReadEntryHeader(bb, headerOffset);
                entryChunkOffsets[i] = headerOffset + ScdEntryHeaderSize;
                entryDataOffsets[i] = entryChunkOffsets[i];

                for (int j = 0; j < EntryHeaders[i].AuxChunkCount; ++j) {
                    bb.position(entryDataOffsets[i] + 4);
                    int tempOffset = bb.getInt(); //デバッグ用(intが変な値を取得することがある)
                    entryDataOffsets[i] += tempOffset;
                }
            }

            this.Entries = new ScdEntry[ScdHeader.SoundEntryCount];
            for (int i = 0; i < ScdHeader.SoundEntryCount; ++i) {
                this.Entries[i] = CreateEntry(EntryHeaders[i], entryChunkOffsets[i], entryDataOffsets[i]);
            }

            scdFile = null;
        } catch (BufferUnderflowException | BufferOverflowException flowException) {
            Utils.getGlobalLogger().error(flowException);
        }
    }

    private ScdEntryHeader ReadEntryHeader(ByteBuffer bb, int offset) {
        bb.position(offset);
        ScdEntryHeader h = new ScdEntryHeader();
        h.DataSize = bb.getInt();               //0x00 Music File Length
        h.ChannelCount = bb.getInt();           //0x04 Num Channels  0x01: Mono, 0x02: Stereo, FFXIV is usually 0x02 channels
        h.Frequency = bb.getInt();              //0x08 Frequency  FFXIV is usually 44100HZ (AC440000)
        h.Codec = ScdCodec.valueOf(bb.getInt());//0x0C Data Type  0x0C: MS-ADPCM, 0x06: OGG. FFXIV seems to use OGG for music, MS-ADPCM for sound.
        h.LoopStartSample = bb.getInt();        //0x10 Loop Start  In Bytes. Calculation: (filesize/amount of samples)*sample
        h.LoopEndSample = bb.getInt();          //0x14 Loop End  Ditto, if you wanted to loop a whole song, set this to the file's size.
        h.SamplesOffset = bb.getInt();          //0x18 First Frame Pos   First OggS after (possibly, not always) encrypted header.
        h.AuxChunkCount = bb.getShort();        //0x1C Aux Chunk Count   Number of Aux Chunks
        h.Unknown1 = bb.getShort();

        return h;
    }

    private ScdEntry CreateEntry(ScdEntryHeader header, int chunksOffset, int dataOffset) {
        if (header.DataSize == 0 || header.Codec == ScdCodec.None) {
            return null;
        }

        switch (header.Codec) {
            case OGG:
                return new ScdOggEntry(this, header, dataOffset);
            case MSADPCM:
                return new ScdAdpcmEntry(this, header, chunksOffset, dataOffset);
            default:
                return null;
        }
    }

    public static class ScdOggEntry extends ScdEntry{
        private byte[] _Decoded;
        private byte[] vorbisHeader;
        private int soundDataOffset;

        //region XorTable
        private final static int[] XorTable = {0x003A, 0x0032, 0x0032, 0x0032, 0x0003, 0x007E, 0x0012,
                0x00F7, 0x00B2, 0x00E2, 0x00A2, 0x0067, 0x0032, 0x0032, 0x0022, 0x0032, 0x0032, 0x0052,
                0x0016, 0x001B, 0x003C, 0x00A1, 0x0054, 0x007B, 0x001B, 0x0097, 0x00A6, 0x0093, 0x001A,
                0x004B, 0x00AA, 0x00A6, 0x007A, 0x007B, 0x001B, 0x0097, 0x00A6, 0x00F7, 0x0002, 0x00BB,
                0x00AA, 0x00A6, 0x00BB, 0x00F7, 0x002A, 0x0051, 0x00BE, 0x0003, 0x00F4, 0x002A, 0x0051,
                0x00BE, 0x0003, 0x00F4, 0x002A, 0x0051, 0x00BE, 0x0012, 0x0006, 0x0056, 0x0027, 0x0032,
                0x0032, 0x0036, 0x0032, 0x00B2, 0x001A, 0x003B, 0x00BC, 0x0091, 0x00D4, 0x007B, 0x0058,
                0x00FC, 0x000B, 0x0055, 0x002A, 0x0015, 0x00BC, 0x0040, 0x0092, 0x000B, 0x005B, 0x007C,
                0x000A, 0x0095, 0x0012, 0x0035, 0x00B8, 0x0063, 0x00D2, 0x000B, 0x003B, 0x00F0, 0x00C7,
                0x0014, 0x0051, 0x005C, 0x0094, 0x0086, 0x0094, 0x0059, 0x005C, 0x00FC, 0x001B, 0x0017,
                0x003A, 0x003F, 0x006B, 0x0037, 0x0032, 0x0032, 0x0030, 0x0032, 0x0072, 0x007A, 0x0013,
                0x00B7, 0x0026, 0x0060, 0x007A, 0x0013, 0x00B7, 0x0026, 0x0050, 0x00BA, 0x0013, 0x00B4,
                0x002A, 0x0050, 0x00BA, 0x0013, 0x00B5, 0x002E, 0x0040, 0x00FA, 0x0013, 0x0095, 0x00AE,
                0x0040, 0x0038, 0x0018, 0x009A, 0x0092, 0x00B0, 0x0038, 0x0000, 0x00FA, 0x0012, 0x00B1,
                0x007E, 0x0000, 0x00DB, 0x0096, 0x00A1, 0x007C, 0x0008, 0x00DB, 0x009A, 0x0091, 0x00BC,
                0x0008, 0x00D8, 0x001A, 0x0086, 0x00E2, 0x0070, 0x0039, 0x001F, 0x0086, 0x00E0, 0x0078,
                0x007E, 0x0003, 0x00E7, 0x0064, 0x0051, 0x009C, 0x008F, 0x0034, 0x006F, 0x004E, 0x0041,
                0x00FC, 0x000B, 0x00D5, 0x00AE, 0x0041, 0x00FC, 0x000B, 0x00D5, 0x00AE, 0x0041, 0x00FC,
                0x003B, 0x0070, 0x0071, 0x0064, 0x0033, 0x0032, 0x0012, 0x0032, 0x0032, 0x0036, 0x0070,
                0x0034, 0x002B, 0x0056, 0x0022, 0x0070, 0x003A, 0x0013, 0x00B7, 0x0026, 0x0060, 0x00BA,
                0x001B, 0x0094, 0x00AA, 0x0040, 0x0038, 0x0000, 0x00FA, 0x00B2, 0x00E2, 0x00A2, 0x0067,
                0x0032, 0x0032, 0x0012, 0x0032, 0x00B2, 0x0032, 0x0032, 0x0032, 0x0032, 0x0075, 0x00A3,
                0x0026, 0x007B, 0x0083, 0x0026, 0x00F9, 0x0083, 0x002E, 0x00FF, 0x00E3, 0x0016, 0x007D,
                0x00C0, 0x001E, 0x0063, 0x0021, 0x0007, 0x00E3, 0x0001};
        //endregion

        public ScdOggEntry(SCD_File file, ScdEntryHeader header, int dataOffset){
            super(file, header);
            Decode(dataOffset);
        }

        @Override
        public byte[] GetDecoded() {
            return _Decoded;
        }

        @Override
        public byte[] GetHeader(){
            return vorbisHeader;
        }

        @Override
        public byte[] GetData(){
            byte[] _Data = new byte[Header.DataSize];
            System.arraycopy(File.scdFile, soundDataOffset, _Data, 0, Header.DataSize);
            return _Data;
        }

        private void Decode(int dataOffset) {
            final int CryptTypeOffset = 0x00;
            final int XorValueOffset = 0x02;
            final int SeekTableSizeOffset = 0x10;
            final int VorbisHeaderSizeOffset = 0x14;

            ByteBuffer bb = ByteBuffer.wrap(File.scdFile);
            bb.order(File.endian);
            bb.position(dataOffset + CryptTypeOffset);
            ScdOggCryptType cryptType = ScdOggCryptType.valueOf(bb.getShort());

            if (cryptType != ScdOggCryptType.None && cryptType != ScdOggCryptType.FullXorUsingTable && cryptType != ScdOggCryptType.VorbisHeaderXor) {
                return;
            }

            bb.position(dataOffset + SeekTableSizeOffset);
            int seekTableSize = bb.getInt();

            bb.position(dataOffset + VorbisHeaderSizeOffset);
            int vorbisHeaderSize = bb.getInt();

            int vorbisHeaderOffset = dataOffset + 0x20 + seekTableSize;
            soundDataOffset = vorbisHeaderOffset + vorbisHeaderSize;

            vorbisHeader = new byte[vorbisHeaderSize];
            bb.position(vorbisHeaderOffset);
            bb.get(vorbisHeader); // = Array.Copy(File._InputBuffer, vorbisHeaderOffset, vorbisHeader, 0, vorbisHeaderSize)

            if (cryptType == ScdOggCryptType.VorbisHeaderXor) {
                bb.position(dataOffset + XorValueOffset);
                byte xorVal = bb.get();
                if (xorVal != 0) {
                    for (int i = 0; i < vorbisHeader.length; i++) {
                        vorbisHeader[i] ^= xorVal;
                    }
                }
            }

            _Decoded = new byte[vorbisHeader.length + Header.DataSize];
            //Array.Copy(vorbisHeader, 0, _Decoded, 0, vorbisHeader.Length);
            System.arraycopy(vorbisHeader,0, _Decoded, 0, vorbisHeader.length);
            //Array.Copy(File._InputBuffer, soundDataOffset, _Decoded, vorbisHeader.Length, Header.DataSize);
            System.arraycopy(File.scdFile, soundDataOffset, _Decoded, vorbisHeader.length, Header.DataSize);

            if (cryptType == ScdOggCryptType.FullXorUsingTable) {
                XorUsingTable();
            }
        }

        private void XorUsingTable(){
            byte staticXor = (byte)(Header.DataSize & 0x7F);
            byte tableOffset = (byte)(Header.DataSize & 0x3F);
            for (int i = 0; i < _Decoded.length; i++) {
                _Decoded[i] ^= XorTable[(tableOffset + i) & 0xFF];
                _Decoded[i] ^= staticXor;
            }
        }
    }

    public static class ScdAdpcmEntry extends ScdEntry{
        private byte[] _Decoded;
        private final int wavHeaderOffset;
        private final int ChunksOffset;

        public ScdAdpcmEntry(SCD_File file, ScdEntryHeader header, int chunksOffset, int dataOffset){
            super(file, header);
            ChunksOffset = chunksOffset;
            wavHeaderOffset = dataOffset;
            Decode();
        }

        @Override
        public byte[] GetDecoded() {
            return _Decoded;
        }

        @Override
        public byte[] GetHeader(){
            final int WaveHeaderSize = 0x10;
            byte[] _Header = new byte[WaveHeaderSize];
            System.arraycopy(File.scdFile, wavHeaderOffset, _Header, 0, WaveHeaderSize);
            return _Header;
        }

        @Override
        public byte[] GetData(){
            int finalDataOffset = ChunksOffset + Header.SamplesOffset;
            byte[] _Data = new byte[Header.DataSize];
            System.arraycopy(File.scdFile, finalDataOffset, _Data, 0, Header.DataSize);
            return _Data;
        }

        private void Decode() {
            //final int WaveHeaderSize = 0x10; //fmtサブチャンクのサイズが違う
            final int WaveHeaderSize = Header.SamplesOffset;

            int finalDataOffset = ChunksOffset + Header.SamplesOffset;

            _Decoded = new byte[0x1C + WaveHeaderSize + Header.DataSize];
            int o = 0;
            _Decoded[o++] = (byte)'R';
            _Decoded[o++] = (byte)'I';
            _Decoded[o++] = (byte)'F';
            _Decoded[o++] = (byte)'F';

            //Array.Copy(BitConverter.GetBytes((int)(0x14 + WaveHeaderSize + Header.DataSize)), 0, _Decoded, o, 4);
            //ビッグエンディアンで配列をコピーしていたため、リトルエンディアンに変更
            System.arraycopy(BitConverter.GetBytes(Integer.reverseBytes(0x0c + WaveHeaderSize + Header.DataSize)),0, _Decoded, o, 4);
            o += 4;

            _Decoded[o++] = (byte)'W';
            _Decoded[o++] = (byte)'A';
            _Decoded[o++] = (byte)'V';
            _Decoded[o++] = (byte)'E';
            _Decoded[o++] = (byte)'f';
            _Decoded[o++] = (byte)'m';
            _Decoded[o++] = (byte)'t';
            _Decoded[o++] = (byte)' ';

            //Array.Copy(BitConverter.GetBytes((int)WaveHeaderSize), 0, _Decoded, o, 4);
            //ビッグエンディアンで配列をコピーしていたため、リトルエンディアンに変更
            System.arraycopy(BitConverter.GetBytes(Integer.reverseBytes(WaveHeaderSize)), 0, _Decoded, o, 4);
            o += 4;

            //Array.Copy(File._InputBuffer, wavHeaderOffset, _Decoded, o, WaveHeaderSize);
            System.arraycopy(File.scdFile, wavHeaderOffset, _Decoded, o, WaveHeaderSize);
            o += WaveHeaderSize;

            _Decoded[o++] = (byte)'d';
            _Decoded[o++] = (byte)'a';
            _Decoded[o++] = (byte)'t';
            _Decoded[o++] = (byte)'a';

            //Array.Copy(BitConverter.GetBytes((int)Header.DataSize), 0, _Decoded, o, 4);
            //ビッグエンディアンで配列をコピーしていたため、リトルエンディアンに変更
            System.arraycopy(BitConverter.GetBytes(Integer.reverseBytes(Header.DataSize)), 0, _Decoded, o, 4);
            o += 4;
            //Array.Copy(File._InputBuffer, finalDataOffset, _Decoded, o, Header.DataSize);
            System.arraycopy(File.scdFile, finalDataOffset, _Decoded, o, Header.DataSize);

        }
    }

    private void ReadScdHeader(ByteBuffer bb, int offset){
        bb.position(offset);
        ScdHeader = new ScdHeader();
        ScdHeader.Table1_Count = bb.getShort();
        ScdHeader.Table2_Count = bb.getShort();
        ScdHeader.SoundEntryCount = bb.getShort();
        ScdHeader.Unknown1 = bb.getShort();
        ScdHeader.Table2_Offset = bb.getInt();
        ScdHeader.SoundEntryTableOffset = bb.getInt();
        ScdHeader.Table1_DataEntryOffset = bb.getInt();
        ScdHeader.UnknownOffset = bb.getInt(); //0x00000000しか見たことないので不明
        ScdHeader.Table3_EntryTableOffset = bb.getInt(); //このEntryTableに入っているオフセットの先に0x80Byteのデータがあり、その後Soundデータが続く
        //int UnknownOffset2 = bb.getInt(); //0x00000000しか見たことないので不明

        //Table1_HeaderEntryOffset[ScdHeader.Table1_Count]が直後に続くがここへのOffsetがなさそう(ここまでScdHeaderに含まれるのか？)
    }

    public byte[] getConverted(int index) {
        if (index > EntryHeaders.length - 1) {
            return null;
        }

        //Exception for placeholders
        if (EntryHeaders[index].DataSize == 0) {
            return null;
        }

        return this.Entries[index].GetDecoded();
    }

    public int getNumEntries() {
        return EntryHeaders.length;
    }

    public byte[] getADPCMData(int index) {
        if (index > EntryHeaders.length - 1) {
            return null;
        }

        //Exception for placeholders
        if (EntryHeaders[index].DataSize == 0) {
            return null;
        }

        return this.Entries[index].GetData();
    }

    public byte[] getADPCMHeader(int index) {
        if (index > EntryHeaders.length - 1) {
            return null;
        }

        //Exception for placeholders
        if (EntryHeaders[index].DataSize == 0) {
            return null;
        }

        return this.Entries[index].GetHeader();
    }

    public static class BitConverter {
        public static byte[] GetBytes(int value){
            int arraySize = Integer.SIZE / Byte.SIZE;
            ByteBuffer buffer = ByteBuffer.allocate(arraySize);
            return buffer.putInt(value).array();
        }
    }

    public abstract static class ScdEntry {

        //region Properties
        public SCD_File File;
        public ScdEntryHeader Header;
        //endregion

        //region Constructor
        protected ScdEntry(SCD_File file, ScdEntryHeader header) {
            this.File = file;
            this.Header = header;
        }
        //endregion

        public abstract byte[] GetDecoded();

        public abstract byte[] GetHeader();

        public abstract byte[] GetData();
    }

    public enum ScdCodec{
        None(0x00),
        OGG(0x06),
        PS3(0x07),
        MSADPCM(0x0C),
        NoData(0xffff);

        private final int id;

        ScdCodec(int i) {
            id = i;
        }

        public static ScdCodec valueOf(final int value){
            for(ScdCodec type : EnumSet.allOf(ScdCodec.class)){
                if(type.getId() == value) {
                    return type;
                }
            }
            Utils.getGlobalLogger().trace(String.format("未知のCodecType : %s", value));
            return NoData;
        }

        public int getId() {
            return id;
        }

    }

    public enum ScdOggCryptType{
        None(0x0000),
        VorbisHeaderXor(0x2002),
        FullXorUsingTable(0x2003),
        UnknownType(0xffff);

        private final int id;

        ScdOggCryptType(int i) {
            id = i;
        }

        public static ScdOggCryptType valueOf(final int value){
            for(ScdOggCryptType type : EnumSet.allOf(ScdOggCryptType.class)){
                if(type.getId() == value) {
                    return type;
                }
            }
            Utils.getGlobalLogger().trace(String.format("未知のOggCryptType : %s", value));
            return UnknownType;
        }

        public int getId() {
            return id;
        }

    }

}
