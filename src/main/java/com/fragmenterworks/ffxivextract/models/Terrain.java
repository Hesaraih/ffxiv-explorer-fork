package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Terrain extends Game_File {
    private final String FilePath;

    public TransformedModel[] Parts;

    public Terrain(byte[] data, ByteOrder endian, String filePath){
        super(endian);
        FilePath = filePath;
        loadTera(data);
    }


    private void loadTera(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        final int CountOffset = 0x04;
        final int SizeOffset = 0x08;
        final int BlockPositionsOffset = 0x34;
        final int BlockPositionSize = 0x04;

        bb.position(CountOffset);
        int blockCount = bb.getInt();
        bb.position(SizeOffset);
        int blockSize = bb.getInt();

        String blockDirectory = FilePath.substring(0, FilePath.lastIndexOf('/') + 1);

        Parts = new TransformedModel[blockCount];
        for (int i = 0; i < blockCount; ++i) {
            String blockPath = blockDirectory + String.format("%d4.mdl", i);
            String archive = HashDatabase.getArchiveID(blockPath);
            SqPack_IndexFile tempIndex = SqPack_IndexFile.GetIndexFileForArchiveID(archive, true);
            byte[] ModelData = tempIndex.extractFile(blockPath);
            Model blockModelFile = new Model(null, tempIndex, ModelData, tempIndex.getEndian());

            bb.position(BlockPositionsOffset + BlockPositionSize * i);
            short x = bb.getShort();
            short z = bb.getShort();

            Vector3 translation = new Vector3(blockSize * (x + 0.5f), 0, blockSize * (z + 0.5f));
            Parts[i] = new TransformedModel(blockModelFile, translation, Vector3.Zero, Vector3.One);
        }

    }
}
