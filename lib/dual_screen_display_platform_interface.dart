import 'package:dual_screen_display/dual_screen_display_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

abstract class DualScreenDisplayPlatform extends PlatformInterface {
  DualScreenDisplayPlatform() : super(token: _token);
  static final Object _token = Object();

  static DualScreenDisplayPlatform _instance = MethodChannelDualScreenDisplay();

  static DualScreenDisplayPlatform get instance => _instance;

  static set instance(DualScreenDisplayPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Stream<Map<String, dynamic>> onEvent() {
    throw UnimplementedError();
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError();
  }

  Future<void> transferDataToMain(Map<String, Object?> data) {
    throw UnimplementedError();
  }

  Future<void> transferDataToPresentation(Map<String, Object?> data) {
    throw UnimplementedError();
  }

  Future<void> startSecondaryFlutter({String entrypoint = 'customerDisplayMain'}) {
    throw UnimplementedError();
  }

  Future<void> stopSecondaryFlutter() {
    throw UnimplementedError();
  }
}
