package com.fragmenterworks.ffxivextract.helpers;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.Main;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile;
import com.fragmenterworks.ffxivextract.models.Texture_File;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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


    @SuppressWarnings("unused")
    public static BufferedImage getIcon(SqPack_IndexFile sqPak, int iconID) {
        String file;
        if (sqPak == null) {
            file = Constants.datPath;
        } else {
            file = sqPak.getPath();
            file = new File(file).getParentFile().getPath();
        }
        return getIcon(file, iconID);
    }

    @SuppressWarnings("unused")
    public static BufferedImage getIcon(int iconID) {
        return getIcon((String) null, iconID);
    }

    public static BufferedImage getIcon(String sqPakPath, int iconID) {
        if (sqPakPath == null) {
            sqPakPath = Constants.datPath;
        }
        String iconPath = String.format("ui/icon/%06d/%06d.tex", iconID - (iconID % 1000), iconID);
        Utils.getGlobalLogger().debug("IconPath: {}, iconID: {}", iconPath, iconID);
        return getTexture(sqPakPath, iconPath);
    }


    @SuppressWarnings("unused")
    public static byte[] getRaw(SqPack_IndexFile sqPak, String path) {
        String file = sqPak.getPath();
        String sqPackPath = new File(file).getParentFile().getPath();
        return getRaw(sqPackPath, path);
    }

    @SuppressWarnings("unused")
    public static byte[] getRaw(String path) {
        return getRaw((String) null, path);
    }


    public static byte[] getRaw(String sqPakPath, String path) {
        if (sqPakPath == null) {
            sqPakPath = Constants.datPath;
        }
        String lowerPath = path.toLowerCase();
        String dat = getDatByPath(lowerPath);

        if (!sqPakPath.endsWith(File.separator)) {
            sqPakPath += File.separator;
        }

        if (!sqPakPath.endsWith("\\game\\sqpack\\ffxiv\\")) {
            sqPakPath += "\\game\\sqpack\\ffxiv\\";
        }

        SqPack_IndexFile index = SqPack_IndexFile.createIndexFileForPath(sqPakPath + dat + ".index", true);

        if (index != null) {
            try {
                return index.extractFile(lowerPath);
            } catch (IOException e) {
                Utils.getGlobalLogger().error(e);
            }
        }
        return new byte[0];
    }

    private static String getDatByPath(final String fullPath) {
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

    @SuppressWarnings("unused")
    public static BufferedImage getTexture(String path) {
        return getTexture(null, path);
    }

    public static BufferedImage getTexture(String sqPakPath, String path) {
        if (sqPakPath == null) {
            sqPakPath = Constants.datPath;
        }
        byte[] data = getRaw(sqPakPath, path);

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
