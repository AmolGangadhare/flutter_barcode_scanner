import 'dart:async';

import 'package:flutter/services.dart';

/// Flutter barcode scanner class that bridge the native classes to flutter project
class FlutterBarcodeScanner {
  /// Create a method channel instance
  static const MethodChannel _channel =
      const MethodChannel('flutter_barcode_scanner');

  static const EventChannel _eventChannel =
      const EventChannel('flutter_barcode_scanner_receiver');

  static Stream _onBarcodeReceiver;

  /// Use this method to start barcode scanning and get the barcode result in string
  /// lineColor is color of a line in scanning
  /// cancelButtonText is text of cancel button
  /// isShowFlashIcon is bool to show or hide flash icon
  /// isContinuousScan is bool to scan barcode continuously (true= scan continuously, don't close on barcode detect||
  /// false= close scanning on barcode detection)
  static Future<String> scanBarcode(
      String lineColor, String cancelButtonText, bool isShowFlashIcon) async {
    if (null == cancelButtonText || cancelButtonText.isEmpty)
      cancelButtonText = "Cancel";

    /// pass a line color param
    Map params = <String, dynamic>{
      "lineColor": lineColor,
      "cancelButtonText": cancelButtonText,
      "isShowFlashIcon": isShowFlashIcon,
      "isContinuousScan": false
    };

    /// Get barcode scan result
    String barcodeResult = await _channel.invokeMethod('scanBarcode', params);
    if (null == barcodeResult) {
      barcodeResult = "";
    }
    _eventChannel.receiveBroadcastStream().listen(((barcode) {
      return barcode;
    }));
    return barcodeResult;
  }

  /// isContinuousScan is bool to scan barcode continuously (true= scan continuously, don't close on barcode detect||
  /// false= close scanning on barcode detection)
  static Stream onBarcodeStreamReceiver(
      String lineColor, String cancelButtonText, bool isShowFlashIcon) {
    if (null == cancelButtonText || cancelButtonText.isEmpty)
      cancelButtonText = "Cancel";

    /// pass a line color param
    Map params = <String, dynamic>{
      "lineColor": lineColor,
      "cancelButtonText": cancelButtonText,
      "isShowFlashIcon": isShowFlashIcon,
      "isContinuousScan": true
    };

    /// Get barcode scan result
    _channel.invokeMethod('scanBarcode', params);
    if (_onBarcodeReceiver == null) {
      _onBarcodeReceiver = _eventChannel.receiveBroadcastStream();
    }
    return _onBarcodeReceiver;
  }
}
