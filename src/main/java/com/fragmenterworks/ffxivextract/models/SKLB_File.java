package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SKLB_File extends Game_File {

    private byte[] havokData;

    @SuppressWarnings("unused")
    public SKLB_File(String path, ByteOrder endian) throws IOException {
        super(endian);
        File file = new File(path);
        byte[] data;
        try (FileInputStream fis = new FileInputStream(file)) {
            data = new byte[(int) file.length()];
            while (fis.read(data) != -1) {
                Utils.getGlobalLogger().debug("SKLB読み取り");
            }
        }
        loadSKLB(data);
    }

    public SKLB_File(byte[] data, ByteOrder endian) throws IOException {
        super(endian);
        loadSKLB(data);
    }

    @SuppressWarnings({"unused", "UnusedAssignment"})
    private void loadSKLB(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);

        int magic = bb.getInt();
        if (magic != 0x736B6C62) {
            Utils.getGlobalLogger().error("SKLB magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", String.format("0x%08X", magic));
            return;
        }

        int version = bb.getInt();

        int offsetToSkeletonSection = -1;
        int offsetToHavokFile = -1;

        //Different depending if 0031 or 0021
        if (version == 0x31333030) {
            offsetToSkeletonSection = bb.getInt();
            offsetToHavokFile = bb.getInt();
            bb.getShort();
            bb.getShort();
            bb.getInt();
            bb.getInt();
        } else if (version == 0x31323030) {
            offsetToSkeletonSection = bb.getShort();
            offsetToHavokFile = bb.getShort();
            bb.getShort();
            bb.getShort();
        }

        //SKEL INFO SECTION

        //HAVOK FILE
        bb.position(offsetToHavokFile);
        havokData = new byte[bb.limit() - offsetToHavokFile];
        bb.get(havokData);

    }

    public byte[] getHavokData() {
        return havokData;
    }

}
