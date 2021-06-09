package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.jogamp.common.nio.Buffers;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class Mesh {

    private final int VertexBufferCount;
    public final ByteBuffer[] vertBuffers;
    public final ByteBuffer indexBuffer;
    private final int[] VertexOffsets;
    final public int[] BytesPerVertexPerBuffer;
    final public int IndexBufferOffset;
    final public int VertexCount, IndexCount;
    final public int PartOffset, PartCount;
    final public int BoneListIndex;
    final public int MaterialIndex;
    private final int vertElementIndex;

    public Mesh(ByteBuffer bb, int elementIndex) {
        //MeshHeader
        VertexCount = bb.getShort() & 0xFFFF; //SaintCoinachではbb.getInt()で処理し次行なし(VertexCount)
        bb.getShort();
        IndexCount = bb.getInt(); //IndexCount

        MaterialIndex = bb.getShort();  //SaintCoinachではMaterialIndex
        PartOffset = bb.getShort();
        PartCount = bb.getShort();
        BoneListIndex = bb.getShort();

        IndexBufferOffset = bb.getInt();

        //FFXIVはすでにAuxバッファ（およびその他）のオフセットを保存しているようです。Saint Coinach参考...
        VertexOffsets = new int[3]; //C#では[MarshalAs(UnmanagedType.ByValArray, SizeConst = 3)]
        for (int x = 0; x < VertexOffsets.length; x++) {
            VertexOffsets[x] = bb.getInt();
        }

        BytesPerVertexPerBuffer = new int[3]; //SaintCoinachではBytesPerVertexPerBuffer
        for (int x = 0; x < BytesPerVertexPerBuffer.length; x++) {
            BytesPerVertexPerBuffer[x] = bb.get() & 0xFF;
        }

        VertexBufferCount = bb.get() & 0xFF; //SaintCoinachではVertexBufferCount

        vertElementIndex = elementIndex;

        vertBuffers = new ByteBuffer[VertexBufferCount];

        for (int i = 0; i < VertexBufferCount; i++) {
            vertBuffers[i] = Buffers.newDirectByteBuffer(VertexCount * BytesPerVertexPerBuffer[i]);
        }
        indexBuffer = Buffers.newDirectByteBuffer(IndexCount * 2);

        Utils.getGlobalLogger().trace("Num parts: {}\n\tNum verts: {}\n\tNum indices: {}\n\tVertex offset: {}\n\tIndex offset: {}",
                PartCount, VertexCount, IndexCount, VertexOffsets[0], IndexBufferOffset);
    }

    public void loadMeshes(ByteBuffer bb, int lodVertexOffset, int lodIndexOffset, int VertexDataSize, int IndexDataSize) throws BufferOverflowException, BufferUnderflowException {

        ByteBuffer bbTemp;

        //Vert Table
        for (int i = 0; i < VertexBufferCount; i++) {
            //lodVertexOffset + vertexBufferOffsets[i]の値がbb.limit()を超え例外が発生している
            bb.position(lodVertexOffset);
            if (bb.limit() > lodVertexOffset + VertexOffsets[i]){
                bb.position(lodVertexOffset + VertexOffsets[i]);
                bbTemp = bb.duplicate();
                //bbTemp.limit(bbTemp.position() + VertexOffsets[i]);

                if(lodVertexOffset + VertexDataSize >= bbTemp.position() + ((BytesPerVertexPerBuffer[i] * VertexCount))) {
                    bbTemp.limit(bbTemp.position() + ((BytesPerVertexPerBuffer[i] * VertexCount)));
                }else{
                    bbTemp.limit(bbTemp.position());
                }

                vertBuffers[i].put(bbTemp);
            }
        }
        //Index Table
        if (bb.limit()>lodIndexOffset + (IndexBufferOffset * 2)){
            bb.position(lodIndexOffset + (IndexBufferOffset * 2));
            bbTemp = bb.duplicate();
            if(IndexDataSize >= (2 * IndexCount))
            {
                bbTemp.limit(bbTemp.position() + (2 * IndexCount));
            }else{
                bbTemp.limit(bbTemp.position() + IndexDataSize);
            }

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
