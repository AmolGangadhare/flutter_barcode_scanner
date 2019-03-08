# flutter_barcode_scanner

A new Flutter plugin supports barcode scanning on both Android and iOS.

## Try example
Just clone the repository, open the project in `Android Studio/ VS Code`, open `pubspec.yaml` and click on `Packages get`.
Connect device and hit `run`. To run on iPhone you need to run from `Xcode first time`.

## Getting Started

### How to use ?

## Android
To use on android, you need to add some permissions and a BarcodeCaptureActivity to AndroidManifest.
1. Add the camera permission to your AndroidManifest.xml

    <uses-permission android:name="android.permission.CAMERA" />

2. Add the BarcodeScanner activity to your AndroidManifest.xml. Do NOT modify the name.

    <activity android:name="com.amolg.flutterbarcodescanner.BarcodeCaptureActivity" />

## iOS

To use on iOS,open the Xcode and add camera usage description in Info.plist

    <key>NSCameraUsageDescription</key>
    <string>Camera permission is required for barcode scanning.</string>
