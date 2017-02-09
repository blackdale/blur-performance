/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.blurperfmormance;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES31;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * A two dimensional textured square with blur filter applied above it
 */
public class BlurSquareTwoPasses extends BlurSquare {

    private final static float MAX_BLUR_RADIUS_DEFAULT = 15.0f;

    final private String vertexShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "in vec4 aPosition;\n" +
            "in vec2 aTexCoord;\n" +
            "out vec2 vTexCoord;\n" +
            "out float weight[16];\n" +
            "uniform float count;\n" +
            "uniform float radius;\n" +
            "uniform float width;\n" +
            "uniform float height;\n" +
            "out float vCount;\n" +
            "out float vRadius;\n" +
            "out float widthOffset;\n" +
            "out float heightOffset;\n" +

            "void calculateWeights(float aRadius)\n" +
            "{\n" +
            "    int r = int(aRadius);\n" +
            "    float sigma = (float(r) + 1.0) / sqrt(2.0 * log(255.0));\n" +
            "    float sumOfWeights = 0.0;\n" +
            "    for (int i = 0; i < r + 1; i++) {\n" +
            "        weight[i] = (1.0 / sqrt(2.0 * 3.14 * pow(sigma, 2.0))) * exp(-pow(float(i), 2.0) / (2.0 * pow(sigma, 2.0)));\n" +
            "        if (i == 0) {\n" +
            "            sumOfWeights += weight[i];\n" +
            "        } else {\n" +
            "            sumOfWeights += 2.0 * weight[i];\n" +
            "        }\n" +
            "    }\n" +
            "    for (int i = 0; i < r + 1; i++) {\n" +
            "        weight[i] = weight[i] / sumOfWeights;\n" +
            "    }\n" +
            "}\n" +

            "void main()\n" +
            "{\n" +
            "    vTexCoord = aTexCoord;\n" +
            "    gl_Position = aPosition;\n" +
            "    widthOffset = radius / width;\n" +
            "    heightOffset = radius / height;\n" +
            "    calculateWeights(" + MAX_BLUR_RADIUS_DEFAULT + ");\n" +
            "    vCount = count;\n" +
            "    vRadius = radius;\n" +
            "}";

    final private String horFragmentShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "uniform sampler2D aTexture;\n" +
            "in float vCount;\n" +
            "in float vRadius;\n" +
            "in float weight[16];\n" +
            "in vec2 vTexCoord;\n" +
            "in float widthOffset;\n" +
            "in float heightOffset;\n" +

            "out vec4 glFragColor;\n" +

            "void main()\n" +
            "{\n" +
            "    vec4 color = vec4(0.0);\n" +
            "    for (int i = 1; i <= int(" + MAX_BLUR_RADIUS_DEFAULT + "); i++) {\n" +
            "       color += texture(aTexture, vTexCoord + vec2(float(i) * widthOffset, 0.0)) * weight[i];\n"+
            "       color += texture(aTexture, vTexCoord - vec2(float(i) * widthOffset, 0.0)) * weight[i];\n"+
            "    }\n"+
            "    color += texture(aTexture, vTexCoord) * weight[0];\n" +
            "    glFragColor = color;\n" +
            "}";


    final private String verFragmentShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "uniform sampler2D aTexture;\n" +
            "uniform float saturation;\n" +

            "in vec2 vTexCoord;\n" +
            "in float vCount;\n" +
            "in float vRadius;\n" +
            "in float weight[16];" +
            "in float widthOffset;\n" +
            "in float heightOffset;\n" +

            "out vec4 glFragColor;\n" +

            "void main()\n" +
            "{\n" +
            "    vec4 color = vec4(0.0);\n" +
            "    for (int i = 1; i <= int(" + MAX_BLUR_RADIUS_DEFAULT + "); i++) {\n" +
            "       color += texture(aTexture, vTexCoord + vec2(0.0, float(i) * heightOffset)) * weight[i];\n" +
            "       color += texture(aTexture, vTexCoord - vec2(0.0, float(i) * heightOffset)) * weight[i];\n" +
            "    }\n"+
            "    color += texture(aTexture, vTexCoord) * weight[0];\n" +
            "    glFragColor = color;\n" +
            "}";

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     *
     * @param context
     * @param width
     * @param height
     */
    public BlurSquareTwoPasses(Context context, int width, int height) {
        super(context, width, height);
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