package com.example.android.blurperfmormance;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.opengl.GLES31;
import android.opengl.GLUtils;

public abstract class BlurSquare {

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

    private int[] mFbos = new int[3];
    private int[] mTextureDataHandlers = new int[3];

    private ProgramData mVerticalProgramData = new ProgramData();
    private ProgramData mHorizontalProgramData = new ProgramData();
    Point mSize;
    private float mMipMap = 5;
    private float mRadius = 7;

    private Context mContext;

    private float squareCoords[] = {
            -1f,  1f, 0.0f,
            -1f, -1f, 0.0f,
             1f, -1f, 0.0f,
             1f,  1f, 0.0f };

    private float squareTexCoord[] = {
            0f, 1f,
            0f, 0f,
            1f, 0f,
            1f, 1f
    };

    private final short drawOrder[] = { 0, 1, 2, 0, 2, 3 };

    public abstract String getVertextShaderCode();
    public abstract String getHorizontalFragmentShaderCode();
    public abstract String getVerticalFragmentShaderCode();

    protected boolean isMultiPass() {
        return false;
    }

    public BlurSquare(Context context, Point size) {
        mContext = context;
        prepareBuffers();
        preparePrograms();
        mSize = size;
        prepareFbo();
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES31.glCreateShader(type);

        GLES31.glShaderSource(shader, shaderCode);
        GLES31.glCompileShader(shader);

        return shader;
    }


    private void preparePrograms() {
        int vertexShader = loadShader(
                GLES31.GL_VERTEX_SHADER,
                getVertextShaderCode());
        int fragmentShaderHorizontal = loadShader(
                GLES31.GL_FRAGMENT_SHADER,
                getHorizontalFragmentShaderCode());

        int verticalShaderHorizontal = loadShader(
                GLES31.GL_FRAGMENT_SHADER,
                getVerticalFragmentShaderCode());

        mHorizontalProgramData.mProgram = GLES31.glCreateProgram();
        GLES31.glAttachShader(mHorizontalProgramData.mProgram, vertexShader);
        GLES31.glAttachShader(mHorizontalProgramData.mProgram, fragmentShaderHorizontal);
        GLES31.glLinkProgram(mHorizontalProgramData.mProgram);

        mVerticalProgramData.mProgram = GLES31.glCreateProgram();
        GLES31.glAttachShader(mVerticalProgramData.mProgram, vertexShader);
        GLES31.glAttachShader(mVerticalProgramData.mProgram, verticalShaderHorizontal);
        GLES31.glLinkProgram(mVerticalProgramData.mProgram);

        GLES31.glUseProgram(mHorizontalProgramData.mProgram);
        mHorizontalProgramData.mPositionHandle = GLES31.glGetAttribLocation(mHorizontalProgramData.mProgram, "aPosition");
        mHorizontalProgramData.mTexCoordHandle = GLES31.glGetAttribLocation(mHorizontalProgramData.mProgram, "aTexCoord");
        mHorizontalProgramData.mWidthHandle = GLES31.glGetUniformLocation(mHorizontalProgramData.mProgram, "uWidth");
        mHorizontalProgramData.mHeightHandle = GLES31.glGetUniformLocation(mHorizontalProgramData.mProgram, "uHeight");
        mHorizontalProgramData.mRadiusHandle = GLES31.glGetUniformLocation(mHorizontalProgramData.mProgram, "uRadius");
        mHorizontalProgramData.mMipMapHandle = GLES31.glGetUniformLocation(mHorizontalProgramData.mProgram, "uMipLevel");
        mHorizontalProgramData.mTextureHandle = GLES31.glGetUniformLocation(mHorizontalProgramData.mProgram, "uTexture");

        GLES31.glUseProgram(mVerticalProgramData.mProgram);
        mVerticalProgramData.mPositionHandle = GLES31.glGetAttribLocation(mVerticalProgramData.mProgram, "aPosition");
        mVerticalProgramData.mTexCoordHandle = GLES31.glGetAttribLocation(mVerticalProgramData.mProgram, "aTexCoord");
        mVerticalProgramData.mWidthHandle = GLES31.glGetUniformLocation(mVerticalProgramData.mProgram, "uWidth");
        mVerticalProgramData.mHeightHandle = GLES31.glGetUniformLocation(mVerticalProgramData.mProgram, "uHeight");
        mVerticalProgramData.mRadiusHandle = GLES31.glGetUniformLocation(mVerticalProgramData.mProgram, "uRadius");
        mVerticalProgramData.mMipMapHandle = GLES31.glGetUniformLocation(mVerticalProgramData.mProgram, "uMipLevel");
        mVerticalProgramData.mTextureHandle = GLES31.glGetUniformLocation(mVerticalProgramData.mProgram, "uTexture");
    }

    private void prepareBuffers() {
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(squareCoords);
        mVertexBuffer.position(0);

        ByteBuffer tb = ByteBuffer.allocateDirect(squareTexCoord.length * 4);
        tb.order(ByteOrder.nativeOrder());
        mTexCoordBuffer = tb.asFloatBuffer();
        mTexCoordBuffer.put(squareTexCoord);
        mTexCoordBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        mDrawOrderBuffer = dlb.asShortBuffer();
        mDrawOrderBuffer.put(drawOrder);
        mDrawOrderBuffer.position(0);
    }

    private void prepareFbo() {
        for (int i = 0; i < mFbos.length - 1; ++i) {
            int[] fbo = new int[1];
            GLES31.glGenFramebuffers(1, fbo, 0);
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fbo[0]);

            int[] texture = new int[1];
            GLES31.glGenTextures(1, texture, 0);
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture[0]);
            GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGB, (int) mSize.x, (int) mSize.y, 0, GLES31.GL_RGB, GLES31.GL_UNSIGNED_BYTE, null);

            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);

            GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, texture[0], 0);

            mFbos[i] = fbo[0];
            mTextureDataHandlers[i + 1] = texture[0];
        }
        mTextureDataHandlers[0] = loadTexture(mContext, R.drawable.image);
        mFbos[mFbos.length - 1] = 0;
    }

    private int loadTexture(final Context context, final int resourceId)
    {
        final int[] textureHandle = new int[1];

        GLES31.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;

            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureHandle[0]);

            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR_MIPMAP_LINEAR);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR_MIPMAP_LINEAR);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0);

            GLES31.glGenerateMipmap(GLES31.GL_TEXTURE_2D);

            bitmap.recycle();
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    private void draw(ProgramData aProgramData, float interpolationValue) {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, aProgramData.mFbo);

        GLES31.glUseProgram(aProgramData.mProgram);

        GLES31.glEnableVertexAttribArray(aProgramData.mPositionHandle);
        GLES31.glEnableVertexAttribArray(aProgramData.mTexCoordHandle);

        GLES31.glVertexAttribPointer(
                aProgramData.mPositionHandle, 3,
                GLES31.GL_FLOAT, false,
                3 * 4, mVertexBuffer);

        GLES31.glVertexAttribPointer(
                aProgramData.mTexCoordHandle, 2,
                GLES31.GL_FLOAT, false,
                2 * 4, mTexCoordBuffer);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);

        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, aProgramData.mTextureDataHandle);
        GLES31.glUniform1i(aProgramData.mTextureHandle, 0);

        GLES31.glUniform1f(aProgramData.mWidthHandle, mSize.x);
        GLES31.glUniform1f(aProgramData.mHeightHandle, mSize.y);
        GLES31.glUniform1f(aProgramData.mMipMapHandle, mMipMap * interpolationValue);
        GLES31.glUniform1f(aProgramData.mRadiusHandle, mRadius * interpolationValue);

        GLES31.glDrawElements(
                GLES31.GL_TRIANGLES, drawOrder.length,
                GLES31.GL_UNSIGNED_SHORT, mDrawOrderBuffer);

        GLES31.glDisableVertexAttribArray(aProgramData.mPositionHandle);
        GLES31.glDisableVertexAttribArray(aProgramData.mTexCoordHandle);
    }

    public void draw(float interpolationValue) {
        mVerticalProgramData.mFbo = mFbos[0];
        mVerticalProgramData.mTextureDataHandle = mTextureDataHandlers[0];
        draw(mVerticalProgramData, interpolationValue);

        if (isMultiPass()) {
            mHorizontalProgramData.mFbo = mFbos[1];
            mHorizontalProgramData.mTextureDataHandle = mTextureDataHandlers[1];
            draw(mHorizontalProgramData, interpolationValue);

            mVerticalProgramData.mFbo = mFbos[0];
            mVerticalProgramData.mTextureDataHandle = mTextureDataHandlers[2];
            draw(mVerticalProgramData, interpolationValue);
            mHorizontalProgramData.mFbo = mFbos[1];
            mHorizontalProgramData.mTextureDataHandle = mTextureDataHandlers[1];
            draw(mHorizontalProgramData, interpolationValue);


            mVerticalProgramData.mFbo = mFbos[0];
            mVerticalProgramData.mTextureDataHandle = mTextureDataHandlers[2];
            draw(mVerticalProgramData, interpolationValue);
        }
        mHorizontalProgramData.mFbo = mFbos[2];
        mHorizontalProgramData.mTextureDataHandle = mTextureDataHandlers[1];
        draw(mHorizontalProgramData, interpolationValue);
    }
}
