package com.example.android.blurperfmormance;

import android.content.Context;
import android.graphics.Point;

public class BlurSquareMipmapLinearSampling extends BlurSquare {

    final private String vertexShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "in vec4 aPosition;\n" +
            "in vec2 aTexCoord;\n" +
            "out vec2 vTexCoord;\n" +
            "uniform float uMipLevel;\n" +
            "uniform float uWidth;\n" +
            "uniform float uHeight;\n" +
            "out float vMipLevel;\n" +
            "out float vWidthOffset;\n" +
            "out float vHeightOffset;\n" +

            "void main()\n" +
            "{\n" +
            "    vTexCoord = aTexCoord;\n" +
            "    gl_Position = aPosition;\n" +
            "    vWidthOffset = pow(2.0, uMipLevel) / uWidth;\n" +
            "    vHeightOffset = pow(2.0, uMipLevel) / uHeight;\n" +
            "    vMipLevel = uMipLevel;\n" +
            "}";

    final private String horFragmentShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "in float vMipLevel;\n" +
            "in vec2 vTexCoord;\n" +
            "in float vWidthOffset;\n" +
            "in float vHeightOffset;\n" +

            "out vec4 glFragColor;\n" +

            "void main()\n" +
            "{\n" +
            "    vec4 color = vec4(0.0);\n" +
            "    float weight[5] = float[]( 0.185471, 0.288322, 0.1035, 0.0147858, 0.000652313 );\n" +
            "    float offset[5] = float[]( 0.0, 1.42105, 3.31579, 5.21053, 7.10526 );\n" +
            "    for (int i = 1; i <= 4; i++) {\n" +
            "       color += texture(uTexture, vTexCoord + vec2(offset[i] * vWidthOffset, 0.0)) * weight[i];\n"+
            "       color += texture(uTexture, vTexCoord - vec2(offset[i] * vWidthOffset, 0.0)) * weight[i];\n"+
            "    }\n"+
            "    color += texture(uTexture, vTexCoord) * weight[0];\n" +
            "    glFragColor = color;\n" +
            "}";


    final private String verFragmentShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform float saturation;\n" +

            "in vec2 vTexCoord;\n" +
            "in float vMipLevel;\n" +
            "in float vWidthOffset;\n" +
            "in float vHeightOffset;\n" +

            "out vec4 glFragColor;\n" +

            "void main()\n" +
            "{\n" +
            "    vec4 color = vec4(0.0);\n" +
            "    float weight[5] = float[]( 0.185471, 0.288322, 0.1035, 0.0147858, 0.000652313 );\n" +
            "    float offset[5] = float[]( 0.0, 1.42105, 3.31579, 5.21053, 7.10526 );\n" +
            "    for (int i = 1; i <= 4; i++) {\n" +
            "       color += textureLod(uTexture, vTexCoord + vec2(0.0, offset[i] * vHeightOffset), vMipLevel) * weight[i];\n" +
            "       color += textureLod(uTexture, vTexCoord - vec2(0.0, offset[i] * vHeightOffset), vMipLevel) * weight[i];\n" +
            "    }\n"+
            "    color += textureLod(uTexture, vTexCoord, vMipLevel) * weight[0];\n" +
            "    glFragColor = color;\n" +
            "}";

    public BlurSquareMipmapLinearSampling(Context context, Point size) {
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
