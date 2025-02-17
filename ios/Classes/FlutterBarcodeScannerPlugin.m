#import "FlutterBarcodeScannerPlugin.h"
#import <flutter_barcode_scanner/flutter_barcode_scanner-Swift.h>

@implementation FlutterBarcodeScannerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterBarcodeScannerPlugin registerWithRegistrar:registrar];
}
@end
