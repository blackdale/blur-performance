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

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;

public class BlurActivity extends Activity {

    private TextureView mTextureView;
    private BlurRenderer mRenderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRenderer = new BlurRenderer(this);
        mRenderer.start();
        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(mRenderer);
        setContentView(mTextureView);

        mTextureView.setOnTouchListener(new View.OnTouchListener() {
            private int mIndex = 0;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() != MotionEvent.ACTION_UP)
                    return true;
                mRenderer.changeBlurAlgorithm(++mIndex);
                return true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRenderer.halt();
    }
}