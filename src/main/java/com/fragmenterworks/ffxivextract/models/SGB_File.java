package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SGB_File extends Game_File {

    @SuppressWarnings("unused")
    public String entryName;
    @SuppressWarnings("unused")
    public String modelName;
    @SuppressWarnings("unused")
    public String collisionName;

    @SuppressWarnings("unused")
    public SGB_File(String path, ByteOrder endian) throws IOException {
        super(endian);
        File file = new File(path);
        byte[] data;
        try (FileInputStream fis = new FileInputStream(file)) {
            data = new byte[(int) file.length()];
            while (fis.read(data) != -1) {
                Utils.getGlobalLogger().debug("SGB読み取り");
            }
        }
        loadSGB(data);
    }

    /**
     * コンストラクタ
     * @param data sgbデータ
     * @param endian エンディアンの種類
     * @throws IOException エラー
     */
    public SGB_File(byte[] data, ByteOrder endian) throws IOException {
        super(endian);
        loadSGB(data);
    }

    @SuppressWarnings("unused")
    private void loadSGB(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        int signature = bb.getInt(); //SGB1
        int fileSize = bb.getInt(); //ファイルサイズ
        bb.getInt(); //不明
        int signature2 = bb.getInt();

        if (signature != 0x31424753) {
            Utils.getGlobalLogger().error("SGB1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", String.format("0x%08X", signature));
        } else if (signature2 != 0x314E4353) {
            Utils.getGlobalLogger().error("SCN1 magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", String.format("0x%08X", signature2));
        }
    }
}
