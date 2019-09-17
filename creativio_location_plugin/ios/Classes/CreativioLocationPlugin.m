#import "CreativioLocationPlugin.h"
#import <creativio_location_plugin/creativio_location_plugin-Swift.h>

@implementation CreativioLocationPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftCreativioLocationPlugin registerWithRegistrar:registrar];
}
@end
