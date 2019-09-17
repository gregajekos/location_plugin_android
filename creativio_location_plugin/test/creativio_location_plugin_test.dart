import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:creativio_location_plugin/creativio_location_plugin.dart';

void main() {
  const MethodChannel channel = MethodChannel('creativio_location_plugin');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await CreativioLocationPlugin.platformVersion, '42');
  });
}
