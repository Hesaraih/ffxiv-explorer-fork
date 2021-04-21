package com.fragmenterworks.ffxivextract.models.uldStuff;

import java.nio.ByteBuffer;

/**
 * Created by Roze on 2017-06-23.
 *
 * @author Roze
 */
public class COHDEntryType_Graphics extends COHDEntryType {
    private final int dw_0x0;
    private final int graphicsNode;

    public COHDEntryType_Graphics(final ByteBuffer data) {
        super(data);
        this.dw_0x0 = data.getInt();
        graphicsNode = data.getInt();
    }

    @Override
    public String toString() {
        return "COHDType_Graphics{" +
                "dw_0x0=" + dw_0x0 +
                ", graphicsNode=" + graphicsNode +
                "} " + super.toString();
    }
}
