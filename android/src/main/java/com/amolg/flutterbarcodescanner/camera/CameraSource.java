/*
 * Copyright (C) The Android Open Source Project
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
package com.amolg.flutterbarcodescanner.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Build;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.annotation.StringDef;

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import java.io.IOException;
import java.lang.Thread.State;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings("deprecation")
public class CameraSource {
    @SuppressLint("InlinedApi")
    public static final int CAMERA_FACING_BACK = CameraInfo.CAMERA_FACING_BACK;
    @SuppressLint("InlinedApi")
    public static final int CAMERA_FACING_FRONT = CameraInfo.CAMERA_FACING_FRONT;
    private static final int DUMMY_TEXTURE_NAME = 100;
    private static final float ASPECT_RATIO_TOLERANCE = 0.01f;

    private Context mContext;
    private final Object mCameraLock = new Object();
    private Camera mCamera;
    private int mFacing = CAMERA_FACING_BACK;
    private int mRotation;
    private Size mPreviewSize;

    // These values may be requested by the caller.  Due to hardware limitations, we may need to
    // select close, but not exactly the same values for these.
    private float mRequestedFps = 30.0f;
    private int mRequestedPreviewWidth = 1024;
    private int mRequestedPreviewHeight = 768;


    private String mFocusMode = null;
    private String mFlashMode = null;

    // These instances need to be held onto to avoid GC of their underlying resources.  Even though
    // these aren't used outside of the method that creates them, they still must have hard
    // references maintained to them.
    private SurfaceView mDummySurfaceView;
    private SurfaceTexture mDummySurfaceTexture;

    /**
     * Dedicated thread and associated runnable for calling into the detector with frames, as the
     * frames become available from the camera.
     */
    private Thread mProcessingThread;
    private FrameProcessingRunnable mFrameProcessor;

    /**
     * Map to convert between a byte array, received from the camera, and its associated byte
     * buffer.  We use byte buffers internally because this is a more efficient way to call into
     * native code later (avoids a potential copy).
     */
    private Map<byte[], ByteBuffer> mBytesToByteBuffer = new HashMap<>();


    @StringDef({
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
            Camera.Parameters.FOCUS_MODE_AUTO,
            Camera.Parameters.FOCUS_MODE_EDOF,
            Camera.Parameters.FOCUS_MODE_FIXED,
            Camera.Parameters.FOCUS_MODE_INFINITY,
            Camera.Parameters.FOCUS_MODE_MACRO
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface FocusMode {
    }

    @StringDef({
            Camera.Parameters.FLASH_MODE_ON,
            Camera.Parameters.FLASH_MODE_OFF,
            Camera.Parameters.FLASH_MODE_AUTO,
            Camera.Parameters.FLASH_MODE_RED_EYE,
            Camera.Parameters.FLASH_MODE_TORCH
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface FlashMode {
    }

    /**
     * Builder for configuring and creating an associated camera source.
     */
    public static class Builder {
        private final Detector<?> mDetector;
        private CameraSource mCameraSource = new CameraSource();

        /**
         * Creates a camera source builder with the supplied context and detector.  Camera preview
         * images will be streamed to the associated detector upon starting the camera source.
         */
        public Builder(Context context, Detector<?> detector) {
            if (context == null) {
                throw new IllegalArgumentException("No context supplied.");
            }
            if (detector == null) {
                throw new IllegalArgumentException("No detector supplied.");
            }

            mDetector = detector;
            mCameraSource.mContext = context;
        }

        /**
         * Sets the requested frame rate in frames per second.  If the exact requested value is not
         * not available, the best matching available value is selected.   Default: 30.
         */
        public Builder setRequestedFps(float fps) {
            if (fps <= 0) {
                throw new IllegalArgumentException("Invalid fps: " + fps);
            }
            mCameraSource.mRequestedFps = fps;
            return this;
        }

        public Builder setFocusMode(@FocusMode String mode) {
            mCameraSource.mFocusMode = mode;
            return this;
        }

        public Builder setFlashMode(@FlashMode String mode) {
            mCameraSource.mFlashMode = mode;
            return this;
        }

        /**
         * Sets the desired width and height of the camera frames in pixels.  If the exact desired
         * values are not available options, the best matching available options are selected.
         * Also, we try to select a preview size which corresponds to the aspect ratio of an
         * associated full picture size, if applicable.  Default: 1024x768.
         */
        public Builder setRequestedPreviewSize(int width, int height) {
            // Restrict the requested range to something within the realm of possibility.  The
            // choice of 1000000 is a bit arbitrary -- intended to be well beyond resolutions that
            // devices can support.  We bound this to avoid int overflow in the code later.
            final int MAX = 1000000;
            if ((width <= 0) || (width > MAX) || (height <= 0) || (height > MAX)) {
                throw new IllegalArgumentException("Invalid preview size: " + width + "x" + height);
            }
            mCameraSource.mRequestedPreviewWidth = width;
            mCameraSource.mRequestedPreviewHeight = height;
            return this;
        }

        /**
         * Sets the camera to use (either {@link #CAMERA_FACING_BACK} or
         * {@link #CAMERA_FACING_FRONT}). Default: back facing.
         */
        public Builder setFacing(int facing) {
            if ((facing != CAMERA_FACING_BACK) && (facing != CAMERA_FACING_FRONT)) {
                throw new IllegalArgumentException("Invalid camera: " + facing);
            }
            mCameraSource.mFacing = facing;
            return this;
        }

        /**
         * Creates an instance of the camera source.
         */
        public CameraSource build() {
            mCameraSource.mFrameProcessor = mCameraSource.new FrameProcessingRunnable(mDetector);
            return mCameraSource;
        }
    }

    /**
     * Callback interface used to signal the moment of actual image capture.
     */
    public interface ShutterCallback {
        /**
         * Called as near as possible to the moment when a photo is captured from the sensor. This
         * is a good opportunity to play a shutter sound or give other feedback of camera operation.
         * This may be some time after the photo was triggered, but some time before the actual data
         * is available.
         */
        void onShutter();
    }

    /**
     * Callback interface used to supply image data from a photo capture.
     */
    public interface PictureCallback {
        /**
         * Called when image data is available after a picture is taken.  The format of the data
         * is a jpeg binary.
         */
        void onPictureTaken(byte[] data);
    }

    /**
     * Callback interface used to notify on completion of camera auto focus.
     */
    public interface AutoFocusCallback {
        /**
         * Called when the camera auto focus completes.  If the camera
         * does not support auto-focus and autoFocus is called,
         * onAutoFocus will be called immediately with a fake value of
         * <code>success</code> set to <code>true</code>.
         * <p/>
         * The auto-focus routine does not lock auto-exposure and auto-white
         * balance after it completes.
         *
         * @param success true if focus was successful, false if otherwise
         */
        void onAutoFocus(boolean success);
    }

    /**
     * Callback interface used to notify on auto focus start and stop.
     * <p/>
     * <p>This is only supported in continuous autofocus modes -- {@link
     * Camera.Parameters#FOCUS_MODE_CONTINUOUS_VIDEO} and {@link
     * Camera.Parameters#FOCUS_MODE_CONTINUOUS_PICTURE}. Applications can show
     * autofocus animation based on this.</p>
     */
    public interface AutoFocusMoveCallback {
        /**
         * Called when the camera auto focus starts or stops.
         *
         * @param start true if focus starts to move, false if focus stops to move
         */
        void onAutoFocusMoving(boolean start);
    }

    /**
     * Stops the camera and releases the resources of the camera and underlying detector.
     */
    public void release() {
        synchronized (mCameraLock) {
            stop();
            mFrameProcessor.release();
        }
    }

    /**
     * Opens the camera and starts sending preview frames to the underlying detector.  The preview
     * frames are not displayed.
     *
     * @throws IOException if the camera's preview texture or display could not be initialized
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    public CameraSource start() throws IOException {
        synchronized (mCameraLock) {
            if (mCamera != null) {
                return this;
            }

            mCamera = createCamera();

            // SurfaceTexture was introduced in Honeycomb (11), so if we are running and
            // old version of Android. fall back to use SurfaceView.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mDummySurfaceTexture = new SurfaceTexture(DUMMY_TEXTURE_NAME);
                mCamera.setPreviewTexture(mDummySurfaceTexture);
            } else {
                mDummySurfaceView = new SurfaceView(mContext);
                mCamera.setPreviewDisplay(mDummySurfaceView.getHolder());
            }
            mCamera.startPreview();

            mProcessingThread = new Thread(mFrameProcessor);
            mFrameProcessor.setActive(true);
            mProcessingThread.start();
        }
        return this;
    }

    /**
     * Opens the camera and starts sending preview frames to the underlying detector.  The supplied
     * surface holder is used for the preview so frames can be displayed to the user.
     *
     * @param surfaceHolder the surface holder to use for the preview frames
     * @throws IOException if the supplied surface holder could not be used as the preview display
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    public CameraSource start(SurfaceHolder surfaceHolder) throws IOException {
        synchronized (mCameraLock) {
            if (mCamera != null) {
                return this;
            }

            mCamera = createCamera();
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();

            mProcessingThread = new Thread(mFrameProcessor);
            mFrameProcessor.setActive(true);
            mProcessingThread.start();
        }
        return this;
    }

    /**
     * Closes the camera and stops sending frames to the underlying frame detector.
     * <p/>
     * This camera source may be restarted again by calling {@link #start()} or
     * {@link #start(SurfaceHolder)}.
     * <p/>
     * Call {@link #release()} instead to completely shut down this camera source and release the
     * resources of the underlying detector.
     */
    public void stop() {
        synchronized (mCameraLock) {
            mFrameProcessor.setActive(false);
            if (mProcessingThread != null) {
                try {
                    // Wait for the thread to complete to ensure that we can't have multiple threads
                    // executing at the same time (i.e., which would happen if we called start too
                    // quickly after stop).
                    mProcessingThread.join();
                } catch (InterruptedException e) {
                }
                mProcessingThread = null;
            }

            // clear the buffer to prevent oom exceptions
            mBytesToByteBuffer.clear();

            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallbackWithBuffer(null);
                try {
                    // We want to be compatible back to Gingerbread, but SurfaceTexture
                    // wasn't introduced until Honeycomb.  Since the interface cannot use a SurfaceTexture, if the
                    // developer wants to display a preview we must use a SurfaceHolder.  If the developer doesn't
                    // want to display a preview we use a SurfaceTexture if we are running at least Honeycomb.

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        mCamera.setPreviewTexture(null);

                    } else {
                        mCamera.setPreviewDisplay(null);
                    }
                } catch (Exception e) {
                }
                mCamera.release();
                mCamera = null;
            }
        }
    }

    /**
     * Returns the preview size that is currently in use by the underlying camera.
     */
    public Size getPreviewSize() {
        return mPreviewSize;
    }

    /**
     * Returns the selected camera; one of {@link #CAMERA_FACING_BACK} or
     * {@link #CAMERA_FACING_FRONT}.
     */
    public int getCameraFacing() {
        return mFacing;
    }

    public int doZoom(float scale) {
        synchronized (mCameraLock) {
            if (mCamera == null) {
                return 0;
            }
            int currentZoom = 0;
            int maxZoom;
            Camera.Parameters parameters = mCamera.getParameters();
            if (!parameters.isZoomSupported()) {
                return currentZoom;
            }
            maxZoom = parameters.getMaxZoom();

            currentZoom = parameters.getZoom() + 1;
            float newZoom;
            if (scale > 1) {
                newZoom = currentZoom + scale * (maxZoom / 10);
            } else {
                newZoom = currentZoom * scale;
            }
            currentZoom = Math.round(newZoom) - 1;
            if (currentZoom < 0) {
                currentZoom = 0;
            } else if (currentZoom > maxZoom) {
                currentZoom = maxZoom;
            }
            parameters.setZoom(currentZoom);
            mCamera.setParameters(parameters);
            return currentZoom;
        }
    }

    /**
     * Initiates taking a picture, which happens asynchronously.  The camera source should have been
     * activated previously with {@link #start()} or {@link #start(SurfaceHolder)}.  The camera
     * preview is suspended while the picture is being taken, but will resume once picture taking is
     * done.
     *
     * @param shutter the callback for image capture moment, or null
     * @param jpeg    the callback for JPEG image data, or null
     */
    public void takePicture(ShutterCallback shutter, PictureCallback jpeg) {
        synchronized (mCameraLock) {
            if (mCamera != null) {
                PictureStartCallback startCallback = new PictureStartCallback();
                startCallback.mDelegate = shutter;
                PictureDoneCallback doneCallback = new PictureDoneCallback();
                doneCallback.mDelegate = jpeg;
                mCamera.takePicture(startCallback, null, null, doneCallback);
            }
        }
    }

    /**
     * Gets the current focus mode setting.
     *
     * @return current focus mode. This value is null if the camera is not yet created. Applications should call {@link
     * #autoFocus(AutoFocusCallback)} to start the focus if focus
     * mode is FOCUS_MODE_AUTO or FOCUS_MODE_MACRO.
     * @see Camera.Parameters#FOCUS_MODE_AUTO
     * @see Camera.Parameters#FOCUS_MODE_INFINITY
     * @see Camera.Parameters#FOCUS_MODE_MACRO
     * @see Camera.Parameters#FOCUS_MODE_FIXED
     * @see Camera.Parameters#FOCUS_MODE_EDOF
     * @see Camera.Parameters#FOCUS_MODE_CONTINUOUS_VIDEO
     * @see Camera.Parameters#FOCUS_MODE_CONTINUOUS_PICTURE
     */
    @Nullable
    @FocusMode
    public String getFocusMode() {
        return mFocusMode;
    }

    /**
     * Sets the focus mode.
     *
     * @param mode the focus mode
     * @return {@code true} if the focus mode is set, {@code false} otherwise
     * @see #getFocusMode()
     */
    public boolean setFocusMode(@FocusMode String mode) {
        synchronized (mCameraLock) {
            if (mCamera != null && mode != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                if (parameters.getSupportedFocusModes().contains(mode)) {
                    parameters.setFocusMode(mode);
                    mCamera.setParameters(parameters);
                    mFocusMode = mode;
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Gets the current flash mode setting.
     *
     * @return current flash mode. null if flash mode setting is not
     * supported or the camera is not yet created.
     * @see Camera.Parameters#FLASH_MODE_OFF
     * @see Camera.Parameters#FLASH_MODE_AUTO
     * @see Camera.Parameters#FLASH_MODE_ON
     * @see Camera.Parameters#FLASH_MODE_RED_EYE
     * @see Camera.Parameters#FLASH_MODE_TORCH
     */
    @Nullable
    @FlashMode
    public String getFlashMode() {
        return mFlashMode;
    }

    /**
     * Sets the flash mode.
     *
     * @param mode flash mode.
     * @return {@code true} if the flash mode is set, {@code false} otherwise
     * @see #getFlashMode()
     */
    public boolean setFlashMode(@FlashMode String mode) {
        synchronized (mCameraLock) {
            if (mCamera != null && mode != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                if (parameters.getSupportedFlashModes().contains(mode)) {
                    parameters.setFlashMode(mode);
                    mCamera.setParameters(parameters);
                    mFlashMode = mode;
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Starts camera auto-focus and registers a callback function to run when
     * the camera is focused.  This method is only valid when preview is active
     * (between {@link #start()} or {@link #start(SurfaceHolder)} and before {@link #stop()} or {@link #release()}).
     * <p/>
     * <p>Callers should check
     * {@link #getFocusMode()} to determine if
     * this method should be called. If the camera does not support auto-focus,
     * it is a no-op and {@link AutoFocusCallback#onAutoFocus(boolean)}
     * callback will be called immediately.
     * <p/>
     * <p>If the current flash mode is not
     * {@link Camera.Parameters#FLASH_MODE_OFF}, flash may be
     * fired during auto-focus, depending on the driver and camera hardware.<p>
     *
     * @param cb the callback to run
     * @see #cancelAutoFocus()
     */
    public void autoFocus(@Nullable AutoFocusCallback cb) {
        synchronized (mCameraLock) {
            if (mCamera != null) {
                CameraAutoFocusCallback autoFocusCallback = null;
                if (cb != null) {
                    autoFocusCallback = new CameraAutoFocusCallback();
                    autoFocusCallback.mDelegate = cb;
                }
                mCamera.autoFocus(autoFocusCallback);
            }
        }
    }

    /**
     * Cancels any auto-focus function in progress.
     * Whether or not auto-focus is currently in progress,
     * this function will return the focus position to the default.
     * If the camera does not support auto-focus, this is a no-op.
     *
     * @see #autoFocus(AutoFocusCallback)
     */
    public void cancelAutoFocus() {
        synchronized (mCameraLock) {
            if (mCamera != null) {
                mCamera.cancelAutoFocus();
            }
        }
    }

    /**
     * Sets camera auto-focus move callback.
     *
     * @param cb the callback to run
     * @return {@code true} if the operation is supported (i.e. from Jelly Bean), {@code false} otherwise
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public boolean setAutoFocusMoveCallback(@Nullable AutoFocusMoveCallback cb) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return false;
        }

        synchronized (mCameraLock) {
            if (mCamera != null) {
                CameraAutoFocusMoveCallback autoFocusMoveCallback = null;
                if (cb != null) {
                    autoFocusMoveCallback = new CameraAutoFocusMoveCallback();
                    autoFocusMoveCallback.mDelegate = cb;
                }
                mCamera.setAutoFocusMoveCallback(autoFocusMoveCallback);
            }
        }

        return true;
    }

    //==============================================================================================
    // Private
    //==============================================================================================

    /**
     * Only allow creation via the builder class.
     */
    private CameraSource() {
    }

    /**
     * Wraps the camera1 shutter callback so that the deprecated API isn't exposed.
     */
    private class PictureStartCallback implements Camera.ShutterCallback {
        private ShutterCallback mDelegate;

        @Override
        public void onShutter() {
            if (mDelegate != null) {
                mDelegate.onShutter();
            }
        }
    }

    /**
     * Wraps the final callback in the camera sequence, so that we can automatically turn the camera
     * preview back on after the picture has been taken.
     */
    private class PictureDoneCallback implements Camera.PictureCallback {
        private PictureCallback mDelegate;

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (mDelegate != null) {
                mDelegate.onPictureTaken(data);
            }
            synchronized (mCameraLock) {
                if (mCamera != null) {
                    mCamera.startPreview();
                }
            }
        }
    }

    /**
     * Wraps the camera1 auto focus callback so that the deprecated API isn't exposed.
     */
    private class CameraAutoFocusCallback implements Camera.AutoFocusCallback {
        private AutoFocusCallback mDelegate;

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (mDelegate != null) {
                mDelegate.onAutoFocus(success);
            }
        }
    }

    /**
     * Wraps the camera1 auto focus move callback so that the deprecated API isn't exposed.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private class CameraAutoFocusMoveCallback implements Camera.AutoFocusMoveCallback {
        private AutoFocusMoveCallback mDelegate;

        @Override
        public void onAutoFocusMoving(boolean start, Camera camera) {
            if (mDelegate != null) {
                mDelegate.onAutoFocusMoving(start);
            }
        }
    }

    /**
     * Opens the camera and applies the user settings.
     *
     * @throws RuntimeException if the method fails
     */
    @SuppressLint("InlinedApi")
    private Camera createCamera() {
        int requestedCameraId = getIdForRequestedCamera(mFacing);
        if (requestedCameraId == -1) {
            throw new RuntimeException("Could not find requested camera.");
        }
        Camera camera = Camera.open(requestedCameraId);

        SizePair sizePair = selectSizePair(camera, mRequestedPreviewWidth, mRequestedPreviewHeight);
        if (sizePair == null) {
            throw new RuntimeException("Could not find suitable preview size.");
        }
        Size pictureSize = sizePair.pictureSize();
        mPreviewSize = sizePair.previewSize();

        int[] previewFpsRange = selectPreviewFpsRange(camera, mRequestedFps);
        if (previewFpsRange == null) {
            throw new RuntimeException("Could not find suitable preview frames per second range.");
        }

        Camera.Parameters parameters = camera.getParameters();

        if (pictureSize != null) {
            parameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        }
        parameters.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        parameters.setPreviewFpsRange(
                previewFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                previewFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
        parameters.setPreviewFormat(ImageFormat.NV21);

        setRotation(camera, parameters, requestedCameraId);

        if (mFocusMode != null) {
            if (parameters.getSupportedFocusModes().contains(
                    mFocusMode)) {
                parameters.setFocusMode(mFocusMode);
            } else {
            }
        }

        // setting mFocusMode to the one set in the params
        mFocusMode = parameters.getFocusMode();

        if (mFlashMode != null) {
            if (parameters.getSupportedFlashModes() != null) {
                if (parameters.getSupportedFlashModes().contains(
                        mFlashMode)) {
                    parameters.setFlashMode(mFlashMode);
                }
            }
        }

        // setting mFlashMode to the one set in the params
        mFlashMode = parameters.getFlashMode();

        camera.setParameters(parameters);

        // Four frame buffers are needed for working with the camera:
        //
        //   one for the frame that is currently being executed upon in doing detection
        //   one for the next pending frame to process immediately upon completing detection
        //   two for the frames that the camera uses to populate future preview images
        camera.setPreviewCallbackWithBuffer(new CameraPreviewCallback());
        camera.addCallbackBuffer(createPreviewBuffer(mPreviewSize));
        camera.addCallbackBuffer(createPreviewBuffer(mPreviewSize));
        camera.addCallbackBuffer(createPreviewBuffer(mPreviewSize));
        camera.addCallbackBuffer(createPreviewBuffer(mPreviewSize));

        return camera;
    }

    /**
     * Gets the id for the camera specified by the direction it is facing.  Returns -1 if no such
     * camera was found.
     *
     * @param facing the desired camera (front-facing or rear-facing)
     */
    private static int getIdForRequestedCamera(int facing) {
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == facing) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Selects the most suitable preview and picture size, given the desired width and height.
     * <p/>
     * Even though we may only need the preview size, it's necessary to find both the preview
     * size and the picture size of the camera together, because these need to have the same aspect
     * ratio.  On some hardware, if you would only set the preview size, you will get a distorted
     * image.
     *
     * @param camera        the camera to select a preview size from
     * @param desiredWidth  the desired width of the camera preview frames
     * @param desiredHeight the desired height of the camera preview frames
     * @return the selected preview and picture size pair
     */
    private static SizePair selectSizePair(Camera camera, int desiredWidth, int desiredHeight) {
        List<SizePair> validPreviewSizes = generateValidPreviewSizeList(camera);

        // The method for selecting the best size is to minimize the sum of the differences between
        // the desired values and the actual values for width and height.  This is certainly not the
        // only way to select the best size, but it provides a decent tradeoff between using the
        // closest aspect ratio vs. using the closest pixel area.
        SizePair selectedPair = null;
        int minDiff = Integer.MAX_VALUE;
        for (SizePair sizePair : validPreviewSizes) {
            Size size = sizePair.previewSize();
            int diff = Math.abs(size.getWidth() - desiredWidth) +
                    Math.abs(size.getHeight() - desiredHeight);
            if (diff < minDiff) {
                selectedPair = sizePair;
                minDiff = diff;
            }
        }

        return selectedPair;
    }

    /**
     * Stores a preview size and a corresponding same-aspect-ratio picture size.  To avoid distorted
     * preview images on some devices, the picture size must be set to a size that is the same
     * aspect ratio as the preview size or the preview may end up being distorted.  If the picture
     * size is null, then there is no picture size with the same aspect ratio as the preview size.
     */
    private static class SizePair {
        private Size mPreview;
        private Size mPicture;

        public SizePair(android.hardware.Camera.Size previewSize,
                        android.hardware.Camera.Size pictureSize) {
            mPreview = new Size(previewSize.width, previewSize.height);
            if (pictureSize != null) {
                mPicture = new Size(pictureSize.width, pictureSize.height);
            }
        }

        public Size previewSize() {
            return mPreview;
        }

        @SuppressWarnings("unused")
        public Size pictureSize() {
            return mPicture;
        }
    }

    private static List<SizePair> generateValidPreviewSizeList(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> supportedPreviewSizes =
                parameters.getSupportedPreviewSizes();
        List<Camera.Size> supportedPictureSizes =
                parameters.getSupportedPictureSizes();
        List<SizePair> validPreviewSizes = new ArrayList<>();
        for (android.hardware.Camera.Size previewSize : supportedPreviewSizes) {
            float previewAspectRatio = (float) previewSize.width / (float) previewSize.height;

            for (android.hardware.Camera.Size pictureSize : supportedPictureSizes) {
                float pictureAspectRatio = (float) pictureSize.width / (float) pictureSize.height;
                if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
                    validPreviewSizes.add(new SizePair(previewSize, pictureSize));
                    break;
                }
            }
        }

        if (validPreviewSizes.size() == 0) {
            for (android.hardware.Camera.Size previewSize : supportedPreviewSizes) {
                validPreviewSizes.add(new SizePair(previewSize, null));
            }
        }

        return validPreviewSizes;
    }

    private int[] selectPreviewFpsRange(Camera camera, float desiredPreviewFps) {
        int desiredPreviewFpsScaled = (int) (desiredPreviewFps * 1000.0f);

        int[] selectedFpsRange = null;
        int minDiff = Integer.MAX_VALUE;
        List<int[]> previewFpsRangeList = camera.getParameters().getSupportedPreviewFpsRange();
        for (int[] range : previewFpsRangeList) {
            int deltaMin = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
            int deltaMax = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
            int diff = Math.abs(deltaMin) + Math.abs(deltaMax);
            if (diff < minDiff) {
                selectedFpsRange = range;
                minDiff = diff;
            }
        }
        return selectedFpsRange;
    }

    private void setRotation(Camera camera, Camera.Parameters parameters, int cameraId) {
        WindowManager windowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int degrees = 0;
        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
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
            default:

        }

        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        int angle;
        int displayAngle;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            angle = (cameraInfo.orientation + degrees) % 360;
            displayAngle = (360 - angle) % 360; // compensate for it being mirrored
        } else {  // back-facing
            angle = (cameraInfo.orientation - degrees + 360) % 360;
            displayAngle = angle;
        }

        mRotation = angle / 90;

        camera.setDisplayOrientation(displayAngle);
        parameters.setRotation(angle);
    }

    private byte[] createPreviewBuffer(Size previewSize) {
        int bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21);
        long sizeInBits = previewSize.getHeight() * previewSize.getWidth() * bitsPerPixel;
        int bufferSize = (int) Math.ceil(sizeInBits / 8.0d) + 1;

        byte[] byteArray = new byte[bufferSize];
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        if (!buffer.hasArray() || (buffer.array() != byteArray)) {
            throw new IllegalStateException("Failed to create valid buffer for camera source.");
        }

        mBytesToByteBuffer.put(byteArray, buffer);
        return byteArray;
    }

    private class CameraPreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            mFrameProcessor.setNextFrame(data, camera);
        }
    }

    private class FrameProcessingRunnable implements Runnable {
        private Detector<?> mDetector;
        private long mStartTimeMillis = SystemClock.elapsedRealtime();

        // This lock guards all of the member variables below.
        private final Object mLock = new Object();
        private boolean mActive = true;

        // These pending variables hold the state associated with the new frame awaiting processing.
        private long mPendingTimeMillis;
        private int mPendingFrameId = 0;
        private ByteBuffer mPendingFrameData;

        FrameProcessingRunnable(Detector<?> detector) {
            mDetector = detector;
        }

        @SuppressLint("Assert")
        void release() {
            assert (mProcessingThread == null || mProcessingThread.getState() == State.TERMINATED);
            if (mDetector != null) {
                mDetector.release();
                mDetector = null;
            }
        }

        void setActive(boolean active) {
            synchronized (mLock) {
                mActive = active;
                mLock.notifyAll();
            }
        }

        void setNextFrame(byte[] data, Camera camera) {
            synchronized (mLock) {
                if (mPendingFrameData != null) {
                    camera.addCallbackBuffer(mPendingFrameData.array());
                    mPendingFrameData = null;
                }

                if (!mBytesToByteBuffer.containsKey(data)) {
                    return;
                }

                mPendingTimeMillis = SystemClock.elapsedRealtime() - mStartTimeMillis;
                mPendingFrameId++;
                mPendingFrameData = mBytesToByteBuffer.get(data);

                mLock.notifyAll();
            }
        }

        @Override
        public void run() {
            Frame outputFrame;
            ByteBuffer data;

            while (true) {
                synchronized (mLock) {
                    while (mActive && (mPendingFrameData == null)) {
                        try {
                            mLock.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    if (!mActive) {
                        return;
                    }

                    outputFrame = new Frame.Builder()
                            .setImageData(mPendingFrameData, mPreviewSize.getWidth(),
                                    mPreviewSize.getHeight(), ImageFormat.NV21)
                            .setId(mPendingFrameId)
                            .setTimestampMillis(mPendingTimeMillis)
                            .setRotation(mRotation)
                            .build();

                    data = mPendingFrameData;
                    mPendingFrameData = null;
                }


                try {
                    mDetector.receiveFrame(outputFrame);
                } catch (Throwable t) {
                } finally {
                    mCamera.addCallbackBuffer(data.array());
                }
            }
        }
    }

    public Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
}
