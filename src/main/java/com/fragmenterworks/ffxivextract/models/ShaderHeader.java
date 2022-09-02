package com.fragmenterworks.ffxivextract.models;

import java.nio.ByteBuffer;

public class ShaderHeader {
    final public int type;
    final public int shaderBytecodeOffset;
    final public int shaderBytecodeSize;

    final public ParameterInfo[] paramInfo;

    public ShaderHeader(int type, ByteBuffer bb) {

        this.type = type;
        shaderBytecodeOffset = bb.getInt();
        shaderBytecodeSize = bb.getInt();
        int numConstants = Short.toUnsignedInt(bb.getShort());
        int numSamplers = Short.toUnsignedInt(bb.getShort());
        int BaseOffset = bb.position();
        int numX = bb.getShort();
        int numY = bb.getShort();
        if (numX < 0 || numY < 0){
            bb.position(BaseOffset);
            numX = 0;
            numY = 0;
        }


        //Read in parameter info
        paramInfo = new ParameterInfo[numConstants + numSamplers + numX + numY];
        for (int i = 0; i < paramInfo.length; i++)
            paramInfo[i] = new ParameterInfo(bb);

    }
}