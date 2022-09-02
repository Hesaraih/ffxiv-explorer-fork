package com.fragmenterworks.ffxivextract.helpers;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.Main;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile;
import com.fragmenterworks.ffxivextract.models.Texture_File;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.awt.image.BufferedImage;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.prefs.Preferences;

/**
 * Created by Roze on 2017-06-17.
 *
 * @author Roze
 */
public class FileTools {

    static {
        if (Constants.datPath == null) {
            Preferences prefs = Preferences.userNodeForPackage(Main.class);
            Constants.datPath = prefs.get(Constants.PREF_DAT_PATH, null);
        }
    }

    public static BufferedImage getIcon(int iconID) {
        String iconPath = String.format("ui/icon/%06d/%06d.tex", iconID - (iconID % 1000), iconID);
        Utils.getGlobalLogger().info("IconPath: {}, iconID: {}", iconPath, iconID);
        return getTexture(iconPath);
    }

    /**
     * ArchiveIDからIndexFileのフルパスを生成
     * @param ArchiveID ArchiveID(例:030100)
     * @return IndexFileフルパス
     */
    public static String ArchiveID2IndexFilePath(String ArchiveID) {
        String exID = ArchiveID.substring(3,4);
        String exPath;
        if ("0".equals(exID)) {
            exPath = "ffxiv";
        } else {
            exPath = "ex" + exID;
        }

        return String.format("%s\\game\\sqpack\\%s\\%s.win32.index", Constants.datPath, exPath, ArchiveID);
    }

    public static byte[] getRaw(String path) {
        String lowerPath = path.toLowerCase();
        String ArchiveID = HashDatabase.getArchiveID(lowerPath);

        SqPack_IndexFile index = SqPack_IndexFile.GetIndexFileForArchiveID(ArchiveID, true);

        if (index != null) {
            return index.extractFile(lowerPath);
        }
        return new byte[0];
    }

    public static BufferedImage getTexture(String path) {
        byte[] data = getRaw(path);

        //TODO: Random ULD stuff can be little-endian, no?
        Texture_File tf = new Texture_File(data, ByteOrder.LITTLE_ENDIAN);
        try {
            return tf.decode(0, new HashMap<>());
        } catch (ImageDecoding.ImageDecodingException e) {
            Utils.getGlobalLogger().error(e);
        }
        return null;

    }

}
