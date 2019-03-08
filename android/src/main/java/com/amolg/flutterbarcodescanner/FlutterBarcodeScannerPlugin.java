package com.amolg.flutterbarcodescanner;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.Map;

import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;

/**
 * FlutterBarcodeScannerPlugin
 */
public class FlutterBarcodeScannerPlugin implements MethodCallHandler, ActivityResultListener, PluginRegistry.RequestPermissionsResultListener {
    private static final String CHANNEL = "flutter_barcode_scanner";
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 3777;
    private static FlutterBarcodeScannerPlugin instance;

    private FlutterActivity activity;
    private Result pendingResult;
    private Map<String, Object> arguments;
    private boolean executeAfterPermissionGranted;

    private static final String TAG = FlutterBarcodeScannerPlugin.class.getSimpleName();
    private static final int RC_BARCODE_CAPTURE = 9001;
    public static String lineColor = "";

    public FlutterBarcodeScannerPlugin(FlutterActivity activity) {
        this.activity = activity;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(PluginRegistry.Registrar registrar) {
        if (instance == null) {
            final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL);
            instance = new FlutterBarcodeScannerPlugin((FlutterActivity) registrar.activity());
            registrar.addActivityResultListener(instance);
            registrar.addRequestPermissionsResultListener(instance);
            channel.setMethodCallHandler(instance);
        }
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        try {
            pendingResult = result;

            if (call.method.equals("scanBarcode")) {
                if (!(call.arguments instanceof Map)) {
                    throw new IllegalArgumentException("Plugin not passing a map as parameter: " + call.arguments);
                }
                arguments = (Map<String, Object>) call.arguments;
                boolean handlePermission = (boolean) arguments.get("handlePermissions");
                this.executeAfterPermissionGranted = (boolean) arguments.get("executeAfterPermissionGranted");

                if (checkSelfPermission(activity,
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(activity,
                            Manifest.permission.CAMERA)) {
                        if (handlePermission) {
                            requestPermissions();
                        } else {
                            setNoPermissionsError();
                        }
                    } else {
                        if (handlePermission) {
                            requestPermissions();
                        } else {
                            setNoPermissionsError();
                        }
                    }
                } else {
                    lineColor = (String) arguments.get("lineColor");
                    startBarcodeScannerActivityView();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onMethodCall: " + e.getLocalizedMessage());
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissions() {
        activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA_PERMISSION);
    }

    private boolean shouldShowRequestPermissionRationale(Activity activity,
                                                         String permission) {
        if (Build.VERSION.SDK_INT >= 23) {
            return activity.shouldShowRequestPermissionRationale(permission);
        }
        return false;
    }

    private int checkSelfPermission(Context context, String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }
        return context.checkPermission(permission, android.os.Process.myPid(), Process.myUid());
    }


    private void startBarcodeScannerActivityView() {
        try {
            Intent intent = new Intent(activity, BarcodeCaptureActivity.class);
            activity.startActivityForResult(intent, RC_BARCODE_CAPTURE);
        } catch (Exception e) {
            Log.e(TAG, "startView: " + e.getLocalizedMessage());
        }
    }


    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.CAMERA)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        if (executeAfterPermissionGranted) {
                            startBarcodeScannerActivityView();
                        }
                    } else {
                        setNoPermissionsError();
                    }
                }
            }
        }
        return false;
    }

    private void setNoPermissionsError() {
        pendingResult.error("permission", "you don't have the user permission to access the camera", null);
        pendingResult = null;
        arguments = null;
    }

    /**
     * Get the barcode scanning results in onActivityResult
     *
     * @param requestCode
     * @param resultCode
     * @param data
     * @return
     */
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    try {
                        Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                        String barcodeResult = barcode.displayValue;
                        pendingResult.success(barcodeResult);
                    } catch (Exception e) {
                        pendingResult.success("");
                    }
                } else {
                    pendingResult.success("");
                }
                pendingResult = null;
                arguments = null;
                return true;
            } else {
                pendingResult.success("");
            }
        }
        return false;
    }
}