import 'package:flutter/material.dart';
import 'package:dual_screen_display/dual_screen_display.dart';
import 'package:flutter/services.dart';

void main() => runApp(const MyApp());

// Secondary entrypoint for the customer screen:
@pragma('vm:entry-point')
void customerDisplayMain() {
  runApp(const CustomerDisplayApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(appBar: AppBar(title: const Text('Dual Screen Example')), body: const _Controls()),
    );
  }
}

class _Controls extends StatefulWidget {
  const _Controls();

  @override
  State<_Controls> createState() => _ControlsState();
}

class _ControlsState extends State<_Controls> {
  int count = 0;

  @override
  void initState() {
    super.initState();
    count = 0;
  }

  void incrementCount() {
    setState(() {
      count++;
    });
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
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Secondary Flutter started successfully')),
              );
            } catch (e) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('Error: $e')),
              );
            }
          },
          child: const Text('Start secondary Flutter UI'),
        ),
        ElevatedButton(
          onPressed: () async {
            try {
              // Make sure we have the latest count value
              print('Sending count: $count'); // Add debugging
              
              await DualScreenDisplay.secondarySetState({
                'headline': 'Welcome!', 
                'subtitle': 'Scan to pay', 
                'count': count
              });
              
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('State sent successfully with count: $count')),
              );
            } catch (e) {
              print('Error sending state: $e'); // Add debugging
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('Error sending state: $e')),
              );
            }
          },
          child: const Text('Send state'),
        ),
        ElevatedButton(
          onPressed: () async {
            try {
              await DualScreenDisplay.stopSecondaryFlutter();
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Secondary Flutter stopped')),
              );
            } catch (e) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('Error stopping: $e')),
              );
            }
          },
          child: const Text('Stop secondary UI'),
        ),
        const SizedBox(height: 24),
        const Text('Secondary listens on MethodChannel("secondary_engine")'),
        const SizedBox(height: 16),
        Text('Count: $count'),
        ElevatedButton(
          onPressed: incrementCount,
          child: const Text('Increment Count'),
        ),
      ],
    );
  }
}

class CustomerDisplayApp extends StatefulWidget {
  const CustomerDisplayApp();

  @override
  State<CustomerDisplayApp> createState() => _CustomerDisplayAppState();
}

class _CustomerDisplayAppState extends State<CustomerDisplayApp> {
  static const MethodChannel _secondaryEngineChannel = MethodChannel('secondary_engine');
  String headline = 'Hello';
  String subtitle = 'Waiting...';
  int count = 0;

  @override
  void initState() {
    super.initState();
    debugPrint('CustomerDisplayApp initState called'); // Add debugging
    
    _secondaryEngineChannel.setMethodCallHandler((call) async {
      debugPrint('Received method call: ${call.method} with args: ${call.arguments}'); // Add debugging
      
      if (call.method == 'secondarySetState') {
        final Map args = (call.arguments as Map?) ?? {};
        debugPrint('Processing secondarySetState with args: $args'); // Add debugging
        
        setState(() {
          headline = args['headline']?.toString() ?? headline;
          subtitle = args['subtitle']?.toString() ?? subtitle;
          count = int.tryParse('${args['count'] ?? count}') ?? count;
          debugPrint('Updated count to: $count'); // Add debugging
        });
      }
    });
    
    debugPrint('Method call handler set up'); // Add debugging
  }

  @override
  Widget build(BuildContext context) {
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
