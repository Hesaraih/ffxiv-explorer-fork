package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.Utils;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class LoDSubModel {
    //struct ModelHeader
    private final short MeshOffset;
    final public short MeshCount;
    private final int vertTableOffset;
    private final int indexTableOffset;
    private final int VertexDataSize;
    private final int IndexDataSize;

    public Mesh[] meshList;

    private LoDSubModel(short MeshOffset, short MeshCount, int VertexDataSize, int IndexDataSize, int vertOffset, int indexOffset) {
        //struct ModelHeader
        this.MeshOffset = MeshOffset;
        this.MeshCount = MeshCount;
        //ushort[] Unknown1; SizeConst = 0x14
        this.VertexDataSize = VertexDataSize;
        this.IndexDataSize = IndexDataSize;
        this.vertTableOffset = vertOffset; ////SaintCoinachでは以下 :ushort[] Unknown1 ×0x04
        this.indexTableOffset = indexOffset;
    }

    public static LoDSubModel loadInfo(ByteBuffer bb) {
        //struct ModelHeader
        short MeshOffset = bb.getShort();
        short MeshCount = bb.getShort();

        bb.position(bb.position() + 0x28); //ushort[] Unknown1 ×0x14

        int VertexDataSize = bb.getInt();
        int IndexDataSize = bb.getInt();
        int vertOffset = bb.getInt();       //SaintCoinachでは以下 :ushort[] Unknown1 ×0x04
        int indexOffset = bb.getInt();

        Utils.getGlobalLogger().trace("Num meshes: {}", MeshCount);
        Utils.getGlobalLogger().trace("\tVert table size: {}\n\tIndex table size: {}\n\tVert table offset: {}\n\tIndex table offset: {}", VertexDataSize, IndexDataSize, vertOffset, indexOffset);

        return new LoDSubModel(MeshOffset, MeshCount, VertexDataSize, IndexDataSize, vertOffset, indexOffset);
    }

    public void setMeshList(Mesh[] list) {
        meshList = list;
    }

    public void loadMeshes(ByteBuffer bb) throws BufferOverflowException, BufferUnderflowException {
        if (meshList == null) {
            return;
        }

        for (Mesh m : meshList) {
            m.loadMeshes(bb, vertTableOffset, indexTableOffset, VertexDataSize, IndexDataSize);
        }
    }

    @SuppressWarnings("unused")
    public short getMeshOffset() {
        return MeshOffset;
    }

    @SuppressWarnings("unused")
    public int getVertexDataSize() {
        return VertexDataSize;
    }

    @SuppressWarnings("unused")
    public int getIndexDataSize() {
        return IndexDataSize;
    }
}
