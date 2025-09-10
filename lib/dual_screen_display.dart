library dual_screen_display;

import 'dual_screen_display_method_channel.dart';
import 'dual_screen_display_platform_interface.dart';

/// Public facade that apps import.
class DualScreenDisplay {
  DualScreenDisplay._();

  static DualScreenDisplayPlatform get _platform {
    // Ensure default impl is MethodChannel unless someone swaps it (e.g., tests/web).
    if (DualScreenDisplayPlatform.instance is MethodChannelStub) {
      DualScreenDisplayPlatform.instance = MethodChannelDualScreenDisplay();
    }
    return DualScreenDisplayPlatform.instance;
  }

  /// Native platform version (debug helper).
  static Future<String?> get platformVersion => _platform.getPlatformVersion();

  /// Start a secondary Flutter engine on the external/customer display.
  ///
  /// [entrypoint] must match a `@pragma('vm:entry-point')` Dart function,
  /// e.g. `customerDisplayMain`.
  static Future<void> startSecondaryFlutter({String entrypoint = 'customerDisplayMain'}) {
    return _platform.startSecondaryFlutter(entrypoint: entrypoint);
  }

  /// Send state to the secondary Flutter UI.
  static Future<void> secondarySetState(Map<String, Object?> data) {
    return _platform.secondarySetState(data);
  }

  /// Stop/destroy the secondary engine and dismiss the Presentation.
  static Future<void> stopSecondaryFlutter() {
    return _platform.stopSecondaryFlutter();
  }
}
