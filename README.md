# flutter_barcode_scanner

A flutter plugin adds barcode scanning support on both Android and iOS.

![Demo gif](https://github.com/AmolGangadhare/MyProfileRepo/blob/master/flutter_barcode_scanning_demo.gif "Demo")


## Try example
Just clone the repository, open the project in `Android Studio/ VS Code`, open `pubspec.yaml` and click on `Packages get`.
Connect device and hit `run`. To run on iPhone you need to run from `Xcode first time` and just make `pod install` in `example/ios`.

## Getting Started 
Follow the steps for Android and iOS

### Android

:zap: , dont worry no need to do anything.

### iOS

Deployment target : 10
Swift Version : 4.2

As iOS code is written in Swift so you need to convert your existing iOS codebase to swift (or if you are creating a new project from Android Studio make sure to check `Include Swift support for iOS code`.) 
To do that you can create a new project with same name in different location and then just copy iOS folder to existing.(if any changes made before make sure to add these in iOS(swift)).
After making codebase in swift make sure that the Swift version is `4.2` as the code for iOS is written in Swift 4.2. 
To use on iOS, open the Xcode and add camera usage description in `Info.plist`. 

    <key>NSCameraUsageDescription</key>
    <string>Camera permission is required for barcode scanning.</string>

## How to use ?

After making the changes in Android ans iOS add flutter_barcode_scanner to `pubspec.yaml`
    
    dependencies:
      ...
      flutter_barcode_scanner: ^0.0.4

1. You need to import the package first.

    `import 'package:flutter_barcode_scanner/flutter_barcode_scanner.dart';`
    
2. Then use the `scanBarcode` method to access barcode scanning.
    
    `String barcodeScanRes = await FlutterBarcodeScanner.scanBarcode(COLOR_CODE);`

Here in `scanBarcode(COLOR_CODE)` you can pass hex-color which is the color of line in barcode overlay.


## Credits :

Special thanks to [@Harshal Jadhav](https://github.com/harshalrj25) for help in iOS


## About me ..
 
E-mail : amol.gangadhare@gmail.com
 
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
