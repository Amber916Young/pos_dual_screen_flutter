package com.orderit.dual_screen_display;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import io.flutter.embedding.android.FlutterSurfaceView;
import io.flutter.embedding.android.FlutterView;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class SecondaryFlutterPresentation extends Presentation {

    private static final String TAG = "SecondaryFlutterPresentation";
    private final FlutterEngine engine;
    private FlutterView flutterView;
    private final String mainViewTypeId = "main_display_channel";

    public SecondaryFlutterPresentation(Context outerContext, Display display, FlutterEngine engine) {
        super(outerContext, display);
        this.engine = engine;
        Log.d(TAG, "Creating SecondaryFlutterPresentation for display: " + display.getDisplayId());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        try {
            FlutterSurfaceView surfaceView = new FlutterSurfaceView(getContext());
            flutterView = new FlutterView(getContext(), surfaceView);

            FrameLayout container = new FrameLayout(getContext());
            container.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            container.addView(flutterView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            setContentView(container);
            flutterView.attachToFlutterEngine(engine);
            Log.d(TAG, "FlutterView attached to engine successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
        if (flutterView != null) {
            flutterView.detachFromFlutterEngine();
        }
    }
}
