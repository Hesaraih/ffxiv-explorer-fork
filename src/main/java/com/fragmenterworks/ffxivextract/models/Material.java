package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.ShaderIdHelper;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.shaders.*;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;
import com.jogamp.opengl.GL3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Material extends Game_File {

    @SuppressWarnings("unused")
    String materialPath;

    //Data
    private int fileSize;
    private int shaderStringOffset;

    private String[] stringArray;

    private Texture_File diffuse;
    private Texture_File mask;
    private Texture_File normal;
    private Texture_File specular;
    private Texture_File colorSet;

    private byte[] colorSetData;

    //Rendering
    private boolean shaderReady = false;

    private Shader shader;

    //Rendering
    final int[] textureIds = new int[5];

    /**
     * コンストラクターがマテリアルに関する情報を取得します
     * @param data materialのバイトデータ
     * @param endian エンディアン指定
     */
    public Material(byte[] data, ByteOrder endian) {
        this(null, null, data, endian);
    }

    /**
     * コンストラクターが情報ファイルとテクスチャファイルを取得します
     * @param folderPath materialのフォルダパス(null可)
     * @param currentIndex 現在のインデックスファイル名(null可)
     * @param data materialのバイトデータ
     * @param endian エンディアン指定
     */
    public Material(String folderPath, SqPack_IndexFile currentIndex, byte[] data, ByteOrder endian) {
        super(endian);

        if (data == null) {
            return;
        }

        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        bb.getInt();
        fileSize = bb.getShort();
        int colorTableSize = bb.getShort();
        int stringSectionSize = bb.getShort();
        shaderStringOffset = bb.getShort();
        short numPaths = bb.get(); //texファイルの数
        short numMaps = bb.get();
        short numColorSets = bb.get();
        short numUnknown = bb.get();

        //Load Strings
        stringArray = new String[numPaths + numMaps + numColorSets + 1];
        byte[] stringBuffer = new byte[stringSectionSize];
        bb.position(bb.position() + (4 * (numPaths + numMaps + numColorSets)));
        bb.get(stringBuffer);

        int stringCounter = 0;
        int start = 0, end = 0;
        for (byte b : stringBuffer) {
            if (b == 0) {
                if (stringCounter >= stringArray.length) {
                    break;
                }
                stringArray[stringCounter] = new String(stringBuffer, start, end - start);
                start = end + 1;
                stringCounter++;
            }
            end++;
        }

        //テクスチャを読み込む
        if (folderPath != null && currentIndex != null) {
            for (int i = 0; i < numPaths; i++) {
                String s = stringArray[i];

                if (!s.contains("/")) {
                    Utils.getGlobalLogger().debug("{}を読み込めませんでした", s);
                    continue;
                }

                String folderName = s.substring(0, s.lastIndexOf("/"));
                String fileString = s.substring(s.lastIndexOf("/") + 1);
                //texファイルの登録
                HashDatabase.addPathToDB(s, currentIndex.getName());

                byte[] extracted = currentIndex.extractFile(folderName, fileString);
                if (extracted == null) {
                    continue;
                }

                if ((fileString.endsWith("_d.tex") || fileString.contains("catchlight")) && diffuse == null) {
                    //拡散光
                    diffuse = new Texture_File(extracted, endian);
                } else if (fileString.endsWith("_n.tex") && normal == null) {
                    //法線マップ
                    normal = new Texture_File(extracted, endian);
                } else if (fileString.endsWith("_s.tex") && specular == null) {
                    //鏡面光
                    specular = new Texture_File(extracted, endian);
                } else if (fileString.endsWith("_m.tex")) {
                    //アルファマスク
                    mask = new Texture_File(extracted, endian);
                } else {
                    colorSet = new Texture_File(extracted, endian);
                }

            }
        }

        //これは、新しいマテリアルファイルのセットアップ用です。 また、bgColorChangeにはテーブルの色がありませんが、それがどこにあるかを見つけることができません。
        if (colorSet == null && colorTableSize >= 512) {
            bb.position(16 + (4 * (numPaths + numMaps + numColorSets)) + stringBuffer.length);
            if (bb.getInt() == 0x0) {
                return;
            }
            colorSetData = new byte[512];
            bb.get(colorSetData);
        }

        //シェーダーリンクと未知数はここから始まります
        bb.position(16 + (4 * (numPaths + numMaps + numColorSets)) + stringBuffer.length + colorTableSize + numUnknown);

        int unknownDataSize = bb.getShort();
        int count1 = bb.getShort();
        int count2 = bb.getShort();
        int count3 = bb.getShort();
        bb.getShort();
        bb.getShort();

        byte[] unknownData = new byte[unknownDataSize];
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        Unknown1[] unknownList1 = new Unknown1[count1];
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        Unknown2[] unknownList2 = new Unknown2[count2];
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        Parameter[] parameterList = new Parameter[count3];

        for (int i = 0; i < unknownList1.length; i++) {
            unknownList1[i] = new Unknown1(bb.getInt(), bb.getInt());
        }
        for (int i = 0; i < unknownList2.length; i++) {
            unknownList2[i] = new Unknown2(bb.getInt(), bb.getShort(), bb.getShort());
        }
        for (int i = 0; i < parameterList.length; i++) {
            parameterList[i] = new Parameter(bb.getInt(), bb.getShort(), bb.getShort(), bb.getInt());
        }

        bb.get(unknownData);
    }

    public void loadShader(GL3 gl) {
        //Weird case
        if (stringArray[stringArray.length - 1] == null) {
            int x = stringArray.length - 1;
            do {
                x--;
                stringArray[stringArray.length - 1] = stringArray[x];
            } while (stringArray[x] == null);
        }

        //Load Shader
        String shaderName = "";
        for (String s : stringArray) {
            if (s.contains(".shpk")) {
                shaderName = s;
                break;
            }
        }

        try {
            switch (shaderName) {
                case "character.shpk":
                    shader = new CharacterShader(gl);
                    break;
                case "hair.shpk":
                    shader = new HairShader(gl);
                    break;
                case "iris.shpk":
                    shader = new IrisShader(gl);
                    break;
                case "skin.shpk":
                    shader = new SkinShader(gl);
                    break;
                case "bg.shpk":
                case "bgcolorchange.shpk":
                    shader = new BGShader(gl);
                    break;
                default:
                    shader = new DefaultShader(gl);
                    break;
            }
        } catch (IOException e) {
            Utils.getGlobalLogger().error(e);
        }

        shaderReady = true;
    }

    public Shader getShader() {
        return shader;
    }

    public boolean isShaderReady() {
        return shaderReady;
    }

    public Texture_File getDiffuseMapTexture() {
        return diffuse;
    }

    public Texture_File getMaskTexture() {
        return mask;
    }

    public Texture_File getNormalMapTexture() {
        return normal;
    }

    public Texture_File getSpecularMapTexture() {
        return specular;
    }

    public Texture_File getColorSetTexture() {
        return colorSet;
    }

    public byte[] getColorSetData() {
        return colorSetData;
    }

    public int[] getGLTextureIds() {
        return textureIds;
    }

    public int getFileSize() {
        return fileSize;
    }

    @SuppressWarnings("unused")
    public int getShaderStringOffset() {
        return shaderStringOffset;
    }

    static class Unknown1 {
        final int unknown1;
        final int unknown2;

        Unknown1(int unknown1, int unknown2) {
            this.unknown1 = unknown1;
            this.unknown2 = unknown2;
        }
    }

    static class Unknown2 {
        final int unknown1;
        final short offset;
        final short size;

        Unknown2(int unknown1, short offset, short size) {
            this.unknown1 = unknown1;
            this.offset = offset;
            this.size = size;
        }
    }

    static class Parameter {
        final int id;
        final short unknown1;
        final short unknown2;
        final int index;

        Parameter(int id, short unknown1, short unknown2, int index) {
            this.id = id;
            this.unknown1 = unknown1;
            this.unknown2 = unknown2;
            this.index = index;

            Utils.getGlobalLogger().trace("Shader name: {}", ShaderIdHelper.getName(id));
        }
    }
}
