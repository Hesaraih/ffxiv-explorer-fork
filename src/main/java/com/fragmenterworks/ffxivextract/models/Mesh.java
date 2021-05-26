package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.jogamp.common.nio.Buffers;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class Mesh {

    private final int numBuffers;
    public final ByteBuffer[] vertBuffers;
    public final ByteBuffer indexBuffer;
    private final int[] vertexBufferOffsets;
    final public int[] vertexSizes;
    final public int indexBufferOffset;
    final public int numVerts, numIndex;
    final public int partTableOffset, partTableCount;
    final public int boneListIndex;
    final public int materialNumber;
    private final int vertElementIndex;

    public Mesh(ByteBuffer bb, int elementIndex) {
        numVerts = bb.getShort() & 0xFFFF; //SaintCoinachではbb.getInt()で処理し次行なし
        bb.getShort();
        numIndex = bb.getInt();

        materialNumber = bb.getShort();
        partTableOffset = bb.getShort();
        partTableCount = bb.getShort();
        boneListIndex = bb.getShort();

        indexBufferOffset = bb.getInt();

        //FFXIVはすでにAuxバッファ（およびその他）のオフセットを保存しているようです。Saint Coinach参考...
        vertexBufferOffsets = new int[3]; //C#では[MarshalAs(UnmanagedType.ByValArray, SizeConst = 3)]
        for (int x = 0; x < vertexBufferOffsets.length; x++) {
            vertexBufferOffsets[x] = bb.getInt();
        }

        vertexSizes = new int[3];
        for (int x = 0; x < vertexSizes.length; x++) {
            vertexSizes[x] = bb.get() & 0xFF;
        }

        numBuffers = bb.get() & 0xFF;

        vertElementIndex = elementIndex;

        vertBuffers = new ByteBuffer[numBuffers];

        for (int i = 0; i < numBuffers; i++) {
            vertBuffers[i] = Buffers.newDirectByteBuffer(numVerts * vertexSizes[i]);
        }
        indexBuffer = Buffers.newDirectByteBuffer(numIndex * 2);

        Utils.getGlobalLogger().trace("Num parts: {}\n\tNum verts: {}\n\tNum indices: {}\n\tVertex offset: {}\n\tIndex offset: {}",
                partTableCount, numVerts, numIndex, vertexBufferOffsets[0], indexBufferOffset);
    }

    public void loadMeshes(ByteBuffer bb, int lodVertexOffset, int lodIndexOffset) throws BufferOverflowException, BufferUnderflowException {

        ByteBuffer bbTemp;

        //Vert Table
        for (int i = 0; i < numBuffers; i++) {
            //lodVertexOffset + vertexBufferOffsets[i]の値がbb.limit()を超え例外が発生している
            if (bb.limit()>lodVertexOffset + vertexBufferOffsets[i]){
                bb.position(lodVertexOffset + vertexBufferOffsets[i]);
                bbTemp = bb.duplicate();
                bbTemp.limit(bbTemp.position() + ((vertexSizes[i] * numVerts)));

                vertBuffers[i].put(bbTemp);
            }
        }
        //Index Table
        if (bb.limit()>lodIndexOffset + (indexBufferOffset * 2)){
            bb.position(lodIndexOffset + (indexBufferOffset * 2));
            bbTemp = bb.duplicate();
            bbTemp.limit(bbTemp.position() + (2 * numIndex));

            indexBuffer.put(bbTemp);
        }
    }

    public int getVertexElementIndex() {
        return vertElementIndex;
    }

    @SuppressWarnings("unused")
    private byte[] reverseElements(byte[] src, int start, int length, int elemSize, int elemAmt) {
        byte[] dest = new byte[length];
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        byte[] tmp = new byte[length];
        System.arraycopy(src, start, tmp, 0, length);

        for (int i = 0; i < elemAmt; i++) {
            for (int j = elemSize - 1; j >= 0; j--) {
                int destOffset = i * elemSize + (elemSize - j - 1);
                int srcOffset = start + (i * elemSize + j);
                int tmpInternalSrcOffset = srcOffset - start;
                dest[destOffset] = src[srcOffset];
            }
        }

        return dest;
    }
}
