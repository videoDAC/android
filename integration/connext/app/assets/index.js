import * as connext from "@connext/client";
import { AddressZero } from "ethers/constants";
import { parseEther } from "ethers/utils";


let channel = null;

export const initChannel = async (privateKey) => {
    channel = await connext.connect("rinkeby", { signer: privateKey });
    return channel
}


export const deposit = (amount) => {
    const payload  = {
        amount: parseEther(amount).toString(), // in wei/wad (ethers.js methods are very convenient for getting wei amounts)
        assetId: AddressZero // Use the AddressZero constant from ethers.js to represent ETH, or enter the token address
      };
      
      channel.deposit(payload);
}

export const swap = async (amount) => {
    // Exchanging Wei for Dai
    const payload  = {
        amount: parseEther(amount).toString(), // in wei (ethers.js methods are very convenient for getting wei amounts)
        toAssetId: "0x89d24a6b4ccb1b6faa2625fe562bdd9a23260359", // Dai
        fromAssetId: AddressZero // ETH
    }

    await channel.swap(payload);
}

export const makePayment = async (amount, recipientChannelId, metadata) => {
    const payload = {
        recipient: recipientChannelId, // counterparty's public identifier
        meta: { value: metadata }, // any arbitrary JSON data, or omit
        amount: parseEther(amount).toString(), // in wei (ethers.js methods are very convenient for getting wei amounts)
        assetId: AddressZero // ETH
    };
      
    await channel.transfer(payload);
}
