package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.helpers.GLHelper;
import com.fragmenterworks.ffxivextract.helpers.HavokNative;
import com.fragmenterworks.ffxivextract.helpers.ImageDecoding.ImageDecodingException;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.models.IMC_File.VarianceInfo;
import com.fragmenterworks.ffxivextract.models.directx.DX9VertexElement;
import com.fragmenterworks.ffxivextract.shaders.*;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL3bc;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Objects;

public class Model extends Game_File {

    //他のファイルを見つけるために使用されます
    private final String modelPath;
    private final SqPack_IndexFile currentIndex;

    //モデル情報
    private final IMC_File imcFile;
    private final DX9VertexElement[][] vertexElements;
    private final String[] stringArray;
    private final short AttributeCount;
    private final short BoneCount;
    private final short MaterialCount;
    private short numShpStrings;
    private final MeshPart[] meshPartTable;

    private final Material[] materials;
    private final LoDSubModel[] lodModels = new LoDSubModel[3];
    private final BoneList[] boneLists;

    private ByteBuffer boneMatrixBuffer;
    private int numBones = -1;

    private final long[] atrMasks;

    private final String[] boneStrings;

    private PAP_File animFile;

    private SimpleShader simpleShader;

    private boolean isVRAMLoaded = false;

    //別のアーカイブの資料が必要な場合
    private SqPack_IndexFile bgCommonIndex;

    private int imcPartsKey = 0;
    private int currentVariant = 1;

    public Model(byte[] data, ByteOrder endian) {
        this(null, null, data, endian);
    }

    public Model(String modelPath, SqPack_IndexFile index, byte[] data, ByteOrder endian) throws BufferOverflowException, BufferUnderflowException {
        super(endian);

        this.modelPath = modelPath;
        this.currentIndex = index;

        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);

        ModelFileHeader header = ModelFileHeader.read(bb);

        int numTotalMeshes = header.vertexDeclarationNum;

        //Count DirectX Vertex Elements
        vertexElements = new DX9VertexElement[numTotalMeshes][];
        for (int i = 0; i < numTotalMeshes; i++) {
            bb.position(0x44 + (0x88 * i));
            int count = 0;
            while (true) {
                byte stream = bb.get();
                if (stream == -1) {
                    break;
                }
                count++;
                bb.position(bb.position() + 7);
            }
            vertexElements[i] = new DX9VertexElement[count];
        }
        //DirectX頂点要素をロードする
        for (int i = 0; i < numTotalMeshes; i++) {
            bb.position(0x44 + (0x88 * i));
            for (int j = 0; j < vertexElements[i].length; j++) {
                vertexElements[i][j] = new DX9VertexElement(bb.get(), bb.get(), bb.get(), bb.get());
                bb.position(bb.position() + 0x4);
            }
        }
        bb.position(0x44 + (0x88 * numTotalMeshes));

        //Strings
        short numStrings = bb.getShort();
        bb.getShort(); //padding
        int stringBlockSize = bb.getInt();

        stringArray = new String[numStrings];
        byte[] stringBuffer = new byte[stringBlockSize];
        bb.get(stringBuffer);

        int stringCounter = 0;
        int start = 0, end = 0;
        for (byte b : stringBuffer) {
            if (b == 0) {
                if (stringCounter >= numStrings) {
                    break;
                }
                stringArray[stringCounter] = new String(stringBuffer, start, end - start);
                start = end + 1;
                stringCounter++;
            }
            end++;
        }

        //ModelDefinitionHeader
        bb.getInt(); //Unknown1
        @SuppressWarnings("unused")
        short MeshCount = bb.getShort();
        AttributeCount = bb.getShort();
        short PartCount = bb.getShort();
        MaterialCount = bb.getShort();
        BoneCount = bb.getShort();
        int UnknownStruct4Count = bb.getShort();    // 3 in hsl //numShpStrings?
        int UnknownStruct5Count = bb.getShort();    // 4 in hsl
        int UnknownStruct6Count = bb.getShort();    // 5 in hsl
        int UnknownStruct7Count = bb.getShort();    // 6 in hsl
        @SuppressWarnings("unused")
        int Unknown2 = bb.getShort();
        //From Rogueadyn's Code
        int UnknownStruct1Count = bb.getShort();    // 0 in hsl
        int UnknownStruct2Count = bb.get() & 0xFF;  // 1 in hsl
        bb.get(); //Unknown3
        bb.position(bb.position() + 10); //ushort[] Unknown4 × 5
        int UnknownStruct3Count = bb.getShort();    // 7 in hsl
        bb.position(bb.position() + 16); //ushort[] Unknown5 × 8

        meshPartTable = new MeshPart[PartCount];
        boneLists = new BoneList[UnknownStruct4Count];
        atrMasks = new long[AttributeCount];
        String[] atrStrings = new String[AttributeCount];
        boneStrings = new String[BoneCount];

        System.arraycopy(stringArray, 0, atrStrings, 0, AttributeCount);
        System.arraycopy(stringArray, AttributeCount, boneStrings, 0, BoneCount);

        for (int i = 0; i < atrStrings.length; i++) {
            atrMasks[i] = getMaskFromAtrName(atrStrings[i]);
        }

        materials = new Material[MaterialCount];

        StringBuilder s = new StringBuilder();
        for (String str : stringArray) {
            s.append(str);
            s.append("\n");
        }

        Utils.getGlobalLogger().trace("Attr strings: {}\nMaterial strings: {}\nBone strings: {}\nStrings:\n{}",
                AttributeCount, MaterialCount, BoneCount, s.toString());

        imcFile = loadImcFile();

        if (imcFile == null) {
            loadVariant(1);
        }

        //Skip Stuff
        bb.position(bb.position() + (32 * UnknownStruct1Count));

        //LOD Headers
        Utils.getGlobalLogger().trace("-----Level of Detail(LoD) Header Info-----");
        for (int i = 0; i < lodModels.length; i++) {
            //ModelQuality  High: i=0, Medium: i=1, Low: i=2
            Utils.getGlobalLogger().trace(String.format("Level of Detail(LoD) Level %d:", i));
            lodModels[i] = LoDSubModel.loadInfo(bb); //SaintCoinachではModelHeaders = buffer.ToStructures<ModelHeader>(ModelCount, ref offset)
        }

        //Load Mesh Info
        Utils.getGlobalLogger().trace("-----Level of Detail(LoD) Mesh Info-----");

        int vertElementNumber = 0;


        Mesh[] MeshHeaders = new Mesh[MeshCount];
        for (int j = 0; j < MeshCount; j++) {
            Utils.getGlobalLogger().trace(String.format("メッシュ %d:", j));
            MeshHeaders[j] = new Mesh(bb, vertElementNumber); //SaintCoinachではMeshHeaders = buffer.ToStructures<MeshHeader>(Header.MeshCount, ref offset)
            vertElementNumber++;

        }

        int num = 0;
        for (int i = 0; i < lodModels.length; i++) {

            Utils.getGlobalLogger().trace(String.format("Level of Detail(LoD) %d:", i));

            //Mesh[] MeshHeaders = new Mesh[lodModels[i].MeshCount]
            //for (int j = 0; j < lodModels[i].MeshCount; j++)
            if (false) {
                if (i == 0 && lodModels[i].MeshCount == 0) {
                    MeshHeaders = new Mesh[lodModels[i].MeshCount];
                    for (int j = 0; j < lodModels[i].MeshCount; j++) {
                        Utils.getGlobalLogger().trace(String.format("メッシュ %d:", j));
                        MeshHeaders[j] = new Mesh(bb, vertElementNumber); //SaintCoinachではMeshHeaders = buffer.ToStructures<MeshHeader>(Header.MeshCount, ref offset)
                        vertElementNumber++;

                    }
                    lodModels[i].setMeshList(MeshHeaders);
                }
            }
            if (lodModels[i].MeshCount > 0) {

                Mesh[] MeshHeaders2 = new Mesh[lodModels[i].MeshCount];
                for (int j = 0; j < MeshHeaders2.length; j++ ){
                    MeshHeaders2[j] = MeshHeaders[num];
                    num++;
                }


                lodModels[i].setMeshList(MeshHeaders2);
            }else if(i == 0){
                num++;
            }
        }

        //New stuff added from SaintCoinach
        bb.position(bb.position() + (AttributeCount * 4));
        bb.position(bb.position() + (UnknownStruct2Count * 20)); //Skip this data

        for (int i = 0; i < PartCount; i++) {
            meshPartTable[i] = new MeshPart(bb);
        }

        for (MeshPart meshPart : meshPartTable) {
            for (int j = 0; j < atrMasks.length; j++) {
                if (((meshPart.attributes >> j) & 1) == 1) {
                    meshPart.attributeMasks.add(atrMasks[j]);
                }
            }
        }

        bb.position(bb.position() + (UnknownStruct3Count * 12));//Skip this data

        bb.position(bb.position() + (MaterialCount * 4));
        bb.position(bb.position() + (BoneCount * 4));

        for (int i = 0; i < UnknownStruct4Count; i++) {
            boneLists[i] = new BoneList(bb);
        }

        bb.position(bb.position() + (UnknownStruct5Count * 16));//Skip this data
        bb.position(bb.position() + (UnknownStruct6Count * 12));//Skip this data
        bb.position(bb.position() + (UnknownStruct7Count * 4));//Skip this data

        int BoneIndicesSize = bb.getShort() & 0xFFFF;
        bb.getShort();
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        short[] boneIndices = new short[BoneIndicesSize / 2];
        for (int i = 0; i < boneIndices.length; i++) {
            boneIndices[i] = bb.getShort();
        }

        //Skip padding
        int paddingToSkip = bb.get() & 0xFF;
        bb.position(bb.position() + paddingToSkip);

        //Read in bounding boxes
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        BoundingBox[] BoundingBoxes = new BoundingBox[4];
        for (int i = 0; i < BoundingBoxes.length; i++) {
            BoundingBoxes[i] = new BoundingBox(bb);
        }


        //メッシュ情報からメッシュをロードします
        //SaintCoinachではpublic Model GetModel(ModelQuality quality)
        for (LoDSubModel lodModel : lodModels) {
            lodModel.loadMeshes(bb);
        }

        if (modelPath != null) {
            String[] modelPathSplit = modelPath.split("/");
            if (modelPathSplit[1].equals("monster")) {
                imcPartsKey = 0;
            } else if (modelPathSplit[1].equals("equipment")) {
                if (modelPath.endsWith("_top.mdl")) {
                    imcPartsKey = 1;
                } else if (modelPath.endsWith("_glv.mdl")) {
                    imcPartsKey = 2;
                } else if (modelPath.endsWith("_dwn.mdl")) {
                    imcPartsKey = 3;
                } else if (modelPath.endsWith("_sho.mdl")) {
                    imcPartsKey = 4;
                } else if (modelPath.endsWith("_ear.mdl")) {
                    imcPartsKey = 0;
                } else if (modelPath.endsWith("_nek.mdl")) {
                    imcPartsKey = 1;
                } else if (modelPath.endsWith("_wrs.mdl")) {
                    imcPartsKey = 2;
                } else if (modelPath.endsWith("_rir.mdl")) {
                    imcPartsKey = 3;
                } else if (modelPath.endsWith("_ril.mdl")) {
                    imcPartsKey = 4;
                }
            }
        }

        //Skeletons and Animations
        if (!Constants.HAVOK_ENABLED) {
            numBones = -1;
            return;
        }

        if (modelPath != null) {
            String[] modelPathSplit = modelPath.split("/");
            String skeletonPath = null, animationPath = null;

            switch (modelPathSplit[1]) {
                case "monster":
                    skeletonPath = String.format("chara/monster/%s/skeleton/base/b0001/skl_%sb0001.sklb", modelPathSplit[2], modelPathSplit[2]);
                    animationPath = String.format("chara/monster/%s/animation/a0001/bt_common/resident/monster.pap", modelPathSplit[2]);
                    break;
                case "human":
                    skeletonPath = String.format("chara/human/%s/skeleton/%s/%s/skl_%s%s.sklb", modelPathSplit[2], modelPathSplit[4], modelPathSplit[5], modelPathSplit[2], modelPathSplit[5]);
                    animationPath = String.format("chara/human/%s/animation/%s/resident/face.pap", modelPathSplit[2], modelPathSplit[5]);
                    break;
                case "equipment":
                    skeletonPath = "chara/human/c0101/skeleton/base/b0001/skl_c0101b0001.sklb";
                    animationPath = "chara/human/c0101/animation/a0001/bt_2ax_emp/ws/bt_2ax_emp/ws_s03.pap";
                    //animationPath = "chara/human/c1101/animation/a0001/bt_common/emote/panic.pap";
                    break;
            }

            if (skeletonPath == null || animationPath == null) {
                skeletonPath = "!/!";
                animationPath = "!/!";
            }


            //Skeleton/Animation
            SKLB_File skeletonFile = null;
            try {
                byte[] sklbData = currentIndex.extractFile(skeletonPath);
                if (sklbData != null) {
                    skeletonFile = new SKLB_File(sklbData, endian);
                }
            } catch (IOException e) {
                Utils.getGlobalLogger().error("Couldn't find skeleton @ {}", skeletonPath, e);
            }

            animFile = null;
            try {
                byte[] animData = currentIndex.extractFile(animationPath);
                if (animData != null) {
                    animFile = new PAP_File(animData, endian);
                }
            } catch (IOException e) {
                Utils.getGlobalLogger().error("Couldn't find animation @ {}", animationPath, e);
            }

            if (animFile != null && skeletonFile != null) {
                ByteBuffer skeletonBuffer = ByteBuffer.allocateDirect(skeletonFile.getHavokData().length);
                skeletonBuffer.order(ByteOrder.nativeOrder());
                skeletonBuffer.put(skeletonFile.getHavokData());
                ByteBuffer animBuffer = ByteBuffer.allocateDirect(animFile.getHavokData().length);
                skeletonBuffer.order(ByteOrder.nativeOrder());
                animBuffer.put(animFile.getHavokData());
                skeletonBuffer.position(0);
                animBuffer.position(0);

                //Havokが機能しない場合
                try {
                    HavokNative.startHavok();
                } catch (UnsatisfiedLinkError e) {
                    numBones = -1;
                    Utils.getGlobalLogger().error(e);
                    return;
                }

                if (HavokNative.loadSkeleton(skeletonBuffer, skeletonFile.getHavokData().length) && (HavokNative.loadAnimation(animBuffer, animFile.getHavokData().length))) {
                    if (HavokNative.setAnimation(0) == -1) {
                        HavokNative.setAnimation(0);
                        Utils.getGlobalLogger().info("Animation 0 was invalid");
                    }
                    numBones = boneStrings.length;
                    Utils.getGlobalLogger().info("Found {} bones", numBones);
                    boneMatrixBuffer = ByteBuffer.allocateDirect(4 * 16 * numBones);
                    boneMatrixBuffer.order(ByteOrder.nativeOrder());
                } else {
                    numBones = -1;
                    HavokNative.endHavok();
                }
            } else {
                numBones = -1;
                try {
                    HavokNative.endHavok();
                } catch (UnsatisfiedLinkError ignored) {

                }
            }
        }
    }

    private IMC_File loadImcFile() {
        if (modelPath == null || modelPath.contains("null") || !modelPath.contains("chara")) {
            return null;
        }

        String imcFolderPath = String.format("%s", modelPath.substring(0, modelPath.indexOf("model") - 1));
        String fileString = imcFolderPath.substring(imcFolderPath.lastIndexOf("/") + 1) + ".imc";
        String imcPath = imcFolderPath + "/" + fileString;
        try {
            byte[] data = currentIndex.extractFile(imcPath);

            if (data == null) {
                return null;
            }
            //imcファイルの登録
            HashDatabase.addPathToDB(imcPath, currentIndex.getName());

            return new IMC_File(data, endian);
        } catch (IOException e) {
            Utils.getGlobalLogger().error("IMCファイル{}を作成できませんでした", imcPath, e);
        }

        return null;

    }

    public void loadVariant(int variantNumber) {
        currentVariant = variantNumber - 1;

        int materialNumber = -1;
        if (imcFile != null) {
            materialNumber = imcFile.parts.get(imcPartsKey).variants.get(currentVariant).materialNumber;
        }

        if (modelPath == null || modelPath.contains("null") || (!modelPath.contains("chara") && !modelPath.contains("bg"))) {
            return;
        }

        @SuppressWarnings("unused")
        String[] split = modelPath.split("/");

        String materialFolderPath;

        if (!stringArray[AttributeCount + BoneCount].startsWith("/") && !stringArray[AttributeCount + BoneCount].contains("chara")) {
            materialFolderPath = stringArray[AttributeCount + BoneCount].substring(0, stringArray[AttributeCount + BoneCount].lastIndexOf("/"));
        } else if (modelPath.contains("face")) {
            materialFolderPath = String.format("%smaterial", modelPath.substring(0, modelPath.indexOf("model")));
        } else if (modelPath.contains("hair")) {
            materialFolderPath = String.format("%smaterial/v%04d", modelPath.substring(0, modelPath.indexOf("model")), 1);
        } else// if (imcFile != null && imcFile.getVarianceInfo(variantNumber-1) != null)
        {
            materialFolderPath = String.format("%smaterial/v%04d", modelPath.substring(0, modelPath.indexOf("model")), materialNumber);
        }
        //else
        //	materialFolderPath = String.format("%smaterial/v%04d", modelPath.substring(0, modelPath.indexOf("model")), 1);

        //imcFile.getVarianceInfo(variantNumber-1).materialNumber

        //HACK HERE
        int bodyMaterialSpot = -1;

        if (materialFolderPath.contains("body") && materialFolderPath.contains("human")) {
            bodyMaterialSpot = 0;
        } else {
            for (int i = 0; i < MaterialCount; i++) {
                String fileString;

                try {
                    if (stringArray[AttributeCount + BoneCount + i].startsWith("/")) {
                        fileString = stringArray[AttributeCount + BoneCount + i].substring(1);
                    } else {
                        fileString = stringArray[AttributeCount + BoneCount + i].substring(stringArray[AttributeCount + BoneCount + i].lastIndexOf("/") + 1);
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    //Ion pls
                    Utils.getGlobalLogger().error("materialの数が間違っています!", e);
                    break;
                }

                //HACK HERE
                if (fileString.matches(Utils.getRegexpFromFormatString("mt_c%04db%04d_%s.mtrl"))) {
                    bodyMaterialSpot = i;
                    continue;
                }

                //参照先ボディモデルのパスを修正
                if (materialFolderPath.contains("/obj/body/b")) {
                    if (fileString.contains("_")) {
                        String[] splitFileString = fileString.split("_");
                        String weaponIdString = splitFileString[1].substring(0, 5);
                        String weaponBodyString = splitFileString[1].substring(5);
                        String[] splitString = materialFolderPath.split("/");
                        materialFolderPath = splitString[0] + "/" + splitString[1] + "/" + weaponIdString + "/" + splitString[3] + "/" + splitString[4] + "/" + weaponBodyString + "/" + splitString[6] + "/" + splitString[7];
                    }else{
                        String[] splitString = materialFolderPath.split("/");
                        fileString = String.format("mt_%s%s_a.mtrl", splitString[2], splitString[5]);
                    }
                }

                try {
                    SqPack_IndexFile indexToUse = currentIndex;
                    byte[] materialData = currentIndex.extractFile(materialFolderPath, fileString);

                    //見つからない場合は、他のアーカイブを確認してください
                    if (materialData == null) {
                        //bgcommonが必要な場合は、開きます
                        if (materialFolderPath.startsWith("bgcommon")) {
                            if (bgCommonIndex == null) {
                                String path = currentIndex.getPath();
                                if (path.lastIndexOf("/") != -1) {
                                    path = path.substring(0, path.lastIndexOf("/sqpack"));
                                } else {
                                    path = path.substring(0, path.lastIndexOf("\\sqpack"));
                                }
                                path += "/sqpack/ffxiv/010000.win32.index";
                                bgCommonIndex = new SqPack_IndexFile(path, true);
                            }

                            materialData = bgCommonIndex.extractFile(materialFolderPath, fileString);
                            indexToUse = bgCommonIndex;
                        }
                    }

                    if (materialData != null) {
                        //Materialの初期化とtexファイルの登録をここで実施
                        materials[i] = new Material(materialFolderPath, indexToUse, materialData, endian);
                        //mtrlファイルの登録
                        HashDatabase.addPathToDB(materialFolderPath + "/" + fileString, indexToUse.getName());
                    }
                } catch (IOException e) {
                    Utils.getGlobalLogger().error(e);
                }

            }

        }

        //body/b..../materialがあったら、ここでハッシュDBに登録
        if (bodyMaterialSpot != -1) {
            //chara/humanのみの処理？
            String s = stringArray[AttributeCount + BoneCount + bodyMaterialSpot].substring(1);
            String s1 = s.replace("mt_c", "").substring(0, 9);
            int chara = Integer.parseInt(s1.substring(0, 4));
            int body = Integer.parseInt(s1.substring(5, 9));
            materialFolderPath = String.format("chara/human/c%04d/obj/body/b%04d/material", chara, body);

            HashDatabase.addPathToDB(materialFolderPath, "040000");

            try {
                //Materialの初期化とtexファイルの登録をここで実施
                materials[bodyMaterialSpot] = new Material(materialFolderPath, currentIndex, currentIndex.extractFile(materialFolderPath, s), getEndian());
            } catch (IOException e) {
                Utils.getGlobalLogger().error(e);
            }

        }
    }

    public Mesh[] getMeshes(int lodLevel) {
        return lodModels[lodLevel].meshList;
    }

    @SuppressWarnings("unused")
    public int getNumLOD0Meshes(int lodLevel) {
        return lodModels[lodLevel].meshList.length;
    }

    @SuppressWarnings("unused")
    public int getLodLevels() {
        return lodModels.length;
    }

    private Material getMaterial(int index) {
        return index < materials.length ? materials[index] : null;
    }

    public int getNumMesh(int lodLevel) {
        return lodModels[lodLevel].MeshCount;
    }

    private int getNumMaterials() {
        return modelPath == null || modelPath.contains("null") ? 0 : materials.length;
    }

    public int getNumVariants() {
        if (imcFile == null) {
            return -1;
        } else {
            return imcFile.getNumVariances();
        }
    }

    @SuppressWarnings("unused")
    public ArrayList<VarianceInfo> getVariantsList() {
        if (imcFile == null) {
            return null;
        } else {
            return imcFile.getVariantsList(imcPartsKey);
        }
    }

    public void render(DefaultShader defaultShader, float[] viewMatrix, float[] modelMatrix,
                       float[] projMatrix, GL3bc gl, int currentLoD, boolean isGlow) {

        if (simpleShader == null) {
            try {
                simpleShader = new SimpleShader(gl);
            } catch (IOException e1) {
                Utils.getGlobalLogger().error(e1);
            }
        }


        for (int i = 0; i < getNumMesh(currentLoD); i++) {

            Mesh mesh = getMeshes(currentLoD)[i];
            Material material = getMaterial(mesh.MaterialIndex);
            Shader shader = material == null || !material.isShaderReady() ? defaultShader : material.getShader();

            if (numBones != -1) {
                boneMatrixBuffer.position(0);
                HavokNative.getBonesWithNames(boneMatrixBuffer, boneStrings, boneLists[mesh.BoneListIndex].boneList, boneLists[mesh.BoneListIndex].boneCount);
            }

            gl.glUseProgram(shader.getShaderProgramID());

            if (numBones != -1) {
                boneMatrixBuffer.position(0);
            }

            for (int partNum = 0; partNum < (mesh.PartCount == 0 ? 1 : mesh.PartCount); partNum++) {
                if (mesh.PartCount != 0 && atrMasks.length != 0) {
                    long fullMask = 0;
                    for (int m = 0; m < meshPartTable[partNum + mesh.PartOffset].attributeMasks.size(); m++) {
                        fullMask |= meshPartTable[partNum + mesh.PartOffset].attributeMasks.get(m);
                        //(meshPartTable[mesh.partOffset+partNum].attributes << m);
                    }

                    if (imcFile == null) {
                        if ((0x3FF & fullMask) != fullMask) {
                            continue;
                        }
                    } else {
                        if ((imcFile.getVarianceInfo(currentVariant).partVisibiltyMask & fullMask) != fullMask) {
                            continue;
                        }
                    }
                }

                for (int e = 0; e < vertexElements[mesh.getVertexElementIndex()].length; e++) {
                    DX9VertexElement element = vertexElements[mesh.getVertexElementIndex()][e];

                    int components = GLHelper.getComponents(element.datatype);
                    int datatype = GLHelper.getDatatype(element.datatype);
                    boolean isNormalized = GLHelper.isNormalized(element.datatype);

                    //バッファのオフセットとサイズを設定します
                    ByteBuffer origin = mesh.vertBuffers[element.stream].duplicate();
                    origin.position(element.offset);
                    int size = mesh.BytesPerVertexPerBuffer[element.stream];

                    //ポインタのセット
                    switch (element.usage) {
                        case 0://位置
                            gl.glVertexAttribPointer(shader.getAttribPosition(), components, datatype, isNormalized, size, origin);
                            break;
                        case 1://Blending Weight
                            gl.glVertexAttribIPointer(shader.getAttribBlendWeight(), components, datatype, size, origin);
                            break;
                        case 2://Blend Indices
                            gl.glVertexAttribIPointer(shader.getAttribBlendIndex(), components, datatype, size, origin);
                            break;
                        case 3://Normal
                            gl.glVertexAttribPointer(shader.getAttribNormal(), components, datatype, isNormalized, size, origin);
                            break;
                        case 4://Texture Coordinates
                            gl.glVertexAttribPointer(shader.getAttribTexCoord(), components, datatype, isNormalized, size, origin);
                            break;
                        case 6://Tangent
                            gl.glVertexAttribPointer(shader.getAttribTangent(), components, datatype, isNormalized, size, origin);
                            break;
                        case 7://色
                            gl.glVertexAttribPointer(shader.getAttribColor(), components, datatype, isNormalized, size, origin);
                            break;
                    }
                }

                shader.setTextures(gl, material);
                shader.setMatrix(gl, modelMatrix, viewMatrix, projMatrix);
                boolean f = shader.isGlowPass(gl, isGlow);
                if (isGlow && !f) {
                    return;
                }

                if (shader instanceof HairShader) {
                    ((HairShader) shader).setHairColor(gl, Constants.defaultHairColor, Constants.defaultHighlightColor);
                } else if (shader instanceof IrisShader) {
                    ((IrisShader) shader).setEyeColor(gl, Constants.defaultEyeColor);
                }

                //Upload Bone Matrix
                if (numBones != -1) {
                    shader.setBoneMatrix(gl, numBones, boneMatrixBuffer);
                }

                int indBufPos;
                int numIndex;

                if (mesh.PartCount != 0) {
                    indBufPos = (meshPartTable[mesh.PartOffset + partNum].indexOffset * 2) - (mesh.IndexBufferOffset * 2);
                    numIndex = meshPartTable[mesh.PartOffset + partNum].indexCount;
                } else {
                    indBufPos = 0;
                    numIndex = mesh.IndexCount;
                }

                Utils.getGlobalLogger().trace("GL Error: {}", gl.glGetError());

                Utils.getGlobalLogger().trace("Vert buffer 1 info:");

                Utils.getGlobalLogger().trace("Drawing index buffer, pos {} num {}", indBufPos, numIndex);

                //Draw
                mesh.indexBuffer.position(indBufPos);
                shader.enableAttribs(gl);

                gl.glDrawElements(GL3.GL_TRIANGLES, numIndex, GL3.GL_UNSIGNED_SHORT, mesh.indexBuffer);
                shader.disableAttribs(gl);
            }

            //Draw Skeleton
		    gl.glDisable(GL3.GL_DEPTH_TEST);
		    if (simpleShader != null && numBones != -1){
			    gl.glPointSize(5f);
		    	simpleShader.enableAttribs(gl);
		    	boneMatrixBuffer.position(4*12);
			    gl.glUseProgram(simpleShader.getShaderProgramID());
			    simpleShader.setMatrix(gl, modelMatrix, viewMatrix, projMatrix);
			    gl.glVertexAttribPointer(simpleShader.getAttribPosition(), 3, GL3.GL_FLOAT, false, 16 * 4, boneMatrixBuffer);
			    gl.glDrawArrays(GL3.GL_POINTS, 0, HavokNative.getNumBones()-1);
			    simpleShader.disableAttribs(gl);	
		    }
		    gl.glEnable(GL3.GL_DEPTH_TEST);
        }

        //Advance Animation
        if (numBones != -1) {
            HavokNative.debugRenderBones();
        }
    }

    @SuppressWarnings("unused")
    private void debugBuffer(ByteBuffer buffer) {
        int pos = buffer.position();
        if (pos == buffer.limit()) {
            buffer.position(0);
        }
        System.out.println(buffer);
        System.out.print("first 4: ");
        for (int i = 0; i < 4; i++) {
            System.out.printf("%d ", buffer.get());
        }

        buffer.position(buffer.limit() - 30);

        System.out.print("\nlast 30: ");
        for (int i = 0; i < 30; i++) {
            System.out.printf("%d ", buffer.get());
        }
        System.out.println();
        buffer.position(pos);
    }

    private double currentTime = (double) System.currentTimeMillis() / 1000.0f;

    public void stepAnimation() {
        float timeStep = 1.0f / 300.0f;
        if (numBones != -1) {
            double newTime = (double) System.currentTimeMillis() / 1000.0f;

            while ((double) System.currentTimeMillis() / 1000.0f < newTime + timeStep) {
                Utils.getGlobalLogger().debug("無限ループ検出用:「Model.javaファイル」public void stepAnimation");
            }
            newTime += timeStep;

            HavokNative.stepAnimation((float) (newTime - currentTime));
            currentTime = newTime;
        }
    }

    public void loadToVRAM(GL3bc gl) {
        for (int i = 0; i < getNumMaterials(); i++) {

            if (getMaterial(i) == null) {
                break;
            }

            gl.glGenTextures(5, Objects.requireNonNull(getMaterial(i)).getGLTextureIds(), 0);
            Material m = getMaterial(i);

            for (int j = 0; j < 5; j++) {

                Texture_File tex = null;
                boolean isBytes = false;
                byte[] byteData = null;

                switch (j) {
                    case 0:
                        tex = Objects.requireNonNull(m).getDiffuseMapTexture();
                        break;
                    case 1:
                        tex = m.getNormalMapTexture();
                        break;
                    case 2:
                        tex = m.getSpecularMapTexture();
                        break;
                    case 3:
                        tex = m.getColorSetTexture();
                        if (tex == null) {
                            isBytes = true;
                            byteData = m.getColorSetData();
                        }
                        break;
                    case 4:
                        tex = m.getMaskTexture();
                        break;
                }

                if (tex == null && byteData == null) {
                    continue;
                }

                //Load into VRAM
                gl.glBindTexture(GL3.GL_TEXTURE_2D, m.getGLTextureIds()[j]);
                gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_S, GL3.GL_REPEAT);
                gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_T, GL3.GL_REPEAT);
                gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER, j == 1 ? GL3.GL_NEAREST : GL3.GL_LINEAR);
                gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER, j == 1 ? GL3.GL_NEAREST : GL3.GL_LINEAR);
				
				//Anisotropic Filtering
				float[] ansIO = new float[1];
				gl.glGetFloatv(GL3.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, ansIO,0);
				gl.glTexParameterf(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAX_ANISOTROPY_EXT, ansIO[0]);

                if (isBytes) {
                    ByteBuffer colorTable = Buffers.newDirectByteBuffer(byteData);
                    colorTable.position(0);
                    colorTable.order(endian);
                    gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGBA, 4, 16, 0, GL3.GL_RGBA, GL3.GL_HALF_FLOAT, colorTable);
                } else {
                    ByteBuffer dxtBB = Buffers.newDirectByteBuffer(tex.data);
                    dxtBB.position(tex.mipmapOffsets[0]);
                    dxtBB.order(endian);

                    switch (tex.compressionType) {
                        case 0x3420:
                            gl.glCompressedTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, tex.uncompressedWidth, tex.uncompressedHeight, 0, tex.mipmapOffsets[1] - tex.mipmapOffsets[0], dxtBB);
                            break;
                        case 0x3430:
                            gl.glCompressedTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, tex.uncompressedWidth, tex.uncompressedHeight, 0, tex.mipmapOffsets[1] - tex.mipmapOffsets[0], dxtBB);
                            break;
                        case 0x3431:
                            gl.glCompressedTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT, tex.uncompressedWidth, tex.uncompressedHeight, 0, tex.mipmapOffsets[1] - tex.mipmapOffsets[0], dxtBB);
                            break;
                        case 0x2460:
                            gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGBA, tex.uncompressedWidth, tex.uncompressedHeight, 0, GL3.GL_RGBA, GL3.GL_HALF_FLOAT, dxtBB);
                            break;
                        default:
                            BufferedImage img = null;
                            try {
                                img = tex.decode(0, null);
                            } catch (ImageDecodingException e) {
                                Utils.getGlobalLogger().error("テクスチャをデコードできませんでした!", e);
                            }

                            int[] pixels = new int[Objects.requireNonNull(img).getWidth() * img.getHeight()];
                            img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());

                            ByteBuffer buffer = Buffers.newDirectByteBuffer(img.getWidth() * img.getHeight() * 4);

                            //Java専用処理
                            for (int y = 0; y < img.getHeight(); y++) {
                                for (int x = 0; x < img.getWidth(); x++) {
                                    int pixel = pixels[y * img.getWidth() + x];
                                    buffer.put((byte) ((pixel >> 16) & 0xFF));
                                    buffer.put((byte) ((pixel >> 8) & 0xFF));
                                    buffer.put((byte) (pixel & 0xFF));
                                    buffer.put((byte) ((pixel >> 24) & 0xFF));
                                }
                            }
                            buffer.flip(); //これを忘れないこと
                            buffer.position(0);
                            gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGBA, img.getWidth(), img.getHeight(), 0, GL3.GL_RGBA, GL3.GL_UNSIGNED_BYTE, buffer);
                    }
                }

                gl.glBindTexture(GL3.GL_TEXTURE_2D, 0);

                m.loadShader(gl);
            }
        }

        isVRAMLoaded = true;
    }

    public DX9VertexElement[] getDX9Struct(int lodLevel, int i) {
        return vertexElements[lodModels[lodLevel].meshList[i].getVertexElementIndex()];
    }

    public int getNumAnimations() {
        if (animFile == null) {
            return 0;
        } else {
            return animFile.getNumAnimations();
        }
    }

    public String getAnimationName(int index) {
        if (animFile == null) {
            return null;
        } else {
            return animFile.getAnimationName(index);
        }
    }

    public void setCurrentAnimation(int selectedIndex) {

        if (numBones == -1) {
            return;
        }

        if (HavokNative.setAnimation(selectedIndex) == -1) {
            HavokNative.setAnimation(0);
            Utils.getGlobalLogger().info("アニメーション{}が無効", selectedIndex);
        }
    }

    public void setAnimationSpeed(float speed) {
        HavokNative.setPlaybackSpeed(speed);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isVRAMLoaded() {
        return isVRAMLoaded;
    }

    public void resetVRAM() {
        isVRAMLoaded = false;
    }

    public void unload(GL3 gl) {
        for (Material material : materials) {
            gl.glDeleteTextures(5, material.textureIds, 0);
        }
    }

    public int getNumAnimationFrames(int animationNumber) {
        if (numBones != -1) {
            return HavokNative.getNumAnimationFrames(animationNumber);
        } else {
            return -1;
        }
    }

    private long getMaskFromAtrName(String attribute) {
        if ("atr_tv_a".equals(attribute)) {
            return 1;
        } else if ("atr_tv_b".equals(attribute)) {
            return 1 << 1;
        } else if ("atr_tv_c".equals(attribute)) {
            return 1 << 2;
        } else if ("atr_tv_d".equals(attribute)) {
            return 1 << 3;
        } else if ("atr_tv_e".equals(attribute)) {
            return 1 << 4;
        } else if ("atr_tv_f".equals(attribute)) {
            return 1 << 5;
        } else if ("atr_tv_g".equals(attribute)) {
            return 1 << 6;
        } else if ("atr_tv_h".equals(attribute)) {
            return 1 << 7;
        } else if ("atr_tv_i".equals(attribute)) {
            return 1 << 8;
        } else if ("atr_tv_j".equals(attribute)) {
            return 1 << 9;
        } else if ("atr_mv_a".equals(attribute)) {
            return 1;
        } else if ("atr_mv_b".equals(attribute)) {
            return 1 << 1;
        } else if ("atr_mv_c".equals(attribute)) {
            return 1 << 2;
        } else if ("atr_mv_d".equals(attribute)) {
            return 1 << 3;
        } else if ("atr_mv_e".equals(attribute)) {
            return 1 << 4;
        } else if ("atr_mv_f".equals(attribute)) {
            return 1 << 5;
        } else if ("atr_mv_g".equals(attribute)) {
            return 1 << 6;
        } else if ("atr_mv_h".equals(attribute)) {
            return 1 << 7;
        } else if ("atr_mv_i".equals(attribute)) {
            return 1 << 8;
        } else if ("atr_mv_j".equals(attribute)) {
            return 1 << 9;
        } else if ("atr_bv_a".equals(attribute)) {
            return 1;
        } else if ("atr_bv_b".equals(attribute)) {
            return 1 << 1;
        } else if ("atr_bv_c".equals(attribute)) {
            return 1 << 2;
        } else if ("atr_bv_d".equals(attribute)) {
            return 1 << 3;
        } else if ("atr_bv_e".equals(attribute)) {
            return 1 << 4;
        } else if ("atr_bv_f".equals(attribute)) {
            return 1 << 5;
        } else if ("atr_bv_g".equals(attribute)) {
            return 1 << 6;
        } else if ("atr_bv_h".equals(attribute)) {
            return 1 << 7;
        } else if ("atr_bv_i".equals(attribute)) {
            return 1 << 8;
        } else if ("atr_bv_j".equals(attribute)) {
            return 1 << 9;
        } else if ("atr_gv_a".equals(attribute)) {
            return 1;
        } else if ("atr_gv_b".equals(attribute)) {
            return 1 << 1;
        } else if ("atr_gv_c".equals(attribute)) {
            return 1 << 2;
        } else if ("atr_gv_d".equals(attribute)) {
            return 1 << 3;
        } else if ("atr_gv_e".equals(attribute)) {
            return 1 << 4;
        } else if ("atr_gv_f".equals(attribute)) {
            return 1 << 5;
        } else if ("atr_gv_g".equals(attribute)) {
            return 1 << 6;
        } else if ("atr_gv_h".equals(attribute)) {
            return 1 << 7;
        } else if ("atr_gv_i".equals(attribute)) {
            return 1 << 8;
        } else if ("atr_gv_j".equals(attribute)) {
            return 1 << 9;
        } else if ("atr_dv_a".equals(attribute)) {
            return 1;
        } else if ("atr_dv_b".equals(attribute)) {
            return 1 << 1;
        } else if ("atr_dv_c".equals(attribute)) {
            return 1 << 2;
        } else if ("atr_dv_d".equals(attribute)) {
            return 1 << 3;
        } else if ("atr_dv_e".equals(attribute)) {
            return 1 << 4;
        } else if ("atr_dv_f".equals(attribute)) {
            return 1 << 5;
        } else if ("atr_dv_g".equals(attribute)) {
            return 1 << 6;
        } else if ("atr_dv_h".equals(attribute)) {
            return 1 << 7;
        } else if ("atr_dv_i".equals(attribute)) {
            return 1 << 8;
        } else if ("atr_dv_j".equals(attribute)) {
            return 1 << 9;
        } else if ("atr_sv_a".equals(attribute)) {
            return 1;
        } else if ("atr_sv_b".equals(attribute)) {
            return 1 << 1;
        } else if ("atr_sv_c".equals(attribute)) {
            return 1 << 2;
        } else if ("atr_sv_d".equals(attribute)) {
            return 1 << 3;
        } else if ("atr_sv_e".equals(attribute)) {
            return 1 << 4;
        } else if ("atr_sv_f".equals(attribute)) {
            return 1 << 5;
        } else if ("atr_sv_g".equals(attribute)) {
            return 1 << 6;
        } else if ("atr_sv_h".equals(attribute)) {
            return 1 << 7;
        } else if ("atr_sv_i".equals(attribute)) {
            return 1 << 8;
        } else if ("atr_sv_j".equals(attribute)) {
            return 1 << 9;
        } else if ("atr_fv_a".equals(attribute)) {
            return 1;
        } else if ("atr_fv_b".equals(attribute)) {
            return 1 << 1;
        } else if ("atr_fv_c".equals(attribute)) {
            return 1 << 2;
        } else if ("atr_fv_d".equals(attribute)) {
            return 1 << 3;
        } else if ("atr_fv_e".equals(attribute)) {
            return 1 << 4;
        } else if ("atr_fv_f".equals(attribute)) {
            return 1 << 5;
        } else if ("atr_fv_g".equals(attribute)) {
            return 1 << 6;
        } else if ("atr_fv_h".equals(attribute)) {
            return 1 << 7;
        } else if ("atr_fv_i".equals(attribute)) {
            return 1 << 8;
        } else if ("atr_fv_j".equals(attribute)) {
            return 1 << 9;
        } else {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public short getNumShpStrings() {
        return numShpStrings;
    }

    @SuppressWarnings("unused")
    public void setNumShpStrings(short numShpStrings) {
        this.numShpStrings = numShpStrings;
    }

    @SuppressWarnings("unused")
    public boolean isMarkedForDelete() {
        return false;
    }
}
