package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.ByteArrayExtensions;
import com.fragmenterworks.ffxivextract.helpers.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PAP_File extends Game_File {

    //他のファイルを見つけるために使用されます
    private static SqPack_IndexFile currentIndex; //現在表示中または呼び出し元のIndexファイル

    public static class HeaderData {
        public String Magic; //uint
        public short Unknown1;
        public short Unknown2;
        public short AnimationCount;
        public short ModelId;
        public byte BaseId; //byte
        public byte VariantId; //byte
        public short NameTableOffset;
        public short Unknown6;
        public int HavokDataOffset;
        public int ParametersOffset;
    }

    public HeaderData Header;
    public PapAnimation[] Animations;
    public byte[] HavokData;
    public byte[] Parameters;
    public TmbFile tmbFile;

    public PAP_File(SqPack_IndexFile index, byte[] data, ByteOrder endian){
        super(endian);
        currentIndex = index;
        loadPAP(data);
    }

    private void loadPAP(byte[] data) {
        byte[] signature = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);

        Header = new HeaderData();

        bb.get(signature);
        Header.Magic = new String(signature).trim();

        if (!Header.Magic.equals("pap")) {
            Utils.getGlobalLogger().error("PAP magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", Header.Magic);
            return;
        }
        Header.Unknown1 = bb.getShort();
        Header.Unknown2 = bb.getShort();
        Header.AnimationCount = bb.getShort();

        Header.ModelId = bb.getShort();
        Header.BaseId = bb.get();
        Header.VariantId = bb.get();

        Header.NameTableOffset = bb.getShort();
        Header.Unknown6 = bb.getShort();

        Header.HavokDataOffset = bb.getInt();
        Header.ParametersOffset = bb.getInt();

        //ANIM NAME TABLE
        Animations = new PapAnimation[Header.AnimationCount];

        int nameTableOffset = bb.position();
        for (int i = 0; i < Header.AnimationCount; ++i) {
            Animations[i] = new PapAnimation(bb, nameTableOffset);
        }

        //HAVOK FILE
        HavokData = new byte[Header.ParametersOffset - Header.HavokDataOffset];
        bb.get(HavokData);

        //FOOTER
        Parameters = new byte[bb.capacity() - Header.ParametersOffset];
        bb.get(Parameters);
        tmbFile = new TmbFile(currentIndex, Parameters, currentIndex.getEndian());
    }

    public static class PapAnimation {
        public String Name;
        public short Unknown20;
        public int Index;
        public short Unknown26;

        protected PapAnimation(ByteBuffer bb, int offset) {
            Name = ByteArrayExtensions.ReadString(bb, offset);
            bb.position(offset + 0x20);
            Unknown20 = bb.getShort();
            Index = bb.getInt();
            Unknown26 = bb.getShort();
        }
    }
    public int getAnimationCount() {
        return Header.AnimationCount;
    }
}
