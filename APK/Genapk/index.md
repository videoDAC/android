# Building and Releasing the APK 

1.  Go to Tools>AVD Manager to create an android virtual device.

2.  Click "Create Virtual Device" and choose a preselected device profile.  The default is a Pixel 2 which will work fine and click next.

![apk1](apk1.png)

3.  Select a version of Android or Android API and choose download:

![apk2](apk2.png)

4.  The device image will take some time to download, when it is complete click "Finish":

![apk3](apk3.png)

5.  Click "Next" on the system image dialogue. 

6.  On the following screen set the default device orientation to "Landscape" and select ok.  You will see a list of your virtual devices.  In this area you can configure multiple virtual devices for debugging: 

![apk4](apk4.png)

7.  Close the virtual device manager and on the main page of Android Studio, set the drop down at the top to the device you just configured and click on the "Play" (Green Aarow) icon: 

![apk5](apk5.png)

8.  After a few minutes the emulated device should appear and the app will start with a message stating that the install was successful: 

![apk6](apk6.png)

9.  In the main window of Android Studio go to Build>Build Variant which will open a pane in the lower left corner of the window.  Here click on the "Active Build Variant" field and change it to "Release":

![apk7](apk7.png)

10.  To output an APK onto your computer, go to Build>Build Bundle(s)/APK(s)>Build APK(s) when complete, you can locate or analyze the APK by selecting one of those options on the little notification in the lower right hand side of the screen.

11.  To sign your app and release to Google Play, you will need to follow these instructions https://developer.android.com/studio/publish/app-signing  using your Google

[Home](../../README.md)
