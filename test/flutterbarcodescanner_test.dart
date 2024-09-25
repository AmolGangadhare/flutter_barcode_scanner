import 'package:flutter_test/flutter_test.dart';
import 'package:flutterbarcodescanner/flutterbarcodescanner.dart';
import 'package:flutterbarcodescanner/flutterbarcodescanner_platform_interface.dart';
import 'package:flutterbarcodescanner/flutterbarcodescanner_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterbarcodescannerPlatform
    with MockPlatformInterfaceMixin
    implements FlutterbarcodescannerPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final FlutterbarcodescannerPlatform initialPlatform = FlutterbarcodescannerPlatform.instance;

  test('$MethodChannelFlutterbarcodescanner is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterbarcodescanner>());
  });

  test('getPlatformVersion', () async {
    Flutterbarcodescanner flutterbarcodescannerPlugin = Flutterbarcodescanner();
    MockFlutterbarcodescannerPlatform fakePlatform = MockFlutterbarcodescannerPlatform();
    FlutterbarcodescannerPlatform.instance = fakePlatform;

    expect(await flutterbarcodescannerPlugin.getPlatformVersion(), '42');
  });
}
