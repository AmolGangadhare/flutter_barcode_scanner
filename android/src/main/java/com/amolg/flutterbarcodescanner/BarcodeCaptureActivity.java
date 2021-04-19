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
package com.amolg.flutterbarcodescanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import com.amolg.flutterbarcodescanner.camera.CameraSource;
import com.amolg.flutterbarcodescanner.camera.CameraSourcePreview;
import com.amolg.flutterbarcodescanner.camera.GraphicOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

/**
 * Activity for the multi-tracker app.  This app detects barcodes and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and ID of each barcode.
 */
public final class BarcodeCaptureActivity extends AppCompatActivity implements BarcodeGraphicTracker.BarcodeUpdateListener, View.OnClickListener {

    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    // constants used to pass extra data in the intent
    public static final String BarcodeObject = "Barcode";

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;

    // helper objects for detecting taps and pinches.
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    private ImageView imgViewBarcodeCaptureUseFlash;
    private ImageView imgViewSwitchCamera;

    public static int SCAN_MODE = SCAN_MODE_ENUM.QR.ordinal();

    public enum SCAN_MODE_ENUM {
        QR,
        BARCODE,
        DEFAULT
    }

    enum USE_FLASH {
        ON,
        OFF
    }

    private int flashStatus = USE_FLASH.OFF.ordinal();

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        try {
            setContentView(R.layout.barcode_capture);

            String buttonText = "";
            try {
                    buttonText = (String) getIntent().getStringExtra("cancelButtonText");
        } catch (Exception e) {
            buttonText = "Cancel";
            Log.e("BCActivity:onCreate()", "onCreate: " + e.getLocalizedMessage());
        }

        Button btnBarcodeCaptureCancel = findViewById(R.id.btnBarcodeCaptureCancel);
        btnBarcodeCaptureCancel.setText(buttonText);
        btnBarcodeCaptureCancel.setOnClickListener(this);

        imgViewBarcodeCaptureUseFlash = findViewById(R.id.imgViewBarcodeCaptureUseFlash);
        imgViewBarcodeCaptureUseFlash.setOnClickListener(this);
        imgViewBarcodeCaptureUseFlash.setVisibility(FlutterBarcodeScannerPlugin.isShowFlashIcon ? View.VISIBLE : View.GONE);

        imgViewSwitchCamera = findViewById(R.id.imgViewSwitchCamera);
        imgViewSwitchCamera.setOnClickListener(this);

        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.graphicOverlay);

        // read parameters from the intent used to launch the activity.
        boolean autoFocus = true;
        boolean useFlash = false;

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (rc == PackageManager.PERMISSION_GRANTED) {
                createCameraSource(autoFocus, useFlash, CameraSource.CAMERA_FACING_BACK);
            } else {
                requestCameraPermission();
            }

            gestureDetector = new GestureDetector(this, new CaptureGestureListener());
            scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        } catch (Exception e) {
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        findViewById(R.id.topLayout).setOnClickListener(listener);
        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean b = scaleGestureDetector.onTouchEvent(e);

        boolean c = gestureDetector.onTouchEvent(e);

        return b || c || super.onTouchEvent(e);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     * <p>
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash, int cameraFacing) {
        Context context = getApplicationContext();

        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay, this);
        barcodeDetector.setProcessor(
                new MultiProcessor.Builder<>(barcodeFactory).build());

        if (!barcodeDetector.isOperational()) {
            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), barcodeDetector)
                .setFacing(cameraFacing)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(30.0f)
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null);

        // make sure that auto focus is an available option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(
                    autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
        }

        // Stop & release current camera source before creating a new one.
        if (mCameraSource != null) {
            mCameraSource.stop();
            mCameraSource.release();
        }
        mCameraSource = builder.build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // we have permission, so create the camerasource
            boolean autoFocus = true;
            boolean useFlash = false;
            createCameraSource(autoFocus, useFlash, CameraSource.CAMERA_FACING_BACK);
            return;
        }

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Allow permissions")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                mCameraSource.release();
                mCameraSource = null;
            }
        }
        System.gc();
    }

    /**
     * onTap returns the tapped barcode result to the calling Activity.
     *
     * @param rawX - the raw position of the tap
     * @param rawY - the raw position of the tap.
     * @return true if the activity is ending.
     */
    private boolean onTap(float rawX, float rawY) {
        // Find tap point in preview frame coordinates.
        int[] location = new int[2];
        mGraphicOverlay.getLocationOnScreen(location);
        float x = (rawX - location[0]) / mGraphicOverlay.getWidthScaleFactor();
        float y = (rawY - location[1]) / mGraphicOverlay.getHeightScaleFactor();

        // Find the barcode whose center is closest to the tapped point.
        Barcode best = null;
        float bestDistance = Float.MAX_VALUE;
        for (BarcodeGraphic graphic : mGraphicOverlay.getGraphics()) {
            Barcode barcode = graphic.getBarcode();
            if (barcode.getBoundingBox().contains((int) x, (int) y)) {
                // Exact hit, no need to keep looking.
                best = barcode;
                break;
            }
            float dx = x - barcode.getBoundingBox().centerX();
            float dy = y - barcode.getBoundingBox().centerY();
            float distance = (dx * dx) + (dy * dy);  // actually squared distance
            if (distance < bestDistance) {
                best = barcode;
                bestDistance = distance;
            }
        }

        if (best != null) {
            Intent data = new Intent();
            data.putExtra(BarcodeObject, best);
            setResult(CommonStatusCodes.SUCCESS, data);
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.imgViewBarcodeCaptureUseFlash &&
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            try {
                if (flashStatus == USE_FLASH.OFF.ordinal()) {
                    flashStatus = USE_FLASH.ON.ordinal();
                    imgViewBarcodeCaptureUseFlash.setImageResource(R.drawable.ic_barcode_flash_on);
                    turnOnOffFlashLight(true);
                } else {
                    flashStatus = USE_FLASH.OFF.ordinal();
                    imgViewBarcodeCaptureUseFlash.setImageResource(R.drawable.ic_barcode_flash_off);
                    turnOnOffFlashLight(false);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Unable to turn on flash", Toast.LENGTH_SHORT).show();
                Log.e("BarcodeCaptureActivity", "FlashOnFailure: " + e.getLocalizedMessage());
            }
        } else if (i == R.id.btnBarcodeCaptureCancel) {
            Barcode barcode = new Barcode();
            barcode.rawValue = "-1";
            barcode.displayValue = "-1";
            FlutterBarcodeScannerPlugin.onBarcodeScanReceiver(barcode);
            finish();
        } else if (i == R.id.imgViewSwitchCamera) {
            int currentFacing = mCameraSource.getCameraFacing();
            boolean autoFocus = mCameraSource.getFocusMode() != null;
            boolean useFlash = flashStatus == USE_FLASH.ON.ordinal();
            createCameraSource(autoFocus, useFlash, getInverseCameraFacing(currentFacing));
            startCameraSource();
        }
    }

    private int getInverseCameraFacing(int cameraFacing) {
        if (cameraFacing == CameraSource.CAMERA_FACING_FRONT) {
            return CameraSource.CAMERA_FACING_BACK;
        }

        if (cameraFacing == CameraSource.CAMERA_FACING_BACK) {
            return CameraSource.CAMERA_FACING_FRONT;
        }

        // Fallback to camera at the back.
        return CameraSource.CAMERA_FACING_BACK;
    }

    /**
     * Turn on and off flash light based on flag
     *
     * @param isFlashToBeTurnOn
     */
    private void turnOnOffFlashLight(boolean isFlashToBeTurnOn) {
        try {
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                String flashMode = "";
                flashMode = isFlashToBeTurnOn ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF;

                mCameraSource.setFlashMode(flashMode);
            } else {
                Toast.makeText(getBaseContext(), "Unable to access flashlight as flashlight not available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "Unable to access flashlight.", Toast.LENGTH_SHORT).show();
        }
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mCameraSource.doZoom(detector.getScaleFactor());
        }
    }

    @Override
    public void onBarcodeDetected(Barcode barcode) {
        if (null != barcode) {
            if (FlutterBarcodeScannerPlugin.isContinuousScan) {
                FlutterBarcodeScannerPlugin.onBarcodeScanReceiver(barcode);
            } else {
                Intent data = new Intent();
                data.putExtra(BarcodeObject, barcode);
                setResult(CommonStatusCodes.SUCCESS, data);
                finish();
            }
        }
    }
}