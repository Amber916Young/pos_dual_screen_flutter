package com.orderit.dual_screen_display;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    private Context appContext;
    private Activity activity;

    // Secondary Flutter
    private EventChannel  secondaryEngine;
    private DisplayManager displayManager;

    private MethodChannel secondaryEngineChannel; // to talk to secondary Dart app
    private SecondaryFlutterPresentation presentation;
    private final String secondaryViewTypeId = "dual_screen_display";
    private final String viewTypeEventsId =  "presentation_display_channel_events";
    private final String mainViewTypeId =  "main_display_channel";
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        appContext = flutterPluginBinding.getApplicationContext();
//        displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);

        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), secondaryViewTypeId);
        channel.setMethodCallHandler(this);

//        eventChannel = EventChannel(flutterPluginBinding.getBinaryMessenger(), viewTypeEventsId);
//        eventChannel.setStreamHandler(new DisplayConnectedStreamHandler(displayManager));


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
                        Log.d(TAG, "Secondary Flutter started successfully");
                        result.success(null);
                    } else {
                        Log.w(TAG, "Failed to start secondary Flutter - no secondary display found");
                        result.error("NO_SECONDARY", "No secondary display found", null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error starting secondary Flutter", e);
                    result.error("START_ERROR", "Error starting secondary Flutter: " + e.getMessage(), null);
                }
                return;
            }

            case "secondarySetState": {
                if (secondaryEngineChannel == null) {
                    result.error("NO_ENGINE", "Secondary engine not started. Call startSecondaryFlutter first.", null);
                    return;
                }
                // Forward arbitrary map payload to the secondary Dart app
                secondaryEngineChannel.invokeMethod("secondarySetState", call.arguments);
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

        // Stop any existing secondary engine first
        stopSecondaryFlutter();

        try {
            // Create a second engine and run the secondary Dart entrypoint
            secondaryEngine = new FlutterEngine(ctx);
            Log.d(TAG, "Created secondary Flutter engine");

            // Ensure Flutter is initialized
            if (!FlutterInjector.instance().flutterLoader().initialized()) {
                Log.d(TAG, "Initializing Flutter loader");
                FlutterInjector.instance().flutterLoader().startInitialization(ctx.getApplicationContext());
                FlutterInjector.instance().flutterLoader().ensureInitializationComplete(ctx, null);
            }

            // Channel INTO the secondary Dart tree (CustomerDisplayApp) - create BEFORE starting Dart execution
            secondaryEngineChannel = new MethodChannel(
                    secondaryEngine.getDartExecutor().getBinaryMessenger(),
                    secondaryViewTypeId
            );
            Log.d(TAG, "Created secondary engine method channel");

            // Use FlutterInjector to find the correct assets path
            String appBundlePath = FlutterInjector.instance().flutterLoader().findAppBundlePath();
            Log.d(TAG, "App bundle path: " + appBundlePath);
            
            DartExecutor.DartEntrypoint entrypoint =
                    new DartExecutor.DartEntrypoint(appBundlePath, dartEntrypoint);
            
            // Execute the Dart entrypoint
            Log.d(TAG, "Executing Dart entrypoint: " + dartEntrypoint);
            secondaryEngine.getDartExecutor().executeDartEntrypoint(entrypoint);

            // Host that engine in a Presentation on the external display
            presentation = new SecondaryFlutterPresentation(ctx, display, secondaryEngine);
            presentation.show();
            Log.d(TAG, "Secondary Flutter presentation shown");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error starting secondary Flutter", e);
            stopSecondaryFlutter(); // Clean up on error
            return false;
        }
    }

    private void stopSecondaryFlutter() {
        Log.d(TAG, "Stopping secondary Flutter");
        
        try {
            if (presentation != null) {
                if (presentation.isShowing()) {
                    Log.d(TAG, "Dismissing presentation");
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
                Log.d(TAG, "Destroying secondary engine");
                secondaryEngine.destroy();
                secondaryEngine = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error destroying secondary engine", e);
            secondaryEngine = null;
        }
        
        secondaryEngineChannel = null;
        Log.d(TAG, "Secondary Flutter stopped");
    }

    @Nullable
    private static Display findPresentationDisplay(Context context) {
        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        if (dm == null) return null;
        Display[] displays = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if (displays == null || displays.length == 0) return null;
        return displays[0];
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (channel != null) channel.setMethodCallHandler(null);
        channel = null;
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
