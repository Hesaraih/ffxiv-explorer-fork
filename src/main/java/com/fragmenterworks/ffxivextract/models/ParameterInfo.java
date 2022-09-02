package com.fragmenterworks.ffxivextract.models;

import java.nio.ByteBuffer;

public class ParameterInfo {
    final public int id;
    final public int stringOffset;
    final public int stringSize;
    private final int registerIndex;
    private final int registerCount;

    public String parameterName;

    public ParameterInfo(ByteBuffer bb) {

        id = bb.getInt();
        stringOffset = bb.getInt();
        stringSize = bb.getInt();
        registerIndex = Short.toUnsignedInt(bb.getShort());
        registerCount = Short.toUnsignedInt(bb.getShort());

    }

    @SuppressWarnings("unused")
    public int getRegisterIndex() {
        return registerIndex;
    }

    @SuppressWarnings("unused")
    public int getRegisterCount() {
        return registerCount;
    }
}
