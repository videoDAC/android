# Intro

This repo contains the native kotlin and swift projects for the `Video Dac` app.


# Building

To build out the app for the both android and iOS, please do the following,

### Android

1. Open the `android` project in [Android Studio](https://developer.android.com/studio)
2. Let the project finish syncing gradle and update any dependencies in the project as prompted.
3. Finally connect your device or start up an emulator and deploy the app to it by going to the menu `Run -> Run 'app'`

To modify the `STREAM_URL` open the file `MainActivity.kt`  and modify line 35 by entering the new valid hls URL. Then build the project as explained above and deploy the app.

### iOS

1. Open the `ios` project in [Xcode](https://developer.apple.com/xcode/)
2. Build the project by going to the menu `Product -> Vuild`
3. Finally deploy click on the `play button` to deploy the app to the simulator or a [provisioned](https://developer.apple.com/library/archive/documentation/ToolsLanguages/Conceptual/YourFirstAppStoreSubmission/ProvisionYourDevicesforDevelopment/ProvisionYourDevicesforDevelopment.html) iOS device

To modify the `STREAM_URL` open the file `ViewcController.swift`  and modify line 17 by entering the new valid hls URL. Then build the project as explained above and deploy the app.
