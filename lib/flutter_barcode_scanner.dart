import 'dart:async';

import 'package:flutter/services.dart';

/// Scan mode which is either QR code or BARCODE
enum ScanMode { QR, BARCODE, DEFAULT }

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
  static Future<String> scanBarcode(String lineColor, String cancelButtonText,
      bool isShowFlashIcon, ScanMode scanMode) async {
    if (null == cancelButtonText || cancelButtonText.isEmpty)
      cancelButtonText = "Cancel";

    if (scanMode == null) {
      scanMode = ScanMode.QR;
    }

    /// create params to be pass to plugin
    Map params = <String, dynamic>{
      "lineColor": lineColor,
      "cancelButtonText": cancelButtonText,
      "isShowFlashIcon": isShowFlashIcon,
      "isContinuousScan": false,
      "scanMode": scanMode.index
    };

    /// Get barcode scan result
    String barcodeResult = await _channel.invokeMethod('scanBarcode', params);
    if (null == barcodeResult) {
      barcodeResult = "";
    }
    return barcodeResult;
  }

  /// This method allows continuous barcode scanning without closing camera.
  /// It will return stream of barcode strings.
  /// Parameters will e same as #scanBarcode
  static Stream getBarcodeStreamReceiver(String lineColor,
      String cancelButtonText, bool isShowFlashIcon, ScanMode scanMode) {
    if (null == cancelButtonText || cancelButtonText.isEmpty)
      cancelButtonText = "Cancel";

    if (scanMode == null) {
      scanMode = ScanMode.QR;
    }

    /// create params to be pass to plugin
    Map params = <String, dynamic>{
      "lineColor": lineColor,
      "cancelButtonText": cancelButtonText,
      "isShowFlashIcon": isShowFlashIcon,
      "isContinuousScan": true,
      "scanMode": scanMode.index
    };

    /// Invoke method to open camera
    /// and then create event channel which will return stream
    _channel.invokeMethod('scanBarcode', params);
    if (_onBarcodeReceiver == null) {
      _onBarcodeReceiver = _eventChannel.receiveBroadcastStream();
    }
    return _onBarcodeReceiver;
  }
}
