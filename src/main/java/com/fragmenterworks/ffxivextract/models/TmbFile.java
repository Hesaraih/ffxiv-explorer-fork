package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.helpers.ByteArrayExtensions;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TmbFile extends Game_File {
    //avfxファイルに似てる

    //他のファイルを見つけるために使用されます
    private final SqPack_IndexFile currentIndex; //現在表示中または呼び出し元のIndexファイル
    private static SqPack_IndexFile bgcommonIndex;
    private static SqPack_IndexFile soundIndex;
    private static SqPack_IndexFile vfxIndex;

    //region Struct
    public static class HeaderData {
        public String Magic1;  // TMLB
        public int FileSize;   //uint
        public int ChunkEntryCount;   //uint
    }
    //endregion

    public HeaderData Header;
    public ITmlbEntry[] Entry;

    /**
     * コンストラクタ
     * @param data sgbデータ
     * @param endian エンディアンの種類
     */
    public TmbFile(SqPack_IndexFile index, byte[] data, ByteOrder endian){
        super(endian);
        this.currentIndex = index;
        loadTMB(data);
    }

    private void loadTMB(byte[] data){
        Header = new HeaderData();

        byte[] signature = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);

        bb.get(signature);
        Header.Magic1 = new String(signature).trim(); //TMLB
        Header.FileSize = bb.getInt(); //ファイルサイズ
        Header.ChunkEntryCount = bb.getInt(); //データチャンク数


        if (!Header.Magic1.equals("TMLB")) {
            Utils.getGlobalLogger().error("TMLB magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic1);
        }


        Entry = new ITmlbEntry[Header.ChunkEntryCount];

        for (int i = 0; i < Header.ChunkEntryCount; i++){
            int chunkStartOffset = bb.position();
            bb.get(signature);
            String Magic2 = new String(signature).trim();  //TMDH等
            int EntrySize = bb.getInt(); //チャンクヘッダサイズ

            switch (Magic2) {
                case "TMDH":
                    Entry[i] = new TmdhEntry(bb, chunkStartOffset);
                    break;
                case "TMPP":
                    Entry[i] = new TmppEntry(bb, chunkStartOffset);
                    break;
                case "TMAL":
                    Entry[i] = new TmalEntry(bb, chunkStartOffset);
                    break;
                case "TMAC":
                    Entry[i] = new TmacEntry(bb, chunkStartOffset);
                    break;
                case "TMFC":
                    Entry[i] = new TmfcEntry(bb, chunkStartOffset);
                    break;
                case "TMTR":
                    Entry[i] = new TmtrEntry(bb, chunkStartOffset);
                    break;
                case "C002":
                    Entry[i] = new C002Entry(bb, chunkStartOffset);
                    break;
                case "C004":
                    Entry[i] = new C004Entry(bb, chunkStartOffset);
                    break;
                case "C006":
                    Entry[i] = new C006Entry(bb, chunkStartOffset);
                    break;
                case "C009":
                    Entry[i] = new C009Entry(bb, chunkStartOffset);
                    break;
                case "C010":
                    Entry[i] = new C010Entry(bb, chunkStartOffset);
                    break;
                case "C011":
                    Entry[i] = new C011Entry(bb, chunkStartOffset);
                    break;
                case "C012":
                    Entry[i] = new C012Entry(bb, chunkStartOffset);
                    break;
                case "C013":
                    Entry[i] = new C013Entry(bb, chunkStartOffset);
                    break;
                case "C014":
                    Entry[i] = new C014Entry(bb, chunkStartOffset);
                    break;
                case "C015":
                    Entry[i] = new C015Entry(bb, chunkStartOffset);
                    break;
                case "C016":
                    Entry[i] = new C016Entry(bb, chunkStartOffset);
                    break;
                case "C018":
                    Entry[i] = new C018Entry(bb, chunkStartOffset);
                    break;
                case "C021":
                    Entry[i] = new C021Entry(bb, chunkStartOffset);
                    break;
                case "C026":
                    Entry[i] = new C026Entry(bb, chunkStartOffset);
                    break;
                case "C031":
                    Entry[i] = new C031Entry(bb, chunkStartOffset);
                    break;
                case "C033":
                    Entry[i] = new C033Entry(bb, chunkStartOffset);
                    break;
                case "C034":
                    Entry[i] = new C034Entry(bb, chunkStartOffset);
                    break;
                case "C040":
                    Entry[i] = new C040Entry(bb, chunkStartOffset);
                    break;
                case "C042":
                    Entry[i] = new C042Entry(bb, chunkStartOffset);
                    break;
                case "C043":
                    Entry[i] = new C043Entry(bb, chunkStartOffset);
                    break;
                case "C051":
                    Entry[i] = new C051Entry(bb, chunkStartOffset);
                    break;
                case "C053":
                    Entry[i] = new C053Entry(bb, chunkStartOffset);
                    break;
                case "C063":
                    Entry[i] = new C063Entry(bb, chunkStartOffset);
                    break;
                case "C067":
                    Entry[i] = new C067Entry(bb, chunkStartOffset);
                    break;
                case "C068":
                    Entry[i] = new C068Entry(bb, chunkStartOffset);
                    break;
                case "C073":
                    Entry[i] = new C073Entry(bb, chunkStartOffset);
                    break;
                case "C075":
                    Entry[i] = new C075Entry(bb, chunkStartOffset);
                    break;
                case "C088":
                    Entry[i] = new C088Entry(bb, chunkStartOffset);
                    break;
                case "C093":
                    Entry[i] = new C093Entry(bb, chunkStartOffset);
                    break;
                case "C094":
                    Entry[i] = new C094Entry(bb, chunkStartOffset);
                    break;
                case "C096":
                    Entry[i] = new C096Entry(bb, chunkStartOffset);
                    break;
                case "C103":
                    Entry[i] = new C103Entry(bb, chunkStartOffset);
                    break;
                case "C107":
                    Entry[i] = new C107Entry(bb, chunkStartOffset);
                    break;
                case "C114":
                    Entry[i] = new C114Entry(bb, chunkStartOffset);
                    break;
                case "C117":
                    Entry[i] = new C117Entry(bb, chunkStartOffset);
                    break;
                case "C118":
                    Entry[i] = new C118Entry(bb, chunkStartOffset);
                    break;
                case "C120":
                    Entry[i] = new C120Entry(bb, chunkStartOffset);
                    break;
                case "C124":
                    Entry[i] = new C124Entry(bb, chunkStartOffset);
                    break;
                case "C125":
                    Entry[i] = new C125Entry(bb, chunkStartOffset);
                    break;
                case "C131":
                    Entry[i] = new C131Entry(bb, chunkStartOffset);
                    break;
                case "C139":
                    Entry[i] = new C139Entry(bb, chunkStartOffset);
                    break;
                case "C142":
                    Entry[i] = new C142Entry(bb, chunkStartOffset);
                    break;
                case "C143":
                    Entry[i] = new C143Entry(bb, chunkStartOffset);
                    break;
                case "C144":
                    Entry[i] = new C144Entry(bb, chunkStartOffset);
                    break;
                case "C168":
                    Entry[i] = new C168Entry(bb, chunkStartOffset);
                    break;
                case "C174":
                    Entry[i] = new C174Entry(bb, chunkStartOffset);
                    break;
                case "C175":
                    Entry[i] = new C175Entry(bb, chunkStartOffset);
                    break;
                case "C176":
                    Entry[i] = new C176Entry(bb, chunkStartOffset);
                    break;
                case "C177":
                    Entry[i] = new C177Entry(bb, chunkStartOffset);
                    break;
                case "C178":
                    Entry[i] = new C178Entry(bb, chunkStartOffset);
                    break;
                case "C187":
                    Entry[i] = new C187Entry(bb, chunkStartOffset);
                    break;
                case "C188":
                    Entry[i] = new C188Entry(bb, chunkStartOffset);
                    break;
                case "C192":
                    Entry[i] = new C192Entry(bb, chunkStartOffset);
                    break;
                case "C194":
                    Entry[i] = new C194Entry(bb, chunkStartOffset);
                    break;
                case "C197":
                    Entry[i] = new C197Entry(bb, chunkStartOffset);
                    break;
                case "C198":
                    Entry[i] = new C198Entry(bb, chunkStartOffset);
                    break;
                case "C202":
                    Entry[i] = new C202Entry(bb, chunkStartOffset);
                    break;
                default:
                    Utils.getGlobalLogger().info(String.format("%sのEntry解析は未実装", Magic2));
                    break;
            }

            bb.position(chunkStartOffset + EntrySize);
        }
        Utils.getGlobalLogger().trace("tmb読み込み完了");
    }

    public static class TmdhEntry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1; //ushort
            public int Unknown2; //ushort
        }

        public HeaderData Header = new HeaderData();

        public TmdhEntry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = Short.toUnsignedInt(bb.getShort());
            Header.Unknown2 = Short.toUnsignedInt(bb.getShort());

            Utils.getGlobalLogger().trace("TmdhEntry");
        }
    }

    public static class TmppEntry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public int NameOffset; //ushort
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public TmppEntry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            int BaseOffset = bb.position();
            Header.NameOffset = Short.toUnsignedInt(bb.getShort());

            Name = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.NameOffset);

            Utils.getGlobalLogger().trace("TmppEntry");
        }
    }

    public static class TmalEntry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public int ChildEntryID_Offset;
            public int ChildEntry_Count;
        }

        public HeaderData Header = new HeaderData();
        public short[] ChildEntryID;

        public TmalEntry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            int BaseOffset = bb.position();
            Header.ChildEntryID_Offset = bb.getInt();
            Header.ChildEntry_Count = bb.getInt();

            ChildEntryID = new short[Header.ChildEntry_Count];
            bb.position(BaseOffset + Header.ChildEntryID_Offset);
            for (int i = 0; i < Header.ChildEntry_Count; i++){
                ChildEntryID[i] = bb.getShort();
            }

            Utils.getGlobalLogger().trace("TmalEntry");
        }
    }

    public static class TmacEntry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public short EntrySubID;
            public short EntrySubType;
            public int ChildEntryID_Offset;
            public int ChildEntry_Count;
        }

        public HeaderData Header = new HeaderData();
        public short[] ChildEntryID;

        public TmacEntry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            int BaseOffset = bb.position();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.EntrySubID = bb.getShort();
            Header.EntrySubType = bb.getShort();
            Header.ChildEntryID_Offset = bb.getInt();
            Header.ChildEntry_Count = bb.getInt();

            ChildEntryID = new short[Header.ChildEntry_Count];
            bb.position(BaseOffset + Header.ChildEntryID_Offset);
            for (int i = 0; i < Header.ChildEntry_Count; i++){
                ChildEntryID[i] = bb.getShort();
            }

            Utils.getGlobalLogger().trace("TmacEntry");
        }
    }

    public static class TmtrEntry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int ChildEntryID_Offset;
            public int ChildEntry_Count;
            public int Unknown1;
        }

        public HeaderData Header = new HeaderData();
        public short[] ChildEntryID;

        public TmtrEntry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            int BaseOffset = bb.position();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.ChildEntryID_Offset = bb.getInt();
            Header.ChildEntry_Count = bb.getInt();
            Header.Unknown1 = bb.getInt();

            ChildEntryID = new short[Header.ChildEntry_Count];
            bb.position(BaseOffset + Header.ChildEntryID_Offset);
            for (int i = 0; i < Header.ChildEntry_Count; i++){
                ChildEntryID[i] = bb.getShort();
            }

            Utils.getGlobalLogger().trace("TmtrEntry");
        }
    }

    public static class TmfcEntry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int EntryOffset1;
            public int EntryCount1;
            public int EntryCount1a;
            public int EntryOffset2;
            public int EntryCount2;
        }

        public HeaderData Header = new HeaderData();
        public TmfcDataEntry[] DataEntry, DataEntry2;

        public TmfcEntry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            int BaseOffset = bb.position();
            Header.EntryOffset1 = bb.getInt();
            Header.EntryCount1 = bb.getInt();
            Header.EntryCount1a = bb.getInt();
            Header.EntryOffset2 = bb.getInt();
            Header.EntryCount2 = bb.getInt();

            DataEntry = new TmfcDataEntry[Header.EntryCount1];
            for (int i = 0; i < Header.EntryCount1; i++){
                DataEntry[i] = new TmfcDataEntry(bb, BaseOffset + Header.EntryOffset1 + i * 0x10);
            }

            DataEntry2 = new TmfcDataEntry[Header.EntryCount2];
            for (int i = 0; i < Header.EntryCount2; i++){
                DataEntry2[i] = new TmfcDataEntry(bb, BaseOffset + Header.EntryOffset2 + i * 0x10);
            }

            Utils.getGlobalLogger().trace("TmfcEntry");
        }

        public static class TmfcDataEntry{
            public static class HeaderData {
                public int Unknown1;
                public short DataID;
                public short DataType;
                public int EntryOffset;
                public int EntryCount;
            }

            public HeaderData Header = new HeaderData();
            public TmfcData[] TmfcData;

            public TmfcDataEntry(ByteBuffer bb, int offset) {
                bb.position(offset);
                Header.Unknown1 = bb.getInt();
                Header.DataID = bb.getShort();
                Header.DataType = bb.getShort();
                Header.EntryOffset = bb.getInt();
                Header.EntryCount = bb.getInt();

                TmfcData = new TmfcData[Header.EntryCount];
                bb.position(offset + Header.EntryOffset);
                for (int i = 0; i < Header.EntryCount; i++){
                    TmfcData[i] = new TmfcData(bb, offset + Header.EntryOffset + i * 0x18);
                }

            }
        }

        public static class TmfcData{
            public int Unknown1;
            public Vector2 UnknownVector1;
            public Vector3 UnknownVector2;

            public TmfcData(ByteBuffer bb, int offset) {
                bb.position(offset);
                Unknown1 = bb.getInt();
                UnknownVector1 = new Vector2(bb.getFloat(), bb.getFloat());
                UnknownVector2 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            }

        }
    }

    public static class C002Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int TmbFileNameOffset;
        }

        public HeaderData Header = new HeaderData();
        public String TmbFileName;

        public C002Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            int BaseOffset = bb.position();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.TmbFileNameOffset = bb.getInt();

            TmbFileName = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.TmbFileNameOffset);

            Utils.getGlobalLogger().trace("C002Entry");
        }
    }

    public static class C004Entry implements ITmlbEntry{
        //カメラワーク？
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int NameOffset;
            public float Unknown4;
            public float Unknown5;
            public float Unknown6;
            public int Unknown7;
            public int Unknown8;
            public int Unknown9;
            public int Unknown10;
            public int Unknown11;
            public int Unknown12;
            public int Unknown13;
            public int Unknown14;
            public int Unknown15;
            public int Unknown16;
            public int Unknown17;
            public int Unknown18;
            public int Unknown19;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public C004Entry(ByteBuffer bb, int offset) {
            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            int BaseOffset = bb.position();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.NameOffset = bb.getInt();
            Header.Unknown4 = bb.getFloat();
            Header.Unknown5 = bb.getFloat();
            Header.Unknown6 = bb.getFloat();
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

            Name = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.NameOffset);

            Utils.getGlobalLogger().trace("C004Entry");
        }
    }

    public static class C006Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
        }

        public HeaderData Header = new HeaderData();

        public C006Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();

            Utils.getGlobalLogger().trace("C006Entry");
        }
    }

    public static class C009Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int NameOffset;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public C009Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            int BaseOffset = bb.position();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.NameOffset = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.NameOffset);

            Utils.getGlobalLogger().trace("C009Entry");
        }
    }

    public static class C010Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public float UnknownFloat1;
            public float UnknownFloat2;
            public int MotionNameOffset;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();
        public String MotionName;

        public C010Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            int BaseOffset = bb.position();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();
            Header.MotionNameOffset = bb.getInt();
            Header.Unknown4 = bb.getInt();

            MotionName = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.MotionNameOffset);

            Utils.getGlobalLogger().trace("C010Entry");
        }
    }

    public static class C011Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
        }

        public HeaderData Header = new HeaderData();

        public C011Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();

            Utils.getGlobalLogger().trace("C011Entry");
        }
    }

    public class C012Entry implements ITmlbEntry{
        public class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int FileOffset;
            public short UnknownShort1;
            public short UnknownShort2;
            public short UnknownShort3;
            public short UnknownShort4;
        }

        public HeaderData Header = new HeaderData();
        public String FileName;
        public C012DataEntry[] DataEntry;

        public C012Entry(ByteBuffer bb, int offset) {
            //avfxファイル？
            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            int BaseOffset = bb.position();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.FileOffset = bb.getInt();
            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.UnknownShort3 = bb.getShort();
            Header.UnknownShort4 = bb.getShort();

            DataEntry = new C012DataEntry[5];
            int EntriesOffset = bb.position();
            for(int i = 0; i < 5; i++){
                bb.position(EntriesOffset + i * 8);
                int EntryOffset = bb.getInt();
                int EntryCount = bb.getInt();
                DataEntry[i] = new C012DataEntry(bb, BaseOffset + EntryOffset, EntryCount);
            }

            FileName = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.FileOffset);

            cAddPathToDB(FileName);

            Utils.getGlobalLogger().trace("C012Entry");
        }
        public class C012DataEntry{
            public float[] UnknownFloat;

            public C012DataEntry(ByteBuffer bb, int offset, int Count){
                bb.position(offset);
                UnknownFloat = new float[Count];
                for(int i = 0; i < Count; i++){
                    UnknownFloat[i] = bb.getFloat();
                }
            }

        }
    }

    public static class C013Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();

        public C013Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();

            Utils.getGlobalLogger().trace("C013Entry");
        }
    }

    public static class C014Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();

        public C014Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();

            Utils.getGlobalLogger().trace("C014Entry");
        }
    }

    public static class C015Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();

        public C015Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();

            Utils.getGlobalLogger().trace("C015Entry");
        }
    }

    public static class C016Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
        }

        public HeaderData Header = new HeaderData();

        public C016Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();

            Utils.getGlobalLogger().trace("C016Entry");
        }
    }

    public static class C018Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public Vector3 UnknownVector1;
            public Vector3 UnknownVector2;
            public Vector3 UnknownVector3;
        }

        public HeaderData Header = new HeaderData();

        public C018Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.UnknownVector1 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownVector2 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownVector3 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());

            Utils.getGlobalLogger().trace("C018Entry");
        }
    }

    public static class C021Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();

        public C021Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();

            Utils.getGlobalLogger().trace("C021Entry");
        }
    }

    public static class C026Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();

        public C026Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();

            Utils.getGlobalLogger().trace("C026Entry");
        }
    }

    public static class C031Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
        }

        public HeaderData Header = new HeaderData();

        public C031Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();

            Utils.getGlobalLogger().trace("C031Entry");
        }
    }

    public static class C033Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
        }

        public HeaderData Header = new HeaderData();

        public C033Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();

            Utils.getGlobalLogger().trace("C033Entry");
        }
    }

    public static class C034Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
        }

        public HeaderData Header = new HeaderData();

        public C034Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();

            Utils.getGlobalLogger().trace("C034Entry");
        }
    }

    public static class C040Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int MotionNameOffset;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
        }

        public HeaderData Header = new HeaderData();
        public String MotionName;

        public C040Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            int BaseOffset = bb.position();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.MotionNameOffset = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();

            MotionName = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.MotionNameOffset);

            Utils.getGlobalLogger().trace("C040Entry");
        }
    }

    public static class C042Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();

        public C042Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();

            Utils.getGlobalLogger().trace("C042Entry");
        }
    }

    public static class C043Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
        }

        public HeaderData Header = new HeaderData();

        public C043Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();

            Utils.getGlobalLogger().trace("C043Entry");
        }
    }

    public static class C051Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
            public int Unknown8;
            public int Unknown9;
            public int Unknown10;
            public int Unknown11;
            public float UnknownFloat1;
            public float UnknownFloat2;
            public float UnknownFloat3;
            public float UnknownFloat4;
            public float UnknownFloat5;
            public float UnknownFloat6;
            public float UnknownFloat7;
            public float UnknownFloat8;
            public float UnknownFloat9;
            public float UnknownFloat10;
            public float UnknownFloat11;
        }

        public HeaderData Header = new HeaderData();

        public C051Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
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
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();
            Header.UnknownFloat3 = bb.getFloat();
            Header.UnknownFloat4 = bb.getFloat();
            Header.UnknownFloat5 = bb.getFloat();
            Header.UnknownFloat6 = bb.getFloat();
            Header.UnknownFloat7 = bb.getFloat();
            Header.UnknownFloat8 = bb.getFloat();
            Header.UnknownFloat9 = bb.getFloat();
            Header.UnknownFloat10 = bb.getFloat();
            Header.UnknownFloat11 = bb.getFloat();

            Utils.getGlobalLogger().trace("C051Entry");
        }
    }

    public static class C053Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public short UnknownShort1;
            public short UnknownShort2;
            public int Unknown3;
        }

        public HeaderData Header = new HeaderData();

        public C053Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.Unknown3 = bb.getInt();

            Utils.getGlobalLogger().trace("C053Entry");
        }
    }

    public class C063Entry implements ITmlbEntry{
        //scdデータ格納用？
        public class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int FileOffset;
            public int Unknown3;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();
        public String FileName;

        public C063Entry(ByteBuffer bb, int offset) {
            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            int BaseOffset = bb.position();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.FileOffset = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();

            FileName = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.FileOffset);

            cAddPathToDB(FileName);

            Utils.getGlobalLogger().trace("C063Entry");
        }
    }

    public static class C067Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
        }

        public HeaderData Header = new HeaderData();

        public C067Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();

            Utils.getGlobalLogger().trace("C067Entry");
        }
    }

    public static class C068Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
        }

        public HeaderData Header = new HeaderData();

        public C068Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();

            Utils.getGlobalLogger().trace("C068Entry");
        }
    }

    public static class C073Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();

        public C073Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();

            Utils.getGlobalLogger().trace("C073Entry");
        }
    }

    public static class C075Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
        }

        public HeaderData Header = new HeaderData();
        public C075DataEntry[] DataEntry;

        public C075Entry(ByteBuffer bb, int offset) {
            //C012に似た構造
            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            int BaseOffset = bb.position();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();

            DataEntry = new C075DataEntry[5];
            int EntriesOffset = bb.position();
            for(int i = 0; i < 5; i++){
                bb.position(EntriesOffset + i * 8);
                int EntryOffset = bb.getInt();
                int EntryCount = bb.getInt();
                DataEntry[i] = new C075DataEntry(bb, BaseOffset + EntryOffset, EntryCount);
            }

            Utils.getGlobalLogger().trace("C075Entry");
        }
        public static class C075DataEntry {
            public float[] UnknownFloat;

            public C075DataEntry(ByteBuffer bb, int offset, int Count){
                bb.position(offset);
                UnknownFloat = new float[Count];
                for(int i = 0; i < Count; i++){
                    UnknownFloat[i] = bb.getFloat();
                }
            }

        }
    }

    public static class C088Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
        }

        public HeaderData Header = new HeaderData();

        public C088Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();

            Utils.getGlobalLogger().trace("C088Entry");
        }
    }

    public static class C093Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
        }

        public HeaderData Header = new HeaderData();

        public C093Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();

            Utils.getGlobalLogger().trace("C093Entry");
        }
    }

    public static class C094Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
        }

        public HeaderData Header = new HeaderData();

        public C094Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();

            Utils.getGlobalLogger().trace("C094Entry");
        }
    }

    public static class C096Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public float UnknownFloat1;
            public float UnknownFloat2;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();

        public C096Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();
            Header.Unknown4 = bb.getInt();

            Utils.getGlobalLogger().trace("C096Entry");
        }
    }

    public static class C103Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
            public int Unknown8;
            public int Unknown9;
        }

        public HeaderData Header = new HeaderData();

        public C103Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.Unknown9 = bb.getInt();

            Utils.getGlobalLogger().trace("C103Entry");
        }
    }

    public static class C107Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();

        public C107Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();

            Utils.getGlobalLogger().trace("C107Entry");
        }
    }

    public static class C114Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public float UnknownFloat1;
        }

        public HeaderData Header = new HeaderData();

        public C114Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.UnknownFloat1 = bb.getFloat();

            Utils.getGlobalLogger().trace("C114Entry");
        }
    }

    public static class C117Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
        }

        public HeaderData Header = new HeaderData();

        public C117Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();

            Utils.getGlobalLogger().trace("C117Entry");
        }
    }

    public static class C118Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
        }

        public HeaderData Header = new HeaderData();

        public C118Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();

            Utils.getGlobalLogger().trace("C118Entry");
        }
    }

    public static class C120Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
        }

        public HeaderData Header = new HeaderData();

        public C120Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();

            Utils.getGlobalLogger().trace("C120Entry");
        }
    }

    public static class C124Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
        }

        public HeaderData Header = new HeaderData();

        public C124Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();

            Utils.getGlobalLogger().trace("C124Entry");
        }
    }

    public static class C125Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
        }

        public HeaderData Header = new HeaderData();

        public C125Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();

            Utils.getGlobalLogger().trace("C125Entry");
        }
    }

    public static class C131Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
        }

        public HeaderData Header = new HeaderData();

        public C131Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();

            Utils.getGlobalLogger().trace("C131Entry");
        }
    }

    public static class C139Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
        }

        public HeaderData Header = new HeaderData();

        public C139Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();

            Utils.getGlobalLogger().trace("C139Entry");
        }
    }

    public static class C142Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();

        public C142Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();

            Utils.getGlobalLogger().trace("C142Entry");
        }
    }

    public static class C143Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
        }

        public HeaderData Header = new HeaderData();

        public C143Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();

            Utils.getGlobalLogger().trace("C143Entry");
        }
    }

    public static class C144Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public float UnknownFloat1;
            public float UnknownFloat2;
        }

        public HeaderData Header = new HeaderData();

        public C144Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();

            Utils.getGlobalLogger().trace("C144Entry");
        }
    }

    public static class C168Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
            public int Unknown8;
            public int Unknown9;
            public int Unknown10;
            public int Unknown11;
        }

        public HeaderData Header = new HeaderData();

        public C168Entry(ByteBuffer bb, int offset) {
            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
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


            Utils.getGlobalLogger().trace("C168Entry");
        }
    }

    public static class C174Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
        }

        public HeaderData Header = new HeaderData();

        public C174Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();

            Utils.getGlobalLogger().trace("C174Entry");
        }
    }

    public static class C175Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
        }

        public HeaderData Header = new HeaderData();

        public C175Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();

            Utils.getGlobalLogger().trace("C175Entry");
        }
    }

    public static class C176Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
        }

        public HeaderData Header = new HeaderData();

        public C176Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();

            Utils.getGlobalLogger().trace("C176Entry");
        }
    }

    public static class C177Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
        }

        public HeaderData Header = new HeaderData();

        public C177Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();

            Utils.getGlobalLogger().trace("C177Entry");
        }
    }

    public static class C178Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
        }

        public HeaderData Header = new HeaderData();

        public C178Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();

            Utils.getGlobalLogger().trace("C178Entry");
        }
    }

    public static class C187Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
        }

        public HeaderData Header = new HeaderData();

        public C187Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();

            Utils.getGlobalLogger().trace("C187Entry");
        }
    }

    public static class C188Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
        }

        public HeaderData Header = new HeaderData();

        public C188Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();

            Utils.getGlobalLogger().trace("C188Entry");
        }
    }

    public static class C192Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
            public int Unknown8;
            public int Unknown9;
            public int Unknown10;
            public int Unknown11;
        }

        public HeaderData Header = new HeaderData();

        public C192Entry(ByteBuffer bb, int offset) {
            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
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


            Utils.getGlobalLogger().trace("C192Entry");
        }
    }

    public static class C194Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();

        public C194Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();

            Utils.getGlobalLogger().trace("C194Entry");
        }
    }

    public static class C197Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
        }

        public HeaderData Header = new HeaderData();

        public C197Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();

            Utils.getGlobalLogger().trace("C197Entry");
        }
    }

    public static class C198Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
        }

        public HeaderData Header = new HeaderData();

        public C198Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();

            Utils.getGlobalLogger().trace("C198Entry");
        }
    }

    public static class C202Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
        }

        public HeaderData Header = new HeaderData();

        public C202Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();

            Utils.getGlobalLogger().trace("C202Entry");
        }
    }

    public interface ITmlbEntry{}

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
        } else if (archive.equals("070000")){
            if(soundIndex == null) {
                try {
                    soundIndex = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\070000.win32.index", false);
                    temp_IndexFile = soundIndex;
                } catch (IOException e) {
                    Utils.getGlobalLogger().error(e);
                }
            }else{
                temp_IndexFile = soundIndex;
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
