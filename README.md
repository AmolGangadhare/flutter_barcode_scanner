# flutter_barcode_scanner

A plugin for Flutter apps that adds barcode scanning support on both Android and iOS.

[![pub package](https://img.shields.io/pub/v/flutter_barcode_scanner.svg)](https://pub.dartlang.org/packages/flutter_barcode_scanner)

![Demo gif](https://github.com/AmolGangadhare/MyProfileRepo/blob/master/flutter_barcode_scanning_demo.gif "Demo")


## Try example
Just clone the repository, open the project in `Android Studio/ VS Code`, open `pubspec.yaml` and click on `Packages get`.
Connect device and hit `run`. To run on iPhone you need to run from `Xcode first time` and just make `pod install` in `example/ios`.

## Getting Started 
Follow the steps for Android and iOS

PLEASE FOLLOW **iOS** STEPS CAREFULLY

### Android

:zap:  Don't worry, you don't need to do anything.

### iOS - Requires Swift support

Deployment target : 10

#### 1. Fresh start: 
Create new flutter app. Please check for Include swift support for iOS code. After creating project open ios project in XCode and set minimum **deployment target to 10**. Now proceed to **How to use**
    
#### 2. Adding to existing flutter app: 
As iOS code is written in Swift so you need to convert your existing iOS codebase to swift (or if you are creating a new project from Android Studio make sure to check `Include Swift support for iOS code`.) 
To do that you can create a new project with same name in different location and then just copy iOS folder to existing.(if any changes made before make sure to add these in iOS(swift)).
After making codebase in swift make sure that the Swift version is `5` as the code for iOS is written in Swift 5. 
To use on iOS, open the Xcode and add camera usage description in `Info.plist`. 

```
<key>NSCameraUsageDescription</key>
<string>Camera permission is required for barcode scanning.</string>
```

## How to use ?

After making the changes in Android ans iOS add flutter_barcode_scanner to `pubspec.yaml`
    
    dependencies:
      ...
      flutter_barcode_scanner: ^0.1.6

### One time scan
1. You need to import the package first.

    `import 'package:flutter_barcode_scanner/flutter_barcode_scanner.dart';`
    
2. Then use the `scanBarcode` method to access barcode scanning.
    
    `String barcodeScanRes = await FlutterBarcodeScanner.scanBarcode(COLOR_CODE,CANCEL_BUTTON_TEXT,isShowFlashIcon);`

Here in `scanBarcode(COLOR_CODE,CANCEL_BUTTON_TEXT)` 
`COLOR_CODE` is hex-color which is the color of line in barcode overlay you can pass color of your choice, 
, `CANCEL_BUTTON_TEXT` is a text of cancel button on screen you can pass text of your choice and language,
`isShowFlashIcon` is bool value used to show or hide the flash icon.

### Continuous scan
* If you need to scan barcodes continuously without closing camera use

```
FlutterBarcodeScanner.getBarcodeStreamReceiver("#ff6666", "Cancel", false)
         .listen((barcode) { 
         /// barcode to be used
         });
```

### Contribution:

 You can always contribute to this plugin by raising any issue or by PR.
 
### Contact:

E-mail: amol.gangadhare@gmail.com