package com.videodac.publisher.ui

import android.content.Context
import android.graphics.*
import android.graphics.LinearGradient
import android.util.AttributeSet
import android.view.View
import com.videodac.publisher.R

/**
 * Custom view that is used to display an ethereum address identicon
 */
class BlockiesIdenticon : View {

    private val DEFAULT_RADIUS = 1f
    private var mData: BlockiesData
    private var mColor: Paint? = null
    private var mBackground: Paint? = null
    private var mSpot: Paint? = null
    private val mBlock = RectF()
    private var cornerRadius = 20f
    private var mShadowPaint: Paint? = null
    private var mBrightPaint: Paint? = null
    var isHasShadow = false

    constructor(context: Context?) : super(context) {
        mData = BlockiesData("", BlockiesData.DEFAULT_SIZE)
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.BlockiesIdenticon, 0, 0)
        var address: String? = ""
        try {
            cornerRadius = a.getFloat(R.styleable.BlockiesIdenticon_radius, DEFAULT_RADIUS)
            address = a.getString(R.styleable.BlockiesIdenticon_address)
        } finally {
            a.recycle()
        }
        mData = BlockiesData(address, BlockiesData.DEFAULT_SIZE)
        init()
    }

    private fun init() {
        val colors: IntArray = mData.colors
        mColor = Paint()
        mColor!!.style = Paint.Style.FILL
        mColor!!.color = colors[0]
        mBackground = Paint()
        mBackground!!.style = Paint.Style.FILL
        mBackground!!.isAntiAlias = true
        mBackground!!.color = colors[1]
        mSpot = Paint()
        mSpot!!.style = Paint.Style.FILL
        mSpot!!.color = colors[2]
        mShadowPaint = Paint()
        mShadowPaint!!.isAntiAlias = true
        mShadowPaint!!.isDither = true
        mBrightPaint = Paint()
        mBrightPaint!!.isDither = true
    }

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        // Account for padding
        val xpad = (paddingLeft + paddingRight).toFloat()
        val ypad = (paddingTop + paddingBottom).toFloat()
        val right = xpad + width
        val bottom = ypad + height
        mBlock[xpad, ypad, right] = bottom
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val clipPath = Path()
        val radius = 10.0f
        val padding = radius / 2
        val w = this.width
        val h = this.height
        clipPath.addRoundRect(
            RectF(padding, padding, w - padding, h - padding),
            cornerRadius,
            cornerRadius,
            Path.Direction.CW
        )
        canvas.clipPath(clipPath)
        canvas.drawRect(mBlock, mBackground!!)
        val data: IntArray = mData.imageData
        val blockSize = w / Math.sqrt(data.size.toDouble())
        for (i in data.indices) {
            val x = blockSize * i % w
            val y = Math.floor(blockSize * i / w) * blockSize
            val rect = RectF(
                x.toFloat(), y.toFloat(), (x + blockSize).toFloat(),
                (y + blockSize).toFloat()
            )
            if (data[i] == 2) {
                canvas.drawRect(rect, mSpot!!)
            } else if (data[i] == 1) {
                canvas.drawRect(rect, mColor!!)
            }
        }
        if (isHasShadow) {
            val shadowColors =
                intArrayOf(Color.argb(200, 50, 50, 50), Color.argb(100, 0, 0, 0), Color.TRANSPARENT)
            val positions = floatArrayOf(0.20f, 0.35f, 1f)
            val shadowGradient = LinearGradient(
                (w / 2).toFloat(),
                h.toFloat(),
                (w / 2).toFloat(),
                (h - blockSize).toFloat(),
                shadowColors,
                positions,
                Shader.TileMode.CLAMP
            )
            mShadowPaint!!.shader = shadowGradient
            val brightColors = intArrayOf(Color.argb(100, 255, 255, 255), Color.TRANSPARENT)
            val brightGradient = LinearGradient(
                (w / 2).toFloat(),
                0f,
                (w / 2).toFloat(),
                blockSize.toFloat(),
                brightColors,
                null,
                Shader.TileMode.CLAMP
            )
            mBrightPaint!!.shader = brightGradient
            canvas.drawRect(mBlock, mShadowPaint!!)
            canvas.drawRect(mBlock, mBrightPaint!!)
        }
    }

    fun setAddress(address: String?) {
        this.setAddress(address, BlockiesData.DEFAULT_SIZE)
        init()
    }

    fun setAddress(address: String?, size: Int) {
        mData = BlockiesData(address, size)
        init()
    }

    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        init()
    }
}