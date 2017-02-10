/*
 * Copyright 2013 Google Inc. All rights reserved.
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

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Display;
import android.view.TextureView;

/**
 * Handles GL rendering and SurfaceTexture callbacks.
 * <p>
 * We don't create a Looper, so the SurfaceTexture-by-way-of-TextureView callbacks
 * happen on the UI thread.
 */
public class BlurRenderer extends Thread implements TextureView.SurfaceTextureListener {
    private Object mLock = new Object();        // guards mSurfaceTexture, mDone
    private SurfaceTexture mSurfaceTexture;
    private EglCore mEglCore;
    private boolean mDone;
    private Context mContext;
    private BlurSquare mBlurSquare;
    private BlurSquare[] mBlurSquares;
    private long mStartTime = 0;
    private long mFrameCounter = 0;
    private long mAnimationDuration = 3000;
    private long mAnimationStart;

    private static final String TAG = "BlurRenderer";

    public BlurRenderer(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        while (true) {
            SurfaceTexture surfaceTexture = null;

            // Latch the SurfaceTexture when it becomes available.  We have to wait for
            // the TextureView to create it.
            synchronized (mLock) {
                while (!mDone && (surfaceTexture = mSurfaceTexture) == null) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);     // not expected
                    }
                }
                if (mDone) {
                    break;
                }
            }
            Log.d(TAG, "Got surfaceTexture=" + surfaceTexture);

            // Create an EGL surface for our new SurfaceTexture.  We're not on the same
            // thread as the SurfaceTexture, which is a concern for the *consumer*, which
            // wants to call updateTexImage().  Because we're the *producer*, i.e. the
            // one generating the frames, we don't need to worry about being on the same
            // thread.
            mEglCore = new EglCore(null, EglCore.FLAG_TRY_GLES3);
            WindowSurface windowSurface = new WindowSurface(mEglCore, mSurfaceTexture);
            windowSurface.makeCurrent();

            // Render frames until we're told to stop or the SurfaceTexture is destroyed.
            doAnimation(windowSurface);

            windowSurface.release();
            mEglCore.release();
            surfaceTexture.release();
        }

        Log.d(TAG, "Renderer thread exiting");
    }

    Point getScreenDimentions() {
        Display display = ((Activity)mContext).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    /**
     * Draws updates as fast as the system will allow.
     * <p>
     * In 4.4, with the synchronous buffer queue queue, the frame rate will be limited.
     * In previous (and future) releases, with the async queue, many of the frames we
     * render may be dropped.
     * <p>
     * The correct thing to do here is use Choreographer to schedule frame updates off
     * of vsync, but that's not nearly as much fun.
     */
    private void doAnimation(WindowSurface eglSurface) {
        Point size = getScreenDimentions();

        mBlurSquares = new BlurSquare[3];
        mBlurSquares[0] = new BlurSquareTwoPasses(mContext, size);
        mBlurSquares[1] = new BlurSquareTwoPassesLinearSampling(mContext, size);
        mBlurSquares[2] = new BlurSquareMipmap(mContext, size);
        mBlurSquare = mBlurSquares[0];

        mAnimationStart = System.currentTimeMillis();

        while (true) {
            // Check to see if the TextureView's SurfaceTexture is still valid.
            synchronized (mLock) {
                SurfaceTexture surfaceTexture = mSurfaceTexture;
                if (surfaceTexture == null) {
                    Log.d(TAG, "doAnimation exiting");
                    return;
                }
            }

            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            float interpolationValue = getInterpolationValue();

            synchronized (mLock) {
                mBlurSquare.draw(interpolationValue);
            }

            printFPS();

            eglSurface.swapBuffers();
        }
    }

    private float getInterpolationValue() {
        long currentTime = System.currentTimeMillis();
        float interpolationValue = (float)(currentTime - mAnimationStart) / (float)mAnimationDuration;
        if (interpolationValue > 1.0 && interpolationValue < 2.0)
            interpolationValue = 2.0f - interpolationValue;
        else if (interpolationValue > 2.0) {
            mAnimationStart = System.currentTimeMillis();
            interpolationValue = 0.0f;
        }
        return interpolationValue;
    }

    private void printFPS() {
        mFrameCounter++;
        if (mFrameCounter % 60 == 0) {
            long duration = System.nanoTime() - mStartTime;
            Log.d("Blur", "FPS: " + 60.0 * 1000000000.0 / (float)duration);

            mStartTime = System.nanoTime();
        }
    }

    /**
     * Tells the thread to stop running.
     */
    public void halt() {
        synchronized (mLock) {
            mDone = true;
            mLock.notify();
        }
    }

    @Override   // will be called on UI thread
    public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable(" + width + "x" + height + ")");
        synchronized (mLock) {
            mSurfaceTexture = st;
            mLock.notify();
        }
    }

    @Override   // will be called on UI thread
    public void onSurfaceTextureSizeChanged(SurfaceTexture st, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged(" + width + "x" + height + ")");
        // TODO: ?
    }

    @Override   // will be called on UI thread
    public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
        Log.d(TAG, "onSurfaceTextureDestroyed");

        // We set the SurfaceTexture reference to null to tell the Renderer thread that
        // it needs to stop.  The renderer might be in the middle of drawing, so we want
        // to return false here so that the caller doesn't try to release the ST out
        // from under us.
        //
        // In theory.
        //
        // In 4.4, the buffer queue was changed to be synchronous, which means we block
        // in dequeueBuffer().  If the renderer has been running flat out and is currently
        // sleeping in eglSwapBuffers(), it's going to be stuck there until somebody
        // tears down the SurfaceTexture.  So we need to tear it down here to ensure
        // that the renderer thread will break.  If we don't, the thread sticks there
        // forever.
        //
        // The only down side to releasing it here is we'll get some complaints in logcat
        // when eglSwapBuffers() fails.
        synchronized (mLock) {
            mSurfaceTexture = null;
        }
        return true;
    }

    @Override   // will be called on UI thread
    public void onSurfaceTextureUpdated(SurfaceTexture st) {
    }

    public void changeBlurAlgorithm(int index) {
        synchronized (mLock) {
            mBlurSquare = mBlurSquares[index % mBlurSquares.length];
        }
    }
}