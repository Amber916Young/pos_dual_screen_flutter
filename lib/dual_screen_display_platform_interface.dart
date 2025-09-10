import 'package:plugin_platform_interface/plugin_platform_interface.dart';

abstract class DualScreenDisplayPlatform extends PlatformInterface {
  DualScreenDisplayPlatform() : super(token: _token);
  static final Object _token = Object();

  static DualScreenDisplayPlatform _instance = MethodChannelStub();
  static DualScreenDisplayPlatform get instance => _instance;

  static set instance(DualScreenDisplayPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  // ---- API surface ----
  Future<String?> getPlatformVersion() {
    throw UnimplementedError('getPlatformVersion() has not been implemented.');
  }

  Future<void> startSecondaryFlutter({String entrypoint = 'customerDisplayMain'}) {
    throw UnimplementedError('startSecondaryFlutter() has not been implemented.');
  }

  Future<void> secondarySetState(Map<String, Object?> data) {
    throw UnimplementedError('secondarySetState() has not been implemented.');
  }

  Future<void> stopSecondaryFlutter() {
    throw UnimplementedError('stopSecondaryFlutter() has not been implemented.');
  }
}

/// Fallback stub so tests/builds won’t crash if no platform is set.
class MethodChannelStub extends DualScreenDisplayPlatform {}
