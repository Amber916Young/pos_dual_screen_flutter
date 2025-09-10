import 'dual_screen_display_platform_interface.dart';

/// Ensure real platform is used

/// High-level API
class DualScreenDisplay {
  static Future<String?> getPlatformVersion() {
    return DualScreenDisplayPlatform.instance.getPlatformVersion();
  }

  static Future<void> startSecondaryFlutter({String entrypoint = 'customerDisplayMain'}) {
    return DualScreenDisplayPlatform.instance.startSecondaryFlutter(entrypoint: entrypoint);
  }

  static Future<void> transferDataToPresentation(Map<String, Object?> data) {
    return DualScreenDisplayPlatform.instance.transferDataToPresentation(data);
  }

  static Future<void> transferDataToMain(Map<String, Object?> data) {
    return DualScreenDisplayPlatform.instance.transferDataToMain(data);
  }

  static Future<void> stopSecondaryFlutter() {
    return DualScreenDisplayPlatform.instance.stopSecondaryFlutter();
  }

  static Stream<Map<String, dynamic>> onEvent() {
    return DualScreenDisplayPlatform.instance.onEvent();
  }
}
