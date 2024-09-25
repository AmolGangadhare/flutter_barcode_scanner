
import 'flutterbarcodescanner_platform_interface.dart';

class Flutterbarcodescanner {
  Future<String?> getPlatformVersion() {
    return FlutterbarcodescannerPlatform.instance.getPlatformVersion();
  }
}
