# flutter_barcode_scanner

A flutter plugin supports barcode scanning on both Android and iOS.

![alt text](https://github.com/AmolGangadhare/MyProfileRepo/blob/master/flutter_barcode_scanning_demo.gif "Demo")


## Try example
Just clone the repository, open the project in `Android Studio/ VS Code`, open `pubspec.yaml` and click on `Packages get`.
Connect device and hit `run`. To run on iPhone you need to run from `Xcode first time` and just make `pod install` in `example/ios`.

## Getting Started 
### Android
To use on android, you need to add some permissions and a BarcodeCaptureActivity to AndroidManifest.
1. Add the camera permission to your AndroidManifest.xml

    <uses-permission android:name="android.permission.CAMERA" />

2. Add the BarcodeScanner activity to your AndroidManifest.xml. Do NOT modify the name.

    <activity android:name="com.amolg.flutterbarcodescanner.BarcodeCaptureActivity" />

### iOS

To use on iOS,open the Xcode and add camera usage description in Info.plist

    <key>NSCameraUsageDescription</key>
    <string>Camera permission is required for barcode scanning.</string>

## How to use ?

After making the changes in Android ans iOS add flutter_barcode_scanner to `pubspec.yaml`
    
    dependencies:
      ...
      flutter_barcode_scanner: ^0.0.1

1. You need to import the package first.

    `import 'package:flutter_barcode_scanner/flutter_barcode_scanner.dart';`
    
2. Then use the `scanBarcode` method to access barcode scanning.
    
    `String barcodeScanRes = await FlutterBarcodeScanner.scanBarcode(COLOR_CODE);`

Here you can pass color which is the line color in barcode overlay.

Special thanks to [@Harshal Jadhav](https://github.com/harshalrj25) for help in iOS


## About me ..
 
My email id, amol.gangadhare@gmail.com
 
<table style="background-color:#F5F5DC">
<tr>
<td> 
Amol Gangadhare
 
<p align="center">
<a href = "https://github.com/AmolGangadhare"><img src = "https://github.com/harshalrj25/MasterAssetsRepo/blob/master/gitHubLogo.png" width="32" height = "33"/></a>
<a href = "https://stackoverflow.com/users/9823185/amol-g?tab=profile"><img src = "https://github.com/harshalrj25/MasterAssetsRepo/blob/master/stackoverflow svg icon.svg" width="36" height="36"/></a>
<a href = "https://www.linkedin.com/in/amolgangadhare/"><img src = "https://github.com/harshalrj25/MasterAssetsRepo/blob/master/linkedInLogo.svg" width="36" height="36"/></a>
</p>
</td>
</tr> 
</table>