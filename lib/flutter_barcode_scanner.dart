import 'dart:async';

import 'package:flutter/services.dart';

bool _handlePermissions = true;
bool _executeAfterPermissionGranted = true;

class FlutterBarcodeScanner {
  static const MethodChannel _channel =
      const MethodChannel('flutter_barcode_scanner');

  FlutterBarcodeScanner setHandlePermissions(bool handlePermissions) {
    _handlePermissions = handlePermissions;
    return this;
  }

  FlutterBarcodeScanner setExecuteAfterPermissionGranted(
      bool executeAfterPermissionGranted) {
    _executeAfterPermissionGranted = executeAfterPermissionGranted;
    return this;
  }

  //Scan barcode and return the barcode string
  static Future<String>  scanBarcode(String lineColor) async {
    Map params = <String, dynamic>{
      "handlePermissions": _handlePermissions,
      "executeAfterPermissionGranted": _executeAfterPermissionGranted,
      "lineColor":lineColor
    };
    String barcodeResult = await _channel.invokeMethod('scanBarcode', params);
    if (null == barcodeResult) {
      barcodeResult = "";
    }
    return barcodeResult;
  }
}
