package com.videodac.publisher.helpers

import android.app.Activity
import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.widget.Toast
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

import com.videodac.publisher.R
import java.math.BigDecimal

@Suppress("DEPRECATION")
object  Utils {

    internal var walletPublicKey: String = ""
    internal var walletBalance: BigDecimal =  BigDecimal(0)

    @JvmStatic
    internal fun getWeb3(context: Context) = Web3j.build(HttpService(context.getString(R.string.rpc_url)))

    @JvmStatic
    fun Activity.toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    // A safe way to get an instance of the camera object.
    fun getCameraInstance(): Camera? {
        return try {
            Camera.open() // attempt to get a Camera instance
        } catch (e: Exception) {

            Log.d(Constants.STREAMING_TAG, "Error starting camera preview: ${e.message}")
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }

    @JvmStatic
    // A safe way to release the cameera object when not in use
    fun releasePreviewCamera() {
        // release the camera
        getCameraInstance()?.release()

    }
    
}