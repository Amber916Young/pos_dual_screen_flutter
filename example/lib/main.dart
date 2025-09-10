import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:dual_screen_display/dual_screen_display.dart';

void main() => runApp(const MyApp());

/// Secondary screen app entrypoint
@pragma('vm:entry-point')
void customerDisplayMain() {
  runApp(const CustomerDisplayApp());
}

/// Main app
class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Dual Screen Example',
      home: Scaffold(appBar: AppBar(title: const Text('Dual Screen Example')), body: const _Controls()),
    );
  }
}

/// Controls for primary screen
class _Controls extends StatefulWidget {
  const _Controls();

  @override
  State<_Controls> createState() => _ControlsState();
}

class _ControlsState extends State<_Controls> {
  int count = 0;
  List<String> _eventLog = [];

  @override
  void initState() {
    super.initState();

    // Subscribe to EventChannel from native
    DualScreenDisplay.onEvent().listen(
      (event) {
        final type = event['type'];
        final payload = event['payload'];
        debugPrint("📺 Event received: $event");

        setState(() {
          _eventLog.insert(0, '[$type] $payload');
        });
      },
      onError: (err) {
        debugPrint('❌ Error receiving event: $err');
      },
    );
  }

  void incrementCount() {
    setState(() => count++);
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        ElevatedButton(
          onPressed: () async {
            try {
              await DualScreenDisplay.startSecondaryFlutter();
              _showMessage(context, 'Secondary Flutter started');
            } catch (e) {
              _showMessage(context, 'Error: $e');
            }
          },
          child: const Text('Start Secondary Flutter UI'),
        ),
        ElevatedButton(
          onPressed: () async {
            try {
              await DualScreenDisplay.transferDataToPresentation({
                'headline': 'Welcome!',
                'subtitle': 'Scan to pay',
                'count': count,
              });
              _showMessage(context, 'Sent state with count $count');
            } catch (e) {
              _showMessage(context, 'Error sending state: $e');
            }
          },
          child: const Text('Send State'),
        ),
        ElevatedButton(
          onPressed: () async {
            try {
              await DualScreenDisplay.stopSecondaryFlutter();
              _showMessage(context, 'Stopped secondary Flutter');
            } catch (e) {
              _showMessage(context, 'Error stopping: $e');
            }
          },
          child: const Text('Stop Secondary UI'),
        ),
        const SizedBox(height: 24),
        Text('Count: $count'),
        ElevatedButton(onPressed: incrementCount, child: const Text('Increment Count')),
        const Divider(height: 32),
        const Text('📨 Incoming Events:', style: TextStyle(fontWeight: FontWeight.bold)),
        ..._eventLog.take(6).map((e) => Text(e)).toList(),
      ],
    );
  }

  void _showMessage(BuildContext context, String msg) {
    debugPrint(msg);
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }
}

/// Secondary display UI (customer screen)
class CustomerDisplayApp extends StatefulWidget {
  const CustomerDisplayApp();

  @override
  State<CustomerDisplayApp> createState() => _CustomerDisplayAppState();
}

class _CustomerDisplayAppState extends State<CustomerDisplayApp> {
  static const MethodChannel _channel = MethodChannel('dual_screen_display');
  String headline = 'Hello';
  String subtitle = 'Waiting...';
  int count = 0;

  @override
  void initState() {
    super.initState();

    _channel.setMethodCallHandler((call) async {
      debugPrint('Received method call: ${call.method}');
      if (call.method == 'transferDataToPresentation') {
        final args = call.arguments as Map?;
        if (args is! Map) return;
        final map = Map<String, dynamic>.from(args);
        headline = map['headline']?.toString() ?? headline;
        subtitle = map['subtitle']?.toString() ?? subtitle;
        count = int.tryParse('${map['count'] ?? count}') ?? count;
        setState(() {});

        // Notify primary that we updated successfully
        await _channel.invokeMethod('notifyPrimary', {'status': 'updated', 'count': count, 'headline': headline});
      }
    });

    // Notify primary app that secondary screen is ready
    Future.delayed(const Duration(seconds: 1), () {
      _channel.invokeMethod('notifyPrimary', {'status': 'ready', 'platform': 'secondary'});
    });
  }

  @override
  Widget build(BuildContext context) {
    debugPrint('⛳ BUILD: headline=$headline, subtitle=$subtitle, count=$count');
    return MaterialApp(
      theme: ThemeData.dark(),
      home: Scaffold(
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(headline, style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              Text(subtitle),
              const SizedBox(height: 16),
              Text('Count: $count'),
            ],
          ),
        ),
      ),
    );
  }
}
