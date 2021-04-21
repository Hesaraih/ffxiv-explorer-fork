package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class CMP_File {

    private final ArrayList<Integer> colors = new ArrayList<>();

    @SuppressWarnings("unused")
    public CMP_File(String path) throws IOException {
        File file = new File(path);
        byte[] data;
        try (FileInputStream fis = new FileInputStream(file)) {
            data = new byte[(int) file.length()];
            while (fis.read(data) != -1) {
                Utils.getGlobalLogger().debug("CMP読み取り");
            }
        }
        loadCMP(data);
    }

    public CMP_File(byte[] data) {
        loadCMP(data);
    }

    private void loadCMP(byte[] data) {
        int index = 0;
        while (index < data.length) {
            int b = data[index + 2] & 0xFF;
            int g = data[index + 1] & 0xFF;
            int r = data[index] & 0xFF;
            int a = data[index + 3] & 0xFF;

            index += 4;
            int col = (a << 24) | (r << 16) | (g << 8) | b;

            colors.add(col);
        }


    }

    public ArrayList<Integer> getColors() {
        return colors;
    }
}
