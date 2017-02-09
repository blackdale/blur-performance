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

/**
 * A two dimensional textured square with blur filter applied above it
 */
public class BlurSquareMipmapLinearSampling extends BlurSquare {

    private final static float MAX_BLUR_RADIUS_DEFAULT = 7.0f;

    final private String vertexShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "in vec4 aPosition;\n" +
            "in vec2 aTexCoord;\n" +
            "out vec2 vTexCoord;\n" +
            "uniform float count;\n" +
            "uniform float radius;\n" +
            "uniform float width;\n" +
            "uniform float height;\n" +
            "out float vCount;\n" +
            "out float vRadius;\n" +
            "out float widthOffset;\n" +
            "out float heightOffset;\n" +

            "void main()\n" +
            "{\n" +
            "    vTexCoord = aTexCoord;\n" +
            "    gl_Position = aPosition;\n" +
            "    widthOffset = pow(2.0, count) / width;\n" +
            "    heightOffset = pow(2.0, count) / height;\n" +
            "    vCount = count;\n" +
            "    vRadius = radius;\n" +
            "}";

    final private String horFragmentShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "uniform sampler2D aTexture;\n" +
            "in float vCount;\n" +
            "in float vRadius;\n" +
            "in vec2 vTexCoord;\n" +
            "in float widthOffset;\n" +
            "in float heightOffset;\n" +

            "out vec4 glFragColor;\n" +

            "void main()\n" +
            "{\n" +
            "    vec4 color = vec4(0.0);\n" +
            "    float weight[5] = float[]( 0.185471, 0.288322, 0.1035, 0.0147858, 0.000652313 );\n" +
            "    float offset[5] = float[]( 0.0, 1.42105, 3.31579, 5.21053, 7.10526 );\n" +
            "    for (int i = 1; i <= 4; i++) {\n" +
            "       color += texture(aTexture, vTexCoord + vec2(offset[i] * widthOffset, 0.0)) * weight[i];\n"+
            "       color += texture(aTexture, vTexCoord - vec2(offset[i] * widthOffset, 0.0)) * weight[i];\n"+
            "    }\n"+
            "    color += texture(aTexture, vTexCoord) * weight[0];\n" +
            "    glFragColor = color;\n" +
//            "    glFragColor = texture(aTexture, vTexCoord);\n" +
            "}";


    final private String verFragmentShaderCode =
            "#version 300 es\n"+
            "precision mediump float;\n" +
            "uniform sampler2D aTexture;\n" +
            "uniform float saturation;\n" +

            "in vec2 vTexCoord;\n" +
            "in float vCount;\n" +
            "in float vRadius;\n" +
            "in float widthOffset;\n" +
            "in float heightOffset;\n" +

            "out vec4 glFragColor;\n" +

            "void main()\n" +
            "{\n" +
            "    vec4 color = vec4(0.0);\n" +
            "    float weight[5] = float[]( 0.185471, 0.288322, 0.1035, 0.0147858, 0.000652313 );\n" +
            "    float offset[5] = float[]( 0.0, 1.42105, 3.31579, 5.21053, 7.10526 );\n" +
            "    for (int i = 1; i <= 4; i++) {\n" +
            "       color += textureLod(aTexture, vTexCoord + vec2(0.0, offset[i] * heightOffset), vCount) * weight[i];\n" +
            "       color += textureLod(aTexture, vTexCoord - vec2(0.0, offset[i] * heightOffset), vCount) * weight[i];\n" +
            "    }\n"+
            "    color += textureLod(aTexture, vTexCoord, vCount) * weight[0];\n" +
            "    glFragColor = color;\n" +
//            "    glFragColor = texture(aTexture, vTexCoord);\n" +
            "}";

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     *
     * @param context
     * @param width
     * @param height
     */
    public BlurSquareMipmapLinearSampling(Context context, int width, int height) {
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