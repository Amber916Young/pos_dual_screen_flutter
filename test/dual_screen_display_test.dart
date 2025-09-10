import 'package:flutter_test/flutter_test.dart';
import 'package:dual_screen_display/dual_screen_display.dart';
import 'package:dual_screen_display/dual_screen_display_platform_interface.dart';
import 'package:dual_screen_display/dual_screen_display_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockDualScreenDisplayPlatform
    with MockPlatformInterfaceMixin
    implements DualScreenDisplayPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final DualScreenDisplayPlatform initialPlatform = DualScreenDisplayPlatform.instance;

  test('$MethodChannelDualScreenDisplay is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelDualScreenDisplay>());
  });

  test('getPlatformVersion', () async {
    DualScreenDisplay dualScreenDisplayPlugin = DualScreenDisplay();
    MockDualScreenDisplayPlatform fakePlatform = MockDualScreenDisplayPlatform();
    DualScreenDisplayPlatform.instance = fakePlatform;

    expect(await dualScreenDisplayPlugin.getPlatformVersion(), '42');
  });
}
