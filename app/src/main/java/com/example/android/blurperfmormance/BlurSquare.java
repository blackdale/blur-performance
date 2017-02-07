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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES31;
import android.opengl.GLUtils;

/**
 * A two dimensional textured square with blur filter applied above it
 */
public class BlurSquare {

    private final static float MAX_BLUR_RADIUS_DEFAULT = 7.0f;
    
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
                    "    for (int i = 0; i < r + 1; i++){\n" +
                    "        weight[i] = (1.0 / sqrt(2.0 * 3.14 * pow(sigma, 2.0))) * exp(-pow(float(i), 2.0) / (2.0 * pow(sigma, 2.0)));\n" +
                    "        if (i == 0){\n" +
                    "            sumOfWeights += weight[i];\n" +
                    "        }else{\n" +
                    "            sumOfWeights += 2.0 * weight[i];\n" +
                    "        }\n" +
                    "    }\n" +
                    "    for (int i = 0; i < r + 1; i++){\n" +
                    "        weight[i] = weight[i] / sumOfWeights;\n" +
                    "    }\n" +
                    "}\n" +

                    "void main()\n" +
                    "{\n" +
                    "    vTexCoord = aTexCoord;\n" +
                    "    gl_Position = aPosition;\n" +
                    "    widthOffset = pow(2.0, count - 1.0) / width;\n" +
                    "    heightOffset = pow(2.0, count - 1.0) / height;\n" +
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
//                    "       color += textureLod( SGTexture, vTexCoords + vec2(float(i) * widthOffset, 0.0), vCount) * weight[i];\n"+
//                    "       color += textureLod( SGTexture, vTexCoords - vec2(float(i) * widthOffset, 0.0), vCount) * weight[i];\n"+
                    "       color += texture(aTexture, vTexCoord + vec2(float(i) * widthOffset, 0.0)) * weight[i];\n"+
                    "       color += texture(aTexture, vTexCoord - vec2(float(i) * widthOffset, 0.0)) * weight[i];\n"+
                    "    }\n"+
//                    "    color += textureLod(SGTexture, vTexCoords, vCount) * weight[0];\n" +
                    "    color += texture(aTexture, vTexCoord) * weight[0];\n" +
                    "    glFragColor = /*texture(aTexture, vTexCoord)*/color;\n" +
//                    "    glFragColor = vec4(color, 1.0);\n" +
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
                    "       color += textureLod(aTexture, vTexCoord + vec2(0.0, float(i) * heightOffset), vCount) * weight[i];\n" +
                    "       color += textureLod(aTexture, vTexCoord - vec2(0.0, float(i) * heightOffset), vCount) * weight[i];\n" +
                    "    }\n"+
                    "    color += textureLod(aTexture, vTexCoord, vCount) * weight[0];\n" +
                    "    glFragColor = /*textureLod(aTexture, vTexCoord, vCount)*/color;\n" +
//                    "    glFragColor = texture(aTexture, vTexCoord);\n" +
                    "}";
    
//    private final String vertexShaderCode =
//            "uniform mat4 uMVPMatrix;" +
//            "attribute vec4 aPosition;" +
//            "attribute vec2 aTexCoord;" +
//            "varying vec2 vTexCoord;" +
//            "void main() {" +
//            "  vTexCoord = aTexCoord;" +
//            "  gl_Position = aPosition;" +
//            "}";
//
//    private final String fragmentShaderCode =
//            "precision mediump float;" +
//            "uniform sampler2D aTexture;" +
//            "varying vec2 vTexCoord;" +
//            "void main() {" +
//            "  gl_FragColor = texture2D(aTexture, vTexCoord);" +
//            "}";

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTexCoordBuffer;
    private ShortBuffer mDrawOrderBuffer;

    private class ProgramData {
        public int mProgram;
        private int mPositionHandle;
        private int mTexCoordHandle;
        public int mTextureHandle;
        public int mTextureDataHandle;
        public int mWidthHandle;
        public int mHeightHandle;
        public int mMipMapHandle;
        public int mRadiusHandle;
        public int mFbo;
    }

    private ProgramData mVerticalProgramData = new ProgramData();
    private ProgramData mHorizontalProgramData = new ProgramData();
    private float mWidth;
    private float mHeight;
    private float mMipMap = 5;
    private float mRadius = 7;

    private Context mContext;

    private float squareCoords[] = {
            -1f,  1f, 0.0f,   // top left
            -1f, -1f, 0.0f,   // bottom left
             1f, -1f, 0.0f,   // bottom right
             1f,  1f, 0.0f }; // top right

    private float squareTexCoord[] = {
            0f, 1f,
            0f, 0f,
            1f, 0f,
            1f, 1f
    };

    private final short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    public BlurSquare(Context context, int width, int height) {
        mContext = context;
        prepareBuffers();
        preparePrograms();
        prepareTexture();
        mWidth = width;
        mHeight = height;
        prepareFbo();
    }

    private static int loadShader(int type, String shaderCode) {

        // create a vertex shader type (GLES31.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES31.GL_FRAGMENT_SHADER)
        int shader = GLES31.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES31.glShaderSource(shader, shaderCode);
        GLES31.glCompileShader(shader);

        return shader;
    }

    private void preparePrograms() {
        // Prepare shaders and OpenGL program
        int vertexShader = loadShader(
                GLES31.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShaderHorizontal = loadShader(
                GLES31.GL_FRAGMENT_SHADER,
                horFragmentShaderCode);

        int verticalShaderHorizontal = loadShader(
                GLES31.GL_FRAGMENT_SHADER,
                verFragmentShaderCode);

        mHorizontalProgramData.mProgram = GLES31.glCreateProgram();             // create empty OpenGL Program
        GLES31.glAttachShader(mHorizontalProgramData.mProgram, vertexShader);   // add the vertex shader to program
        GLES31.glAttachShader(mHorizontalProgramData.mProgram, fragmentShaderHorizontal); // add the fragment shader to program
        GLES31.glLinkProgram(mHorizontalProgramData.mProgram);                  // create OpenGL program executables

        mVerticalProgramData.mProgram = GLES31.glCreateProgram();
        GLES31.glAttachShader(mVerticalProgramData.mProgram, vertexShader);
        GLES31.glAttachShader(mVerticalProgramData.mProgram, verticalShaderHorizontal);
        GLES31.glLinkProgram(mVerticalProgramData.mProgram);

        GLES31.glUseProgram(mHorizontalProgramData.mProgram);
        mHorizontalProgramData.mPositionHandle = GLES31.glGetAttribLocation(mHorizontalProgramData.mProgram, "aPosition");
        mHorizontalProgramData.mTexCoordHandle = GLES31.glGetAttribLocation(mHorizontalProgramData.mProgram, "aTexCoord");
        mHorizontalProgramData.mWidthHandle = GLES31.glGetUniformLocation(mHorizontalProgramData.mProgram, "width");
        mHorizontalProgramData.mHeightHandle = GLES31.glGetUniformLocation(mHorizontalProgramData.mProgram, "height");
        mHorizontalProgramData.mRadiusHandle = GLES31.glGetUniformLocation(mHorizontalProgramData.mProgram, "radius");
        mHorizontalProgramData.mMipMapHandle = GLES31.glGetUniformLocation(mHorizontalProgramData.mProgram, "count");
        mHorizontalProgramData.mTextureHandle = GLES31.glGetUniformLocation(mHorizontalProgramData.mProgram, "aTexture");

        GLES31.glUseProgram(mVerticalProgramData.mProgram);
        mVerticalProgramData.mPositionHandle = GLES31.glGetAttribLocation(mVerticalProgramData.mProgram, "aPosition");
        mVerticalProgramData.mTexCoordHandle = GLES31.glGetAttribLocation(mVerticalProgramData.mProgram, "aTexCoord");
        mVerticalProgramData.mWidthHandle = GLES31.glGetUniformLocation(mVerticalProgramData.mProgram, "width");
        mVerticalProgramData.mHeightHandle = GLES31.glGetUniformLocation(mVerticalProgramData.mProgram, "height");
        mVerticalProgramData.mRadiusHandle = GLES31.glGetUniformLocation(mVerticalProgramData.mProgram, "radius");
        mVerticalProgramData.mMipMapHandle = GLES31.glGetUniformLocation(mVerticalProgramData.mProgram, "count");
        mVerticalProgramData.mTextureHandle = GLES31.glGetUniformLocation(mVerticalProgramData.mProgram, "aTexture");
    }

    private void prepareTexture() {
        mVerticalProgramData.mTextureDataHandle = loadTexture(mContext, R.drawable.image);
    }

    private void prepareBuffers() {
        // Initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 4 bytes per float)
                squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(squareCoords);
        mVertexBuffer.position(0);

        ByteBuffer tb = ByteBuffer.allocateDirect(squareTexCoord.length * 4);
        tb.order(ByteOrder.nativeOrder());
        mTexCoordBuffer = tb.asFloatBuffer();
        mTexCoordBuffer.put(squareTexCoord);
        mTexCoordBuffer.position(0);

        // Initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of texcoord values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        mDrawOrderBuffer = dlb.asShortBuffer();
        mDrawOrderBuffer.put(drawOrder);
        mDrawOrderBuffer.position(0);
    }

    private void prepareFbo() {
        int[] fbo = new int[1];
        GLES31.glGenFramebuffers(1, fbo, 0);
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fbo[0]);
        mVerticalProgramData.mFbo = fbo[0];

        int[] texture = new int[1];
        GLES31.glGenTextures(1, texture, 0);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture[0]);
        GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGB, (int)mWidth, (int)mHeight, 0, GLES31.GL_RGB, GLES31.GL_UNSIGNED_BYTE, null);
        mHorizontalProgramData.mTextureDataHandle = texture[0];

        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR/*_MIPMAP_LINEAR*/);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);

        GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, texture[0], 0);

        mHorizontalProgramData.mFbo = 0;
    }

    private int loadTexture(final Context context, final int resourceId)
    {
        final int[] textureHandle = new int[1];

        GLES31.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            // No pre-scaling
            options.inScaled = false;

            // Read in the resource
            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

            // Bind to the texture in OpenGL
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR_MIPMAP_LINEAR);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0);

            // Generate mipmaps
            GLES31.glGenerateMipmap(GLES31.GL_TEXTURE_2D);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    private void draw(ProgramData aProgramData, float width, float height, float interpolationValue) {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, aProgramData.mFbo);

        // Add program to OpenGL environment
        GLES31.glUseProgram(aProgramData.mProgram);

        // Enable a handle to the triangle vertices
        GLES31.glEnableVertexAttribArray(aProgramData.mPositionHandle);
        GLES31.glEnableVertexAttribArray(aProgramData.mTexCoordHandle);

        // Prepare the triangle coordinate data
        GLES31.glVertexAttribPointer(
                aProgramData.mPositionHandle, 3,
                GLES31.GL_FLOAT, false,
                3 * 4, mVertexBuffer);

        GLES31.glVertexAttribPointer(
                aProgramData.mTexCoordHandle, 2,
                GLES31.GL_FLOAT, false,
                2 * 4, mTexCoordBuffer);

        // Get handle to fragment shader's vColor member
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, aProgramData.mTextureDataHandle);
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES31.glUniform1i(aProgramData.mTextureHandle, 0);

        GLES31.glUniform1f(aProgramData.mWidthHandle, width);
        GLES31.glUniform1f(aProgramData.mHeightHandle, height);
        GLES31.glUniform1f(aProgramData.mMipMapHandle, mMipMap * interpolationValue);
        GLES31.glUniform1f(aProgramData.mRadiusHandle, mRadius * interpolationValue);

        // Draw the square
        GLES31.glDrawElements(
                GLES31.GL_TRIANGLES, drawOrder.length,
                GLES31.GL_UNSIGNED_SHORT, mDrawOrderBuffer);

        // Disable vertex array
        GLES31.glDisableVertexAttribArray(aProgramData.mPositionHandle);
        GLES31.glDisableVertexAttribArray(aProgramData.mTexCoordHandle);
    }

    public void draw(float interpolationValue) {
        // Make horizontal blur pass (render to fbo)
        draw(mVerticalProgramData, mWidth, mHeight, interpolationValue);
        // Make vertical blur pass (render on screen)
        draw(mHorizontalProgramData, mWidth, mHeight, interpolationValue);
    }
}