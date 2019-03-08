import 'dart:async';

import 'package:flutter/services.dart';


class FlutterBarcodeScanner {
  static const MethodChannel _channel =
      const MethodChannel('flutter_barcode_scanner');

  //Scan barcode and return the barcode string
  static Future<String>  scanBarcode(String lineColor) async {
    Map params = <String, dynamic>{
      "lineColor":lineColor
    };
    String barcodeResult = await _channel.invokeMethod('scanBarcode', params);
    if (null == barcodeResult) {
      barcodeResult = "";
    }
    return barcodeResult;
  }
}
