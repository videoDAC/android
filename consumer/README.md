# videoDAC Livestream Consumer App

This repo contains the `videoDAC` template for a pay-to-play Livestream Consumer App, for Android Operating System.

Contents:

- [What do you mean by "template"?](#what-do-you-mean-by-template)
- [User Journey](#user-journey)
- [Generating an Android APK](#generating-an-android-apk)
- [Example Deployment](#example-deployment)

Please submit any issues or pull requests to this repository.

# What do you mean by "template"?

A "template app" is all the code required to generate a file (`.apk`) which can be installed onto an Android device.

The template must be configured with the following:

- **"Pay-to" address** i.e. where you would like the app to send payments
- **Price-per-minute** in network's native ETH token (can be zero for free-to-play app)
- **`RPC URL`** for publishing transactions on Ethereum testnets or mainnet
- **`STREAM_URL`** which is the stream of A/V content to be played in the app
- **App name** e.g. "Alice and Bob's Pay-to-play Streaming App", and minimal copy

# User Journey

The User Journey for a user of this app is:

## 1. **User** installs and launches app

- **App** creates own new wallet, checks balance onchain
- **App** shows "paywall screen", including
  - Wallet's 0 balance (new wallet)
  - **Price-per-minute**
  - App's own ETH address + QR Code
  - App-specific copy

![image](https://user-images.githubusercontent.com/2212651/108596305-222de600-73aa-11eb-9011-45d83edef689.png)

## 2. **User** taps screen

- **App** closes
- **Android** notifies User that App's ETH address is stored to clipboard

![image](https://user-images.githubusercontent.com/2212651/82750460-f7d4db00-9dcd-11ea-8eea-b06982c94356.png)

## 3. **User** sends enough ETH / MATIC to app's ETH address

- This can be done from any app, like [WallETH](https://walleth.org/).

## 4. **User** launches app again

- **App** checks wallet balance with Infura / Matic Vigil / your own RPC endpoint

## 5. **App** hides the paywall page and shows a channel list.

## 6. **User** selects which livestream channel to watch, from the app's scrollable "Channel List".

![image](https://user-images.githubusercontent.com/2212651/108596330-5dc8b000-73aa-11eb-9188-7731fcab580c.png)

- Livestream channelID is the Ethereum address of the livestream Publisher.
- App collates address data from [ENS Domains](https://ens.domains/) and [IPFS](https://ipfs.io/) (served via [3Box](https://3boxlabs.com/), and configured via [Livepeer Protocol Explorer](explorer.livepeer.org/).

## 7. **User** watches content from the livestream channel over the internet

![image](https://user-images.githubusercontent.com/2212651/108596338-67eaae80-73aa-11eb-81b2-c13d903d1328.png)

- Content shown is an automatically generate test source, but contains audio and video.
- The app pays every minute, directly to the Ethereum address of the livestream channel
- App has been tested against eth1 Mainnet, Goerli, Rinkeby and Ropsten, and Matic Mainnet and Mumbai Testnet.
- Content is served from a Livepeer Broadcaster node, set up per videoDAC's [`simple-streaming-server`](https://github.com/videoDAC/simple-streaming-server).

## 8. **App** shows "paywall screen", when balance runs out:

![image](https://user-images.githubusercontent.com/2212651/108596305-222de600-73aa-11eb-9011-45d83edef689.png)

## Go to Step 2.

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
