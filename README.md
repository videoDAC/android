# Intro

This repo contains the `videoDAC` template pay-to-play streaming app for Android.

These instructions include:

- Explanation of a "Template App"
- Recipe to create and publish your own livestream channel app ("Acme App")
- Overview of app's user journey
- Example deployments of this "Template App"

Please submit any issues or pull requests to this repository.

# What do you mean "Template App"?

A "template app" is all the code required for the publisher of a stream to generate a file (`.apk`) which can be installed directly onto any Android device, or which can be published to Android distribution channels (e.g. Google Play Store, F-droid).

The template must be configured with the following:

- **App name** e.g. "Acme Pay-to-play Streaming App", and copy
- **"Pay-to" address** i.e. where you would like to receive payment
- **Price-per-minute** in network's native ETH token (can be zero for free-to-play app)
- **`RPC URL`** for publishing transactions on Ethereum testnets and mainnet
- **`STREAM_URL`** which is the stream of A/V content to be played in the app

# User Journey

The initial User Journey for a user of the app is:

> **User** installs and launches app
- App creates own new wallet, checks balance with Infura
- App shows "paywall screen", including
  - Wallet's 0 balance (new wallet)
  - **Price-per-minute**
  - App's own ETH address + QR Code
  - App-specific copy
> **User** taps screen
- App closes
- Android notifies User that App's ETH address is stored to clipboard
> **User** sends enough ETH to app's ETH address
> **User** launches app
- App checks wallet balance with Infura
- App shows livestream video content
> **User** watches livestream video content
- App pays to Livestreamer's ETH address
  - Payment made every minute until `( balance < price-per-minute )`

# Generating an Android APK with the Option to publish to Google Play

This guide will take you step by step through creating an APK from a template and publishing it on the Google Play store.

OS:  Linux Ubuntu 18.04

1. [Create a Google Play Developer Account](APK/Account/index.md)
2. [Developer Environment](APK/Prereq/index.md)
3. [Install Android Studio](APK/Install/index.md)
4. [Get the App Template](APK/Getapp/index.md)
5. [Import the App to Android Studio](APK/Import/index.md)
6. [Setting Key Variables](APK/Variables/index.md)
7. [Generating the APK](APK/Genapk/index.md)
8. [Releasing the APK](APK/Relapk/index.md)

# Example Deployments

This app has been used in the following deployments:

- Alice's Pay-As-You-Go Livestream Viewer App
  - [App published on Google Play](https://play.google.com/store/apps/details?id=com.videodac.alice)
  - Payments in goETH
  - Paying to 0x4b4E19E18EbADdFB57DC1f07E07268b827A0EC18
  - Here is an example of [an address used by the app to pay per minute of content](https://goerli.etherscan.io/address/0x5ed294120886b2fdbde04064231efe3e8c3aee7b).
