# flutter_barcode_scanner

A plugin for Flutter apps that adds barcode scanning support on both Android and iOS.

[![pub package](https://img.shields.io/pub/v/flutter_barcode_scanner.svg)](https://pub.dartlang.org/packages/flutter_barcode_scanner)

![Demo gif](https://github.com/AmolGangadhare/MyProfileRepo/blob/master/flutter_barcode_scanning_demo.gif "Demo")


## Try example
Just clone or download the repository, open the project in `Android Studio/ VS Code`, open `pubspec.yaml` and click on `Packages get`.
Connect device and hit `run`. 
To run on iPhone you need to run from Xcode first time and just make `pod install` in `example/ios` then run from Xcode.

## Getting Started 
Follow the steps for Android and iOS

PLEASE FOLLOW **iOS** STEPS CAREFULLY

### Android

:zap:  Don't worry, you don't need to do anything.

### iOS - Requires Swift support

Deployment target : 12

#### 1. Fresh start: 
 1. Create a new flutter project. Please check for **Include swift support for iOS code**.
 2. After creating new flutter project open `/ios` project in Xcode and set minimum **deployment target to 12**
    and set **Swift version to 5**.
 3. After setting up the deployment target and swift version, close the Xcode then run **pod install** in `/ios` in flutter project.
 
 You have done with basic configuration now proceed to section **How to use**.
 
#### 2. Adding to existing flutter app: 
#### If your existing ios code is **Swift** then you just need to do following.
  1. Set **minimum deployment target to 12** and set **Swift version to 5**.
  2. Close the Xcode and run **pod install** in `/ios` in flutter project.
  3. Now proceed to section **How to use**.
 
#### If your existing ios code is **Objective-C** then you need to do following.
  1. Create a new flutter project with same name at different location (Don't forget to check **Include swift support for iOS code** while creating) 
  2. Just copy newly created `/ios` folder from project and replace with existing `/ios`.
  3. Open ios project in Xcode and set **minimum deployment target to 12** and set **Swift version to 5**.
  4. Run **pod install** in `/ios` 
    
**Note: If you did any changes in ios part before, you might need to make these configuration again**

## How to use ?

To use on iOS, you will need to add the camera usage description.
To do that open the Xcode and add camera usage description in `Info.plist`. 

```
<key>NSCameraUsageDescription</key>
<string>Camera permission is required for barcode scanning.</string>
```


After making the changes in Android ans iOS add flutter_barcode_scanner to `pubspec.yaml`
```  
dependencies:
  ...
  flutter_barcode_scanner: ^2.0.0
```

### One time scan
1. You need to import the package first.

```
import 'package:flutter_barcode_scanner/flutter_barcode_scanner.dart';
```

    
2. Then use the `scanBarcode` method to access barcode scanning.
    
```
String barcodeScanRes = await FlutterBarcodeScanner.scanBarcode(
                                                    COLOR_CODE, 
                                                    CANCEL_BUTTON_TEXT, 
                                                    isShowFlashIcon, 
                                                    scanMode);
```

Here in `scanBarcode`,

 `COLOR_CODE` is hex-color which is the color of line in barcode overlay you can pass color of your choice,
 
 `CANCEL_BUTTON_TEXT` is a text of cancel button on screen you can pass text of your choice and language,
 
 `isShowFlashIcon` is bool value used to show or hide the flash icon,
 
 `scanMode` is a enum in which user can pass any of `{ QR, BARCODE, DEFAULT }`, if nothing is passed it will consider a default value which will be `QR`.
 It shows the graphics overlay like for barcode and QR.
 
 NOTE: Currently, `scanMode` is just to show the graphics overlay for barcode and QR. Any of this mode selected will scan both QR and barcode. 

### Continuous scan
* If you need to scan barcodes continuously without closing camera use `FlutterBarcodeScanner.getBarcodeStreamReceiver`
params will be same like `FlutterBarcodeScanner.scanBarcode`
e.g. 


```
FlutterBarcodeScanner.getBarcodeStreamReceiver("#ff6666", "Cancel", false, ScanMode.DEFAULT)
         .listen((barcode) { 
         /// barcode to be used
         });
```

### Contribution:

would :heart: to see any contribution, give :star:  if you like

### Contact:


<p>
<a href="https://github.com/AmolGangadhare"><img src="https://github.com/AmolGangadhare/MyProfileRepo/blob/master/git_hub_logo.png" width="32" height="33" style="max-width:100%;"></a>
<a href="https://stackoverflow.com/users/9823185/amol-gangadhare" rel="nofollow"><img src="https://github.com/AmolGangadhare/MyProfileRepo/blob/master/stack_o_logo.svg" width="36" height="36" style="max-width:100%;"></a>
<a href="https://www.linkedin.com/in/amolgangadhare/" rel="nofollow"><img src="https://github.com/AmolGangadhare/MyProfileRepo/blob/master/linked_in_logo.svg" width="36" height="36" style="max-width:100%;"></a>
</p>

E-mail: amol.gangadhare@gmail.com
