package com.fragmenterworks.ffxivextract.shaders;

import com.fragmenterworks.ffxivextract.gui.components.OpenGL_View;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.models.Material;
import com.jogamp.opengl.GL3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Scanner;


public class Shader {

    int shaderProgram;

    private final int modelLocation;
    private final int viewLocation;
    private final int projLocation;
    private final int numBonesLocation;
    private final int boneArrayLocation;

    private final int positionLocation;
    private final int blendWeightLocation;
    private final int blendIndexLocation;
    private final int normalLocation;
    private final int texCoordLocation;
    private final int tangentLocation;
    private final int biNormalLocation;

    private final int colorLocation;

    private final int diffuseTexLocation;
    private final int maskTexLocation;
    private final int normalTexLocation;
    private final int specularTexLocation;
    private final int colorSetTexLocation;

    private final int usesDiffuseLocation;
    private final int usesMaskLocation;
    private final int usesNormalLocation;
    private final int usesSpecularLocation;
    private final int usesColorSetLocation;

    private final int isGlowPassLocation;

    Shader(GL3 gl, String vert, String frag, boolean isPath) throws IOException {
        //Build shader
        String vsrc, fsrc;

        if (isPath) {
            vsrc = readFromStream(OpenGL_View.class
                    .getResourceAsStream(vert));

            fsrc = readFromStream(OpenGL_View.class
                    .getResourceAsStream(frag));
        } else {
            vsrc = vert;
            fsrc = frag;
        }

        int vertShader = createShader(gl, GL3.GL_VERTEX_SHADER, vsrc);
        int fragShader = createShader(gl, GL3.GL_FRAGMENT_SHADER, fsrc);

        shaderProgram = gl.glCreateProgram();
        gl.glAttachShader(shaderProgram, vertShader);
        gl.glAttachShader(shaderProgram, fragShader);
        gl.glLinkProgram(shaderProgram);


        gl.glValidateProgram(shaderProgram);

        //Set Attrib Locations
        positionLocation = gl.glGetAttribLocation(shaderProgram, "aPosition");
        blendWeightLocation = gl.glGetAttribLocation(shaderProgram, "aBlendWeight");
        blendIndexLocation = gl.glGetAttribLocation(shaderProgram, "aBlendIndex");
        normalLocation = gl.glGetAttribLocation(shaderProgram, "aNormal");
        texCoordLocation = gl.glGetAttribLocation(shaderProgram, "aTexCoord");
        tangentLocation = gl.glGetAttribLocation(shaderProgram, "aBiTangent");
        biNormalLocation = gl.glGetAttribLocation(shaderProgram, "aBiNormal");
        colorLocation = gl.glGetAttribLocation(shaderProgram, "aColor");

        //Set Uniform Locations
        modelLocation = gl.glGetUniformLocation(shaderProgram, "uModelMatrix");
        viewLocation = gl.glGetUniformLocation(shaderProgram, "uViewMatrix");
        projLocation = gl.glGetUniformLocation(shaderProgram, "uProjMatrix");
        numBonesLocation = gl.glGetUniformLocation(shaderProgram, "uNumBones");
        boneArrayLocation = gl.glGetUniformLocation(shaderProgram, "uBones");

        //Set Uniform Tex Locations
        diffuseTexLocation = gl.glGetUniformLocation(shaderProgram, "uDiffuseTex");
        maskTexLocation = gl.glGetUniformLocation(shaderProgram, "uMaskTex");
        normalTexLocation = gl.glGetUniformLocation(shaderProgram, "uNormalTex");
        specularTexLocation = gl.glGetUniformLocation(shaderProgram, "uSpecularTex");
        colorSetTexLocation = gl.glGetUniformLocation(shaderProgram, "uColorSetTex");

        usesDiffuseLocation = gl.glGetUniformLocation(shaderProgram, "uHasDiffuse");
        usesMaskLocation = gl.glGetUniformLocation(shaderProgram, "uHasMask");
        usesNormalLocation = gl.glGetUniformLocation(shaderProgram, "uHasNormal");
        usesSpecularLocation = gl.glGetUniformLocation(shaderProgram, "uHasSpecular");
        usesColorSetLocation = gl.glGetUniformLocation(shaderProgram, "uHasColorSet");

        //Other
        isGlowPassLocation = gl.glGetUniformLocation(shaderProgram, "uIsGlow");
    }

    private String readFromStream(InputStream ins) throws IOException {
        if (ins == null) {
            throw new IOException("Could not read from stream.");
        }
        StringBuilder buffer = new StringBuilder();
        try (Scanner scanner = new Scanner(ins)) {
            while (scanner.hasNextLine()) {
                buffer.append(scanner.nextLine()).append("\n");
            }
        }
        return buffer.toString();
    }

    private int createShader(GL3 gl, int shaderType, String source) {
        int shader = gl.glCreateShader(shaderType);
        gl.glShaderSource(shader, 1, new String[]{source}, null, 0);
        gl.glCompileShader(shader);

        int[] compiled = new int[1];
        gl.glGetShaderiv(shader, GL3.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            byte[] infoLog = new byte[1024];
            gl.glGetShaderInfoLog(shader, 1024, null, 0, infoLog, 0);
            Utils.getGlobalLogger().debug("Shader info: {}", new String(infoLog));
            gl.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    public void setTextures(GL3 gl, Material mat) {
        //Textures
        if (mat != null) {

            if (mat.getDiffuseMapTexture() != null)
                gl.glUniform1i(usesDiffuseLocation, 1);
            if (mat.getMaskTexture() != null)
                gl.glUniform1i(usesMaskLocation, 1);
            if (mat.getNormalMapTexture() != null)
                gl.glUniform1i(usesNormalLocation, 1);
            if (mat.getSpecularMapTexture() != null)
                gl.glUniform1i(usesSpecularLocation, 1);
            if (mat.getColorSetTexture() != null || mat.getColorSetData() != null)
                gl.glUniform1i(usesColorSetLocation, 1);

            gl.glUniform1i(diffuseTexLocation, 0);
            gl.glUniform1i(normalTexLocation, 1);
            gl.glUniform1i(specularTexLocation, 2);
            gl.glUniform1i(colorSetTexLocation, 3);
            gl.glUniform1i(maskTexLocation, 4);

            gl.glActiveTexture(GL3.GL_TEXTURE0);
            gl.glBindTexture(GL3.GL_TEXTURE_2D, mat.getGLTextureIds()[0]);
            gl.glActiveTexture(GL3.GL_TEXTURE1);
            gl.glBindTexture(GL3.GL_TEXTURE_2D, mat.getGLTextureIds()[1]);
            gl.glActiveTexture(GL3.GL_TEXTURE2);
            gl.glBindTexture(GL3.GL_TEXTURE_2D, mat.getGLTextureIds()[2]);
            gl.glActiveTexture(GL3.GL_TEXTURE3);
            gl.glBindTexture(GL3.GL_TEXTURE_2D, mat.getGLTextureIds()[3]);
            gl.glActiveTexture(GL3.GL_TEXTURE4);
            gl.glBindTexture(GL3.GL_TEXTURE_2D, mat.getGLTextureIds()[4]);
        }

    }

    public boolean isGlowPass(GL3 gl, boolean b) {
        if (isGlowPassLocation == -1)
            return false;
        gl.glUniform1i(isGlowPassLocation, b ? 1 : 0);
        return true;
    }

    public int getShaderProgramID() {
        return shaderProgram;
    }

    public void setMatrix(GL3 gl, float[] modelMatrix, float[] viewMatrix, float[] projMatrix) {
        gl.glUniformMatrix4fv(modelLocation, 1, false, modelMatrix, 0);
        gl.glUniformMatrix4fv(viewLocation, 1, false, viewMatrix, 0);
        gl.glUniformMatrix4fv(projLocation, 1, false, projMatrix, 0);
    }

    public void setBoneMatrix(GL3 gl, int numBones, ByteBuffer boneMatrixBuffer) {
        boneMatrixBuffer.position(0);
        gl.glUniform1i(numBonesLocation, numBones);
        gl.glUniformMatrix4fv(boneArrayLocation, numBones, false, boneMatrixBuffer.asFloatBuffer());
    }

    public int getAttribPosition() {
        return positionLocation;
    }

    public int getAttribBlendWeight() {
        return blendWeightLocation;
    }

    public int getAttribBlendIndex() {
        return blendIndexLocation;
    }

    public int getAttribColor() {
        return colorLocation;
    }

    public int getAttribNormal() {
        return normalLocation;
    }

    public int getAttribTangent() {
        return tangentLocation;
    }

    @SuppressWarnings("unused")
    public int getAttribBiNormal() {
        return biNormalLocation;
    }

    public int getAttribTexCoord() {
        return texCoordLocation;
    }

    public void enableAttribs(GL3 gl) {
        gl.glEnableVertexAttribArray(positionLocation);
        gl.glEnableVertexAttribArray(blendWeightLocation);
        gl.glEnableVertexAttribArray(blendIndexLocation);
        gl.glEnableVertexAttribArray(normalLocation);
        gl.glEnableVertexAttribArray(texCoordLocation);
        gl.glEnableVertexAttribArray(tangentLocation);
        gl.glEnableVertexAttribArray(biNormalLocation);
        gl.glEnableVertexAttribArray(colorLocation);
    }

    public void disableAttribs(GL3 gl) {
        gl.glDisableVertexAttribArray(positionLocation);
        gl.glDisableVertexAttribArray(blendWeightLocation);
        gl.glDisableVertexAttribArray(blendIndexLocation);
        gl.glDisableVertexAttribArray(normalLocation);
        gl.glDisableVertexAttribArray(texCoordLocation);
        gl.glDisableVertexAttribArray(tangentLocation);
        gl.glDisableVertexAttribArray(biNormalLocation);
        gl.glDisableVertexAttribArray(colorLocation);
    }

    int getNextSamplerId() {
        int numSampler = 4;
        return numSampler + 1;
    }

}
