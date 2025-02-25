package com.fragmenterworks.ffxivextract.shaders;

import com.fragmenterworks.ffxivextract.models.Material;
import com.jogamp.opengl.GL3;

import java.io.IOException;

public class IrisShader extends Shader {

    private final int usesCatchLightTexLocation;
    private final int eyeCatchTexLocation;
    private final int eyeColorLocation;

    public IrisShader(GL3 gl) throws IOException {
        //super(gl, "/res/shaders/model_vert.glsl", "/res/shaders/iris_frag.glsl", true);
        super(gl, MinifiedShaders.model_vert_glsl, MinifiedShaders.iris_frag_glsl, false);

        usesCatchLightTexLocation = gl.glGetUniformLocation(shaderProgram, "uHasCatchLight");
        eyeCatchTexLocation = gl.glGetUniformLocation(shaderProgram, "uCatchLightTex");
        eyeColorLocation = gl.glGetUniformLocation(shaderProgram, "uEyeColor");
    }

    @SuppressWarnings("unused")
    public void setEyeCatchTexture(GL3 gl, Material mat) {
        gl.glUniform1i(usesCatchLightTexLocation, 1);
        gl.glUniform1i(eyeCatchTexLocation, getNextSamplerId());
        gl.glActiveTexture(GL3.GL_TEXTURE0 + getNextSamplerId());
        gl.glBindTexture(GL3.GL_TEXTURE_2D, mat.getGLTextureIds()[0]);
    }

    public void setEyeColor(GL3 gl, float[] eyeColor) {
        gl.glUniform4fv(eyeColorLocation, 1, eyeColor, 0);
    }

}
