# Introduction

This repo contains the `Consumer App` template for a pay-to-play stream Consumer app for Android.

Contents:

- [What do you mean by "template"?](#what-do-you-mean-by-template)
- [User Journey](#user-journey)
- [Generating an Android APK](#generating-an-android-apk)
- [Example Deployment](#example-deployment)

Please submit any issues or pull requests to this repository.

# What do you mean by "template"?

A "template app" is all the code required to generate a file (`.apk`) which can be installed onto an Android device.

The template must be configured with the following params in the `strings.xml` file:

- **streaming_fee** in network's native MATIC token (can be zero for free-to-play app)
- **rpc_ur`** for publishing transactions on ethereum testnets or mainnet or sidechains like Matic/Polygon
- **app_name* e.g. "Acme Pay-to-play Streaming App", and minimal copy

# User Journey

The User Journey for a user of this app is:

- **User** installs and launches app
  - App creates own new wallet, checks balance with Infura
  - App shows "paywall screen", including
    - Wallet's 0 balance (new wallet)
    - **Price-per-minute**
    - App's own MATIC address + QR Code
    - App-specific copy

![Screenshot_20200514-232110](https://user-images.githubusercontent.com/59374467/81968276-ccb5f300-9639-11ea-9b0e-0b1c7dd41c27.png)

- **User** taps screen
  - App closes
  - Android notifies User that App's ETH address is stored to clipboard

![image](https://user-images.githubusercontent.com/2212651/82750460-f7d4db00-9dcd-11ea-8eea-b06982c94356.png)

- **User** sends enough MATIC to app's ETH address
  - This can be done in any app.

- **User** launches app
  - App checks wallet balance with Mumbai Testnet

![Screenshot_20200514-232732](https://user-images.githubusercontent.com/59374467/81968815-9167f400-963a-11ea-94a9-5db498882865.png)

  - App shows livestream video content (currently a test signal)

![Screenshot_20200514-232737](https://user-images.githubusercontent.com/59374467/81968828-95941180-963a-11ea-97f6-1f2ff988d9ee.png)

- **User** watches livestream video content
  - App pays to Livestreamer's ETH address
  - Payment made every minute

- App shows "paywall screen" when balance is < price-per-minute:

![Screenshot_20200514-235811](https://user-images.githubusercontent.com/59374467/81971634-063d2d00-963f-11ea-958c-59e833ee92c9.png)

# Generating an Android APK

This guide will take you step by step through creating an APK from a template and publishing it on the Google Play store.

OS:  Linux Ubuntu 18.04

1. [Create a Google Play Developer Account](docs/account/index.md)
2. [Developer Environment](docs/prereq/index.md)
3. [Install Android Studio](docs/install/index.md)
4. [Get the App Template](docs/getapp/index.md)
5. [Import the App to Android Studio](docs/import/index.md)
6. [Setting Key Variables](docs/variables/index.md)
7. [Generating the APK](docs/genapk/index.md)
8. [Releasing the APK](docs/relapk/index.md)

# Example Deployment

- Alice's Pay-As-You-Go Livestream Viewer App
  - [App published on Google Play](https://play.google.com/store/apps/details?id=com.videodac.alice)
  - Pay [`0x4b4E19E18EbADdFB57DC1f07E07268b827A0EC18`](https://goerli.etherscan.io/address/0x4b4E19E18EbADdFB57DC1f07E07268b827A0EC18) to play
  - Payments are in goETH, the native token of the Goerli testnet
  - Here is an example of [an address used by the app to pay per minute of content](https://goerli.etherscan.io/address/0x5ed294120886b2fdbde04064231efe3e8c3aee7b).
  - The livestream is a test card signal served from a [`simple-streaming-server`](https://github.com/videoDAC/simple-streaming-server)

![image](https://user-images.githubusercontent.com/2212651/82750740-c4934b80-9dcf-11ea-8eb4-f9046209cad4.png)

