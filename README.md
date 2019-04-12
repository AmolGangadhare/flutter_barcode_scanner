# flutter_barcode_scanner

A plugin for Flutter apps that adds barcode scanning support on both Android and iOS.

[![pub package](https://img.shields.io/pub/v/flutter_barcode_scanner.svg)](https://pub.dartlang.org/packages/flutter_barcode_scanner)

![Demo gif](https://github.com/AmolGangadhare/MyProfileRepo/blob/master/flutter_barcode_scanning_demo.gif "Demo")


## Try example
Just clone the repository, open the project in `Android Studio/ VS Code`, open `pubspec.yaml` and click on `Packages get`.
Connect device and hit `run`. To run on iPhone you need to run from `Xcode first time` and just make `pod install` in `example/ios`.

## Getting Started 
Follow the steps for Android and iOS

### Android

:zap:  Don't worry, you don't need to do anything.

### iOS

Deployment target : 10
Swift Version : 5

As iOS code is written in Swift so you need to convert your existing iOS codebase to swift (or if you are creating a new project from Android Studio make sure to check `Include Swift support for iOS code`.) 
To do that you can create a new project with same name in different location and then just copy iOS folder to existing.(if any changes made before make sure to add these in iOS(swift)).
After making codebase in swift make sure that the Swift version is `5` as the code for iOS is written in Swift 5. 
To use on iOS, open the Xcode and add camera usage description in `Info.plist`. 

    <key>NSCameraUsageDescription</key>
    <string>Camera permission is required for barcode scanning.</string>

## How to use ?

After making the changes in Android ans iOS add flutter_barcode_scanner to `pubspec.yaml`
    
    dependencies:
      ...
      flutter_barcode_scanner: ^0.0.8

1. You need to import the package first.

    `import 'package:flutter_barcode_scanner/flutter_barcode_scanner.dart';`
    
2. Then use the `scanBarcode` method to access barcode scanning.
    
    `String barcodeScanRes = await FlutterBarcodeScanner.scanBarcode(COLOR_CODE,CANCEL_BUTTON_TEXT,isShowFlashIcon);`

Here in `scanBarcode(COLOR_CODE,CANCEL_BUTTON_TEXT)` 
`COLOR_CODE` is hex-color which is the color of line in barcode overlay you can pass color of your choice, 
, `CANCEL_BUTTON_TEXT` is a text of cancel button on screen you can pass text of your choice and language,
`isShowFlashIcon` is bool value used to show or hide the flash icon.

## Credits :

Special thanks to [@Harshal Jadhav](https://github.com/harshalrj25) for helping me for iOS part.


## About me ..
 
E-mail : amol.gangadhare@gmail.com
 
<table style="background-color:#F5F5DC">
<tr>
<td> 
Amol Gangadhare
 
<p align="center">
<a href = "https://github.com/AmolGangadhare"><img src = "https://github.com/AmolGangadhare/MyProfileRepo/blob/master/git_hub_logo.png" width="32" height = "33"/></a>
<a href = "https://stackoverflow.com/users/9823185/amol-g?tab=profile"><img src = "https://github.com/AmolGangadhare/MyProfileRepo/blob/master/stack_o_logo.svg" width="36" height="36"/></a>
<a href = "https://www.linkedin.com/in/amolgangadhare/"><img src = "https://github.com/AmolGangadhare/MyProfileRepo/blob/master/linked_in_logo.svg" width="36" height="36"/></a>
</p>
</td>
</tr> 
</table>