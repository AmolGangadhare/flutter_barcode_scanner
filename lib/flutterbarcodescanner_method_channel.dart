import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutterbarcodescanner_platform_interface.dart';

/// An implementation of [FlutterbarcodescannerPlatform] that uses method channels.
class MethodChannelFlutterbarcodescanner extends FlutterbarcodescannerPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutterbarcodescanner');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
