@file:Suppress("DEPRECATION")
package com.videodac.publisher.activities

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import android.widget.ImageButton
import android.widget.TextView.BufferType
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions
import com.livinglifetechway.quickpermissions_kotlin.util.QuickPermissionsOptions
import com.livinglifetechway.quickpermissions_kotlin.util.QuickPermissionsRequest

import com.videodac.publisher.R
import com.videodac.publisher.databinding.StreamingCameraScreenBinding
import com.videodac.publisher.databinding.StreamingLaunchScreenBinding
import com.videodac.publisher.databinding.StreamingScreenBinding
import com.videodac.publisher.helpers.CameraPreview
import com.videodac.publisher.helpers.Constants.PREF_NAME
import com.videodac.publisher.helpers.Constants.PRIVATE_MODE
import com.videodac.publisher.helpers.Constants.STREAMING_TAG
import com.videodac.publisher.helpers.Constants.WALLET_FIRST_PAYMENT_RECEIVED
import com.videodac.publisher.helpers.Constants.WALLET_PASSWORD
import com.videodac.publisher.helpers.Constants.WALLET_PATH
import com.videodac.publisher.helpers.Constants.WALLET_TAG
import com.videodac.publisher.helpers.Utils.getCameraInstance
import com.videodac.publisher.helpers.Utils.getCurrentChain
import com.videodac.publisher.helpers.Utils.releasePreviewCamera
import com.videodac.publisher.helpers.Utils.walletBalance
import com.videodac.publisher.helpers.Utils.walletFirstPaymentReceived
import com.videodac.publisher.helpers.Utils.walletPrivateKey
import com.videodac.publisher.helpers.Utils.walletAddress
import com.videodac.publisher.helpers.WebThreeHelper.web3
import com.videodac.publisher.ui.AspectTextureView

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import me.lake.librestreaming.client.RESClient
import me.lake.librestreaming.core.listener.RESConnectionListener
import me.lake.librestreaming.core.listener.RESVideoChangeListener
import me.lake.librestreaming.model.RESConfig

import nl.dionsegijn.konfetti.models.Shape

import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Convert
import java.io.IOException
import java.math.BigDecimal

class StreamingActivity : AppCompatActivity(),  SurfaceTextureListener,  RESVideoChangeListener, RESConnectionListener {
    // shared pref
    private lateinit var sharedPref: SharedPreferences

    // view binding
    private lateinit var mainScreenBinding: StreamingScreenBinding
    private lateinit var launchLayoutBinding: StreamingLaunchScreenBinding
    private lateinit var cameraLayoutBinding: StreamingCameraScreenBinding

    // permissions
    private var permissionsAlert: Dialog? = null

    // streaming
    private lateinit var resClient: RESClient
    private var texture: SurfaceTexture? = null
    private var sw = 0
    private var sh = 0
    private var isStreaming = false
    private var flashLightOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // inflate layout
        mainScreenBinding = StreamingScreenBinding.inflate(layoutInflater)
        launchLayoutBinding = StreamingLaunchScreenBinding.bind(mainScreenBinding.root)
        cameraLayoutBinding = StreamingCameraScreenBinding.bind(mainScreenBinding.root)

        setContentView(mainScreenBinding.root)

        // get the prefs
        sharedPref = getSharedPreferences(PREF_NAME, PRIVATE_MODE)

        // load the wallet
        loadWallet()

        // setup the toolbar icons
        showActionBar()

        // finally init the streaming ui
        showLaunchUI()
    }

    // setup custom toolbar with icons
    private fun showActionBar() {
        val inflator = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val v = inflator.inflate(R.layout.streaming_header, null)
        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(false)
        actionBar.setDisplayShowHomeEnabled(false)
        actionBar.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(false)
        actionBar.navigationMode = ActionBar.NAVIGATION_MODE_STANDARD
        actionBar.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        actionBar.customView = v

        val closeButton = findViewById<View>(R.id.close_btn) as ImageButton
        val homeButton = findViewById<View>(R.id.home_btn) as ImageButton

        homeButton.setOnClickListener {
           startActivity(Intent(this@StreamingActivity, InfoActivity::class.java))
            finish()
        }
        closeButton.setOnClickListener {
            finish()
        }
    }

    // show the launch UI
    private fun showLaunchUI() {
        // set the channel instructions
       val spanColor2 = ContextCompat.getColor(this, R.color.light_gray)
        launchLayoutBinding.channelInstructions.setText(
            getString(R.string.choose_a_live_stream),
            BufferType.SPANNABLE
        )
        ( launchLayoutBinding.channelInstructions.text as Spannable).apply {
            setSpan(
                ForegroundColorSpan(spanColor2), 9, 13,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        // listen for clicks on the preview ui, release the camera and launch the streaming ui
        launchLayoutBinding.previewView.setOnClickListener {
            releasePreviewCamera()
            hideStreamingUI()
            showStreamingUI()
        }

    }

    // show the streaming ui
    @Suppress("DEPRECATED_IDENTITY_EQUALS")
    private fun showStreamingUI() {
        // hide the launch screen
        mainScreenBinding.launchScreen.realLaunchScreen.visibility = View.GONE

        // then show the streaming screen
        mainScreenBinding.streamingScreen.realStreamingScreen.visibility = View.VISIBLE
        mainScreenBinding.streamingScreen.livestreamView.surfaceTextureListener = this

        // init the other streaming params
        val rtmpAddress = getString(R.string.rtmp_base_url, walletAddress)
        resClient = RESClient()
        val resConfig = RESConfig.obtain()
        resConfig.targetVideoSize = me.lake.librestreaming.model.Size(
            getString(R.string.video_width).toInt(),
            getString(R.string.video_height).toInt()
        )
        resConfig.bitRate = getString(R.string.video_bitrate).toInt()
        resConfig.videoFPS = getString(R.string.video_fps).toInt()
        resConfig.defaultCamera = CameraInfo.CAMERA_FACING_BACK
        val frontDirection: Int
        val backDirection: Int
        val cameraInfo = CameraInfo()
        Camera.getCameraInfo(CameraInfo.CAMERA_FACING_FRONT, cameraInfo)
        frontDirection = cameraInfo.orientation
        Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, cameraInfo)
        backDirection = cameraInfo.orientation
        if (this.resources.configuration.orientation === Configuration.ORIENTATION_PORTRAIT) {
            resConfig.frontCameraDirectionMode =
                (if (frontDirection == 90) RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_270 else RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_90) or RESConfig.DirectionMode.FLAG_DIRECTION_FLIP_HORIZONTAL
            resConfig.backCameraDirectionMode =
                if (backDirection == 90) RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_90 else RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_270
        } else {
            resConfig.backCameraDirectionMode =
                if (backDirection == 90) RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_0 else RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_180
            resConfig.frontCameraDirectionMode =
                (if (frontDirection == 90) RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_180 else RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_0) or RESConfig.DirectionMode.FLAG_DIRECTION_FLIP_HORIZONTAL
        }
        resConfig.rtmpAddr = rtmpAddress
        if (!resClient.prepare(resConfig)) {
            Log.e(STREAMING_TAG, "prepare,failed!!")
            Toast.makeText(this, "RESClient prepare failed", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val s = resClient.videoSize
        val aspectRatio = s.width.toDouble() / s.height
        mainScreenBinding.streamingScreen.livestreamView.setAspectRatio(
            AspectTextureView.MODE_FITXY,
            aspectRatio
        )

        Log.d(STREAMING_TAG, "version=" + resClient.vertion)

        // update the rest of the UI
        val isMaticNetwork = getCurrentChain(this@StreamingActivity)
        val price = getString(
            R.string.matic_price_label, getString(R.string.streaming_price), getString(
                R.string.price_label
            )
        )

        mainScreenBinding.streamingScreen.youIdenticon.setAddress(walletAddress)
        mainScreenBinding.streamingScreen.streamingPrice.text = price
        mainScreenBinding.streamingScreen.streamingPriceInstruction.text = getString(
            R.string.set_your_price, getString(
                R.string.price_label
            )
        )
        mainScreenBinding.streamingScreen.streamingPriceInstruction.paintFlags = mainScreenBinding.streamingScreen.streamingPriceInstruction.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        mainScreenBinding.streamingScreen.broadcasterIdenticon.setAddress(getString(R.string.broadcaster_address))
        mainScreenBinding.streamingScreen.youFooterIdenticon.setAddress(getString(R.string.you_address))


        mainScreenBinding.streamingScreen.flashLight.setOnClickListener {
            toggleFlashLightIcon()
            resClient.toggleFlashLight()
        }

        // listen for video changes
        resClient.setVideoChangeListener(this)

        // start streaming and set the streaming flag to true
        resClient.startStreaming()
        isStreaming = true
    }

    // toggle flashlight icon switch
    private fun toggleFlashLightIcon() {
        if (flashLightOn) {
            flashLightOn = false
            mainScreenBinding.streamingScreen.flashLight.background = ContextCompat.getDrawable(
                this@StreamingActivity,
                R.drawable.ic_baseline_flash_on_24
            )
        }
        else {
            flashLightOn = true
            mainScreenBinding.streamingScreen.flashLight.background = ContextCompat.getDrawable(
                this@StreamingActivity,
                R.drawable.ic_baseline_flash_off_24
            )
        }
    }

    // hide the streamming ui
    private fun hideStreamingUI() {
        // stop streaming first
        if (isStreaming) {
            resClient.stopStreaming()
        }

        // hide the launch screen
        mainScreenBinding.launchScreen.realLaunchScreen.visibility = View.VISIBLE

        // then show the streaming screen
        mainScreenBinding.streamingScreen.realStreamingScreen.visibility = View.GONE

        // set streaming to false
        isStreaming = false

        // then finally show the launch screen
        showLaunchUI()

    }

    //  start a preview of the camera
    private fun startCameraPreview() {
        // Create an instance of Camera
        val mCamera = getCameraInstance()
        val mPreview = mCamera?.let {
            // Create our Preview view
            CameraPreview(this, it)
        }

        // Set the Preview view as the content of our activity.
        mPreview?.also {
            val preview =  launchLayoutBinding.previewView
            preview.addView(it)
        }
    }

    // allow permissions
    private fun checkPermissions() {
        if (permissionsAlert?.isShowing != true) {
            val options = QuickPermissionsOptions(
                handleRationale = false,
                rationaleMessage = getString(R.string.alert_permissions),
                permanentlyDeniedMessage = getString(R.string.alert_permissions_denied_message),
                permissionsDeniedMethod = ::onPermissionDenied,
                permanentDeniedMethod = ::onPermissionDenied
            )

            runWithPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                options = options,
                callback = {
                    startCameraPreview()
                }
            )
        }
    }

    // handle permission denial
    private fun onPermissionDenied(request: QuickPermissionsRequest) {
        permissionsAlert = AlertDialog.Builder(this)
            .setMessage(R.string.alert_permissions_denied_message)
            .setCancelable(false)
            .setPositiveButton(R.string.close) { _, _ -> finish() }
            .show()
    }

    override fun onResume() {
        super.onResume()

        // Check for permissions and start camera preview
        checkPermissions()
    }

    override fun onPause() {
        super.onPause()
        if(isStreaming) {
            isStreaming = false
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(isStreaming) {
            hideStreamingUI()
        } else{
            releasePreviewCamera()
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        resClient.startPreview(surface, width, height)
        texture = surface
        sw = width
        sh = height
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        resClient.updatePreview(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        resClient.stopPreview(true)
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    override fun onVideoSizeChanged(width: Int, height: Int) {
        mainScreenBinding.streamingScreen.livestreamView.setAspectRatio(
            AspectTextureView.MODE_FITXY,
            width.toDouble() / height
        )
    }

    override fun onOpenConnectionResult(result: Int) {
        /**
         * result==0 success
         * result!=0 failed
         */

        if (result == 0) {
            Log.d(STREAMING_TAG, "server IP = " + resClient.serverIpAddr)

            Toast.makeText(this, "connection started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "connection failed", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onWriteError(errno: Int) {
        /**
         * failed to write data,maybe restart.
         */

        if (errno == 9) {
            resClient.stopStreaming()
            resClient.startStreaming()
            Toast.makeText(this, "Re-connecting to rtmp server", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onCloseConnectionResult(result: Int) {
        /**
         * result==0 success
         * result!=0 failed
         */

    }

    // load an existing wallet
    private fun loadWallet() {

        val walletPath = sharedPref.getString(WALLET_PATH, "")
        val credentials = WalletUtils.loadCredentials(WALLET_PASSWORD, walletPath)

        // set the wallet public/private key global var
        walletAddress = credentials.address
        walletPrivateKey = credentials.ecKeyPair.privateKey.toString(16)

        // set the wallet public key
        mainScreenBinding.channelAddress.text = walletAddress

        // set the identicon
        mainScreenBinding.channelIdenticon.setAddress(walletAddress)


        // set the wallet balance so far
        val walletBalanceString = "__________________"
        val creditText = "Credit: ${getString(R.string.matic_price_label, walletBalanceString, "")}"
        val spanColor1 = ContextCompat.getColor(this, R.color.greenColor)
        val spanLength =  26
        mainScreenBinding.channelCredit.setText(creditText, BufferType.SPANNABLE)
        (mainScreenBinding.channelCredit.text as Spannable).apply {
            setSpan(
                ForegroundColorSpan(spanColor1), 8, spanLength,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        // the check the wallet balance
        checkWalletBalance()

    }

    // check wallet balance from RPC Endpoint
    private fun checkWalletBalance() {

        // check the balance after every 2 secs
        val loopDelay = 2000L

        lifecycleScope.launch(Dispatchers.IO) {

            // finally start a loop to get the user's balance as per the RPC Endpoint
            var checkingBal = true

            while (checkingBal) {

                delay(loopDelay)

                try {
                    // get the web3 client version
                    val clientVersion = web3!!.web3ClientVersion().send()

                    // if client has no error, proceed to check the wallet balance
                    if(!clientVersion.hasError()) {

                        val balanceInWei = web3!!.ethGetBalance(
                            walletAddress,
                            DefaultBlockParameterName.LATEST
                        ).send().balance.toString()
                        walletBalance = Convert.fromWei(balanceInWei, Convert.Unit.ETHER)

                        val balFormat = "%.16f"
                        val walletBalanceString = String.format(balFormat, walletBalance)
                        val creditText = "Credit: ${getString(R.string.matic_price_label,walletBalanceString,"")}"
                        val spanColor1 = ContextCompat.getColor(this@StreamingActivity, R.color.greenColor)
                        val spanLength =  26

                        // update the wallet balance
                        withContext(Dispatchers.Main) {
                            mainScreenBinding.channelCredit.setText(creditText, BufferType.SPANNABLE)
                            (mainScreenBinding.channelCredit.text as Spannable).apply {
                                setSpan(
                                    ForegroundColorSpan(spanColor1), 8, spanLength,
                                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                                )
                            }

                            // if this is the first payment received, show the confetti
                            if (!walletFirstPaymentReceived &&  walletBalance > BigDecimal(0)){
                                val viewKonfetti = mainScreenBinding.streamingScreen.viewKonfetti
                                viewKonfetti.bringToFront()
                                viewKonfetti.build()
                                    .addColors(Color.YELLOW, Color.GREEN, Color.RED)
                                    .setDirection(0.0, 359.0)
                                    .setSpeed(1f, 5f)
                                    .setFadeOutEnabled(true)
                                    .setTimeToLive(2000L)
                                    .addShapes(Shape.Square, Shape.Circle)
                                    .addSizes(nl.dionsegijn.konfetti.models.Size(12))
                                    .setPosition(0f, 800f, 0f, -850f)
                                    .streamFor(300, 5000L)

                                // set the first payment as received
                                sharedPref.edit().apply {
                                    walletFirstPaymentReceived = true
                                    putBoolean(WALLET_FIRST_PAYMENT_RECEIVED, true)
                                    apply()
                                }

                            }

                        }

                        Log.d(WALLET_TAG, "wallet balance in WEI is $balanceInWei")
                        Log.d(WALLET_TAG, "wallet balance in MATIC is $walletBalance")

                    } else{
                        checkingBal = handleError("Client Error!!!")
                    }


                } catch (io: IOException) {
                    checkingBal = handleError("IOException: " + io.message!!)
                } catch (ex: InterruptedException) {
                    checkingBal = handleError("InterruptedException: " + ex.message!!)
                }
                catch (re: RuntimeException){
                    checkingBal = handleError("RuntimeException: " +re.message!!)
                }

            }
        }

    }

    private suspend fun handleError(errorMsg: String?): Boolean {

        Log.e(WALLET_TAG, errorMsg!!)
        withContext(Dispatchers.Main) {

            android.app.AlertDialog.Builder(this@StreamingActivity)
                .setTitle("RPC Error!")
                .setMessage("Unable to connect to the blockchain, please make sure your internet is working and try again!")
                .setCancelable(false)
                .setPositiveButton("ok") { _, _ ->
                    finish()
                }.show()
        }

        return false
    }

}