# SafeGap

SafeGap is a focused Android driving assistant that uses the phone's rear camera to estimate the distance to the vehicle ahead. It supports portrait and landscape mounting, overlays the estimated distance on the detected vehicle, and shows GPS speed directly over the camera view.

## Alerts

- Following distance warning and danger thresholds
- Collision-risk warning using estimated time to collision
- Vehicle-ahead-moved notification
- Blocked, dark, or low-detail camera warning
- Configurable GPS speed-limit warning

The speed feature uses the phone's GPS. It does not read road signs automatically. If location permission is declined, all camera and distance features remain available.

## Setup

1. Mount the phone securely with the rear camera facing forward through the windshield.
2. Grant camera permission. Grant location permission only if GPS speed alerts are wanted.
3. Park safely and use **Calibrate** with a known measured distance for better estimates.
4. Set distance thresholds and the speed limit in **Settings**.

## Build

Requirements: Android Studio with JDK 17 and Android SDK 36.

```bash
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions also produces a downloadable `SafeGap-debug` artifact after each push.

## Important safety limitation

SafeGap is an experimental camera-only aid, not certified ADAS, radar, lidar, or a replacement for attentive driving. Distance depends on calibration, camera placement, lighting, road geometry, and model accuracy. Do not use its estimate as an exact legal or braking measurement.

## Detection model

The app bundles Google's `efficientdet_lite0_320_ptq.tflite` COCO test model from [google-coral/test_data](https://github.com/google-coral/test_data), distributed under the Apache License 2.0. Only car, truck, bus, and motorcycle detections near the lane centre are considered.
