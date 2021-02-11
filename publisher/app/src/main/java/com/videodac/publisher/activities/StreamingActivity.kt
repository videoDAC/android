package com.videodac.publisher.activities

import android.Manifest
import android.app.Dialog
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.widget.TextView
import android.widget.TextView.BufferType
import android.widget.Toast

import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat

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
import com.videodac.publisher.helpers.TypefaceSpan
import com.videodac.publisher.helpers.Utils.getCameraInstance
import com.videodac.publisher.helpers.Utils.releasePreviewCamera
import com.videodac.publisher.helpers.Utils.walletBalance
import com.videodac.publisher.helpers.Utils.walletPublicKey
import com.videodac.publisher.ui.AspectTextureView
import me.lake.librestreaming.client.RESClient

import me.lake.librestreaming.core.listener.RESVideoChangeListener
import me.lake.librestreaming.model.RESConfig
import me.lake.librestreaming.model.Size

import org.web3j.crypto.Hash
import org.web3j.utils.Numeric

import java.util.*

@Suppress("DEPRECATION")
class StreamingActivity : AppCompatActivity(),  SurfaceTextureListener,  RESVideoChangeListener {
    // shared pref
    private lateinit var sharedPref: SharedPreferences

    // view binding
    private lateinit var mainScreenBinding: StreamingScreenBinding
    private lateinit var launchLayoutBinding : StreamingLaunchScreenBinding
    private lateinit var cameraLayoutBinding : StreamingCameraScreenBinding

    // permissions
    private var permissionsAlert: Dialog? = null

    // streaming
    private lateinit var resClient: RESClient
    protected var texture: SurfaceTexture? = null
    protected var sw = 0
    protected  var sh = 0
    private var isStreaming = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // inflate layout
        mainScreenBinding = StreamingScreenBinding.inflate(layoutInflater)
        launchLayoutBinding = StreamingLaunchScreenBinding.bind(mainScreenBinding.root)
        cameraLayoutBinding = StreamingCameraScreenBinding.bind(mainScreenBinding.root)

        setContentView(mainScreenBinding.root)


        // get the prefs
        sharedPref = getSharedPreferences(PREF_NAME, PRIVATE_MODE)

        // setup the toolbar icons
        setupToolbarIcons()
        centerActionBarTitle()

        // finally init the streaming ui
        showLaunchUI()

    }

    // setup top toolbar icons
    private fun setupToolbarIcons() {
        val actionBar: ActionBar? = supportActionBar
        if (actionBar != null) {

            actionBar.setDisplayHomeAsUpEnabled(true) // switch on the left hand icon
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_dehaze_24) // replace with your custom icon
        }
    }

    // center the toolbar title
    private fun centerActionBarTitle() {
        val titleId: Int = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            resources.getIdentifier("action_bar_title", "id", "android")
        } else {
            // This is the id is from your app's generated R class when ActionBarActivity is used
            // for SupportActionBar
            1
        }

        // Final check for non-zero invalid id
        if (titleId > 0) {
            val titleTextView = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                findViewById<View>(titleId) as TextView
            } else {
                val toolbar = findViewById<View>(R.id.action_bar) as Toolbar
                toolbar.getChildAt(0) as TextView
            }

            titleTextView.gravity = Gravity.CENTER_HORIZONTAL
            titleTextView.width = resources.displayMetrics.widthPixels
            titleTextView.textSize = 28f

            supportActionBar!!.title  = SpannableString(getString(R.string.launch_screen_title)).apply {
                setSpan(
                    TypefaceSpan(this@StreamingActivity, getString(R.string.font_name)),
                    0,
                    length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }


        }
    }

    // show the launch UI
    private fun showLaunchUI() {

        // set the identicon
        val hash = Numeric.toBigInt(
            Hash.sha3(
                walletPublicKey.toLowerCase(Locale.ROOT).toByteArray()
            )
        )
       mainScreenBinding.channelIdenticon.hash = hash.toInt()


        // set the wallet balance so far
        val isMaticNetwork = getString(R.string.rpc_url).contains(
            other = "matic",
            ignoreCase = true
        )

        val symbol = if (isMaticNetwork) "MATIC" else "ETH"
        val balFormat = if (isMaticNetwork) "%.16f" else "%.18f"
        val walletBalanceString = String.format(balFormat, walletBalance)
        val creditText = "Credit: $walletBalanceString $symbol"
        val spanColor1 = ContextCompat.getColor(this, R.color.walletBalance)
        val spanLength = if (isMaticNetwork) 26 else 28
        mainScreenBinding.channelCredit.setText(creditText, BufferType.SPANNABLE)
         (mainScreenBinding.channelCredit.text as Spannable).apply {
            setSpan(
                ForegroundColorSpan(spanColor1), 8, spanLength,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        // set the wallet public key
        mainScreenBinding.channelAddress.text = walletPublicKey


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
            showStreamingUI()
        }

    }

    // show the streaming ui
    private fun showStreamingUI() {

        // hide the launch screen
        mainScreenBinding.launchScreen.realLaunchScreen.visibility = View.GONE

        // then show the streaming screen
        mainScreenBinding.streamingScreen.realStreamingScreen.visibility = View.VISIBLE
        mainScreenBinding.streamingScreen.livestreamView.surfaceTextureListener = this

        // init the other streaming params
        val rtmpaddr = getString(R.string.rtmp_base_url, walletPublicKey)
        resClient = RESClient()
        val resConfig = RESConfig.obtain()
        resConfig.filterMode = RESConfig.FilterMode.SOFT
        resConfig.targetVideoSize = Size(1440, 810)
        resConfig.bitRate = 750 * 1024
        resConfig.videoFPS = 20
        resConfig.videoGOP = 1
        resConfig.renderingMode = RESConfig.RenderingMode.OpenGLES
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
        resConfig.rtmpAddr = rtmpaddr
        if (!resClient.prepare(resConfig)) {
            Log.e(STREAMING_TAG, "prepare,failed!!")
            Toast.makeText(this, "RESClient prepare failed", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val s = resClient.videoSize
        val aspectRatio = s.width.toDouble() / s.height
        mainScreenBinding.streamingScreen.livestreamView.setAspectRatio(
            AspectTextureView.MODE_OUTSIDE,
            aspectRatio
        )
        Log.d(STREAMING_TAG, "version=" + resClient.vertion)

        resClient.setVideoChangeListener(this)

        resClient.startStreaming()
        isStreaming = true

    }

    // hide the streamming ui
    private fun hideStreamingUI() {
        // stop streaming first
        if (isStreaming) {
            resClient.stopStreaming()
            resClient.destroy()
        }


        // hide the launch screen
        mainScreenBinding.launchScreen.realLaunchScreen.visibility = View.VISIBLE

        // then show the streaming screen
        mainScreenBinding.streamingScreen.realStreamingScreen.visibility = View.GONE

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
            hideStreamingUI()
        } else{
            releasePreviewCamera()
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.close_activity -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
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

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

    }

    override fun onVideoSizeChanged(width: Int, height: Int) {
        mainScreenBinding.streamingScreen.livestreamView.setAspectRatio(
            AspectTextureView.MODE_INSIDE,
            width.toDouble() / height
        )
    }

}