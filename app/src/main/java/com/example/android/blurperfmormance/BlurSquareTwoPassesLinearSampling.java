package com.example.android.blurperfmormance;

import android.content.Context;
import android.graphics.Point;

public class BlurSquareTwoPassesLinearSampling extends BlurSquare {

    private final static float MAX_BLUR_RADIUS_DEFAULT = 15.0f;

    final private String vertexShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "in vec4 aPosition;\n" +
            "in vec2 aTexCoord;\n" +
            "out vec2 vTexCoord;\n" +
            "out float vWeight[16];\n" +
            "out float vOffset[16];\n" +
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
            "    float weight[16];\n" +
            "    for (int i = 0; i < r + 1; i++) {\n" +
            "        weight[i] = (1.0 / sqrt(2.0 * 3.14 * float(sigma * sigma))) * exp(-float(i * i) / (2.0 * float(sigma * sigma)));\n" +
            "        if (i == 0) {\n" +
            "            sumOfWeights += weight[i];\n" +
            "        } else {\n" +
            "            sumOfWeights += 2.0 * weight[i];\n" +
            "        }\n" +
            "    }\n" +
            "    for (int i = 0; i < r + 1; i++) {\n" +
            "        weight[i] = weight[i] / sumOfWeights;\n" +
            "    }\n" +
            "    vWeight[0] = weight[0];\n" +
            "    for (int i = 1; i <= (r + 1) / 2; i++) {\n" +
            "        vWeight[i] = weight[2 * i - 1] + weight[2 * i];\n" +
            "        vOffset[i] = (float(2 * i - 1) * weight[2 * i - 1] + float(2 * i) * weight[2 * i]) / vWeight[i];\n" +
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
            "in vec2 vTexCoord;\n" +
            "in float vWeight[16];\n" +
            "in float vOffset[16];\n" +
            "in float vWidthOffset;\n" +
            "in float vHeightOffset;\n" +

            "out vec4 glFragColor;\n" +

            "float rand(vec2 co){\n" +
            "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);\n" +
            "}" +

            "void main()\n" +
            "{\n" +
            "    vec4 color = vec4(0.0);\n" +
//            "    float weight[8] = float[]( 0.135834, 0.236319, 0.135039, 0.0485715, 0.0106857, 0.00136997, 0.0000944803, 0.00000304775 );\n" +
//            "    float offset[8] = float[]( 0.0, 1.45714, 3.4, 5.34286, 7.28571, 9.22857, 11.1714, 13.1143 );\n" +
            "    for (int i = 1; i <= 7; i++) {\n" +
            "       color += texture(uTexture, vTexCoord + vec2(vOffset[i] * vWidthOffset, 0.0)) * vWeight[i];\n"+
            "       color += texture(uTexture, vTexCoord - vec2(vOffset[i] * vWidthOffset, 0.0)) * vWeight[i];\n"+
            "    }\n"+
            "    color += texture(uTexture, vTexCoord) * vWeight[0];\n" +
            "    glFragColor = color;\n" +
            "}";


    final private String verFragmentShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +

            "in vec2 vTexCoord;\n" +
            "in float vWeight[16];\n" +
            "in float vOffset[16];\n" +
            "in float vWidthOffset;\n" +
            "in float vHeightOffset;\n" +

            "out vec4 glFragColor;\n" +

            "float rand(vec2 co){\n" +
            "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);\n" +
            "}" +

            "void main()\n" +
            "{\n" +
            "    vec4 color = vec4(0.0);\n" +
//            "    float weight[8] = float[]( 0.135834, 0.236319, 0.135039, 0.0485715, 0.0106857, 0.00136997, 0.0000944803, 0.00000304775 );\n" +
//            "    float offset[8] = float[]( 0.0, 1.45714, 3.4, 5.34286, 7.28571, 9.22857, 11.1714, 13.1143 );\n" +
            "    for (int i = 1; i <= 7; i++) {\n" +
            "       color += texture(uTexture, vTexCoord + vec2(0.0, vOffset[i] * vHeightOffset)) * vWeight[i];\n" +
            "       color += texture(uTexture, vTexCoord - vec2(0.0, vOffset[i] * vHeightOffset)) * vWeight[i];\n" +
            "    }\n"+
            "    color += texture(uTexture, vTexCoord) * vWeight[0];\n" +
            "    glFragColor = color;\n" +
            "}";

    public BlurSquareTwoPassesLinearSampling(Context context, Point size) {
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
