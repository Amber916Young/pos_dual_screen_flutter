package com.orderit.dual_screen_display;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.FlutterInjector;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.EventChannel;

/**
 * DualScreenDisplayPlugin
 */
public class DualScreenDisplayPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    private static final String TAG = "DualScreenDisplayPlugin";

    private MethodChannel channel;
    private EventChannel eventChannel;
    private EventChannel.EventSink eventSink;

    private Context appContext;
    private Activity activity;

    // Secondary Flutter
    private FlutterEngine secondaryEngine;
    private MethodChannel secondaryEngineChannel; // to talk to secondary Dart app
    private SecondaryFlutterPresentation presentation;

    private final String secondaryViewTypeId = "dual_screen_display";
    private final String viewTypeEventsId = "presentation_display_channel_events";

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        appContext = flutterPluginBinding.getApplicationContext();

        // Method channel: controls
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), secondaryViewTypeId);
        channel.setMethodCallHandler(this);

        // Event channel: async streams
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), viewTypeEventsId);
        eventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                Log.d(TAG, "EventChannel onListen");
                eventSink = events;
            }

            @Override
            public void onCancel(Object arguments) {
                Log.d(TAG, "EventChannel onCancel");
                eventSink = null;
            }
        });
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                return;

            case "startSecondaryFlutter": {
                String entrypoint = call.argument("entrypoint");
                Log.d(TAG, "startSecondaryFlutter called with entrypoint: " + entrypoint);

                if (entrypoint == null || entrypoint.isEmpty()) {
                    entrypoint = "customerDisplayMain"; // default
                }

                try {
                    boolean ok = startSecondaryFlutter(entrypoint);
                    if (ok) {
                        result.success(null);
                    } else {
                        result.error("NO_SECONDARY", "No secondary display found", null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error starting secondary Flutter", e);
                    result.error("START_ERROR", "Error starting secondary Flutter: " + e.getMessage(), null);
                }
                return;
            }

            case "transferDataToPresentation": {
                if (secondaryEngineChannel == null) {
                    result.error("NO_ENGINE", "Secondary engine not started. Call startSecondaryFlutter first.", null);
                    return;
                }
                // Forward payload to secondary Dart app
                secondaryEngineChannel.invokeMethod("transferDataToPresentation", call.arguments);
                result.success(null);
                return;
            }

            case "stopSecondaryFlutter": {
                stopSecondaryFlutter();
                result.success(null);
                return;
            }
        }
        result.notImplemented();
    }

    private boolean startSecondaryFlutter(@NonNull String dartEntrypoint) {
        Log.d(TAG, "Starting secondary Flutter with entrypoint: " + dartEntrypoint);

        Display display = findPresentationDisplay(appContext);
        if (display == null) {
            Log.w(TAG, "No secondary display found");
            return false;
        }

        Log.d(TAG, "Found secondary display: " + display.getDisplayId());

        if (presentation != null && presentation.isShowing()) {
            Log.d(TAG, "Secondary Flutter already running");
            return true;
        }

        Context ctx = (activity != null) ? activity : appContext;

        stopSecondaryFlutter(); // Clean up if already running

        try {
            secondaryEngine = new FlutterEngine(ctx);
            Log.d(TAG, "Created secondary Flutter engine");

            if (!FlutterInjector.instance().flutterLoader().initialized()) {
                FlutterInjector.instance().flutterLoader().startInitialization(ctx.getApplicationContext());
                FlutterInjector.instance().flutterLoader().ensureInitializationComplete(ctx, null);
            }

            // Setup method channel from plugin to secondary Dart app
            secondaryEngineChannel = new MethodChannel(
                    secondaryEngine.getDartExecutor().getBinaryMessenger(),
                    secondaryViewTypeId
            );
            Log.d(TAG, "Created secondary engine method channel");

            // Listen to method calls from secondary Dart app
            secondaryEngineChannel.setMethodCallHandler((call, result) -> {
                if ("notifyPrimary".equals(call.method)) {
                    sendEventToFlutter("fromSecondary", call.arguments);
                    result.success(null);
                } else {
                    result.notImplemented();
                }
            });

            String appBundlePath = FlutterInjector.instance().flutterLoader().findAppBundlePath();
            DartExecutor.DartEntrypoint entrypoint =
                    new DartExecutor.DartEntrypoint(appBundlePath, dartEntrypoint);

            secondaryEngine.getDartExecutor().executeDartEntrypoint(entrypoint);
            Log.d(TAG, "Dart entrypoint executed"+dartEntrypoint);

            presentation = new SecondaryFlutterPresentation(ctx, display, secondaryEngine);
            presentation.show();
            Log.d(TAG, "Secondary Flutter presentation shown");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error starting secondary Flutter", e);
            stopSecondaryFlutter();
            return false;
        }
    }

    private void stopSecondaryFlutter() {
        Log.d(TAG, "Stopping secondary Flutter");

        try {
            if (presentation != null) {
                if (presentation.isShowing()) {
                    presentation.dismiss();
                }
                presentation = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error dismissing presentation", e);
            presentation = null;
        }

        try {
            if (secondaryEngine != null) {
                secondaryEngine.destroy();
                secondaryEngine = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error destroying engine", e);
            secondaryEngine = null;
        }

        secondaryEngineChannel = null;
    }

    private void sendEventToFlutter(String type, Object payload) {
        if (eventSink != null) {
            Map<String, Object> event = new HashMap<>();
            event.put("type", type);
            event.put("payload", payload);
            eventSink.success(event);
        } else {
            Log.w(TAG, "No active eventSink to send event");
        }
    }

    @Nullable
    private static Display findPresentationDisplay(Context context) {
        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        if (dm == null) return null;
        Display[] displays = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if (displays.length == 0) return null;
        return displays[0];
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (channel != null) channel.setMethodCallHandler(null);
        if (eventChannel != null) eventChannel.setStreamHandler(null);

        channel = null;
        eventChannel = null;
        eventSink = null;
        appContext = null;
    }

    // ---------- ActivityAware ----------
    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }
}
