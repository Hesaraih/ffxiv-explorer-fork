package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.models.directx.D3DXShader_ConstantTable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SHCD_File extends Game_File {

    private final static int SHADERTYPE_VERTEX = 0;
    @SuppressWarnings("unused")
    public final static int SHADERTYPE_PIXEL = 1;

    private int shaderType;
    private ShaderHeader shaderHeader;

    private byte[] shaderBytecode;

    private D3DXShader_ConstantTable constantTable;

    @SuppressWarnings("unused")
    public SHCD_File(String path, ByteOrder endian) throws IOException {
        super(endian);
        File file = new File(path);
        byte[] data;
        try (FileInputStream fis = new FileInputStream(file)) {
            data = new byte[(int) file.length()];
            while (fis.read(data) != -1) {
                Utils.getGlobalLogger().debug("SHCD読み取り");
            }
        }
        loadSHPK(data);
    }

    public SHCD_File(byte[] data, ByteOrder endian) throws IOException {
        super(endian);
        loadSHPK(data);
    }

    private void loadSHPK(byte[] data) {

        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);

        int magic = bb.getInt();

        //Check Signatures
        if (magic != 0x64436853) {
            Utils.getGlobalLogger().error("SHCD magic was incorrect.");
            Utils.getGlobalLogger().debug("Magic was {}", String.format("0x%08X", magic));
            return;
        }

        //Read in header
        bb.getShort();
        bb.get();
        shaderType = bb.get();

        byte[] dxStringBuffer = new byte[4];
        bb.get(dxStringBuffer);
        String directXVersion = new String(dxStringBuffer);

        @SuppressWarnings("unused")
        int fileLength = bb.getInt();
        int shaderStartBytecodeOffset = bb.getInt();
        int shaderStringBlockOffset = bb.getInt();

        //Read in shader header
        shaderHeader = new ShaderHeader(shaderType, bb);

        //Set the param strings
        for (int i = 0; i < shaderHeader.paramInfo.length; i++) {
            bb.position(shaderStringBlockOffset + shaderHeader.paramInfo[i].stringOffset);
            byte[] buffer = new byte[shaderHeader.paramInfo[i].stringSize];
            bb.get(buffer);
            shaderHeader.paramInfo[i].parameterName = new String(buffer);
        }

        //Read in bytecode
        bb.position(shaderStartBytecodeOffset + shaderHeader.shaderBytecodeOffset);

        //Read in ? if vertex shader
        if (shaderType == SHADERTYPE_VERTEX)
            bb.getInt();

        shaderBytecode = new byte[shaderHeader.shaderBytecodeSize - (shaderType == SHADERTYPE_VERTEX ? 4 : 0)];
        bb.get(shaderBytecode);

        //Constant Table in bytecode IF DX9
        if (directXVersion.equals("DX9\0"))
            constantTable = D3DXShader_ConstantTable.getConstantTable(shaderBytecode);

        if (constantTable != null) {
            StringBuilder s = new StringBuilder();

            for (int i = 0; i < constantTable.constantInfo.length; i++) {
                s.append(" ");
                s.append(constantTable.constantInfo[i].TypeInfo.Columns);
                s.append("x");
                s.append(constantTable.constantInfo[i].TypeInfo.Rows);
                s.append(" Index: ");
                s.append(constantTable.constantInfo[i].RegisterIndex);
                s.append(" Count: ");
                s.append(constantTable.constantInfo[i].RegisterCount);

                if (constantTable.constantInfo[i].TypeInfo.StructMembers != 0) {
                    s.append("Struct!\n");
                    for (int j = 0; j < constantTable.constantInfo[i].TypeInfo.StructMemberInfo.length; j++) {
                        s.append("  => ");
                        s.append(constantTable.constantInfo[i].TypeInfo.StructMemberInfo[j].Name);
                    }
                }
            }

            Utils.getGlobalLogger().trace("SHCD info:\nCreator: {}\nTarget: {}\nConstants: {}",
                    constantTable.Creator, constantTable.Target, s.toString());
        }
    }

    public D3DXShader_ConstantTable getConstantTable() {
        return constantTable;
    }

    public int getShaderType() {
        return shaderType;
    }

    public byte[] getShaderBytecode() {
        return shaderBytecode;
    }

    public ShaderHeader getShaderHeader() {
        return shaderHeader;
    }
}
