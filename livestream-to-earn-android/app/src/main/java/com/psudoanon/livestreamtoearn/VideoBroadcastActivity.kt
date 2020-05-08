package com.psudoanon.livestreamtoearn

import android.content.*
import android.hardware.Camera
import android.icu.text.IDNA
import android.opengl.GLSurfaceView
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.*
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
    private lateinit var mSettingsBtn:    ImageButton
    private lateinit var mStreamButton:   Button
    private lateinit var mWeb3Connection: Web3j

    private var privateKeyHex:         String?               = null
    private var address:               String?               = null
    private var keyPair:               ECKeyPair?            = null
    private var privateKeyDec:         BigInteger?           = null
    private var mLiveVideoBroadcaster: LiveVideoBroadcaster? = null

    private val INFURA_URL            = "https://mainnet.infura.io/v3/"
    private val RTMP_BASE_URL         = "rtmp://192.168.1.151:1935/0x%s"
    private val PUBLIC_ADDRESS_KEY    = "public_address"
    private val PRIVATE_HEX_KEY       = "private"
    private val WALLET_PASSWORD       = "changeme"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_video_broadcast)

        mGLView = findViewById<GLSurfaceView>(R.id.cameraPerview).also { it.setOnClickListener(this) }
        mGLView.setEGLContextClientVersion(2)

        mStreamButton = findViewById<Button>(R.id.streamButton).also { it.setOnClickListener(this) }
        mSettingsBtn = findViewById<ImageButton>(R.id.flashButton).also { it.setOnClickListener(this) }
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
            R.id.streamButton -> toggleBroadcasting()
            R.id.cameraPerview -> {
                if (live) {
                    mLiveVideoBroadcaster!!.stopBroadcasting()
                }
                copyPrivateKeyToClipboard()
                finishAffinity()
                exitProcess(0)
            }
            R.id.flashButton -> {
                toggleFlash()
            }
        }
    }

    private fun copyPrivateKeyToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("key", getPrivateKey())

        clipboard.setPrimaryClip(clip)
    }

    private fun toggleBroadcasting() {
        try {
            if (address != null) {
                if (!live) {
                    val streamUrl = String.format(RTMP_BASE_URL, address)

                    showLongToast(String.format("Broadcasting to %s", streamUrl))
                    mLiveVideoBroadcaster!!.startBroadcasting(streamUrl)

                    mStreamButton.text = "Stop Stream"
                    live = true
                } else {
                    showLongToast("Broadcast stopped")
                    mLiveVideoBroadcaster!!.stopBroadcasting()
                    mStreamButton.text = "Start Stream"
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
            mSettingsBtn.setImageResource(R.drawable.ic_flash_off_24px)
            flashOn = true
        } else {
            mLiveVideoBroadcaster?.stopFlash()
            mSettingsBtn.setImageResource(R.drawable.ic_flash_on_24px)
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
