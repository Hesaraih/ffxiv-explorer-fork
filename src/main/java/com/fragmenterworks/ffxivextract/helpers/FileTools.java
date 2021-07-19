package com.fragmenterworks.ffxivextract.helpers;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.Main;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile;
import com.fragmenterworks.ffxivextract.models.Texture_File;

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
        String ArchiveID = getArchiveID_ByPath(lowerPath);

        SqPack_IndexFile index = SqPack_IndexFile.GetIndexFileForArchiveID(ArchiveID, true);

        if (index != null) {
            return index.extractFile(lowerPath);
        }
        return new byte[0];
    }

    private static String getArchiveID_ByPath(final String fullPath) {
        if (fullPath.startsWith("common/")){return "000000" + ".win32";}
        else if (fullPath.startsWith("bgcommon/")){return "010000" + ".win32";}
        else if (fullPath.startsWith("bg/")){
            if (fullPath.charAt(5)=='x'){
                //bg/ffxiv
                return "020000" + ".win32";
            }
            else{
                //bg/ex1/01_
                return "020"+ fullPath.charAt(5) + fullPath.substring(7,9) + ".win32";
            }
        }
        else if (fullPath.startsWith("cut/")){return "030000" + ".win32";}
        else if (fullPath.startsWith("chara/")){return "040000" + ".win32";}
        else if (fullPath.startsWith("shader/")){return "050000" + ".win32";}
        else if (fullPath.startsWith("ui/")){return "060000" + ".win32";}
        else if (fullPath.startsWith("sound/")){return "070000" + ".win32";}
        else if (fullPath.startsWith("vfx/")){return "080000" + ".win32";}
        else if (fullPath.startsWith("ui_script/")){return "090000" + ".win32";}
        else if (fullPath.startsWith("exd/")){return "0a0000" + ".win32";}
        else if (fullPath.startsWith("game_script/")){return "0b0000" + ".win32";}
        else if (fullPath.startsWith("music/")){
            if (fullPath.charAt(8)=='x'){
                //music/ffxiv
                return "0c0000" + ".win32";
            }
            else{
                //music/ex1
                return "0c0" + fullPath.charAt(8) + "00" + ".win32";
            }
        }
        else if (fullPath.startsWith("sqpack_test/")){return "120000" + ".win32";}
        else if (fullPath.startsWith("debug/")){return "130000" + ".win32";}
        return "";
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
