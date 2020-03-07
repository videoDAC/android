# Intro

This repo contains the native Kotlin project for the `videoDAC` template pay-as-you-go livestream viewer app for Android.

These instructions include:

- Explanation of a "Template App"
- Recipe to create and publish your own livestream channel app ("Acme TV")
- Overview of app's user journey
- Example deployments of this "Template App"

Please submit any issues or pull requests to this repository.

# What do you mean "Template App"?

A "template app" is all the code required for a Livestreamer to generate a file (`.apk`) which can be installed directly onto any Android device, or which can be published to Android distribution channels (e.g. Google Play Store).

The template must be configured with the following:

- App name, e.g. "Acme TV Channel"
- Pay-to address, i.e. where you would like the app
- Network, from rinkeby, goerli, ropsten, kovan, mainnet, or a custom RPC URL
- Price-per-minute in network's native ETH
- `STREAM_URL` which is the stream of `hls` video content which the app should play

# Generating an Android APK with the Option to publish to Google Play

This guide will take you step by step through creating an APK from a template and publishing it on the Google Play store.

OS:  Linux Ubuntu 18.04

1. [Create a Google Play Developer Account](APK/Account/index.md)
2. [Developer Environment](APK/Prereq/index.md)
3. [Install Android Studio](APK/Install/index.md)
4. [Get the App Template](APK/Getapp/index.md)
5. [Import the App to Android Studio](APK/Import/index.md)
6. [Setting Key Variables](APK/Variables/index.md)
7. [Building and Releasing the APK](APK/Genapk/index.md)

# User Journey

The initial User Journey for a user of the app is:

- **User** installs and launches app
- App creates new wallet
- App checks wallet balance with Infura
- App shows "paywall screen", including
  - 0 balance (new wallet)
  - price-per-minute
  - App's ETH address
- **User** taps screen
- App closes
- Android notifies User that App's ETH address is stored to clipboard
- **User** sends ETH to app's ETH address
- **User** launches app
- App checks wallet balance with Infura
- App shows livestream video content
- **User** watches livestream video content
- App pays to Livestreamer's ETH address
  - Payment made every minute until `( balance < price-per-minute )`

# Example Deployments

This app has been used in the following deployments:

- Gorli TV
  - [App published on Google Play](https://play.google.com/store/apps/details?id=com.videodac.hls)
  - Payments in goETH
  - Paying to 0xdac817294c0c87ca4fa1895ef4b972eade99f2fd
  - Sample burner wallet created: https://goerli.etherscan.io/address/0x3d507516c93b05e2d59a70cd90b197addf65ea53



