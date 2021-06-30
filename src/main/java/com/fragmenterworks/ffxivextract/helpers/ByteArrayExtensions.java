package com.fragmenterworks.ffxivextract.helpers;

import java.nio.ByteBuffer;

public class ByteArrayExtensions {

    /**
     * ByteBufferの開始オフセット位置から0が出現するまでの文字列を読み取る
     * @param bb ByteBuffer
     * @param offset 開始オフセット
     * @return 文字列
     */
    public static String ReadString(ByteBuffer bb, int offset){
        if (offset > bb.capacity()){
            return "";
        }

        bb.position(offset);
        StringBuilder byteStringBld;
        byteStringBld = new StringBuilder();
        while (bb.position() < bb.capacity()) {
            byte c = bb.get();
            if (c < 0x20) {
                break;
            } else {
                byteStringBld.append((char) c);
            }
        }

        if (byteStringBld.length() == 0) {
            return "";
        }
        return byteStringBld.toString();
    }

}
