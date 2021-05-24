package com.fragmenterworks.ffxivextract.helpers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Utils {

    private static final Logger logger = LogManager.getLogger();

    public static Logger getGlobalLogger() {
        return logger;
    }

    /**
     * 単精度（32ビット）浮動小数点値を半精度（16ビット）に変換します。
     * <p>
     * Source: http://www.fox-toolkit.org/ftp/fasthalffloatconversion.pdf
     *
     * @param half shortとしての半精度浮動小数点値
     * @return 半精度浮動小数点値
     */
    public static float convertHalfToFloat(short half) {
        switch ((int) half) {
            case 0x0000:
                return 0f;
            case 0x8000:
                return -0f;
            case 0x7c00:
                return Float.POSITIVE_INFINITY;
            case 0xfc00:
                return Float.NEGATIVE_INFINITY;
            // TODO: Support for NaN?
            default:
                return Float.intBitsToFloat(((half & 0x8000) << 16)
                        | (((half & 0x7c00) + 0x1C000) << 13)
                        | ((half & 0x03FF) << 13));
        }
    }

    @SuppressWarnings("unused")
    public static short convertFloatToHalf(float flt) {
        if (Float.isNaN(flt)) {
            throw new UnsupportedOperationException("Not a Numberから半精度浮動小数点への変換はサポートされていません!");
        } else if (flt == Float.POSITIVE_INFINITY) {
            return (short) 0x7c00;
        } else if (flt == Float.NEGATIVE_INFINITY) {
            return (short) 0xfc00;
        } else if (flt == 0f) {
            return (short) 0x0000;
        } else if (flt == -0f) {
            return (short) 0x8000;
        } else if (flt > 65504f) {
            // 半精度浮動小数点でサポートされる最大値
            return 0x7bff;
        } else if (flt < -65504f) {
            return (short) (0x7bff | 0x8000);
        } else if (flt > 0f && flt < 5.96046E-8f) {
            return 0x0001;
        } else if (flt < 0f && flt > -5.96046E-8f) {
            return (short) 0x8001;
        }

        int f = Float.floatToIntBits(flt);
        return (short) (((f >> 16) & 0x8000)
                | ((((f & 0x7f800000) - 0x38000000) >> 13) & 0x7c00)
                | ((f >> 13) & 0x03ff));
    }

    public static String getRegexpFromFormatString(String format) {
        String toReturn = format;

        // いくつかの特別な正規表現文字をエスケープ
        toReturn = toReturn.replaceAll("\\.", "\\\\.");
        toReturn = toReturn.replaceAll("!", "\\\\!");

        if (toReturn.contains("%")) {
            toReturn = toReturn.replaceAll("%s", "[\\\\w]+"); //accepts 0-9 A-Z a-z _

            while (toReturn.matches(".*%([0-9]+)[d].*")) {
                String digitStr = toReturn.replaceFirst(".*%([0-9]+)[d].*", "$1");
                int numDigits = Integer.parseInt(digitStr);
                toReturn = toReturn.replaceFirst("(.*)(%[0-9]+[d])(.*)", "$1[0-9]{" + numDigits + "}$3");
            }
        }

        return "^" + toReturn + "$";
    }

    public static byte[] readContentIntoByteArray(File file) throws IOException {
        FileInputStream fileInputStream;
        byte[] bFile = new byte[(int) file.length()];

        //ファイルをバイトの配列に変換
        fileInputStream = new FileInputStream(file);
        //noinspection ResultOfMethodCallIgnored
        fileInputStream.read(bFile);
        fileInputStream.close();

        return bFile;
    }

}
