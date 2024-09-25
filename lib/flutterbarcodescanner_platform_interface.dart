import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutterbarcodescanner_method_channel.dart';

abstract class FlutterbarcodescannerPlatform extends PlatformInterface {
  /// Constructs a FlutterbarcodescannerPlatform.
  FlutterbarcodescannerPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterbarcodescannerPlatform _instance = MethodChannelFlutterbarcodescanner();

  /// The default instance of [FlutterbarcodescannerPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterbarcodescanner].
  static FlutterbarcodescannerPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterbarcodescannerPlatform] when
  /// they register themselves.
  static set instance(FlutterbarcodescannerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
