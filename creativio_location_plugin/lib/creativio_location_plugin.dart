import 'dart:async';

import 'package:flutter/services.dart';

class CreativioLocationPlugin {
  static const MethodChannel _channel =
      const MethodChannel('creativio_location_plugin');

  static const EventChannel _stream =
      const EventChannel('creativio_location_plugin_stream');

  static StreamController<Location> _onLocationChangeController =
      StreamController<Location>();

  ///Stream that will return new location everytime it changes. You can listen to this stream to get the latest [Location], also works in background mode.
  static Stream<Location> onLocationChange = _onLocationChangeController.stream;
  static Stream _onLocationChange = _stream.receiveBroadcastStream();
  static StreamSubscription _onLocationChangeSubscription;

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static get startLocationService async {
    _channel.invokeMethod('startUpdatingLocation');
    _listenToLocationUpdates();
  }

  ///Stop updating location.
  static void stopUpdatingLocation() {
    _channel.invokeMethod("stopUpdatingLocation");
    _onLocationChangeSubscription.cancel();
  }

  static _listenToLocationUpdates() {
    _onLocationChangeSubscription = _onLocationChange.listen((data) {
      final newLocation = Location.fromMap(data);
      _onLocationChangeController.add(newLocation);
    });
  }

  static Future<Map<dynamic, dynamic>> get currentLocation async {
    return _channel.invokeMethod('currentLocation');
  }
}

class Location {
  double altitude;
  double latitude;
  double longitude;
  double speed;
  int timestamp;
  double accuracy;

  Location(
      {double altitude,
      double latitude,
      double longitude,
      double speed,
      int timeStamp,
      double accuracy}) {
    this.altitude = altitude;
    this.latitude = latitude;
    this.longitude = longitude;
    this.speed = speed;
    this.timestamp = timestamp;
    this.accuracy = accuracy;
  }

  Location.fromMap(Map<dynamic, dynamic> map) {
    this.altitude = map["altitude"];
    this.latitude = map["latitude"];
    this.longitude = map["longitude"];
    this.speed = map["speed"];
    this.timestamp = map["timestamp"];
    this.accuracy = map["accuracy"];
  }
}
