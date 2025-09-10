import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'dual_screen_display_platform_interface.dart';

/// Default implementation using a MethodChannel.
class MethodChannelDualScreenDisplay extends DualScreenDisplayPlatform {
  @visibleForTesting
  final MethodChannel methodChannel = const MethodChannel('dual_screen_display');

  @override
  Future<String?> getPlatformVersion() async {
    final v = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return v;
  }

  @override
  Future<void> startSecondaryFlutter({String entrypoint = 'customerDisplayMain'}) async {
    await methodChannel.invokeMethod('startSecondaryFlutter', {'entrypoint': entrypoint});
  }

  @override
  Future<void> secondarySetState(Map<String, Object?> data) async {
    await methodChannel.invokeMethod('secondarySetState', data);
  }

  @override
  Future<void> stopSecondaryFlutter() async {
    await methodChannel.invokeMethod('stopSecondaryFlutter');
  }
}
