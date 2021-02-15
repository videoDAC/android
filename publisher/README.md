# Introduction

This folder contains the `Publisher` template for a pay-to-play stream Consumer app for Android.

Contents:

- [What do you mean by "template"?](#what-do-you-mean-by-template)
- [User Journey](#user-journey)
- [Generating an Android APK](#generating-an-android-apk)

Please submit any issues or pull requests to this repository.

# What do you mean by "template"?

A "template app" is all the code required to generate a file (`.apk`) which can be installed onto an Android device.

The template must be configured with the following:

- **streaming_price** in network's native ETH or MATIC token (can be zero for free-to-play app)
- **rpc_url** for publishing transactions on Ethereum testnets or mainnet
- **rtmp_base_url** which is the base url pointing to the livepeer broadcaster node i.e. rtmp://89.145.161.141:1935/%1$s
- **app_name** e.g. "Publisher App", and minimal copy

# User Journey

The User Journey for a user of this app is:

- **User** installs and launches app
  - App creates own new wallet, checks balance with Matic
  - App shows "`streaming screen`", including
    - Wallet's 0 balance (new wallet)
    - App's own ETH address
    - `You` option to start streaming

- **User** taps `You` screen
  - App starts streaming and monitors incoming payments on the Matic network
  - App plays confetti on first payment received

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

