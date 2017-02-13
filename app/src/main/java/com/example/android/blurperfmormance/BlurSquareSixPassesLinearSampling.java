package com.example.android.blurperfmormance;

import android.content.Context;
import android.graphics.Point;

public class BlurSquareSixPassesLinearSampling extends BlurSquare {

    final private String vertexShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "in vec4 aPosition;\n" +
            "in vec2 aTexCoord;\n" +
            "out vec2 vTexCoord;\n" +
            "uniform float uRadius;\n" +
            "uniform float uWidth;\n" +
            "uniform float uHeight;\n" +
            "out float vWidthOffset;\n" +
            "out float vHeightOffset;\n" +

            "void main()\n" +
            "{\n" +
            "    vTexCoord = aTexCoord;\n" +
            "    gl_Position = aPosition;\n" +
            "    vWidthOffset = uRadius / uWidth;\n" +
            "    vHeightOffset = uRadius / uHeight;\n" +
            "}";

    final private String horFragmentShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "in vec2 vTexCoord;\n" +
            "in float vWidthOffset;\n" +
            "in float vHeightOffset;\n" +

            "out vec4 glFragColor;\n" +

            "float rand(vec2 co){\n" +
            "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);\n" +
            "}" +

            "void main()\n" +
            "{\n" +
            "    vec4 color = vec4(0.0);\n" +
            "    float weight[4] = float[]( 0.185497, 0.288364, 0.103515, 0.0147879 );\n" +
            "    float offset[4] = float[]( 0.0, 1.42105, 3.31579, 5.21053 );\n" +
            "    for (int i = 1; i <= 3; i++) {\n" +
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

            "in vec2 vTexCoord;\n" +
            "in float vWidthOffset;\n" +
            "in float vHeightOffset;\n" +

            "out vec4 glFragColor;\n" +

            "float rand(vec2 co){\n" +
            "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);\n" +
            "}" +

            "void main()\n" +
            "{\n" +
            "    vec4 color = vec4(0.0);\n" +
            "    float weight[4] = float[]( 0.185497, 0.288364, 0.103515, 0.0147879 );\n" +
            "    float offset[4] = float[]( 0.0, 1.42105, 3.31579, 5.21053 );\n" +
            "    for (int i = 1; i <= 3; i++) {\n" +
            "       color += texture(uTexture, vTexCoord + vec2(0.0, offset[i] * vHeightOffset)) * weight[i];\n" +
            "       color += texture(uTexture, vTexCoord - vec2(0.0, offset[i] * vHeightOffset)) * weight[i];\n" +
            "    }\n"+
            "    color += texture(uTexture, vTexCoord) * weight[0];\n" +
            "    glFragColor = color;\n" +
            "}";

    public BlurSquareSixPassesLinearSampling(Context context, Point size) {
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

    @Override
    protected boolean isMultiPass() {
        return true;
    }
}
