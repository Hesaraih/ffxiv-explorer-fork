package com.fragmenterworks.ffxivextract.models;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

public class IMC_File extends Game_File {

    final HashMap<Integer, ImcPart> _Parts = new HashMap<>();
    public int PartsMask;
    public int Count;

    public IMC_File(byte[] data, ByteOrder endian) {
        super(endian);
        loadIMC(data);
    }

    private void loadIMC(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);

        Count = bb.getShort();
        PartsMask = bb.getShort();
        boolean gotFirst = false;

        //This is weird variants sitting here. SaintCoinach は、部分マスクに基づいて亜種を読み取ります。
        for (int bit = 0; bit < 8; bit++) {
            int match = (byte) (1 << bit);
            if ((PartsMask & match) == match) {
                if (!gotFirst) {
                    gotFirst = true;
                }
                _Parts.put(bit, new ImcPart(bb ,match));
            }
        }

        //Varianceを取得
        int remaining = Count;
        while (--remaining >= 0) {
            for (ImcPart part : _Parts.values()) {
                part.Variants.add(new ImcVariant(bb));
            }
        }
    }

    public ImcVariant getVarianceInfo(int i) {
        if (i > Count || i == -1) {
            return _Parts.get(0).Variants.get(0);
        }

        return _Parts.get(0).Variants.get(i);
    }

    public ArrayList<ImcVariant> getVariantsList(int key) {
        if (_Parts.size() > key) {
           return _Parts.get(key).Variants;
        }
        return _Parts.get(0).Variants;
    }

    public int getCount() {

        return Count;
    }

    public int getPartsSize() {

        return _Parts.size();
   }
    public static class ImcPart {
        public final int Bit;
        public final ArrayList<ImcVariant> Variants = new ArrayList<>();

        ImcPart(ByteBuffer bb, int bit) {
            this.Bit = bit;
            Variants.add(new ImcVariant(bb));
        }
    }

    public static class ImcVariant {
        //Variantをbyteに変更
        public byte Variant;
        public byte SubVariant;

        public short PartVisibilityMask;
        public byte VFX_id1;
        public short VFX_id2;

        ImcVariant(ByteBuffer bb) {
            byte materialNumber  = bb.get();
            if (materialNumber != 0) {
                Variant = materialNumber;
            }else{
                Variant = 1;
            }

            SubVariant = bb.get();
            PartVisibilityMask = bb.getShort();
            VFX_id1 = bb.get();
            VFX_id2 = bb.get();
        }

        @Override
        public String toString() {
            return String.format("Material Set: %d, Hidden Parts:0x%x, VFX ID: %d, VFX ID2: %d", Variant, PartVisibilityMask, VFX_id1, VFX_id2);
        }
    }
}
