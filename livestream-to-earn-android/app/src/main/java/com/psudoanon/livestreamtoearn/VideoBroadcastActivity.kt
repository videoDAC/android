package com.psudoanon.livestreamtoearn

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.ActivityInfo
import android.hardware.Camera
import android.icu.text.IDNA
import android.opengl.GLSurfaceView
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.psudoanon.broadcaster.LiveVideoBroadcaster
import com.psudoanon.broadcaster.OnEventListener
import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.Words
import io.github.novacrypto.bip39.wordlists.English
import io.github.novacrypto.hashing.Sha256.sha256
import kotlinx.android.synthetic.main.activity_video_broadcast.*
import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jFactory
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.rx.Web3jRx
import org.web3j.utils.Convert
import rx.internal.util.ActionSubscriber
import rx.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.lang.Exception
import java.math.BigInteger
import java.util.*
import kotlin.random.Random
import kotlin.system.exitProcess

class VideoBroadcastActivity : AppCompatActivity(), View.OnClickListener {

    private var live    = false
    private var flashOn = false

    private lateinit var wallet:          WalletFile
    private lateinit var mGLView:         GLSurfaceView
    private lateinit var mBalanceView:    TextView
    private lateinit var mWeb3Connection: Web3j

    private var privateKeyHex:         String?               = null
    private var address:               String?               = null
    private var keyPair:               ECKeyPair?            = null
    private var privateKeyDec:         BigInteger?           = null
    private var mLiveVideoBroadcaster: LiveVideoBroadcaster? = null

    private val INFURA_URL            = "https://mainnet.infura.io/v3/9a1db80dbf7f4e4fa036a38f2618c71a"
    private val RTMP_BASE_URL         = "rtmp://192.168.1.151:1935/0x%s"
    private val PUBLIC_ADDRESS_KEY    = "public_address"
    private val PRIVATE_HEX_KEY       = "private"
    private val WALLET_PASSWORD       = "changeme"
    private val N_CHANNEL_ID          = "LSTE_NOTIFICATION_CHANNEL"
    private val N_CHANNEL_NAME        = "Livestream to Earn"
    private val N_CHANNEL_DESC        = "Livestream to Earn Notification Channel"
    private val DESIRED_AUDIO_BITRATE = 128  * 1024
    private val DESIRED_VIDEO_BITRATE = 1000 * 1024
    private val I_FRAME_INTERVAL_SEC  = 1
    private val TIMER_INTERVAL        = (30 * 1000).toLong()   // Fetch balance every 30 seconds

    private val mConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mLiveVideoBroadcaster = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LiveVideoBroadcaster.LocalBinder

            if (mLiveVideoBroadcaster == null) {
                mLiveVideoBroadcaster = binder.service as LiveVideoBroadcaster
                mLiveVideoBroadcaster!!.init(this@VideoBroadcastActivity, mGLView)

                mLiveVideoBroadcaster!!.setVideoBitrate(DESIRED_VIDEO_BITRATE)
                mLiveVideoBroadcaster!!.setAudioBitrate(DESIRED_AUDIO_BITRATE)
                mLiveVideoBroadcaster!!.setIFrameIntervalSeconds(I_FRAME_INTERVAL_SEC)

                // mLiveVideoBroadcaster!!.setAdaptiveStreaming(true)
            }

            mLiveVideoBroadcaster!!.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK)
        }
    }

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        val UPDATE_TEXT = 1

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                UPDATE_TEXT -> {
                    try {
                        val balance = mWeb3Connection.ethGetBalance("0x"+address, DefaultBlockParameterName.LATEST).send()
                        val ethBalance = Convert.fromWei(balance.balance.toString(), Convert.Unit.ETHER)

                        runOnUiThread {
                            mBalanceView.text = String.format("Balance: %s", ethBalance.toString())
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            mBalanceView.text = "Unable to fetch wallet balance"
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setupNotificationChannel()

        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        this.supportActionBar?.hide()
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_video_broadcast)

        mGLView = findViewById<GLSurfaceView>(R.id.cameraPerview).also { it.setOnClickListener(this) }
        mGLView.setEGLContextClientVersion(2)

        mBalanceView = findViewById(R.id.balanceView)

        mBalanceView.text = "Fetching balance..."

        mWeb3Connection = Web3jFactory.build(HttpService(INFURA_URL))

        if (getPrivateKey() == null || getWalletAddress() == null) {
            keyPair       = Keys.createEcKeyPair()
            privateKeyDec = keyPair?.privateKey
            privateKeyHex = privateKeyDec?.toString(16)
            wallet        = Wallet.createLight(WALLET_PASSWORD, keyPair)
            address       = wallet.address

            val sharedPreferences = getPreferences(Context.MODE_PRIVATE)

            with (sharedPreferences.edit()) {
                putString(PRIVATE_HEX_KEY, privateKeyHex)
                putString(PUBLIC_ADDRESS_KEY, address)
                commit()
            }
        } else {
            address = getWalletAddress()
            privateKeyHex = getPrivateKey()
        }

        val timer = Timer()

        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                mHandler.dispatchMessage(mHandler.obtainMessage(1))
            }

        }, 0, TIMER_INTERVAL)
    }

    override fun onResume() {
        super.onResume()
        Handler().postDelayed({
            if (!live) {
                toggleBroadcasting()
                toggleFlash()
            }
        }, 1000)
    }

    override fun onStart() {
        super.onStart()
        Intent(this, LiveVideoBroadcaster::class.java).also { intent -> bindService(intent, mConnection, Context.BIND_AUTO_CREATE) }
    }

    override fun onStop() {
        super.onStop()
        unbindService(mConnection)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.cameraPerview -> {
                if (live) {
                    mLiveVideoBroadcaster!!.stopBroadcasting()
                }
                copyPrivateKeyToClipboard()
                finishAffinity()
                exitProcess(0)
            }
        }
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(N_CHANNEL_ID, N_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = N_CHANNEL_DESC
            }

            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun copyPrivateKeyToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("key", getPrivateKey())

        clipboard.setPrimaryClip(clip)
        val notification = NotificationCompat.Builder(this, N_CHANNEL_ID)
                                            .setSmallIcon(R.drawable.ic_flash_on_24px)
                                            .setContentTitle("Livestream to Earn")
                                            .setContentText("Private key copied to clipboard")
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(0, notification.build())
        }
    }

    private fun toggleBroadcasting() {
        try {
            if (address != null) {
                if (!live) {
                    val streamUrl = String.format(RTMP_BASE_URL, address)

                    showLongToast(String.format("Broadcasting to %s", streamUrl))
                    mLiveVideoBroadcaster!!.startBroadcasting(streamUrl)

                    live = true
                } else {
                    showLongToast("Broadcast stopped")
                    mLiveVideoBroadcaster!!.stopBroadcasting()
                    live = false
                }
            } else {
                showLongToast("Missing wallet")
            }
        } catch (e: Exception) {
            showLongToast("Error starting broadcast")
        }
    }

    private fun toggleFlash() {
        if (!flashOn) {
            mLiveVideoBroadcaster?.startFlash()
            flashOn = true
        } else {
            mLiveVideoBroadcaster?.stopFlash()
            flashOn = false
        }
    }

    private fun getPrivateKey(): String? {
        return getPreferences(Context.MODE_PRIVATE).getString(PRIVATE_HEX_KEY, null)
    }

    private fun getWalletAddress(): String? {
        return getPreferences(Context.MODE_PRIVATE).getString(PUBLIC_ADDRESS_KEY, null)
    }

    private fun showLongToast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
    }
}
