package com.example.android.blurperfmormance;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.opengl.GLES31;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class BlurSquareTwoPasses extends BlurSquare {

    private final static float MAX_BLUR_RADIUS_DEFAULT = 15.0f;

    final private String vertexShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "in vec4 aPosition;\n" +
            "in vec2 aTexCoord;\n" +
            "out vec2 vTexCoord;\n" +
            "out float vWeight[16];\n" +
            "uniform float uMipLevel;\n" +
            "uniform float uRadius;\n" +
            "uniform float uWidth;\n" +
            "uniform float uHeight;\n" +
            "out float vWidthOffset;\n" +
            "out float vHeightOffset;\n" +

            "void calculateWeights(float aRadius)\n" +
            "{\n" +
            "    int r = int(aRadius);\n" +
            "    float sigma = uRadius / 2.0;\n" +
            "    float sumOfWeights = 0.0;\n" +
            "    for (int i = 0; i < r + 1; i++) {\n" +
            "        vWeight[i] = (1.0 / sqrt(2.0 * 3.14 * float(sigma * sigma))) * exp(-float(i * i) / (2.0 * float(sigma * sigma)));\n" +
            "        if (i == 0) {\n" +
            "            sumOfWeights += vWeight[i];\n" +
            "        } else {\n" +
            "            sumOfWeights += 2.0 * vWeight[i];\n" +
            "        }\n" +
            "    }\n" +
            "    for (int i = 0; i < r + 1; i++) {\n" +
            "        vWeight[i] = vWeight[i] / sumOfWeights;\n" +
            "    }\n" +
            "}\n" +

            "void main()\n" +
            "{\n" +
            "    vTexCoord = aTexCoord;\n" +
            "    gl_Position = aPosition;\n" +
            "    vWidthOffset = uRadius / uWidth;\n" +
            "    vHeightOffset = uRadius / uHeight;\n" +
            "    calculateWeights(" + MAX_BLUR_RADIUS_DEFAULT + ");\n" +
            "}";

    final private String horFragmentShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform float uRadius;\n" +
            "in float vWeight[16];\n" +
            "in vec2 vTexCoord;\n" +
            "in float vWidthOffset;\n" +
            "in float vHeightOffset;\n" +

            "out vec4 glFragColor;\n" +

            "void main()\n" +
            "{\n" +
            "    vec4 color = vec4(0.0);\n" +
            "    for (int i = 1; i <= int(" + MAX_BLUR_RADIUS_DEFAULT + "); i++) {\n" +
            "       color += texture(uTexture, vTexCoord + vec2(float(i) * vWidthOffset, 0.0)) * vWeight[i];\n"+
            "       color += texture(uTexture, vTexCoord - vec2(float(i) * vWidthOffset, 0.0)) * vWeight[i];\n"+
            "    }\n"+
            "    color += texture(uTexture, vTexCoord) * vWeight[0];\n" +
            "    glFragColor = color;\n" +
            "}";


    final private String verFragmentShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform float uRadius;\n" +
            "uniform float saturation;\n" +

            "in vec2 vTexCoord;\n" +
            "in float vWeight[16];" +
            "in float vWidthOffset;\n" +
            "in float vHeightOffset;\n" +

            "out vec4 glFragColor;\n" +

            "void main()\n" +
            "{\n" +
            "    vec4 color = vec4(0.0);\n" +
            "    for (int i = 1; i <= int(" + MAX_BLUR_RADIUS_DEFAULT + "); i++) {\n" +
            "       color += texture(uTexture, vTexCoord + vec2(0.0, float(i) * vHeightOffset)) * vWeight[i];\n" +
            "       color += texture(uTexture, vTexCoord - vec2(0.0, float(i) * vHeightOffset)) * vWeight[i];\n" +
            "    }\n"+
            "    color += texture(uTexture, vTexCoord) * vWeight[0];\n" +
            "    glFragColor = color;\n" +
            "}";

    public BlurSquareTwoPasses(Context context, Point size) {
        super(context, size);
    }

    @Override
    public String getVertextShaderCode() {
        return vertexShaderCode;
    }

    @Override
    public String getHorizontalFragmentShaderCode() {
        return horFragmentShaderCode;
    }

    @Override
    public String getVerticalFragmentShaderCode() {
        return verFragmentShaderCode;
    }
}
