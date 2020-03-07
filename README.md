# Intro

This repo contains the native kotlin project for the `videoDAC` template pay-as-you-go livestream viewer app for Android.

# Building

To build out the app, please do the following,

1. Open the `android` project in [Android Studio](https://developer.android.com/studio)
2. Let the project finish syncing gradle and update any dependencies in the project as prompted.
3. Finally connect your device or start up an emulator and deploy the app to it by going to the menu `Run -> Run 'app'`

### Settings

- To modify the `STREAM_URL` open the file `VideoActivity.kt` and modify line 35 by entering the new valid hls URL. Then build the project as explained above and deploy the app.
- To set the streaming fee and recipient address, modify the lines 21 & 22 in `helpers/Utils.kt` and set the variables `streamingFeeInEth` & `recipientAddress`

### Screenshots

You can view the app at different screen size's in the [screenshot](/android/screenshots/) folder.
