package com.amolg.flutterbarcodescanner;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.Map;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter;

public class FlutterBarcodeScannerPlugin implements MethodCallHandler, StreamHandler, FlutterPlugin, ActivityAware {
    private static final String CHANNEL = "flutter_barcode_scanner";
    private static final String TAG = FlutterBarcodeScannerPlugin.class.getSimpleName();
    private static final int RC_BARCODE_CAPTURE = 9001;

    // Configuration properties
    public static String lineColor = "";
    public static boolean isShowFlashIcon = false;
    public static boolean isContinuousScan = false;

    // Flutter communication channels
    private MethodChannel channel;
    private EventChannel eventChannel;
    private static EventChannel.EventSink barcodeStream;

    // Activity and binding references
    private static FlutterActivity activity;
    private static Result pendingResult;
    private Map<String, Object> arguments;
    private FlutterPluginBinding pluginBinding;
    private ActivityPluginBinding activityBinding;
    private Application applicationContext;
    private Lifecycle lifecycle;
    private LifeCycleObserver observer;

    public FlutterBarcodeScannerPlugin() {}

    // FlutterPlugin implementation
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        pluginBinding = binding;
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        pluginBinding = null;
    }

    // ActivityAware implementation
    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activityBinding = binding;
        setupPlugin(
            pluginBinding.getBinaryMessenger(),
            (Application) pluginBinding.getApplicationContext(),
            binding
        );
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        tearDownPlugin();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        tearDownPlugin();
    }

    // Plugin setup and teardown
    private void setupPlugin(
            BinaryMessenger messenger,
            Application applicationContext,
            ActivityPluginBinding binding) {
        
        this.activity = (FlutterActivity) binding.getActivity();
        this.applicationContext = applicationContext;
        
        // Setup event channel for continuous scanning
        eventChannel = new EventChannel(messenger, "flutter_barcode_scanner_receiver");
        eventChannel.setStreamHandler(this);
        
        // Setup method channel for method calls
        channel = new MethodChannel(messenger, CHANNEL);
        channel.setMethodCallHandler(this);
        
        // Register activity result listener
        binding.addActivityResultListener((requestCode, resultCode, data) -> {
            if (requestCode == RC_BARCODE_CAPTURE) {
                handleBarcodeResult(resultCode, data);
                return true;
            }
            return false;
        });
        
        // Setup lifecycle observer
        lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding);
        observer = new LifeCycleObserver(binding.getActivity());
        lifecycle.addObserver(observer);
    }

    private void tearDownPlugin() {
        if (activityBinding != null) {
            activityBinding.removeActivityResultListener(this);
        }
        
        if (lifecycle != null && observer != null) {
            lifecycle.removeObserver(observer);
        }
        
        if (channel != null) {
            channel.setMethodCallHandler(null);
        }
        
        if (eventChannel != null) {
            eventChannel.setStreamHandler(null);
        }
        
        if (applicationContext != null && observer != null) {
            applicationContext.unregisterActivityLifecycleCallbacks(observer);
        }
        
        // Clear all references
        activity = null;
        activityBinding = null;
        lifecycle = null;
        applicationContext = null;
        pendingResult = null;
        arguments = null;
    }

    // MethodCallHandler implementation
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        try {
            pendingResult = result;

            if (call.method.equals("scanBarcode")) {
                if (!(call.arguments instanceof Map)) {
                    throw new IllegalArgumentException("Plugin not passing a map as parameter: " + call.arguments);
                }
                
                arguments = (Map<String, Object>) call.arguments;
                configureScannerSettings();
                startBarcodeScannerActivity();
            }
        } catch (Exception e) {
            Log.e(TAG, "onMethodCall error: ", e);
            result.error("ERROR", e.getMessage(), null);
        }
    }

    private void configureScannerSettings() {
        lineColor = (String) arguments.get("lineColor");
        isShowFlashIcon = (boolean) arguments.get("isShowFlashIcon");
        isContinuousScan = (boolean) arguments.get("isContinuousScan");

        // Set default line color if not provided
        if (lineColor == null || lineColor.isEmpty()) {
            lineColor = "#DC143C";
        }

        // Configure scan mode
        if (arguments.get("scanMode") != null) {
            if ((int) arguments.get("scanMode") == BarcodeCaptureActivity.SCAN_MODE_ENUM.DEFAULT.ordinal()) {
                BarcodeCaptureActivity.SCAN_MODE = BarcodeCaptureActivity.SCAN_MODE_ENUM.QR.ordinal();
            } else {
                BarcodeCaptureActivity.SCAN_MODE = (int) arguments.get("scanMode");
            }
        } else {
            BarcodeCaptureActivity.SCAN_MODE = BarcodeCaptureActivity.SCAN_MODE_ENUM.QR.ordinal();
        }
    }

    private void startBarcodeScannerActivity() {
        try {
            String buttonText = (String) arguments.get("cancelButtonText");
            Intent intent = new Intent(activity, BarcodeCaptureActivity.class)
                .putExtra("cancelButtonText", buttonText);
            
            if (isContinuousScan) {
                activity.startActivity(intent);
            } else {
                activity.startActivityForResult(intent, RC_BARCODE_CAPTURE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting barcode scanner: ", e);
            if (pendingResult != null) {
                pendingResult.error("ERROR", e.getMessage(), null);
                pendingResult = null;
            }
        }
    }

    private void handleBarcodeResult(int resultCode, Intent data) {
        if (pendingResult == null) return;

        if (resultCode == CommonStatusCodes.SUCCESS) {
            try {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    pendingResult.success(barcode != null ? barcode.rawValue : "-1");
                } else {
                    pendingResult.success("-1");
                }
            } catch (Exception e) {
                pendingResult.success("-1");
            }
        } else {
            pendingResult.success("-1");
        }
        
        pendingResult = null;
        arguments = null;
    }

    // StreamHandler implementation for continuous scanning
    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        barcodeStream = events;
    }

    @Override
    public void onCancel(Object arguments) {
        barcodeStream = null;
    }

    // Static method to receive barcode results from continuous scan
    public static void onBarcodeScanReceived(final Barcode barcode) {
        if (activity != null && barcode != null && barcode.displayValue != null && !barcode.displayValue.isEmpty()) {
            activity.runOnUiThread(() -> {
                if (barcodeStream != null) {
                    barcodeStream.success(barcode.rawValue);
                }
            });
        }
    }

    // Lifecycle observer class
    private class LifeCycleObserver implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
        private final Activity observedActivity;

        LifeCycleObserver(Activity activity) {
            this.observedActivity = activity;
        }

        // DefaultLifecycleObserver methods
        @Override
        public void onCreate(@NonNull LifecycleOwner owner) {}
        @Override
        public void onStart(@NonNull LifecycleOwner owner) {}
        @Override
        public void onResume(@NonNull LifecycleOwner owner) {}
        @Override
        public void onPause(@NonNull LifecycleOwner owner) {}
        @Override
        public void onStop(@NonNull LifecycleOwner owner) {}
        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            onActivityDestroyed(observedActivity);
        }

        // ActivityLifecycleCallbacks methods
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
        @Override
        public void onActivityStarted(Activity activity) {}
        @Override
        public void onActivityResumed(Activity activity) {}
        @Override
        public void onActivityPaused(Activity activity) {}
        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
        @Override
        public void onActivityDestroyed(Activity activity) {
            if (observedActivity == activity && activity.getApplicationContext() != null) {
                ((Application) activity.getApplicationContext())
                    .unregisterActivityLifecycleCallbacks(this);
            }
        }
        @Override
        public void onActivityStopped(Activity activity) {}
    }
}
