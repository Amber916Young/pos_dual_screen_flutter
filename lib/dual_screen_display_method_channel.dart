import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'dual_screen_display_platform_interface.dart';

class MethodChannelDualScreenDisplay extends DualScreenDisplayPlatform {
  @visibleForTesting
  final MethodChannel methodChannel = const MethodChannel('dual_screen_display');

  final EventChannel _eventChannel = const EventChannel('presentation_display_channel_events');

  Stream<Map<String, dynamic>>? _eventStream;

  @override
  Future<String?> getPlatformVersion() async {
    return await methodChannel.invokeMethod<String>('getPlatformVersion');
  }

  @override
  Future<void> startSecondaryFlutter({String entrypoint = 'customerDisplayMain'}) async {
    await methodChannel.invokeMethod('startSecondaryFlutter', {'entrypoint': entrypoint});
  }

  @override
  Future<void> transferDataToPresentation(Map<String, Object?> data) async {
    await methodChannel.invokeMethod('transferDataToPresentation', data);
  }

  @override
  Future<void> transferDataToMain(Map<String, Object?> data) async {
    await methodChannel.invokeMethod('transferDataToMain', data);
  }

  @override
  Future<void> stopSecondaryFlutter() async {
    await methodChannel.invokeMethod('stopSecondaryFlutter');
  }

  @override
  Stream<Map<String, dynamic>> onEvent() {
    _eventStream ??= _eventChannel.receiveBroadcastStream().map((event) => Map<String, dynamic>.from(event));
    return _eventStream!;
  }
}
