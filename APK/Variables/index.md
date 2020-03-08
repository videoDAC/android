# Setting Key Variables

In this section, you will set some key variables to tailor the app to your requirements:

* Change the `applicationId` from the default `com.videodac.hls`
* Change the "in-app text" used to guide the user
* Change the parameters used by the app to make payments and request content

You should now have the app imported, and see this screen:

![Screenshot from 2020-03-08 17-58-10](https://user-images.githubusercontent.com/59374467/76162763-9ace6f00-6166-11ea-8ac4-ef64e3fecc9f.png)

## Change `applicationId`

The default `applicationId` is `com.videodac.hls`. You must change this to something unique to you, otherwise this app with collide with other apps on your device, or in Google Play Store.

1.  In Android Studio, navigate to `app>java`:

![image](https://user-images.githubusercontent.com/59374467/76163064-0fa2a880-6169-11ea-9764-eeaf47a828f6.png)

2.  Right-click on the first `com.videodac.hls` and select "Refactor" then "Rename". When prompted, select to "Rename package":

![image](https://user-images.githubusercontent.com/59374467/76163111-7cb63e00-6169-11ea-900a-cc5e813bb540.png)

3. Choose the new name for the package and click "Refactor". In this case, we will use `acme` but you can choose whatever you want:

![image](https://user-images.githubusercontent.com/59374467/76163128-9f485700-6169-11ea-89ce-1f7bfe7a39a8.png)

4. Once completed, you should see it look like this:

![image](https://user-images.githubusercontent.com/59374467/76163170-f9e1b300-6169-11ea-9066-10a5257a6253.png)

## Change `strings.xml`

The template shows the following paywall screen to the user, based on the default configuration:

![Screenshot_20200308-182730](https://user-images.githubusercontent.com/59374467/76163280-cce1d000-616a-11ea-9a8f-89097563f246.png)
![Screenshot_20200308-182716](https://user-images.githubusercontent.com/59374467/76163277-c81d1c00-616a-11ea-9d79-172828070bda.png)
![Screenshot_20200308-182832](https://user-images.githubusercontent.com/59374467/76163281-ce12fd00-616a-11ea-8c8f-900906b76536.png)

To change this text, open `app/res/value/strings.xml`:

![image](https://user-images.githubusercontent.com/59374467/76163326-1a5e3d00-616b-11ea-9d82-0dbaf8f76637.png)

* App name (to e.g. "Acme")
* Pay-to address
* Network, from rinkeby, goerli, ropsten, kovan, mainnet, or a    custom RPC URL
* price-per-minute in chosen network's native ETH
* `STREAM_URL` for the video content

For example, you may wish to configure the following values in `strings.xml`:

* App name can be set in the "app_name" field.
* Pay-to address can be set in the "wallet_address" field.
* You must set the "infura_url" to choose a network, or this is where you would enter a custom RPC URL.

    * To get an infura_rul proceed to https://infura.io and create an account.
    * Create a new project.
    * You can name the project, and then under keys select the endpoint to be the Ethereum network you wish to use.
    * Copy the link that is generated and paste it in the "infura_url" field of the configuration.

## Change `Utils.kt`

* Open `app/java/com.videodac.hls/helpers/Utils.kt`

* To set the streaming fee, recipient address and STREAM_URL, modify lines 21, 22 and 25 in `Utils.kt` and set the variables `streamingFeeInEth`, `recipientAddress` and `STREAM_URL`
    * There is a test livestream running at http://52.29.226.43:8935/stream/hello_world.m3u8 if you need a resource
    * To find out more about how to create your own STREAM_URL, see [Infinite Digital Stage from videoDAC](https://github.com/videoDAC/infinite-digital-stage).

[Home](../../README.md)
