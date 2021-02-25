# videoDAC/android

This repository contains software created by videoDAC for the android operating system.

It currently consists of 2 separate applications for mobile-based pay-to-pay livestreaming:

- [Consumer](https://github.com/videoDAC/android/blob/master/consumer/README.md) - for consuming livestream content, and sending payments
  - [Link to Code](https://github.com/videoDAC/android/tree/master/consumer)
- [Publisher](https://github.com/videoDAC/android/issues/31) - for publishing livestream content, and receiving payments
  - Under construction :)

Livestream content is handled by a [simple-streaming-server](https://github.com/videoDAC/simple-streaming-server), which receives content from the Publisher app, and serves content to the Consumer app.

Payments are cleared via cryptocurrency payment networks based on the Ethereum ecosystem (i.e. using addresses in the format of `0xabc123...`). The apps have been tested against Ethereum 1.0, and Matic.
