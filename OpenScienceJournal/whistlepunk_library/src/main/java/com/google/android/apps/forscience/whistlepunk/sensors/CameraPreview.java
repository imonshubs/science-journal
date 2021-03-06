/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/* Custom SurfaceView that receives Camera preview frames and displays them. */
package com.google.android.apps.forscience.whistlepunk.sensors;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.android.apps.forscience.javalib.MaybeConsumer;
import com.google.android.apps.forscience.whistlepunk.AccessibilityUtils;
import com.google.android.apps.forscience.whistlepunk.PictureUtils;
import com.google.android.apps.forscience.whistlepunk.filemetadata.FileMetadataManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Maybe;


public class CameraPreview extends SurfaceView {
    private static final String TAG = "CameraPreview";

    private Camera mCamera;
    private boolean mPreviewStarted = false;

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CameraPreview(Context context) {
        super(context);
        init();
    }

    private void init() {
        // Warning: this callback is only invoked on SurfaceHolder updates if the CameraPreview
        // is visible.
        getHolder().addCallback(new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {
                // The Surface has been created, now tell the camera where to draw the preview.
                if (holder == null) {

                    displayError("Creating camera preview failed; the surface holder was invalid.");
                    return;
                }
                setupPreviewDisplay(holder);
            }

            public void surfaceDestroyed(SurfaceHolder holder) {
                stopPreview();
            }

            public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
                if (holder.getSurface() == null) {
                    return;
                }

                stopPreview();

                // set preview size and make any resize, rotate or reformatting changes here

                setupPreviewDisplay(holder);
            }
        });
    }

    private void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        mPreviewStarted = false;
    }

    private int getDisplayRotation() {
        WindowManager manager =
                (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        return manager.getDefaultDisplay().getRotation();
    }

    public void displayError(String errorMessage) {
        AccessibilityUtils.makeSnackbar(this, errorMessage, Snackbar.LENGTH_SHORT).show();
        if (Log.isLoggable(TAG, Log.ERROR)) {
            Log.e(TAG, "Preview error: " + errorMessage);
        }
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (!mPreviewStarted) {
            setupPreviewDisplay(getHolder());
        }
        requestLayout();
    }

    public void setupPreviewDisplay(SurfaceHolder holder) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
                startPreview();
                setCameraDisplayOrientation(0, mCamera);
            }
        } catch (IOException e) {
            displayError("Creating camera preview failed; the surface holder was invalid.");
        }
    }

    private void startPreview() {
        mCamera.startPreview();
        mPreviewStarted = true;
    }

    private void setCameraDisplayOrientation(int cameraId, Camera camera) {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int degrees = 0;
        switch (getDisplayRotation()) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        // Adjust orientation of taken photo
        Camera.Parameters params = mCamera.getParameters();
        params.setRotation(result);
        mCamera.setParameters(params);

        // Adjust orientation of preview
        camera.setDisplayOrientation(result);
    }

    public void removeCamera() {
        if (mCamera != null) {
            stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mCamera != null) {
            adjustPreviewSizeAndShrinkToMatchRatio();
        }
    }

    private void adjustPreviewSizeAndShrinkToMatchRatio() {
        Camera.Parameters params = mCamera.getParameters();
        int idealWidth = getMeasuredWidth();
        int idealHeight = getMeasuredHeight();
        double idealRatio = (1.0 * idealHeight) / idealWidth;

        // Sizes come out as larger-dimension first regardless of orientation, which makes them
        // weird if we're in portrait mode, so we have to flip them in that case
        // TODO: is this still right on a Chromebook?

        boolean flipSizes = isInPortrait();
        Camera.Size bestSize = null;
        double bestRatioMatch = 0.0;

        // Find preview size with closest aspect ratio, tiebreaking with absolute size
        // On one test device, largest size was first.  Assuming this always works
        for (Camera.Size size : params.getSupportedPreviewSizes()) {
            int testHeight = !flipSizes ? size.height : size.width;
            int testWidth = !flipSizes ? size.width : size.height;
            double ratio = (1.0 * testHeight) / testWidth;
            double ratioMatch = Math.min(ratio, idealRatio) / Math.max(ratio, idealRatio);
            if (ratioMatch > bestRatioMatch) {
                bestSize = size;
                bestRatioMatch = ratioMatch;
            }
        }

        if (bestSize == null) {
            return;
        }

        // Bake in the new preview size
        params.setPreviewSize(bestSize.width, bestSize.height);
        try {
            stopPreview();
            mCamera.setParameters(params);
        } catch (RuntimeException e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                String msg =
                        "Failure setting camera size to " + bestSize.width + "," + bestSize.height;
                Log.e(TAG, msg, e);
                return;
            }
        } finally {
            startPreview();
        }

        // Remeasure to match ideal
        double ratio = 1.0 * bestSize.height / bestSize.width;

        if (flipSizes) {
            ratio = 1.0 / ratio;
        }

        if (ratio < idealRatio) {
            // preview is too short, reduce measured height
            setMeasuredDimension(idealWidth, (int) (idealHeight * ratio / idealRatio));
        } else {
            // preview is too skinny (or just right), reduce measured width
            setMeasuredDimension((int) (idealWidth * idealRatio / ratio), idealHeight);
        }
    }

    private boolean isInPortrait() {
        int rotation = getDisplayRotation();
        return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180;
    }

    public void takePicture(Maybe<String> maybeExperimentId, String uuid,
            final MaybeConsumer<String> onSuccess) {
        // TODO: better strategy (RxJava?) to avoid these null checks
        if (mCamera == null) {
            onSuccess.fail(new IllegalStateException("No camera loaded in CameraPreview"));
        }
        maybeExperimentId.subscribe(experimentId -> takePicture(experimentId, uuid, onSuccess));
    }

    private void takePicture(String experimentId, String uuid, MaybeConsumer<String> onSuccess) {
        final File photoFile = PictureUtils.createImageFile(getContext(), experimentId, uuid);

        try {
            mCamera.takePicture(null, null, null, (data, camera) -> {
                try (FileOutputStream out = new FileOutputStream(photoFile)) {
                    out.write(data);
                    // Pass back the relative path for saving in the label.
                    onSuccess.success(FileMetadataManager.getRelativePathInExperiment(experimentId,
                            photoFile));
                    startPreview();
                } catch (IOException e) {
                    onSuccess.fail(e);
                }
            });
        } catch (RuntimeException e) {
            // TODO: why is this so common?  Do we need to disable the picture button for a while
            //       after taking a picture?
            onSuccess.fail(e);
        }
    }
}
