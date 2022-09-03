package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.ByteArrayExtensions;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TmbFile extends Game_File {

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
    public TmbFile(byte[] data, ByteOrder endian){
        super(endian);
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

        ByteBuffer bbCopy = bb.duplicate();
        bbCopy.get(signature);
        String Sig = new String(signature).trim();
        if (!Sig.equals("TMDH")){
            Utils.DummyLog("旧tmb形式");
            //旧タイプのtmbファイル(ブロックサイズが0x10の倍数になっている)を読み込む場合は
            //別の読み込みメソッドを作成する必要があるが、サンプル数が少ないため後回し
            return;
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
                case "C017":
                    Entry[i] = new C017Entry(bb, chunkStartOffset);
                    break;
                case "C018":
                    Entry[i] = new C018Entry(bb, chunkStartOffset);
                    break;
                case "C019":
                    Entry[i] = new C019Entry(bb, chunkStartOffset);
                    break;
                case "C020":
                    Entry[i] = new C020Entry(bb, chunkStartOffset);
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
                case "C035":
                    Entry[i] = new C035Entry(bb, chunkStartOffset);
                    break;
                case "C036":
                    Entry[i] = new C036Entry(bb, chunkStartOffset);
                    break;
                case "C037":
                    Entry[i] = new C037Entry(bb, chunkStartOffset);
                    break;
                case "C038":
                    Entry[i] = new C038Entry(bb, chunkStartOffset);
                    break;
                case "C039":
                    Entry[i] = new C039Entry(bb, chunkStartOffset);
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
                case "C044":
                    //MovieFile関係
                    Entry[i] = new C044Entry(bb, chunkStartOffset);
                    break;
                case "C045":
                    //会話テキスト関係
                    Entry[i] = new C045Entry(bb, chunkStartOffset);
                    break;
                case "C047":
                    Entry[i] = new C047Entry(bb, chunkStartOffset);
                    break;
                case "C048":
                    Entry[i] = new C048Entry(bb, chunkStartOffset);
                    break;
                case "C049":
                    Entry[i] = new C049Entry(bb, chunkStartOffset);
                    break;
                case "C050":
                    Entry[i] = new C050Entry(bb, chunkStartOffset);
                    break;
                case "C051":
                    Entry[i] = new C051Entry(bb, chunkStartOffset);
                    break;
                case "C053":
                    Entry[i] = new C053Entry(bb, chunkStartOffset);
                    break;
                case "C054":
                    Entry[i] = new C054Entry(bb, chunkStartOffset);
                    break;
                case "C055":
                    Entry[i] = new C055Entry(bb, chunkStartOffset);
                    break;
                case "C056":
                    Entry[i] = new C056Entry(bb, chunkStartOffset);
                    break;
                case "C057":
                    Entry[i] = new C057Entry(bb, chunkStartOffset);
                    break;
                case "C058":
                    Entry[i] = new C058Entry(bb, chunkStartOffset);
                    break;
                case "C059":
                    Entry[i] = new C059Entry(bb, chunkStartOffset);
                    break;
                case "C063":
                    Entry[i] = new C063Entry(bb, chunkStartOffset);
                    break;
                case "C064":
                    Entry[i] = new C064Entry(bb, chunkStartOffset);
                    break;
                case "C065":
                    Entry[i] = new C065Entry(bb, chunkStartOffset);
                    break;
                case "C066":
                    Entry[i] = new C066Entry(bb, chunkStartOffset);
                    break;
                case "C067":
                    Entry[i] = new C067Entry(bb, chunkStartOffset);
                    break;
                case "C068":
                    Entry[i] = new C068Entry(bb, chunkStartOffset);
                    break;
                case "C069":
                    Entry[i] = new C069Entry(bb, chunkStartOffset);
                    break;
                case "C070":
                    Entry[i] = new C070Entry(bb, chunkStartOffset);
                    break;
                case "C071":
                    Entry[i] = new C071Entry(bb, chunkStartOffset);
                    break;
                case "C072":
                    Entry[i] = new C072Entry(bb, chunkStartOffset);
                    break;
                case "C073":
                    Entry[i] = new C073Entry(bb, chunkStartOffset);
                    break;
                case "C075":
                    Entry[i] = new C075Entry(bb, chunkStartOffset);
                    break;
                case "C077":
                    Entry[i] = new C077Entry(bb, chunkStartOffset);
                    break;
                case "C081":
                    Entry[i] = new C081Entry(bb, chunkStartOffset);
                    break;
                case "C082":
                    Entry[i] = new C082Entry(bb, chunkStartOffset);
                    break;
                case "C083":
                    Entry[i] = new C083Entry(bb, chunkStartOffset);
                    break;
                case "C084":
                    Entry[i] = new C084Entry(bb, chunkStartOffset);
                    break;
                case "C085":
                    Entry[i] = new C085Entry(bb, chunkStartOffset);
                    break;
                case "C087":
                    Entry[i] = new C087Entry(bb, chunkStartOffset);
                    break;
                case "C088":
                    Entry[i] = new C088Entry(bb, chunkStartOffset);
                    break;
                case "C089":
                    Entry[i] = new C089Entry(bb, chunkStartOffset);
                    break;
                case "C090":
                    Entry[i] = new C090Entry(bb, chunkStartOffset);
                    break;
                case "C091":
                    Entry[i] = new C091Entry(bb, chunkStartOffset);
                    break;
                case "C092":
                    Entry[i] = new C092Entry(bb, chunkStartOffset);
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
                case "C097":
                    Entry[i] = new C097Entry(bb, chunkStartOffset);
                    break;
                case "C098":
                    Entry[i] = new C098Entry(bb, chunkStartOffset);
                    break;
                case "C099":
                    Entry[i] = new C099Entry(bb, chunkStartOffset);
                    break;
                case "C100":
                    Entry[i] = new C100Entry(bb, chunkStartOffset);
                    break;
                case "C101":
                    Entry[i] = new C101Entry(bb, chunkStartOffset);
                    break;
                case "C102":
                    Entry[i] = new C102Entry(bb, chunkStartOffset);
                    break;
                case "C103":
                    Entry[i] = new C103Entry(bb, chunkStartOffset);
                    break;
                case "C104":
                    Entry[i] = new C104Entry(bb, chunkStartOffset);
                    break;
                case "C107":
                    Entry[i] = new C107Entry(bb, chunkStartOffset);
                    break;
                case "C108":
                    Entry[i] = new C108Entry(bb, chunkStartOffset);
                    break;
                case "C109":
                    Entry[i] = new C109Entry(bb, chunkStartOffset);
                    break;
                case "C110":
                    Entry[i] = new C110Entry(bb, chunkStartOffset);
                    break;
                case "C111":
                    Entry[i] = new C111Entry(bb, chunkStartOffset);
                    break;
                case "C112":
                    Entry[i] = new C112Entry(bb, chunkStartOffset);
                    break;
                case "C113":
                    Entry[i] = new C113Entry(bb, chunkStartOffset);
                    break;
                case "C114":
                    Entry[i] = new C114Entry(bb, chunkStartOffset);
                    break;
                case "C115":
                    Entry[i] = new C115Entry(bb, chunkStartOffset);
                    break;
                case "C116":
                    Entry[i] = new C116Entry(bb, chunkStartOffset);
                    break;
                case "C117":
                    Entry[i] = new C117Entry(bb, chunkStartOffset);
                    break;
                case "C118":
                    Entry[i] = new C118Entry(bb, chunkStartOffset);
                    break;
                case "C119":
                    Entry[i] = new C119Entry(bb, chunkStartOffset);
                    break;
                case "C120":
                    Entry[i] = new C120Entry(bb, chunkStartOffset);
                    break;
                case "C121":
                    Entry[i] = new C121Entry(bb, chunkStartOffset);
                    break;
                case "C122":
                    Entry[i] = new C122Entry(bb, chunkStartOffset);
                    break;
                case "C124":
                    Entry[i] = new C124Entry(bb, chunkStartOffset);
                    break;
                case "C125":
                    Entry[i] = new C125Entry(bb, chunkStartOffset);
                    break;
                case "C126":
                    Entry[i] = new C126Entry(bb, chunkStartOffset);
                    break;
                case "C127":
                    Entry[i] = new C127Entry(bb, chunkStartOffset);
                    break;
                case "C128":
                    Entry[i] = new C128Entry(bb, chunkStartOffset);
                    break;
                case "C129":
                    Entry[i] = new C129Entry(bb, chunkStartOffset);
                    break;
                case "C130":
                    Entry[i] = new C130Entry(bb, chunkStartOffset);
                    break;
                case "C131":
                    Entry[i] = new C131Entry(bb, chunkStartOffset);
                    break;
                case "C132":
                    Entry[i] = new C132Entry(bb, chunkStartOffset);
                    break;
                case "C133":
                    Entry[i] = new C133Entry(bb, chunkStartOffset);
                    break;
                case "C135":
                    Entry[i] = new C135Entry(bb, chunkStartOffset);
                    break;
                case "C137":
                    Entry[i] = new C137Entry(bb, chunkStartOffset);
                    break;
                case "C138":
                    Entry[i] = new C138Entry(bb, chunkStartOffset);
                    break;
                case "C139":
                    Entry[i] = new C139Entry(bb, chunkStartOffset);
                    break;
                case "C140":
                    Entry[i] = new C140Entry(bb, chunkStartOffset);
                    break;
                case "C141":
                    Entry[i] = new C141Entry(bb, chunkStartOffset);
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
                case "C145":
                    Entry[i] = new C145Entry(bb, chunkStartOffset);
                    break;
                case "C146":
                    Entry[i] = new C146Entry(bb, chunkStartOffset);
                    break;
                case "C147":
                    Entry[i] = new C147Entry(bb, chunkStartOffset);
                    break;
                case "C148":
                    Entry[i] = new C148Entry(bb, chunkStartOffset);
                    break;
                case "C149":
                    Entry[i] = new C149Entry(bb, chunkStartOffset);
                    break;
                case "C151":
                    Entry[i] = new C151Entry(bb, chunkStartOffset);
                    break;
                case "C152":
                    Entry[i] = new C152Entry(bb, chunkStartOffset);
                    break;
                case "C153":
                    Entry[i] = new C153Entry(bb, chunkStartOffset);
                    break;
                case "C154":
                    Entry[i] = new C154Entry(bb, chunkStartOffset);
                    break;
                case "C155":
                    Entry[i] = new C155Entry(bb, chunkStartOffset);
                    break;
                case "C156":
                    Entry[i] = new C156Entry(bb, chunkStartOffset);
                    break;
                case "C157":
                    Entry[i] = new C157Entry(bb, chunkStartOffset);
                    break;
                case "C158":
                    Entry[i] = new C158Entry(bb, chunkStartOffset);
                    break;
                case "C159":
                    Entry[i] = new C159Entry(bb, chunkStartOffset);
                    break;
                case "C161":
                    Entry[i] = new C161Entry(bb, chunkStartOffset);
                    break;
                case "C162":
                    Entry[i] = new C162Entry(bb, chunkStartOffset);
                    break;
                case "C163":
                    Entry[i] = new C163Entry(bb, chunkStartOffset);
                    break;
                case "C164":
                    Entry[i] = new C164Entry(bb, chunkStartOffset);
                    break;
                case "C165":
                    Entry[i] = new C165Entry(bb, chunkStartOffset);
                    break;
                case "C168":
                    Entry[i] = new C168Entry(bb, chunkStartOffset);
                    break;
                case "C169":
                    Entry[i] = new C169Entry(bb, chunkStartOffset);
                    break;
                case "C171":
                    Entry[i] = new C171Entry(bb, chunkStartOffset);
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
                case "C179":
                    Entry[i] = new C179Entry(bb, chunkStartOffset);
                    break;
                case "C180":
                    Entry[i] = new C180Entry(bb, chunkStartOffset);
                    break;
                case "C181":
                    Entry[i] = new C181Entry(bb, chunkStartOffset);
                    break;
                case "C182":
                    Entry[i] = new C182Entry(bb, chunkStartOffset);
                    break;
                case "C183":
                    Entry[i] = new C183Entry(bb, chunkStartOffset);
                    break;
                case "C184":
                    Entry[i] = new C184Entry(bb, chunkStartOffset);
                    break;
                case "C185":
                    Entry[i] = new C185Entry(bb, chunkStartOffset);
                    break;
                case "C187":
                    Entry[i] = new C187Entry(bb, chunkStartOffset);
                    break;
                case "C188":
                    Entry[i] = new C188Entry(bb, chunkStartOffset);
                    break;
                case "C190":
                    Entry[i] = new C190Entry(bb, chunkStartOffset);
                    break;
                case "C192":
                    Entry[i] = new C192Entry(bb, chunkStartOffset);
                    break;
                case "C194":
                    Entry[i] = new C194Entry(bb, chunkStartOffset);
                    break;
                case "C196":
                    Entry[i] = new C196Entry(bb, chunkStartOffset);
                    break;
                case "C197":
                    Entry[i] = new C197Entry(bb, chunkStartOffset);
                    break;
                case "C198":
                    Entry[i] = new C198Entry(bb, chunkStartOffset);
                    break;
                case "C200":
                    Entry[i] = new C200Entry(bb, chunkStartOffset);
                    break;
                case "C201":
                    Entry[i] = new C201Entry(bb, chunkStartOffset);
                    break;
                case "C202":
                    Entry[i] = new C202Entry(bb, chunkStartOffset);
                    break;
                default:
                    Utils.getGlobalLogger().trace(String.format("%sのEntry解析は未実装", Magic2));
                    break;
            }

            bb.position(chunkStartOffset + EntrySize);
        }
        Utils.DummyLog("tmb読み込み完了");
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

            Utils.DummyLog("TmdhEntry");
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

            Utils.DummyLog("TmppEntry");
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

            Utils.DummyLog("TmalEntry");
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

            Utils.DummyLog("TmacEntry");
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

            Utils.DummyLog("TmtrEntry");
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

            Utils.DummyLog("TmfcEntry");
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

            Utils.DummyLog("C002Entry");
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
            public short Unknown6;
            public short Unknown6_2;
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
            Header.Unknown6 = bb.getShort();
            Header.Unknown6_2 = bb.getShort();
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

            Utils.DummyLog("C004Entry");
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

            Utils.DummyLog("C006Entry");
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

            Utils.DummyLog("C009Entry");
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

            Utils.DummyLog("C010Entry");
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

            Utils.DummyLog("C011Entry");
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

            Utils.DummyLog("C012Entry");
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

            Utils.DummyLog("C013Entry");
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

            Utils.DummyLog("C014Entry");
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

            Utils.DummyLog("C015Entry");
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

            Utils.DummyLog("C016Entry");
        }
    }

    public static class C017Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int EntryOffset1;
            public int EntryCount1;
            public int EntryOffset2;
            public int EntryCount2;
            public int Unknown7;
            public int Unknown8;
        }

        public HeaderData Header = new HeaderData();
        public C017DataEntry[] Entry1;
        public int[] Entry2;

        public C017Entry(ByteBuffer bb, int offset) {

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
            Header.EntryOffset1 = bb.getInt();
            Header.EntryCount1 = bb.getInt();
            Header.EntryOffset2 = bb.getInt();
            Header.EntryCount2 = bb.getInt();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();



            Entry1 = new C017DataEntry[Header.EntryCount1];
            bb.position(BaseOffset + Header.EntryOffset1);
            for (int i = 0; i < Header.EntryCount1; i++){
                Entry1[i] = new C017DataEntry(bb);
            }

            Entry2 = new int[Header.EntryCount2];
            bb.position(BaseOffset + Header.EntryOffset2);
            for (int i = 0; i < Header.EntryCount2; i++){
                Entry2[i] = bb.getInt();
            }

            Utils.DummyLog("C017Entry");
        }

        public static class C017DataEntry{
            public short UnknownShort1;
            public short UnknownShort2;
            public C017DataEntry(ByteBuffer bb){
                UnknownShort1 = bb.getShort();
                UnknownShort2 = bb.getShort();
            }
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

            Utils.DummyLog("C018Entry");
        }
    }

    public static class C019Entry implements ITmlbEntry{
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

        public C019Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C019Entry");
        }
    }

    public static class C020Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int FilePathOffset;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
            public int Unknown8;
            public int Unknown9;
            public float UnknownFloat1;
            public byte UnknownFlag1;
            public byte UnknownFlag2;
            public byte UnknownFlag3;
            public byte UnknownFlag4;
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
            public int Unknown20;
            public int Unknown21;
            public int Unknown22;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public C020Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            int BaseOffset = bb.position();
            Header.FilePathOffset = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.Unknown9 = bb.getInt();
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFlag1 = bb.get();
            Header.UnknownFlag2 = bb.get();
            Header.UnknownFlag3 = bb.get();
            Header.UnknownFlag4 = bb.get();
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
            Header.Unknown20 = bb.getInt();
            Header.Unknown21 = bb.getInt();
            Header.Unknown22 = bb.getInt();

            //例：(bg/)「ffxiv/wil_w1/evt/w1e6/level/w1e6」 (.lgb等･･･)
            Name = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.FilePathOffset);

            Utils.DummyLog("C020Entry");
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

            Utils.DummyLog("C021Entry");
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

            Utils.DummyLog("C026Entry");
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

            Utils.DummyLog("C031Entry");
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

            Utils.DummyLog("C033Entry");
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

            Utils.DummyLog("C034Entry");
        }
    }

    public static class C035Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public Vector3 UnknownVector;
        }

        public HeaderData Header = new HeaderData();

        public C035Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.UnknownVector = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());

            Utils.DummyLog("C035Entry");
        }
    }

    public static class C036Entry implements ITmlbEntry{
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

        public C036Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C036Entry");
        }
    }

    public static class C037Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public short UnknownShort1;
            public short UnknownShort2;
            public short UnknownShort3;
            public short UnknownShort4;
            public Vector3 UnknownVector1;
            public Vector3 UnknownVector2;
            public Vector3 UnknownVector3;
            public Vector3 UnknownVector4;
            public Vector3 UnknownVector5;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
            public int Unknown8;
            public float UnknownFloat1;
            public float UnknownFloat2;
        }

        public HeaderData Header = new HeaderData();

        public C037Entry(ByteBuffer bb, int offset) {
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
            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.UnknownShort3 = bb.getShort();
            Header.UnknownShort4 = bb.getShort();
            Header.UnknownVector1 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownVector2 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownVector3 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownVector4 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownVector5 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();

            Utils.DummyLog("C037Entry");
        }
    }

    public static class C038Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public float UnknownFloat1;
        }

        public HeaderData Header = new HeaderData();

        public C038Entry(ByteBuffer bb, int offset) {
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

            Utils.DummyLog("C038Entry");
        }
    }

    public static class C039Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public short UnknownShort1;
            public short UnknownShort2;
            public short UnknownShort3;
            public short UnknownShort4;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
        }

        public HeaderData Header = new HeaderData();

        public C039Entry(ByteBuffer bb, int offset) {

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
            Header.UnknownShort3 = bb.getShort();
            Header.UnknownShort4 = bb.getShort();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();

            Utils.DummyLog("C039Entry");
        }
    }

    public static class C040Entry implements ITmlbEntry{
        //Motion関係
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

            Utils.DummyLog("C040Entry");
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

            Utils.DummyLog("C042Entry");
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

            Utils.DummyLog("C043Entry");
        }
    }

    public static class C044Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int MovieFilePathOffset;
            public int Unknown4;
            public int Unknown5;
        }

        public HeaderData Header = new HeaderData();
        public String MovieFilePath;

        public C044Entry(ByteBuffer bb, int offset) {

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
            Header.MovieFilePathOffset = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();

            //例：ffxiv/00003
            //多分C:\Program Files (x86)\SquareEnix\FINAL FANTASY XIV - A Realm Reborn\game\movie\ffxiv\00003.datかな
            MovieFilePath = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.MovieFilePathOffset);

            Utils.DummyLog("C044Entry");
        }
    }

    public static class C045Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int TextSignatureOffset;
        }

        public HeaderData Header = new HeaderData();
        public String TextSignature;

        public C045Entry(ByteBuffer bb, int offset) {

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
            Header.TextSignatureOffset = bb.getInt();

            //例：TEXT_MANFST009_00449_700750_GAIUS
            TextSignature = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.TextSignatureOffset);

            Utils.DummyLog("C045Entry");
        }
    }

    public static class C047Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
        }

        public HeaderData Header = new HeaderData();

        public C047Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();

            Utils.DummyLog("C047Entry");
        }
    }

    public static class C048Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public byte UnknownFlag1;
            public byte UnknownFlag2;
            public byte UnknownFlag3;
            public byte UnknownFlag4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
            public int Unknown8;
            public int Unknown9;
            public int Unknown10;
            public int Unknown11;
            public int Unknown12;
        }

        public HeaderData Header = new HeaderData();

        public C048Entry(ByteBuffer bb, int offset) {

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
            Header.UnknownFlag1 = bb.get();
            Header.UnknownFlag2 = bb.get();
            Header.UnknownFlag3 = bb.get();
            Header.UnknownFlag4 = bb.get();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.Unknown9 = bb.getInt();
            Header.Unknown10 = bb.getInt();
            Header.Unknown11 = bb.getInt();
            Header.Unknown12 = bb.getInt();

            Utils.DummyLog("C048Entry");
        }
    }

    public static class C049Entry implements ITmlbEntry{
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
        }

        public HeaderData Header = new HeaderData();

        public C049Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C049Entry");
        }
    }

    public static class C050Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int EntryOffset;
            public int EntryCount;
        }

        public HeaderData Header = new HeaderData();
        public C050DataEntry[] Entry;

        public C050Entry(ByteBuffer bb, int offset) {

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
            Header.EntryOffset = bb.getInt();
            Header.EntryCount = bb.getInt();

            if (Header.EntryCount > 1){
                Utils.DummyLog("Debug用：データ構造が違うかも");
            }
            Entry = new C050DataEntry[Header.EntryCount];
            bb.position(BaseOffset + Header.EntryOffset);
            for (int i = 0; i < Header.EntryCount; i++){
                Entry[i] = new C050DataEntry(bb);
            }

            Utils.DummyLog("C050Entry");
        }

        public static class C050DataEntry{
            public int Unknown1;
            public short Unknown2;
            public short Unknown3;

            public C050DataEntry(ByteBuffer bb){
                Unknown1 = bb.getInt();
                Unknown2 = bb.getShort();
                Unknown3 = bb.getShort();
            }
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
            public short Unknown6;
            public short Unknown6_2;
            public int Unknown7;
            public int Unknown8;
            public int Unknown9;
            public int Unknown10;
            public int Unknown11;
            public float UnknownFloat1;
            public float UnknownFloat2;
            public float UnknownFloat3;
            public float PI;
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
            Header.Unknown6 = bb.getShort();
            Header.Unknown6_2 = bb.getShort();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.Unknown9 = bb.getInt();
            Header.Unknown10 = bb.getInt();
            Header.Unknown11 = bb.getInt();
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();
            Header.UnknownFloat3 = bb.getFloat();
            Header.PI = bb.getFloat();
            Header.UnknownFloat5 = bb.getFloat();
            Header.UnknownFloat6 = bb.getFloat();
            Header.UnknownFloat7 = bb.getFloat();
            Header.UnknownFloat8 = bb.getFloat();
            Header.UnknownFloat9 = bb.getFloat();
            Header.UnknownFloat10 = bb.getFloat();
            Header.UnknownFloat11 = bb.getFloat();

            Utils.DummyLog("C051Entry");
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

            Utils.DummyLog("C053Entry");
        }
    }

    public static class C054Entry implements ITmlbEntry{
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

        public C054Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C054Entry");
        }
    }

    public static class C055Entry implements ITmlbEntry{
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

        public C055Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C055Entry");
        }
    }

    public static class C056Entry implements ITmlbEntry{
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

        public C056Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C056Entry");
        }
    }

    public static class C057Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public float Unknown3;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();

        public C057Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getFloat();
            Header.Unknown4 = bb.getInt();

            Utils.DummyLog("C057Entry");
        }
    }

    public static class C058Entry implements ITmlbEntry{
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

        public C058Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C058Entry");
        }
    }

    public static class C059Entry implements ITmlbEntry{
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

        public C059Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C059Entry");
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

            Utils.DummyLog("C063Entry");
        }
    }

    public static class C064Entry implements ITmlbEntry{
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

        public C064Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C064Entry");
        }
    }

    public static class C065Entry implements ITmlbEntry{
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

        public C065Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C065Entry");
        }
    }

    public static class C066Entry implements ITmlbEntry{
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

        public C066Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C066Entry");
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

            Utils.DummyLog("C067Entry");
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

            Utils.DummyLog("C068Entry");
        }
    }

    public static class C069Entry implements ITmlbEntry{
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

        public C069Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C069Entry");
        }
    }

    public static class C070Entry implements ITmlbEntry{
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

        public C070Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C070Entry");
        }
    }

    public static class C071Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public short Unknown4;
            public short Unknown5;
        }

        public HeaderData Header = new HeaderData();

        public C071Entry(ByteBuffer bb, int offset) {

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
            Header.Unknown4 = bb.getShort();
            Header.Unknown5 = bb.getShort();

            Utils.DummyLog("C071Entry");
        }
    }

    public static class C072Entry implements ITmlbEntry{
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

        public C072Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C072Entry");
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
            public short Unknown4;
            public short Unknown5;
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
            Header.Unknown4 = bb.getShort();
            Header.Unknown5 = bb.getShort();

            Utils.DummyLog("C073Entry");
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

            Utils.DummyLog("C075Entry");
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

    public static class C077Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int EntryOffset;
            public int EntryCount;
        }

        public HeaderData Header = new HeaderData();

        public C077Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.EntryOffset = bb.getInt();
            Header.EntryCount = bb.getInt();

            Utils.DummyLog("C077Entry");
        }
    }

    public static class C081Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public float Unknown4;
        }

        public HeaderData Header = new HeaderData();

        public C081Entry(ByteBuffer bb, int offset) {

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
            Header.Unknown4 = bb.getFloat();

            Utils.DummyLog("C081Entry");
        }
    }

    public static class C082Entry implements ITmlbEntry{
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

        public C082Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C082Entry");
        }
    }

    public static class C083Entry implements ITmlbEntry{
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

        public C083Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C083Entry");
        }
    }

    public static class C084Entry implements ITmlbEntry{
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

        public C084Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C084Entry");
        }
    }

    public static class C085Entry implements ITmlbEntry{
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

        public C085Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C085Entry");
        }
    }

    public static class C087Entry implements ITmlbEntry{
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

        public C087Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C087Entry");
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

            Utils.DummyLog("C088Entry");
        }
    }

    public static class C089Entry implements ITmlbEntry{
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

        public C089Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C089Entry");
        }
    }

    public static class C090Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int NameOffset;
            public int Unknown4;
            public int Unknown5;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public C090Entry(ByteBuffer bb, int offset) {

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
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();

            //MotionName
            Name = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.NameOffset);

            Utils.DummyLog("C090Entry");
        }
    }

    public static class C091Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public Vector3 UnknownVector1;
        }

        public HeaderData Header = new HeaderData();

        public C091Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C091Entry");
        }
    }

    public static class C092Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public float UnknownFloat1;
            public float UnknownFloat2;
            public float UnknownFloat3;
            public float UnknownFloat4;
            public float UnknownFloat5;
        }

        public HeaderData Header = new HeaderData();

        public C092Entry(ByteBuffer bb, int offset) {

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

            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();
            Header.UnknownFloat3 = bb.getFloat();
            Header.UnknownFloat4 = bb.getFloat();
            Header.UnknownFloat5 = bb.getFloat();

            Utils.DummyLog("C092Entry");
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

            Utils.DummyLog("C093Entry");
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

            Utils.DummyLog("C094Entry");
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

            Utils.DummyLog("C096Entry");
        }
    }

    public static class C097Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public byte UnknownFlag1;
            public byte UnknownFlag2;
            public byte UnknownFlag3;
            public byte UnknownFlag4;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
            public float UnknownFloat1;
            public float UnknownFloat2;
            public int Unknown6;
            public int Unknown7;
        }

        public HeaderData Header = new HeaderData();

        public C097Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.UnknownFlag1 = bb.get();
            Header.UnknownFlag2 = bb.get();
            Header.UnknownFlag3 = bb.get();
            Header.UnknownFlag4 = bb.get();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();

            Utils.DummyLog("C097Entry");
        }
    }

    public static class C098Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int NameOffset;
            public int EntryOffset;
            public int EntryCount;
            public int Unknown6;
        }

        public HeaderData Header = new HeaderData();
        public String TextDataID_Name;
        public int[] Entry;

        public C098Entry(ByteBuffer bb, int offset) {

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
            Header.EntryOffset = bb.getInt();
            Header.EntryCount = bb.getInt();
            Header.Unknown6 = bb.getInt();

            TextDataID_Name = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.NameOffset);

            Entry = new int[Header.EntryCount];
            bb.position(BaseOffset + Header.EntryOffset);
            for (int i = 0; i < Header.EntryCount; i++){
                Entry[i] = bb.getInt();
            }

            Utils.DummyLog("C098Entry");
        }
    }

    public static class C099Entry implements ITmlbEntry{
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

        public C099Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C099Entry");
        }
    }

    public static class C100Entry implements ITmlbEntry{
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

        public C100Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C100Entry");
        }
    }

    public static class C101Entry implements ITmlbEntry{
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

        public C101Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C101Entry");
        }
    }

    public static class C102Entry implements ITmlbEntry{
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

        public C102Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C102Entry");
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

            Utils.DummyLog("C103Entry");
        }
    }

    public static class C104Entry implements ITmlbEntry{
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

        public C104Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C104Entry");
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

            Utils.DummyLog("C107Entry");
        }
    }

    public static class C108Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
        }

        public HeaderData Header = new HeaderData();

        public C108Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();

            Utils.DummyLog("C108Entry");
        }
    }

    public static class C109Entry implements ITmlbEntry{
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

        public C109Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C109Entry");
        }
    }

    public static class C110Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
        }

        public HeaderData Header = new HeaderData();

        public C110Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();

            Utils.DummyLog("C110Entry");
        }
    }

    public static class C111Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public short Unknown3;
            public short Unknown4;
            public int Unknown5;
        }

        public HeaderData Header = new HeaderData();

        public C111Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getShort();
            Header.Unknown4 = bb.getShort();
            Header.Unknown5 = bb.getInt();

            Utils.DummyLog("C111Entry");
        }
    }

    public static class C112Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public float Unknown3;
            public float Unknown4;
            public float Unknown5;
            public float Unknown6;
        }

        public HeaderData Header = new HeaderData();

        public C112Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getFloat();
            Header.Unknown4 = bb.getFloat();
            Header.Unknown5 = bb.getFloat();
            Header.Unknown6 = bb.getFloat();

            Utils.DummyLog("C112Entry");
        }
    }

    public static class C113Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public float UnknownFloat1;
            public float UnknownFloat2;
            public float UnknownFloat3;
            public float UnknownFloat4;
        }

        public HeaderData Header = new HeaderData();
        public Vector3[] Entry;

        public C113Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.UnknownFloat1 = bb.getFloat();
            Header.UnknownFloat2 = bb.getFloat();
            Header.UnknownFloat3 = bb.getFloat();
            Header.UnknownFloat4 = bb.getFloat();

            Utils.DummyLog("C113Entry");
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
            public int EntryOffset;
            public int EntryCount;
            public int Unknown5;
            public int Unknown6;
            public float UnknownFloat1;
        }

        public HeaderData Header = new HeaderData();
        public Vector3[] Entry;

        public C114Entry(ByteBuffer bb, int offset) {

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
            Header.EntryOffset = bb.getInt();
            Header.EntryCount = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.UnknownFloat1 = bb.getFloat();

            Entry = new Vector3[Header.EntryCount];
            bb.position(BaseOffset + Header.EntryOffset);
            for (int i = 0; i < Header.EntryCount; i++){
                Entry[i] = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            }

            Utils.DummyLog("C114Entry");
        }
    }

    public static class C115Entry implements ITmlbEntry{
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

        public C115Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C115Entry");
        }
    }

    public static class C116Entry implements ITmlbEntry{
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

        public C116Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C116Entry");
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

            Utils.DummyLog("C117Entry");
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

            Utils.DummyLog("C118Entry");
        }
    }

    public static class C119Entry implements ITmlbEntry{
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

        public C119Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C119Entry");
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

            Utils.DummyLog("C120Entry");
        }
    }

    public static class C121Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public byte UnknownFlag1;
            public byte UnknownFlag2;
            public byte UnknownFlag3;
            public byte UnknownFlag4;
            public int Unknown3;
        }

        public HeaderData Header = new HeaderData();

        public C121Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.UnknownFlag1 = bb.get();
            Header.UnknownFlag2 = bb.get();
            Header.UnknownFlag3 = bb.get();
            Header.UnknownFlag4 = bb.get();
            Header.Unknown3 = bb.getInt();

            Utils.DummyLog("C121Entry");
        }
    }

    public static class C122Entry implements ITmlbEntry{
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

        public C122Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C122Entry");
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

            Utils.DummyLog("C124Entry");
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

            Utils.DummyLog("C125Entry");
        }
    }

    public static class C126Entry implements ITmlbEntry{
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

        public C126Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C126Entry");
        }
    }

    public static class C127Entry implements ITmlbEntry{
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

        public C127Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C127Entry");
        }
    }

    public static class C128Entry implements ITmlbEntry{
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

        public C128Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C128Entry");
        }
    }

    public static class C129Entry implements ITmlbEntry{
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

        public C129Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C129Entry");
        }
    }

    public static class C130Entry implements ITmlbEntry{
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

        public C130Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C130Entry");
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

            Utils.DummyLog("C131Entry");
        }
    }

    public static class C132Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public Vector2 UnknownVector1;
        }

        public HeaderData Header = new HeaderData();

        public C132Entry(ByteBuffer bb, int offset) {

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
            Header.UnknownVector1 = new Vector2(bb.getFloat(), bb.getFloat());

            Utils.DummyLog("C132Entry");
        }
    }

    public static class C133Entry implements ITmlbEntry{
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

        public C133Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C132Entry");
        }
    }

    public static class C135Entry implements ITmlbEntry{
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

        public C135Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C135Entry");
        }
    }

    public static class C137Entry implements ITmlbEntry{
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

        public C137Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C137Entry");
        }
    }

    public static class C138Entry implements ITmlbEntry{
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
            public int Unknown12;
            public int Unknown13;
        }

        public HeaderData Header = new HeaderData();

        public C138Entry(ByteBuffer bb, int offset) {

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
            Header.Unknown12 = bb.getInt();
            Header.Unknown13 = bb.getInt();

            Utils.DummyLog("C138Entry");
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

            Utils.DummyLog("C139Entry");
        }
    }

    public static class C140Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public float Unknown3;
            public int Unknown4;
        }

        public HeaderData Header = new HeaderData();

        public C140Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.Unknown3 = bb.getFloat();
            Header.Unknown4 = bb.getInt();

            Utils.DummyLog("C140Entry");
        }
    }

    public static class C141Entry implements ITmlbEntry{
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

        public C141Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C141Entry");
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

            Utils.DummyLog("C142Entry");
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

            Utils.DummyLog("C143Entry");
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

            Utils.DummyLog("C144Entry");
        }
    }

    public static class C145Entry implements ITmlbEntry{
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

        public C145Entry(ByteBuffer bb, int offset) {
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

            Utils.DummyLog("C145Entry");
        }
    }

    public static class C146Entry implements ITmlbEntry{
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

        public C146Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C146Entry");
        }
    }

    public static class C147Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public byte UnknownFlag1;
            public byte UnknownFlag2;
            public byte UnknownFlag3;
            public byte UnknownFlag4;
            public short UnknownShort1;
            public short UnknownShort2;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
        }

        public HeaderData Header = new HeaderData();

        public C147Entry(ByteBuffer bb, int offset) {

            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.UnknownFlag1 = bb.get();
            Header.UnknownFlag2 = bb.get();
            Header.UnknownFlag3 = bb.get();
            Header.UnknownFlag4 = bb.get();
            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();

            Utils.DummyLog("C147Entry");
        }
    }

    public static class C148Entry implements ITmlbEntry{
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

        public C148Entry(ByteBuffer bb, int offset) {
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

            Utils.DummyLog("C148Entry");
        }
    }

    public static class C149Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public short UnknownShort1;
            public short UnknownShort2;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
        }

        public HeaderData Header = new HeaderData();

        public C149Entry(ByteBuffer bb, int offset) {

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
            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();

            Utils.DummyLog("C149Entry");
        }
    }

    public static class C151Entry implements ITmlbEntry{
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

        public C151Entry(ByteBuffer bb, int offset) {
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

            Utils.DummyLog("C151Entry");
        }
    }

    public static class C152Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public Vector2 UnknownVector1;
            public Vector2 UnknownVector2;
            public Vector2 UnknownVector3;
            public Vector2 UnknownVector4;
            public int Unknown3;
            public int Unknown4;
            public int Unknown5;
        }

        public HeaderData Header = new HeaderData();

        public C152Entry(ByteBuffer bb, int offset) {
            bb.position(offset);
            byte[] signature = new byte[4];

            bb.get(signature);
            Header.Magic2 = new String(signature).trim();
            Header.HeaderSize = bb.getInt();
            Header.EntryID = bb.getShort();
            Header.EntryType = bb.getShort();
            Header.Unknown1 = bb.getInt();
            Header.Unknown2 = bb.getInt();
            Header.UnknownVector1 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.UnknownVector2 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.UnknownVector3 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.UnknownVector4 = new Vector2(bb.getFloat(), bb.getFloat());
            Header.Unknown3 = bb.getInt();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();

            Utils.DummyLog("C152Entry");
        }
    }

    public static class C153Entry implements ITmlbEntry{
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

        public C153Entry(ByteBuffer bb, int offset) {
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

            Utils.DummyLog("C153Entry");
        }
    }

    public static class C154Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public float UnknownFloat1;
            public int Unknown4;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
        }

        public HeaderData Header = new HeaderData();

        public C154Entry(ByteBuffer bb, int offset) {
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
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();

            Utils.DummyLog("C154Entry");
        }
    }

    public static class C155Entry implements ITmlbEntry{
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

        public C155Entry(ByteBuffer bb, int offset) {
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

            Utils.DummyLog("C155Entry");
        }
    }

    public static class C156Entry implements ITmlbEntry{
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
            public short UnknownShort1;
            public short UnknownShort2;
            public int Unknown6;
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
            public int Unknown20;
            public int Unknown21;
            public int Unknown22;
            public int Unknown23;
            public int Unknown24;
            public int Unknown25;
        }

        public HeaderData Header = new HeaderData();

        public C156Entry(ByteBuffer bb, int offset) {
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
            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
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
            Header.Unknown20 = bb.getInt();
            Header.Unknown21 = bb.getInt();
            Header.Unknown22 = bb.getInt();
            Header.Unknown23 = bb.getInt();
            Header.Unknown24 = bb.getInt();
            Header.Unknown25 = bb.getInt();

            Utils.DummyLog("C156Entry");
        }
    }

    public static class C157Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int EntryOffset;
            public int EntryCount;
        }

        public HeaderData Header = new HeaderData();
        public int[] Entry;

        public C157Entry(ByteBuffer bb, int offset) {
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
            Header.EntryOffset = bb.getInt();
            Header.EntryCount = bb.getInt();

            Entry = new int[Header.EntryCount];
            for (int i = 0; i < Header.EntryCount; i++){
                bb.position(BaseOffset + Header.EntryOffset + i * 4);
                Entry[i] = bb.getInt();
            }

            Utils.DummyLog("C157Entry");
        }
    }

    public static class C158Entry implements ITmlbEntry{
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

        public C158Entry(ByteBuffer bb, int offset) {
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

            Utils.DummyLog("C158Entry");
        }
    }

    public static class C159Entry implements ITmlbEntry{
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

        public C159Entry(ByteBuffer bb, int offset) {
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

            Utils.DummyLog("C159Entry");
        }
    }

    public static class C161Entry implements ITmlbEntry{
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

        public C161Entry(ByteBuffer bb, int offset) {
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

            Utils.DummyLog("C161Entry");
        }
    }

    public static class C162Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public short UnknownShort1;
            public short UnknownShort2;
            public short UnknownShort3;
            public short UnknownShort4;
            public Vector3 UnknownVector1;
            public Vector3 UnknownVector2;
            public Vector3 UnknownVector3;
            public Vector3 UnknownVector4; //π
            public Vector3 UnknownVector5;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
            public int Unknown8;
            public Vector2 UnknownVector6;
        }

        public HeaderData Header = new HeaderData();

        public C162Entry(ByteBuffer bb, int offset) {
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
            Header.UnknownShort1 = bb.getShort();
            Header.UnknownShort2 = bb.getShort();
            Header.UnknownShort3 = bb.getShort();
            Header.UnknownShort4 = bb.getShort();
            Header.UnknownVector1 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownVector2 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownVector3 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownVector4 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.UnknownVector5 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.UnknownVector6 = new Vector2(bb.getFloat(), bb.getFloat());

            Utils.DummyLog("C162Entry");
        }
    }

    public static class C163Entry implements ITmlbEntry{
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

        public C163Entry(ByteBuffer bb, int offset) {
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

            Utils.DummyLog("C163Entry");
        }
    }

    public static class C164Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int NameOffset;
            public int Unknown4;
            public int Unknown5;
        }

        public HeaderData Header = new HeaderData();
        public String Name;

        public C164Entry(ByteBuffer bb, int offset) {
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
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();

            Name = ByteArrayExtensions.ReadString(bb, BaseOffset + Header.NameOffset);

            Utils.DummyLog("C164Entry");
        }
    }

    public static class C165Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public float Unknown5;
            public int Unknown6;
            public int Unknown7;
            public int Unknown8;
            public int Unknown9;
        }

        public HeaderData Header = new HeaderData();

        public C165Entry(ByteBuffer bb, int offset) {
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
            Header.Unknown5 = bb.getFloat();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.Unknown9 = bb.getInt();

            Utils.DummyLog("C165Entry");
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


            Utils.DummyLog("C168Entry");
        }
    }

    public static class C169Entry implements ITmlbEntry{
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

        public C169Entry(ByteBuffer bb, int offset) {
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

            Utils.DummyLog("C169Entry");
        }
    }

    public static class C171Entry implements ITmlbEntry{
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
            public int EntryOffset1;
            public int EntryCount1;
            public int Unknown8;
            public int Unknown9;
            public int EntryOffset2;
            public int EntryCount2;
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
        public C171DataEntry[] Entry1;
        public float[] Entry2;

        public C171Entry(ByteBuffer bb, int offset) {
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
            int EntryBaseOffset = bb.position();
            Header.Unknown4 = bb.getInt();
            Header.Unknown5 = bb.getInt();
            Header.EntryOffset1 = bb.getInt();
            Header.EntryCount1 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.Unknown9 = bb.getInt();
            Header.EntryOffset2 = bb.getInt();
            Header.EntryCount2 = bb.getInt();
            Header.Unknown12 = bb.getInt();
            Header.Unknown13 = bb.getInt();
            Header.Unknown14 = bb.getInt();
            Header.Unknown15 = bb.getInt();
            Header.Unknown16 = bb.getInt();
            Header.Unknown17 = bb.getInt();
            Header.Unknown18 = bb.getInt();
            Header.Unknown19 = bb.getInt();

            bb.position(EntryBaseOffset + Header.EntryOffset1);
            if (Header.Unknown4 != 1 || Header.Unknown5 != 2){
                Utils.DummyLog("データ構造が違うかも");
            }
            Entry1 = new C171DataEntry[Header.EntryCount1];
            for (int i = 0; i < Header.EntryCount1; i++){
                Entry1[i] = new C171DataEntry(bb);
            }

            bb.position(BaseOffset + Header.EntryOffset2);
            if (Header.Unknown8 != 0 || Header.Unknown9 != 0){
                Utils.DummyLog("データ構造が違うかも");
            }
            Entry2 = new float[Header.EntryCount2];
            for (int i = 0; i < Header.EntryCount2; i++){
                Entry2[i] = bb.getFloat();
            }



            Utils.DummyLog("C171Entry");
        }

        public static class C171DataEntry{
            public Vector3 UnknownVector1;
            public Vector3 UnknownVector2;
            public Vector3 UnknownVector3;

            public C171DataEntry(ByteBuffer bb) {
                UnknownVector1 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
                UnknownVector2 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
                UnknownVector3 = new Vector3(bb.getFloat(), bb.getFloat(), bb.getFloat());
            }


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

            Utils.DummyLog("C174Entry");
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

            Utils.DummyLog("C175Entry");
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

            Utils.DummyLog("C176Entry");
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

            Utils.DummyLog("C177Entry");
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

            Utils.DummyLog("C178Entry");
        }
    }

    public static class C179Entry implements ITmlbEntry{
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
            public float Unknown6;
            public float Unknown7;
            public float Unknown8;
        }

        public HeaderData Header = new HeaderData();

        public C179Entry(ByteBuffer bb, int offset) {

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
            Header.Unknown6 = bb.getFloat();
            Header.Unknown7 = bb.getFloat();
            Header.Unknown8 = bb.getFloat();

            Utils.DummyLog("C179Entry");
        }
    }

    public static class C180Entry implements ITmlbEntry{
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

        public C180Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C180Entry");
        }
    }

    public static class C181Entry implements ITmlbEntry{
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

        public C181Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C181Entry");
        }
    }

    public static class C182Entry implements ITmlbEntry{
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

        public C182Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C182Entry");
        }
    }

    public static class C183Entry implements ITmlbEntry{
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

        public C183Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C183Entry");
        }
    }

    public static class C184Entry implements ITmlbEntry{
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

        public C184Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C184Entry");
        }
    }

    public static class C185Entry implements ITmlbEntry{
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

        public C185Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C185Entry");
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

            Utils.DummyLog("C187Entry");
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

            Utils.DummyLog("C188Entry");
        }
    }

    public static class C190Entry implements ITmlbEntry{
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

        public C190Entry(ByteBuffer bb, int offset) {
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

            Utils.DummyLog("C190Entry");
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


            Utils.DummyLog("C192Entry");
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

            Utils.DummyLog("C194Entry");
        }
    }

    public static class C196Entry implements ITmlbEntry{
        public static class HeaderData{
            public String Magic2;  // C010等
            public int HeaderSize;   //uint
            public short EntryID;
            public short EntryType;
            public int Unknown1;
            public int Unknown2;
            public int Unknown3;
            public int Unknown4;
            public float UnknownFloat1;
            public int Unknown5;
            public int Unknown6;
            public int Unknown7;
            public int Unknown8;
            public int Unknown9;
            public int Unknown10;
        }

        public HeaderData Header = new HeaderData();

        public C196Entry(ByteBuffer bb, int offset) {

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
            Header.UnknownFloat1 = bb.getFloat();
            Header.Unknown5 = bb.getInt();
            Header.Unknown6 = bb.getInt();
            Header.Unknown7 = bb.getInt();
            Header.Unknown8 = bb.getInt();
            Header.Unknown9 = bb.getInt();
            Header.Unknown10 = bb.getInt();

            Utils.DummyLog("C196Entry");
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

            Utils.DummyLog("C197Entry");
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

            Utils.DummyLog("C198Entry");
        }
    }

    public static class C200Entry implements ITmlbEntry{
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

        public C200Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C200Entry");
        }
    }

    public static class C201Entry implements ITmlbEntry{
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

        public C201Entry(ByteBuffer bb, int offset) {

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

            Utils.DummyLog("C201Entry");
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

            Utils.DummyLog("C202Entry");
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
